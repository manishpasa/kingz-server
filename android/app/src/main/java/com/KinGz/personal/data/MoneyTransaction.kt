package com.KinGz.personal.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_transactions")
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "1")
    val accountId: Long = 1L,
    val isIncome: Boolean,
    val amountPaisa: Long,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
