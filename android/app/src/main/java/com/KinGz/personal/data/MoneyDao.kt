
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
    suspend fun insertTransaction(transaction: MoneyTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: MoneyTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: MoneyTransaction)

    @Query("SELECT * FROM money_transactions WHERE debtId = :debtId")
    suspend fun getTransactionsForDebt(debtId: Long): List<MoneyTransaction>

    @Query("DELETE FROM money_transactions WHERE debtId = :debtId")
    suspend fun deleteTransactionsForDebt(debtId: Long)

    @Query("SELECT COUNT(*) FROM money_transactions WHERE accountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Long): Int
}
