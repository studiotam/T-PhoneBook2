package com.example.ui.call

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import android.telecom.CallAudioState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.PhonebookApplication
import com.example.data.local.SpamNumberEntity
import com.example.domain.model.IncomingCallDisplayConfig
import com.example.domain.model.OutgoingCallDisplayConfig
import com.example.service.CallManager
import com.example.service.CallRecorder
import com.example.service.CallStateInfo
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PhoneNumberFormatter
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            MyApplicationTheme {
                InCallScreen(onFinish = { finish() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallRecorder.onCallEnded(this)
    }
}

@Composable
fun InCallScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val callInfo by CallManager.callStateInfo.collectAsStateWithLifecycle()
    val isRecording by CallRecorder.isRecording.collectAsStateWithLifecycle()
    val recordDurationSeconds by CallRecorder.recordingDurationSeconds.collectAsStateWithLifecycle()

    var showDtmfPad by remember { mutableStateOf(false) }
    var showQuickSmsDialog by remember { mutableStateOf(false) }
    var showAudioRouteDialog by remember { mutableStateOf(false) }
    var detectedSpam by remember { mutableStateOf<SpamNumberEntity?>(null) }
    val appContext = context.applicationContext as? PhonebookApplication

    val incomingConfig by (appContext?.settingsRepository?.incomingCallDisplayConfigFlow
        ?.collectAsStateWithLifecycle(initialValue = IncomingCallDisplayConfig())
        ?: remember { mutableStateOf(IncomingCallDisplayConfig()) })

    val activeConfig by (appContext?.settingsRepository?.activeCallDisplayConfigFlow
        ?.collectAsStateWithLifecycle(initialValue = com.example.domain.model.ActiveCallDisplayConfig())
        ?: remember { mutableStateOf(com.example.domain.model.ActiveCallDisplayConfig()) })

    val outgoingConfig by (appContext?.settingsRepository?.outgoingCallDisplayConfigFlow
        ?.collectAsStateWithLifecycle(initialValue = OutgoingCallDisplayConfig())
        ?: remember { mutableStateOf(OutgoingCallDisplayConfig()) })

    val autoRecordCalls by (appContext?.settingsRepository?.autoRecordCallsFlow
        ?.collectAsStateWithLifecycle(initialValue = false)
        ?: remember { mutableStateOf(false) })

    // マイク権限リクエストランチャー
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cur = callInfo
            if (cur != null) {
                CallRecorder.startRecording(
                    context = context,
                    phoneNumber = cur.phoneNumber,
                    contactName = cur.contactName,
                    isOutgoing = cur.isOutgoing,
                    isPreview = cur.isPreview
                )
            }
        } else {
            Toast.makeText(context, "通話録音を行うにはマイクの権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    // 迷惑電話DB照合（着信時のみ）
    LaunchedEffect(callInfo?.phoneNumber, callInfo?.isOutgoing) {
        val num = callInfo?.phoneNumber
        val isOut = callInfo?.isOutgoing == true ||
                callInfo?.state == Call.STATE_DIALING ||
                callInfo?.state == Call.STATE_CONNECTING
        if (!num.isNullOrBlank() && appContext != null && !isOut) {
            detectedSpam = appContext.spamSyncRepository.checkSpamNumber(num)
        } else {
            detectedSpam = null
        }
    }

    // 通話終了を検知して画面を閉じる＆録音停止
    LaunchedEffect(callInfo) {
        if (callInfo == null || callInfo?.state == Call.STATE_DISCONNECTED) {
            CallRecorder.onCallEnded(context)
            delay(1200)
            onFinish()
        }
    }

    // 自動録音の判定
    LaunchedEffect(callInfo?.state, autoRecordCalls) {
        if (callInfo?.state == Call.STATE_ACTIVE && autoRecordCalls && !isRecording) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm || callInfo?.isPreview == true) {
                CallRecorder.startRecording(
                    context = context,
                    phoneNumber = callInfo!!.phoneNumber,
                    contactName = callInfo!!.contactName,
                    isOutgoing = callInfo!!.isOutgoing,
                    isPreview = callInfo!!.isPreview
                )
            }
        }
    }

    var callDurationSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(callInfo?.state) {
        if (callInfo?.state == Call.STATE_ACTIVE) {
            val start = if (callInfo?.connectTimeMillis != null && callInfo!!.connectTimeMillis > 0) {
                callInfo!!.connectTimeMillis
            } else {
                System.currentTimeMillis()
            }
            while (true) {
                val current = System.currentTimeMillis()
                callDurationSeconds = ((current - start) / 1000).toInt().coerceAtLeast(0)
                delay(1000)
            }
        }
    }

    if (callInfo == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121820)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("通話を終了しました", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
            }
        }
        return
    }

    val info = callInfo!!
    val isRinging = info.state == Call.STATE_RINGING
    val isActiveCall = info.state == Call.STATE_ACTIVE || info.state == Call.STATE_HOLDING
    val isOutgoing = (info.isOutgoing || info.state == Call.STATE_DIALING || info.state == Call.STATE_CONNECTING) && !isActiveCall

    // 各画面（着信画面・通話中画面・発信画面）の設定分岐
    val showPhoto = when {
        isActiveCall -> activeConfig.showPhoto
        isOutgoing -> outgoingConfig.showPhoto
        else -> incomingConfig.showPhoto
    }
    val showPhoneNumber = when {
        isActiveCall -> activeConfig.showPhoneNumber
        isOutgoing -> outgoingConfig.showPhoneNumber
        else -> incomingConfig.showPhoneNumber
    }
    val showCompany = when {
        isActiveCall -> activeConfig.showCompany
        isOutgoing -> outgoingConfig.showCompany
        else -> incomingConfig.showCompany
    }
    val showCallTimer = when {
        isActiveCall -> activeConfig.showCallTimer
        isOutgoing -> outgoingConfig.showCallTimer
        else -> incomingConfig.showCallTimer
    }
    val showMuteButton = when {
        isActiveCall -> activeConfig.showMuteButton
        isOutgoing -> outgoingConfig.showMuteButton
        else -> incomingConfig.showMuteButton
    }
    val showSpeakerButton = when {
        isActiveCall -> activeConfig.showSpeakerButton
        isOutgoing -> outgoingConfig.showSpeakerButton
        else -> incomingConfig.showSpeakerButton
    }
    val showKeypadButton = when {
        isActiveCall -> activeConfig.showKeypadButton
        isOutgoing -> outgoingConfig.showKeypadButton
        else -> incomingConfig.showKeypadButton
    }
    val showRecordButton = when {
        isActiveCall -> activeConfig.showRecordButton
        isOutgoing -> outgoingConfig.showRecordButton
        else -> incomingConfig.showRecordButton
    }
    val showAudioDeviceBadge = when {
        isActiveCall -> activeConfig.showAudioDeviceBadge
        else -> true
    }
    val themeColorKey = when {
        isActiveCall -> activeConfig.theme
        isOutgoing -> outgoingConfig.theme
        else -> incomingConfig.theme
    }

    val bgColor = when (themeColorKey) {
        "blue" -> Color(0xFF0C1726)
        "purple" -> Color(0xFF160E24)
        "amoled" -> Color(0xFF000000)
        else -> Color(0xFF121820)
    }

    val statusText = when (info.state) {
        Call.STATE_RINGING -> "着信中..."
        Call.STATE_DIALING -> "発信中..."
        Call.STATE_CONNECTING -> "接続中..."
        Call.STATE_ACTIVE -> {
            if (showCallTimer) {
                val mins = callDurationSeconds / 60
                val secs = callDurationSeconds % 60
                String.format("通話中 %02d:%02d", mins, secs)
            } else {
                "通話中"
            }
        }
        Call.STATE_HOLDING -> "保留中"
        Call.STATE_DISCONNECTED -> "通話終了"
        else -> "通話中"
    }

    // 録音中アニメーション（点滅）
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部：プレビュー表示バッジ ＆ 発信者情報
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (info.isPreview) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = (if (isOutgoing) Color(0xFF00897B) else Color(0xFF3F51B5)).copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isOutgoing) "🧪 発信画面プレビュー中" else "🧪 着信画面プレビュー中",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "閉じる",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        CallManager.clearPreview()
                                        onFinish()
                                    }
                            )
                        }
                    }
                }

                // 録音中インジケーターバッジ
                if (isRecording) {
                    val rMin = recordDurationSeconds / 60
                    val rSec = recordDurationSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.92f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(14.dp)
                                    .alpha(recAlpha)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = String.format("通話録音中 (%02d:%02d) .mp3保存", rMin, rSec),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 発信SIM / プレフィックス情報バッジ（発信時のみ）
                if (isOutgoing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (outgoingConfig.showSimInfo && info.simSlot != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E88E5).copy(alpha = 0.35f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        tint = Color(0xFF90CAF9),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "SIM ${info.simSlot}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF90CAF9),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (outgoingConfig.showPrefixInfo && !info.prefixName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF26A69A).copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = info.prefixName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF80CBC4),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 連絡先アバター・写真
                if (showPhoto) {
                    val photoUri = info.photoUri
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "連絡先写真",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (isOutgoing) Color(0xFF00796B).copy(alpha = 0.45f) else Color(0xFF3949AB).copy(alpha = 0.45f),
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 発信者名 / 連絡先名
                val displayName = if (!info.contactName.isNullOrBlank()) {
                    info.contactName
                } else if (showPhoneNumber) {
                    PhoneNumberFormatter.formatForDisplay(context, info.phoneNumber)
                } else {
                    if (isOutgoing) "発信中" else "着信中"
                }

                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // 電話番号表示
                if (showPhoneNumber && !info.contactName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = PhoneNumberFormatter.formatForDisplay(context, info.phoneNumber),
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                // 会社名 / 組織情報
                if (showCompany && !info.company.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = info.company,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 通話状態ステータス
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRinging) Color(0xFF64B5F6) else if (isOutgoing && info.state != Call.STATE_ACTIVE) Color(0xFF80CBC4) else Color.White.copy(alpha = 0.85f)
                )

                // 音声出力先バッジ（Bluetooth接続時 or スピーカーON時）
                if (showAudioDeviceBadge && (info.state == Call.STATE_ACTIVE || isOutgoing)) {
                    if (info.isBluetoothOn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAudioRouteDialog = true }
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothAudio,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bluetooth通話中: ${info.bluetoothDeviceName ?: "接続中"}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (info.isSpeakerOn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00796B).copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAudioRouteDialog = true }
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "スピーカー通話中",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 発信テスト中（ダイヤル中）の接続シミュレーションボタン
                if (info.isPreview && isOutgoing && (info.state == Call.STATE_DIALING || info.state == Call.STATE_CONNECTING)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF26A69A),
                        modifier = Modifier
                            .clickable { CallManager.answer() }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "通話接続（応答シミュレート）",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 迷惑電話の警告表示（着信時のみ）
                if (!isOutgoing && incomingConfig.showSpamWarning && (detectedSpam != null || info.isPreview)) {
                    val spam = detectedSpam ?: SpamNumberEntity(
                        number = info.phoneNumber,
                        riskScore = 4,
                        category = "営業勧誘・スパム疑い",
                        targetEntity = "サンプル迷惑電話DB"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ 迷惑電話の警告 (危険度: ${spam.riskScore}/5)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${spam.category}${if (!spam.targetEntity.isNullOrBlank()) " (${spam.targetEntity})" else ""}",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // DTMFダイヤルパッド表示（必要時）
            AnimatedVisibility(visible = showDtmfPad) {
                DtmfKeypad(onDigit = { CallManager.sendDtmf(it) })
            }

            // 下部：操作ボタンエリア
            if (isRinging) {
                // 着信時：クイックSMS、応答（緑）、拒否（赤）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (incomingConfig.showQuickSms) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showQuickSmsDialog = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "メッセージで返信して拒否",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 拒否ボタン
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { CallManager.reject() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "着信拒否",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("拒否", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        }

                        // 応答ボタン
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { CallManager.answer() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "通話に応答",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("応答", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // 発信中 または 通話中 操作パネル
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hasControls = showMuteButton || showSpeakerButton || showKeypadButton || showRecordButton
                    if (hasControls) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (showMuteButton) {
                                InCallActionButton(
                                    icon = if (info.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    label = if (info.isMuted) "ミュート中" else "消音",
                                    isActive = info.isMuted,
                                    onClick = { CallManager.toggleMute() }
                                )
                            }

                            if (showSpeakerButton) {
                                val isBtAvailable = info.isBluetoothAvailable || info.isBluetoothOn
                                val audioIcon = when {
                                    info.isBluetoothOn -> Icons.Default.BluetoothAudio
                                    info.isSpeakerOn -> Icons.Default.VolumeUp
                                    info.isWiredHeadsetOn -> Icons.Default.Headset
                                    else -> if (isBtAvailable) Icons.Default.PhoneInTalk else Icons.Default.VolumeUp
                                }
                                val audioLabel = when {
                                    info.isBluetoothOn -> "Bluetooth"
                                    info.isSpeakerOn -> "スピーカーON"
                                    info.isWiredHeadsetOn -> "ヘッドセット"
                                    else -> if (isBtAvailable) "受話口" else "スピーカー"
                                }
                                val isAudioActive = info.isBluetoothOn || info.isSpeakerOn || info.isWiredHeadsetOn

                                InCallActionButton(
                                    icon = audioIcon,
                                    label = audioLabel,
                                    isActive = isAudioActive,
                                    activeColor = when {
                                        info.isBluetoothOn -> Color(0xFF1976D2)
                                        info.isSpeakerOn -> Color(0xFF00897B)
                                        else -> Color.White
                                    },
                                    activeIconTint = Color.White,
                                    onClick = {
                                        if (isBtAvailable) {
                                            showAudioRouteDialog = true
                                        } else {
                                            CallManager.toggleSpeaker()
                                        }
                                    }
                                )
                            }

                            if (showKeypadButton) {
                                InCallActionButton(
                                    icon = Icons.Default.Dialpad,
                                    label = "キーパッド",
                                    isActive = showDtmfPad,
                                    onClick = { showDtmfPad = !showDtmfPad }
                                )
                            }

                            if (showRecordButton) {
                                val recLabel = if (isRecording) {
                                    val rMin = recordDurationSeconds / 60
                                    val rSec = recordDurationSeconds % 60
                                    String.format("録音中 %02d:%02d", rMin, rSec)
                                } else {
                                    "通話録音"
                                }

                                InCallActionButton(
                                    icon = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    label = recLabel,
                                    isActive = isRecording,
                                    activeColor = Color(0xFFD32F2F),
                                    activeIconTint = Color.White,
                                    onClick = {
                                        if (isRecording) {
                                            CallRecorder.stopRecording(context)
                                        } else {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPerm || info.isPreview) {
                                                CallRecorder.startRecording(
                                                    context = context,
                                                    phoneNumber = info.phoneNumber,
                                                    contactName = info.contactName,
                                                    isOutgoing = isOutgoing,
                                                    isPreview = info.isPreview
                                                )
                                            } else {
                                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 終話 / 発信中止ボタン (赤色)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable {
                                CallRecorder.onCallEnded(context)
                                CallManager.disconnect()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "通話終了",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isOutgoing && info.state != Call.STATE_ACTIVE) "発信を中止" else "通話を終了",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // クイックSMS拒否ダイアログ（着信時）
    if (showQuickSmsDialog && callInfo != null) {
        val quickMessages = listOf(
            "現在電話に出られません。後で折り返します。",
            "電車移動中のため後ほどご連絡します。",
            "会議中のため通話ができません。",
            "緊急の用件でしょうか？"
        )

        AlertDialog(
            onDismissRequest = { showQuickSmsDialog = false },
            title = { Text("メッセージで返信して拒否") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickMessages.forEach { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showQuickSmsDialog = false
                                    // SMS送信と着信拒否を実行
                                    try {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${callInfo?.phoneNumber}")
                                            putExtra("sms_body", msg)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(smsIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "SMSアプリの起動に失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                    CallManager.reject()
                                }
                                .padding(12.dp)
                        ) {
                            Text(text = msg, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickSmsDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 音声出力先切り替えダイアログ (Bluetooth / スピーカー / 受話口 / 有線ヘッドセット)
    if (showAudioRouteDialog && callInfo != null) {
        val info = callInfo!!
        AudioRouteSelectionDialog(
            currentRoute = info.audioRoute,
            isBluetoothAvailable = info.isBluetoothAvailable || info.isBluetoothOn,
            bluetoothDeviceName = info.bluetoothDeviceName,
            isWiredHeadsetAvailable = info.isWiredHeadsetOn || (info.supportedAudioRoutes and CallAudioState.ROUTE_WIRED_HEADSET) != 0,
            onSelectRoute = { route ->
                CallManager.setAudioRoute(route)
                showAudioRouteDialog = false
            },
            onDismiss = { showAudioRouteDialog = false }
        )
    }
}

@Composable
fun AudioRouteSelectionDialog(
    currentRoute: Int,
    isBluetoothAvailable: Boolean,
    bluetoothDeviceName: String?,
    isWiredHeadsetAvailable: Boolean,
    onSelectRoute: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.BluetoothAudio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "音声出力先の選択",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Bluetooth機器
                if (isBluetoothAvailable) {
                    AudioRouteOptionRow(
                        icon = Icons.Default.BluetoothAudio,
                        title = bluetoothDeviceName ?: "Bluetooth機器",
                        subtitle = "ワイヤレスイヤホン / カーオーディオ等",
                        isSelected = currentRoute == CallAudioState.ROUTE_BLUETOOTH,
                        badgeColor = Color(0xFF1976D2),
                        onClick = { onSelectRoute(CallAudioState.ROUTE_BLUETOOTH) }
                    )
                }

                // 2. スピーカーフォン
                AudioRouteOptionRow(
                    icon = Icons.Default.VolumeUp,
                    title = "スピーカーフォン",
                    subtitle = "端末の内蔵スピーカーから音声を拡声",
                    isSelected = currentRoute == CallAudioState.ROUTE_SPEAKER,
                    badgeColor = Color(0xFF00796B),
                    onClick = { onSelectRoute(CallAudioState.ROUTE_SPEAKER) }
                )

                // 3. 受話口（内蔵レシーバー）
                AudioRouteOptionRow(
                    icon = Icons.Default.PhoneInTalk,
                    title = "受話口（レシーバー）",
                    subtitle = "耳にあてて通常通話",
                    isSelected = currentRoute == CallAudioState.ROUTE_EARPIECE,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    onClick = { onSelectRoute(CallAudioState.ROUTE_EARPIECE) }
                )

                // 4. 有線ヘッドセット
                if (isWiredHeadsetAvailable) {
                    AudioRouteOptionRow(
                        icon = Icons.Default.Headset,
                        title = "有線ヘッドセット",
                        subtitle = "イヤホン端子 / USB-C接続",
                        isSelected = currentRoute == CallAudioState.ROUTE_WIRED_HEADSET,
                        badgeColor = Color(0xFF6A1B9A),
                        onClick = { onSelectRoute(CallAudioState.ROUTE_WIRED_HEADSET) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun AudioRouteOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) badgeColor else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "選択中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun InCallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = Color.White,
    activeIconTint: Color = Color(0xFF121820),
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeIconTint else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DtmfKeypad(onDigit: (Char) -> Unit) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { char ->
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onDigit(char) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char.toString(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
