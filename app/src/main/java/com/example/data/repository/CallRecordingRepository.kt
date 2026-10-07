package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.CallRecordingDao
import com.example.data.local.CallRecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class CallRecordingRepository(
    private val context: Context,
    private val callRecordingDao: CallRecordingDao
) {
    companion object {
        private const val TAG = "CallRecordingRepo"
    }

    val allRecordingsFlow: Flow<List<CallRecordingEntity>> = callRecordingDao.getAllRecordings()

    fun getRecordingsForNumber(phoneNumber: String): Flow<List<CallRecordingEntity>> {
        return callRecordingDao.getRecordingsForNumber(phoneNumber)
    }

    fun getRecordingCountForNumber(phoneNumber: String): Flow<Int> {
        return callRecordingDao.getRecordingCountForNumber(phoneNumber)
    }

    suspend fun saveRecording(
        phoneNumber: String,
        contactName: String?,
        isOutgoing: Boolean,
        filePath: String,
        fileName: String,
        durationSeconds: Int,
        fileSize: Long
    ): CallRecordingEntity = withContext(Dispatchers.IO) {
        val entity = CallRecordingEntity(
            phoneNumber = phoneNumber,
            contactName = contactName,
            isOutgoing = isOutgoing,
            filePath = filePath,
            fileName = fileName,
            durationSeconds = durationSeconds,
            fileSize = fileSize,
            timestamp = System.currentTimeMillis()
        )
        callRecordingDao.insertRecording(entity)
        Log.d(TAG, "Saved recording record: ${entity.id} - ${entity.fileName}")
        entity
    }

    suspend fun deleteRecording(entity: CallRecordingEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
            callRecordingDao.deleteRecordingById(entity.id)
            Log.d(TAG, "Deleted recording: ${entity.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAllRecordings(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = context.getExternalFilesDir("Recordings") ?: context.filesDir.resolve("recordings")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
            callRecordingDao.deleteAllRecordings()
            Log.d(TAG, "All recordings deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all recordings: ${e.message}", e)
            false
        }
    }
}
