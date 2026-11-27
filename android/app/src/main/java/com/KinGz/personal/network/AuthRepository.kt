package com.KinGz.personal.network

import android.content.Context

class AuthRepository(context: Context) {

    private val api = RetrofitClient.api
    private val preferences = AuthPreferences(context.applicationContext)

    suspend fun login(
        username: String,
        password: String
    ): Result<UserResponse> {
        return try {
            val response = api.login(
                mapOf(
                    "username" to username.trim(),
                    "password" to password
                )
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            val session = response.body()
                ?: return Result.failure(Exception("Login returned no session data."))

            preferences.saveSession(
                token = session.accessToken,
                username = session.user.username
            )

            Result.success(session.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(Exception("No saved login session."))

            val response = api.getMe("Bearer $token")

            if (!response.isSuccessful) {
                if (response.code() == 401 || response.code() == 403) {
                    preferences.clearSession()
                }
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no user data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDatabaseHealth(): Result<Map<String, Any>> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(Exception("No saved login session."))

            val response = api.adminDatabaseHealth("Bearer $token")

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no database status.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val token = preferences.getToken()

            if (token != null) {
                val response = api.logout("Bearer $token")

                if (!response.isSuccessful &&
                    response.code() != 401 &&
                    response.code() != 403
                ) {
                    return Result.failure(Exception(readError(response)))
                }
            }

            preferences.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Compatibility placeholders.
    // Device management was removed from the rebuilt backend.

    suspend fun adminListAllDevices(): Result<List<DeviceResponse>> =
        Result.failure(Exception("Device management is no longer available."))

    suspend fun adminRevokeDevice(deviceId: Int): Result<DeviceResponse> =
        Result.failure(Exception("Device management is no longer available."))

    suspend fun adminReactivateDevice(deviceId: Int): Result<DeviceResponse> =
        Result.failure(Exception("Device management is no longer available."))

    suspend fun adminListSessions(): Result<List<AdminSessionResponse>> =
        Result.failure(Exception("Session management is not available yet."))

    suspend fun adminRevokeSession(
        sessionId: Int
    ): Result<AdminSessionRevokeResponse> =
        Result.failure(Exception("Session management is not available yet."))

    fun isLoggedIn(): Boolean = preferences.isLoggedIn()

    fun getUsername(): String? = preferences.getUsername()

    private fun readError(response: retrofit2.Response<*>): String {
        val body = response.errorBody()?.string()
        return "HTTP ${response.code()}: " +
            (body ?: response.message().ifBlank { "Request failed" })
    }
}
