package com.KinGz.personal.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.KinGz.personal.data.AppDatabase
import com.KinGz.personal.data.Task
import com.KinGz.personal.notifications.TaskReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val database =
        AppDatabase.getDatabase(application)

    private val taskDao = database.taskDao()

    val tasks: StateFlow<List<Task>> =
        taskDao.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val existingTasks = taskDao.getAllTasks().first()
            TaskReminderScheduler.rescheduleAll(
                appContext,
                existingTasks
            )
        }
    }

    fun addTask(
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                dueDate = dueDate
            )

            val id = taskDao.insertTask(task)
            val savedTask = task.copy(id = id.toInt())

            TaskReminderScheduler.schedule(
                appContext,
                savedTask
            )
        }
    }

    fun updateTask(task: Task) {
        if (task.id <= 0) return
        if (task.title.isBlank()) return

        viewModelScope.launch {
            taskDao.updateTask(
                task.copy(
                    title = task.title.trim(),
                    description = task.description.trim()
                )
            )

            TaskReminderScheduler.schedule(
                appContext,
                task
            )
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted
            )

            taskDao.updateTask(updatedTask)

            if (updatedTask.isCompleted) {
                TaskReminderScheduler.cancel(
                    appContext,
                    updatedTask.id
                )
            } else {
                TaskReminderScheduler.schedule(
                    appContext,
                    updatedTask
                )
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            TaskReminderScheduler.cancel(
                appContext,
                task.id
            )
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val existingTasks = taskDao.getAllTasks().first()
            taskDao.deleteAllTasks()

            existingTasks.forEach { task ->
                TaskReminderScheduler.cancel(
                    appContext,
                    task.id
                )
            }
        }
    }
}
