package com.KinGz.personal.network

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FileStorageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api = RetrofitClient.api
    private val preferences = AuthPreferences(appContext)

    fun authorizationHeader(): String {
        val token = preferences.getToken()
        if (token.isNullOrBlank()) {
            throw IllegalStateException("Please sign in to KinGz Server.")
        }
        return "Bearer $token"
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = null
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index)
            }
        }
        return name?.takeIf { it.isNotBlank() } ?: "upload"
    }

    private fun getFileSize(uri: Uri): Long {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) {
                    return cursor.getLong(index)
                }
            }
        }
        return -1L
    }

    private fun streamingRequestBody(uri: Uri, mimeType: String): RequestBody {
        val resolver = appContext.contentResolver
        val length = getFileSize(uri)

        return object : RequestBody() {
            override fun contentType(): MediaType? =
                mimeType.toMediaTypeOrNull()

            override fun contentLength(): Long = length

            override fun writeTo(sink: BufferedSink) {
                val input = resolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to read selected file.")
                input.source().use { source -> sink.writeAll(source) }
            }
        }
    }

    suspend fun getFolders(parentId: Int? = null): Result<List<FolderResponse>> =
        execute { api.getFolders(authorizationHeader(), parentId) }

    suspend fun createFolder(
        name: String,
        parentId: Int? = null,
        isPublic: Boolean = false
    ): Result<FolderResponse> = execute {
        api.createFolder(
            authorizationHeader(),
            CreateFolderRequest(name, parentId, isPublic)
        )
    }

    suspend fun renameFolder(
        folderId: Int,
        name: String
    ): Result<FileActionResponse> = execute {
        api.renameFolder(
            authorizationHeader(), folderId, RenameFolderRequest(name)
        )
    }

    suspend fun changeFolderVisibility(
        folderId: Int,
        isPublic: Boolean
    ): Result<FileActionResponse> = execute {
        api.changeFolderVisibility(
            authorizationHeader(), folderId, FolderVisibilityRequest(isPublic)
        )
    }

    suspend fun deleteFolder(folderId: Int): Result<FileActionResponse> =
        execute { api.deleteFolder(authorizationHeader(), folderId) }

    suspend fun getFiles(folderId: Int? = null): Result<List<FileResponse>> =
        execute { api.getFiles(authorizationHeader(), folderId) }

    suspend fun getGalleryFiles(): Result<List<FileResponse>> =
        execute { api.getGalleryFiles(authorizationHeader()) }

    suspend fun uploadFile(
        uri: Uri,
        folderId: Int? = null
    ): Result<FileResponse> = withContext(Dispatchers.IO) {
        try {
            val resolver = appContext.contentResolver
            val name = getFileName(uri)
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val part = MultipartBody.Part.createFormData(
                "file", name, streamingRequestBody(uri, mime)
            )
            val response = api.uploadFile(authorizationHeader(), part, folderId)

            if (!response.isSuccessful) {
                throw Exception(
                    "HTTP ${response.code()}: " +
                        (response.errorBody()?.string() ?: response.message())
                )
            }

            Result.success(
                response.body() ?: throw Exception("Server returned no file data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFiles(
        uris: List<Uri>,
        folderId: Int? = null,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<List<FileResponse>> = coroutineScope {
        val semaphore = Semaphore(3)
        val completed = AtomicInteger(0)

        val results = uris.map { uri ->
            async {
                semaphore.withPermit {
                    val result = uploadFile(uri, folderId)
                    val count = completed.incrementAndGet()
                    withContext(Dispatchers.Main) {
                        onProgress(count, uris.size, getFileName(uri))
                    }
                    result
                }
            }
        }.awaitAll()

        val failedIndex = results.indexOfFirst { it.isFailure }
        if (failedIndex >= 0) {
            val name = getFileName(uris[failedIndex])
            val reason = results[failedIndex].exceptionOrNull()?.message
                ?: "Upload failed."
            Result.failure(
                Exception("Upload failed for $name: $reason")
            )
        } else {
            Result.success(results.map { it.getOrThrow() })
        }
    }

    suspend fun downloadFile(fileId: Int): Result<ResponseBody> =
        execute { api.downloadFile(authorizationHeader(), fileId) }

    suspend fun downloadToCache(
        fileId: Int,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val body = downloadFile(fileId).getOrThrow()
            val extension = fileName.substringAfterLast('.', "")
                .lowercase()
                .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
                ?.let { ".$it" } ?: ".media"

            val target = File(appContext.cacheDir, "gallery_$fileId$extension")
            body.use { response ->
                response.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadToGallery(
        file: FileResponse
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var savedUri: Uri? = null
        try {
            val resolver = appContext.contentResolver
            val isVideo = file.contentType.startsWith("video/", true)
            val collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, file.contentType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DCIM}/KinGz"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM
                        ), "KinGz"
                    )
                    if (!directory.exists()) directory.mkdirs()
                    put(
                        MediaStore.MediaColumns.DATA,
                        File(directory, file.name).absolutePath
                    )
                }
            }

            savedUri = resolver.insert(collection, values)
                ?: throw Exception("Unable to create the KinGz gallery file.")

            val body = downloadFile(file.id).getOrThrow()
            body.use { response ->
                val output = resolver.openOutputStream(savedUri!!, "w")
                    ?: throw Exception("Unable to write the downloaded file.")
                response.byteStream().use { input ->
                    output.use { stream -> input.copyTo(stream) }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val complete = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(savedUri!!, complete, null, null)
            }

            Result.success(savedUri!!)
        } catch (e: Exception) {
            savedUri?.let { resolverDelete(it) }
            Result.failure(e)
        }
    }

    suspend fun downloadFilesToGallery(
        files: List<FileResponse>,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<List<Uri>> = coroutineScope {
        val semaphore = Semaphore(3)
        val completed = AtomicInteger(0)

        val results = files.map { file ->
            async {
                semaphore.withPermit {
                    val result = downloadToGallery(file)
                    val count = completed.incrementAndGet()
                    withContext(Dispatchers.Main) {
                        onProgress(count, files.size, file.name)
                    }
                    result
                }
            }
        }.awaitAll()

        val failed = results.indexOfFirst { it.isFailure }
        if (failed >= 0) {
            Result.failure(
                Exception(
                    "Download failed for ${files[failed].name}: " +
                        (results[failed].exceptionOrNull()?.message ?: "Unknown error")
                )
            )
        } else {
            Result.success(results.map { it.getOrThrow() })
        }
    }

    private fun resolverDelete(uri: Uri) {
        runCatching { appContext.contentResolver.delete(uri, null, null) }
    }

    suspend fun moveFile(
        fileId: Int,
        folderId: Int?
    ): Result<FileActionResponse> = execute {
        api.moveFile(
            authorizationHeader(), fileId, MoveFileRequest(folderId)
        )
    }

    suspend fun deleteFile(fileId: Int): Result<FileActionResponse> =
        execute { api.deleteFile(authorizationHeader(), fileId) }

    private suspend fun <T : Any> execute(
        request: suspend () -> retrofit2.Response<T>
    ): Result<T> {
        return try {
            val response = request()
            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        "HTTP ${response.code()}: " +
                            (response.errorBody()?.string() ?: response.message())
                    )
                )
            }
            Result.success(
                response.body() ?: throw Exception("Server returned no data.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
