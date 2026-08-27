package ru.samates.gardenspa.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.R
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.others.MainActivity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.coroutines.coroutineContext

internal val treatmentNotificationLock = Any()
internal var treatmentNotificationRevision = 0L

class TreatmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            TreatmentReminderScheduler.schedule(context)
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (showTreatmentReminders(context.applicationContext)) {
                    TreatmentReminderScheduler.markCheckedToday(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showTreatmentReminders(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        val application = context as BookeeperApp
        val today = LocalDate.now()
        val treatments = currentRegularTreatments(application, today)
        treatments.forEach { treatment ->
            publishRegularReminderIfCurrent(application, treatment, today)
        }
        WeatherReminderJobService.schedule(context)
        return true
    }
}

class WeatherRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val plantId = intent?.getIntExtra(EXTRA_PLANT_ID, -1) ?: return
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME)?.takeIf(String::isNotBlank) ?: return
        val originalDate = intent.localDateExtra(EXTRA_ORIGINAL_DATE) ?: return
        val currentDate = intent.localDateExtra(EXTRA_CURRENT_DATE) ?: return
        val newDate = intent.localDateExtra(EXTRA_NEW_DATE)?.takeIf { it.isAfter(currentDate) } ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as BookeeperApp
                val plants = application.repository.getAllPlantsOnce()
                val procedures = application.repository.getAllProceduresOnce()
                val isStillPending = scheduledTreatmentsOn(plants, procedures, currentDate).any { treatment ->
                    treatment.plant.id == plantId &&
                        treatment.originalDate == originalDate &&
                        !treatment.completed
                }
                if (!isStillPending) return@launch
                application.repository.insertProcedure(
                    ProcedureEntity(
                        plantId = plantId,
                        procedureName = taskName,
                        scheduledDate = originalDate.toString(),
                        rescheduledDate = newDate.toString().takeIf { newDate != originalDate },
                        status = "PLANNED"
                    )
                )
                TreatmentReminderScheduler.cancelTreatmentNotification(
                    application,
                    plantId,
                    originalDate.toString()
                )
                TreatmentNotificationPublisher.publishRescheduled(
                    context = application,
                    taskName = taskName,
                    newDate = newDate,
                    notificationId = TreatmentReminderScheduler.weatherNotificationId(
                        plantId,
                        originalDate.toString()
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun Intent.localDateExtra(name: String): LocalDate? =
    getStringExtra(name)?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

private suspend fun currentRegularTreatments(
    application: BookeeperApp,
    today: LocalDate
): List<ScheduledTreatment> {
    val plants = application.repository.getAllPlantsOnce()
    val procedures = application.repository.getAllProceduresOnce()
    return listOf(0, 1, 5).flatMap { daysBefore ->
        scheduledTreatmentsOn(
            plants = plants,
            procedures = procedures,
            date = today.plusDays(daysBefore.toLong())
        ).filter { treatment ->
            !treatment.completed && treatment.plant.reminderDaysBefore == daysBefore
        }
    }.distinctBy { it.plant.id to it.originalDate }
}

private suspend fun publishRegularReminderIfCurrent(
    application: BookeeperApp,
    expected: ScheduledTreatment,
    today: LocalDate
) {
    while (true) {
        coroutineContext.ensureActive()
        val observedRevision = synchronized(treatmentNotificationLock) {
            treatmentNotificationRevision
        }
        val current = currentRegularTreatments(application, today).firstOrNull { treatment ->
            treatment.plant.id == expected.plant.id && treatment.originalDate == expected.originalDate
        }
        coroutineContext.ensureActive()
        val shouldRetry = synchronized(treatmentNotificationLock) {
            if (observedRevision != treatmentNotificationRevision) {
                true
            } else {
                if (current == null) {
                    TreatmentNotificationPublisher.cancelRegular(application, expected)
                    TreatmentNotificationPublisher.cancelWeather(application, expected)
                } else if (
                    shouldPublishRegularReminder(
                        observedRevision = observedRevision,
                        currentRevision = treatmentNotificationRevision,
                        isStillPending = true,
                        weatherWarningActive = TreatmentNotificationPublisher.isWeatherWarningActive(
                            application,
                            current
                        )
                    )
                ) {
                    TreatmentNotificationPublisher.publish(application, current, today)
                }
                false
            }
        }
        if (!shouldRetry) return
    }
}

internal fun shouldPublishRegularReminder(
    observedRevision: Long,
    currentRevision: Long,
    isStillPending: Boolean,
    weatherWarningActive: Boolean
): Boolean = isNotificationRevisionCurrent(observedRevision, currentRevision) &&
    isStillPending &&
    !weatherWarningActive

internal fun isNotificationRevisionCurrent(
    observedRevision: Long,
    currentRevision: Long
): Boolean = observedRevision == currentRevision

internal object TreatmentNotificationPublisher {
    fun publish(
        context: Context,
        treatment: ScheduledTreatment,
        today: LocalDate,
        weatherAdvice: String? = null,
        weatherHasPrecipitation: Boolean = false,
        suggestedDate: LocalDate? = null,
        silent: Boolean = false
    ) {
        val plant = treatment.plant
        val timing = when (ChronoUnit.DAYS.between(today, treatment.scheduledDate).toInt()) {
            0 -> "Сегодня"
            1 -> "Завтра"
            5 -> "Через 5 дней"
            else -> "Скоро"
        }
        val details = buildString {
            append(plant.taskName)
            append(" — ")
            append(plant.plantName)
            if (plant.gardenName.isNotBlank()) append(", ${plant.gardenName}")
            val drugName = plant.drugName.toDrugDisplayName()
            if (drugName.isNotBlank()) {
                append(if (drugName == NO_DRUG_REQUIRED_LABEL) ", $drugName" else ", препарат: $drugName")
            }
        }
        val openApp = PendingIntent.getActivity(
            context,
            plant.id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, TreatmentReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(
                if (weatherAdvice == null) "$timing — запланированная процедура"
                else if (weatherHasPrecipitation) "$timing дождь — перенести обработку?"
                else "$timing неподходящая погода — перенести работу?"
            )
            .setContentText(
                if (weatherAdvice == null) details
                else "Хотите перенести «${plant.taskName}»?"
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (weatherAdvice == null) "$timing: $details"
                    else "Хотите перенести «${plant.taskName}»? $weatherAdvice\n$details"
                )
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(silent)
            .apply {
                if (weatherAdvice != null) {
                    val expiresAt = treatment.scheduledDate
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    setTimeoutAfter((expiresAt - System.currentTimeMillis()).coerceAtLeast(60_000L))
                    suggestedDate?.let { date ->
                        addAction(
                            R.mipmap.ic_launcher,
                            "Перенести на ${date.shortRussianDate()}",
                            PendingIntent.getBroadcast(
                                context,
                                treatment.weatherNotificationId(),
                                Intent(context, WeatherRescheduleReceiver::class.java).apply {
                                    putExtra(EXTRA_PLANT_ID, plant.id)
                                    putExtra(EXTRA_TASK_NAME, plant.taskName)
                                    putExtra(EXTRA_ORIGINAL_DATE, treatment.originalDate.toString())
                                    putExtra(EXTRA_CURRENT_DATE, treatment.scheduledDate.toString())
                                    putExtra(EXTRA_NEW_DATE, date.toString())
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    }
                }
            }
            .build()
        val notificationId = if (weatherAdvice == null) {
            treatment.regularNotificationId()
        } else {
            treatment.weatherNotificationId()
        }
        context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    fun publishRescheduled(
        context: Context,
        taskName: String,
        newDate: LocalDate,
        notificationId: Int
    ) {
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, TreatmentReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Обработка перенесена")
            .setContentText("«$taskName» — ${newDate.shortRussianDate()}")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setTimeoutAfter(6 * 60 * 60 * 1_000L)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    fun cancelRegular(context: Context, treatment: ScheduledTreatment) {
        context.getSystemService(NotificationManager::class.java).cancel(treatment.regularNotificationId())
    }

    fun cancelWeather(context: Context, treatment: ScheduledTreatment) {
        context.getSystemService(NotificationManager::class.java).cancel(treatment.weatherNotificationId())
    }

    fun isWeatherWarningActive(context: Context, treatment: ScheduledTreatment): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications
            .any { notification -> notification.id == treatment.weatherNotificationId() }

    fun isRegularReminder(treatment: ScheduledTreatment, today: LocalDate): Boolean {
        val daysBefore = ChronoUnit.DAYS.between(today, treatment.scheduledDate).toInt()
        return daysBefore in setOf(0, 1, 5) && treatment.plant.reminderDaysBefore == daysBefore
    }

    private fun ScheduledTreatment.regularNotificationId(): Int =
        TreatmentReminderScheduler.notificationId(plant.id, originalDate.toString())

    private fun ScheduledTreatment.weatherNotificationId(): Int =
        TreatmentReminderScheduler.weatherNotificationId(plant.id, originalDate.toString())
}

private const val EXTRA_PLANT_ID = "weather_plant_id"
private const val EXTRA_TASK_NAME = "weather_task_name"
private const val EXTRA_ORIGINAL_DATE = "weather_original_date"
private const val EXTRA_CURRENT_DATE = "weather_current_date"
private const val EXTRA_NEW_DATE = "weather_new_date"

private val SHORT_RUSSIAN_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru-RU"))

private fun LocalDate.shortRussianDate(): String = format(SHORT_RUSSIAN_DATE)
