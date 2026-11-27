
package com.KinGz.personal.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface KinGzApi {

    // Health
    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    // Authentication
    @POST("api/auth/login")
    suspend fun login(
        @Body request: Map<String, String>
    ): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<Map<String, String>>

    // Admin
    @GET("api/admin/database/health")
    suspend fun adminDatabaseHealth(
        @Header("Authorization") authorization: String
    ): Response<Map<String, Any>>

    // Folders
    @GET("api/files/folders")
    suspend fun getFolders(
        @Header("Authorization") authorization: String,
        @Query("parent_id") parentId: Int? = null
    ): Response<List<FolderResponse>>

    @POST("api/files/folders")
    suspend fun createFolder(
        @Header("Authorization") authorization: String,
        @Body request: CreateFolderRequest
    ): Response<FolderResponse>

    @PATCH("api/files/folders/{folder_id}")
    suspend fun renameFolder(
        @Header("Authorization") authorization: String,
        @Path("folder_id") folderId: Int,
        @Body request: RenameFolderRequest
    ): Response<FileActionResponse>

    @PATCH("api/files/folders/{folder_id}/visibility")
    suspend fun changeFolderVisibility(
        @Header("Authorization") authorization: String,
        @Path("folder_id") folderId: Int,
        @Body request: FolderVisibilityRequest
    ): Response<FileActionResponse>

    @DELETE("api/files/folders/{folder_id}")
    suspend fun deleteFolder(
        @Header("Authorization") authorization: String,
        @Path("folder_id") folderId: Int
    ): Response<FileActionResponse>

    // Files
    @GET("api/files")
    suspend fun getFiles(
        @Header("Authorization") authorization: String,
        @Query("folder_id") folderId: Int? = null
    ): Response<List<FileResponse>>

    @GET("api/files/gallery")
    suspend fun getGalleryFiles(
        @Header("Authorization") authorization: String
    ): Response<List<FileResponse>>
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Query("folder_id") folderId: Int? = null
    ): Response<FileResponse>

    @Streaming
    @GET("api/files/{file_id}/download")
    suspend fun downloadFile(
        @Header("Authorization") authorization: String,
        @Path("file_id") fileId: Int
    ): Response<ResponseBody>

    @PATCH("api/files/{file_id}/move")
    suspend fun moveFile(
        @Header("Authorization") authorization: String,
        @Path("file_id") fileId: Int,
        @Body request: MoveFileRequest
    ): Response<FileActionResponse>

    @DELETE("api/files/{file_id}")
    suspend fun deleteFile(
        @Header("Authorization") authorization: String,
        @Path("file_id") fileId: Int
    ): Response<FileActionResponse>
}
