package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val contactName: String?,
    val isOutgoing: Boolean,
    val filePath: String,
    val fileName: String,
    val durationSeconds: Int,
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis()
)
