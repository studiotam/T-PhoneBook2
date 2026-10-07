package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CallHistoryEntity::class, SpamNumberEntity::class, CallRecordingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callHistoryDao(): CallHistoryDao
    abstract fun spamDao(): SpamDao
    abstract fun callRecordingDao(): CallRecordingDao
}

