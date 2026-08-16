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
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.toDrugDisplayName
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
                showTreatmentReminders(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showTreatmentReminders(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val application = context as BookeeperApp
        val today = LocalDate.now()
        val plants = application.repository.getAllPlantsOnce()
        val procedures = application.repository.getAllProceduresOnce()
        val treatments = listOf(0, 1, 5).flatMap { daysBefore ->
            scheduledTreatmentsOn(
                plants = plants,
                procedures = procedures,
                date = today.plusDays(daysBefore.toLong())
            ).filter { treatment ->
                !treatment.completed && treatment.plant.reminderDaysBefore == daysBefore
            }
        }.distinctBy { it.plant.id to it.originalDate }
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        treatments.forEach { treatment ->
            val plant = treatment.plant
            val timing = when (plant.reminderDaysBefore) {
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
                treatment.plant.id,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, TreatmentReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$timing — запланированная процедура")
                .setContentText(details)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$timing: $details"))
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
