package com.KinGz.personal.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.KinGz.personal.ui.notes.NoteSettingsStore

private fun settingsPalette(): List<Triple<String, Color, Color>> = listOf(
    Triple("default", Color(0xFFF7F7F4), Color(0xFF202124)),
    Triple("gray", Color(0xFFE7E8E5), Color(0xFF202124)),
    Triple("yellow", Color(0xFFF2EEDC), Color(0xFF202124)),
    Triple("green", Color(0xFFE2ECE4), Color(0xFF202124)),
    Triple("blue", Color(0xFFE2E8F0), Color(0xFF202124)),
    Triple("purple", Color(0xFFE9E4EC), Color(0xFF202124)),
    Triple("pink", Color(0xFFF0E5E7), Color(0xFF202124)),
    Triple("black", Color(0xFF303236), Color.White)
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { NoteSettingsStore(context) }

    var defaultColor by rememberSaveable { mutableStateOf(store.getDefaultColor()) }
    var confirmTrash by rememberSaveable { mutableStateOf(store.getConfirmTrash()) }
    var showTimestamps by rememberSaveable { mutableStateOf(store.getShowTimestamps()) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionTitle("Notes")

            Text(
                "Default note color",
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingsPalette().forEach { (key, bg, textColor) ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .border(
                                if (key == defaultColor) 3.dp else 1.dp,
                                if (key == defaultColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                            .clickable {
                                defaultColor = key
                                store.setDefaultColor(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == defaultColor) {
                            Text("✓", color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingToggle(
                title = "Ask before moving to Trash",
                subtitle = "Prevent accidental deletion",
                checked = confirmTrash,
                onCheckedChange = {
                    confirmTrash = it
                    store.setConfirmTrash(it)
                }
            )

            SettingToggle(
                title = "Show timestamps",
                subtitle = "Show last-updated time on note cards",
                checked = showTimestamps,
                onCheckedChange = {
                    showTimestamps = it
                    store.setShowTimestamps(it)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle("App")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("About KinGz", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Personal Hub • local-first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showAbout = true }) {
                    Text("View")
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("KinGz Personal Hub") },
            text = {
                Text(
                    "Your personal toolkit. Notes, tasks, money and data usage are designed to work locally on your device first."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
