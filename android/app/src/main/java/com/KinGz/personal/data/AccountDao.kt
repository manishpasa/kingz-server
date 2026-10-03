package com.KinGz.personal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM money_accounts WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveAccounts(): Flow<List<Account>>

    @Insert
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)
}
