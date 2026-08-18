package ru.samates.gardenspa.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Calendar
import ru.samates.gardenspa.domain.TreatmentReminderAlarm

object TreatmentReminderScheduler {
    const val CHANNEL_ID = "planned_treatments"
    const val ACTION_REFRESH = "ru.samates.gardenspa.REFRESH_TREATMENT_REMINDERS"
    const val ACTION_FIRE = "ru.samates.gardenspa.FIRE_TREATMENT_REMINDER"
    const val EXTRA_PLANT_ID = "plant_id"
    const val EXTRA_ORIGINAL_DATE = "original_date"
    const val EXTRA_SCHEDULED_DATE = "scheduled_date"
    const val EXTRA_OFFSET_MINUTES = "offset_minutes"
    private const val DAILY_REQUEST_CODE = 4100
    private const val PREFS_NAME = "treatment_reminders"
    private const val LAST_CHECK_DATE = "last_check_date"
    private const val SCHEDULED_ALARM_IDS = "scheduled_alarm_ids"

    fun schedule(context: Context) {
        createNotificationChannel(context)
        val intent = Intent(context, TreatmentReminderReceiver::class.java).setAction(ACTION_REFRESH)
        val pendingIntent = PendingIntent.getBroadcast(context, DAILY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val firstRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        context.getSystemService(AlarmManager::class.java).setInexactRepeating(AlarmManager.RTC_WAKEUP, firstRun, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    fun replaceReminderAlarms(context: Context, alarms: List<TreatmentReminderAlarm>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.getStringSet(SCHEDULED_ALARM_IDS, emptySet()).orEmpty().forEach { storedId ->
            val requestCode = storedId.toIntOrNull() ?: return@forEach
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, Intent(context, TreatmentReminderReceiver::class.java).setAction(ACTION_FIRE), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            pendingIntent?.let(alarmManager::cancel)
        }
        val ids = alarms.map { alarm ->
            val requestCode = alarmRequestCode(alarm)
            val intent = Intent(context, TreatmentReminderReceiver::class.java).setAction(ACTION_FIRE)
                .putExtra(EXTRA_PLANT_ID, alarm.plantId)
                .putExtra(EXTRA_ORIGINAL_DATE, alarm.originalDate.toString())
                .putExtra(EXTRA_SCHEDULED_DATE, alarm.scheduledDate.toString())
                .putExtra(EXTRA_OFFSET_MINUTES, alarm.offsetMinutes)
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt.toInstant().toEpochMilli(), pendingIntent)
            requestCode.toString()
        }.toSet()
        preferences.edit().putStringSet(SCHEDULED_ALARM_IDS, ids).apply()
    }

    fun refreshNow(context: Context) {
        context.sendBroadcast(Intent(context, TreatmentReminderReceiver::class.java).setAction(ACTION_REFRESH))
    }

    fun refreshOnceToday(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getString(LAST_CHECK_DATE, null) == LocalDate.now().toString()) return
        refreshNow(context)
    }

    fun markCheckedToday(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(LAST_CHECK_DATE, LocalDate.now().toString()).apply()
    }

    fun cancelTreatmentNotification(context: Context, plantId: Int, originalDate: String) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(plantId, originalDate))
    }

    fun notificationId(plantId: Int, originalDate: String): Int = "treatment:$plantId:$originalDate".hashCode()

    private fun alarmRequestCode(alarm: TreatmentReminderAlarm): Int = "alarm:${alarm.plantId}:${alarm.originalDate}:${alarm.offsetMinutes}".hashCode()

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Запланированные процедуры", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Напоминания о процедурах в выбранный пользователем срок"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
