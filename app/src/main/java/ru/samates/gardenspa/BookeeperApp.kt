package ru.samates.gardenspa

import android.app.Application
import kotlin.getValue
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.climate.ClimateService
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.notifications.GardenWorkReminderScheduler

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

    val climateService by lazy { ClimateService() }

    override fun onCreate() {
        super.onCreate()
        TreatmentReminderScheduler.schedule(this)
        TreatmentReminderScheduler.refreshOnceToday(this)
        GardenWorkReminderScheduler.schedule(this)
    }
}
