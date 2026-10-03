package com.KinGz.personal.data

/**
 * A single checklist row inside a note.
 */
data class ChecklistItem(
    val id: Long,
    val text: String,
    val isChecked: Boolean = false
)

/**
 * Local-first Keep-style note.
 *
 * Older V1 JSON notes remain compatible because all new fields have defaults.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val checklist: List<ChecklistItem> = emptyList(),
    val imagePaths: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val colorKey: String = "default",
    val createdAt: Long,
    val updatedAt: Long
)
