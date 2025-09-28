package com.example.uvce_faculty

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class FacultyApp : Application() {

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskReminderChannel = NotificationChannel(
                TASK_REMINDER_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminder notifications"
                // You can set other properties like light color, vibration pattern etc.
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(taskReminderChannel)
        }
    }
}
