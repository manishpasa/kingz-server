
package com.KinGz.personal.network

import com.google.gson.annotations.SerializedName

data class FolderResponse(
    val id: Int,
    val name: String,
    @SerializedName("parent_id")
    val parentId: Int?,
    @SerializedName("is_public")
    val isPublic: Boolean,
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("is_owner")
    val isOwner: Boolean
)

data class FileResponse(
    val id: Int,
    val name: String,
    @SerializedName("folder_id")
    val folderId: Int?,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("size_bytes")
    val sizeBytes: Long,
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("is_owner")
    val isOwner: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class CreateFolderRequest(
    val name: String,
    @SerializedName("parent_id")
    val parentId: Int? = null,
    @SerializedName("is_public")
    val isPublic: Boolean = false
)

data class RenameFolderRequest(
    val name: String
)

data class FolderVisibilityRequest(
    @SerializedName("is_public")
    val isPublic: Boolean
)

data class MoveFileRequest(
    @SerializedName("folder_id")
    val folderId: Int?
)

data class FileActionResponse(
    val message: String,
    @SerializedName("is_public")
    val isPublic: Boolean? = null,
    @SerializedName("folder_id")
    val folderId: Int? = null
)
