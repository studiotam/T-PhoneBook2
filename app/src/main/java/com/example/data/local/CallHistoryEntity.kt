package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String, // original number without prefix
    val contactName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = true
)
