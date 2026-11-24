
package com.KinGz.personal

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object LocalPatternStore {

    private const val PREFS_NAME = "kingz_local_lock"
    private const val KEY_SALT = "pattern_salt"
    private const val KEY_HASH = "pattern_hash"

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun isPatternSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return !prefs.getString(KEY_SALT, null).isNullOrBlank() &&
                !prefs.getString(KEY_HASH, null).isNullOrBlank()
    }

    fun savePattern(
        context: Context,
        pattern: List<Int>
    ): Boolean {
        if (!isValidPattern(pattern)) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = deriveHash(pattern, salt)

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.edit()
            .putString(
                KEY_SALT,
                Base64.encodeToString(salt, Base64.NO_WRAP)
            )
            .putString(
                KEY_HASH,
                Base64.encodeToString(hash, Base64.NO_WRAP)
            )
            .commit()
    }

    fun verifyPattern(
        context: Context,
        pattern: List<Int>
    ): Boolean {
        if (!isValidPattern(pattern)) return false

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val saltString = prefs.getString(KEY_SALT, null)
            ?: return false

        val hashString = prefs.getString(KEY_HASH, null)
            ?: return false

        return try {
            val salt = Base64.decode(
                saltString,
                Base64.NO_WRAP
            )

            val savedHash = Base64.decode(
                hashString,
                Base64.NO_WRAP
            )

            val enteredHash = deriveHash(pattern, salt)

            MessageDigest.isEqual(savedHash, enteredHash)
        } catch (_: Exception) {
            false
        }
    }

    fun removePattern(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .commit()
    }

    private fun isValidPattern(
        pattern: List<Int>
    ): Boolean {
        return pattern.size >= 4 &&
                pattern.size <= 9 &&
                pattern.all { it in 0..8 } &&
                pattern.distinct().size == pattern.size
    }

    private fun deriveHash(
        pattern: List<Int>,
        salt: ByteArray
    ): ByteArray {
        val chars = pattern.joinToString(",").toCharArray()

        val spec = PBEKeySpec(
            chars,
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}

