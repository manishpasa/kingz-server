
package com.KinGz.personal.ui.money

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.KinGz.personal.data.Account
import com.KinGz.personal.data.AppDatabase
import com.KinGz.personal.data.Debt
import com.KinGz.personal.data.MoneyTransaction
import com.KinGz.personal.data.MoneyTransfer
import com.KinGz.personal.repository.MoneyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoneyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    private val repository = MoneyRepository(
        moneyDao = database.moneyDao(),
        accountDao = database.accountDao(),
        transferDao = database.moneyTransferDao(),
        debtDao = database.debtDao()
    )

    private val preferences = application.getSharedPreferences(
        "kingz_money_preferences",
        Context.MODE_PRIVATE
    )

    private val _emergencyFundPaisa = MutableStateFlow(
        preferences.getLong("emergency_fund_paisa", 0L).coerceAtLeast(0L)
    )

    val emergencyFundPaisa: StateFlow<Long> = _emergencyFundPaisa

    private val _savingsSourceAccountId = MutableStateFlow(
        preferences.getLong("savings_source_account_id", 0L)
            .takeIf { it > 0L }
    )

    val savingsSourceAccountId: StateFlow<Long?> = _savingsSourceAccountId

    val transactions: StateFlow<List<MoneyTransaction>> =
        repository.transactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val accounts: StateFlow<List<Account>> =
        repository.accounts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val transfers: StateFlow<List<MoneyTransfer>> =
        repository.transfers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val debts: StateFlow<List<Debt>> =
        repository.debts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    companion object {
        private const val DEBT_SOURCE_PREFIX = "__KINGZ_SRC__"
    }

    private fun debtSourceAccountId(note: String): Long? {
        if (!note.startsWith(DEBT_SOURCE_PREFIX)) return null

        val encodedId = note
            .removePrefix(DEBT_SOURCE_PREFIX)
            .substringBefore("|")

        return encodedId.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun debtVisibleNote(note: String): String {
        if (!note.startsWith(DEBT_SOURCE_PREFIX)) return note

        return note.substringAfter("|", "")
    }

    private suspend fun createDebtLedgerTransaction(debt: Debt) {
        if (debt.isSettled) return

        val accountId = debtSourceAccountId(debt.note) ?: return
        if (debt.amountPaisa <= 0L) return

        database.moneyDao().insertTransaction(
            MoneyTransaction(
                accountId = accountId,
                isIncome = !debt.isOwedToMe,
                amountPaisa = debt.amountPaisa,
                category = if (debt.isOwedToMe) {
                    "Debt Given"
                } else {
                    "Debt Borrowed"
                },
                note = debtVisibleNote(debt.note),
                date = debt.date,
                createdAt = debt.createdAt,
                debtId = debt.id
            )
        )
    }

    fun setProtectedSavings(
        amountPaisa: Long,
        sourceAccountId: Long
    ) {
        val safeAmount = amountPaisa.coerceAtLeast(0L)

        if (safeAmount > 0L && sourceAccountId <= 0L) return

        preferences.edit()
            .putLong("emergency_fund_paisa", safeAmount)
            .putLong(
                "savings_source_account_id",
                if (safeAmount > 0L) sourceAccountId else 0L
            )
            .apply()

        _emergencyFundPaisa.value = safeAmount
        _savingsSourceAccountId.value =
            sourceAccountId.takeIf { safeAmount > 0L && it > 0L }
    }

    fun clearProtectedSavings() {
        setProtectedSavings(0L, 0L)
    }

    fun setEmergencyFundPaisa(amountPaisa: Long) {
        setProtectedSavings(
            amountPaisa,
            _savingsSourceAccountId.value ?: 0L
        )
    }

    fun addAccount(
        name: String,
        initialBalancePaisa: Long
    ) {
        if (name.isBlank() || initialBalancePaisa < 0L) return

        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name.trim(),
                    initialBalancePaisa = initialBalancePaisa
                )
            )
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(
        account: Account,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (
                emergencyFundPaisa.value > 0L &&
                savingsSourceAccountId.value == account.id
            ) {
                onResult("This account holds your protected savings. Move or remove the protection first.")
                return@launch
            }

            val currentAccount = accounts.value
                .firstOrNull { it.id == account.id } ?: account

            val currentBalance = balanceForAccount(
                currentAccount,
                transactions.value,
                transfers.value
            )

            if (currentBalance != 0L) {
                onResult("This account must have a zero balance before it can be removed.")
                return@launch
            }

            try {
                val result = database.withTransaction {
                    val transactionCount =
                        database.moneyDao()
                            .countTransactionsForAccount(account.id)

                    val transferCount =
                        database.moneyTransferDao()
                            .countTransfersForAccount(account.id)

                    val debtCount =
                        database.debtDao()
                            .countDebtsForAccount(
                                "${DEBT_SOURCE_PREFIX}${account.id}|"
                            )

                    val hasHistory =
                        transactionCount > 0 ||
                                transferCount > 0 ||
                                debtCount > 0

                    if (hasHistory) {
                        database.accountDao().updateAccount(
                            currentAccount.copy(isArchived = true)
                        )
                        "Account archived. Financial history has been preserved."
                    } else {
                        database.accountDao().deleteAccount(currentAccount)
                        "Account permanently deleted."
                    }
                }

                onResult(result)
            } catch (e: Exception) {
                onResult("Could not remove this account. Please try again.")
            }
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
        if (amountPaisa <= 0L || category.isBlank()) return

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

    fun updateTransaction(transaction: MoneyTransaction) {
        if (transaction.accountId <= 0L) return
        if (transaction.amountPaisa <= 0L) return
        if (transaction.category.isBlank()) return

        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: MoneyTransaction) {
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
        if (fromAccountId == toAccountId || amountPaisa <= 0L) return

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

    fun deleteTransfer(transfer: MoneyTransfer) {
        viewModelScope.launch {
            repository.deleteTransfer(transfer)
        }
    }

    fun updateTransfer(transfer: MoneyTransfer) {
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
        if (person.isBlank() || amountPaisa <= 0L) return

        viewModelScope.launch {
            database.withTransaction {
                val debt = Debt(
                    person = person.trim(),
                    amountPaisa = amountPaisa,
                    isOwedToMe = isOwedToMe,
                    note = note.trim(),
                    date = date
                )

                val debtId = database.debtDao().insertDebt(debt)

                createDebtLedgerTransaction(
                    debt.copy(id = debtId)
                )
            }
        }
    }

    fun updateDebt(debt: Debt) {
        if (debt.id <= 0L) return
        if (debt.person.isBlank() || debt.amountPaisa <= 0L) return

        viewModelScope.launch {
            database.withTransaction {
                database.moneyDao().deleteTransactionsForDebt(debt.id)

                database.debtDao().updateDebt(debt)

                createDebtLedgerTransaction(debt)
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            database.withTransaction {
                database.moneyDao().deleteTransactionsForDebt(debt.id)
                database.debtDao().deleteDebt(debt)
            }
        }
    }

    fun balanceForAccount(
        account: Account,
        transactions: List<MoneyTransaction>,
        transfers: List<MoneyTransfer>
    ): Long {

        var balance = account.initialBalancePaisa

        transactions
            .filter { it.accountId == account.id }
            .forEach { transaction ->
                balance += if (transaction.isIncome) {
                    transaction.amountPaisa
                } else {
                    -transaction.amountPaisa
                }
            }

        transfers
            .filter { it.fromAccountId == account.id }
            .forEach { balance -= it.amountPaisa }

        transfers
            .filter { it.toAccountId == account.id }
            .forEach { balance += it.amountPaisa }

        return balance
    }
}
