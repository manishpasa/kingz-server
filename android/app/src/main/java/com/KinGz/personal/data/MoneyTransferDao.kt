package com.KinGz.personal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyTransferDao {

    @Query("SELECT * FROM money_transfers ORDER BY date DESC, createdAt DESC")
    fun getAllTransfers(): Flow<List<MoneyTransfer>>

    @Insert
    suspend fun insertTransfer(transfer: MoneyTransfer)

    @Update
    suspend fun updateTransfer(transfer: MoneyTransfer)

    @Delete
    suspend fun deleteTransfer(transfer: MoneyTransfer)
}
