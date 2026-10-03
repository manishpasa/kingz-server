package com.KinGz.personal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.KinGz.personal.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != "android.intent.action.TIME_SET" &&
            intent.action != "android.intent.action.TIMEZONE_CHANGED"
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Job() + Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(appContext)
                val tasks = database.taskDao().getAllTasks().first()
                TaskReminderScheduler.rescheduleAll(
                    appContext,
                    tasks
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
