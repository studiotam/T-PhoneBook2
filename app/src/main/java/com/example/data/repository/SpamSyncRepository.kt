package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.SpamDao
import com.example.data.local.SpamNumberEntity
import com.example.service.SpamSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val addedCount: Int, val revokedCount: Int, val totalCount: Int, val date: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SpamSyncRepository(
    private val context: Context,
    private val spamDao: SpamDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spam_sync_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _spreadsheetSource = MutableStateFlow(
        prefs.getString(KEY_SPREADSHEET_SOURCE, null)?.let {
            if (it == OLD_DEFAULT_SPREADSHEET_ID || it.isBlank()) DEFAULT_SPREADSHEET_ID else it
        } ?: DEFAULT_SPREADSHEET_ID
    )
    val spreadsheetSource: StateFlow<String> = _spreadsheetSource.asStateFlow()

    private val _callingCode = MutableStateFlow(
        prefs.getString(KEY_CALLING_CODE, DEFAULT_CALLING_CODE) ?: DEFAULT_CALLING_CODE
    )
    val callingCode: StateFlow<String> = _callingCode.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(
        prefs.getString(KEY_LAST_SYNC_TIME, "") ?: ""
    )
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _autoBlockThreshold = MutableStateFlow(
        prefs.getInt(KEY_AUTO_BLOCK_THRESHOLD, 4)
    )
    val autoBlockThreshold: StateFlow<Int> = _autoBlockThreshold.asStateFlow()

    private val _blockUnregisteredInternational = MutableStateFlow(
        prefs.getBoolean(KEY_BLOCK_UNREGISTERED_INTERNATIONAL, false)
    )
    val blockUnregisteredInternational: StateFlow<Boolean> = _blockUnregisteredInternational.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    )
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    init {
        if (_autoSyncEnabled.value) {
            schedulePeriodicSync(true)
        }
    }

    val totalSpamCountFlow: Flow<Int> = spamDao.getCountFlow()

    fun getSpamListFlow(query: String = ""): Flow<List<SpamNumberEntity>> {
        return if (query.isBlank()) {
            spamDao.getAllFlow()
        } else {
            spamDao.searchFlow(query.trim())
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        _autoSyncEnabled.value = enabled
        schedulePeriodicSync(enabled)
    }

    /**
     * WorkManager による3日に1度の定期同期スケジュール登録 / 解除
     */
    fun schedulePeriodicSync(enabled: Boolean) {
        try {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<SpamSyncWorker>(
                    repeatInterval = 3,
                    repeatIntervalTimeUnit = TimeUnit.DAYS,
                    flexTimeInterval = 6,
                    flexTimeIntervalUnit = TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    SpamSyncWorker.WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.i("SpamSyncRepository", "Enqueued 3-day periodic WorkManager for spam sync")
            } else {
                workManager.cancelUniqueWork(SpamSyncWorker.WORK_NAME_PERIODIC)
                workManager.cancelUniqueWork(SpamSyncWorker.WORK_NAME_BACKGROUND_CHECK)
                Log.i("SpamSyncRepository", "Cancelled periodic WorkManager for spam sync")
            }
        } catch (e: Exception) {
            Log.e("SpamSyncRepository", "Failed to schedule WorkManager periodic sync", e)
        }
    }

    /**
     * アプリがバックグラウンドに移行した際に、前回同期から3日以上経過していればWorkManagerで即時同期を実行
     */
    fun checkAndTriggerBackgroundSync() {
        if (!_autoSyncEnabled.value) return
        val lastTime = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        val threeDaysMillis = 3L * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        if (now - lastTime >= threeDaysMillis) {
            Log.i("SpamSyncRepository", "App entered background and 3+ days elapsed since last sync. Enqueueing check.")
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val oneTimeWork = OneTimeWorkRequestBuilder<SpamSyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    SpamSyncWorker.WORK_NAME_BACKGROUND_CHECK,
                    ExistingWorkPolicy.KEEP,
                    oneTimeWork
                )
            } catch (e: Exception) {
                Log.e("SpamSyncRepository", "Failed to enqueue background check sync", e)
            }
        }
    }

    fun setBlockUnregisteredInternational(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_UNREGISTERED_INTERNATIONAL, enabled).apply()
        _blockUnregisteredInternational.value = enabled
    }

    fun setAutoBlockThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_AUTO_BLOCK_THRESHOLD, threshold).apply()
        _autoBlockThreshold.value = threshold
    }

    fun setSpreadsheetSource(source: String) {
        val trimmed = source.trim()
        prefs.edit().putString(KEY_SPREADSHEET_SOURCE, trimmed).apply()
        _spreadsheetSource.value = trimmed
    }

    fun setCallingCode(code: String) {
        val trimmed = code.trim().replace("+", "")
        prefs.edit().putString(KEY_CALLING_CODE, trimmed).apply()
        _callingCode.value = trimmed
    }

    /**
     * 入力されたスプレッドシートURLやIDからIDを抽出
     */
    fun extractSpreadsheetId(source: String): String {
        val trimmed = source.trim()
        // https://docs.google.com/spreadsheets/d/{ID}/...
        val sheetsMatch = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)").find(trimmed)
        if (sheetsMatch != null) return sheetsMatch.groupValues[1]

        // https://drive.google.com/drive/folders/{ID} または /file/d/{ID}
        val driveFolderMatch = Regex("/folders/([a-zA-Z0-9_-]+)").find(trimmed)
        if (driveFolderMatch != null) return driveFolderMatch.groupValues[1]

        val driveFileMatch = Regex("/file/d/([a-zA-Z0-9_-]+)").find(trimmed)
        if (driveFileMatch != null) return driveFileMatch.groupValues[1]

        val idParamMatch = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(trimmed)
        if (idParamMatch != null) return idParamMatch.groupValues[1]

        // URLではなく直接IDが入力された場合
        return trimmed
    }

    /**
     * Googleスプレッドシート gviz/tq CSV エンドポイントURLを生成
     */
    fun buildCsvEndpointUrl(source: String, code: String): String {
        val trimmed = source.trim()
        val cleanCode = code.trim().replace("+", "").ifBlank { "81" }

        // 既にgviz/tq形式または完全なCSVエンドポイントが直接指定されている場合
        if (trimmed.startsWith("http") && trimmed.contains("gviz/tq")) {
            return if (!trimmed.contains("sheet=")) {
                "$trimmed&sheet=$cleanCode"
            } else {
                trimmed
            }
        }

        val sheetId = extractSpreadsheetId(trimmed)
        return "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=$cleanCode"
    }

    /**
     * 電話番号をE.164規格または比較可能な正規化形式に変換
     * 例: 050-1234-5678 -> +815012345678, +81-50-1234-5678 -> +815012345678
     */
    fun normalizeToE164(rawNumber: String, defaultCountryCode: String = "81"): String {
        val cleaned = rawNumber.replace(Regex("[^0-9+]"), "")
        val code = defaultCountryCode.replace("+", "").ifBlank { "81" }
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("0") -> "+$code${cleaned.substring(1)}"
            cleaned.length >= 9 -> "+$code$cleaned"
            else -> cleaned
        }
    }

    /**
     * 国内ローカル形式 (050...) への変換（フォールバック照合用）
     */
    fun normalizeToDomestic(rawNumber: String, defaultCountryCode: String = "81"): String {
        val cleaned = rawNumber.replace(Regex("[^0-9+]"), "")
        val code = defaultCountryCode.replace("+", "").ifBlank { "81" }
        return if (cleaned.startsWith("+$code")) {
            "0" + cleaned.removePrefix("+$code")
        } else if (cleaned.startsWith(code) && cleaned.length > code.length + 8) {
            "0" + cleaned.removePrefix(code)
        } else {
            cleaned
        }
    }

    /**
     * RFC 4180準拠のCSV行パーサー（ダブルクォート、カンマ、多言語テキスト対応）
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Googleスプレッドシートの指定タブからCSVをストリーミング取得し、Room DBへ差分同期
     */
    suspend fun syncSpamList(
        customSource: String? = null,
        customCallingCode: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing

        val source = customSource?.trim()?.takeIf { it.isNotBlank() } ?: _spreadsheetSource.value
        val code = customCallingCode?.trim()?.takeIf { it.isNotBlank() } ?: _callingCode.value
        setSpreadsheetSource(source)
        setCallingCode(code)

        val csvUrl = buildCsvEndpointUrl(source, code)
        Log.i("SpamSyncRepository", "Syncing CSV from: $csvUrl")

        try {
            val request = Request.Builder()
                .url(csvUrl)
                .header("User-Agent", "T-PhoneBook-SpamFilter/1.0 (Android)")
                .header("Accept", "text/csv, text/plain, */*")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "CSV取得失敗: HTTP ${response.code} (${response.message})"
                _syncState.value = SyncState.Error(errorMsg)
                return@withContext SyncResult.Failure(errorMsg)
            }

            val bodyStream = response.body?.byteStream()
            if (bodyStream == null) {
                val errorMsg = "レスポンスストリームが空です"
                _syncState.value = SyncState.Error(errorMsg)
                return@withContext SyncResult.Failure(errorMsg)
            }

            val reader = BufferedReader(InputStreamReader(bodyStream, Charsets.UTF_8))
            val headerLine = reader.readLine()

            if (headerLine == null) {
                val errorMsg = "データが空です"
                _syncState.value = SyncState.Error(errorMsg)
                return@withContext SyncResult.Failure(errorMsg)
            }

            // HTML（Googleログイン画面やエラー画面）が返ってきた場合のハンドリング
            if (headerLine.trim().startsWith("<!DOCTYPE html", ignoreCase = true) ||
                headerLine.trim().startsWith("<html", ignoreCase = true)
            ) {
                val errorMsg = "Googleスプレッドシートの共有設定を「リンクを知っている全員（閲覧者）」に設定してください（認証画面が返されました）。"
                _syncState.value = SyncState.Error(errorMsg)
                return@withContext SyncResult.Failure(errorMsg)
            }

            // ヘッダー行のカラム位置を解析
            // phone_number,category,target_entity,risk_score,first_detected_date,last_confirmed_date,status
            val headers = parseCsvLine(headerLine).map { it.lowercase().trim('"', ' ') }
            val phoneIdx = headers.indexOfFirst { it.contains("phone") || it.contains("number") }.takeIf { it >= 0 } ?: 0
            val catIdx = headers.indexOfFirst { it.contains("category") }.takeIf { it >= 0 } ?: 1
            val targetIdx = headers.indexOfFirst { it.contains("target") || it.contains("entity") }.takeIf { it >= 0 } ?: 2
            val riskIdx = headers.indexOfFirst { it.contains("risk") || it.contains("score") }.takeIf { it >= 0 } ?: 3
            val lastDateIdx = headers.indexOfFirst { it.contains("last_confirmed") || it.contains("date") }.takeIf { it >= 0 } ?: 5
            val statusIdx = headers.indexOfFirst { it.contains("status") }.takeIf { it >= 0 } ?: 6

            val addedEntities = mutableListOf<SpamNumberEntity>()
            val revokedNumbers = mutableListOf<String>()
            val currentTime = System.currentTimeMillis()

            var line: String?
            var lineCount = 0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.isBlank()) continue

                lineCount++
                val cols = parseCsvLine(currentLine)
                if (cols.size <= phoneIdx) continue

                val rawPhone = cols[phoneIdx].trim('"', ' ')
                if (rawPhone.isBlank() || rawPhone.startsWith("#")) continue

                val normalizedE164 = normalizeToE164(rawPhone, code)
                val status = if (statusIdx < cols.size) cols[statusIdx].trim('"', ' ').lowercase() else "active"

                if (status == "revoked" || status == "deleted" || status == "removed") {
                    revokedNumbers.add(normalizedE164)
                    revokedNumbers.add(normalizeToDomestic(rawPhone, code))
                    revokedNumbers.add(rawPhone.replace(Regex("[^0-9+]"), ""))
                } else {
                    val category = if (catIdx < cols.size) cols[catIdx].trim('"', ' ') else "迷惑電話"
                    val targetEntity = if (targetIdx < cols.size) cols[targetIdx].trim('"', ' ').takeIf { it.isNotBlank() } else null
                    val riskScoreStr = if (riskIdx < cols.size) cols[riskIdx].trim('"', ' ') else "1"
                    val riskScore = riskScoreStr.toIntOrNull()?.coerceIn(1, 5) ?: 1
                    val lastDate = if (lastDateIdx < cols.size) cols[lastDateIdx].trim('"', ' ').takeIf { it.isNotBlank() } else null

                    addedEntities.add(
                        SpamNumberEntity(
                            number = normalizedE164,
                            callingCode = code,
                            category = category.ifBlank { "迷惑電話" },
                            targetEntity = targetEntity,
                            riskScore = riskScore,
                            lastConfirmedDate = lastDate,
                            updatedAt = currentTime
                        )
                    )
                }
            }

            reader.close()

            // Room DB へミラー同期反映 (スプレッドシート上で有効期限切れで行削除された番号も自動削除)
            val (addedCount, revokedCount) = spamDao.applyMirrorSync(
                callingCode = code,
                newEntities = addedEntities,
                explicitRevoked = revokedNumbers.distinct()
            )
            val totalCount = spamDao.getCount()

            val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            val timeString = timeFormat.format(Date(currentTime))
            prefs.edit()
                .putString(KEY_LAST_SYNC_TIME, timeString)
                .putLong(KEY_LAST_SYNC_TIMESTAMP, currentTime)
                .apply()
            _lastSyncTime.value = timeString

            val successState = SyncState.Success(
                addedCount = addedCount,
                revokedCount = revokedCount,
                totalCount = totalCount,
                date = timeString
            )
            _syncState.value = successState

            return@withContext SyncResult.Success(
                addedCount = addedCount,
                revokedCount = revokedCount,
                totalCount = totalCount
            )

        } catch (e: Exception) {
            val msg = "同期エラー: ${e.localizedMessage ?: e.javaClass.simpleName}"
            Log.e("SpamSyncRepository", "Sync failed", e)
            _syncState.value = SyncState.Error(msg)
            return@withContext SyncResult.Failure(msg)
        }
    }

    /**
     * 着信電話番号の高速照合（CallScreeningServiceから0.01秒で呼び出し）
     */
    suspend fun checkSpamNumber(rawNumber: String): SpamNumberEntity? = withContext(Dispatchers.IO) {
        if (rawNumber.isBlank()) return@withContext null

        val code = _callingCode.value
        val e164 = normalizeToE164(rawNumber, code)
        val domestic = normalizeToDomestic(rawNumber, code)
        val plain = rawNumber.replace(Regex("[^0-9+]"), "")

        // 1. E.164 完全一致検索
        var match = spamDao.findByNumber(e164)
        if (match != null) return@withContext match

        // 2. 国内ローカル番号またはプレーン形式検索
        if (domestic != e164) {
            match = spamDao.findByNumber(domestic)
            if (match != null) return@withContext match
        }
        if (plain != e164 && plain != domestic) {
            match = spamDao.findByNumber(plain)
            if (match != null) return@withContext match
        }

        return@withContext null
    }

    suspend fun addManualSpam(
        number: String,
        callingCode: String,
        category: String,
        targetEntity: String?,
        riskScore: Int
    ) = withContext(Dispatchers.IO) {
        val e164 = normalizeToE164(number, callingCode)
        spamDao.upsert(
            SpamNumberEntity(
                number = e164,
                callingCode = callingCode.ifBlank { "81" },
                category = category.ifBlank { "手動登録" },
                targetEntity = targetEntity,
                riskScore = riskScore.coerceIn(1, 5),
                lastConfirmedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSpamNumber(number: String) = withContext(Dispatchers.IO) {
        val code = _callingCode.value
        val e164 = normalizeToE164(number, code)
        val domestic = normalizeToDomestic(number, code)
        spamDao.deleteByNumbers(listOf(e164, domestic, number))
    }

    /**
     * 国際電話かどうかを判定
     */
    fun isInternationalNumber(rawNumber: String, myCountryCallingCode: String = _callingCode.value): Boolean {
        val cleanNumber = rawNumber.trim().replace(" ", "").replace("-", "")
        val code = myCountryCallingCode.trim().replace("+", "").ifBlank { "81" }

        if (cleanNumber.isBlank()) return false

        // +で始まる場合 (例: +1..., +86...)
        if (cleanNumber.startsWith("+")) {
            // 自国の国番号（例: +81）で始まっていれば国内、それ以外は国際電話
            return !cleanNumber.startsWith("+$code")
        }

        // 日本の国際発信プレフィックス 010 (例: 010-1-...)
        if (cleanNumber.startsWith("010") && cleanNumber.length > 5) {
            val rest = cleanNumber.removePrefix("010")
            return !rest.startsWith(code)
        }

        // 国際電話アクセスコード 00 (例: 001, 0044)
        if (cleanNumber.startsWith("00") && cleanNumber.length > 5) {
            val rest = cleanNumber.removePrefix("00")
            return !rest.startsWith(code)
        }

        return false
    }

    /**
     * 端末の連絡先（Contacts）に登録されているか判定
     */
    fun isNumberInContacts(rawNumber: String): Boolean {
        if (rawNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(rawNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            Log.e("SpamSyncRepository", "Error checking if number in contacts: $rawNumber", e)
            false
        }
    }

    suspend fun clearAllSpamNumbers() = withContext(Dispatchers.IO) {
        spamDao.clearAll()
    }

    /**
     * 連絡先（Contacts）に登録されている電話番号の中から、迷惑電話データベースと一致する番号を抽出
     */
    suspend fun findSpamMatchesInContacts(contacts: List<com.example.domain.model.Contact>): List<SpamContactMatch> = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) return@withContext emptyList()

        val callingCode = _callingCode.value
        val allSpam = spamDao.getAllSpamNumbers()
        if (allSpam.isEmpty()) return@withContext emptyList()

        val spamMap = HashMap<String, SpamNumberEntity>()
        for (entity in allSpam) {
            spamMap[entity.number] = entity
            val domestic = normalizeToDomestic(entity.number, callingCode)
            if (domestic.isNotBlank()) spamMap[domestic] = entity
            val plain = entity.number.replace(Regex("[^0-9+]"), "")
            if (plain.isNotBlank()) spamMap[plain] = entity
        }

        val matches = mutableListOf<SpamContactMatch>()
        for (contact in contacts) {
            val rawNum = contact.phoneNumber
            if (rawNum.isBlank()) continue

            val e164 = normalizeToE164(rawNum, callingCode)
            val domestic = normalizeToDomestic(rawNum, callingCode)
            val plain = rawNum.replace(Regex("[^0-9+]"), "")

            val matchedEntity = spamMap[e164] ?: spamMap[domestic] ?: spamMap[plain]
            if (matchedEntity != null) {
                matches.add(
                    SpamContactMatch(
                        contactId = contact.id,
                        contactName = contact.name.ifBlank { "名前なし" },
                        contactPhoneNumber = contact.phoneNumber,
                        contactPhotoUri = contact.photoUri,
                        spamEntity = matchedEntity
                    )
                )
            }
        }

        return@withContext matches.distinctBy { it.contactPhoneNumber + "_" + it.spamEntity.number }
    }

    companion object {
        private const val KEY_SPREADSHEET_SOURCE = "spam_spreadsheet_source"
        private const val KEY_CALLING_CODE = "spam_calling_code"
        private const val KEY_LAST_SYNC_TIME = "spam_last_sync_time"
        private const val KEY_LAST_SYNC_TIMESTAMP = "spam_last_sync_timestamp"
        private const val KEY_AUTO_BLOCK_THRESHOLD = "spam_auto_block_threshold"
        private const val KEY_BLOCK_UNREGISTERED_INTERNATIONAL = "spam_block_unregistered_international"
        private const val KEY_AUTO_SYNC_ENABLED = "spam_auto_sync_enabled"

        private const val OLD_DEFAULT_SPREADSHEET_ID = "1I1OaV0sv1A3z1E9IvP0gOE9TjjfIqi39"
        const val DEFAULT_SPREADSHEET_ID = "1CVL_SI9c4pcCdNRe9UOWoqJ9DvlaEUCD3TDTssCDeEA"
        const val DEFAULT_CALLING_CODE = "81"
    }
}

sealed class SyncResult {
    data class Success(val addedCount: Int, val revokedCount: Int, val totalCount: Int) : SyncResult()
    data class Failure(val errorMessage: String) : SyncResult()
}

data class SpamContactMatch(
    val contactId: String,
    val contactName: String,
    val contactPhoneNumber: String,
    val contactPhotoUri: String?,
    val spamEntity: SpamNumberEntity
)
