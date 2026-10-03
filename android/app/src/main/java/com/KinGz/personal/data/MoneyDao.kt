package com.KinGz.personal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {

    @Query("SELECT * FROM money_transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<MoneyTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: MoneyTransaction)

    @Update
    suspend fun updateTransaction(transaction: MoneyTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: MoneyTransaction)
}
