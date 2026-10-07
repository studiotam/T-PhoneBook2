package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.AppDatabase
import com.example.data.repository.CallLogRepository
import com.example.data.repository.CallRecordingRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.GoogleDriveBackupRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpamSyncRepository
import com.example.service.AppCallScreeningService

class PhonebookApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var contactsRepository: ContactsRepository
        private set

    lateinit var callLogRepository: CallLogRepository
        private set

    lateinit var callRecordingRepository: CallRecordingRepository
        private set

    lateinit var googleAuthManager: GoogleAuthManager
        private set

    lateinit var spamSyncRepository: SpamSyncRepository
        private set

    lateinit var googleDriveBackupRepository: GoogleDriveBackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "phonebook_db"
        ).fallbackToDestructiveMigration()
        .build()

        settingsRepository = SettingsRepository(this)
        contactsRepository = ContactsRepository(this)
        callLogRepository = CallLogRepository(this, database.callHistoryDao())
        callRecordingRepository = CallRecordingRepository(this, database.callRecordingDao())
        googleAuthManager = GoogleAuthManager(this)
        spamSyncRepository = SpamSyncRepository(this, database.spamDao())
        googleDriveBackupRepository = GoogleDriveBackupRepository(this, googleAuthManager)

        AppCallScreeningService.createNotificationChannel(this)
        com.example.service.MissedCallNotifier.createNotificationChannel(this)
    }
}
