package com.KinGz.personal.network

import android.content.Context

class AuthRepository(context: Context) {

    private val api = RetrofitClient.api
    private val preferences = AuthPreferences(context.applicationContext)
    private val appContext = context.applicationContext

    suspend fun register(
        username: String,
        password: String
    ): Result<UserResponse> {
        return try {
            val cleanUsername = username.trim()

            val response = api.register(
                RegisterRequest(
                    username = cleanUsername,
                    password = password
                )
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            val user = response.body()
                ?: return Result.failure(
                    Exception("Registration returned no user data.")
                )

            val deviceResponse = api.bootstrapDevice(
                BootstrapDeviceRequest(
                    username = cleanUsername,
                    password = password,
                    deviceKey = DeviceIdentity.getDeviceKey(appContext),
                    name = "KinGz Android",
                    platform = "Android"
                )
            )

            if (!deviceResponse.isSuccessful) {
                return Result.failure(Exception(readError(deviceResponse)))
            }

            login(cleanUsername, password).fold(
                onSuccess = { Result.success(user) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        username: String,
        password: String
    ): Result<UserResponse> {
        return try {
            val response = api.login(
                username = username.trim(),
                password = password,
                deviceKey = DeviceIdentity.getDeviceKey(appContext)
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            val session = response.body()
                ?: return Result.failure(
                    Exception("Login returned no session data.")
                )

            preferences.saveSession(
                token = session.accessToken,
                username = session.user.username
            )

            Result.success(session.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollAdminDevice(
        username: String,
        password: String,
        enrollmentSecret: String
    ): Result<UserResponse> {
        return try {
            val cleanUsername = username.trim()

            if (enrollmentSecret.isBlank()) {
                return Result.failure(
                    Exception("Enter the admin enrollment secret.")
                )
            }

            val response = api.bootstrapDevice(
                BootstrapDeviceRequest(
                    username = cleanUsername,
                    password = password,
                    deviceKey = DeviceIdentity.getDeviceKey(appContext),
                    name = "KinGz Admin Android",
                    platform = "Android",
                    enrollmentSecret = enrollmentSecret.trim()
                )
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            // The server has authorized this device.
            // Now create a normal authenticated session.
            login(cleanUsername, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.getMe("Bearer $token")

            if (!response.isSuccessful) {
                if (response.code() == 401 ||
                    response.code() == 403
                ) {
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

    suspend fun adminListAllDevices(): Result<List<DeviceResponse>> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.adminListAllDevices("Bearer $token")

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no device data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRevokeDevice(deviceId: Int): Result<DeviceResponse> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.adminRevokeDevice(
                deviceId = deviceId,
                authorization = "Bearer $token"
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no device data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminReactivateDevice(
        deviceId: Int
    ): Result<DeviceResponse> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.adminReactivateDevice(
                deviceId = deviceId,
                authorization = "Bearer $token"
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no device data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun adminListSessions(): Result<List<AdminSessionResponse>> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.adminListSessions("Bearer $token")

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no session data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRevokeSession(
        sessionId: Int
    ): Result<AdminSessionRevokeResponse> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

            val response = api.adminRevokeSession(
                sessionId = sessionId,
                authorization = "Bearer $token"
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(readError(response)))
            }

            Result.success(
                response.body()
                    ?: throw Exception("Server returned no revocation status.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun adminDatabaseHealth(): Result<Map<String, Any>> {
        return try {
            val token = preferences.getToken()
                ?: return Result.failure(
                    Exception("No saved login session.")
                )

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

    fun isLoggedIn(): Boolean = preferences.isLoggedIn()

    fun getUsername(): String? = preferences.getUsername()

    private fun readError(response: retrofit2.Response<*>): String {
        val body = response.errorBody()?.string()

        return "HTTP ${response.code()}: " +
                (body ?: response.message().ifBlank { "Request failed" })
    }
}
