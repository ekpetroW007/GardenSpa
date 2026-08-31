package ru.samates.gardenspa

import android.app.Application
import kotlin.getValue
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.climate.ClimateService
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.notifications.GardenWorkReminderScheduler
import ru.samates.gardenspa.notifications.WeatherReminderJobService

class BookeeperApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        BookeeperRepository(
            drugDao = database.drugDao(),
            plantDAO = database.plantDao(),
            gardenDAO = database.gardenDao(),
            taskDAO = database.taskDao(),
            procedureDAO = database.procedureDao(),
            gardenWorkDAO = database.gardenWorkDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        TreatmentReminderScheduler.schedule(this)
        TreatmentReminderScheduler.refreshOnceToday(this)
        runCatching { WeatherReminderJobService.reschedule(this) }
        GardenWorkReminderScheduler.schedule(this)
    }

    val climateService by lazy {
        ClimateService()
    }
}

internal fun buildConfigString(fieldName: String): String =
    BuildConfig::class.java.getField(fieldName).get(null) as String
