package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.IncomingCallDisplayConfig
import com.example.domain.model.OutgoingCallDisplayConfig
import com.example.util.DefaultDialerHelper

@Composable
fun IncomingCallSettingsPage(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 着信画面, 1: 発信画面
    val isDefaultDialer = remember(context) { DefaultDialerHelper.isDefaultDialer(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ヘッダー説明
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "電話の発着信・通話画面設定",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "電話の「着信画面」「通話中画面」「発信画面」のデザインや各機能の表示/非表示を個別に設定できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // デフォルト電話アプリ確認カード
        if (!isDefaultDialer) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "標準の電話アプリへの設定",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "本アプリのカスタム発着信・通話中画面を実際の通話時に表示するには、標準の電話アプリに設定する必要があります。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val intent = DefaultDialerHelper.createRequestDefaultDialerIntent(context)
                            if (intent != null) context.startActivity(intent)
                        }
                    ) {
                        Text("設定")
                    }
                }
            }
        }

        // タブ切り替え（着信画面 / 通話中画面 / 発信画面）
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("着信画面", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("通話中画面", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("発信画面", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        when (selectedTab) {
            0 -> IncomingCallSubSection(viewModel = viewModel)
            1 -> ActiveCallSubSection(viewModel = viewModel)
            else -> OutgoingCallSubSection(viewModel = viewModel)
        }
    }
}

/**
 * 着信画面設定タブコンテンツ
 */
@Composable
private fun IncomingCallSubSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val config by viewModel.incomingCallDisplayConfig.collectAsStateWithLifecycle()

    var previewMode by remember { mutableIntStateOf(0) } // 0: 着信時, 1: 通話中

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 着信画面マスター有効化スイッチカード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (config.enabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = if (config.enabled) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "カスタム着信画面の表示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (config.enabled) "電話着信時に本アプリのカスタム画面を表示します" else "システム標準の着信通知を使用します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { isChecked ->
                        viewModel.setIncomingCallDisplayConfig(config.copy(enabled = isChecked))
                    }
                )
            }
        }

        // リアルタイムミニプレビュー＆全画面テスト起動カード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "着信画面プレビュー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // プレビュー状態切り替え (着信時 / 通話中)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (previewMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { previewMode = 0 }
                        ) {
                            Text(
                                text = "🔔 着信時",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (previewMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (previewMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { previewMode = 1 }
                        ) {
                            Text(
                                text = "📞 通話中",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (previewMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // ミニ着信画面モックアップ
                val mockBgColor = when (config.theme) {
                    "blue" -> Color(0xFF0C1726)
                    "purple" -> Color(0xFF160E24)
                    "amoled" -> Color(0xFF000000)
                    else -> Color(0xFF121820)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = mockBgColor,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (config.showPhoto) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "山田 太郎",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (config.showPhoneNumber) {
                            Text(
                                text = "090-1234-5678",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (config.showCompany) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "株式会社サンプル 営業部",
                                    fontSize = 12.sp,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }

                        if (previewMode == 0) {
                            // 着信中画面プレビュー
                            Text(
                                text = "着信中...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64B5F6)
                            )

                            if (config.showSpamWarning) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⚠️ 迷惑電話の警告 (危険度: 4/5 - 営業勧誘)",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (config.showQuickSms) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "メッセージで返信して拒否",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CallEnd,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // 通話中画面プレビュー
                            Text(
                                text = if (config.showCallTimer) "通話中 01:23" else "通話中",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(Modifier.height(4.dp))

                            // 操作ボタン一覧（設定に応じた表示）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (config.showMuteButton) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text("消音", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                if (config.showSpeakerButton) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text("スピーカー", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                if (config.showKeypadButton) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Dialpad,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text("テンキー", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                if (config.showRecordButton) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FiberManualRecord,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text("通話録音", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // 終話ボタン
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.startIncomingCallPreview(
                            context = context,
                            phoneNumber = "090-1234-5678",
                            contactName = "山田 太郎",
                            company = "株式会社サンプル 営業部"
                        )
                        Toast.makeText(context, "着信画面テストを起動しました", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("📱 着信画面を全画面でテスト実行（プレビュー）", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 表示項目の個別設定カード
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "着信画面の表示項目・機能設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "電話がかかってきた際、画面上に表示する情報やボタンを設定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                CallToggleRow(
                    icon = Icons.Default.Person,
                    title = "連絡先写真・アバターアイコン",
                    subtitle = "連絡先に登録された写真またはアイコンを表示",
                    checked = config.showPhoto,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showPhoto = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Call,
                    title = "電話番号の表示",
                    subtitle = "名前の下に電話番号を表示（非通知時は名前欄のみ）",
                    checked = config.showPhoneNumber,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showPhoneNumber = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Business,
                    title = "会社名・所属組織の表示",
                    subtitle = "連絡先に登録された会社名や役職を着信画面に表示",
                    checked = config.showCompany,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showCompany = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Warning,
                    title = "迷惑電話の警告バナー表示",
                    subtitle = "迷惑電話DBと照合して危険度・カテゴリ警告を表示",
                    checked = config.showSpamWarning,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showSpamWarning = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Timer,
                    title = "通話経過時間タイマー",
                    subtitle = "通話開始後の経過時間（mm:ss）を画面中央に表示",
                    checked = config.showCallTimer,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showCallTimer = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Mic,
                    title = "消音（ミュート）ボタン",
                    subtitle = "通話中にマイクを消音するボタンを表示",
                    checked = config.showMuteButton,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showMuteButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "スピーカー / Bluetooth切り替えボタン",
                    subtitle = "通話中にスピーカーフォンやBluetoothイヤホン・機器に切り替えるボタンを表示",
                    checked = config.showSpeakerButton,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showSpeakerButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Dialpad,
                    title = "ダイヤルキーパッド（DTMF）ボタン",
                    subtitle = "通話中の音声案内番号入力用テンキーを表示するボタン",
                    checked = config.showKeypadButton,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showKeypadButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.FiberManualRecord,
                    title = "通話録音ボタンの表示",
                    subtitle = "通話中にワンタップでMP3録音を開始・停止できるボタンを表示",
                    checked = config.showRecordButton,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showRecordButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Message,
                    title = "クイックSMS応答機能",
                    subtitle = "着信時に定型メッセージを送信して拒否するボタンを表示",
                    checked = config.showQuickSms,
                    onCheckedChange = { viewModel.setIncomingCallDisplayConfig(config.copy(showQuickSms = it)) }
                )
            }
        }

        // 背景テーマ選択カード
        ThemeSelectorCard(
            selectedTheme = config.theme,
            onThemeSelected = { viewModel.setIncomingCallDisplayConfig(config.copy(theme = it)) }
        )

        // 初期設定リセットボタン
        OutlinedButton(
            onClick = {
                viewModel.resetIncomingCallDisplayConfig()
                Toast.makeText(context, "着信画面設定を初期状態にリセットしました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("着信画面の表示設定を初期値に戻す")
        }
    }
}

/**
 * 通話中画面設定タブコンテンツ
 */
@Composable
private fun ActiveCallSubSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val config by viewModel.activeCallDisplayConfig.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 通話中画面マスター有効化スイッチカード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (config.enabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = if (config.enabled) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "カスタム通話中画面の表示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (config.enabled) "通話接続中に本アプリのカスタム通話中画面を表示します" else "システム標準の通話画面を使用します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { isChecked ->
                        viewModel.setActiveCallDisplayConfig(config.copy(enabled = isChecked))
                    }
                )
            }
        }

        // リアルタイムミニプレビュー＆全画面テスト起動カード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "通話中画面プレビュー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "設定の変更が即時反映されます",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ミニ通話中画面モックアップ
                val mockBgColor = when (config.theme) {
                    "blue" -> Color(0xFF0C1726)
                    "purple" -> Color(0xFF160E24)
                    "amoled" -> Color(0xFF000000)
                    else -> Color(0xFF121820)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = mockBgColor,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (config.showPhoto) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "山田 太郎",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (config.showPhoneNumber) {
                            Text(
                                text = "090-1234-5678",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (config.showCompany) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "株式会社サンプル 営業部",
                                    fontSize = 12.sp,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }

                        Text(
                            text = if (config.showCallTimer) "通話中 01:23" else "通話中",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        if (config.showAudioDeviceBadge) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1976D2).copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "🎧 Bluetooth通話中: Pixel Buds Pro",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 操作ボタン一覧（設定に応じた表示）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (config.showMuteButton) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text("消音", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            if (config.showSpeakerButton) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text("スピーカー", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            if (config.showKeypadButton) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Dialpad,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text("テンキー", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            if (config.showRecordButton) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FiberManualRecord,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text("通話録音", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 終話ボタン
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.startActiveCallPreview(
                            context = context,
                            phoneNumber = "090-1234-5678",
                            contactName = "山田 太郎",
                            company = "株式会社サンプル 営業部"
                        )
                        Toast.makeText(context, "通話中画面テストを起動しました", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("📱 通話中画面を全画面でテスト実行（プレビュー）", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 表示項目の個別設定カード
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "通話中画面の表示項目・機能設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "通話接続中（会話中）に表示する各種情報や操作ボタンを設定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                CallToggleRow(
                    icon = Icons.Default.Person,
                    title = "連絡先写真・アバターアイコン",
                    subtitle = "相手の連絡先写真またはアイコンを表示",
                    checked = config.showPhoto,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showPhoto = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Call,
                    title = "電話番号の表示",
                    subtitle = "名前の下に相手の電話番号を表示",
                    checked = config.showPhoneNumber,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showPhoneNumber = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Business,
                    title = "会社名・所属組織の表示",
                    subtitle = "相手の会社名や役職を表示",
                    checked = config.showCompany,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showCompany = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Timer,
                    title = "通話経過時間タイマー",
                    subtitle = "通話開始後の経過時間（mm:ss）を表示",
                    checked = config.showCallTimer,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showCallTimer = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Mic,
                    title = "消音（ミュート）ボタン",
                    subtitle = "自分の声を消音するマイクボタンを表示",
                    checked = config.showMuteButton,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showMuteButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "スピーカー / Bluetooth切り替えボタン",
                    subtitle = "スピーカーフォンやBluetoothイヤホンへ切り替えるボタンを表示",
                    checked = config.showSpeakerButton,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showSpeakerButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Dialpad,
                    title = "ダイヤルキーパッド（DTMF）ボタン",
                    subtitle = "音声案内入力用テンキーを表示するボタン",
                    checked = config.showKeypadButton,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showKeypadButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.FiberManualRecord,
                    title = "通話録音ボタンの表示",
                    subtitle = "ワンタップでMP3録音を開始・停止できるボタンを表示",
                    checked = config.showRecordButton,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showRecordButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Headset,
                    title = "音声出力先（Bluetooth / スピーカー）バッジ表示",
                    subtitle = "現在使用中の音声機器名を画面上部に表示",
                    checked = config.showAudioDeviceBadge,
                    onCheckedChange = { viewModel.setActiveCallDisplayConfig(config.copy(showAudioDeviceBadge = it)) }
                )
            }
        }

        // 背景テーマ選択カード
        ThemeSelectorCard(
            selectedTheme = config.theme,
            onThemeSelected = { viewModel.setActiveCallDisplayConfig(config.copy(theme = it)) }
        )

        // 初期設定リセットボタン
        OutlinedButton(
            onClick = {
                viewModel.resetActiveCallDisplayConfig()
                Toast.makeText(context, "通話中画面設定を初期状態にリセットしました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("通話中画面の表示設定を初期値に戻す")
        }
    }
}

/**
 * 発信画面設定タブコンテンツ
 */
@Composable
private fun OutgoingCallSubSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val config by viewModel.outgoingCallDisplayConfig.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 発信画面マスター有効化スイッチカード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (config.enabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (config.enabled) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "カスタム発信画面の表示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (config.enabled) "発信時に本アプリのカスタム画面を表示します" else "システム標準の発信画面を使用します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { isChecked ->
                        viewModel.setOutgoingCallDisplayConfig(config.copy(enabled = isChecked))
                    }
                )
            }
        }

        // リアルタイムミニプレビュー＆全画面テスト起動カード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "発信画面プレビュー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "設定の変更が即時反映されます",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ミニ発信画面モックアップ
                val mockBgColor = when (config.theme) {
                    "blue" -> Color(0xFF0C1726)
                    "purple" -> Color(0xFF160E24)
                    "amoled" -> Color(0xFF000000)
                    else -> Color(0xFF121820)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = mockBgColor,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SIM & プレフィックス情報バッジ
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (config.showSimInfo) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E88E5).copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = Color(0xFF90CAF9),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text("SIM 1", fontSize = 10.sp, color = Color(0xFF90CAF9))
                                    }
                                }
                            }
                            if (config.showPrefixInfo) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF43A047).copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        "🏷️ 楽天でんわ (003768)",
                                        fontSize = 10.sp,
                                        color = Color(0xFFA5D6A7),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 写真・アバター
                        if (config.showPhoto) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // 発信先名
                        Text(
                            text = "佐藤 花子",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // 電話番号
                        if (config.showPhoneNumber) {
                            Text(
                                text = "090-9876-5432",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // 会社名
                        if (config.showCompany) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "デザインパートナーズ 代表",
                                    fontSize = 12.sp,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }

                        Text(
                            text = "発信中...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF80CBC4)
                        )

                        Spacer(Modifier.height(6.dp))

                        // 発信中コントロールボタン（消音・スピーカー・キーパッド）
                        val hasControls = config.showMuteButton || config.showSpeakerButton || config.showKeypadButton
                        if (hasControls) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.7f),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                if (config.showMuteButton) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (config.showSpeakerButton) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (config.showKeypadButton) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Dialpad, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        // 発信中止ボタン (赤色)
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.startOutgoingCallPreview(
                            context = context,
                            phoneNumber = "090-9876-5432",
                            contactName = "佐藤 花子",
                            company = "デザインパートナーズ 代表",
                            prefixName = "楽天でんわ (003768)",
                            simSlot = 1
                        )
                        Toast.makeText(context, "発信画面テストを起動しました", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("📱 発信画面を全画面でテスト実行（プレビュー）", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 表示項目の個別設定カード
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "発信画面の表示項目・機能設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "こちらから電話をかける際、画面上に表示する情報やボタンを設定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                CallToggleRow(
                    icon = Icons.Default.Person,
                    title = "連絡先写真・アバターアイコン",
                    subtitle = "連絡先に登録された写真またはアイコンを表示",
                    checked = config.showPhoto,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showPhoto = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Call,
                    title = "相手の電話番号の表示",
                    subtitle = "相手の名前の下に発信先電話番号を表示",
                    checked = config.showPhoneNumber,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showPhoneNumber = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Business,
                    title = "会社名・所属組織の表示",
                    subtitle = "連絡先に登録された会社名や役職を発信画面に表示",
                    checked = config.showCompany,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showCompany = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Info,
                    title = "付加プレフィックス情報の表示",
                    subtitle = "「楽天でんわ」「非通知(184)」など使用中プレフィックスのバッジを表示",
                    checked = config.showPrefixInfo,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showPrefixInfo = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.SimCard,
                    title = "発信SIMスロット情報の表示",
                    subtitle = "通話に使用しているSIM（SIM 1 / SIM 2）のバッジを表示",
                    checked = config.showSimInfo,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showSimInfo = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Timer,
                    title = "通話経過時間タイマー",
                    subtitle = "相手が応答して通話開始後の経過時間（mm:ss）を画面中央に表示",
                    checked = config.showCallTimer,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showCallTimer = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Mic,
                    title = "消音（ミュート）ボタン",
                    subtitle = "発信中および通話中にマイクを消音するボタンを表示",
                    checked = config.showMuteButton,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showMuteButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "スピーカー / Bluetooth切り替えボタン",
                    subtitle = "発信中および通話中にスピーカーフォンやBluetooth機器に切り替えるボタンを表示",
                    checked = config.showSpeakerButton,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showSpeakerButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.Dialpad,
                    title = "ダイヤルキーパッド（DTMF）ボタン",
                    subtitle = "通話中の音声案内番号入力用テンキーを表示するボタン",
                    checked = config.showKeypadButton,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showKeypadButton = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CallToggleRow(
                    icon = Icons.Default.FiberManualRecord,
                    title = "通話録音ボタンの表示",
                    subtitle = "発信・通話中にワンタップでMP3録音を開始・停止できるボタンを表示",
                    checked = config.showRecordButton,
                    onCheckedChange = { viewModel.setOutgoingCallDisplayConfig(config.copy(showRecordButton = it)) }
                )
            }
        }

        // 背景テーマ選択カード
        ThemeSelectorCard(
            selectedTheme = config.theme,
            onThemeSelected = { viewModel.setOutgoingCallDisplayConfig(config.copy(theme = it)) }
        )

        // 初期設定リセットボタン
        OutlinedButton(
            onClick = {
                viewModel.resetOutgoingCallDisplayConfig()
                Toast.makeText(context, "発信画面設定を初期状態にリセットしました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("発信画面の表示設定を初期値に戻す")
        }
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "画面の背景テーマカラー",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val themes = listOf(
                "dark" to ("ダーク（標準）" to Color(0xFF121820)),
                "blue" to ("ディープネイビー" to Color(0xFF0C1726)),
                "purple" to ("ディープパープル" to Color(0xFF160E24)),
                "amoled" to ("AMOLEDブラック" to Color(0xFF000000))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { (themeKey, data) ->
                    val (label, col) = data
                    val isSelected = selectedTheme == themeKey

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = col,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onThemeSelected(themeKey) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "選択中",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
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

@Composable
private fun CallToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (checked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
