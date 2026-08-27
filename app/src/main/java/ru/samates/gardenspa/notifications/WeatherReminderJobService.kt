package ru.samates.gardenspa.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.locationOrNull
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.WeatherLimits
import ru.samates.gardenspa.domain.WeatherLimitKind
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.suggestedWeatherSafeDate
import ru.samates.gardenspa.domain.weatherWorkAdvice
import kotlin.coroutines.coroutineContext

class WeatherReminderJobService : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        work = scope.launch {
            val retry = try {
                updateWeatherNotifications(applicationContext as BookeeperApp)
            } catch (_: CancellationException) {
                scope.cancel()
                return@launch
            } catch (_: Exception) {
                true
            }
            jobFinished(params, retry)
            scope.cancel()
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        work = null
        return true
    }

    companion object {
        private const val JOB_ID = 0x475357
        private const val RETRY_BACKOFF_MILLIS = 15 * 60 * 1_000L
        private const val START_DELAY_MILLIS = 30_000L

        fun schedule(context: Context) {
            val job = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, WeatherReminderJobService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(START_DELAY_MILLIS)
                .setBackoffCriteria(RETRY_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()
            check(context.getSystemService(JobScheduler::class.java).schedule(job) == JobScheduler.RESULT_SUCCESS) {
                "Не удалось запланировать погодную проверку"
            }
        }

        fun reschedule(context: Context) {
            context.getSystemService(JobScheduler::class.java).cancel(JOB_ID)
            schedule(context)
        }
    }
}

private suspend fun updateWeatherNotifications(application: BookeeperApp): Boolean {
    val jobRevision = synchronized(treatmentNotificationLock) {
        treatmentNotificationRevision
    }
    val today = LocalDate.now()
    val plants = application.repository.getAllPlantsOnce()
    val procedures = application.repository.getAllProceduresOnce()
    val candidates = listOf(today, today.plusDays(1))
        .flatMap { date -> scheduledTreatmentsOn(plants, procedures, date) }
        .filter { treatment -> !treatment.completed && treatment.isGeneratedWeatherSensitive() }
        .distinctBy { it.notificationKey() }
    if (candidates.isEmpty()) return false

    val treatmentsByGarden = candidates
        .mapNotNull { treatment -> treatment.plant.gardenId?.let { gardenId -> gardenId to treatment } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    val gardensById = application.repository.allGardens.first().associateBy { it.id }
    var retryNeeded = false

    treatmentsByGarden.forEach { (gardenId, treatments) ->
        val location = gardensById[gardenId]?.locationOrNull() ?: return@forEach
        val forecast = try {
            application.climateService.loadForecast(location).takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Пустой прогноз")
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            retryNeeded = true
            return@forEach
        }
        val forecastDates = forecast.mapTo(mutableSetOf()) { it.date }
        val coveredTreatments = treatments.filter { treatment ->
            (treatment.scheduledDate in forecastDates).also { covered ->
                if (!covered) retryNeeded = true
            }
        }
        val adviceByTreatment = weatherWorkAdvice(coveredTreatments, forecast, today)
            .associateBy { it.treatment.notificationKey() }
        coveredTreatments.forEach treatmentLoop@{ treatment ->
            coroutineContext.ensureActive()
            val isStillPending = treatment.notificationKey() in currentWeatherCandidateKeys(application, today)
            coroutineContext.ensureActive()
            synchronized(treatmentNotificationLock) {
                coroutineContext.ensureActive()
                if (!isNotificationRevisionCurrent(jobRevision, treatmentNotificationRevision)) return false
                if (!isStillPending) {
                    TreatmentNotificationPublisher.cancelRegular(application, treatment)
                    TreatmentNotificationPublisher.cancelWeather(application, treatment)
                    return@treatmentLoop
                }
                val advice = adviceByTreatment[treatment.notificationKey()]
                if (advice == null) {
                    restoreRegularReminderOrCancelWeather(application, treatment, today)
                } else {
                    TreatmentNotificationPublisher.publish(
                        context = application,
                        treatment = treatment,
                        today = today,
                        weatherAdvice = advice.message,
                        weatherHasPrecipitation = advice.violations.any {
                            it.kind == WeatherLimitKind.PRECIPITATION
                        },
                        suggestedDate = suggestedWeatherSafeDate(treatment, forecast)
                    )
                    TreatmentNotificationPublisher.cancelRegular(application, treatment)
                }
            }
        }
    }
    return retryNeeded
}

private suspend fun currentWeatherCandidateKeys(
    application: BookeeperApp,
    today: LocalDate
): Set<Pair<Int, LocalDate>> {
    val plants = application.repository.getAllPlantsOnce()
    val procedures = application.repository.getAllProceduresOnce()
    return listOf(today, today.plusDays(1))
        .flatMap { date -> scheduledTreatmentsOn(plants, procedures, date) }
        .filter { treatment -> !treatment.completed && treatment.isGeneratedWeatherSensitive() }
        .mapTo(mutableSetOf()) { it.notificationKey() }
}

private fun restoreRegularReminderOrCancelWeather(
    context: Context,
    treatment: ScheduledTreatment,
    today: LocalDate
) {
    TreatmentNotificationPublisher.cancelWeather(context, treatment)
    if (TreatmentNotificationPublisher.isRegularReminder(treatment, today)) {
        TreatmentNotificationPublisher.publish(context, treatment, today, silent = true)
    }
}

private fun ScheduledTreatment.isGeneratedWeatherSensitive(): Boolean {
    val templateId = plant.programId ?: return false
    val stepId = plant.programStepId ?: return false
    val step = PlantCareCatalog.all()
        .firstOrNull { it.id == templateId }
        ?.steps
        ?.firstOrNull { it.id == stepId }
        ?: return false
    return step.weatherLimits != WeatherLimits()
}

private fun ScheduledTreatment.notificationKey(): Pair<Int, LocalDate> = plant.id to originalDate
