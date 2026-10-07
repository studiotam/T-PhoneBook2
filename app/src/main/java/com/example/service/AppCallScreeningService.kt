package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PhonebookApplication
import com.example.R
import com.example.data.local.SpamNumberEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android標準の着信スクリーニングサービス (CallScreeningService)
 * 着信時に発信元電話番号をRoom DBで高速検索し、自動拒否または警告通知を発行します。
 */
class AppCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
            ?: callDetails.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: ""

        if (rawNumber.isBlank()) {
            val defaultResponse = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            respondToCall(callDetails, defaultResponse)
            return
        }

        val app = applicationContext as? PhonebookApplication
        val spamRepo = app?.spamSyncRepository

        if (spamRepo == null) {
            val defaultResponse = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            respondToCall(callDetails, defaultResponse)
            return
        }

        serviceScope.launch {
            try {
                val blockInternational = spamRepo.blockUnregisteredInternational.value
                val isInternational = spamRepo.isInternationalNumber(rawNumber)

                // 1. 連絡先登録外の国際電話 着信拒否チェック
                if (blockInternational && isInternational) {
                    val inContacts = spamRepo.isNumberInContacts(rawNumber)
                    if (!inContacts) {
                        Log.i("AppCallScreeningService", "Blocked unregistered international call: $rawNumber")
                        val blockResponse = CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipCallLog(false)
                            .setSkipNotification(false)
                            .build()
                        respondToCall(callDetails, blockResponse)

                        showInternationalBlockedNotification(applicationContext, rawNumber)
                        return@launch
                    }
                }

                // 2. 迷惑電話データベース照合チェック
                val spamEntity = spamRepo.checkSpamNumber(rawNumber)
                val threshold = spamRepo.autoBlockThreshold.value

                if (spamEntity != null) {
                    val riskScore = spamEntity.riskScore
                    Log.i("AppCallScreeningService", "Spam detected: ${spamEntity.number}, Risk: $riskScore, Category: ${spamEntity.category}")

                    if (riskScore >= threshold) {
                        // 危険度 >= しきい値: 自動拒否 & 切断 (着信履歴には記録)
                        val blockResponse = CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipCallLog(false)
                            .setSkipNotification(false)
                            .build()
                        respondToCall(callDetails, blockResponse)

                        showSpamBlockedNotification(applicationContext, spamEntity, rawNumber)
                    } else {
                        // 危険度 < しきい値: 通話は許可するが警告通知・表示
                        val allowResponse = CallResponse.Builder()
                            .setDisallowCall(false)
                            .setRejectCall(false)
                            .setSkipCallLog(false)
                            .setSkipNotification(false)
                            .build()
                        respondToCall(callDetails, allowResponse)

                        showSpamWarningNotification(applicationContext, spamEntity, rawNumber)
                    }
                } else {
                    // 迷惑電話リストに該当なし: 通常通話
                    val normalResponse = CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .build()
                    respondToCall(callDetails, normalResponse)
                }
            } catch (e: Exception) {
                Log.e("AppCallScreeningService", "Error screening call", e)
                val fallbackResponse = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
                respondToCall(callDetails, fallbackResponse)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID_SPAM = "spam_call_screening_channel"
        private const val CHANNEL_NAME_SPAM = "迷惑電話・着信警告"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID_SPAM,
                    CHANNEL_NAME_SPAM,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "迷惑電話の自動拒否および着信時の警告通知を表示します"
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }

        private fun showSpamBlockedNotification(
            context: Context,
            spam: SpamNumberEntity,
            incomingNumber: String
        ) {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                incomingNumber.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🚫 迷惑電話を着信拒否しました"
            val targetInfo = if (!spam.targetEntity.isNullOrBlank()) " (${spam.targetEntity})" else ""
            val content = "$incomingNumber : ${spam.category}$targetInfo (危険度: ${spam.riskScore}/5)"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPAM)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(incomingNumber.hashCode(), notification)
        }

        private fun showInternationalBlockedNotification(
            context: Context,
            incomingNumber: String
        ) {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                incomingNumber.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🌐 未登録の国際電話を着信拒否しました"
            val content = "$incomingNumber : 連絡先に未登録の国際電話のため自動切断しました"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPAM)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(incomingNumber.hashCode(), notification)
        }

        private fun showSpamWarningNotification(
            context: Context,
            spam: SpamNumberEntity,
            incomingNumber: String
        ) {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                incomingNumber.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "⚠️ 迷惑電話の注意: $incomingNumber"
            val targetInfo = if (!spam.targetEntity.isNullOrBlank()) " (${spam.targetEntity})" else ""
            val content = "カテゴリ: ${spam.category}$targetInfo | 危険度スコア: ${spam.riskScore}/5"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPAM)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(incomingNumber.hashCode(), notification)
        }
    }
}
