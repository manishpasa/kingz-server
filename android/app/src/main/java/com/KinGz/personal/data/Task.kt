package com.KinGz.personal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val description: String = "",

    val isCompleted: Boolean = false,

    val priority: Int = 0,

    val dueDate: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
)