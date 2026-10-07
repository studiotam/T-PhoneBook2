package com.example.ui.screens

import android.Manifest
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.PhonebookApplication
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import com.example.util.PhoneNumberFormatter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onCallHistory: (String) -> Unit = {},
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(
            (LocalContext.current.applicationContext as PhonebookApplication).callLogRepository
        )
    )
) {
    val outgoingCalls by viewModel.outgoingCalls.collectAsStateWithLifecycle()
    val incomingCalls by viewModel.incomingCalls.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    val callLogPermissionState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)

    LaunchedEffect(Unit) {
        viewModel.refresh()
        if (!callLogPermissionState.status.isGranted) {
            callLogPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(callLogPermissionState.status.isGranted) {
        if (callLogPermissionState.status.isGranted) {
            viewModel.refresh()
        }
    }

    val currentList = if (selectedTabIndex == 0) outgoingCalls else incomingCalls
    val context = LocalContext.current
    val filteredList = remember(currentList, searchQuery) {
        if (searchQuery.isBlank()) {
            currentList
        } else {
            val q = searchQuery.replace("-", "").trim()
            currentList.filter {
                val formatted = PhoneNumberFormatter.formatForDisplay(context, it.phoneNumber)
                (it.contactName?.contains(searchQuery, ignoreCase = true) == true) ||
                it.phoneNumber.replace("-", "").contains(q) ||
                formatted.replace("-", "").contains(q) ||
                formatted.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通話履歴", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "履歴を更新"
                        )
                    }
                    if (currentList.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "履歴を消去"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // タブ切り替え（発信履歴 / 着信履歴）
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { viewModel.setSelectedTabIndex(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "発信履歴",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (outgoingCalls.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${outgoingCalls.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { viewModel.setSelectedTabIndex(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "着信履歴",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                            if (incomingCalls.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${incomingCalls.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // 権限案内バナー（未許可時のみ表示）
            if (!callLogPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "端末の通話履歴を同期",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "OS全体の着信・発信履歴を表示するには権限が必要です",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { callLogPermissionState.launchPermissionRequest() }
                        ) {
                            Text("許可")
                        }
                    }
                }
            }

            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(if (selectedTabIndex == 0) "発信履歴を名前や電話番号で検索" else "着信履歴を名前や電話番号で検索")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 履歴リスト
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTabIndex == 0) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "検索条件に一致する履歴はありません"
                            } else if (selectedTabIndex == 0) {
                                "発信履歴がありません"
                            } else {
                                "着信履歴がありません"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { call ->
                        CallRecordItem(
                            call = call,
                            onCall = { onCallHistory(PhoneNumberFormatter.toNationalDigits(context, call.phoneNumber)) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // 削除確認ダイアログ
    if (showClearDialog) {
        val tabTitle = if (selectedTabIndex == 0) "発信履歴" else "着信履歴"
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("${tabTitle}の消去") },
            text = { Text("アプリ内に保存されている${tabTitle}を消去しますか？\n（※端末のシステム履歴は保持されます）") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory(isOutgoingTab = selectedTabIndex == 0)
                        showClearDialog = false
                    }
                ) {
                    Text("消去する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun CallRecordItem(
    call: CallRecord,
    onCall: () -> Unit = {}
) {
    val dateString = DateFormat.format("yyyy/MM/dd HH:mm", Date(call.timestamp)).toString()

    val (icon, iconTint, typeLabel) = when (call.callType) {
        CallType.OUTGOING -> Triple(
            Icons.AutoMirrored.Filled.CallMade,
            MaterialTheme.colorScheme.primary,
            "発信"
        )
        CallType.INCOMING -> Triple(
            Icons.AutoMirrored.Filled.CallReceived,
            Color(0xFF2E7D32), // 緑
            "着信"
        )
        CallType.MISSED -> Triple(
            Icons.AutoMirrored.Filled.CallMissed,
            MaterialTheme.colorScheme.error,
            "不在着信"
        )
        CallType.REJECTED -> Triple(
            Icons.AutoMirrored.Filled.CallMissed,
            MaterialTheme.colorScheme.error,
            "着信拒否"
        )
        CallType.OTHER -> Triple(
            Icons.Default.Call,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "通話"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCall() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // アイコンサークル
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 名前・電話番号・日時・通話時間
        val context = LocalContext.current
        val formattedNumber = remember(call.phoneNumber) {
            PhoneNumberFormatter.formatForDisplay(context, call.phoneNumber)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = call.contactName ?: formattedNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (call.callType == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                )
                if (call.callType == CallType.MISSED) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "不在",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (call.contactName != null) {
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (call.durationSeconds > 0) {
                    Spacer(Modifier.width(8.dp))
                    val minutes = call.durationSeconds / 60
                    val seconds = call.durationSeconds % 60
                    val durationText = if (minutes > 0) "${minutes}分${seconds}秒" else "${seconds}秒"
                    Text(
                        text = "• 通話時間: $durationText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 発信ボタン
        IconButton(
            onClick = onCall,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "${call.phoneNumber} に電話を発信",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
