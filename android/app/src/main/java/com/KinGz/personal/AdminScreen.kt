
package com.KinGz.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.KinGz.personal.network.AdminSessionResponse
import com.KinGz.personal.network.AuthRepository
import com.KinGz.personal.network.DeviceResponse
import com.KinGz.personal.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    var serverStatus by remember { mutableStateOf("Not checked") }
    var checkingServer by remember { mutableStateOf(false) }
    var databaseStatus by remember { mutableStateOf("Not checked") }
    var checkingDatabase by remember { mutableStateOf(false) }

    var devices by remember { mutableStateOf<List<DeviceResponse>>(emptyList()) }
    var devicesMessage by remember { mutableStateOf("Tap below to load devices.") }
    var loadingDevices by remember { mutableStateOf(false) }
    var devicesLoaded by remember { mutableStateOf(false) }
    var revokingDeviceId by remember { mutableStateOf<Int?>(null) }
    var revokeMessage by remember { mutableStateOf("") }

    var sessions by remember {
        mutableStateOf<List<AdminSessionResponse>>(emptyList())
    }
    var sessionsMessage by remember {
        mutableStateOf("Load sessions to inspect authenticated sessions.")
    }
    var loadingSessions by remember { mutableStateOf(false) }
    var sessionsLoaded by remember { mutableStateOf(false) }
    var revokingSessionId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("KinGz Admin")
                        Text(
                            "Server Control Center",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Server Status", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Status: $serverStatus")
                        Spacer(Modifier.height(12.dp))

                        Button(
                            enabled = !checkingServer,
                            onClick = {
                                scope.launch {
                                    checkingServer = true
                                    serverStatus = "Checking..."
                                    try {
                                        val response = RetrofitClient.api.healthCheck()
                                        serverStatus = if (response.isSuccessful) {
                                            response.body()?.get("status")?.toString()
                                                ?: "Online (response received)"
                                        } else {
                                            "HTTP ${response.code()}"
                                        }
                                    } catch (e: Exception) {
                                        serverStatus =
                                            "Connection failed: ${e.message ?: "Unknown error"}"
                                    } finally {
                                        checkingServer = false
                                    }
                                }
                            }
                        ) {
                            Text(if (checkingServer) "Checking..." else "Check Server")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Devices, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Device Management", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "View devices registered by all users.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = !loadingDevices,
                            onClick = {
                                scope.launch {
                                    loadingDevices = true
                                    devicesMessage = "Loading devices..."
                                    repository.adminListAllDevices().fold(
                                        onSuccess = { result ->
                                            devices = result
                                            devicesLoaded = true
                                            devicesMessage = if (result.isEmpty()) {
                                                "No registered devices found."
                                            } else {
                                                "${result.size} device(s) found."
                                            }
                                        },
                                        onFailure = { error ->
                                            devicesMessage =
                                                error.message ?: "Failed to load devices."
                                        }
                                    )
                                    loadingDevices = false
                                }
                            }
                        ) {
                            Text(if (loadingDevices) "Loading..." else "Load Devices")
                        }

                        if (loadingDevices) {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(devicesMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (devicesLoaded) {
                item {
                    if (revokeMessage.isNotBlank()) {
                        Text(revokeMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                items(devices, key = { it.id }) { device ->
                    DeviceItemCard(
                        device = device,
                        revoking = revokingDeviceId == device.id,
                        onRevoke = { deviceId ->
                            scope.launch {
                                revokingDeviceId = deviceId
                                revokeMessage = ""
                                repository.adminRevokeDevice(deviceId).fold(
                                    onSuccess = { updatedDevice ->
                                        devices = devices.map {
                                            if (it.id == updatedDevice.id) updatedDevice else it
                                        }
                                        revokeMessage =
                                            "Device $deviceId revoked successfully."
                                    },
                                    onFailure = { error ->
                                        revokeMessage =
                                            error.message ?: "Failed to revoke device."
                                    }
                                )
                                revokingDeviceId = null
                            }
                        },
                        onReactivate = { deviceId ->
                            scope.launch {
                                revokingDeviceId = deviceId
                                revokeMessage = ""
                                repository.adminReactivateDevice(deviceId).fold(
                                    onSuccess = { updatedDevice ->
                                        devices = devices.map {
                                            if (it.id == updatedDevice.id) updatedDevice else it
                                        }
                                        revokeMessage =
                                            "Device $deviceId reactivated successfully."
                                    },
                                    onFailure = { error ->
                                        revokeMessage =
                                            error.message ?: "Failed to reactivate device."
                                    }
                                )
                                revokingDeviceId = null
                            }
                        }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Security & Sessions", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Inspect authenticated sessions and revoke individual sessions.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (sessionsLoaded) {
                            Spacer(Modifier.height(12.dp))
                            val activeCount = sessions.count { it.status.equals("active", true) }
                            val expiredCount = sessions.count { it.status.equals("expired", true) }
                            val revokedCount = sessions.count { it.status.equals("revoked", true) }

                            Text("Active: $activeCount")
                            Text("Expired: $expiredCount")
                            Text("Revoked: $revokedCount")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = !loadingSessions,
                            onClick = {
                                scope.launch {
                                    loadingSessions = true
                                    sessionsMessage = "Loading sessions..."
                                    repository.adminListSessions().fold(
                                        onSuccess = { result ->
                                            sessions = result
                                            sessionsLoaded = true
                                            sessionsMessage = if (result.isEmpty()) {
                                                "No sessions found."
                                            } else {
                                                "${result.size} session(s) found."
                                            }
                                        },
                                        onFailure = { error ->
                                            sessionsMessage =
                                                error.message ?: "Failed to load sessions."
                                        }
                                    )
                                    loadingSessions = false
                                }
                            }
                        ) {
                            Text(if (loadingSessions) "Loading..." else "Refresh Sessions")
                        }

                        if (loadingSessions) {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(sessionsMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (sessionsLoaded) {
                items(sessions, key = { it.id }) { session ->
                    AdminSessionCard(
                        session = session,
                        revoking = revokingSessionId == session.id,
                        onRevoke = { sessionId ->
                            scope.launch {
                                revokingSessionId = sessionId
                                repository.adminRevokeSession(sessionId).fold(
                                    onSuccess = {
                                        sessions = sessions.map { item ->
                                            if (item.id == sessionId) {
                                                item.copy(
                                                    status = "revoked",
                                                    revokedAt = item.revokedAt
                                                )
                                            } else {
                                                item
                                            }
                                        }
                                        sessionsMessage = "Session $sessionId revoked successfully."
                                    },
                                    onFailure = { error ->
                                        sessionsMessage =
                                            error.message ?: "Failed to revoke session."
                                    }
                                )
                                revokingSessionId = null
                            }
                        }
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Column {
                                Text("Database", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "PostgreSQL connection and health",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Status: $databaseStatus")
                        Spacer(Modifier.height(12.dp))

                        Button(
                            enabled = !checkingDatabase,
                            onClick = {
                                scope.launch {
                                    checkingDatabase = true
                                    databaseStatus = "Checking..."
                                    repository.adminDatabaseHealth().fold(
                                        onSuccess = { result ->
                                            databaseStatus =
                                                result["status"]?.toString() ?: "Connected"
                                        },
                                        onFailure = { error ->
                                            databaseStatus =
                                                error.message ?: "Database check failed"
                                        }
                                    )
                                    checkingDatabase = false
                                }
                            }
                        ) {
                            Text(if (checkingDatabase) "Checking..." else "Check Database")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItemCard(
    device: DeviceResponse,
    onRevoke: (Int) -> Unit,
    onReactivate: (Int) -> Unit,
    revoking: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Device ID: ${device.id}")
            Text("User ID: ${device.userId}")
            Text("Platform: ${device.platform}")
            Text("Authorization: " + if (device.isAuthorized) "Authorized" else "Revoked")
            device.lastSeenAt?.let { Text("Last seen: $it") }
            Spacer(Modifier.height(12.dp))

            if (device.isAuthorized) {
                Button(enabled = !revoking, onClick = { onRevoke(device.id) }) {
                    Text(if (revoking) "Processing..." else "Revoke Device")
                }
            } else {
                Button(enabled = !revoking, onClick = { onReactivate(device.id) }) {
                    Text(if (revoking) "Processing..." else "Reactivate Device")
                }
            }
        }
    }
}

@Composable
private fun AdminSessionCard(
    session: AdminSessionResponse,
    revoking: Boolean,
    onRevoke: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(session.username, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Session ID: ${session.id}")
            Text("Device: ${session.deviceName} (#${session.deviceId})")
            Text("Platform: ${session.platform}")
            Text("Status: ${session.status.uppercase()}")
            Text("Created: ${session.createdAt}")
            Text("Expires: ${session.expiresAt}")

            Spacer(Modifier.height(12.dp))
            if (session.status.equals("active", true)) {
                Button(
                    enabled = !revoking,
                    onClick = { onRevoke(session.id) }
                ) {
                    Text(if (revoking) "Revoking..." else "Revoke Session")
                }
            }
        }
    }
}