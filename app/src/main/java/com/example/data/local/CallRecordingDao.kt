package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getRecordingsForNumber(phoneNumber: String): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: String): CallRecordingEntity?

    @Query("SELECT COUNT(*) FROM call_recordings WHERE phoneNumber = :phoneNumber")
    fun getRecordingCountForNumber(phoneNumber: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)

    @Query("DELETE FROM call_recordings")
    suspend fun deleteAllRecordings()
}
