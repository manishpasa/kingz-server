package com.KinGz.personal.ui.money

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.KinGz.personal.data.Account
import com.KinGz.personal.data.AppDatabase
import com.KinGz.personal.data.Debt
import com.KinGz.personal.data.MoneyTransaction
import com.KinGz.personal.data.MoneyTransfer
import com.KinGz.personal.repository.MoneyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoneyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        AppDatabase.getDatabase(application)

    private val repository =
        MoneyRepository(
            moneyDao = database.moneyDao(),
            accountDao = database.accountDao(),
            transferDao = database.moneyTransferDao(),
            debtDao = database.debtDao()
        )

    val transactions: StateFlow<List<MoneyTransaction>> =
        repository.transactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<Account>> =
        repository.accounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val transfers: StateFlow<List<MoneyTransfer>> =
        repository.transfers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val debts: StateFlow<List<Debt>> =
        repository.debts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addAccount(
        name: String,
        initialBalancePaisa: Long
    ) {
        if (name.isBlank()) return
        if (initialBalancePaisa < 0L) return

        viewModelScope.launch {

            repository.insertAccount(
                Account(
                    name = name.trim(),
                    initialBalancePaisa = initialBalancePaisa
                )
            )
        }
    }

    fun updateAccount(
        account: Account
    ) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun addTransaction(
        accountId: Long,
        isIncome: Boolean,
        amountPaisa: Long,
        category: String,
        note: String,
        date: Long
    ) {
        if (accountId <= 0L) return
        if (amountPaisa <= 0L) return
        if (category.isBlank()) return

        viewModelScope.launch {

            repository.insertTransaction(
                MoneyTransaction(
                    accountId = accountId,
                    isIncome = isIncome,
                    amountPaisa = amountPaisa,
                    category = category.trim(),
                    note = note.trim(),
                    date = date
                )
            )
        }
    }

    fun updateTransaction(
        transaction: MoneyTransaction
    ) {
        if (transaction.amountPaisa <= 0L) return
        if (transaction.category.isBlank()) return

        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(
        transaction: MoneyTransaction
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountPaisa: Long,
        note: String,
        date: Long
    ) {
        if (fromAccountId <= 0L || toAccountId <= 0L) return
        if (fromAccountId == toAccountId) return
        if (amountPaisa <= 0L) return

        viewModelScope.launch {

            repository.insertTransfer(
                MoneyTransfer(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amountPaisa = amountPaisa,
                    note = note.trim(),
                    date = date
                )
            )
        }
    }

    fun deleteTransfer(
        transfer: MoneyTransfer
    ) {
        viewModelScope.launch {
            repository.deleteTransfer(transfer)
        }
    }

    fun updateTransfer(
        transfer: MoneyTransfer
    ) {
        if (transfer.fromAccountId <= 0L || transfer.toAccountId <= 0L) return
        if (transfer.fromAccountId == transfer.toAccountId) return
        if (transfer.amountPaisa <= 0L) return

        viewModelScope.launch {
            repository.updateTransfer(transfer)
        }
    }


    fun addDebt(
        person: String,
        amountPaisa: Long,
        isOwedToMe: Boolean,
        note: String,
        date: Long
    ) {
        if (person.isBlank()) return
        if (amountPaisa <= 0L) return

        viewModelScope.launch {

            repository.insertDebt(
                Debt(
                    person = person.trim(),
                    amountPaisa = amountPaisa,
                    isOwedToMe = isOwedToMe,
                    note = note.trim(),
                    date = date
                )
            )
        }
    }

    fun updateDebt(
        debt: Debt
    ) {
        viewModelScope.launch {
            repository.updateDebt(debt)
        }
    }

    fun deleteDebt(
        debt: Debt
    ) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun balanceForAccount(
        account: Account,
        transactions: List<MoneyTransaction>,
        transfers: List<MoneyTransfer>
    ): Long {

        var balance =
            account.initialBalancePaisa

        transactions
            .filter {
                it.accountId == account.id
            }
            .forEach { transaction ->

                balance += if (transaction.isIncome) {
                    transaction.amountPaisa
                } else {
                    -transaction.amountPaisa
                }
            }

        transfers
            .filter {
                it.fromAccountId == account.id
            }
            .forEach {
                balance -= it.amountPaisa
            }

        transfers
            .filter {
                it.toAccountId == account.id
            }
            .forEach {
                balance += it.amountPaisa
            }

        return balance
    }
}
