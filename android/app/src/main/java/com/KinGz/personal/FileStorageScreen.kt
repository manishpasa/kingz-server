package com.KinGz.personal

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.KinGz.personal.network.FileResponse
import com.KinGz.personal.network.FileStorageRepository
import com.KinGz.personal.network.FolderResponse
import kotlinx.coroutines.launch

@Composable
fun FileStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FileStorageRepository(context) }
    val scope = rememberCoroutineScope()

    var folderStack by remember { mutableStateOf<List<FolderResponse>>(emptyList()) }
    val currentFolder = folderStack.lastOrNull()
    val currentFolderId = currentFolder?.id

    var folders by remember { mutableStateOf<List<FolderResponse>>(emptyList()) }
    var files by remember { mutableStateOf<List<FileResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    var dialog by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderResponse?>(null) }
    var selectedFile by remember { mutableStateOf<FileResponse?>(null) }
    var folderName by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            loading = true
            error = null

            val folderResult = repository.getFolders(currentFolderId)
            val fileResult = repository.getFiles(currentFolderId)

            folderResult.onSuccess { folders = it }
                .onFailure { error = it.message ?: "Unable to load folders." }

            fileResult.onSuccess { files = it }
                .onFailure {
                    error = it.message ?: "Unable to load files."
                }

            loading = false
        }
    }

    LaunchedEffect(currentFolderId) {
        refresh()
    }

    fun runAction(action: suspend () -> Result<*>, successMessage: String) {
        scope.launch {
            loading = true
            error = null
            message = null

            action()
                .onSuccess {
                    message = successMessage
                    refresh()
                }
                .onFailure {
                    error = it.message ?: "Operation failed."
                }

            loading = false
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runAction(
                { repository.uploadFile(uri, currentFolderId) },
                "File uploaded successfully."
            )
        }
    }

    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val file = selectedFile
        if (uri != null && file != null) {
            scope.launch {
                loading = true
                error = null

                repository.downloadFile(file.id)
                    .onSuccess { body ->
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                body.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            } ?: throw Exception("Unable to save the file.")

                            message = "File downloaded successfully."
                        } catch (e: Exception) {
                            error = e.message ?: "Unable to save the file."
                        } finally {
                            body.close()
                        }
                    }
                    .onFailure {
                        error = it.message ?: "Download failed."
                    }

                loading = false
                selectedFile = null
            }
        }
    }

    BackHandler(enabled = folderStack.isNotEmpty()) {
        folderStack = folderStack.dropLast(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                if (folderStack.isNotEmpty()) {
                    folderStack = folderStack.dropLast(1)
                } else {
                    onBack()
                }
            }) {
                Text("â† Back")
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = { refresh() },
                enabled = !loading
            ) {
                Text("Refresh")
            }
        }

        Text(
            text = currentFolder?.name ?: "File Storage",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (folderStack.isEmpty()) {
                "Your server files and folders"
            } else {
                "Folder: ${folderStack.joinToString(" / ") { it.name }}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    folderName = ""
                    dialog = "create"
                },
                enabled = !loading
            ) {
                Text("New folder")
            }

            OutlinedButton(
                onClick = { uploadLauncher.launch(arrayOf("*/*")) },
                enabled = !loading
            ) {
                Text("Upload file")
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (folders.isEmpty() && files.isEmpty() && !loading) {
                item {
                    Text(
                        "This folder is empty.",
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(folders, key = { "folder-${it.id}" }) { folder ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "ðŸ“ ${folder.name}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (folder.isPublic) "Public folder" else "Private folder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(onClick = {
                                folderStack = folderStack + folder
                            }) {
                                Text("Open")
                            }
                        }

                        if (folder.isOwner) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(onClick = {
                                    selectedFolder = folder
                                    folderName = folder.name
                                    dialog = "rename"
                                }) {
                                    Text("Rename")
                                }

                                TextButton(onClick = {
                                    runAction(
                                        {
                                            repository.changeFolderVisibility(
                                                folder.id,
                                                !folder.isPublic
                                            )
                                        },
                                        "Folder visibility updated."
                                    )
                                }) {
                                    Text(if (folder.isPublic) "Make private" else "Make public")
                                }

                                TextButton(onClick = {
                                    selectedFolder = folder
                                    dialog = "delete-folder"
                                }) {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(files, key = { "file-${it.id}" }) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(file.name, fontWeight = FontWeight.SemiBold)

                        Text(
                            "${file.contentType} â€¢ ${formatFileSize(file.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = {
                                selectedFile = file
                                downloadLauncher.launch(file.name)
                            }) {
                                Text("Download")
                            }

                            if (file.isOwner) {
                                TextButton(onClick = {
                                    selectedFile = file
                                    dialog = "move"
                                }) {
                                    Text("Move to root")
                                }

                                TextButton(onClick = {
                                    selectedFile = file
                                    dialog = "delete-file"
                                }) {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "create", "rename" -> {
            val isRename = dialog == "rename"

            AlertDialog(
                onDismissRequest = { dialog = null },
                title = {
                    Text(if (isRename) "Rename folder" else "Create folder")
                },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = folderName.isNotBlank() && !loading,
                        onClick = {
                            val name = folderName.trim()
                            dialog = null

                            if (isRename) {
                                val folder = selectedFolder
                                if (folder != null) {
                                    runAction(
                                        { repository.renameFolder(folder.id, name) },
                                        "Folder renamed."
                                    )
                                }
                            } else {
                                runAction(
                                    {
                                        repository.createFolder(
                                            name,
                                            currentFolderId
                                        )
                                    },
                                    "Folder created."
                                )
                            }
                        }
                    ) {
                        Text(if (isRename) "Save" else "Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        "delete-folder" -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Delete folder?") },
                text = {
                    Text("Delete '${selectedFolder?.name}'? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val folder = selectedFolder
                        dialog = null
                        if (folder != null) {
                            runAction(
                                { repository.deleteFolder(folder.id) },
                                "Folder deleted."
                            )
                        }
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        "delete-file" -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Delete file?") },
                text = {
                    Text("Delete '${selectedFile?.name}'? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val file = selectedFile
                        dialog = null
                        if (file != null) {
                            runAction(
                                { repository.deleteFile(file.id) },
                                "File deleted."
                            )
                        }
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        "move" -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Move file to root?") },
                text = {
                    Text("Move '${selectedFile?.name}' out of this folder and into the root folder?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val file = selectedFile
                        dialog = null
                        if (file != null) {
                            runAction(
                                { repository.moveFile(file.id, null) },
                                "File moved to root."
                            )
                        }
                    }) {
                        Text("Move")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L ->
            "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
