package com.example.uvce_faculty

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TaskNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        // You might want to pass more task details if needed for the notification or pending intent
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: System.currentTimeMillis().toString() // Use a fallback or ensure taskId is always passed
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: "Don\'t forget your task!"

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager

        // Intent to launch the app when notification is tapped
        // This should ideally open the specific task or a relevant screen
        val launchIntent = Intent(context, MainActivity::class.java).apply { // Assuming MainActivity is your main entry point
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // You can add extras to the launchIntent to navigate to a specific task if needed
            // putExtra("NAVIGATE_TO_TASK_ID", taskId)
        }

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(), // Use taskId's hashcode for a unique request code
            launchIntent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, FacultyApp.TASK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book) // TODO: Replace with your actual notification icon
            .setContentTitle(taskTitle)
            .setContentText(taskDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .build()

        notificationManager.notify(taskId.hashCode(), notification) // Use taskId's hashcode for a unique notification ID
    }
}
