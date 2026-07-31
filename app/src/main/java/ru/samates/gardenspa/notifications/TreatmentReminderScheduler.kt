package ru.samates.gardenspa.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Calendar

object TreatmentReminderScheduler {
    const val CHANNEL_ID = "planned_treatments"
    private const val DAILY_REQUEST_CODE = 4100
    private const val PREFS_NAME = "treatment_reminders"
    private const val LAST_CHECK_DATE = "last_check_date"

    fun schedule(context: Context) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, TreatmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val firstRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstRun,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun refreshNow(context: Context) {
        context.sendBroadcast(Intent(context, TreatmentReminderReceiver::class.java))
    }

    fun refreshOnceToday(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val today = LocalDate.now().toString()
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getString(LAST_CHECK_DATE, null) == today) return
        markCheckedToday(context)
        refreshNow(context)
    }

    fun markCheckedToday(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LAST_CHECK_DATE, LocalDate.now().toString())
            .apply()
    }

    fun cancelTreatmentNotification(context: Context, plantId: Int, originalDate: String) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(plantId, originalDate))
    }

    fun notificationId(plantId: Int, originalDate: String): Int =
        "treatment:$plantId:$originalDate".hashCode()

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Запланированные процедуры",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о процедурах в выбранный пользователем срок"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
