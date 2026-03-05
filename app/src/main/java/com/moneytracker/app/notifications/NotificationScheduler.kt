package com.moneytracker.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationScheduler {
    
    private const val ALARM_REQUEST_CODE = 7000

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set for 7:00 PM today
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19) // 19 = 7 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // If it's already past 7 PM today, schedule for tomorrow instead
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        // Using setInexactRepeating is better for battery life and doesn't require Android 14's strict exact alarm permissions
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}