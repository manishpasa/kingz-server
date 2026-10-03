package com.KinGz.personal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val person: String,
    val amountPaisa: Long,
    val isOwedToMe: Boolean,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
