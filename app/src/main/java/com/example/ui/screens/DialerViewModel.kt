package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CallHistoryDao
import com.example.data.local.CallHistoryEntity
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ExcludedPrefix
import com.example.domain.model.PhonePrefix
import com.example.domain.model.SimInfo
import com.example.util.PhoneNumberFormatter
import com.example.util.SimHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DialerViewModel(
    private val settingsRepository: SettingsRepository,
    private val callHistoryDao: CallHistoryDao
) : ViewModel() {

    val prefixes: StateFlow<List<PhonePrefix>> = settingsRepository.prefixesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPrefixId: StateFlow<String?> = settingsRepository.selectedPrefixIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPrefixIdSim1: StateFlow<String?> = settingsRepository.selectedPrefixIdSim1Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPrefixIdSim2: StateFlow<String?> = settingsRepository.selectedPrefixIdSim2Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sim1PhoneNumber: StateFlow<String?> = settingsRepository.sim1PhoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sim2PhoneNumber: StateFlow<String?> = settingsRepository.sim2PhoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val prefix: StateFlow<String> = settingsRepository.prefixFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val excludedPrefixes: StateFlow<List<ExcludedPrefix>> = settingsRepository.excludedPrefixesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmCall: StateFlow<Boolean> = settingsRepository.confirmCallFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val callConfirmMethod: StateFlow<String> = settingsRepository.callConfirmMethodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.METHOD_SLIDE)

    val defaultSimMode: StateFlow<String> = settingsRepository.defaultSimModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.SIM_MODE_ASK_EVERY_TIME)

    val useRakutenLink: StateFlow<Boolean> = settingsRepository.useRakutenLinkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedSimSlot = MutableStateFlow<Int?>(null)
    val selectedSimSlot: StateFlow<Int?> = _selectedSimSlot.asStateFlow()

    // 画面上で一時選択されたプレフィックスID（nullの場合は対象SIMのデフォルトを使用）
    private val _activePrefixOverride = MutableStateFlow<String?>(null)
    val activePrefixOverride: StateFlow<String?> = _activePrefixOverride.asStateFlow()

    init {
        viewModelScope.launch {
            defaultSimMode.collect { mode ->
                when (mode) {
                    SettingsRepository.SIM_MODE_SIM1 -> _selectedSimSlot.value = 0
                    SettingsRepository.SIM_MODE_SIM2 -> _selectedSimSlot.value = 1
                    SettingsRepository.SIM_MODE_ASK_EVERY_TIME -> {
                        // 通話ごとの切り替え設定の場合は未選択状態にして選択を必須化
                        _selectedSimSlot.value = null
                    }
                }
            }
        }
    }

    fun selectPrefix(id: String?) {
        _activePrefixOverride.value = id
    }

    fun selectSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
        // SIMスロット切り替え時は、そのSIMのデフォルトプレフィックスを適用（オーバーライドをリセット）
        _activePrefixOverride.value = null
    }

    fun getEffectivePrefix(simSlot: Int? = _selectedSimSlot.value): PhonePrefix? {
        val overrideId = _activePrefixOverride.value
        val list = prefixes.value

        if (overrideId != null) {
            if (overrideId == "none") return null
            val custom = list.firstOrNull { it.id == overrideId }
            if (custom != null) {
                // SIM互換性の確認
                val matchesSim = when (custom.targetSim) {
                    PhonePrefix.TARGET_SIM_1 -> simSlot == 0
                    PhonePrefix.TARGET_SIM_2 -> simSlot == 1
                    else -> true
                }
                if (matchesSim) return custom
            }
        }

        // SIMスロットごとのデフォルトプレフィックスを取得
        val defaultIdForSim = when (simSlot) {
            0 -> selectedPrefixIdSim1.value ?: selectedPrefixId.value
            1 -> selectedPrefixIdSim2.value ?: selectedPrefixId.value
            else -> selectedPrefixId.value
        }

        if (defaultIdForSim == null || defaultIdForSim == "none") return null
        val defaultPrefix = list.firstOrNull { it.id == defaultIdForSim } ?: return null

        val matchesSim = when (defaultPrefix.targetSim) {
            PhonePrefix.TARGET_SIM_1 -> simSlot == 0
            PhonePrefix.TARGET_SIM_2 -> simSlot == 1
            else -> true
        }

        return if (matchesSim) defaultPrefix else null
    }

    fun getMatchingExcludedPrefix(rawNumber: String): ExcludedPrefix? {
        val normalized = rawNumber.replace(Regex("[^0-9+]"), "")
        if (normalized.isEmpty()) return null
        return excludedPrefixes.value.firstOrNull { item ->
            val pat = item.pattern.trim().replace(Regex("[^0-9+]"), "")
            pat.isNotEmpty() && normalized.startsWith(pat)
        }
    }

    fun computeNumberToCall(rawNumber: String, simSlot: Int? = _selectedSimSlot.value): String {
        val effectivePrefixObj = getEffectivePrefix(simSlot)
        val currentPrefix = effectivePrefixObj?.prefix ?: ""

        val isExcluded = getMatchingExcludedPrefix(rawNumber) != null

        return if (currentPrefix.isNotEmpty() && !isExcluded && !rawNumber.startsWith(currentPrefix)) {
            currentPrefix + rawNumber
        } else {
            rawNumber
        }
    }

    fun makeCall(context: Context, rawNumber: String, simSlot: Int? = _selectedSimSlot.value) {
        val nationalNumber = PhoneNumberFormatter.toNationalDigits(context, rawNumber)
        val numberToCall = computeNumberToCall(nationalNumber, simSlot)

        // Save history WITHOUT prefix
        viewModelScope.launch {
            callHistoryDao.insertCall(
                CallHistoryEntity(
                    phoneNumber = nationalNumber,
                    contactName = null,
                    isOutgoing = true
                )
            )
        }

        val availableSims = SimHelper.getAvailableSims(
            context = context,
            customSim1Number = sim1PhoneNumber.value,
            customSim2Number = sim2PhoneNumber.value
        )
        val selectedSim = availableSims.firstOrNull { it.slotIndex == simSlot }
        val subscriptionId = selectedSim?.subscriptionId

        val intent = SimHelper.createCallIntent(
            context = context,
            numberToCall = numberToCall,
            simSlotIndex = simSlot,
            subscriptionId = subscriptionId
        )

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DialerViewModel", "Direct call failed, trying DIAL action", e)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$numberToCall")
                    if (simSlot != null) {
                        putExtra("com.android.phone.extra.slot", simSlot)
                        putExtra("simSlot", simSlot)
                    }
                }
                context.startActivity(dialIntent)
            } catch (e2: Exception) {
                Log.e("DialerViewModel", "Dial action also failed", e2)
            }
        }
    }

    fun setSimPhoneNumber(simSlot: Int, number: String?) {
        viewModelScope.launch {
            settingsRepository.setSimPhoneNumber(simSlot, number)
        }
    }

    fun launchRakutenLink(context: Context, rawNumber: String) {
        val nationalNumber = PhoneNumberFormatter.toNationalDigits(context, rawNumber)
        // 発信履歴に記録
        viewModelScope.launch {
            callHistoryDao.insertCall(
                CallHistoryEntity(
                     phoneNumber = nationalNumber,
                     contactName = null,
                     isOutgoing = true
                )
            )
        }
        SimHelper.launchRakutenLink(context, nationalNumber)
    }
}

class DialerViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val callHistoryDao: CallHistoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DialerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DialerViewModel(settingsRepository, callHistoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
