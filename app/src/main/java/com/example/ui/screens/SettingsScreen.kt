package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import com.example.util.DefaultDialerHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.PhonebookApplication
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ExcludedPrefix
import com.example.domain.model.PhonePrefix
import com.example.domain.model.SimInfo
import com.example.ui.components.CountdownCallDialog
import com.example.ui.components.LongPressCallButton
import com.example.ui.components.SlideToCallButton
import com.example.ui.components.performHaptic
import com.example.util.AppSignatureHelper
import com.example.util.SignatureInfo
import com.example.util.SimHelper

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * 設定画面の各ページ定義
 */
enum class SettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    DRIVE_BACKUP(
        title = "Googleアカウント & Driveバックアップ",
        subtitle = "アカウント選択・設定のバックアップと復元",
        icon = Icons.Default.CloudSync
    ),
    CONTACT_FIELDS(
        title = "連絡先項目の表示設定",
        subtitle = "表に表示する項目・アコーディオン設定",
        icon = Icons.Default.ViewAgenda
    ),
    SPAM_CALL(
        title = "迷惑電話対策・着信拒否",
        subtitle = "番号リスト同期と自動拒否・警告設定",
        icon = Icons.Default.Security
    ),
    GROUPS(
        title = "グループ表示設定",
        subtitle = "連絡先グループの表示・非表示の選択",
        icon = Icons.Default.Group
    ),
    PREFIX(
        title = "プレフィックス設定・編集",
        subtitle = "発信プレフィックスの追加・編集・削除",
        icon = Icons.Default.Tune
    ),
    CALL(
        title = "通話・一般設定",
        subtitle = "誤発信防止ダイアログ等の設定",
        icon = Icons.Default.Call
    ),
    INCOMING_CALL(
        title = "電話の発着信画面設定",
        subtitle = "発信・着信画面の表示と機能カスタマイズ",
        icon = Icons.Default.PhoneInTalk
    ),
    RECORDING(
        title = "通話録音・保存設定",
        subtitle = "録音データ一覧・再生・自動録音設定 (MP3)",
        icon = Icons.Default.Mic
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val app = context.applicationContext as PhonebookApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            app.settingsRepository,
            app.googleAuthManager,
            app.contactsRepository,
            app.spamSyncRepository,
            app.googleDriveBackupRepository,
            app.callRecordingRepository
        )
    )

    val prefixes by viewModel.prefixes.collectAsStateWithLifecycle()
    val selectedPrefixId by viewModel.selectedPrefixId.collectAsStateWithLifecycle()
    val selectedPrefixIdSim1 by viewModel.selectedPrefixIdSim1.collectAsStateWithLifecycle()
    val selectedPrefixIdSim2 by viewModel.selectedPrefixIdSim2.collectAsStateWithLifecycle()
    val sim1PhoneNumber by viewModel.sim1PhoneNumber.collectAsStateWithLifecycle()
    val sim2PhoneNumber by viewModel.sim2PhoneNumber.collectAsStateWithLifecycle()
    val excludedPrefixes by viewModel.excludedPrefixes.collectAsStateWithLifecycle()
    val confirmCall by viewModel.confirmCall.collectAsStateWithLifecycle()
    val callConfirmMethod by viewModel.callConfirmMethod.collectAsStateWithLifecycle()
    val defaultSimMode by viewModel.defaultSimMode.collectAsStateWithLifecycle()
    val useRakutenLink by viewModel.useRakutenLink.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val deviceAccounts by viewModel.deviceAccounts.collectAsStateWithLifecycle()
    val deviceGroups by viewModel.deviceGroups.collectAsStateWithLifecycle()
    val hiddenGroupIds by viewModel.hiddenGroupIds.collectAsStateWithLifecycle()
    val rememberLastGroup by viewModel.rememberLastGroup.collectAsStateWithLifecycle()
    val isLoadingGroups by viewModel.isLoadingGroups.collectAsStateWithLifecycle()

    var currentPage by remember { mutableStateOf(SettingsPage.DRIVE_BACKUP) }
    var pageDropdownExpanded by remember { mutableStateOf(false) }

    var showAddPrefixDialog by remember { mutableStateOf(false) }
    var editingPrefix by remember { mutableStateOf<PhonePrefix?>(null) }
    var prefixToDelete by remember { mutableStateOf<PhonePrefix?>(null) }

    var showAddExcludedDialog by remember { mutableStateOf(false) }
    var editingExcluded by remember { mutableStateOf<ExcludedPrefix?>(null) }
    var excludedToDelete by remember { mutableStateOf<ExcludedPrefix?>(null) }
    var showResetExcludedConfirmDialog by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label をコピーしました", Toast.LENGTH_SHORT).show()
    }

    // 画面表示時に端末のアカウント一覧を再読み込み
    LaunchedEffect(Unit) {
        viewModel.loadDeviceAccounts()
    }

    // 端末のGoogleアカウント選択ダイアログ用Launcher
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val selectedAccount = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!selectedAccount.isNullOrBlank()) {
                viewModel.selectGoogleAccount(selectedAccount)
                Toast.makeText(context, "アカウントを選択しました: $selectedAccount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("設定", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // プルダウン切り替えカード
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pageDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentPage.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "設定ページ（タップして切り替え）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentPage.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentPage.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "ページを選択",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = pageDropdownExpanded,
                    onDismissRequest = { pageDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    SettingsPage.entries.forEach { page ->
                        val isSelected = page == currentPage
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = page.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = page.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "選択中",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            onClick = {
                                currentPage = page
                                pageDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 選択されたページのコンテンツ
            when (currentPage) {
                SettingsPage.CONTACT_FIELDS -> {
                    ContactFieldsSettingsPage(viewModel = viewModel)
                }
                SettingsPage.DRIVE_BACKUP -> {
                    DriveBackupSettingsPage(
                        viewModel = viewModel,
                        onOpenAccountPicker = {
                            accountPickerLauncher.launch(viewModel.createChooseAccountIntent())
                        }
                    )
                }
                SettingsPage.SPAM_CALL -> {
                    SpamSettingsPage(viewModel = viewModel)
                }
                SettingsPage.GROUPS -> {
                    GroupVisibilitySettingsSection(
                        groups = deviceGroups,
                        hiddenGroupIds = hiddenGroupIds,
                        rememberLastGroup = rememberLastGroup,
                        isLoading = isLoadingGroups,
                        deviceAccounts = deviceAccounts,
                        defaultAccount = userEmail,
                        onToggleRememberLastGroup = { enabled ->
                            viewModel.setRememberLastGroup(enabled)
                        },
                        onToggleVisibility = { groupId, isVisible ->
                            viewModel.setGroupVisibility(groupId, isVisible)
                        },
                        onSetAllVisibility = { isVisible ->
                            viewModel.setAllGroupsVisibility(isVisible)
                        },
                        onResetVisibility = {
                            viewModel.resetGroupVisibility()
                        },
                        onReorderGroups = { newOrderIds ->
                            viewModel.setGroupOrder(newOrderIds)
                        },
                        onResetOrder = {
                            viewModel.resetGroupOrder()
                        },
                        onCreateGroup = { title, account ->
                            viewModel.createGroup(title, account) { success, msg ->
                                Toast.makeText(context, if (success) "グループ「$title」を追加しました" else (msg ?: "グループ追加に失敗しました"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUpdateGroupTitle = { groupId, newTitle ->
                            viewModel.updateGroupTitle(groupId, newTitle) { success, msg ->
                                Toast.makeText(context, if (success) "グループ名を「$newTitle」に変更しました" else (msg ?: "グループ名変更に失敗しました"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteGroup = { groupId, groupTitle ->
                            viewModel.deleteGroup(groupId) { success, msg ->
                                Toast.makeText(context, if (success) "グループ「$groupTitle」を削除しました" else (msg ?: "グループ削除に失敗しました"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRefresh = {
                            viewModel.loadDeviceGroups()
                        }
                    )
                }
                SettingsPage.PREFIX -> {
                    PrefixSettingsSection(
                        prefixes = prefixes,
                        selectedPrefixId = selectedPrefixId,
                        selectedPrefixIdSim1 = selectedPrefixIdSim1,
                        selectedPrefixIdSim2 = selectedPrefixIdSim2,
                        onSelectPrefix = { viewModel.setSelectedPrefixId(it) },
                        onSelectPrefixForSim = { simSlot, id -> viewModel.setSelectedPrefixIdForSim(simSlot, id) },
                        onAddClick = { showAddPrefixDialog = true },
                        onEditClick = { editingPrefix = it },
                        onDeleteClick = { prefixToDelete = it },
                        excludedPrefixes = excludedPrefixes,
                        onAddExcludedClick = { showAddExcludedDialog = true },
                        onEditExcludedClick = { editingExcluded = it },
                        onDeleteExcludedClick = { excludedToDelete = it },
                        onResetExcludedClick = { showResetExcludedConfirmDialog = true }
                    )
                }
                SettingsPage.CALL -> {
                    CallSettingsSection(
                        confirmCall = confirmCall,
                        onConfirmCallChange = { viewModel.setConfirmCall(it) },
                        callConfirmMethod = callConfirmMethod,
                        onCallConfirmMethodChange = { viewModel.setCallConfirmMethod(it) },
                        defaultSimMode = defaultSimMode,
                        onDefaultSimModeChange = { viewModel.setDefaultSimMode(it) },
                        useRakutenLink = useRakutenLink,
                        onUseRakutenLinkChange = { viewModel.setUseRakutenLink(it) },
                        sim1PhoneNumber = sim1PhoneNumber,
                        sim2PhoneNumber = sim2PhoneNumber,
                        onSetSimPhoneNumber = { slot, num -> viewModel.setSimPhoneNumber(slot, num) },
                        currentPrefix = prefixes.firstOrNull { it.id == selectedPrefixId },
                        onNavigateToPrefixSettings = { currentPage = SettingsPage.PREFIX }
                    )
                }
                SettingsPage.INCOMING_CALL -> {
                    IncomingCallSettingsPage(viewModel = viewModel)
                }
                SettingsPage.RECORDING -> {
                    CallRecordingsPage(viewModel = viewModel)
                }
            }
        }
    }

    // プレフィックス追加ダイアログ
    if (showAddPrefixDialog) {
        var newName by remember { mutableStateOf("") }
        var newPrefixNumber by remember { mutableStateOf("") }
        var targetSim by remember { mutableStateOf(PhonePrefix.TARGET_SIM_BOTH) }
        var setAsDefault by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddPrefixDialog = false },
            title = { Text("プレフィックスを追加") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("プレフィックス名") },
                        placeholder = { Text("例: 楽天でんわ, 非通知, 社用") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPrefixNumber,
                        onValueChange = { newPrefixNumber = it },
                        label = { Text("付加番号") },
                        placeholder = { Text("例: 184, 003768") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 対象SIM選択
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "適用対象SIM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val simChoiceList = listOf(
                            PhonePrefix.TARGET_SIM_BOTH to "両方のSIM（共通）",
                            PhonePrefix.TARGET_SIM_1 to "SIM 1 専用",
                            PhonePrefix.TARGET_SIM_2 to "SIM 2 専用"
                        )
                        simChoiceList.forEach { (simKey, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { targetSim = simKey }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = targetSim == simKey,
                                    onClick = { targetSim = simKey }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { setAsDefault = !setAsDefault }
                    ) {
                        Checkbox(
                            checked = setAsDefault,
                            onCheckedChange = { setAsDefault = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when (targetSim) {
                                PhonePrefix.TARGET_SIM_1 -> "SIM 1 のデフォルトプレフィックスにする"
                                PhonePrefix.TARGET_SIM_2 -> "SIM 2 のデフォルトプレフィックスにする"
                                else -> "デフォルトプレフィックスにする（SIM 1・2共通）"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPrefixNumber.isNotBlank()) {
                            viewModel.addPrefix(newName, newPrefixNumber, targetSim, setAsSelected = setAsDefault)
                            showAddPrefixDialog = false
                        }
                    },
                    enabled = newName.isNotBlank() && newPrefixNumber.isNotBlank()
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPrefixDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // プレフィックス編集ダイアログ
    editingPrefix?.let { target ->
        var editName by remember(target) { mutableStateOf(target.name) }
        var editNumber by remember(target) { mutableStateOf(target.prefix) }
        var editTargetSim by remember(target) { mutableStateOf(target.targetSim) }

        AlertDialog(
            onDismissRequest = { editingPrefix = null },
            title = { Text("プレフィックスを編集") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("プレフィックス名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNumber,
                        onValueChange = { editNumber = it },
                        label = { Text("付加番号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 対象SIM選択
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "適用対象SIM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val simChoiceList = listOf(
                            PhonePrefix.TARGET_SIM_BOTH to "両方のSIM（共通）",
                            PhonePrefix.TARGET_SIM_1 to "SIM 1 専用",
                            PhonePrefix.TARGET_SIM_2 to "SIM 2 専用"
                        )
                        simChoiceList.forEach { (simKey, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editTargetSim = simKey }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = editTargetSim == simKey,
                                    onClick = { editTargetSim = simKey }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editNumber.isNotBlank()) {
                            viewModel.updatePrefix(target.id, editName, editNumber, editTargetSim)
                            editingPrefix = null
                        }
                    },
                    enabled = editName.isNotBlank() && editNumber.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPrefix = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // プレフィックス削除確認ダイアログ
    prefixToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { prefixToDelete = null },
            title = { Text("プレフィックスを削除") },
            text = { Text("「${target.name}」(${target.prefix}) を削除してもよろしいですか？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePrefix(target.id)
                        prefixToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { prefixToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 対象外番号（除外先頭番号）追加ダイアログ
    if (showAddExcludedDialog) {
        var pattern by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExcludedDialog = false },
            title = { Text("対象外番号（除外先頭番号）を追加") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "指定した番号で始まる電話番号へ発信する際は、プレフィックスを付加せず直接発信します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("先頭番号パターン") },
                        placeholder = { Text("例: 110, 0120, 911, 0570") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("メモ・説明（任意）") },
                        placeholder = { Text("例: 警察(緊急通報), フリーダイヤル") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pattern.isNotBlank()) {
                            viewModel.addExcludedPrefix(pattern.trim(), description.trim())
                            showAddExcludedDialog = false
                        }
                    },
                    enabled = pattern.isNotBlank()
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExcludedDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 対象外番号（除外先頭番号）編集ダイアログ
    editingExcluded?.let { target ->
        var pattern by remember(target) { mutableStateOf(target.pattern) }
        var description by remember(target) { mutableStateOf(target.description) }

        AlertDialog(
            onDismissRequest = { editingExcluded = null },
            title = { Text("対象外番号を編集") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("先頭番号パターン") },
                        placeholder = { Text("例: 110, 0120, 911") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("メモ・説明") },
                        placeholder = { Text("例: 警察, フリーダイヤル") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pattern.isNotBlank()) {
                            viewModel.updateExcludedPrefix(target.id, pattern.trim(), description.trim())
                            editingExcluded = null
                        }
                    },
                    enabled = pattern.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExcluded = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 対象外番号削除確認ダイアログ
    excludedToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { excludedToDelete = null },
            title = { Text("対象外番号を削除") },
            text = { Text("「${target.pattern}${if (target.description.isNotEmpty()) " : " + target.description else ""}」を除外リストから削除してもよろしいですか？\n削除後はこの番号にもプレフィックスが付加されるようになります。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExcludedPrefix(target.id)
                        excludedToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { excludedToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 対象外番号初期化確認ダイアログ
    if (showResetExcludedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetExcludedConfirmDialog = false },
            title = { Text("対象外番号を初期値にリセット") },
            text = { Text("除外番号リストを標準の初期設定（日本の緊急通報 110/118/119、特番 117/177、フリーダイヤル 0120/0800、ナビダイヤル 0570 等）にリセットしますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDefaultExcludedPrefixes()
                        showResetExcludedConfirmDialog = false
                    }
                ) {
                    Text("リセット")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetExcludedConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

/**
 * プレフィックス設定・編集ページ
 */
@Composable
private fun PrefixSettingsSection(
    prefixes: List<PhonePrefix>,
    selectedPrefixId: String?,
    selectedPrefixIdSim1: String?,
    selectedPrefixIdSim2: String?,
    onSelectPrefix: (String?) -> Unit,
    onSelectPrefixForSim: (simSlot: Int, prefixId: String?) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (PhonePrefix) -> Unit,
    onDeleteClick: (PhonePrefix) -> Unit,
    excludedPrefixes: List<ExcludedPrefix>,
    onAddExcludedClick: () -> Unit,
    onEditExcludedClick: (ExcludedPrefix) -> Unit,
    onDeleteExcludedClick: (ExcludedPrefix) -> Unit,
    onResetExcludedClick: () -> Unit
) {
    val context = LocalContext.current
    val availableSims = remember(context) { SimHelper.getAvailableSims(context) }
    val hasSim2 = availableSims.size >= 2 || true // Show both SIM 1 and SIM 2 for dual-SIM awareness

    val sim1Prefix = prefixes.firstOrNull { it.id == selectedPrefixIdSim1 }
    val sim2Prefix = prefixes.firstOrNull { it.id == selectedPrefixIdSim2 }

    var sim1DropdownExpanded by remember { mutableStateOf(false) }
    var sim2DropdownExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ヘッダー＆追加ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "プレフィックスの管理・設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SIMごとに異なるデフォルトプレフィックスを設定したり、通話対象外の番号（110, 0120等）を登録できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAddClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("追加")
            }
        }

        // SIM別デフォルトプレフィックス設定カード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SIMごとのデフォルトプレフィックス",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "発信に使用するSIMスロットに応じて、自動で付加されるデフォルトプレフィックスを設定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // SIM 1 設定
                val sim1CompatiblePrefixes = remember(prefixes) {
                    prefixes.filter { it.targetSim == PhonePrefix.TARGET_SIM_1 || it.targetSim == PhonePrefix.TARGET_SIM_BOTH }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SIM 1",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SIM 1 デフォルト",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { sim1DropdownExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = sim1Prefix?.name ?: "なし（通常発信）",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sim1DropdownExpanded,
                                    onDismissRequest = { sim1DropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("プレフィックスなし（通常発信）") },
                                        onClick = {
                                            onSelectPrefixForSim(0, null)
                                            sim1DropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            RadioButton(
                                                selected = selectedPrefixIdSim1 == null,
                                                onClick = null
                                            )
                                        }
                                    )
                                    if (sim1CompatiblePrefixes.isNotEmpty()) {
                                        HorizontalDivider()
                                        sim1CompatiblePrefixes.forEach { item ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(item.name, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "付加番号: ${item.prefix} (${PhonePrefix.getSimLabel(item.targetSim)})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    onSelectPrefixForSim(0, item.id)
                                                    sim1DropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    RadioButton(
                                                        selected = selectedPrefixIdSim1 == item.id,
                                                        onClick = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (sim1Prefix != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "付加番号: ${sim1Prefix.prefix} (${PhonePrefix.getSimLabel(sim1Prefix.targetSim)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // SIM 2 設定
                val sim2CompatiblePrefixes = remember(prefixes) {
                    prefixes.filter { it.targetSim == PhonePrefix.TARGET_SIM_2 || it.targetSim == PhonePrefix.TARGET_SIM_BOTH }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SIM 2",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SIM 2 デフォルト",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { sim2DropdownExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = sim2Prefix?.name ?: "なし（通常発信）",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sim2DropdownExpanded,
                                    onDismissRequest = { sim2DropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("プレフィックスなし（通常発信）") },
                                        onClick = {
                                            onSelectPrefixForSim(1, null)
                                            sim2DropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            RadioButton(
                                                selected = selectedPrefixIdSim2 == null,
                                                onClick = null
                                            )
                                        }
                                    )
                                    if (sim2CompatiblePrefixes.isNotEmpty()) {
                                        HorizontalDivider()
                                        sim2CompatiblePrefixes.forEach { item ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(item.name, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "付加番号: ${item.prefix} (${PhonePrefix.getSimLabel(item.targetSim)})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    onSelectPrefixForSim(1, item.id)
                                                    sim2DropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    RadioButton(
                                                        selected = selectedPrefixIdSim2 == item.id,
                                                        onClick = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (sim2Prefix != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "付加番号: ${sim2Prefix.prefix} (${PhonePrefix.getSimLabel(sim2Prefix.targetSim)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 登録済みプレフィックス一覧
        if (prefixes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "登録済みプレフィックス一覧 (${prefixes.size}件)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                prefixes.forEach { item ->
                    val isSim1Default = selectedPrefixIdSim1 == item.id
                    val isSim2Default = selectedPrefixIdSim2 == item.id
                    val canBeSim1 = item.targetSim == PhonePrefix.TARGET_SIM_1 || item.targetSim == PhonePrefix.TARGET_SIM_BOTH
                    val canBeSim2 = item.targetSim == PhonePrefix.TARGET_SIM_2 || item.targetSim == PhonePrefix.TARGET_SIM_BOTH

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSim1Default || isSim2Default) 2.dp else 1.dp,
                            color = if (isSim1Default || isSim2Default) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = when (item.targetSim) {
                                                PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.tertiaryContainer
                                                PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = PhonePrefix.getSimLabel(item.targetSim),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when (item.targetSim) {
                                                    PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.onTertiaryContainer
                                                    PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "付加番号: ${item.prefix}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row {
                                    IconButton(onClick = { onEditClick(item) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "編集",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteClick(item) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "削除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // デフォルト設定ボタン/バッジ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canBeSim1) {
                                    FilterChip(
                                        selected = isSim1Default,
                                        onClick = {
                                            onSelectPrefixForSim(0, if (isSim1Default) null else item.id)
                                        },
                                        label = {
                                            Text(if (isSim1Default) "SIM 1 デフォルト中" else "SIM 1 デフォルトに設定")
                                        },
                                        leadingIcon = {
                                            if (isSim1Default) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                                if (canBeSim2) {
                                    FilterChip(
                                        selected = isSim2Default,
                                        onClick = {
                                            onSelectPrefixForSim(1, if (isSim2Default) null else item.id)
                                        },
                                        label = {
                                            Text(if (isSim2Default) "SIM 2 デフォルト中" else "SIM 2 デフォルトに設定")
                                        },
                                        leadingIcon = {
                                            if (isSim2Default) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("登録されたプレフィックスはありません", fontWeight = FontWeight.Medium)
                    Text(
                        "「追加」ボタンから楽天でんわ等の事業者番号や非通知(184)を登録できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("プレフィックスを追加")
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // プレフィックス対象外番号（除外先頭番号）セクション
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "対象外番号（除外先頭番号）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "緊急通報やフリーダイヤルなど、プレフィックスを付けずに直接発信したい電話番号の先頭部分を登録します。国や地域のルールに合わせて自由に追加・削除可能です。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onAddExcludedClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("除外番号を追加")
            }
            OutlinedButton(
                onClick = onResetExcludedClick
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("初期値に戻す")
            }
        }

        if (excludedPrefixes.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                excludedPrefixes.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = item.pattern,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.description.isNotBlank()) item.description else "（説明なし）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (item.description.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "「${item.pattern}」で始まる番号はプレフィックス対象外",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEditExcludedClick(item) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "編集",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteExcludedClick(item) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("登録された除外番号はありません", fontWeight = FontWeight.Medium)
                    Text(
                        "除外番号が空の場合、すべての発信番号にプレフィックスが付加されます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(onClick = onResetExcludedClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("標準の除外番号を復元")
                    }
                }
            }
        }

        // ガイドカード
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "プレフィックス機能のポイント",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    "・発信時に選択されたプレフィックスが電話番号の先頭に自動付加されます（すでにプレフィックスで始まる番号の場合は二重付加されません）。\n・通話履歴にはプレフィックスを除去した本来の電話番号で保存されます。\n・ダイヤラー画面のプルダウンからも通話ごとにワンタップでプレフィックスを切り替え可能です。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private data class CallConfirmOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badge: String? = null
)

private data class SimModeOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val badge: String? = null
)

/**
 * 通話・一般設定ページ
 */
@Composable
private fun CallSettingsSection(
    confirmCall: Boolean,
    onConfirmCallChange: (Boolean) -> Unit,
    callConfirmMethod: String,
    onCallConfirmMethodChange: (String) -> Unit,
    defaultSimMode: String,
    onDefaultSimModeChange: (String) -> Unit,
    useRakutenLink: Boolean = false,
    onUseRakutenLinkChange: (Boolean) -> Unit = {},
    sim1PhoneNumber: String?,
    sim2PhoneNumber: String?,
    onSetSimPhoneNumber: (Int, String?) -> Unit,
    currentPrefix: PhonePrefix?,
    onNavigateToPrefixSettings: () -> Unit
) {
    val context = LocalContext.current
    val isJapan = remember(context) { SimHelper.isJapan(context) }
    var isDefaultDialer by remember { mutableStateOf(DefaultDialerHelper.isDefaultDialer(context)) }
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = DefaultDialerHelper.isDefaultDialer(context)
    }
    var simRefreshTrigger by remember { mutableStateOf(0) }
    var hasPhonePermission by remember(simRefreshTrigger) {
        mutableStateOf(SimHelper.hasPhoneStatePermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _: Map<String, Boolean> ->
        hasPhonePermission = SimHelper.hasPhoneStatePermission(context)
        simRefreshTrigger++
    }

    val availableSims = remember(context, sim1PhoneNumber, sim2PhoneNumber, simRefreshTrigger, hasPhonePermission) {
        SimHelper.getAvailableSims(context, sim1PhoneNumber, sim2PhoneNumber)
    }
    var showTestDialog by remember { mutableStateOf(false) }
    var showTestCountdown by remember { mutableStateOf(false) }
    var editingSimSlotForNumber by remember { mutableStateOf<Int?>(null) }
    var inputSimNumberText by remember { mutableStateOf("") }

    val simOptions = remember {
        listOf(
            SimModeOption(
                id = SettingsRepository.SIM_MODE_ASK_EVERY_TIME,
                title = "通話ごとに選択する（毎回確認）",
                subtitle = "発信時にSIM 1またはSIM 2を選択",
                description = "ダイヤル画面でSIMを選択するまで発信ボタンが無効になり、発信先やSIMの誤りを確実に防ぎます。",
                badge = "初期値・推奨"
            ),
            SimModeOption(
                id = SettingsRepository.SIM_MODE_SIM1,
                title = "常にSIM 1で発信",
                subtitle = "デフォルトでSIM 1を使用",
                description = "ダイヤル時に自動でSIM 1が選択された状態になります（ダイヤル画面でSIM 2への切り替えも可能です）。"
            ),
            SimModeOption(
                id = SettingsRepository.SIM_MODE_SIM2,
                title = "常にSIM 2で発信",
                subtitle = "デフォルトでSIM 2を使用",
                description = "ダイヤル時に自動でSIM 2が選択された状態になります（ダイヤル画面でSIM 1への切り替えも可能です）。"
            )
        )
    }

    val options = remember {
        listOf(
            CallConfirmOption(
                id = SettingsRepository.METHOD_SLIDE,
                title = "スライドして発信",
                subtitle = "右へスライドして通話開始",
                description = "発信バーを右端までスライドして発信します。ポケットや鞄の中での誤タップや誤操作を確実に防ぎます。",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                badge = "初期値・おすすめ"
            ),
            CallConfirmOption(
                id = SettingsRepository.METHOD_DIALOG,
                title = "ポップアップ確認ダイアログ",
                subtitle = "2段階タップ確認",
                description = "発信ボタンを押した際に、発信先電話番号と付加プレフィックスを確認するポップアップを表示します。",
                icon = Icons.Default.QuestionAnswer
            ),
            CallConfirmOption(
                id = SettingsRepository.METHOD_LONG_PRESS,
                title = "長押しして発信 (約1秒)",
                subtitle = "長押しで安全に発信",
                description = "発信ボタンを約1秒間長押しすることで発信します。軽い画面タッチによる誤発信を防ぎます。",
                icon = Icons.Default.TouchApp
            ),
            CallConfirmOption(
                id = SettingsRepository.METHOD_COUNTDOWN,
                title = "カウントダウン確認 (3秒)",
                subtitle = "発信前のキャンセル猶予タイマー",
                description = "発信ボタンを押すと3秒のカウントダウンが始まり、満了で自動発信されます。その間にキャンセルが可能です。",
                icon = Icons.Default.Timer
            ),
            CallConfirmOption(
                id = SettingsRepository.METHOD_NONE,
                title = "確認なし（直接即時発信）",
                subtitle = "ワンタップで即発信",
                description = "確認ステップを挟まず、発信ボタンを押すとすぐに電話をかけます。",
                icon = Icons.Default.FlashOn
            )
        )
    }

    // テスト用ダイアログ
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text("発信確認（テスト）") },
            text = { Text("テスト番号 (090-1234-5678) に発信しますか？\n（※実際には発信されません）") },
            confirmButton = {
                TextButton(onClick = {
                    showTestDialog = false
                    Toast.makeText(context, "✓ 確認ダイアログテスト成功！", Toast.LENGTH_SHORT).show()
                }) {
                    Text("発信")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showTestCountdown) {
        CountdownCallDialog(
            targetDisplay = "テスト番号 090-1234-5678\n（※実際には発信されません）",
            onConfirm = {
                showTestCountdown = false
                Toast.makeText(context, "✓ カウントダウン満了テスト成功！", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showTestCountdown = false
                Toast.makeText(context, "カウントダウンをキャンセルしました", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ヘッダー
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "誤発信防止・通話設定",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // デフォルト電話アプリ設定カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDefaultDialer) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDefaultDialer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDefaultDialer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "標準の電話アプリ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDefaultDialer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = if (isDefaultDialer) "設定済み" else "未設定",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDefaultDialer) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isDefaultDialer) {
                                "本アプリがシステムの標準電話アプリとして登録されています。安全発信や着信画面が正常に機能します。"
                            } else {
                                "未設定の場合、外部からの発信連携やプレフィックス自動付加、着信時の対応が制限される場合があります。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isDefaultDialer) {
                    Button(
                        onClick = {
                            val intent = DefaultDialerHelper.createRequestDefaultDialerIntent(context)
                            if (intent != null) {
                                defaultDialerLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, "設定画面を開けませんでした", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("標準の電話アプリとして設定する")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val intent = DefaultDialerHelper.createRequestDefaultDialerIntent(context)
                            if (intent != null) {
                                defaultDialerLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("デフォルトアプリ設定を確認・変更")
                    }
                }
            }
        }

        // 楽天Link (Rakuten Link) 発信連携設定 (日本環境の場合のみ表示)
        if (isJapan) {
            val hasRakutenSim = remember(availableSims, sim1PhoneNumber, sim2PhoneNumber) {
                SimHelper.hasRakutenSim(context, sim1PhoneNumber, sim2PhoneNumber)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (useRakutenLink && hasRakutenSim) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (useRakutenLink && hasRakutenSim) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = if (useRakutenLink && hasRakutenSim) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rakuten Link を使用",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (hasRakutenSim) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (hasRakutenSim) "楽天SIM検出" else "楽天SIM未検出",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (hasRakutenSim) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "楽天Linkアプリ (jp.co.rakuten.mobile.rcs) で発信",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = useRakutenLink && hasRakutenSim,
                            onCheckedChange = { enabled ->
                                if (hasRakutenSim) {
                                    onUseRakutenLinkChange(enabled)
                                } else {
                                    Toast.makeText(context, "楽天モバイルのSIMが検出された場合のみ設定可能です", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasRakutenSim
                        )
                    }

                    if (!hasRakutenSim) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "※端末に楽天モバイルのSIMカード / eSIMが装着・認識されている場合のみ設定可能です。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "・発信時に楽天Linkアプリへ直接インテントを送信し、発信履歴に記録されます。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "・Rakuten Link使用時は、プレフィックス付加および誤発信防止確認は無効になり、ダイヤラーから『楽天リンクを起動』ボタンで直接起動します。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 誤発信防止方法の選択
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "誤発信防止の方法を選択",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "電話を発信するときに、意図しない誤発信を防ぐための操作方法を選択してください。お好みに合わせていつでも変更できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { opt ->
                        val isSelected = callConfirmMethod == opt.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCallConfirmMethodChange(opt.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onCallConfirmMethodChange(opt.id) },
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = opt.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (opt.badge != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = opt.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = opt.subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = opt.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 動作テスト（お試し体験）カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "選択した方法の動作テスト（お試し体験）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "ここで設定した操作感を試すことができます（※電話は発信されません）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (callConfirmMethod) {
                        SettingsRepository.METHOD_SLIDE -> {
                            SlideToCallButton(
                                onSlideComplete = {
                                    Toast.makeText(context, "✓ スライド発信テスト成功！", Toast.LENGTH_SHORT).show()
                                },
                                enabled = true,
                                label = "テスト: 右にスライドして発信 ≫",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        SettingsRepository.METHOD_LONG_PRESS -> {
                            LongPressCallButton(
                                onLongPressComplete = {
                                    Toast.makeText(context, "✓ 長押し発信テスト成功！", Toast.LENGTH_SHORT).show()
                                },
                                enabled = true
                            )
                        }
                        SettingsRepository.METHOD_COUNTDOWN -> {
                            Button(
                                onClick = { showTestCountdown = true }
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("カウントダウン発信を試す")
                            }
                        }
                        SettingsRepository.METHOD_DIALOG -> {
                            Button(
                                onClick = { showTestDialog = true }
                            ) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("確認ダイアログを試す")
                            }
                        }
                        else -> { // METHOD_NONE
                            Button(
                                onClick = {
                                    Toast.makeText(context, "✓ 即座に発信されます（確認なし）", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("即時発信を試す（即通話開始）")
                            }
                        }
                    }
                }
            }
        }

        // Dual SIM 発信設定カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SimCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "発信SIM設定 (Dual SIM対応)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "通話発信時に使用するSIMスロット（SIM 1 / SIM 2）の挙動を設定します",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 認識されたSIMカード情報
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "検出されたSIM情報",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (hasPhonePermission) "端末からSIM情報（キャリア名・電話番号）を取得中" else "※SIM情報の自動取得には電話権限が必要です",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasPhonePermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!hasPhonePermission) {
                                    FilledTonalButton(
                                        onClick = {
                                            val perms = mutableListOf(
                                                Manifest.permission.READ_PHONE_STATE,
                                                Manifest.permission.CALL_PHONE
                                            )
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                perms.add(Manifest.permission.READ_PHONE_NUMBERS)
                                            }
                                            permissionLauncher.launch(perms.toTypedArray())
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("権限を許可", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = {
                                        hasPhonePermission = SimHelper.hasPhoneStatePermission(context)
                                        simRefreshTrigger++
                                        Toast.makeText(context, "SIM情報を再読み込みしました", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "SIM情報を再検出",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableSims.forEach { sim ->
                                val currentNumber = if (sim.isActive) {
                                    sim.phoneNumber?.takeIf { it.isNotBlank() } ?: "番号未登録（手動入力可）"
                                } else {
                                    "無効 (未装着)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (sim.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, if (sim.isActive) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (sim.isActive) {
                                                inputSimNumberText = sim.phoneNumber ?: ""
                                                editingSimSlotForNumber = sim.slotIndex
                                            } else {
                                                Toast.makeText(context, "SIM ${sim.slotIndex + 1} は無効または未装着です", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = if (sim.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "SIM ${sim.slotIndex + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sim.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                if (sim.isActive && sim.carrierName.isNotBlank() && sim.carrierName != "SIM ${sim.slotIndex + 1}") {
                                                    Text(
                                                        " (${sim.carrierName})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Text(
                                                currentNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = when {
                                                    !sim.isActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    sim.phoneNumber.isNullOrBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                maxLines = 1
                                            )
                                        }
                                        if (sim.isActive) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "電話番号を設定",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (editingSimSlotForNumber != null) {
                    val targetSlot = editingSimSlotForNumber!!
                    AlertDialog(
                        onDismissRequest = { editingSimSlotForNumber = null },
                        title = { Text("SIM ${targetSlot + 1} の電話番号設定") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "ダイヤラーや各種設定画面でSIM ${targetSlot + 1} の下に表示する電話番号を入力してください。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = inputSimNumberText,
                                    onValueChange = { inputSimNumberText = it },
                                    label = { Text("電話番号 (例: 090-1234-5678)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                onSetSimPhoneNumber(targetSlot, inputSimNumberText)
                                editingSimSlotForNumber = null
                            }) {
                                Text("保存")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingSimSlotForNumber = null }) {
                                Text("キャンセル")
                            }
                        }
                    )
                }

                // SIM発信モード選択肢
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    simOptions.forEach { opt ->
                        val isSelected = opt.id == defaultSimMode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDefaultSimModeChange(opt.id)
                                    performHaptic(context)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onDefaultSimModeChange(opt.id)
                                        performHaptic(context)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (opt.badge != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = opt.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = opt.subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = opt.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // デフォルトプレフィックス情報カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "現在のデフォルトプレフィックス",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (currentPrefix != null) "${currentPrefix.name} (${currentPrefix.prefix})" else "なし（通常発信）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentPrefix != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onNavigateToPrefixSettings) {
                        Text("編集ページを開く")
                    }
                }
            }
        }
    }
}

/**
 * グループ表示・非表示および並び順設定セクション (ドラッグ並び替え・追加・編集・削除機能付き)
 */
@Composable
private fun GroupVisibilitySettingsSection(
    groups: List<com.example.domain.model.Group>,
    hiddenGroupIds: Set<String>,
    rememberLastGroup: Boolean = true,
    isLoading: Boolean,
    deviceAccounts: List<String> = emptyList(),
    defaultAccount: String? = null,
    onToggleRememberLastGroup: (Boolean) -> Unit = {},
    onToggleVisibility: (groupId: String, isVisible: Boolean) -> Unit,
    onSetAllVisibility: (isVisible: Boolean) -> Unit,
    onResetVisibility: () -> Unit,
    onReorderGroups: (newOrderIds: List<String>) -> Unit,
    onResetOrder: () -> Unit,
    onCreateGroup: (title: String, account: String?) -> Unit,
    onUpdateGroupTitle: (groupId: String, newTitle: String) -> Unit,
    onDeleteGroup: (groupId: String, groupTitle: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<com.example.domain.model.Group?>(null) }
    var groupToDelete by remember { mutableStateOf<com.example.domain.model.Group?>(null) }

    var localGroups by remember(groups) { mutableStateOf(groups) }
    var draggingGroupId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ヘッダー説明 & 新規追加ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "グループの表示・順序設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "グループの新規追加、名前変更、削除やドラッグによる並び順のカスタマイズ、表示・非表示の設定が可能です。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "グループ一覧を再読み込み",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 最後に選択したグループの記憶設定スイッチカード
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "最後に選択したグループを記憶",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "アプリ再起動時や次回表示時に、最後に選択していたグループを自動的に表示します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = rememberLastGroup,
                    onCheckedChange = onToggleRememberLastGroup
                )
            }
        }

        // 新規グループ追加ボタン
        Button(
            onClick = { showAddGroupDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("新規グループを追加", fontWeight = FontWeight.Bold)
        }

        // 一括操作バー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onSetAllVisibility(true) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text("全表示", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = { onSetAllVisibility(false) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text("全非表示", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = {
                    onResetOrder()
                },
                modifier = Modifier.weight(1.2f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text("順序初期化", style = MaterialTheme.typography.bodySmall)
            }

            TextButton(
                onClick = onResetVisibility,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text("リセット", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (localGroups.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "グループが見つかりませんでした",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "「新規グループを追加」ボタンから新しい連絡先グループを作成できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showAddGroupDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("グループを作成")
                    }
                }
            }
        } else {
            // 表示件数サマリー
            val visibleCount = localGroups.count { it.id !in hiddenGroupIds }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "表示中: ${visibleCount} / 全${localGroups.size}件",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "ドラッグで並び替え可能",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // グループ一覧リスト
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    localGroups.forEachIndexed { index, group ->
                        val isUngrouped = group.id == com.example.data.repository.ContactsRepository.GROUP_ID_UNGROUPED
                        val isVisible = group.id !in hiddenGroupIds
                        val isDragging = draggingGroupId == group.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 1f)
                                .offset(y = if (isDragging) with(density) { dragAccumulatedY.toDp() } else 0.dp)
                                .background(
                                    if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ドラッグ並び替え用ハンドル
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .pointerInput(group.id) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                draggingGroupId = group.id
                                                dragAccumulatedY = 0f
                                            },
                                            onDragEnd = {
                                                draggingGroupId = null
                                                dragAccumulatedY = 0f
                                                onReorderGroups(localGroups.map { it.id })
                                            },
                                            onDragCancel = {
                                                draggingGroupId = null
                                                dragAccumulatedY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulatedY += dragAmount
                                                val currentList = localGroups
                                                val currentIdx = currentList.indexOfFirst { it.id == group.id }
                                                if (currentIdx != -1) {
                                                    val itemHeightPx = with(density) { 54.dp.toPx() }
                                                    if (dragAccumulatedY > itemHeightPx && currentIdx < currentList.size - 1) {
                                                        val mutable = currentList.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx + 1, item)
                                                        localGroups = mutable
                                                        dragAccumulatedY -= itemHeightPx
                                                    } else if (dragAccumulatedY < -itemHeightPx && currentIdx > 0) {
                                                        val mutable = currentList.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx - 1, item)
                                                        localGroups = mutable
                                                        dragAccumulatedY += itemHeightPx
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "ドラッグして並び替え",
                                    tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(4.dp))

                            // アイコン
                            Surface(
                                shape = CircleShape,
                                color = if (isVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { onToggleVisibility(group.id, !isVisible) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isUngrouped) Icons.Default.FolderSpecial else Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleVisibility(group.id, !isVisible) }
                            ) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isVisible) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (!isUngrouped && group.accountName.isNotBlank()) {
                                    Text(
                                        text = group.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (isUngrouped) {
                                    Text(
                                        text = "どのグループにも属さない連絡先",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 編集・削除ボタン (未グループ以外)
                            if (!isUngrouped) {
                                IconButton(
                                    onClick = { groupToEdit = group },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "グループ名を変更",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { groupToDelete = group },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "グループを削除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "システム",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(4.dp))

                            Switch(
                                checked = isVisible,
                                onCheckedChange = { checked ->
                                    onToggleVisibility(group.id, checked)
                                }
                            )
                        }

                        if (index < localGroups.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 74.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 新規グループ追加ダイアログ
    if (showAddGroupDialog) {
        var groupTitleText by remember { mutableStateOf("") }
        val accountsList = remember(deviceAccounts, defaultAccount) {
            val list = mutableListOf<String>()
            defaultAccount?.let { if (it.isNotBlank()) list.add(it) }
            deviceAccounts.forEach { acc ->
                if (acc !in list) list.add(acc)
            }
            list
        }
        var selectedAccount by remember(accountsList) {
            mutableStateOf(accountsList.firstOrNull())
        }

        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("新規グループを追加") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = groupTitleText,
                        onValueChange = { groupTitleText = it },
                        label = { Text("グループ名 *") },
                        placeholder = { Text("例: 仕事, 家族, 同僚, VIP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (accountsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "保存先Googleアカウント",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            accountsList.forEach { acc ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedAccount = acc }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedAccount == acc,
                                        onClick = { selectedAccount = acc }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = acc,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupTitleText.trim().isNotBlank()) {
                            onCreateGroup(groupTitleText.trim(), selectedAccount)
                            showAddGroupDialog = false
                        }
                    },
                    enabled = groupTitleText.trim().isNotBlank()
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // グループ名変更ダイアログ
    if (groupToEdit != null) {
        val targetGroup = groupToEdit!!
        var editTitleText by remember(targetGroup) { mutableStateOf(targetGroup.title) }

        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text("グループ名の変更") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "新しいグループ名を入力してください。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        label = { Text("グループ名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitleText.trim().isNotBlank()) {
                            onUpdateGroupTitle(targetGroup.id, editTitleText.trim())
                            groupToEdit = null
                        }
                    },
                    enabled = editTitleText.trim().isNotBlank() && editTitleText.trim() != targetGroup.title
                ) {
                    Text("変更を保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // グループ削除確認ダイアログ
    if (groupToDelete != null) {
        val targetGroup = groupToDelete!!
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("グループを削除") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("グループ「${targetGroup.title}」を削除しますか？")
                    Text(
                        text = "※グループを削除しても、所属する連絡先データ自体は削除されず「未グループ」に移動します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(targetGroup.id, targetGroup.title)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

