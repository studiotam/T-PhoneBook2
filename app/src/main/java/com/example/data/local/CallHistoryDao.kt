package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallHistoryDao {
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CallHistoryEntity>>

    @Query("SELECT * FROM call_history WHERE isOutgoing = 1 ORDER BY timestamp DESC")
    fun getOutgoingHistory(): Flow<List<CallHistoryEntity>>

    @Query("SELECT * FROM call_history WHERE isOutgoing = 0 ORDER BY timestamp DESC")
    fun getIncomingHistory(): Flow<List<CallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallHistoryEntity)

    @Query("DELETE FROM call_history")
    suspend fun clearHistory()

    @Query("DELETE FROM call_history WHERE isOutgoing = 1")
    suspend fun clearOutgoingHistory()

    @Query("DELETE FROM call_history WHERE isOutgoing = 0")
    suspend fun clearIncomingHistory()
}
