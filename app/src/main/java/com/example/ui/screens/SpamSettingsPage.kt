package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SpamNumberEntity
import com.example.data.repository.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamSettingsPage(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val syncState by viewModel.spamSyncState.collectAsStateWithLifecycle()
    val currentSpreadsheetSource by viewModel.spamSpreadsheetSource.collectAsStateWithLifecycle()
    val currentCallingCode by viewModel.spamCallingCode.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.spamLastSyncTime.collectAsStateWithLifecycle()
    val totalCount by viewModel.spamTotalCount.collectAsStateWithLifecycle()
    val autoBlockThreshold by viewModel.spamAutoBlockThreshold.collectAsStateWithLifecycle()
    val blockUnregisteredInternational by viewModel.spamBlockUnregisteredInternational.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.spamAutoSyncEnabled.collectAsStateWithLifecycle()
    val spamList by viewModel.spamList.collectAsStateWithLifecycle()
    val testResult by viewModel.testSpamResult.collectAsStateWithLifecycle()
    val testIsInternational by viewModel.testIsInternational.collectAsStateWithLifecycle()
    val testIsBlockedAsInternational by viewModel.testIsBlockedAsInternational.collectAsStateWithLifecycle()
    val spamContactMatches by viewModel.spamContactMatches.collectAsStateWithLifecycle()

    var testInput by remember { mutableStateOf("") }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var numberToDelete by remember { mutableStateOf<String?>(null) }

    // RoleManager チェック
    var isCallScreeningRoleHeld by remember { mutableStateOf(false) }

    fun checkRoleStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            isCallScreeningRoleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            isCallScreeningRoleHeld = true
        }
    }

    LaunchedEffect(Unit) {
        checkRoleStatus()
        viewModel.checkSpamMatchesInContacts()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkRoleStatus()
        if (isCallScreeningRoleHeld) {
            Toast.makeText(context, "着信スクリーニング権限が許可されました", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "権限が許可されませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. 連絡先未登録の国際電話着信拒否設定 (最上部に配置) ---
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .background(
                                    if (blockUnregisteredInternational)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (blockUnregisteredInternational)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "未登録の国際電話を着信拒否",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (blockUnregisteredInternational) "有効（未登録の国際着信を自動拒否）" else "無効（すべての国際着信を許可）",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (blockUnregisteredInternational) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (blockUnregisteredInternational) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    Switch(
                        checked = blockUnregisteredInternational,
                        onCheckedChange = { viewModel.setSpamBlockUnregisteredInternational(it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "• 連絡先に登録されていない海外からの国際電話（国番号 +$currentCallingCode 以外の「+」や「010」で始まる番号）からの着信を自動切断・拒否します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "※ 端末の連絡帳に登録されている海外の家族・知人・取引先の番号は、本設定がONでも通常通り着信可能です。",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- 2. 着信スクリーニング権限 ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isCallScreeningRoleHeld) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCallScreeningRoleHeld) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isCallScreeningRoleHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "着信スクリーニング権限",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCallScreeningRoleHeld) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = if (isCallScreeningRoleHeld) "有効" else "未許可",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCallScreeningRoleHeld) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isCallScreeningRoleHeld) {
                        "AndroidシステムのCallScreeningServiceとして登録されています。着信時に自動でデータベースと照合されます。"
                    } else {
                        "着信時の自動判定・自動切断を行うには、システムに着信スクリーニングアプリとして許可する必要があります。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isCallScreeningRoleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, "この端末ではロール要求が利用できません", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Screeningロールを要求する")
                    }
                }
            }
        }

        // --- 4. 迷惑電話リスト取得セクション ---
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "迷惑電話リスト取得",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "最新の迷惑電話データベースをオンラインから取得し、端末のローカルデータベースを同期します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 国番号プレフィックス（編集不可で表示）
                OutlinedTextField(
                    value = currentCallingCode.ifBlank { "81" },
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("国番号プレフィックス") },
                    leadingIcon = {
                        Icon(Icons.Default.Public, contentDescription = null)
                    },
                    supportingText = {
                        Text("※ 日本 (+81) の迷惑電話リストに固定されています")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 自動取得設定 (WorkManager 3日周期)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自動取得",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "アプリがバックグラウンドになった際に3日に1度、WorkManagerで最新情報をチェックして自動取得します。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { viewModel.setSpamAutoSyncEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 番号リストを同期ボタン
                Button(
                    onClick = {
                        viewModel.syncSpamDatabase()
                    },
                    enabled = syncState !is SyncState.Syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (syncState is SyncState.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("番号リストを同期中...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("番号リストを同期")
                    }
                }

                // 同期結果ステータス
                Spacer(modifier = Modifier.height(8.dp))
                when (val state = syncState) {
                    is SyncState.Success -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "✅ 番号リスト同期成功！",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "追加/更新: ${state.addedCount}件 | 削除: ${state.revokedCount}件 | 現在DB総件数: ${state.totalCount}件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    is SyncState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    else -> {
                        if (lastSyncTime.isNotBlank()) {
                            Text(
                                text = "最終同期日時: $lastSyncTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 5. 自動着信拒否のしきい値設定 ---
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "着信拒否・警告の動作設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "危険度スコア（1〜5）に応じた着信時の挙動を指定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                ThresholdDropdown(
                    selectedThreshold = autoBlockThreshold,
                    onThresholdSelected = { viewModel.setSpamAutoBlockThreshold(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "• 危険度 $autoBlockThreshold 以上: 着信拒否（自動切断）＋ 履歴に記録",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "• 危険度 $autoBlockThreshold 未満: 通話は通すが、画面・通知で警告を表示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 6. 番号照合テスト ---
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "電話番号の照合テスト",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "任意の番号（050... や +81..., +1...）を入力して、DB照合・国際電話判定結果をテストできます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = testInput,
                        onValueChange = {
                            testInput = it
                            viewModel.testCheckPhoneNumber(it)
                        },
                        label = { Text("テスト電話番号") },
                        placeholder = { Text("例: 05012345678 または +12025550123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.testCheckPhoneNumber(testInput) },
                        enabled = testInput.isNotBlank()
                    ) {
                        Text("判定")
                    }
                }

                if (testInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (testIsBlockedAsInternational) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🚨 【国際電話着信拒否】対象",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "連絡先に登録されていない国際電話番号のため、着信拒否（自動切断）されます。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else if (testResult != null) {
                        val res = testResult!!
                        val willBlock = res.riskScore >= autoBlockThreshold
                        Surface(
                            color = if (willBlock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (willBlock) Icons.Default.Block else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (willBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (willBlock) "🚨 【着信自動拒否】対象" else "⚠️ 【注意喚起】対象",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "番号: ${res.number}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "カテゴリ: ${res.category}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!res.targetEntity.isNullOrBlank()) {
                                    Text(
                                        text = "対象事業者/名目: ${res.targetEntity}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "危険度スコア: ${res.riskScore} / 5",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!res.lastConfirmedDate.isNullOrBlank()) {
                                    Text(
                                        text = "最終確認日: ${res.lastConfirmedDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (testIsInternational)
                                        "連絡先に登録済みの国際電話番号（または国際拒否OFF）のため通話可能です"
                                    else
                                        "迷惑電話リストには登録されていません（通常通話）",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 6. 登録件数表示 & データベース管理 ---
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "登録済み迷惑電話データベース",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "現在登録件数: ${totalCount} 件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (lastSyncTime.isNotBlank()) {
                            Text(
                                text = "最終同期日時: $lastSyncTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showAddManualDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "手動追加")
                        }
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            enabled = totalCount > 0
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "全件クリア")
                        }
                    }
                }
            }
        }

        // --- 7. 連絡先に登録されている迷惑電話番号の警告・削除管理 ---
        if (spamContactMatches.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "連絡先に登録されている迷惑電話番号 (${spamContactMatches.size}件)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "以下の電話番号はあなたの連絡帳に登録されていますが、迷惑電話データベースと一致しています。意図しない着信拒否を防ぐため、必要に応じて迷惑電話リストから削除してください。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    spamContactMatches.forEach { match ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = match.contactName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = match.contactPhoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "カテゴリ: ${match.spamEntity.category} (危険度: ${match.spamEntity.riskScore}/5)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        numberToDelete = match.spamEntity.number
                                    }
                                ) {
                                    Text(
                                        text = "リストから削除",
                                        color = MaterialTheme.colorScheme.error,
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

    // 手動追加ダイアログ
    if (showAddManualDialog) {
        AddManualSpamDialog(
            defaultCallingCode = currentCallingCode.ifBlank { "81" },
            onDismiss = { showAddManualDialog = false },
            onAdd = { number, callingCode, category, targetEntity, riskScore ->
                viewModel.addManualSpamNumber(number, callingCode, category, targetEntity, riskScore)
                showAddManualDialog = false
                Toast.makeText(context, "番号を追加しました: $number", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 削除確認ダイアログ
    if (numberToDelete != null) {
        val num = numberToDelete!!
        AlertDialog(
            onDismissRequest = { numberToDelete = null },
            title = { Text("番号の削除") },
            text = { Text("$num を迷惑電話リストから削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSpamNumber(num)
                        numberToDelete = null
                        Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { numberToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 全件クリア確認ダイアログ
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("データベース全件クリア") },
            text = { Text("登録されているすべての迷惑電話番号（${totalCount}件）を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllSpamDatabase()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "全件クリアしました", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("全件クリア", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SpamItemCard(
    item: SpamNumberEntity,
    autoBlockThreshold: Int,
    onDelete: () -> Unit
) {
    val willBlock = item.riskScore >= autoBlockThreshold
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.number,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (willBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = if (willBlock) "拒否 (Lv.${item.riskScore})" else "警告 (Lv.${item.riskScore})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.category + if (!item.targetEntity.isNullOrBlank()) " • ${item.targetEntity}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.lastConfirmedDate.isNullOrBlank()) {
                    Text(
                        text = "最終確認: ${item.lastConfirmedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdDropdown(
    selectedThreshold: Int,
    onThresholdSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        1 to "危険度 1以上（すべての検出番号を着信拒否）",
        2 to "危険度 2以上を着信拒否（低リスクも拒否）",
        3 to "危険度 3以上を着信拒否（中リスク以上）",
        4 to "危険度 4以上を着信拒否（標準・推奨）",
        5 to "危険度 5のみ着信拒否（最凶悪番号のみ拒否）"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selectedThreshold }?.second ?: "危険度 $selectedThreshold 以上を着信拒否",
            onValueChange = {},
            readOnly = true,
            label = { Text("自動着信拒否のしきい値") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (level, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onThresholdSelected(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddManualSpamDialog(
    defaultCallingCode: String,
    onDismiss: () -> Unit,
    onAdd: (number: String, callingCode: String, category: String, targetEntity: String?, riskScore: Int) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var callingCode by remember { mutableStateOf(defaultCallingCode) }
    var category by remember { mutableStateOf("営業・勧誘電話") }
    var targetEntity by remember { mutableStateOf("") }
    var riskScore by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("迷惑電話の手動登録") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("電話番号") },
                    placeholder = { Text("例: 05012345678 / +815012345678") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = callingCode,
                    onValueChange = { callingCode = it },
                    label = { Text("国番号プレフィックス") },
                    placeholder = { Text("例: 81") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("カテゴリ") },
                    placeholder = { Text("例: 悪質勧誘, 詐欺の疑い") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetEntity,
                    onValueChange = { targetEntity = it },
                    label = { Text("名目・事業者名（任意）") },
                    placeholder = { Text("例: 電力切替勧誘") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "危険度スコア: $riskScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..5).forEach { score ->
                        FilterChip(
                            selected = riskScore == score,
                            onClick = { riskScore = score },
                            label = { Text("$score") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onAdd(
                            number.trim(),
                            callingCode.trim().ifBlank { "81" },
                            category.trim(),
                            targetEntity.trim().ifBlank { null },
                            riskScore
                        )
                    }
                },
                enabled = number.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
