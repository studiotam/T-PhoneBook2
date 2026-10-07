package com.example.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.SpamNumberEntity
import com.example.data.repository.CallLogRepository
import com.example.data.repository.CallRecordingRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.DriveBackupResult
import com.example.data.repository.DriveCheckResult
import com.example.data.repository.DriveFileInfo
import com.example.data.repository.DriveRestoreResult
import com.example.data.repository.GoogleDriveBackupRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpamSyncRepository
import com.example.data.repository.SyncState
import com.example.domain.model.ExcludedPrefix
import com.example.domain.model.Group
import com.example.domain.model.PhonePrefix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DriveBackupUiState {
    object Idle : DriveBackupUiState()
    object Checking : DriveBackupUiState()
    object BackingUp : DriveBackupUiState()
    object Restoring : DriveBackupUiState()
    data class Success(val message: String, val fileInfo: DriveFileInfo?) : DriveBackupUiState()
    data class Error(val message: String) : DriveBackupUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val authManager: GoogleAuthManager,
    private val contactsRepository: ContactsRepository? = null,
    val spamSyncRepository: SpamSyncRepository? = null,
    val googleDriveBackupRepository: GoogleDriveBackupRepository? = null,
    val callRecordingRepository: CallRecordingRepository? = null
) : ViewModel() {

    val prefixes: StateFlow<List<PhonePrefix>> = repository.prefixesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPrefixId: StateFlow<String?> = repository.selectedPrefixIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPrefixIdSim1: StateFlow<String?> = repository.selectedPrefixIdSim1Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPrefixIdSim2: StateFlow<String?> = repository.selectedPrefixIdSim2Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sim1PhoneNumber: StateFlow<String?> = repository.sim1PhoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sim2PhoneNumber: StateFlow<String?> = repository.sim2PhoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val prefix: StateFlow<String> = repository.prefixFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val excludedPrefixes: StateFlow<List<ExcludedPrefix>> = repository.excludedPrefixesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmCall: StateFlow<Boolean> = repository.confirmCallFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val callConfirmMethod: StateFlow<String> = repository.callConfirmMethodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.METHOD_SLIDE)

    val defaultSimMode: StateFlow<String> = repository.defaultSimModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.SIM_MODE_ASK_EVERY_TIME)

    val useRakutenLink: StateFlow<Boolean> = repository.useRakutenLinkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String?> = repository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String?> = repository.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPhoto: StateFlow<String?> = repository.userPhotoFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hiddenGroupIds: StateFlow<Set<String>> = repository.hiddenGroupIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val rememberLastGroup: StateFlow<Boolean> = repository.rememberLastGroupFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val contactFieldDisplayConfig: StateFlow<com.example.domain.model.ContactFieldDisplayConfig> = repository.contactFieldDisplayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.domain.model.ContactFieldDisplayConfig())

    val incomingCallDisplayConfig: StateFlow<com.example.domain.model.IncomingCallDisplayConfig> = repository.incomingCallDisplayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.domain.model.IncomingCallDisplayConfig())

    val activeCallDisplayConfig: StateFlow<com.example.domain.model.ActiveCallDisplayConfig> = repository.activeCallDisplayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.domain.model.ActiveCallDisplayConfig())

    val outgoingCallDisplayConfig: StateFlow<com.example.domain.model.OutgoingCallDisplayConfig> = repository.outgoingCallDisplayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.domain.model.OutgoingCallDisplayConfig())

    val autoRecordCalls: StateFlow<Boolean> = repository.autoRecordCallsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val callRecordings: StateFlow<List<com.example.data.local.CallRecordingEntity>> = (callRecordingRepository?.allRecordingsFlow
        ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAutoRecordCalls(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRecordCalls(enabled)
        }
    }

    fun deleteCallRecording(recording: com.example.data.local.CallRecordingEntity) {
        viewModelScope.launch {
            callRecordingRepository?.deleteRecording(recording)
        }
    }

    fun deleteAllCallRecordings() {
        viewModelScope.launch {
            callRecordingRepository?.deleteAllRecordings()
        }
    }

    fun setContactFieldDisplayConfig(config: com.example.domain.model.ContactFieldDisplayConfig) {
        viewModelScope.launch {
            repository.setContactFieldDisplayConfig(config)
        }
    }

    fun setIncomingCallDisplayConfig(config: com.example.domain.model.IncomingCallDisplayConfig) {
        viewModelScope.launch {
            repository.setIncomingCallDisplayConfig(config)
        }
    }

    fun resetIncomingCallDisplayConfig() {
        viewModelScope.launch {
            repository.resetIncomingCallDisplayConfig()
        }
    }

    fun setActiveCallDisplayConfig(config: com.example.domain.model.ActiveCallDisplayConfig) {
        viewModelScope.launch {
            repository.setActiveCallDisplayConfig(config)
        }
    }

    fun resetActiveCallDisplayConfig() {
        viewModelScope.launch {
            repository.resetActiveCallDisplayConfig()
        }
    }

    fun setOutgoingCallDisplayConfig(config: com.example.domain.model.OutgoingCallDisplayConfig) {
        viewModelScope.launch {
            repository.setOutgoingCallDisplayConfig(config)
        }
    }

    fun resetOutgoingCallDisplayConfig() {
        viewModelScope.launch {
            repository.resetOutgoingCallDisplayConfig()
        }
    }

    fun startIncomingCallPreview(
        context: android.content.Context,
        phoneNumber: String = "090-1234-5678",
        contactName: String? = "山田 太郎",
        company: String? = "株式会社サンプル 営業部",
        photoUri: String? = null
    ) {
        com.example.service.CallManager.startPreviewCall(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri
        )
        val intent = Intent(context, com.example.ui.call.InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(intent)
    }

    fun startActiveCallPreview(
        context: android.content.Context,
        phoneNumber: String = "090-1234-5678",
        contactName: String? = "山田 太郎",
        company: String? = "株式会社サンプル 営業部",
        photoUri: String? = null
    ) {
        com.example.service.CallManager.startPreviewActiveCall(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri
        )
        val intent = Intent(context, com.example.ui.call.InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(intent)
    }

    fun startOutgoingCallPreview(
        context: android.content.Context,
        phoneNumber: String = "090-9876-5432",
        contactName: String? = "佐藤 花子",
        company: String? = "デザインパートナーズ 代表",
        photoUri: String? = null,
        prefixName: String? = "楽天でんわ (003768)",
        simSlot: Int? = 1
    ) {
        com.example.service.CallManager.startPreviewOutgoingCall(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri,
            prefixName = prefixName,
            simSlot = simSlot
        )
        val intent = Intent(context, com.example.ui.call.InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(intent)
    }

    private val _rawDeviceGroups = MutableStateFlow<List<Group>>(emptyList())

    val groupOrderIds: StateFlow<List<String>> = repository.groupOrderIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceGroups: StateFlow<List<Group>> = kotlinx.coroutines.flow.combine(
        _rawDeviceGroups,
        repository.groupOrderIdsFlow
    ) { groups, orderIds ->
        if (orderIds.isEmpty()) {
            groups
        } else {
            val indexMap = orderIds.mapIndexed { index, id -> id to index }.toMap()
            groups.sortedWith(
                compareBy<Group> { group -> indexMap[group.id] ?: (Int.MAX_VALUE - 1000) }
                    .thenBy { it.title }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingGroups = MutableStateFlow(false)
    val isLoadingGroups: StateFlow<Boolean> = _isLoadingGroups.asStateFlow()

    private val _deviceAccounts = MutableStateFlow<List<String>>(emptyList())
    val deviceAccounts: StateFlow<List<String>> = _deviceAccounts.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    // --- Google Drive Backup / Restore 関連 ---
    private val _driveBackupState = MutableStateFlow<DriveBackupUiState>(DriveBackupUiState.Idle)
    val driveBackupState: StateFlow<DriveBackupUiState> = _driveBackupState.asStateFlow()

    private val _driveFileInfo = MutableStateFlow<DriveFileInfo?>(null)
    val driveFileInfo: StateFlow<DriveFileInfo?> = _driveFileInfo.asStateFlow()

    private val _userConsentIntent = MutableStateFlow<Intent?>(null)
    val userConsentIntent: StateFlow<Intent?> = _userConsentIntent.asStateFlow()

    init {
        loadDeviceAccounts()
        loadDeviceGroups()
    }

    fun loadDeviceAccounts() {
        viewModelScope.launch {
            _deviceAccounts.value = authManager.getDeviceGoogleAccounts()
        }
    }

    fun loadDeviceGroups() {
        if (contactsRepository == null) return
        viewModelScope.launch {
            _isLoadingGroups.value = true
            try {
                _rawDeviceGroups.value = contactsRepository.getGroups(includeUngrouped = true)
            } catch (e: Exception) {
                _rawDeviceGroups.value = emptyList()
            } finally {
                _isLoadingGroups.value = false
            }
        }
    }

    fun setGroupVisibility(groupId: String, isVisible: Boolean) {
        viewModelScope.launch {
            repository.setGroupVisibility(groupId, isVisible)
        }
    }

    fun setAllGroupsVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            val allIds = _rawDeviceGroups.value.map { it.id }
            repository.setAllGroupsVisibility(allIds, isVisible)
        }
    }

    fun resetGroupVisibility() {
        viewModelScope.launch {
            repository.resetGroupVisibility()
        }
    }

    fun moveGroupUp(groupId: String) {
        val currentList = deviceGroups.value
        val index = currentList.indexOfFirst { it.id == groupId }
        if (index > 0) {
            val orderList = currentList.map { it.id }.toMutableList()
            val temp = orderList[index]
            orderList[index] = orderList[index - 1]
            orderList[index - 1] = temp
            viewModelScope.launch {
                repository.setGroupOrderIds(orderList)
            }
        }
    }

    fun moveGroupDown(groupId: String) {
        val currentList = deviceGroups.value
        val index = currentList.indexOfFirst { it.id == groupId }
        if (index in 0 until currentList.size - 1) {
            val orderList = currentList.map { it.id }.toMutableList()
            val temp = orderList[index]
            orderList[index] = orderList[index + 1]
            orderList[index + 1] = temp
            viewModelScope.launch {
                repository.setGroupOrderIds(orderList)
            }
        }
    }

    fun setGroupOrder(orderIds: List<String>) {
        viewModelScope.launch {
            repository.setGroupOrderIds(orderIds)
        }
    }

    fun setRememberLastGroup(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRememberLastGroup(enabled)
        }
    }

    fun resetGroupOrder() {
        viewModelScope.launch {
            repository.setGroupOrderIds(emptyList())
        }
    }

    fun createGroup(
        title: String,
        accountName: String? = null,
        accountType: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (contactsRepository == null) {
            onComplete(false, "連絡先リポジトリが利用できません")
            return
        }
        viewModelScope.launch {
            try {
                val result = contactsRepository.createGroup(
                    title = title,
                    accountName = accountName ?: userEmail.value,
                    accountType = accountType ?: "com.google"
                )
                if (result.isSuccess) {
                    loadDeviceGroups()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループの追加に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "エラーが発生しました")
            }
        }
    }

    fun updateGroupTitle(
        groupId: String,
        newTitle: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (contactsRepository == null) {
            onComplete(false, "連絡先リポジトリが利用できません")
            return
        }
        viewModelScope.launch {
            try {
                val result = contactsRepository.updateGroupTitle(groupId, newTitle)
                if (result.isSuccess) {
                    loadDeviceGroups()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループ名の変更に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "エラーが発生しました")
            }
        }
    }

    fun deleteGroup(
        groupId: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (contactsRepository == null) {
            onComplete(false, "連絡先リポジトリが利用できません")
            return
        }
        viewModelScope.launch {
            try {
                val result = contactsRepository.deleteGroup(groupId)
                if (result.isSuccess) {
                    // Also remove from hidden IDs and order IDs if present
                    val currentOrder = groupOrderIds.value
                    if (groupId in currentOrder) {
                        repository.setGroupOrderIds(currentOrder.filter { it != groupId })
                    }
                    loadDeviceGroups()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループの削除に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "エラーが発生しました")
            }
        }
    }

    fun setSelectedPrefixId(id: String?) {
        viewModelScope.launch {
            repository.setSelectedPrefixId(id)
        }
    }

    fun setSelectedPrefixIdForSim(simSlot: Int, id: String?) {
        viewModelScope.launch {
            repository.setSelectedPrefixIdForSim(simSlot, id)
        }
    }

    fun setSimPhoneNumber(simSlot: Int, number: String?) {
        viewModelScope.launch {
            repository.setSimPhoneNumber(simSlot, number)
        }
    }

    fun addPrefix(name: String, prefix: String, targetSim: String = PhonePrefix.TARGET_SIM_BOTH, setAsSelected: Boolean = false) {
        viewModelScope.launch {
            repository.addPrefix(name, prefix, targetSim, setAsSelected)
        }
    }

    fun updatePrefix(id: String, name: String, prefix: String, targetSim: String = PhonePrefix.TARGET_SIM_BOTH) {
        viewModelScope.launch {
            repository.updatePrefix(id, name, prefix, targetSim)
        }
    }

    fun deletePrefix(id: String) {
        viewModelScope.launch {
            repository.deletePrefix(id)
        }
    }

    fun addExcludedPrefix(pattern: String, description: String = "") {
        viewModelScope.launch {
            repository.addExcludedPrefix(pattern, description)
        }
    }

    fun updateExcludedPrefix(id: String, pattern: String, description: String = "") {
        viewModelScope.launch {
            repository.updateExcludedPrefix(id, pattern, description)
        }
    }

    fun deleteExcludedPrefix(id: String) {
        viewModelScope.launch {
            repository.deleteExcludedPrefix(id)
        }
    }

    fun resetDefaultExcludedPrefixes() {
        viewModelScope.launch {
            repository.resetDefaultExcludedPrefixes()
        }
    }

    fun setConfirmCall(confirm: Boolean) {
        viewModelScope.launch {
            repository.setConfirmCall(confirm)
        }
    }

    fun setCallConfirmMethod(method: String) {
        viewModelScope.launch {
            repository.setCallConfirmMethod(method)
        }
    }

    fun setDefaultSimMode(mode: String) {
        viewModelScope.launch {
            repository.setDefaultSimMode(mode)
        }
    }

    fun setUseRakutenLink(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseRakutenLink(enabled)
        }
    }

    fun selectGoogleAccount(accountEmail: String) {
        viewModelScope.launch {
            repository.saveGoogleUser(accountEmail, accountEmail.substringBefore("@"), null)
            checkDriveBackupStatus()
        }
    }

    fun createChooseAccountIntent(): Intent {
        return authManager.createChooseAccountIntent(userEmail.value)
    }

    fun disconnectAccount() {
        viewModelScope.launch {
            repository.clearGoogleUser()
            _driveFileInfo.value = null
            _driveBackupState.value = DriveBackupUiState.Idle
        }
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun clearConsentIntent() {
        _userConsentIntent.value = null
    }

    // --- Google Drive バックアップ・復元 処理 ---
    fun checkDriveBackupStatus() {
        val email = userEmail.value ?: return
        if (googleDriveBackupRepository == null) return

        viewModelScope.launch {
            _driveBackupState.value = DriveBackupUiState.Checking
            when (val res = googleDriveBackupRepository.findBackupFile(email)) {
                is DriveCheckResult.Found -> {
                    _driveFileInfo.value = res.fileInfo
                    _driveBackupState.value = DriveBackupUiState.Idle
                }
                is DriveCheckResult.NotFound -> {
                    _driveFileInfo.value = null
                    _driveBackupState.value = DriveBackupUiState.Idle
                }
                is DriveCheckResult.NeedsConsent -> {
                    _userConsentIntent.value = res.consentIntent
                    _driveBackupState.value = DriveBackupUiState.Idle
                }
                is DriveCheckResult.Error -> {
                    _driveBackupState.value = DriveBackupUiState.Error(res.message)
                }
            }
        }
    }

    fun backupSettingsToDrive() {
        val email = userEmail.value
        if (email.isNullOrBlank()) {
            _driveBackupState.value = DriveBackupUiState.Error("Googleアカウントを選択してください")
            return
        }
        if (googleDriveBackupRepository == null) return

        viewModelScope.launch {
            _driveBackupState.value = DriveBackupUiState.BackingUp
            val json = repository.exportAllSettingsJson()
            when (val res = googleDriveBackupRepository.backupSettings(email, json)) {
                is DriveBackupResult.Success -> {
                    _driveFileInfo.value = res.fileInfo
                    _driveBackupState.value = DriveBackupUiState.Success(res.message, res.fileInfo)
                }
                is DriveBackupResult.NeedsConsent -> {
                    _userConsentIntent.value = res.consentIntent
                    _driveBackupState.value = DriveBackupUiState.Idle
                }
                is DriveBackupResult.Error -> {
                    _driveBackupState.value = DriveBackupUiState.Error(res.message)
                }
            }
        }
    }

    fun restoreSettingsFromDrive() {
        val email = userEmail.value
        if (email.isNullOrBlank()) {
            _driveBackupState.value = DriveBackupUiState.Error("Googleアカウントを選択してください")
            return
        }
        if (googleDriveBackupRepository == null) return

        viewModelScope.launch {
            _driveBackupState.value = DriveBackupUiState.Restoring
            when (val res = googleDriveBackupRepository.restoreSettings(email)) {
                is DriveRestoreResult.Success -> {
                    val ok = repository.restoreSettingsFromJson(res.jsonContent)
                    if (ok) {
                        _driveFileInfo.value = res.fileInfo
                        _driveBackupState.value = DriveBackupUiState.Success("Google Driveから設定を正常に復元しました", res.fileInfo)
                    } else {
                        _driveBackupState.value = DriveBackupUiState.Error("バックアップデータの解析・適用に失敗しました")
                    }
                }
                is DriveRestoreResult.NotFound -> {
                    _driveBackupState.value = DriveBackupUiState.Error(res.message)
                }
                is DriveRestoreResult.NeedsConsent -> {
                    _userConsentIntent.value = res.consentIntent
                    _driveBackupState.value = DriveBackupUiState.Idle
                }
                is DriveRestoreResult.Error -> {
                    _driveBackupState.value = DriveBackupUiState.Error(res.message)
                }
            }
        }
    }

    // --- 迷惑電話 (Spam Call Screening) 関連 ---
    val spamSyncState: StateFlow<SyncState> = spamSyncRepository?.syncState
        ?: MutableStateFlow(SyncState.Idle).asStateFlow()

    val spamSpreadsheetSource: StateFlow<String> = spamSyncRepository?.spreadsheetSource
        ?: MutableStateFlow(SpamSyncRepository.DEFAULT_SPREADSHEET_ID).asStateFlow()

    val spamCallingCode: StateFlow<String> = spamSyncRepository?.callingCode
        ?: MutableStateFlow(SpamSyncRepository.DEFAULT_CALLING_CODE).asStateFlow()

    val spamLastSyncTime: StateFlow<String> = spamSyncRepository?.lastSyncTime
        ?: MutableStateFlow("").asStateFlow()

    val spamTotalCount: StateFlow<Int> = spamSyncRepository?.totalSpamCountFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        ?: MutableStateFlow(0).asStateFlow()

    val spamAutoBlockThreshold: StateFlow<Int> = spamSyncRepository?.autoBlockThreshold
        ?: MutableStateFlow(4).asStateFlow()

    val spamBlockUnregisteredInternational: StateFlow<Boolean> = spamSyncRepository?.blockUnregisteredInternational
        ?: MutableStateFlow(false).asStateFlow()

    val spamAutoSyncEnabled: StateFlow<Boolean> = spamSyncRepository?.autoSyncEnabled
        ?: MutableStateFlow(false).asStateFlow()

    private val _spamContactMatches = MutableStateFlow<List<com.example.data.repository.SpamContactMatch>>(emptyList())
    val spamContactMatches: StateFlow<List<com.example.data.repository.SpamContactMatch>> = _spamContactMatches.asStateFlow()

    val spamSearchQuery = MutableStateFlow("")

    val spamList: StateFlow<List<SpamNumberEntity>> = spamSearchQuery.flatMapLatest { query ->
        spamSyncRepository?.getSpamListFlow(query) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testPhoneNumberQuery = MutableStateFlow("")
    val testSpamResult = MutableStateFlow<SpamNumberEntity?>(null)
    val testIsInternational = MutableStateFlow(false)
    val testIsBlockedAsInternational = MutableStateFlow(false)
    val isTestingPhoneNumber = MutableStateFlow(false)

    fun setSpamSpreadsheetSource(source: String) {
        spamSyncRepository?.setSpreadsheetSource(source)
    }

    fun setSpamCallingCode(code: String) {
        spamSyncRepository?.setCallingCode(code)
    }

    fun setSpamAutoBlockThreshold(threshold: Int) {
        spamSyncRepository?.setAutoBlockThreshold(threshold)
    }

    fun setSpamBlockUnregisteredInternational(enabled: Boolean) {
        spamSyncRepository?.setBlockUnregisteredInternational(enabled)
    }

    fun setSpamAutoSyncEnabled(enabled: Boolean) {
        spamSyncRepository?.setAutoSyncEnabled(enabled)
    }

    fun checkSpamMatchesInContacts() {
        viewModelScope.launch {
            val repo = spamSyncRepository ?: return@launch
            val contactsRepo = contactsRepository ?: return@launch
            val contacts = contactsRepo.getContacts()
            _spamContactMatches.value = repo.findSpamMatchesInContacts(contacts)
        }
    }

    fun syncSpamDatabase(customSource: String? = null, customCallingCode: String? = null) {
        viewModelScope.launch {
            spamSyncRepository?.syncSpamList(customSource, customCallingCode)
            checkSpamMatchesInContacts()
        }
    }

    fun testCheckPhoneNumber(rawNumber: String) {
        testPhoneNumberQuery.value = rawNumber
        if (rawNumber.isBlank()) {
            testSpamResult.value = null
            testIsInternational.value = false
            testIsBlockedAsInternational.value = false
            return
        }
        viewModelScope.launch {
            isTestingPhoneNumber.value = true
            val repo = spamSyncRepository
            val isInter = repo?.isInternationalNumber(rawNumber) ?: false
            val inContacts = repo?.isNumberInContacts(rawNumber) ?: false
            val blockInter = repo?.blockUnregisteredInternational?.value ?: false

            testIsInternational.value = isInter
            testIsBlockedAsInternational.value = isInter && !inContacts && blockInter
            testSpamResult.value = repo?.checkSpamNumber(rawNumber)
            isTestingPhoneNumber.value = false
        }
    }

    fun addManualSpamNumber(
        number: String,
        callingCode: String = "81",
        category: String,
        targetEntity: String?,
        riskScore: Int
    ) {
        viewModelScope.launch {
            spamSyncRepository?.addManualSpam(number, callingCode, category, targetEntity, riskScore)
        }
    }

    fun deleteSpamNumber(number: String) {
        viewModelScope.launch {
            spamSyncRepository?.deleteSpamNumber(number)
            checkSpamMatchesInContacts()
        }
    }

    fun clearAllSpamDatabase() {
        viewModelScope.launch {
            spamSyncRepository?.clearAllSpamNumbers()
            checkSpamMatchesInContacts()
        }
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val authManager: GoogleAuthManager,
    private val contactsRepository: ContactsRepository? = null,
    private val spamSyncRepository: SpamSyncRepository? = null,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository? = null,
    private val callRecordingRepository: CallRecordingRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                repository,
                authManager,
                contactsRepository,
                spamSyncRepository,
                googleDriveBackupRepository,
                callRecordingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
