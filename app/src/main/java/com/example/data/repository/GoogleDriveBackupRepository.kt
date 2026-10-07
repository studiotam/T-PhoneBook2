package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.OAuthTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveFileInfo(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val sizeBytes: Long
)

sealed class DriveBackupResult {
    data class Success(val fileInfo: DriveFileInfo, val message: String) : DriveBackupResult()
    data class NeedsConsent(val consentIntent: Intent) : DriveBackupResult()
    data class Error(val message: String) : DriveBackupResult()
}

sealed class DriveRestoreResult {
    data class Success(val jsonContent: String, val fileInfo: DriveFileInfo) : DriveRestoreResult()
    data class NotFound(val message: String) : DriveRestoreResult()
    data class NeedsConsent(val consentIntent: Intent) : DriveRestoreResult()
    data class Error(val message: String) : DriveRestoreResult()
}

sealed class DriveCheckResult {
    data class Found(val fileInfo: DriveFileInfo) : DriveCheckResult()
    object NotFound : DriveCheckResult()
    data class NeedsConsent(val consentIntent: Intent) : DriveCheckResult()
    data class Error(val message: String) : DriveCheckResult()
}

class GoogleDriveBackupRepository(
    private val context: Context,
    private val authManager: GoogleAuthManager
) {
    companion object {
        private const val TAG = "DriveBackupRepo"
        const val BACKUP_FILE_NAME = "t_phonebook_settings_backup.json"
        private const val MIME_JSON = "application/json; charset=utf-8"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Google Drive内の既存バックアップファイルを検索
     */
    suspend fun findBackupFile(email: String): DriveCheckResult = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getOAuthAccessToken(email)
        val token = when (tokenResult) {
            is OAuthTokenResult.Success -> tokenResult.token
            is OAuthTokenResult.NeedsUserConsent -> return@withContext DriveCheckResult.NeedsConsent(tokenResult.consentIntent)
            is OAuthTokenResult.Error -> return@withContext DriveCheckResult.Error(tokenResult.message)
        }

        try {
            // spaces=appDataFolder,drive でファイルを検索
            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime,size,description)&spaces=drive,appDataFolder"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.code == 401) {
                authManager.invalidateToken(token)
                return@withContext DriveCheckResult.Error("認証の有効期限が切れました。再度お試しください。")
            }

            if (!response.isSuccessful) {
                val detailError = parseGoogleApiError(responseBody, response.code, response.message)
                Log.e(TAG, "findBackupFile failed: $detailError (raw: $responseBody)")
                return@withContext DriveCheckResult.Error("Drive検索エラー: $detailError")
            }

            val body = responseBody ?: return@withContext DriveCheckResult.NotFound
            val root = JSONObject(body)
            val filesArray = root.optJSONArray("files")

            if (filesArray == null || filesArray.length() == 0) {
                return@withContext DriveCheckResult.NotFound
            }

            val firstFile = filesArray.getJSONObject(0)
            val id = firstFile.optString("id", "")
            val name = firstFile.optString("name", BACKUP_FILE_NAME)
            val modifiedTime = firstFile.optString("modifiedTime", "")
            val size = firstFile.optLong("size", 0L)

            DriveCheckResult.Found(
                DriveFileInfo(
                    id = id,
                    name = name,
                    modifiedTime = formatIsoTime(modifiedTime),
                    sizeBytes = size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup file", e)
            DriveCheckResult.Error("Drive接続エラー: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /**
     * 設定JSONをGoogle Driveへバックアップ（新規作成または更新）
     */
    suspend fun backupSettings(email: String, jsonContent: String): DriveBackupResult = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getOAuthAccessToken(email)
        val token = when (tokenResult) {
            is OAuthTokenResult.Success -> tokenResult.token
            is OAuthTokenResult.NeedsUserConsent -> return@withContext DriveBackupResult.NeedsConsent(tokenResult.consentIntent)
            is OAuthTokenResult.Error -> return@withContext DriveBackupResult.Error(tokenResult.message)
        }

        try {
            // 既存ファイルの存在確認
            val existingResult = findBackupFile(email)
            val existingFileId = when (existingResult) {
                is DriveCheckResult.Found -> existingResult.fileInfo.id
                else -> null
            }

            val response = if (existingFileId != null) {
                // 既存ファイルの上書き更新 (PATCH media)
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val mediaBody = jsonContent.toRequestBody(MIME_JSON.toMediaType())
                val patchRequest = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(mediaBody)
                    .build()
                okHttpClient.newCall(patchRequest).execute()
            } else {
                // 新規ファイルのアップロード (Multipart POST)
                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILE_NAME)
                    put("mimeType", "application/json")
                    put("description", "T-PhoneBook Settings Backup")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addPart(jsonContent.toRequestBody(MIME_JSON.toMediaType()))
                    .build()

                val postRequest = Request.Builder()
                    .url(createUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()
                okHttpClient.newCall(postRequest).execute()
            }

            if (response.code == 401) {
                authManager.invalidateToken(token)
                return@withContext DriveBackupResult.Error("認証の有効期限が切れました。再度お試しください。")
            }

            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val detailError = parseGoogleApiError(respBody.ifBlank { null }, response.code, response.message)
                Log.e(TAG, "backupSettings failed: $detailError (raw: $respBody)")
                return@withContext DriveBackupResult.Error("Drive保存失敗: $detailError")
            }

            val jsonResp = JSONObject(respBody)
            val fileId = jsonResp.optString("id", existingFileId ?: "")
            val name = jsonResp.optString("name", BACKUP_FILE_NAME)

            val nowFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

            val info = DriveFileInfo(
                id = fileId,
                name = name,
                modifiedTime = nowFormatted,
                sizeBytes = jsonContent.toByteArray(Charsets.UTF_8).size.toLong()
            )

            DriveBackupResult.Success(info, "Google Driveへバックアップを保存しました")
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up to drive", e)
            DriveBackupResult.Error("バックアップエラー: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Google Drive上のバックアップファイルをダウンロード
     */
    suspend fun restoreSettings(email: String): DriveRestoreResult = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getOAuthAccessToken(email)
        val token = when (tokenResult) {
            is OAuthTokenResult.Success -> tokenResult.token
            is OAuthTokenResult.NeedsUserConsent -> return@withContext DriveRestoreResult.NeedsConsent(tokenResult.consentIntent)
            is OAuthTokenResult.Error -> return@withContext DriveRestoreResult.Error(tokenResult.message)
        }

        try {
            val check = findBackupFile(email)
            val fileInfo = when (check) {
                is DriveCheckResult.Found -> check.fileInfo
                is DriveCheckResult.NotFound -> return@withContext DriveRestoreResult.NotFound("Google Drive上にバックアップファイルが見つかりませんでした")
                is DriveCheckResult.NeedsConsent -> return@withContext DriveRestoreResult.NeedsConsent(check.consentIntent)
                is DriveCheckResult.Error -> return@withContext DriveRestoreResult.Error(check.message)
            }

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/${fileInfo.id}?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.code == 401) {
                authManager.invalidateToken(token)
                return@withContext DriveRestoreResult.Error("認証の有効期限が切れました。再度お試しください。")
            }

            if (!response.isSuccessful) {
                val detailError = parseGoogleApiError(responseBody, response.code, response.message)
                Log.e(TAG, "restoreSettings failed: $detailError (raw: $responseBody)")
                return@withContext DriveRestoreResult.Error("Drive復元失敗: $detailError")
            }

            val content = responseBody
            if (content.isNullOrBlank()) {
                return@withContext DriveRestoreResult.Error("ダウンロードしたバックアップデータが空でした")
            }

            DriveRestoreResult.Success(content, fileInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from drive", e)
            DriveRestoreResult.Error("復元エラー: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun parseGoogleApiError(body: String?, code: Int, msg: String): String {
        if (body.isNullOrBlank()) {
            return if (code == 403) {
                "HTTP 403 Forbidden (Google Cloud Consoleで「Google Drive API」が有効化されているか、またはOAuth同意画面でテストユーザー/スコープが設定されているかご確認ください)"
            } else {
                "HTTP $code $msg"
            }
        }
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            if (errorObj != null) {
                val errorMsg = errorObj.optString("message", "")
                val errorsArray = errorObj.optJSONArray("errors")
                val reason = errorsArray?.optJSONObject(0)?.optString("reason", "") ?: ""
                val details = if (reason.isNotBlank()) " [$reason]" else ""
                
                if (code == 403 || reason == "accessNotConfigured" || reason == "forbidden") {
                    val helpHint = if (reason == "accessNotConfigured") {
                        " (Google Cloud Consoleで「Google Drive API」を有効にしてください)"
                    } else {
                        " (Google Drive APIの有効化またはOAuthテストユーザー設定を確認してください)"
                    }
                    "${errorMsg.ifBlank { "アクセス拒否 (403)" }}$details$helpHint"
                } else if (errorMsg.isNotBlank()) {
                    "$errorMsg$details"
                } else {
                    "HTTP $code: $msg"
                }
            } else {
                "HTTP $code: $body"
            }
        } catch (_: Exception) {
            if (code == 403) {
                "HTTP 403 Forbidden (Google Cloud ConsoleでDrive APIが有効化されているかご確認ください)"
            } else {
                "HTTP $code: $msg"
            }
        }
    }

    private fun formatIsoTime(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdfInput.parse(iso.take(19))
            if (date != null) {
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(date)
            } else {
                iso
            }
        } catch (_: Exception) {
            iso
        }
    }
}
