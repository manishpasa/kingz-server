
package com.KinGz.personal.network

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_admin")
    val isAdmin: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    val user: UserResponse
)

data class BootstrapDeviceRequest(
    val username: String,
    val password: String,
    @SerializedName("device_key")
    val deviceKey: String,
    val name: String,
    val platform: String,
    @SerializedName("enrollment_secret")
    val enrollmentSecret: String? = null
)

data class DeviceResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("device_key")
    val deviceKey: String,
    val name: String,
    val platform: String,
    @SerializedName("is_authorized")
    val isAuthorized: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("last_seen_at")
    val lastSeenAt: String?,
    @SerializedName("revoked_at")
    val revokedAt: String?
)
data class ServerHealthResponse(
    val status: String? = null
)
data class AdminSessionResponse(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    val username: String,

    @SerializedName("device_id")
    val deviceId: Int,

    @SerializedName("device_name")
    val deviceName: String,

    val platform: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("expires_at")
    val expiresAt: String,

    @SerializedName("revoked_at")
    val revokedAt: String?,

    val status: String
)

data class AdminSessionRevokeResponse(
    val status: String,

    @SerializedName("session_id")
    val sessionId: Int
)