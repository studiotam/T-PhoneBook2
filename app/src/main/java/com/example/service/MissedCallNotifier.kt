package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.PhoneNumberFormatter

object MissedCallNotifier {

    const val CHANNEL_ID_MISSED_CALLS = "channel_missed_calls"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_MISSED_CALLS,
                "不在着信",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "不在着信の通知を表示します"
                enableLights(true)
                lightColor = 0xFFFF0000.toInt()
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 不在着信通知を表示します。
     * タップするとMainActivityが起動し、対象の電話番号が入力されたダイヤラー画面が開きます。
     */
    fun showMissedCallNotification(
        context: Context,
        phoneNumber: String,
        contactName: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val displayFormatted = PhoneNumberFormatter.formatForDisplay(context, phoneNumber)
        val dialerNumber = PhoneNumberFormatter.toNationalDigits(context, phoneNumber)

        val title = "不在着信"
        val content = if (!contactName.isNullOrBlank()) {
            "$contactName ($displayFormatted)"
        } else {
            displayFormatted.ifBlank { phoneNumber }
        }

        // 通知タップ時のPendingIntent (MainActivity -> DialerScreen with number)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("dial_number", dialerNumber)
            putExtra("phone_number", dialerNumber)
            putExtra("number", dialerNumber)
            putExtra("tab", "dialer")
            data = Uri.parse("dialer?number=${Uri.encode(dialerNumber)}")
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            phoneNumber.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MISSED_CALLS)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(timestamp)
            .setColor(0xFFD32F2F.toInt())
            .setContentIntent(pendingContentIntent)
            .addAction(
                android.R.drawable.stat_notify_missed_call,
                "ダイヤラーで開く",
                pendingContentIntent
            )

        val notificationId = ("missed_" + phoneNumber).hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}
