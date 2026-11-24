
package com.KinGz.personal.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface KinGzApi {

    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<UserResponse>

    @POST("api/devices/bootstrap")
    suspend fun bootstrapDevice(
        @Body request: BootstrapDeviceRequest
    ): Response<DeviceResponse>


    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Header("X-Device-Key") deviceKey: String
    ): Response<LoginResponse>


    @GET("api/auth/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<Map<String, String>>

    @GET("api/devices/admin/all")
    suspend fun adminListAllDevices(
        @Header("Authorization") authorization: String
    ): Response<List<DeviceResponse>>

    @POST("api/devices/admin/{device_id}/revoke")
    suspend fun adminRevokeDevice(
        @Path("device_id") deviceId: Int,
        @Header("Authorization") authorization: String
    ): Response<DeviceResponse>

    @POST("api/devices/admin/{device_id}/reactivate")
    suspend fun adminReactivateDevice(
        @Path("device_id") deviceId: Int,
        @Header("Authorization") authorization: String
    ): Response<DeviceResponse>


    @GET("api/admin/database/health")
    suspend fun adminDatabaseHealth(
        @Header("Authorization") authorization: String
    ): Response<Map<String, Any>>
    @GET("api/devices/admin/sessions")
    suspend fun adminListSessions(
        @Header("Authorization") authorization: String
    ): Response<List<AdminSessionResponse>>

    @POST("api/devices/admin/sessions/{session_id}/revoke")
    suspend fun adminRevokeSession(
        @Path("session_id") sessionId: Int,
        @Header("Authorization") authorization: String
    ): Response<AdminSessionRevokeResponse>
}