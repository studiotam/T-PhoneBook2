package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.local.CallHistoryDao
import com.example.data.local.CallHistoryEntity
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CallLogRepository(
    private val context: Context,
    private val callHistoryDao: CallHistoryDao
) {
    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refresh()
        }
    }

    init {
        try {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Exception) {
        }
    }

    fun refresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 発信履歴を取得（Room DB + システム通話ログをマージして時系列順）
     */
    fun getOutgoingCalls(): Flow<List<CallRecord>> {
        return combine(
            callHistoryDao.getOutgoingHistory(),
            refreshTrigger
        ) { localList, _ ->
            val systemList = fetchSystemOutgoingCalls()
            mergeAndSortCalls(
                localList.map { it.toCallRecord() },
                systemList
            )
        }.flowOn(Dispatchers.IO)
    }

    /**
     * 着信履歴を取得（Room DB + システム通話ログをマージして時系列順）
     */
    fun getIncomingCalls(): Flow<List<CallRecord>> {
        return combine(
            callHistoryDao.getIncomingHistory(),
            refreshTrigger
        ) { localList, _ ->
            val systemList = fetchSystemIncomingCalls()
            mergeAndSortCalls(
                localList.map { it.toCallRecord() },
                systemList
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun clearOutgoingHistory() {
        withContext(Dispatchers.IO) {
            callHistoryDao.clearOutgoingHistory()
        }
    }

    suspend fun clearIncomingHistory() {
        withContext(Dispatchers.IO) {
            callHistoryDao.clearIncomingHistory()
        }
    }

    suspend fun clearAllLocalHistory() {
        withContext(Dispatchers.IO) {
            callHistoryDao.clearHistory()
        }
    }

    private fun mergeAndSortCalls(
        localRecords: List<CallRecord>,
        systemRecords: List<CallRecord>
    ): List<CallRecord> {
        val combined = mutableListOf<CallRecord>()
        combined.addAll(systemRecords)

        // 重複（ほぼ同時刻＆同番号）をチェックしてローカルレコードを追加
        for (local in localRecords) {
            val duplicate = systemRecords.any { system ->
                val timeDiff = Math.abs(system.timestamp - local.timestamp)
                timeDiff < 10000 && isSamePhoneNumber(system.phoneNumber, local.phoneNumber)
            }
            if (!duplicate) {
                combined.add(local)
            }
        }

        return combined.sortedByDescending { it.timestamp }
    }

    private fun isSamePhoneNumber(num1: String, num2: String): Boolean {
        val n1 = num1.replace("[^0-9+]".toRegex(), "")
        val n2 = num2.replace("[^0-9+]".toRegex(), "")
        if (n1.isEmpty() || n2.isEmpty()) return false
        return n1 == n2 || n1.endsWith(n2) || n2.endsWith(n1)
    }

    suspend fun fetchSystemOutgoingCalls(): List<CallRecord> = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext emptyList()
        val results = mutableListOf<CallRecord>()
        val nameCache = mutableMapOf<String, String?>()

        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )
            val selection = "${CallLog.Calls.TYPE} = ?"
            val selectionArgs = arrayOf(CallLog.Calls.OUTGOING_TYPE.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 300"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationCol = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getString(idCol) else ""
                    val number = if (numberCol >= 0) cursor.getString(numberCol) ?: "" else ""
                    var name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    if (name.isNullOrBlank() && number.isNotBlank()) {
                        name = nameCache.getOrPut(number) {
                            lookupContactName(number)
                        }
                    }
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L

                    results.add(
                        CallRecord(
                            id = "sys_$id",
                            phoneNumber = number,
                            contactName = name,
                            timestamp = date,
                            durationSeconds = duration,
                            callType = CallType.OUTGOING
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    suspend fun fetchSystemIncomingCalls(): List<CallRecord> = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext emptyList()
        val results = mutableListOf<CallRecord>()
        val nameCache = mutableMapOf<String, String?>()

        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )
            // TYPE != OUTGOING_TYPE で全ての着信（応答・不在・着信拒否など）を取得
            val selection = "${CallLog.Calls.TYPE} != ?"
            val selectionArgs = arrayOf(CallLog.Calls.OUTGOING_TYPE.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 300"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationCol = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getString(idCol) else ""
                    val number = if (numberCol >= 0) cursor.getString(numberCol) ?: "" else ""
                    var name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    if (name.isNullOrBlank() && number.isNotBlank()) {
                        name = nameCache.getOrPut(number) {
                            lookupContactName(number)
                        }
                    }
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val type = if (typeCol >= 0) cursor.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE

                    val callType = when (type) {
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        else -> CallType.INCOMING
                    }

                    results.add(
                        CallRecord(
                            id = "sys_$id",
                            phoneNumber = number,
                            contactName = name,
                            timestamp = date,
                            durationSeconds = duration,
                            callType = callType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    private fun lookupContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun CallHistoryEntity.toCallRecord(): CallRecord {
    return CallRecord(
        id = "local_${this.id}",
        phoneNumber = this.phoneNumber,
        contactName = this.contactName,
        timestamp = this.timestamp,
        durationSeconds = 0,
        callType = if (this.isOutgoing) CallType.OUTGOING else CallType.INCOMING
    )
}
