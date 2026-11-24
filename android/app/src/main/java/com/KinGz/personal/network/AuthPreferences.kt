
package com.KinGz.personal.network

import android.content.Context

class AuthPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        "kingz_auth_preferences",
        Context.MODE_PRIVATE
    )

    fun saveSession(token: String, username: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_TOKEN, null)
    }

    fun getUsername(): String? {
        return preferences.getString(KEY_USERNAME, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USERNAME = "username"
    }
}
