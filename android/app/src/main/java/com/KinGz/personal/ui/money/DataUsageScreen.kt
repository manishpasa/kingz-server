package com.KinGz.personal.ui.datausage

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.KinGz.personal.datausage.DataUsageManager
import java.util.Locale
import kotlin.math.ceil

private const val MILLIS_PER_DAY = 86_400_000.0

@Composable
fun DataUsageScreen(
    viewModel: DataUsageViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            DataUsageTopBar(
                onBack = onBack,
                onRefresh = viewModel::refresh,
                onSettings = { showSettings = true }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Mobile data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Track cellular usage for the current billing cycle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!state.hasUsageAccess) {
                item {
                    PermissionCard(
                        onGrantAccess = {
                            try {
                                context.startActivity(
                                    DataUsageManager.usageAccessSettingsIntent(context)
                                )
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "Usage Access settings are not available on this device.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            } else {
                item {
                    UsageSummaryCard(
                        state = state,
                        onRefresh = viewModel::refresh
                    )
                }

                item {
                    CycleStatsCard(state = state)
                }

                item {
                    InfoCard()
                }

                item {
                    Text(
                        text = "Top apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.isLoading) {
                    item {
                        LoadingCard()
                    }
                } else if (state.topApps.isEmpty()) {
                    item {
                        EmptyAppsCard()
                    }
                } else {
                    items(
                        items = state.topApps,
                        key = { "${it.packageName}_${it.bytes}" }
                    ) { app ->
                        AppUsageCard(app = app)
                    }
                }
            }

            state.errorMessage?.let { errorMessage ->
                item {
                    ErrorCard(message = errorMessage)
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showSettings) {
        DataPlanDialog(
            initialQuotaGb = state.quotaGb,
            initialCycleStartDay = state.cycleStartDay,
            onDismiss = { showSettings = false },
            onSave = { quotaGb, cycleStartDay ->
                viewModel.saveSettings(
                    quotaGb = quotaGb,
                    cycleStartDay = cycleStartDay
                )
                showSettings = false
            }
        )
    }
}

@Composable
private fun DataUsageTopBar(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Data Usage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All mobile networks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }

        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Data plan settings"
            )
        }
    }
}

@Composable
private fun PermissionCard(
    onGrantAccess: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DataUsage,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Usage Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Android requires a special Settings permission so KinGz can read mobile data usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onGrantAccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Usage Access Settings")
            }
        }
    }
}

@Composable
private fun UsageSummaryCard(
    state: DataUsageUiState,
    onRefresh: () -> Unit
) {
    val usageText = formatGb(state.usedGb)
    val quotaText = if (state.quotaGb > 0.0) {
        formatGb(state.quotaGb)
    } else {
        "No limit set"
    }

    val remainingText = if (state.quotaGb > 0.0) {
        "${formatGb(state.remainingGb)} remaining"
    } else {
        "Monthly quota not configured"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Used this cycle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = usageText,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh usage"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quota: $quotaText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (state.quotaGb > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { state.quotaProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cycle: ${formatDate(state.cycleStart)} → ${formatDate(state.cycleEnd)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CycleStatsCard(
    state: DataUsageUiState
) {
    val now = System.currentTimeMillis()
    val cycleStart = state.cycleStart
    val cycleEnd = state.cycleEnd

    if (cycleStart <= 0L || cycleEnd <= cycleStart) {
        return
    }

    val elapsedDays =
        ((now - cycleStart) / MILLIS_PER_DAY).coerceAtLeast(1.0)
    val totalDays =
        ((cycleEnd - cycleStart) / MILLIS_PER_DAY).coerceAtLeast(1.0)
    val daysRemaining =
        ceil(((cycleEnd - now) / MILLIS_PER_DAY).coerceAtLeast(0.0)).toInt()

    val dailyAverage = state.usedGb / elapsedDays
    val projectedUsage = dailyAverage * totalDays

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Cycle stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBlock(
                    modifier = Modifier.weight(1f),
                    title = "Daily average",
                    value = formatGb(dailyAverage)
                )

                StatBlock(
                    modifier = Modifier.weight(1f),
                    title = "Projected cycle",
                    value = formatGb(projectedUsage)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (daysRemaining == 1) {
                    "1 day remaining in this cycle"
                } else {
                    "$daysRemaining days remaining in this cycle"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatBlock(
    modifier: Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = "Usage is read from Android's network statistics. It may differ slightly from the carrier's own counter because carriers can apply their own billing rules.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppUsageCard(
    app: com.KinGz.personal.datausage.AppDataUsage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatGb(app.bytes / 1_000_000_000.0),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Text("Reading mobile data usage…")
        }
    }
}

@Composable
private fun EmptyAppsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No per-app mobile usage was returned for this cycle.",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DataPlanDialog(
    initialQuotaGb: Double,
    initialCycleStartDay: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var quotaText by remember(initialQuotaGb) {
        mutableStateOf(
            if (initialQuotaGb > 0.0) {
                formatGb(initialQuotaGb)
            } else {
                ""
            }
        )
    }

    var cycleDayText by remember(initialCycleStartDay) {
        mutableStateOf(initialCycleStartDay.toString())
    }

    val parsedDay = cycleDayText.toIntOrNull()
    val validDay = parsedDay != null && parsedDay in 1..31
    val parsedQuota = quotaText.replace(",", ".").toDoubleOrNull()
    val validQuota = parsedQuota != null && parsedQuota >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Data plan",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = quotaText,
                    onValueChange = { quotaText = it },
                    label = { Text("Monthly quota (GB)") },
                    placeholder = { Text("e.g. 12") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cycleDayText,
                    onValueChange = { cycleDayText = it },
                    label = { Text("Cycle starts on day") },
                    placeholder = { Text("1–31") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Leave quota empty to track usage without a monthly limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        parsedQuota ?: 0.0,
                        parsedDay ?: 1
                    )
                },
                enabled = validDay && validQuota
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

private fun formatGb(value: Double): String {
    return String.format(
        Locale.getDefault(),
        "%.2f GB",
        value
    )
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return "—"
    return DataUsageManager.formatDate(millis)
}
