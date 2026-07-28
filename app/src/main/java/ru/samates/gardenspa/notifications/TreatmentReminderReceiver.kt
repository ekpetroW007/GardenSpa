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
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.R
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.others.MainActivity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TreatmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            TreatmentReminderScheduler.schedule(context)
        }
        TreatmentReminderScheduler.markCheckedToday(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showTomorrowReminders(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showTomorrowReminders(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val application = context as BookeeperApp
        val tomorrow = LocalDate.now().plusDays(1)
        val treatments = scheduledTreatmentsOn(
            plants = application.repository.getAllPlantsOnce(),
            procedures = application.repository.getAllProceduresOnce(),
            date = tomorrow
        ).filterNot { it.completed }
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        treatments.forEach { treatment ->
            val plant = treatment.plant
            val details = buildString {
                append(plant.taskName)
                append(" — ")
                append(plant.plantName)
                if (plant.gardenName.isNotBlank()) append(", ${plant.gardenName}")
                if (plant.drugName.isNotBlank()) append(", препарат: ${plant.drugName}")
            }
            val openApp = PendingIntent.getActivity(
                context,
                treatment.plant.id,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, TreatmentReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Запланированная обработка")
                .setContentText("Запланированная обработка: $details")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Запланированная обработка: $details"))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(
                TreatmentReminderScheduler.notificationId(plant.id, treatment.originalDate.toString()),
                notification
            )
        }
    }

}
