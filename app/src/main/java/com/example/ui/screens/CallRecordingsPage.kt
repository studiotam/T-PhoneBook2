package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CallRecordingEntity
import com.example.util.PhoneNumberFormatter
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallRecordingsPage(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.callRecordings.collectAsStateWithLifecycle()
    val autoRecordCalls by viewModel.autoRecordCalls.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var recordingToDelete by remember { mutableStateOf<CallRecordingEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // 現在再生中の録音IDとMediaPlayerの管理
    var activePlayingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }

    // 画面破棄時にMediaPlayerを停止・解放
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    // 再生中の進捗タイマー
    LaunchedEffect(isPlaying, activePlayingId) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying) {
                try {
                    val pos = mediaPlayer?.currentPosition ?: 0
                    currentPositionMs = pos
                    val dur = mediaPlayer?.duration ?: 0
                    if (dur > 0) totalDurationMs = dur
                } catch (_: Exception) {}
                delay(200)
            }
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        activePlayingId = null
        currentPositionMs = 0
        totalDurationMs = 0
    }

    fun playRecording(recording: CallRecordingEntity) {
        if (activePlayingId == recording.id && isPlaying) {
            // 一時停止
            try {
                mediaPlayer?.pause()
                isPlaying = false
            } catch (_: Exception) {
                stopPlayback()
            }
            return
        }

        if (activePlayingId == recording.id && !isPlaying && mediaPlayer != null) {
            // 再開
            try {
                mediaPlayer?.start()
                isPlaying = true
            } catch (_: Exception) {
                stopPlayback()
            }
            return
        }

        // 新規再生
        stopPlayback()
        val file = File(recording.filePath)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "録音ファイルが見つかりません", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(recording.filePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0
                }
            }
            mediaPlayer = player
            isPlaying = true
            activePlayingId = recording.id
            totalDurationMs = player.duration.coerceAtLeast(recording.durationSeconds * 1000)
            currentPositionMs = 0
        } catch (e: Exception) {
            Toast.makeText(context, "音声再生エラー: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            stopPlayback()
        }
    }

    fun shareRecording(recording: CallRecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "ファイルが存在しません", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "通話録音: ${recording.contactName ?: recording.phoneNumber}")
                putExtra(Intent.EXTRA_TEXT, "通話録音データ (${recording.fileName})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "通話録音を共有"))
        } catch (e: Exception) {
            Toast.makeText(context, "共有エラー: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredRecordings = remember(recordings, searchQuery) {
        if (searchQuery.isBlank()) {
            recordings
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            recordings.filter {
                (it.contactName?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                        it.phoneNumber.contains(q) ||
                        it.fileName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    val totalSizeBytes = remember(recordings) {
        recordings.sumOf { it.fileSize }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ヘッダー情報
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "通話録音・保存設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (recordings.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("一括削除", fontSize = 12.sp)
                    }
                }
            }
            Text(
                text = "通話画面での手動録音および自動通話録音（MP3形式）の管理・再生が行えます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 自動通話録音設定カード
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (autoRecordCalls) Color(0xFFD32F2F).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (autoRecordCalls) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "通話時の自動録音",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "通話が開始されると自動的にMP3形式で録音を開始します",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = autoRecordCalls,
                        onCheckedChange = { viewModel.setAutoRecordCalls(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "保存形式: MP3 (MPEG-4 AAC / 44.1kHz / 128kbps)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "録音件数: ${recordings.size}件 (${formatFileSize(totalSizeBytes)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 検索ボックス
        if (recordings.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("名前や電話番号で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 録音一覧リスト
        if (filteredRecordings.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = if (searchQuery.isNotBlank()) "一致する通話録音は見つかりませんでした" else "保存された通話録音はありません",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "通話中画面の「通話録音」ボタンをタップするか、上記の「自動録音」をONにすることで、通話内容がMP3ファイルとしてここに保存されます。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredRecordings.forEach { recording ->
                    val isThisActive = activePlayingId == recording.id
                    val isThisPlaying = isThisActive && isPlaying

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isThisActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isThisActive) 1.5.dp else 1.dp,
                            color = if (isThisActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ヘッダー行：発着信アイコン、名前/番号、日時、共有・削除ボタン
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (recording.isOutgoing) Color(0xFF26A69A).copy(alpha = 0.15f) else Color(0xFF42A5F5).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (recording.isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                                                contentDescription = if (recording.isOutgoing) "発信" else "着信",
                                                tint = if (recording.isOutgoing) Color(0xFF00796B) else Color(0xFF1976D2),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        val titleName = recording.contactName ?: PhoneNumberFormatter.formatForDisplay(context, recording.phoneNumber)
                                        Text(
                                            text = titleName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (!recording.contactName.isNullOrBlank()) {
                                                Text(
                                                    text = PhoneNumberFormatter.formatForDisplay(context, recording.phoneNumber),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = formatTimestamp(recording.timestamp),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { shareRecording(recording) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "共有",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { recordingToDelete = recording },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "削除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // バッジ情報（MP3, 長さ, ファイルサイズ）
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD32F2F).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "MP3",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = formatDuration(recording.durationSeconds),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = formatFileSize(recording.fileSize),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // プレイヤー操作バー
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isThisPlaying) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { playRecording(recording) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isThisPlaying) "一時停止" else "再生",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    // シークバー / タイムライン
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (isThisActive && totalDurationMs > 0) {
                                            Slider(
                                                value = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f),
                                                onValueChange = { fraction ->
                                                    val targetMs = (fraction * totalDurationMs).toInt()
                                                    currentPositionMs = targetMs
                                                    try {
                                                        mediaPlayer?.seekTo(targetMs)
                                                    } catch (_: Exception) {}
                                                },
                                                colors = SliderDefaults.colors(
                                                    thumbColor = MaterialTheme.colorScheme.primary,
                                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(24.dp)
                                            )
                                        } else {
                                            Slider(
                                                value = 0f,
                                                onValueChange = {},
                                                enabled = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(24.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isThisActive) formatDurationMs(currentPositionMs) else "00:00",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatDuration(recording.durationSeconds),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 単体削除確認ダイアログ
    if (recordingToDelete != null) {
        val target = recordingToDelete!!
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("通話録音の削除") },
            text = {
                Text(
                    text = "「${target.contactName ?: target.phoneNumber}」の録音データ（${target.fileName}）を端末から完全に削除しますか？"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activePlayingId == target.id) {
                            stopPlayback()
                        }
                        viewModel.deleteCallRecording(target)
                        recordingToDelete = null
                        Toast.makeText(context, "録音を削除しました", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 全件削除確認ダイアログ
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("すべての通話録音を削除") },
            text = {
                Text("保存されているすべての通話録音ファイル（${recordings.size}件）を一括削除します。この操作は取り消せません。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        stopPlayback()
                        viewModel.deleteAllCallRecordings()
                        showDeleteAllDialog = false
                        Toast.makeText(context, "すべての録音を削除しました", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("すべて削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
