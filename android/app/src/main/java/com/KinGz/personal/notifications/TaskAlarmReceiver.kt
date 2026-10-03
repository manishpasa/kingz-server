package com.KinGz.personal.notifications

import android.app.PendingIntent
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.KinGz.personal.MainActivity
import com.KinGz.personal.data.AppDatabase
import com.KinGz.personal.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Job() + Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(appContext)
                val taskDao = database.taskDao()
                val taskId = intent.getIntExtra(
                    TaskReminderScheduler.EXTRA_TASK_ID,
                    -1
                )

                if (taskId <= 0) return@launch

                if (intent.action == ACTION_COMPLETE) {
                    val task = taskDao.getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        taskDao.updateTask(task.copy(isCompleted = true))
                    }

                    TaskReminderScheduler.cancel(
                        appContext,
                        taskId
                    )
                    return@launch
                }

                val task = taskDao.getTaskById(taskId)
                    ?: return@launch

                if (task.isCompleted || task.dueDate == null) {
                    TaskReminderScheduler.cancel(
                        appContext,
                        taskId
                    )
                    return@launch
                }

                showNotification(
                    appContext,
                    task,
                    intent.getIntExtra(
                        TaskReminderScheduler.EXTRA_REMINDER_TYPE,
                        TaskReminderScheduler.TYPE_FINAL_HOUR
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        task: Task,
        type: Int
    ) {
        TaskReminderScheduler.ensureNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", task.id)
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            TaskReminderScheduler.notificationId(task.id, type),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(
            context,
            TaskAlarmReceiver::class.java
        ).apply {
            action = ACTION_COMPLETE
            putExtra(
                TaskReminderScheduler.EXTRA_TASK_ID,
                task.id
            )
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            completeActionRequestCode(task.id),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            TaskReminderScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(
                TaskReminderScheduler.reminderTitle(type)
            )
            .setContentText(task.title)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildMessage(task, type)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Mark complete",
                completePendingIntent
            )

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.notify(
            TaskReminderScheduler.notificationId(task.id, type),
            builder.build()
        )
    }

    private fun buildMessage(
        task: Task,
        type: Int
    ): String {
        val dueText = android.text.format.DateFormat.format(
            "MMM d, h:mm a",
            task.dueDate ?: System.currentTimeMillis()
        )

        return when (type) {
            TaskReminderScheduler.TYPE_DAY_BEFORE ->
                "${task.title} is due tomorrow at $dueText."

            TaskReminderScheduler.TYPE_FINAL_HOUR ->
                "${task.title} is due at $dueText."

            else ->
                "${task.title} has a scheduled deadline at $dueText."
        }
    }

    private fun completeActionRequestCode(taskId: Int): Int {
        return taskId * 10 + 3
    }

    companion object {
        const val ACTION_COMPLETE = "com.KinGz.personal.ACTION_COMPLETE_TASK"
    }
}
