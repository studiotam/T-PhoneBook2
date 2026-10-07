package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PhonebookApplication
import com.example.data.repository.SyncResult

/**
 * 迷惑電話リストのバックグラウンド定期同期 Worker
 * WorkManager により3日に1度、またはアプリがバックグラウンドに遷移した際に最新情報を自動確認・同期します。
 */
class SpamSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "SpamSyncWorker started")
        val app = applicationContext as? PhonebookApplication ?: run {
            Log.e(TAG, "ApplicationContext is not PhonebookApplication")
            return Result.failure()
        }

        val spamRepo = app.spamSyncRepository
        if (!spamRepo.autoSyncEnabled.value) {
            Log.i(TAG, "Auto sync is disabled by user. Skipping work.")
            return Result.success()
        }

        return try {
            when (val result = spamRepo.syncSpamList()) {
                is SyncResult.Success -> {
                    Log.i(TAG, "Spam sync successful: +${result.addedCount}, -${result.revokedCount}, total: ${result.totalCount}")
                    Result.success()
                }
                is SyncResult.Failure -> {
                    Log.w(TAG, "Spam sync returned failure: ${result.errorMessage}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Spam sync worker encountered unexpected exception", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SpamSyncWorker"
        const val WORK_NAME_PERIODIC = "spam_periodic_sync_work"
        const val WORK_NAME_BACKGROUND_CHECK = "spam_background_check_work"
    }
}
