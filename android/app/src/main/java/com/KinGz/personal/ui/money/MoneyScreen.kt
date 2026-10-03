package com.KinGz.personal.ui.money
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.KinGz.personal.data.Account
import com.KinGz.personal.data.Debt
import com.KinGz.personal.data.MoneyTransaction
import com.KinGz.personal.data.MoneyTransfer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE
}

private val defaultCategories = listOf(
    "Food",
    "Transport",
    "Bills",
    "Shopping",
    "Education",
    "Entertainment",
    "Health",
    "Salary",
    "Freelance",
    "Gift",
    "Rent",
    "Investment",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val debts by viewModel.debts.collectAsState()

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var showAccounts by remember { mutableStateOf(false) }
    var showDebts by remember { mutableStateOf(false) }
    var showTransfers by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var transactionFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var showActionDialog by remember { mutableStateOf(false) }

    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }

    var showTransferDialog by remember { mutableStateOf(false) }
    var editingTransfer by remember { mutableStateOf<MoneyTransfer?>(null) }
    var deletingTransfer by remember { mutableStateOf<MoneyTransfer?>(null) }

    var showDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var deletingDebt by remember { mutableStateOf<Debt?>(null) }
    var settlingDebt by remember { mutableStateOf<Debt?>(null) }

    val accountBalances = remember(accounts, transactions, transfers) {
        accounts.associate { account ->
            account.id to viewModel.balanceForAccount(
                account,
                transactions,
                transfers
            )
        }
    }

    val totalBalance = accountBalances.values.sum()
    val accountScopedTransactions = transactions.filter {
        selectedAccountId == null || it.accountId == selectedAccountId
    }

    val now = System.currentTimeMillis()
    val monthStart = startOfMonth(now)
    val todayStart = startOfDay(now)

    val monthTransactions = accountScopedTransactions.filter {
        it.date >= monthStart
    }

    val todayTransactions = accountScopedTransactions.filter {
        it.date >= todayStart
    }

    val monthIncome = monthTransactions
        .filter { it.isIncome }
        .sumOf { it.amountPaisa }

    val monthExpense = monthTransactions
        .filter { !it.isIncome }
        .sumOf { it.amountPaisa }

    val monthNet = monthIncome - monthExpense

    val todayIncome = todayTransactions
        .filter { it.isIncome }
        .sumOf { it.amountPaisa }

    val todayExpense = todayTransactions
        .filter { !it.isIncome }
        .sumOf { it.amountPaisa }

    val customCategories = transactions
        .map { it.category.trim() }
        .filter { it.isNotBlank() }

    val availableCategories = remember(customCategories) {
        (defaultCategories + customCategories)
            .distinct()
            .sorted()
    }

    val visibleTransactions = accountScopedTransactions
        .filter { transaction ->
            when (transactionFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> transaction.isIncome
                TransactionFilter.EXPENSE -> !transaction.isIncome
            }
        }
        .filter { transaction ->
            selectedCategory == null ||
                    transaction.category.equals(
                        selectedCategory,
                        ignoreCase = true
                    )
        }
        .filter { transaction ->
            if (searchQuery.isBlank()) {
                true
            } else {
                val query = searchQuery.trim().lowercase()
                val accountName = accounts.firstOrNull {
                    it.id == transaction.accountId
                }?.name.orEmpty()

                transaction.category.lowercase().contains(query) ||
                        transaction.note.lowercase().contains(query) ||
                        accountName.lowercase().contains(query) ||
                        formatDate(transaction.date)
                            .lowercase()
                            .contains(query)
            }
        }
        .sortedByDescending { it.date }

    val groupedTransactions = visibleTransactions.groupBy {
        monthLabel(it.date)
    }

    val openDebts = debts
        .filter { !it.isSettled }
        .sortedByDescending { it.date }

    val owedToMe = openDebts
        .filter { it.isOwedToMe }
        .sumOf { it.amountPaisa }

    val iOwe = openDebts
        .filter { !it.isOwedToMe }
        .sumOf { it.amountPaisa }

    val netPeopleBalance = owedToMe - iOwe
    val personalTotalBalance = totalBalance + netPeopleBalance
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = 8.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Money",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${transactions.size} transactions • ${accounts.size} accounts",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(
                    onClick = {
                        searchQuery = ""
                        selectedCategory = null
                        transactionFilter = TransactionFilter.ALL
                        selectedAccountId = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Clear filters"
                    )
                }
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { showActionDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add money item"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                BalanceCardV3(
                    displayedBalance =
                        if (selectedAccountId == null) {
                            personalTotalBalance
                        } else {
                            accountBalances[selectedAccountId] ?: 0L
                        },
                    selectedAccount = accounts.firstOrNull {
                        it.id == selectedAccountId
                    },
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    monthNet = monthNet,
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    accountTotalBalance = totalBalance,
                    owedToMe = owedToMe,
                    iOwe = iOwe,
                    onToggleAccounts = {
                        showAccounts = !showAccounts
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search transactions") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Text("×")
                            }
                        }
                    }
                )
            }

            item {
                FilterRow(
                    selectedFilter = transactionFilter,
                    onFilterChange = { transactionFilter = it },
                    selectedCategory = selectedCategory,
                    categories = availableCategories,
                    onCategoryChange = { selectedCategory = it }
                )
            }

            if (showAccounts) {
                item {
                    SectionHeaderV3(
                        title = "Accounts",
                        actionText = if (showAccounts) "Hide" else "Show",
                        onAction = {
                            showAccounts = !showAccounts
                        }
                    )

                }
                if (showAccounts) {

                    item {
                        OutlinedButton(
                            onClick = {
                                editingAccount = null
                                showAccountDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text("Add account")
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                selectedAccountId = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (selectedAccountId == null) {
                                    "✓ All accounts"
                                } else {
                                    "Show all accounts"
                                }
                            )
                        }
                    }

                    itemsIndexed(
                        items = accounts,
                        key = { index, account ->
                            "account_${account.id}_$index"
                        }
                    ) { _, account ->
                        AccountCardV3(
                            account = account,
                            balance = accountBalances[account.id]
                                ?: account.initialBalancePaisa,
                            selected = selectedAccountId == account.id,
                            transactionCount = transactions.count {
                                it.accountId == account.id
                            },
                            onSelect = {
                                selectedAccountId = account.id
                            },
                            onEdit = {
                                editingAccount = account
                                showAccountDialog = true
                            }
                        )
                    }
                }

            }

            item {
                SectionHeaderV3(
                    title = "People / Owes",
                    actionText = if (showDebts) "Hide" else "Show",
                    onAction = {
                        showDebts = !showDebts
                    }
                )
            }

            if (showDebts) {
                item {
                    DebtSummaryCardV3(
                        iOwe = iOwe,
                        owedToMe = owedToMe,
                        netPeopleBalance = netPeopleBalance,
                        onAdd = {
                            editingDebt = null
                            showDebtDialog = true
                        }
                    )
                }

                items(
                    items = openDebts,
                    key = { "debt_${it.id}" }
                ) { debt ->
                    DebtCardV3(
                        debt = debt,
                        onEdit = {
                            editingDebt = debt
                            showDebtDialog = true
                        },
                        onDelete = {
                            deletingDebt = debt
                        },
                        onSettle = {
                            settlingDebt = debt
                        }
                    )
                }
            }

            item {
                SectionHeaderV3(
                    title = "Transactions",
                    actionText = if (selectedAccountId != null) {
                        "Clear account"
                    } else {
                        ""
                    },
                    onAction = { selectedAccountId = null }
                )
            }

            if (visibleTransactions.isEmpty()) {
                item {
                    EmptyFilteredMoneyState(
                        hasAnyTransactions = transactions.isNotEmpty(),
                        onClear = {
                            searchQuery = ""
                            selectedCategory = null
                            transactionFilter = TransactionFilter.ALL
                            selectedAccountId = null
                        }
                    )
                }
            } else {
                groupedTransactions.forEach { (month, monthItems) ->
                    item {
                        MonthHeader(
                            month = month,
                            count = monthItems.size
                        )
                    }

                    items(
                        items = monthItems,
                        key = { "transaction_${it.id}" }
                    ) { transaction ->
                        TransactionCardV3(
                            transaction = transaction,
                            accountName = accounts.firstOrNull {
                                it.id == transaction.accountId
                            }?.name ?: "Unknown account",
                            onEdit = {
                                editingTransaction = transaction
                                showTransactionDialog = true
                            },
                            onDelete = {
                                deletingTransaction = transaction
                            }
                        )
                    }
                }
            }

            if (transfers.isNotEmpty()) {
                item {
                    SectionHeaderV3(
                        title = "Transfers",
                        actionText = if (showTransfers) "Hide" else "Show",
                        onAction = { showTransfers = !showTransfers }
                    )
                }

                if (showTransfers) {
                    items(
                        items = transfers
                            .sortedByDescending { it.date }
                            .take(10),
                        key = { "transfer_${it.id}" }
                    ) { transfer ->
                        TransferCardV3(
                            transfer = transfer,
                            fromName = accounts.firstOrNull {
                                it.id == transfer.fromAccountId
                            }?.name ?: "Unknown",
                            toName = accounts.firstOrNull {
                                it.id == transfer.toAccountId
                            }?.name ?: "Unknown",
                            onEdit = {
                                editingTransfer = transfer
                                showTransferDialog = true
                            },
                            onDelete = {
                                deletingTransfer = transfer
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    if (showActionDialog) {
        ActionChoiceDialogV3(
            onDismiss = { showActionDialog = false },
            onTransaction = {
                showActionDialog = false
                editingTransaction = null
                showTransactionDialog = true
            },
            onTransfer = {
                showActionDialog = false
                editingTransfer = null
                showTransferDialog = true
            },
            onDebt = {
                showActionDialog = false
                editingDebt = null
                showDebtDialog = true
            },
            onAccount = {
                showActionDialog = false
                editingAccount = null
                showAccountDialog = true
            }
        )
    }

    if (showAccountDialog) {
        AccountDialogV3(
            initial = editingAccount,
            onDismiss = {
                showAccountDialog = false
                editingAccount = null
            },
            onSave = { account, isNew ->
                if (isNew) {
                    viewModel.addAccount(
                        name = account.name,
                        initialBalancePaisa = account.initialBalancePaisa
                    )
                } else {
                    viewModel.updateAccount(account)
                }
                showAccountDialog = false
                editingAccount = null
            }
        )
    }

    if (showTransactionDialog) {
        TransactionDialogV3(
            accounts = accounts,
            categories = availableCategories,
            initial = editingTransaction,
            preferredAccountId =
                selectedAccountId ?: accounts.firstOrNull()?.id,
            onDismiss = {
                showTransactionDialog = false
                editingTransaction = null
            },
            onSave = { transaction ->
                if (editingTransaction == null) {
                    viewModel.addTransaction(
                        accountId = transaction.accountId,
                        isIncome = transaction.isIncome,
                        amountPaisa = transaction.amountPaisa,
                        category = transaction.category,
                        note = transaction.note,
                        date = transaction.date
                    )
                } else {
                    viewModel.updateTransaction(transaction)
                }
                showTransactionDialog = false
                editingTransaction = null
            }
        )
    }

    if (showTransferDialog) {
        TransferDialogV3(
            accounts = accounts,
            initial = editingTransfer,
            onDismiss = {
                showTransferDialog = false
                editingTransfer = null
            },
            onSave = { transfer ->
                if (editingTransfer == null) {
                    viewModel.addTransfer(
                        fromAccountId = transfer.fromAccountId,
                        toAccountId = transfer.toAccountId,
                        amountPaisa = transfer.amountPaisa,
                        note = transfer.note,
                        date = transfer.date
                    )
                } else {
                    viewModel.updateTransfer(transfer)
                }
                showTransferDialog = false
                editingTransfer = null
            }
        )
    }

    if (showDebtDialog) {
        DebtDialogV3(
            initial = editingDebt,
            onDismiss = {
                showDebtDialog = false
                editingDebt = null
            },
            onSave = { debt ->
                if (editingDebt == null) {
                    val existingDebt = openDebts
                        .filter {
                            it.isOwedToMe == debt.isOwedToMe &&
                                    it.person.equals(debt.person.trim(), ignoreCase = true)
                        }
                        .maxByOrNull { it.date }

                    if (existingDebt != null) {
                        val combinedNote = when {
                            existingDebt.note.isBlank() -> debt.note.trim()
                            debt.note.isBlank() -> existingDebt.note
                            else -> existingDebt.note + " • " + debt.note.trim()
                        }

                        viewModel.updateDebt(
                            existingDebt.copy(
                                amountPaisa = existingDebt.amountPaisa + debt.amountPaisa,
                                note = combinedNote,
                                date = debt.date,
                                isSettled = false
                            )
                        )
                    } else {
                        viewModel.addDebt(
                            person = debt.person,
                            amountPaisa = debt.amountPaisa,
                            isOwedToMe = debt.isOwedToMe,
                            note = debt.note,
                            date = debt.date
                        )
                    }
                } else {
                    viewModel.updateDebt(debt)
                }
                showDebtDialog = false
                editingDebt = null
            }
        )
    }

    settlingDebt?.let { debt ->
        DebtSettlementDialog(
            debt = debt,
            onDismiss = { settlingDebt = null },
            onSave = { updatedDebt ->
                viewModel.updateDebt(updatedDebt)
                settlingDebt = null
            }
        )
    }

    deletingTransaction?.let { transaction ->
        ConfirmDeleteDialogV3(
            title = "Delete transaction?",
            message =
                "Delete ${transaction.category} for " +
                        "${formatMoney(transaction.amountPaisa)}?",
            onDismiss = { deletingTransaction = null },
            onConfirm = {
                viewModel.deleteTransaction(transaction)
                deletingTransaction = null
            }
        )
    }

    deletingTransfer?.let { transfer ->
        ConfirmDeleteDialogV3(
            title = "Delete transfer?",
            message =
                "Delete ${formatMoney(transfer.amountPaisa)} from " +
                        "${accounts.firstOrNull { it.id == transfer.fromAccountId }?.name ?: "Unknown"} to " +
                        "${accounts.firstOrNull { it.id == transfer.toAccountId }?.name ?: "Unknown"}?",
            onDismiss = { deletingTransfer = null },
            onConfirm = {
                viewModel.deleteTransfer(transfer)
                deletingTransfer = null
            }
        )
    }

    deletingDebt?.let { debt ->
        ConfirmDeleteDialogV3(
            title = "Delete owe record?",
            message = "Delete the record for ${debt.person}?",
            onDismiss = { deletingDebt = null },
            onConfirm = {
                viewModel.deleteDebt(debt)
                deletingDebt = null
            }
        )
    }
}

@Composable
private fun BalanceCardV3(
    displayedBalance: Long,
    selectedAccount: Account?,
    monthIncome: Long,
    monthExpense: Long,
    monthNet: Long,
    todayIncome: Long,
    todayExpense: Long,
    accountTotalBalance: Long,
    owedToMe: Long,
    iOwe: Long,
    onToggleAccounts: () -> Unit
){
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (selectedAccount == null) {
                    "Your total money"
                } else {
                    "${selectedAccount.name} balance"
                },
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = formatMoney(displayedBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (selectedAccount == null) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Accounts: ${formatMoney(accountTotalBalance)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Owed to you: +${formatMoneyRaw(owedToMe)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "You owe: -${formatMoneyRaw(iOwe)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }



            Text(
                text = "This month",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Income", monthIncome)
                MetricColumn("Expense", monthExpense)
                MetricColumn("Net", monthNet, alignEnd = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today: +${formatMoneyRaw(todayIncome)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "-${formatMoneyRaw(todayExpense)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            TextButton(onClick = onToggleAccounts) {
                Text("Accounts")
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: Long,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = formatMoneyRaw(value),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FilterRow(
    selectedFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    selectedCategory: String?,
    categories: List<String>,
    onCategoryChange: (String?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == TransactionFilter.ALL,
                onClick = { onFilterChange(TransactionFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == TransactionFilter.INCOME,
                onClick = { onFilterChange(TransactionFilter.INCOME) },
                label = { Text("Income") }
            )
            FilterChip(
                selected = selectedFilter == TransactionFilter.EXPENSE,
                onClick = { onFilterChange(TransactionFilter.EXPENSE) },
                label = { Text("Expense") }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategoryChange(null) },
                label = { Text("All categories") }
            )

            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory.equals(
                        category,
                        ignoreCase = true
                    ),
                    onClick = {
                        onCategoryChange(
                            if (selectedCategory.equals(
                                    category,
                                    ignoreCase = true
                                )
                            ) {
                                null
                            } else {
                                category
                            }
                        )
                    },
                    label = { Text(category) }
                )
            }
        }
    }
}

@Composable
private fun AccountCardV3(
    account: Account,
    balance: Long,
    selected: Boolean,
    transactionCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$transactionCount transactions • opening ${formatMoney(account.initialBalancePaisa)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                text = formatMoney(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit account"
                )
            }
        }
    }
}

@Composable
private fun DebtSummaryCardV3(
    iOwe: Long,
    owedToMe: Long,
    netPeopleBalance: Long,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("I owe", iOwe)
                MetricColumn("Owed to me", owedToMe, alignEnd = true)
            }

            Text(
                text = "Net people balance: ${formatMoney(netPeopleBalance)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            TextButton(onClick = onAdd) {
                Text("Add owe record")
            }
        }
    }
}

@Composable
private fun DebtCardV3(
    debt: Debt,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.person,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (debt.isOwedToMe) {
                            "Owes you • ${formatDate(debt.date)}"
                        } else {
                            "You owe • ${formatDate(debt.date)}"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = formatMoney(debt.amountPaisa),
                    fontWeight = FontWeight.Bold
                )
            }

            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSettle) {
                    Text("Settle")
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit debt"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete debt"
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCardV3(
    transaction: MoneyTransaction,
    accountName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val prefix = if (transaction.isIncome) "+" else "-"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$accountName • ${formatDate(transaction.date)}",
                    style = MaterialTheme.typography.labelSmall
                )

                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${formatMoney(transaction.amountPaisa)}",
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit transaction"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete transaction"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferCardV3(
    transfer: MoneyTransfer,
    fromName: String,
    toName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null
            )

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$fromName → $toName",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDate(transfer.date),
                    style = MaterialTheme.typography.labelSmall
                )
                if (transfer.note.isNotBlank()) {
                    Text(
                        text = transfer.note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(transfer.amountPaisa),
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit transfer"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete transfer"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SectionHeaderV3(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (actionText.isNotBlank()) {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun EmptyFilteredMoneyState(
    hasAnyTransactions: Boolean,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasAnyTransactions) {
                Icons.Default.Search
            } else {
                Icons.Default.ReceiptLong
            },
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (hasAnyTransactions) {
                "No matching transactions"
            } else {
                "No transactions yet"
            },
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onClear) {
            Text("Clear filters")
        }
    }
}

@Composable
private fun ActionChoiceDialogV3(
    onDismiss: () -> Unit,
    onTransaction: () -> Unit,
    onTransfer: () -> Unit,
    onDebt: () -> Unit,
    onAccount: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Money") },
        text = {
            Column {
                Button(
                    onClick = onTransaction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Income / Expense")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onTransfer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Transfer between accounts")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDebt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Owe / Owed to me")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add account")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialogV3(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AccountDialogV3(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (Account, Boolean) -> Unit
) {
    var name by remember(initial?.id) {
        mutableStateOf(initial?.name ?: "")
    }

    var opening by remember(initial?.id) {
        mutableStateOf(
            initial?.let {
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    it.initialBalancePaisa / 100.0
                )
            } ?: ""
        )
    }

    val isNew = initial == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) "Add account" else "Edit account"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    placeholder = { Text("Bank, eSewa, Cash...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = { Text("Opening balance (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial?.copy(
                            name = name.trim(),
                            initialBalancePaisa = parseAmountToPaisa(opening)
                        ) ?: Account(
                            name = name.trim(),
                            initialBalancePaisa = parseAmountToPaisa(opening)
                        ),
                        isNew
                    )
                },
                enabled = name.isNotBlank() && parseAmountToPaisa(opening) >= 0L
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDialogV3(
    accounts: List<Account>,
    categories: List<String>,
    initial: MoneyTransaction?,
    preferredAccountId: Long?,
    onDismiss: () -> Unit,
    onSave: (MoneyTransaction) -> Unit
) {
    var isIncome by remember(initial?.id) {
        mutableStateOf(initial?.isIncome ?: false)
    }

    var accountId by remember(initial?.id) {
        mutableStateOf(
            initial?.accountId
                ?: preferredAccountId
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial?.amountPaisa?.let {
                String.format(Locale.getDefault(), "%.2f", it / 100.0)
            } ?: ""
        )
    }

    var category by remember(initial?.id) {
        mutableStateOf(initial?.category ?: "")
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note ?: "")
    }

    var date by remember(initial?.id) {
        mutableStateOf(initial?.date ?: System.currentTimeMillis())
    }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "New transaction" else "Edit transaction"
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { isIncome = false }) {
                        Text(if (!isIncome) "✓ Expense" else "Expense")
                    }
                    TextButton(onClick = { isIncome = true }) {
                        Text(if (isIncome) "✓ Income" else "Income")
                    }
                }

                OutlinedButton(
                    onClick = { showAccountPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Account: " +
                                (accounts.firstOrNull { it.id == accountId }?.name
                                    ?: "Select account")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Food, Salary, Transport...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose category")
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: ${formatDate(date)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paisa = parseAmountToPaisa(amount)
                    if (
                        accountId > 0L &&
                        paisa > 0L &&
                        category.isNotBlank()
                    ) {
                        onSave(
                            MoneyTransaction(
                                id = 0L,
                                accountId = accountId,
                                isIncome = isIncome,
                                amountPaisa = paisa,
                                category = category.trim(),
                                note = note.trim(),
                                date = date,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled =
                    accountId > 0L &&
                            parseAmountToPaisa(amount) > 0L &&
                            category.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAccountPicker) {
        AccountPickerDialogV3(
            accounts = accounts,
            selectedAccountId = accountId,
            onDismiss = { showAccountPicker = false },
            onSelect = {
                accountId = it
                showAccountPicker = false
            }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategory = category,
            onDismiss = { showCategoryPicker = false },
            onSelect = {
                category = it
                showCategoryPicker = false
            }
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { date = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<String>,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose category") },
        text = {
            Column {
                categories.take(40).forEach { category ->
                    OutlinedButton(
                        onClick = { onSelect(category) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedCategory.equals(category, true)) {
                                "✓ $category"
                            } else {
                                category
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AccountPickerDialogV3(
    accounts: List<Account>,
    selectedAccountId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose account") },
        text = {
            Column {
                accounts.forEach { account ->
                    OutlinedButton(
                        onClick = { onSelect(account.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (account.id == selectedAccountId) {
                                "✓ ${account.name}"
                            } else {
                                account.name
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferDialogV3(
    accounts: List<Account>,
    initial: MoneyTransfer?,
    onDismiss: () -> Unit,
    onSave: (MoneyTransfer) -> Unit
) {
    var fromId by remember(initial?.id) {
        mutableStateOf(
            initial?.fromAccountId
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    var toId by remember(initial?.id) {
        mutableStateOf(
            initial?.toAccountId
                ?: accounts.getOrNull(1)?.id
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial?.amountPaisa?.let {
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    it / 100.0
                )
            } ?: ""
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note ?: "")
    }

    var date by remember(initial?.id) {
        mutableStateOf(
            initial?.date ?: System.currentTimeMillis()
        )
    }

    var choosingFrom by remember { mutableStateOf(false) }
    var choosingTo by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "Transfer money" else "Edit transfer"
            )
        },
        text = {
            Column {
                OutlinedButton(
                    onClick = { choosingFrom = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "From: " +
                                (accounts.firstOrNull { it.id == fromId }?.name
                                    ?: "Select")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { choosingTo = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "To: " +
                                (accounts.firstOrNull { it.id == toId }?.name
                                    ?: "Select")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: ${formatDate(date)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paisa = parseAmountToPaisa(amount)
                    if (fromId != toId && paisa > 0L) {
                        onSave(
                            MoneyTransfer(
                                id = initial?.id ?: 0L,
                                fromAccountId = fromId,
                                toAccountId = toId,
                                amountPaisa = paisa,
                                note = note.trim(),
                                date = date,
                                createdAt =
                                    initial?.createdAt
                                        ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled =
                    fromId != toId &&
                            fromId > 0L &&
                            toId > 0L &&
                            parseAmountToPaisa(amount) > 0L
            ) {
                Text(
                    if (initial == null) "Transfer" else "Update"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (choosingFrom) {
        AccountPickerDialogV3(
            accounts = accounts,
            selectedAccountId = fromId,
            onDismiss = { choosingFrom = false },
            onSelect = {
                fromId = it
                choosingFrom = false
            }
        )
    }

    if (choosingTo) {
        AccountPickerDialogV3(
            accounts = accounts,
            selectedAccountId = toId,
            onDismiss = { choosingTo = false },
            onSelect = {
                toId = it
                choosingTo = false
            }
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { date = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtDialogV3(
    initial: Debt?,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    var isOwedToMe by remember(initial?.id) {
        mutableStateOf(initial?.isOwedToMe ?: false)
    }

    var person by remember(initial?.id) {
        mutableStateOf(initial?.person ?: "")
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial?.amountPaisa?.let {
                String.format(Locale.getDefault(), "%.2f", it / 100.0)
            } ?: ""
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note ?: "")
    }

    var date by remember(initial?.id) {
        mutableStateOf(initial?.date ?: System.currentTimeMillis())
    }

    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Add owe record" else "Edit owe record")
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { isOwedToMe = false }) {
                        Text(if (!isOwedToMe) "✓ I owe" else "I owe")
                    }
                    TextButton(onClick = { isOwedToMe = true }) {
                        Text(if (isOwedToMe) "✓ Owes me" else "Owes me")
                    }
                }

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: ${formatDate(date)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paisa = parseAmountToPaisa(amount)
                    if (paisa > 0L && person.isNotBlank()) {
                        onSave(
                            Debt(
                                id = initial?.id ?: 0L,
                                person = person.trim(),
                                amountPaisa = paisa,
                                isOwedToMe = isOwedToMe,
                                note = note.trim(),
                                date = date,
                                isSettled = initial?.isSettled ?: false,
                                createdAt = initial?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled =
                    parseAmountToPaisa(amount) > 0L &&
                            person.isNotBlank()
            ) {
                Text(if (initial == null) "Add owe" else "Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { date = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DebtSettlementDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    var amount by remember(debt.id) {
        mutableStateOf(
            String.format(
                Locale.getDefault(),
                "%.2f",
                debt.amountPaisa / 100.0
            )
        )
    }

    val settlementPaisa = parseAmountToPaisa(amount)
    val remaining = (debt.amountPaisa - settlementPaisa).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle ${debt.person}") },
        text = {
            Column {
                Text(
                    text = "Remaining: ${formatMoney(debt.amountPaisa)}",
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to settle (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (settlementPaisa >= debt.amountPaisa && settlementPaisa > 0L) {
                        "This will mark the debt as fully settled."
                    } else {
                        "Remaining after this: ${formatMoney(remaining)}"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (settlementPaisa >= debt.amountPaisa) {
                        onSave(debt.copy(isSettled = true))
                    } else if (settlementPaisa > 0L) {
                        onSave(
                            debt.copy(
                                amountPaisa = debt.amountPaisa - settlementPaisa,
                                isSettled = false
                            )
                        )
                    }
                },
                enabled = settlementPaisa > 0L
            ) {
                Text(
                    if (settlementPaisa >= debt.amountPaisa) {
                        "Settle fully"
                    } else {
                        "Settle part"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseAmountToPaisa(value: String): Long {
    return value
        .trim()
        .replace(",", "")
        .toDoubleOrNull()
        ?.let {
            if (it <= 0.0) 0L else (it * 100.0).toLong()
        }
        ?: 0L
}

private fun formatMoney(amountPaisa: Long): String {
    return String.format(
        Locale.getDefault(),
        "NPR %.2f",
        amountPaisa / 100.0
    )
}

private fun formatMoneyRaw(amountPaisa: Long): String {
    return String.format(
        Locale.getDefault(),
        "NPR %.2f",
        amountPaisa / 100.0
    )
}

private fun formatDate(millis: Long): String {
    return SimpleDateFormat(
        "MMM d, yyyy",
        Locale.getDefault()
    ).format(Date(millis))
}

private fun monthLabel(millis: Long): String {
    return SimpleDateFormat(
        "MMMM yyyy",
        Locale.getDefault()
    ).format(Date(millis))
}

private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun startOfMonth(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
