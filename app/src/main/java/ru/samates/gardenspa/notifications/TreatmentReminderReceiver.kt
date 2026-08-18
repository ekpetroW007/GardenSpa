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
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.R
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.nextTreatmentReminderAlarms
import ru.samates.gardenspa.domain.reminderOffsetLabel
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.others.MainActivity

class TreatmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) TreatmentReminderScheduler.schedule(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent?.action == TreatmentReminderScheduler.ACTION_FIRE) showReminder(context.applicationContext, intent) else refreshAlarms(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun refreshAlarms(context: Context) {
        val application = context as BookeeperApp
        val alarms = nextTreatmentReminderAlarms(application.repository.getAllPlantsOnce(), application.repository.getAllProceduresOnce())
        TreatmentReminderScheduler.replaceReminderAlarms(context, alarms)
        TreatmentReminderScheduler.markCheckedToday(context)
    }

    private suspend fun showReminder(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val plantId = intent.getIntExtra(TreatmentReminderScheduler.EXTRA_PLANT_ID, -1)
        val originalDate = intent.getStringExtra(TreatmentReminderScheduler.EXTRA_ORIGINAL_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val scheduledDate = intent.getStringExtra(TreatmentReminderScheduler.EXTRA_SCHEDULED_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val offsetMinutes = intent.getIntExtra(TreatmentReminderScheduler.EXTRA_OFFSET_MINUTES, 0)
        val application = context as BookeeperApp
        val treatment = scheduledTreatmentsOn(application.repository.getAllPlantsOnce(), application.repository.getAllProceduresOnce(), scheduledDate)
            .firstOrNull { it.plant.id == plantId && it.originalDate == originalDate && !it.completed } ?: return
        val plant = treatment.plant
        val timing = if (offsetMinutes == 0) "Сейчас" else reminderOffsetLabel(offsetMinutes).replaceFirst("За ", "Через ")
        val details = buildString {
            append(plant.taskName)
            append(" — ")
            append(plant.plantName)
            if (plant.gardenName.isNotBlank()) append(", ${plant.gardenName}")
            val drugName = plant.drugName.toDrugDisplayName()
            if (drugName.isNotBlank()) append(if (drugName == NO_DRUG_REQUIRED_LABEL) ", $drugName" else ", препарат: $drugName")
        }
        val openApp = PendingIntent.getActivity(context, plant.id, Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, TreatmentReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$timing — запланированная процедура")
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$timing: $details"))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(TreatmentReminderScheduler.notificationId(plant.id, originalDate.toString()), notification)
    }
}
