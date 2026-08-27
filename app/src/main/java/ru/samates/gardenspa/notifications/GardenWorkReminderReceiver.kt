package ru.samates.gardenspa.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.scheduledTreatmentsOn

class GardenWorkReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            GardenWorkReminderScheduler.schedule(context)
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showReminderWhenThereWasWork(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showReminderWhenThereWasWork(context: Context) {
        val application = context as BookeeperApp
        val today = LocalDate.now()
        val treatments = scheduledTreatmentsOn(
            plants = application.repository.getAllPlantsOnce(),
            procedures = application.repository.getAllProceduresOnce(),
            date = today
        )
        if (treatments.isEmpty()) return
        GardenWorkReminderScheduler.showReminder(context)
    }
}
