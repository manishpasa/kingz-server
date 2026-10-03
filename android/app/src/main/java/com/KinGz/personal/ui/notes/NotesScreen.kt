package com.KinGz.personal.ui.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.KinGz.personal.data.ChecklistItem
import com.KinGz.personal.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotesFilter {
    ALL,
    PINNED,
    ARCHIVED,
    TRASH
}

private data class DraftChecklistItem(
    val id: Long,
    val text: String,
    val isChecked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current
    val settingsStore = remember { NoteSettingsStore(context) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(NotesFilter.ALL) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var creatingNote by rememberSaveable { mutableStateOf(false) }
    var deletingNote by remember { mutableStateOf<Note?>(null) }
    var deletingForeverNote by remember { mutableStateOf<Note?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var defaultNoteColor by rememberSaveable { mutableStateOf(settingsStore.getDefaultColor()) }
    var confirmTrash by rememberSaveable { mutableStateOf(settingsStore.getConfirmTrash()) }
    var showTimestamps by rememberSaveable { mutableStateOf(settingsStore.getShowTimestamps()) }

    val editorOpen = creatingNote || editingNote != null

    BackHandler(enabled = editorOpen) {
        creatingNote = false
        editingNote = null
    }

    if (editorOpen) {
        NoteEditorScreen(
            initialNote = editingNote,
            defaultColorKey = defaultNoteColor,
            viewModel = viewModel,
            onBack = {
                creatingNote = false
                editingNote = null
            },
            onSave = { title, content, checklist, imagePaths, colorKey ->
                if (editingNote == null) {
                    viewModel.addNote(
                        title = title,
                        content = content,
                        checklist = checklist,
                        imagePaths = imagePaths,
                        colorKey = colorKey
                    )
                } else {
                    viewModel.updateNote(
                        note = editingNote!!,
                        title = title,
                        content = content,
                        checklist = checklist,
                        imagePaths = imagePaths,
                        colorKey = colorKey
                    )
                }

                creatingNote = false
                editingNote = null
            }
        )
        return
    }

    val trimmedQuery = searchQuery.trim()

    val visibleNotes = notes.filter { note ->
        val matchesSearch = trimmedQuery.isBlank() ||
                note.title.contains(trimmedQuery, ignoreCase = true) ||
                note.content.contains(trimmedQuery, ignoreCase = true) ||
                note.checklist.any {
                    it.text.contains(trimmedQuery, ignoreCase = true)
                }

        val matchesFilter = when (filter) {
            NotesFilter.ALL -> !note.isArchived && !note.isTrashed
            NotesFilter.PINNED ->
                note.isPinned && !note.isArchived && !note.isTrashed
            NotesFilter.ARCHIVED -> note.isArchived && !note.isTrashed
            NotesFilter.TRASH -> note.isTrashed
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            NotesHeader(
                filter = filter,
                noteCount = visibleNotes.size,
                onBack = onBack,
                onSettings = { showSettings = true }
            )
        },
        floatingActionButton = {
            if (filter != NotesFilter.TRASH) {
                FloatingActionButton(
                    onClick = { creatingNote = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New note"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                placeholder = { Text("Search notes, lists or text") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteFilterChip("All", filter == NotesFilter.ALL) {
                    filter = NotesFilter.ALL
                }
                NoteFilterChip("Pinned", filter == NotesFilter.PINNED) {
                    filter = NotesFilter.PINNED
                }
                NoteFilterChip("Archive", filter == NotesFilter.ARCHIVED) {
                    filter = NotesFilter.ARCHIVED
                }
                NoteFilterChip("Trash", filter == NotesFilter.TRASH) {
                    filter = NotesFilter.TRASH
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (visibleNotes.isEmpty()) {
                EmptyNotesV2(
                    filter = filter,
                    hasSearch = trimmedQuery.isNotBlank(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    items(
                        items = visibleNotes,
                        key = { "note_${it.id}" }
                    ) { note ->
                        NoteCardV2(
                            note = note,
                            onOpen = {
                                editingNote = note
                            },
                            onPin = {
                                viewModel.togglePin(note)
                            },
                            onArchive = {
                                viewModel.toggleArchive(note)
                            },
                            onTrash = {
                                if (note.isTrashed) {
                                    viewModel.deleteForever(note)
                                } else if (confirmTrash) {
                                    deletingNote = note
                                } else {
                                    viewModel.moveToTrash(note)
                                }
                            },
                            onRestore = {
                                viewModel.restoreFromTrash(note)
                            },
                            onDeleteForever = {
                                deletingForeverNote = note
                            },
                            showTimestamp = showTimestamps,
                            onToggleChecklist = { itemId ->
                                viewModel.toggleChecklistItem(note, itemId)
                            }
                        )
                    }
                }
            }
        }
    }

    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text("Move to Trash?") },
            text = {
                Text(
                    "\"${note.title.ifBlank { "Untitled note" }}\" will stay in Trash until you permanently delete it."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.moveToTrash(note)
                        deletingNote = null
                    }
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingNote = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deletingForeverNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingForeverNote = null },
            title = { Text("Delete permanently?") },
            text = {
                Text("This permanently removes the note and its stored images.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteForever(note)
                        deletingForeverNote = null
                    }
                ) {
                    Text("Delete forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingForeverNote = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showSettings) {
        NoteSettingsDialog(
            defaultColor = defaultNoteColor,
            confirmTrash = confirmTrash,
            showTimestamps = showTimestamps,
            trashCount = notes.count { it.isTrashed },
            onDefaultColorChange = {
                defaultNoteColor = it
                settingsStore.setDefaultColor(it)
            },
            onConfirmTrashChange = {
                confirmTrash = it
                settingsStore.setConfirmTrash(it)
            },
            onShowTimestampsChange = {
                showTimestamps = it
                settingsStore.setShowTimestamps(it)
            },
            onEmptyTrash = { viewModel.emptyTrash() },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun NotesHeader(
    filter: NotesFilter,
    noteCount: Int,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (filter) {
                    NotesFilter.ALL -> "Notes"
                    NotesFilter.PINNED -> "Pinned"
                    NotesFilter.ARCHIVED -> "Archive"
                    NotesFilter.TRASH -> "Trash"
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$noteCount ${if (noteCount == 1) "note" else "notes"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Note settings")
        }
    }
}

@Composable
private fun NoteSettingsDialog(
    defaultColor: String,
    confirmTrash: Boolean,
    showTimestamps: Boolean,
    trashCount: Int,
    onDefaultColorChange: (String) -> Unit,
    onConfirmTrashChange: (Boolean) -> Unit,
    onShowTimestampsChange: (Boolean) -> Unit,
    onEmptyTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmEmptyTrash by rememberSaveable { mutableStateOf(false) }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("Empty trash?") },
            text = { Text("Permanently delete $trashCount ${if (trashCount == 1) "note" else "notes"} and their stored images?") },
            confirmButton = {
                TextButton(onClick = {
                    onEmptyTrash()
                    confirmEmptyTrash = false
                }) { Text("Empty trash") }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Default note color", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mutedNotePalette().forEach { (key, bg, _) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(
                                    if (key == defaultColor) 3.dp else 1.dp,
                                    if (key == defaultColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                                .clickable { onDefaultColorChange(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == defaultColor) Text("✓", color = noteTextColor(key))
                        }
                    }
                }

                SettingSwitchRow(
                    title = "Ask before moving to Trash",
                    subtitle = "Prevent accidental deletion",
                    checked = confirmTrash,
                    onCheckedChange = onConfirmTrashChange
                )

                SettingSwitchRow(
                    title = "Show timestamps",
                    subtitle = "Show last-updated time on note cards",
                    checked = showTimestamps,
                    onCheckedChange = onShowTimestampsChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trash", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "$trashCount ${if (trashCount == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { confirmEmptyTrash = true },
                        enabled = trashCount > 0
                    ) { Text("Empty") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NoteFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
private fun EmptyNotesV2(
    filter: NotesFilter,
    hasSearch: Boolean,
    modifier: Modifier = Modifier
) {
    val title = when {
        hasSearch -> "No matching notes"
        filter == NotesFilter.PINNED -> "No pinned notes"
        filter == NotesFilter.ARCHIVED -> "No archived notes"
        filter == NotesFilter.TRASH -> "Trash is empty"
        else -> "No notes yet"
    }

    val message = when {
        hasSearch -> "Try a different search term."
        filter == NotesFilter.PINNED -> "Pin a note and it will appear here."
        filter == NotesFilter.ARCHIVED -> "Archived notes will appear here."
        filter == NotesFilter.TRASH -> "Deleted notes will appear here."
        else -> "Tap + to create your first note."
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (filter) {
                    NotesFilter.TRASH -> Icons.Default.Delete
                    NotesFilter.ARCHIVED -> Icons.Default.Archive
                    NotesFilter.PINNED -> Icons.Default.PushPin
                    NotesFilter.ALL -> if (hasSearch) Icons.Default.Search else Icons.Default.Checklist
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun mutedNotePalette(): List<Triple<String, Color, Color>> = listOf(
    Triple("default", Color(0xFFF7F7F4), Color(0xFF202124)),
    Triple("gray", Color(0xFFE7E8E5), Color(0xFF202124)),
    Triple("yellow", Color(0xFFF2EEDC), Color(0xFF202124)),
    Triple("green", Color(0xFFE2ECE4), Color(0xFF202124)),
    Triple("blue", Color(0xFFE2E8F0), Color(0xFF202124)),
    Triple("purple", Color(0xFFE9E4EC), Color(0xFF202124)),
    Triple("pink", Color(0xFFF0E5E7), Color(0xFF202124)),
    Triple("black", Color(0xFF303236), Color.White)
)

private fun noteColor(colorKey: String): Color =
    mutedNotePalette().firstOrNull { it.first == colorKey.lowercase() }?.second
        ?: mutedNotePalette().first().second

private fun noteTextColor(colorKey: String): Color =
    mutedNotePalette().firstOrNull { it.first == colorKey.lowercase() }?.third
        ?: Color(0xFF202124)

private fun noteSecondaryColor(colorKey: String): Color =
    if (colorKey.equals("black", ignoreCase = true)) Color(0xFFD6D7DB) else Color(0xFF5F6368)

private fun formatNoteTime(millis: Long): String {
    val formatter = SimpleDateFormat(
        "MMM d, h:mm a",
        Locale.getDefault()
    )
    return formatter.format(Date(millis))
}

@Composable
private fun NoteCardV2(
    note: Note,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    showTimestamp: Boolean,
    onToggleChecklist: (Long) -> Unit
) {
    val background = noteColor(note.colorKey)
    val primaryText = noteTextColor(note.colorKey)
    val secondaryText = noteSecondaryColor(note.colorKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (note.imagePaths.isNotEmpty()) {
                NoteImageStrip(
                    imagePaths = note.imagePaths,
                    height = 120.dp
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryText
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryText,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.checklist.isNotEmpty()) {
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.checklist.take(5).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleChecklist(item.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) {
                                        Icons.Default.CheckBox
                                    } else {
                                        Icons.Default.CheckBoxOutlineBlank
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = primaryText
                                )
                            }

                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryText,
                                textDecoration = if (item.isChecked) {
                                    TextDecoration.LineThrough
                                } else {
                                    TextDecoration.None
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (note.checklist.size > 5) {
                        Text(
                            text = "+${note.checklist.size - 5} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
            }

            if (showTimestamp) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTimestamp) {
                    Text(
                        text = formatNoteTime(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(17.dp),
                        tint = primaryText
                    )
                }

                if (note.isTrashed) {
                    IconButton(onClick = onRestore) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = primaryText
                        )
                    }
                    IconButton(onClick = onDeleteForever) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete forever",
                            tint = primaryText
                        )
                    }
                } else {
                    IconButton(onClick = onPin) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (note.isPinned) {
                                "Unpin"
                            } else {
                                "Pin"
                            },
                            tint = primaryText
                        )
                    }
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = if (note.isArchived) {
                                Icons.Default.Unarchive
                            } else {
                                Icons.Default.Archive
                            },
                            contentDescription = if (note.isArchived) {
                                "Unarchive"
                            } else {
                                "Archive"
                            },
                            tint = primaryText
                        )
                    }
                    IconButton(onClick = onTrash) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Move to Trash"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteImageStrip(
    imagePaths: List<String>,
    height: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val visible = imagePaths.take(3)

        visible.forEachIndexed { index, path ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                LocalNoteImage(
                    path = path,
                    modifier = Modifier.fillMaxSize()
                )

                if (index == 2 && imagePaths.size > 3) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${imagePaths.size - 3}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalNoteImage(
    path: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = path
    ) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Note image",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    initialNote: Note?,
    defaultColorKey: String,
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onSave: (
        String,
        String,
        List<ChecklistItem>,
        List<String>,
        String
    ) -> Unit
) {
    var title by rememberSaveable(initialNote?.id) {
        mutableStateOf(initialNote?.title.orEmpty())
    }

    var content by rememberSaveable(initialNote?.id) {
        mutableStateOf(initialNote?.content.orEmpty())
    }

    var showChecklist by rememberSaveable(initialNote?.id) {
        mutableStateOf(initialNote?.checklist?.isNotEmpty() == true)
    }

    var colorKey by rememberSaveable(initialNote?.id) {
        mutableStateOf(initialNote?.colorKey ?: defaultColorKey)
    }

    val checklistItems = remember(initialNote?.id) {
        mutableStateListOf<DraftChecklistItem>().apply {
            initialNote?.checklist?.forEach { item ->
                add(
                    DraftChecklistItem(
                        id = item.id,
                        text = item.text,
                        isChecked = item.isChecked
                    )
                )
            }
        }
    }

    val imagePaths = remember(initialNote?.id) {
        mutableStateListOf<String>().apply {
            addAll(initialNote?.imagePaths.orEmpty())
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        uris.forEach { uri ->
            viewModel.importImage(
                uri = uri,
                onImported = { path ->
                    if (!imagePaths.contains(path)) {
                        imagePaths += path
                    }
                }
            )
        }
    }

    var isSaving by rememberSaveable { mutableStateOf(false) }

    val effectiveColor = editorColor(colorKey)
    val editorTextColor = noteTextColor(colorKey)

    Scaffold(
        containerColor = effectiveColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (!isSaving) onBack()
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = editorTextColor)
                }

                Text(
                    text = if (initialNote == null) "New note" else "Edit note",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = editorTextColor,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                            if (isSaving) return@TextButton
                            isSaving = true

                            val cleanedChecklist = checklistItems
                                .map {
                                    ChecklistItem(
                                        id = it.id,
                                        text = it.text.trim(),
                                        isChecked = it.isChecked
                                    )
                                }
                                .filter { it.text.isNotBlank() }

                            onSave(
                                title,
                                content,
                                cleanedChecklist,
                                imagePaths.toList(),
                                colorKey
                            )
                        },
                        enabled = !isSaving
                    ) {
                        Text("Done", color = editorTextColor)
                    }
                }
            }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title", color = editorTextColor.copy(alpha = 0.65f)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = editorTextColor),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = editorTextColor,
                    unfocusedTextColor = editorTextColor,
                    focusedBorderColor = editorTextColor.copy(alpha = 0.55f),
                    unfocusedBorderColor = editorTextColor.copy(alpha = 0.35f),
                    cursorColor = editorTextColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("Take a note...", color = editorTextColor.copy(alpha = 0.65f)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = editorTextColor),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = editorTextColor,
                    unfocusedTextColor = editorTextColor,
                    focusedBorderColor = editorTextColor.copy(alpha = 0.55f),
                    unfocusedBorderColor = editorTextColor.copy(alpha = 0.35f),
                    cursorColor = editorTextColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showChecklist = !showChecklist
                        if (showChecklist && checklistItems.isEmpty()) {
                            checklistItems += DraftChecklistItem(
                                id = System.currentTimeMillis(),
                                text = "",
                                isChecked = false
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Checklist")
                }

                OutlinedButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pictures")
                }
            }

            if (showChecklist) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Checklist",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = editorTextColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    checklistItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    checklistItems[index] = item.copy(
                                        isChecked = !item.isChecked
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) {
                                        Icons.Default.CheckBox
                                    } else {
                                        Icons.Default.CheckBoxOutlineBlank
                                    },
                                    contentDescription = "Toggle checklist item"
                                )
                            }

                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    checklistItems[index] = item.copy(
                                        text = newText
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("List item", color = editorTextColor.copy(alpha = 0.65f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = editorTextColor,
                                    textDecoration = if (item.isChecked) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                ),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = editorTextColor,
                                    unfocusedTextColor = editorTextColor,
                                    focusedBorderColor = editorTextColor.copy(alpha = 0.55f),
                                    unfocusedBorderColor = editorTextColor.copy(alpha = 0.35f),
                                    cursorColor = editorTextColor
                                )
                            )

                            IconButton(
                                onClick = {
                                    checklistItems.removeAt(index)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove item"
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            checklistItems += DraftChecklistItem(
                                id = System.currentTimeMillis() + checklistItems.size,
                                text = "",
                                isChecked = false
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add item")
                    }
                }
            }

            if (imagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pictures",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = editorTextColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imagePaths.forEach { path ->
                        Box(
                            modifier = Modifier.size(110.dp)
                        ) {
                            LocalNoteImage(
                                path = path,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                            )

                            IconButton(
                                onClick = { imagePaths.remove(path) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove picture",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Note color",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = editorTextColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val palette = mutedNotePalette().map { (key, color, _) -> key to color }

                palette.forEach { (key, color) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (key == colorKey) 3.dp else 1.dp,
                                color = if (key == colorKey) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            )
                            .clickable { colorKey = key },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == colorKey) {
                            Text(
                                text = "✓",
                                fontWeight = FontWeight.Bold,
                                color = noteTextColor(key)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Stored locally on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = editorTextColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun editorColor(
    colorKey: String
): Color {
    return when (colorKey.lowercase()) {
        "black" -> Color(0xFF303236)
        "gray" -> Color(0xFFE7E8E5)
        "yellow" -> Color(0xFFF2EEDC)
        "green" -> Color(0xFFE2ECE4)
        "blue" -> Color(0xFFE2E8F0)
        "purple" -> Color(0xFFE9E4EC)
        "pink" -> Color(0xFFF0E5E7)
        else -> MaterialTheme.colorScheme.surface
    }
}
