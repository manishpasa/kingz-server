package com.KinGz.personal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_transfers")
data class MoneyTransfer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountPaisa: Long,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
