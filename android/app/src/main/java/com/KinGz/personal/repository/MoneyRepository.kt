package com.KinGz.personal.repository

import com.KinGz.personal.data.Account
import com.KinGz.personal.data.AccountDao
import com.KinGz.personal.data.Debt
import com.KinGz.personal.data.DebtDao
import com.KinGz.personal.data.MoneyDao
import com.KinGz.personal.data.MoneyTransaction
import com.KinGz.personal.data.MoneyTransfer
import com.KinGz.personal.data.MoneyTransferDao
import kotlinx.coroutines.flow.Flow

class MoneyRepository(
    private val moneyDao: MoneyDao,
    private val accountDao: AccountDao,
    private val transferDao: MoneyTransferDao,
    private val debtDao: DebtDao
) {
    val transactions: Flow<List<MoneyTransaction>> =
        moneyDao.getAllTransactions()

    val accounts: Flow<List<Account>> =
        accountDao.getActiveAccounts()

    val transfers: Flow<List<MoneyTransfer>> =
        transferDao.getAllTransfers()

    val debts: Flow<List<Debt>> =
        debtDao.getAllDebts()

    suspend fun insertTransaction(transaction: MoneyTransaction) {
        moneyDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: MoneyTransaction) {
        moneyDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: MoneyTransaction) {
        moneyDao.deleteTransaction(transaction)
    }

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun insertTransfer(transfer: MoneyTransfer) {
        transferDao.insertTransfer(transfer)
    }

    suspend fun updateTransfer(transfer: MoneyTransfer) {
        transferDao.updateTransfer(transfer)
    }

    suspend fun deleteTransfer(transfer: MoneyTransfer) {
        transferDao.deleteTransfer(transfer)
    }

    suspend fun insertDebt(debt: Debt) {
        debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }
}
