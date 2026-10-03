package com.KinGz.personal.ui.notes

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.KinGz.personal.data.ChecklistItem
import com.KinGz.personal.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class NoteViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val preferences = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val imageDirectory = File(
        appContext.filesDir,
        NOTES_IMAGE_DIRECTORY
    ).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val saveMutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        loadNotes()
    }

    fun addNote(
        title: String,
        content: String,
        checklist: List<ChecklistItem>,
        imagePaths: List<String>,
        colorKey: String
    ) {
        if (isBlankNote(title, content, checklist, imagePaths)) return

        val now = System.currentTimeMillis()
        val note = Note(
            id = now,
            title = title.trim(),
            content = content.trim(),
            checklist = sanitizeChecklist(checklist),
            imagePaths = imagePaths.distinct(),
            isPinned = false,
            isArchived = false,
            isTrashed = false,
            colorKey = normalizeColorKey(colorKey),
            createdAt = now,
            updatedAt = now
        )

        saveNotes(listOf(note) + _notes.value)
    }

    fun updateNote(
        note: Note,
        title: String,
        content: String,
        checklist: List<ChecklistItem>,
        imagePaths: List<String>,
        colorKey: String
    ) {
        if (isBlankNote(title, content, checklist, imagePaths)) return

        val cleanedPaths = imagePaths.distinct()
        val updatedNote = note.copy(
            title = title.trim(),
            content = content.trim(),
            checklist = sanitizeChecklist(checklist),
            imagePaths = cleanedPaths,
            colorKey = normalizeColorKey(colorKey),
            updatedAt = System.currentTimeMillis()
        )

        val updated = _notes.value.map {
            if (it.id == note.id) updatedNote else it
        }

        deleteRemovedImages(
            oldPaths = note.imagePaths,
            newPaths = cleanedPaths
        )

        saveNotes(updated)
    }

    fun togglePin(note: Note) {
        val updated = _notes.value.map {
            if (it.id == note.id) {
                it.copy(
                    isPinned = !it.isPinned,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        saveNotes(updated)
    }

    fun toggleArchive(note: Note) {
        val updated = _notes.value.map {
            if (it.id == note.id) {
                it.copy(
                    isArchived = !it.isArchived,
                    isTrashed = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        saveNotes(updated)
    }

    fun moveToTrash(note: Note) {
        val updated = _notes.value.map {
            if (it.id == note.id) {
                it.copy(
                    isTrashed = true,
                    isArchived = false,
                    isPinned = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        saveNotes(updated)
    }

    fun restoreFromTrash(note: Note) {
        val updated = _notes.value.map {
            if (it.id == note.id) {
                it.copy(
                    isTrashed = false,
                    isArchived = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        saveNotes(updated)
    }

    fun deleteForever(note: Note) {
        deleteImages(note.imagePaths)

        val updated = _notes.value.filterNot {
            it.id == note.id
        }

        saveNotes(updated)
    }

    fun emptyTrash() {
        val trashNotes = _notes.value.filter { it.isTrashed }
        trashNotes.forEach { deleteImages(it.imagePaths) }
        saveNotes(_notes.value.filterNot { it.isTrashed })
    }

    fun toggleChecklistItem(
        note: Note,
        itemId: Long
    ) {
        val updated = _notes.value.map { current ->
            if (current.id != note.id) {
                current
            } else {
                current.copy(
                    checklist = current.checklist.map { item ->
                        if (item.id == itemId) {
                            item.copy(isChecked = !item.isChecked)
                        } else {
                            item
                        }
                    },
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        saveNotes(updated)
    }

    /**
     * Copies the selected image into the app's private storage so the note
     * remains valid even if external URI permissions later change.
     */
    fun importImage(
        uri: Uri,
        onImported: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val importedPath = runCatching {
                val source = appContext.contentResolver.openInputStream(uri)
                    ?: error("Unable to open image")

                val target = File(
                    imageDirectory,
                    "note_${System.currentTimeMillis()}_${UUID.randomUUID()}.img"
                )

                source.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                target.absolutePath
            }.getOrNull()

            withContext(Dispatchers.Main) {
                if (importedPath != null) {
                    onImported(importedPath)
                } else {
                    onFailed()
                }
            }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = preferences.getString(KEY_NOTES, "[]") ?: "[]"
            val loaded = runCatching {
                parseNotes(json)
            }.getOrDefault(emptyList())

            withContext(Dispatchers.Main) {
                _notes.value = loaded
            }
        }
    }

    private fun saveNotes(notes: List<Note>) {
        val sorted = sortNotes(notes)
        _notes.value = sorted

        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock {
                preferences.edit()
                    .putString(
                        KEY_NOTES,
                        toJson(sorted).toString()
                    )
                    .commit()
            }
        }
    }

    private fun parseNotes(json: String): List<Note> {
        val array = JSONArray(json)
        val result = mutableListOf<Note>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue

            val id = item.optLong("id", 0L)
            if (id <= 0L) continue

            val title = item.optString("title", "")
            val content = item.optString("content", "")
            val createdAt = item.optLong("createdAt", id)
            val updatedAt = item.optLong("updatedAt", createdAt)

            val checklist = parseChecklist(
                item.optJSONArray("checklist")
            )

            val imagePaths = parseStringArray(
                item.optJSONArray("imagePaths")
            )

            if (isBlankNote(title, content, checklist, imagePaths)) {
                continue
            }

            result += Note(
                id = id,
                title = title,
                content = content,
                checklist = checklist,
                imagePaths = imagePaths,
                isPinned = item.optBoolean("isPinned", false),
                isArchived = item.optBoolean("isArchived", false),
                isTrashed = item.optBoolean("isTrashed", false),
                colorKey = normalizeColorKey(
                    item.optString("colorKey", "default")
                ),
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        return sortNotes(result)
    }

    private fun parseChecklist(
        array: JSONArray?
    ): List<ChecklistItem> {
        if (array == null) return emptyList()

        val result = mutableListOf<ChecklistItem>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optLong("id", 0L)
            val text = item.optString("text", "").trim()

            if (id <= 0L || text.isBlank()) continue

            result += ChecklistItem(
                id = id,
                text = text,
                isChecked = item.optBoolean("isChecked", false)
            )
        }

        return result
    }

    private fun parseStringArray(
        array: JSONArray?
    ): List<String> {
        if (array == null) return emptyList()

        val result = mutableListOf<String>()
        for (index in 0 until array.length()) {
            val value = array.optString(index, "").trim()
            if (value.isNotBlank()) {
                result += value
            }
        }

        return result.distinct()
    }

    private fun toJson(
        notes: List<Note>
    ): JSONArray {
        val array = JSONArray()

        notes.forEach { note ->
            array.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("isPinned", note.isPinned)
                    put("isArchived", note.isArchived)
                    put("isTrashed", note.isTrashed)
                    put("colorKey", note.colorKey)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)

                    val checklistArray = JSONArray()
                    note.checklist.forEach { item ->
                        checklistArray.put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("text", item.text)
                                put("isChecked", item.isChecked)
                            }
                        )
                    }
                    put("checklist", checklistArray)

                    val imageArray = JSONArray()
                    note.imagePaths.forEach { path ->
                        imageArray.put(path)
                    }
                    put("imagePaths", imageArray)
                }
            )
        }

        return array
    }

    private fun sortNotes(
        notes: List<Note>
    ): List<Note> {
        return notes.sortedWith(
            compareBy<Note> { it.isTrashed }
                .thenBy { it.isArchived }
                .thenByDescending { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }

    private fun sanitizeChecklist(
        checklist: List<ChecklistItem>
    ): List<ChecklistItem> {
        return checklist
            .map {
                it.copy(text = it.text.trim())
            }
            .filter { it.text.isNotBlank() }
            .distinctBy { it.id }
    }

    private fun normalizeColorKey(
        colorKey: String
    ): String {
        return when (colorKey.lowercase()) {
            "gray",
            "yellow",
            "green",
            "blue",
            "purple",
            "pink",
            "black" -> colorKey.lowercase()
            else -> "default"
        }
    }

    private fun isBlankNote(
        title: String,
        content: String,
        checklist: List<ChecklistItem>,
        imagePaths: List<String>
    ): Boolean {
        return title.isBlank() &&
                content.isBlank() &&
                checklist.none { it.text.isNotBlank() } &&
                imagePaths.isEmpty()
    }

    private fun deleteRemovedImages(
        oldPaths: List<String>,
        newPaths: List<String>
    ) {
        val removed = oldPaths.toSet() - newPaths.toSet()
        deleteImages(removed.toList())
    }

    private fun deleteImages(
        paths: List<String>
    ) {
        paths.forEach { path ->
            runCatching {
                val file = File(path)
                if (file.exists() && file.parentFile?.canonicalPath == imageDirectory.canonicalPath) {
                    file.delete()
                }
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "kingz_notes"
        const val KEY_NOTES = "notes_json"
        const val NOTES_IMAGE_DIRECTORY = "note_images"
    }
}
