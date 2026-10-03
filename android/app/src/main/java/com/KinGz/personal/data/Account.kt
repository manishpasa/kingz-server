package com.KinGz.personal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val initialBalancePaisa: Long = 0L,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
