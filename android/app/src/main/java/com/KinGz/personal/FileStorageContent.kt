package com.KinGz.personal

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.KinGz.personal.network.ApiConfig
import com.KinGz.personal.network.FileResponse
import com.KinGz.personal.network.FileStorageRepository
import com.KinGz.personal.network.FolderResponse
import com.KinGz.personal.ui.files.rememberGalleryImageLoader
import kotlinx.coroutines.launch

private enum class GalleryTab { PHOTOS, ALBUMS, OTHER }

private fun FileResponse.isImage() = contentType.startsWith("image/", true)
private fun FileResponse.isVideo() = contentType.startsWith("video/", true)
private fun FileResponse.isMedia() = isImage() || isVideo()

private fun FileResponse.downloadUrl() =
    "${ApiConfig.BASE_URL.trimEnd('/')}/api/files/$id/download"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileStorageContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FileStorageRepository(context) }
    val imageLoader = rememberGalleryImageLoader()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(GalleryTab.PHOTOS) }
    var path by remember { mutableStateOf(emptyList<FolderResponse>()) }
    val currentFolder = path.lastOrNull()

    var folders by remember { mutableStateOf(emptyList<FolderResponse>()) }
    var files by remember { mutableStateOf(emptyList<FileResponse>()) }
    var galleryFiles by remember { mutableStateOf(emptyList<FileResponse>()) }

    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }

    var showCreate by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var editingFolder by remember { mutableStateOf<FolderResponse?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var confirmDeleteFiles by remember { mutableStateOf(false) }
    var confirmDeleteFolder by remember { mutableStateOf<FolderResponse?>(null) }
    var albumContents by remember { mutableStateOf<Map<Int, List<FileResponse>>>(emptyMap()) }

    fun refresh() {
        scope.launch {
            loading = true
            error = null

            val folderResult = repository.getFolders(currentFolder?.id)
            val fileResult = repository.getFiles(currentFolder?.id)
            val galleryResult = repository.getGalleryFiles()

            if (folderResult.isSuccess && fileResult.isSuccess) {
                folders = folderResult.getOrThrow()
                files = fileResult.getOrThrow()
                galleryFiles = galleryResult.getOrElse { emptyList() }
                error = galleryResult.exceptionOrNull()?.message
            } else {
                error = folderResult.exceptionOrNull()?.message
                    ?: fileResult.exceptionOrNull()?.message
                    ?: "Unable to load Gallery."
            }

            loading = false
        }
    }

    LaunchedEffect(currentFolder?.id) {
        selectedIds = emptySet()
        refresh()
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                busy = true
                error = null
                message = null

                val result = repository.uploadFiles(
                    uris,
                    currentFolder?.id
                ) { current, total, name ->
                    progress = "Uploaded $current of $total: $name"
                }

                if (result.isSuccess) {
                    message = "${result.getOrThrow().size} files uploaded."
                } else {
                    error = result.exceptionOrNull()?.message ?: "Upload failed."
                }

                busy = false
                progress = ""
                refresh()
            }
        }
    }

    LaunchedEffect(folders, tab) {
        if (tab == GalleryTab.ALBUMS) {
            val contents = mutableMapOf<Int, List<FileResponse>>()
            folders.forEach { folder ->
                val result = repository.getFiles(folder.id)
                if (result.isSuccess) {
                    contents[folder.id] = result.getOrThrow()
                }
            }
            albumContents = contents
        }
    }
    val visibleFiles = if (currentFolder == null) galleryFiles else files
    val photos = visibleFiles.filter { it.isMedia() }
    val otherFiles = visibleFiles.filterNot { it.isMedia() }
    val selectionMode = selectedIds.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                TextButton(onClick = { selectedIds = emptySet() }) {
                    Text("Cancel")
                }
                Text(
                    "${selectedIds.size} selected",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val selected = visibleFiles.filter { it.id in selectedIds }
                        if (selected.isNotEmpty()) {
                            scope.launch {
                                busy = true
                                error = null
                                message = null

                                val result = repository.downloadFilesToGallery(
                                    selected
                                ) { current, total, name ->
                                    progress = "Downloaded $current of $total: $name"
                                }

                                if (result.isSuccess) {
                                    message = "${selected.size} files saved to DCIM/KinGz."
                                    selectedIds = emptySet()
                                } else {
                                    error = result.exceptionOrNull()?.message
                                        ?: "Download failed."
                                }

                                busy = false
                                progress = ""
                            }
                        }
                    }
                ) {
                    Text("Save to phone")
                }
                TextButton(
                    enabled = !busy,
                    onClick = { confirmDeleteFiles = true }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onBack) { Text("‹") }
                Text(
                    currentFolder?.name ?: "Gallery",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { refresh() }) { Text("Refresh") }
            }
        }

        if (path.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    path = emptyList()
                    tab = GalleryTab.ALBUMS
                }) {
                    Text("Albums")
                }

                path.forEachIndexed { index, folder ->
                    TextButton(onClick = {
                        path = path.take(index + 1)
                        tab = GalleryTab.PHOTOS
                    }) {
                        Text(folder.name)
                    }
                }
            }
        }

        if (!selectionMode) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == GalleryTab.PHOTOS,
                    onClick = { tab = GalleryTab.PHOTOS },
                    text = { Text("Photos") }
                )
                Tab(
                    selected = tab == GalleryTab.ALBUMS,
                    onClick = { tab = GalleryTab.ALBUMS },
                    text = { Text("Albums") }
                )
                Tab(
                    selected = tab == GalleryTab.OTHER,
                    onClick = { tab = GalleryTab.OTHER },
                    text = { Text("Other") }
                )
            }
        }

        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                progress,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (tab) {
                GalleryTab.PHOTOS -> {
                    Box(Modifier.weight(1f)) {
                        if (photos.isEmpty()) {
                            EmptyGallery("No photos or videos yet.")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(112.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                gridItems(photos, key = { it.id }) { file ->
                                    val isSelected = file.id in selectedIds

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .combinedClickable(
                                                onClick = {
                                                    if (selectionMode) {
                                                        selectedIds =
                                                            if (isSelected) selectedIds - file.id
                                                            else selectedIds + file.id
                                                    } else {
                                                        viewerStartIndex = photos.indexOfFirst {
                                                            it.id == file.id
                                                        }.coerceAtLeast(0)
                                                        viewerOpen = true
                                                    }
                                                },
                                                onLongClick = {
                                                    selectedIds = selectedIds + file.id
                                                }
                                            )
                                    ) {
                                        if (file.isImage()) {
                                            AsyncImage(
                                                model = file.downloadUrl(),
                                                imageLoader = imageLoader,
                                                contentDescription = file.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "▶",
                                                        style = MaterialTheme.typography.headlineLarge
                                                    )
                                                }
                                            }
                                        }

                                        if (file.isVideo()) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(5.dp),
                                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    "VIDEO",
                                                    modifier = Modifier.padding(
                                                        horizontal = 5.dp,
                                                        vertical = 2.dp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp),
                                                shape = MaterialTheme.shapes.extraLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    "✓",
                                                    modifier = Modifier.padding(
                                                        horizontal = 7.dp,
                                                        vertical = 3.dp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!selectionMode) {
                            FloatingActionButton(
                                onClick = {
                                    uploadLauncher.launch(
                                        arrayOf("image/*", "video/*")
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(18.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }

                GalleryTab.ALBUMS -> {
                    Box(Modifier.weight(1f)) {
                        if (folders.isEmpty()) {
                            EmptyGallery("No albums yet. Create your first album.")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gridItems(folders, key = { it.id }) { folder ->
                                    val albumFiles = albumContents[folder.id].orEmpty()
                                    val cover = albumFiles.firstOrNull { it.isImage() }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    path = path + folder
                                                    tab = GalleryTab.PHOTOS
                                                },
                                                onLongClick = {
                                                    editingFolder = folder
                                                    renameValue = folder.name
                                                }
                                            )
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1.25f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (cover != null) {
                                                    AsyncImage(
                                                        model = cover.downloadUrl(),
                                                        imageLoader = imageLoader,
                                                        contentDescription = folder.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Surface(
                                                        modifier = Modifier.fillMaxSize(),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                "▣",
                                                                style = MaterialTheme.typography.displayMedium
                                                            )
                                                        }
                                                    }
                                                }

                                                if (albumFiles.any { it.isVideo() }) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp),
                                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                                        shape = MaterialTheme.shapes.small
                                                    ) {
                                                        Text(
                                                            "▶",
                                                            modifier = Modifier.padding(5.dp),
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }

                                            Column(Modifier.padding(10.dp)) {
                                                Text(
                                                    folder.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "${albumFiles.size} items",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                createName = ""
                                showCreate = true
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(18.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                GalleryTab.OTHER -> {
                    if (otherFiles.isEmpty()) {
                        EmptyGallery("No other files.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            gridItems(otherFiles, key = { it.id }) { file ->
                                val isSelected = file.id in selectedIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                selectedIds =
                                                    if (isSelected) selectedIds - file.id
                                                    else selectedIds + file.id
                                            },
                                            onLongClick = {
                                                selectedIds = selectedIds + file.id
                                            }
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("▤", style = MaterialTheme.typography.headlineMedium)
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(file.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${file.contentType} • ${formatFileSize(file.sizeBytes)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isSelected) Text("✓")
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewerOpen && photos.isNotEmpty()) {
        Dialog(
            onDismissRequest = { viewerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                val pagerState = rememberPagerState(
                    initialPage = viewerStartIndex.coerceIn(0, photos.lastIndex),
                    pageCount = { photos.size }
                )

                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${photos.size}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { viewerOpen = false }) {
                            Text("Close")
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        val file = photos[page]

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (file.isImage()) {
                                AsyncImage(
                                    model = file.downloadUrl(),
                                    imageLoader = imageLoader,
                                    contentDescription = file.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                key(file.id) {
                                    AndroidView(
                                        factory = { viewContext ->
                                            VideoView(viewContext).apply {
                                                val controller = MediaController(viewContext)
                                                controller.setAnchorView(this)
                                                setMediaController(controller)

                                                val headers = mapOf(
                                                    "Authorization" to repository.authorizationHeader()
                                                )
                                                setVideoURI(
                                                    Uri.parse(file.downloadUrl()),
                                                    headers
                                                )
                                                setOnPreparedListener { start() }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        photos.getOrNull(pagerState.currentPage)?.name.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (confirmDeleteFiles) {
        AlertDialog(
            onDismissRequest = { confirmDeleteFiles = false },
            title = { Text("Delete selected files?") },
            text = {
                Text("Permanently delete ${selectedIds.size} selected file(s) from the server?")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirmDeleteFiles = false
                        scope.launch {
                            busy = true
                            var deleted = 0
                            var failed = 0
                            selectedIds.toList().forEach { id ->
                                val result = repository.deleteFile(id)
                                if (result.isSuccess) deleted++ else failed++
                            }
                            selectedIds = emptySet()
                            message = "$deleted deleted; $failed failed."
                            busy = false
                            refresh()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFiles = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New album") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text("Album name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = createName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val result = repository.createFolder(
                                createName.trim(),
                                currentFolder?.id
                            )
                            error = result.exceptionOrNull()?.message
                            if (result.isSuccess) {
                                showCreate = false
                                refresh()
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { editingFolder = null },
            title = { Text("Rename album") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Album name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val result = repository.renameFolder(
                                folder.id,
                                renameValue.trim()
                            )
                            error = result.exceptionOrNull()?.message
                            if (result.isSuccess) {
                                editingFolder = null
                                refresh()
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            editingFolder = null
                            confirmDeleteFolder = folder
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { editingFolder = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    confirmDeleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { confirmDeleteFolder = null },
            title = { Text("Delete album?") },
            text = {
                Text("Delete '${folder.name}' and its contents permanently?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteFolder = null
                        scope.launch {
                            val result = repository.deleteFolder(folder.id)
                            error = result.exceptionOrNull()?.message
                            if (result.isSuccess) refresh()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyGallery(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatFileSize(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L ->
            "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }




