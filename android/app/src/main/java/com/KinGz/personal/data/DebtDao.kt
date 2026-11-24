
package com.KinGz.personal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("""
        SELECT * FROM money_debts
        ORDER BY isSettled ASC, date DESC, createdAt DESC
    """)
    fun getAllDebts(): Flow<List<Debt>>

    @Insert
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("SELECT * FROM money_debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): Debt?

    @Query("""
        SELECT COUNT(*) FROM money_debts
        WHERE substr(note, 1, length(:prefix)) = :prefix
    """)
    suspend fun countDebtsForAccount(prefix: String): Int
}
