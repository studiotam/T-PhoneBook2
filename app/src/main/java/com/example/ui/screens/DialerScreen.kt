package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.PhonebookApplication
import com.example.data.repository.SettingsRepository
import com.example.domain.model.PhonePrefix
import com.example.ui.components.CountdownCallDialog
import com.example.ui.components.LongPressCallButton
import com.example.ui.components.SlideToCallButton
import com.example.ui.components.performHaptic
import com.example.util.PhoneNumberFormatter
import com.example.util.SimHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

data class DialKey(val digit: String, val subText: String)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    initialPhoneNumber: String? = null,
    viewModel: DialerViewModel = viewModel(
        factory = DialerViewModelFactory(
            (LocalContext.current.applicationContext as PhonebookApplication).settingsRepository,
            (LocalContext.current.applicationContext as PhonebookApplication).database.callHistoryDao()
        )
    )
) {
    val context = LocalContext.current
    val initialFormatted = remember(initialPhoneNumber, context) {
        PhoneNumberFormatter.toNationalDigits(context, initialPhoneNumber)
    }
    var phoneNumber by remember { mutableStateOf(initialFormatted) }
    val confirmCall by viewModel.confirmCall.collectAsStateWithLifecycle()
    val callConfirmMethod by viewModel.callConfirmMethod.collectAsStateWithLifecycle()
    val prefixes by viewModel.prefixes.collectAsStateWithLifecycle()
    val selectedPrefixId by viewModel.selectedPrefixId.collectAsStateWithLifecycle()
    val selectedPrefixIdSim1 by viewModel.selectedPrefixIdSim1.collectAsStateWithLifecycle()
    val selectedPrefixIdSim2 by viewModel.selectedPrefixIdSim2.collectAsStateWithLifecycle()
    val activePrefixOverride by viewModel.activePrefixOverride.collectAsStateWithLifecycle()
    val excludedPrefixes by viewModel.excludedPrefixes.collectAsStateWithLifecycle()

    val defaultSimMode by viewModel.defaultSimMode.collectAsStateWithLifecycle()
    val useRakutenLink by viewModel.useRakutenLink.collectAsStateWithLifecycle()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsStateWithLifecycle()
    val sim1PhoneNumber by viewModel.sim1PhoneNumber.collectAsStateWithLifecycle()
    val sim2PhoneNumber by viewModel.sim2PhoneNumber.collectAsStateWithLifecycle()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var prefixDropdownExpanded by remember { mutableStateOf(false) }
    var editingSimSlotForNumber by remember { mutableStateOf<Int?>(null) }
    var inputSimNumberText by remember { mutableStateOf("") }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(initialPhoneNumber) {
        if (!initialPhoneNumber.isNullOrBlank()) {
            phoneNumber = PhoneNumberFormatter.toNationalDigits(context, initialPhoneNumber)
        }
    }

    val permissionsGranted = permissionsState.allPermissionsGranted
    val availableSims = remember(context, sim1PhoneNumber, sim2PhoneNumber, permissionsGranted) {
        SimHelper.getAvailableSims(context, sim1PhoneNumber, sim2PhoneNumber)
    }
    val activeSims = remember(availableSims) { availableSims.filter { it.isActive } }
    val selectedSimInfo = availableSims.firstOrNull { it.slotIndex == selectedSimSlot }
    val isSimSelected = selectedSimSlot != null && (selectedSimInfo?.isActive == true)
    val canMakeCall = phoneNumber.isNotEmpty() && isSimSelected

    // Auto-select valid SIM if current is inactive or if only 1 SIM is active
    LaunchedEffect(availableSims, selectedSimSlot) {
        if (selectedSimSlot != null) {
            val current = availableSims.firstOrNull { it.slotIndex == selectedSimSlot }
            if (current == null || !current.isActive) {
                val firstActive = availableSims.firstOrNull { it.isActive }
                if (firstActive != null) {
                    viewModel.selectSimSlot(firstActive.slotIndex)
                }
            }
        } else if (activeSims.size == 1) {
            viewModel.selectSimSlot(activeSims.first().slotIndex)
        }
    }

    // Effective prefix calculation based on selected SIM slot
    val effectivePrefixObj = remember(prefixes, selectedPrefixId, selectedPrefixIdSim1, selectedPrefixIdSim2, activePrefixOverride, selectedSimSlot) {
        viewModel.getEffectivePrefix(selectedSimSlot)
    }
    val activePrefixNumber = effectivePrefixObj?.prefix ?: ""

    // Excluded prefix and SIM match checks
    val matchingExcluded = remember(phoneNumber, excludedPrefixes) {
        viewModel.getMatchingExcludedPrefix(phoneNumber)
    }
    val isExcluded = matchingExcluded != null

    val isPrefixSimCompatible = remember(effectivePrefixObj, selectedSimSlot) {
        if (effectivePrefixObj == null) true
        else when (effectivePrefixObj.targetSim) {
            PhonePrefix.TARGET_SIM_1 -> selectedSimSlot == 0
            PhonePrefix.TARGET_SIM_2 -> selectedSimSlot == 1
            else -> true
        }
    }

    val actualNumberToCall = remember(phoneNumber, effectivePrefixObj, selectedSimSlot, excludedPrefixes) {
        viewModel.computeNumberToCall(phoneNumber, selectedSimSlot)
    }

    val simNotice = if (selectedSimInfo != null) {
        val simNum = selectedSimInfo.phoneNumber?.takeIf { it.isNotBlank() }
        if (simNum != null) {
            "\n(発信SIM: SIM ${selectedSimInfo.slotIndex + 1} - $simNum)"
        } else {
            "\n(発信SIM: SIM ${selectedSimInfo.slotIndex + 1})"
        }
    } else ""
    val displayCallTarget = when {
        isExcluded -> "$phoneNumber\n[プレフィックス除外: ${matchingExcluded?.description ?: matchingExcluded?.pattern}]$simNotice"
        actualNumberToCall != phoneNumber && effectivePrefixObj != null -> "${effectivePrefixObj.name} ($activePrefixNumber) 付加\n$actualNumberToCall$simNotice"
        else -> "$phoneNumber$simNotice"
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("発信確認") },
            text = { Text("$displayCallTarget に発信しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    if (isSimSelected) {
                        val target = phoneNumber
                        phoneNumber = ""
                        viewModel.makeCall(context, target, selectedSimSlot)
                    } else {
                        Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("発信")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showCountdownDialog) {
        CountdownCallDialog(
            targetDisplay = displayCallTarget,
            onConfirm = {
                showCountdownDialog = false
                if (isSimSelected) {
                    val target = phoneNumber
                    phoneNumber = ""
                    viewModel.makeCall(context, target, selectedSimSlot)
                } else {
                    Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                showCountdownDialog = false
            }
        )
    }

    if (editingSimSlotForNumber != null) {
        val targetSlot = editingSimSlotForNumber!!
        AlertDialog(
            onDismissRequest = { editingSimSlotForNumber = null },
            title = { Text("SIM ${targetSlot + 1} の電話番号設定") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SIM ${targetSlot + 1} の発信元電話番号を登録すると、ボタン下に番号が表示されます。",
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
                    viewModel.setSimPhoneNumber(targetSlot, inputSimNumberText)
                    editingSimSlotForNumber = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingSimSlotForNumber = null
                }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Dialpad definition (standard 4 rows)
    val dialpadRows = remember {
        listOf(
            listOf(DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF")),
            listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
            listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
            listOf(DialKey("*", ""), DialKey("0", "+"), DialKey("#", ""))
        )
    }

    // Adaptive Full-Screen Layout without scrolling
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val availableHeight = maxHeight
        val isVeryCompact = availableHeight < 560.dp
        val isCompact = availableHeight in 560.dp..660.dp

        // Dynamic Sizing based on available screen height
        val buttonSize: Dp = when {
            isVeryCompact -> 46.dp
            isCompact -> 54.dp
            else -> 62.dp
        }
        val digitFontSize: TextUnit = when {
            isVeryCompact -> 20.sp
            isCompact -> 24.sp
            else -> 28.sp
        }
        val keyRowSpacing: Dp = when {
            isVeryCompact -> 3.dp
            isCompact -> 5.dp
            else -> 8.dp
        }
        val contentPaddingV: Dp = when {
            isVeryCompact -> 4.dp
            isCompact -> 8.dp
            else -> 12.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp, vertical = contentPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==================== 1. TOP HEADER (SIM & Prefix Selection) ====================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dual SIM Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSims.forEach { sim ->
                        val isSelected = selectedSimSlot == sim.slotIndex && sim.isActive
                        val displayPhoneNumber = if (sim.isActive) {
                            sim.phoneNumber?.takeIf { it.isNotBlank() } ?: "番号未設定"
                        } else {
                            "未装着"
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                !sim.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    !sim.isActive -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (sim.isActive) {
                                        viewModel.selectSimSlot(sim.slotIndex)
                                        performHaptic(context)
                                    } else {
                                        Toast.makeText(context, "SIM ${sim.slotIndex + 1} は無効または未装着です", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = if (isVeryCompact) 4.dp else 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = when {
                                        !sim.isActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "SIM ${sim.slotIndex + 1}${if (!sim.isActive) " (無効)" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = when {
                                            !sim.isActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = displayPhoneNumber,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = when {
                                            !sim.isActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                            isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Prefix Selector Pulldown Bar / Rakuten Link Indicator
                if (useRakutenLink) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isVeryCompact) 4.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rakuten Link 発信モード（プレフィックス無効）",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "楽天Link",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activePrefixNumber.isNotEmpty()) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (activePrefixNumber.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { prefixDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isVeryCompact) 4.dp else 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = null,
                                    tint = if (activePrefixNumber.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (effectivePrefixObj != null) {
                                        "${effectivePrefixObj.name} (${effectivePrefixObj.prefix})"
                                    } else {
                                        "通常発信 (プレフィックスなし)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (effectivePrefixObj != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (effectivePrefixObj != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (effectivePrefixObj != null) {
                                    Surface(
                                        color = when (effectivePrefixObj.targetSim) {
                                            PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.tertiaryContainer
                                            PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = PhonePrefix.getSimLabel(effectivePrefixObj.targetSim),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = when (effectivePrefixObj.targetSim) {
                                                PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.onTertiaryContainer
                                                PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "プレフィックスを選択",
                                    tint = if (activePrefixNumber.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = prefixDropdownExpanded,
                            onDismissRequest = { prefixDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            val isNoneSelected = effectivePrefixObj == null
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "通常発信（プレフィックスなし）",
                                        fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.selectPrefix("none")
                                    prefixDropdownExpanded = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = isNoneSelected,
                                        onClick = null
                                    )
                                }
                            )
                            if (prefixes.isNotEmpty()) {
                                HorizontalDivider()
                                prefixes.forEach { item ->
                                    val isSelected = effectivePrefixObj?.id == item.id
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.name,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        color = when (item.targetSim) {
                                                            PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.tertiaryContainer
                                                            PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.secondaryContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        },
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = PhonePrefix.getSimLabel(item.targetSim),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = when (item.targetSim) {
                                                                PhonePrefix.TARGET_SIM_1 -> MaterialTheme.colorScheme.onTertiaryContainer
                                                                PhonePrefix.TARGET_SIM_2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "付加番号: ${item.prefix}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectPrefix(item.id)
                                            prefixDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Warning / Info Badges (if applicable)
                if (!isSimSelected && phoneNumber.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "発信に使用するSIMを選択してください",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (activePrefixNumber.isNotEmpty() && effectivePrefixObj != null) {
                    if (isExcluded) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "除外番号 (${matchingExcluded?.pattern}): プレフィックスなし発信",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (!isPrefixSimCompatible) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${effectivePrefixObj.name} は ${PhonePrefix.getSimLabel(effectivePrefixObj.targetSim)} 対象のため付加されません",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ==================== 2. PHONE NUMBER DISPLAY & BACKSPACE ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = if (isVeryCompact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Spacer on left to keep number centered
                Spacer(modifier = Modifier.size(40.dp))

                // Number text
                Text(
                    text = if (phoneNumber.isEmpty()) "番号を入力" else phoneNumber,
                    fontSize = when {
                        phoneNumber.isEmpty() -> if (isVeryCompact) 18.sp else 22.sp
                        phoneNumber.length > 15 -> 18.sp
                        phoneNumber.length > 11 -> 22.sp
                        isVeryCompact -> 24.sp
                        isCompact -> 28.sp
                        else -> 32.sp
                    },
                    fontWeight = if (phoneNumber.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (phoneNumber.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    modifier = Modifier.weight(1f)
                )

                // Backspace action button on right (tap: delete 1, long click: clear all)
                if (phoneNumber.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    performHaptic(context)
                                    phoneNumber = phoneNumber.dropLast(1)
                                },
                                onLongClick = {
                                    performHaptic(context)
                                    phoneNumber = ""
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "1文字削除 (長押しで全消去)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // ==================== 3. DIALPAD KEYPAD (4 Rows) ====================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(keyRowSpacing)
            ) {
                dialpadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            DialButton(
                                digit = key.digit,
                                subText = key.subText,
                                size = buttonSize,
                                digitFontSize = digitFontSize,
                                onClick = {
                                    phoneNumber += key.digit
                                },
                                onLongClick = if (key.digit == "0") {
                                    { phoneNumber += "+" }
                                } else null
                            )
                        }
                    }
                }
            }

            // ==================== 4. CALL BUTTON ACTION ====================
            val slideLabel = when {
                phoneNumber.isEmpty() -> "電話番号を入力"
                !isSimSelected -> "SIMを選択してください"
                else -> "SIM ${selectedSimSlot!! + 1} でスライド発信 ≫"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isVeryCompact) 2.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (useRakutenLink) {
                    // 楽天Link使用時: プレフィックス無効、誤発信確認なし、ワンタップで「楽天リンクを起動」
                    val canLaunchRakuten = phoneNumber.isNotEmpty()
                    Button(
                        onClick = {
                            if (canLaunchRakuten) {
                                val target = phoneNumber
                                phoneNumber = ""
                                viewModel.launchRakutenLink(context, target)
                            }
                        },
                        enabled = canLaunchRakuten,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = if (isVeryCompact) 10.dp else 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(if (isVeryCompact) 48.dp else 56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                modifier = Modifier.size(if (isVeryCompact) 20.dp else 24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "楽天リンクを起動",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isVeryCompact) 16.sp else 18.sp
                            )
                        }
                    }
                } else {
                    when (callConfirmMethod) {
                        SettingsRepository.METHOD_SLIDE -> {
                            SlideToCallButton(
                                onSlideComplete = {
                                    if (phoneNumber.isNotEmpty()) {
                                        if (isSimSelected) {
                                            val target = phoneNumber
                                            phoneNumber = ""
                                            viewModel.makeCall(context, target, selectedSimSlot)
                                        } else {
                                            Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = canMakeCall,
                                label = slideLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                        SettingsRepository.METHOD_LONG_PRESS -> {
                            LongPressCallButton(
                                onLongPressComplete = {
                                    if (phoneNumber.isNotEmpty()) {
                                        if (isSimSelected) {
                                            val target = phoneNumber
                                            phoneNumber = ""
                                            viewModel.makeCall(context, target, selectedSimSlot)
                                        } else {
                                            Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = canMakeCall
                            )
                        }
                        SettingsRepository.METHOD_COUNTDOWN -> {
                            FloatingActionButton(
                                onClick = {
                                    if (phoneNumber.isNotEmpty()) {
                                        if (isSimSelected) {
                                            showCountdownDialog = true
                                        } else {
                                            Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                containerColor = if (canMakeCall) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(if (isVeryCompact) 52.dp else 60.dp)
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "発信",
                                    modifier = Modifier.size(if (isVeryCompact) 26.dp else 30.dp)
                                )
                            }
                        }
                        SettingsRepository.METHOD_DIALOG -> {
                            FloatingActionButton(
                                onClick = {
                                    if (phoneNumber.isNotEmpty()) {
                                        if (isSimSelected) {
                                            showConfirmDialog = true
                                        } else {
                                            Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                containerColor = if (canMakeCall) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(if (isVeryCompact) 52.dp else 60.dp)
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "発信",
                                    modifier = Modifier.size(if (isVeryCompact) 26.dp else 30.dp)
                                )
                            }
                        }
                        else -> { // METHOD_NONE
                            FloatingActionButton(
                                onClick = {
                                    if (phoneNumber.isNotEmpty()) {
                                        if (isSimSelected) {
                                            val target = phoneNumber
                                            phoneNumber = ""
                                            viewModel.makeCall(context, target, selectedSimSlot)
                                        } else {
                                            Toast.makeText(context, "発信に使用するSIMを選択してください", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                containerColor = if (canMakeCall) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(if (isVeryCompact) 52.dp else 60.dp)
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "発信",
                                    modifier = Modifier.size(if (isVeryCompact) 26.dp else 30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialButton(
    digit: String,
    subText: String = "",
    size: Dp,
    digitFontSize: TextUnit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                onClick = {
                    performHaptic(context)
                    onClick()
                },
                onLongClick = onLongClick?.let { action ->
                    {
                        performHaptic(context)
                        action()
                    }
                }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = digit,
                fontSize = digitFontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = digitFontSize
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    fontSize = (digitFontSize.value * 0.36f).coerceAtLeast(8f).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    letterSpacing = 1.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}
