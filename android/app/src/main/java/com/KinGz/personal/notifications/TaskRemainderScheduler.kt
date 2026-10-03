package com.KinGz.personal.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.KinGz.personal.data.Task

object TaskReminderScheduler {

    const val ACTION_REMINDER = "com.KinGz.personal.ACTION_TASK_REMINDER"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_REMINDER_TYPE = "reminder_type"
    const val TYPE_DAY_BEFORE = 1
    const val TYPE_FINAL_HOUR = 2

    const val CHANNEL_ID = "task_reminders"

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    fun schedule(context: Context, task: Task) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancel(context, task.id)

        if (task.isCompleted || task.dueDate == null) return

        val dueDate = task.dueDate
        val now = System.currentTimeMillis()

        scheduleReminder(
            context = context,
            alarmManager = alarmManager,
            task = task,
            triggerAtMillis = dueDate - DAY_MILLIS,
            type = TYPE_DAY_BEFORE,
            now = now
        )

        scheduleReminder(
            context = context,
            alarmManager = alarmManager,
            task = task,
            triggerAtMillis = dueDate - HOUR_MILLIS,
            type = TYPE_FINAL_HOUR,
            now = now
        )
    }

    fun cancel(context: Context, taskId: Int) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelOne(context, alarmManager, taskId, TYPE_DAY_BEFORE)
        cancelOne(context, alarmManager, taskId, TYPE_FINAL_HOUR)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(notificationId(taskId, TYPE_DAY_BEFORE))
        notificationManager.cancel(notificationId(taskId, TYPE_FINAL_HOUR))
    }

    fun rescheduleAll(context: Context, tasks: List<Task>) {
        tasks.forEach { task ->
            if (task.isCompleted || task.dueDate == null) {
                cancel(context, task.id)
            } else {
                schedule(context, task)
            }
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for scheduled KinGz tasks"
        }

        manager.createNotificationChannel(channel)
    }

    fun requestCode(taskId: Int, type: Int): Int {
        return taskId * 10 + type
    }

    fun notificationId(taskId: Int, type: Int): Int {
        return requestCode(taskId, type)
    }

    fun reminderTitle(type: Int): String {
        return when (type) {
            TYPE_DAY_BEFORE -> "Task due tomorrow"
            TYPE_FINAL_HOUR -> "Task due in 1 hour"
            else -> "Task reminder"
        }
    }

    private fun scheduleReminder(
        context: Context,
        alarmManager: AlarmManager,
        task: Task,
        triggerAtMillis: Long,
        type: Int,
        now: Long
    ) {
        if (triggerAtMillis <= now) return

        ensureNotificationChannel(context)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_REMINDER_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(task.id, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelOne(
        context: Context,
        alarmManager: AlarmManager,
        taskId: Int,
        type: Int
    ) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_REMINDER_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(taskId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
