package com.KinGz.personal.ui.notes

import android.content.Context

class NoteSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultColor(): String =
        prefs.getString(KEY_DEFAULT_COLOR, "default") ?: "default"

    fun setDefaultColor(value: String) {
        prefs.edit().putString(KEY_DEFAULT_COLOR, value).apply()
    }

    fun getConfirmTrash(): Boolean =
        prefs.getBoolean(KEY_CONFIRM_TRASH, true)

    fun setConfirmTrash(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_TRASH, value).apply()
    }

    fun getShowTimestamps(): Boolean =
        prefs.getBoolean(KEY_SHOW_TIMESTAMPS, true)

    fun setShowTimestamps(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIMESTAMPS, value).apply()
    }

    private companion object {
        const val PREFS_NAME = "kingz_note_settings"
        const val KEY_DEFAULT_COLOR = "default_note_color"
        const val KEY_CONFIRM_TRASH = "confirm_trash"
        const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
    }
}
