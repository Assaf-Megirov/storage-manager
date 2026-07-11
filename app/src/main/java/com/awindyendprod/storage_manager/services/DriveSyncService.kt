package com.awindyendprod.storage_manager.services

import com.google.gson.Gson
import com.google.gson.JsonParseException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

const val SYNC_FILE_NAME = "sync_data.json"

data class DriveFileMeta(val id: String, val modifiedTime: String)

sealed class DriveResult<out T> {
    data class Success<T>(val value: T) : DriveResult<T>()
    object AuthExpired : DriveResult<Nothing>()
    object NetworkError : DriveResult<Nothing>()
    data class ServerError(val code: Int) : DriveResult<Nothing>()
}

class DriveSyncService {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun findSyncFile(accessToken: String): DriveResult<DriveFileMeta?> {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name='$SYNC_FILE_NAME'")
            .addQueryParameter("fields", "files(id,modifiedTime)")
            .build()
        val request = authorizedRequest(url.toString(), accessToken).get().build()

        return when (val result = executeRaw(request)) {
            is DriveResult.Success -> try {
                DriveResult.Success(gson.fromJson(result.value, FilesListResponse::class.java)?.files?.firstOrNull())
            } catch (e: JsonParseException) {
                DriveResult.ServerError(0)
            }
            is DriveResult.AuthExpired -> result
            is DriveResult.NetworkError -> result
            is DriveResult.ServerError -> result
        }
    }

    suspend fun downloadSyncFile(accessToken: String, fileId: String): DriveResult<String> {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = authorizedRequest(url, accessToken).get().build()
        return executeRaw(request)
    }

    suspend fun deleteSyncFile(accessToken: String, fileId: String): DriveResult<Unit> {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId"
        val request = authorizedRequest(url, accessToken).delete().build()
        return when (val result = executeRaw(request)) {
            is DriveResult.Success -> DriveResult.Success(Unit)
            is DriveResult.AuthExpired -> result
            is DriveResult.NetworkError -> result
            is DriveResult.ServerError -> result
        }
    }

    suspend fun uploadSyncFile(accessToken: String, existingFileId: String?, json: String): DriveResult<String> {
        val metadataJson = if (existingFileId == null) {
            """{"name":"$SYNC_FILE_NAME","parents":["appDataFolder"]}"""
        } else {
            "{}"
        }
        val jsonMediaType = "application/json; charset=UTF-8".toMediaType()
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadataJson.toRequestBody(jsonMediaType))
            .addPart(json.toRequestBody(jsonMediaType))
            .build()

        val url = if (existingFileId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
        }
        val requestBuilder = authorizedRequest(url, accessToken)
        val request = if (existingFileId == null) requestBuilder.post(body).build() else requestBuilder.patch(body).build()

        return when (val result = executeRaw(request)) {
            is DriveResult.Success -> parseOrServerError(result.value) { responseBody ->
                gson.fromJson(responseBody, CreatedFile::class.java)?.id
            }
            is DriveResult.AuthExpired -> result
            is DriveResult.NetworkError -> result
            is DriveResult.ServerError -> result
        }
    }

    private fun authorizedRequest(url: String, accessToken: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $accessToken")

    private fun executeRaw(request: Request): DriveResult<String> {
        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 -> DriveResult.AuthExpired
                    response.isSuccessful -> DriveResult.Success(response.body?.string().orEmpty())
                    else -> DriveResult.ServerError(response.code)
                }
            }
        } catch (e: IOException) {
            DriveResult.NetworkError
        }
    }

    private fun <T> parseOrServerError(body: String, parse: (String) -> T?): DriveResult<T> {
        return try {
            val parsed = parse(body)
            if (parsed != null) DriveResult.Success(parsed) else DriveResult.ServerError(0)
        } catch (e: JsonParseException) {
            DriveResult.ServerError(0)
        }
    }

    private data class FilesListResponse(val files: List<DriveFileMeta>?)
    private data class CreatedFile(val id: String)
}
