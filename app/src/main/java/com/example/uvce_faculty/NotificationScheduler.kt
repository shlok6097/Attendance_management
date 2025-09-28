package com.example.uvce_faculty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar

object NotificationScheduler {

    fun scheduleTaskReminder(context: Context, taskId: String, taskTitle: String, taskDescription: String?, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskNotificationReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskNotificationReceiver.EXTRA_TASK_DESCRIPTION, taskDescription)
        }

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(), // Use the same request code for scheduling and cancelling
            intent,
            pendingIntentFlags
        )

        // Check for exact alarm permission before scheduling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Permission to schedule exact alarms is not granted.", Toast.LENGTH_LONG).show()
            // Optionally, guide the user to settings: Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            // For testing, you can show a toast:
            // Toast.makeText(context, "Reminder scheduled for $taskTitle at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(triggerTimeMillis)}", Toast.LENGTH_LONG).show()
        } catch (se: SecurityException) {
            Toast.makeText(context, "SecurityException: Could not schedule exact alarm.", Toast.LENGTH_LONG).show()
            // Handle the case where permission might still be an issue despite the check
        }
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        // Recreate the same PendingIntent used for scheduling

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Use FLAG_NO_CREATE to check if it exists
            } else {
                PendingIntent.FLAG_NO_CREATE
            }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            pendingIntentFlags
        )

        if (pendingIntent != null) { // Only cancel if the PendingIntent exists
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            // Toast.makeText(context, "Reminder cancelled for task ID: $taskId", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Calculates the trigger time in milliseconds based on the due date and reminder option.
     * This is a helper function you'll use in your Fragment/ViewModel.
     */
    fun calculateTriggerTime(dueDateTimeMillis: Long, reminderOption: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dueDateTimeMillis

        when (reminderOption) {
            "At time of event" -> { /* Do nothing, trigger time is dueDateTimeMillis */ }
            "5 minutes before" -> calendar.add(Calendar.MINUTE, -5)
            "15 minutes before" -> calendar.add(Calendar.MINUTE, -15)
            "30 minutes before" -> calendar.add(Calendar.MINUTE, -30)
            "1 hour before" -> calendar.add(Calendar.HOUR_OF_DAY, -1)
            "2 hours before" -> calendar.add(Calendar.HOUR_OF_DAY, -2)
            "1 day before" -> calendar.add(Calendar.DAY_OF_MONTH, -1)
            "No reminder" -> return -1 // Indicate no reminder
            else -> return dueDateTimeMillis // Default or unknown, trigger at due time
        }
        return calendar.timeInMillis
    }
}
