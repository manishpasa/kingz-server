
package com.KinGz.personal.network

import android.content.Context
import java.util.UUID

object DeviceIdentity {

    private const val PREFS_NAME = "kingz_device_identity"
    private const val KEY_DEVICE_ID = "device_key"

    fun getDeviceKey(context: Context): String {
        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val existingKey = preferences.getString(KEY_DEVICE_ID, null)

        if (!existingKey.isNullOrBlank()) {
            return existingKey
        }

        val newKey = UUID.randomUUID().toString() +
                UUID.randomUUID().toString()

        preferences.edit()
            .putString(KEY_DEVICE_ID, newKey)
            .apply()

        return newKey
    }
}
