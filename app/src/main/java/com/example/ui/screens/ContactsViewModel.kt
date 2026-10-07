package com.example.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ContactsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.Contact
import com.example.domain.model.ContactDetail
import com.example.domain.model.ContactFieldDisplayConfig
import com.example.domain.model.Group
import com.example.domain.model.LabeledValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val selectedTabIndex: StateFlow<Int> = settingsRepository.contactsSelectedTabFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val fieldDisplayConfig: StateFlow<ContactFieldDisplayConfig> = settingsRepository.contactFieldDisplayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactFieldDisplayConfig())

    private val _allGroups = MutableStateFlow<List<Group>>(emptyList())
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _dropdownGroupId = MutableStateFlow<String>(ContactsRepository.GROUP_ID_UNGROUPED)
    val dropdownGroupId: StateFlow<String> = _dropdownGroupId.asStateFlow()

    private val _dropdownGroupContacts = MutableStateFlow<List<Contact>>(emptyList())
    val dropdownGroupContacts: StateFlow<List<Contact>> = _dropdownGroupContacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSavingContact = MutableStateFlow(false)
    val isSavingContact: StateFlow<Boolean> = _isSavingContact.asStateFlow()

    private val _selectedContactDetail = MutableStateFlow<ContactDetail?>(null)
    val selectedContactDetail: StateFlow<ContactDetail?> = _selectedContactDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.hiddenGroupIdsFlow,
                settingsRepository.groupOrderIdsFlow
            ) { hiddenIds, orderIds ->
                Pair(hiddenIds, orderIds)
            }.collect { (hiddenIds, orderIds) ->
                val sorted = sortAndFilterGroups(_allGroups.value, hiddenIds, orderIds)
                _groups.value = sorted
                if (_dropdownGroupId.value in hiddenIds || (sorted.isNotEmpty() && sorted.none { it.id == _dropdownGroupId.value })) {
                    val fallbackId = sorted.firstOrNull()?.id ?: ContactsRepository.GROUP_ID_UNGROUPED
                    _dropdownGroupId.value = fallbackId
                    loadDropdownGroupContacts(fallbackId)
                }
            }
        }
    }

    private fun sortAndFilterGroups(
        allGroups: List<Group>,
        hiddenIds: Set<String>,
        orderIds: List<String>
    ): List<Group> {
        val filtered = allGroups.filter { it.id !in hiddenIds }
        if (orderIds.isEmpty()) return filtered
        val indexMap = orderIds.mapIndexed { index, id -> id to index }.toMap()
        return filtered.sortedWith(
            compareBy<Group> { group -> indexMap[group.id] ?: (Int.MAX_VALUE - 1000) }
                .thenBy { it.title }
        )
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groupList = repository.getGroups(includeUngrouped = true)
                _allGroups.value = groupList
                val currentHidden = try {
                    settingsRepository.hiddenGroupIdsFlow.first()
                } catch (_: Exception) {
                    emptySet()
                }
                val currentOrder = try {
                    settingsRepository.groupOrderIdsFlow.first()
                } catch (_: Exception) {
                    emptyList()
                }

                val sorted = sortAndFilterGroups(groupList, currentHidden, currentOrder)
                _groups.value = sorted

                val rememberLastGroup = try {
                    settingsRepository.rememberLastGroupFlow.first()
                } catch (_: Exception) {
                    true
                }
                val savedGroupId = if (rememberLastGroup) {
                    try {
                        settingsRepository.contactsSelectedGroupIdFlow.first()
                    } catch (_: Exception) {
                        null
                    }
                } else null

                val targetGroupId = if (!savedGroupId.isNullOrBlank() && savedGroupId !in currentHidden && sorted.any { it.id == savedGroupId }) {
                    savedGroupId
                } else if (_dropdownGroupId.value in currentHidden || (sorted.isNotEmpty() && sorted.none { it.id == _dropdownGroupId.value })) {
                    sorted.firstOrNull()?.id ?: ContactsRepository.GROUP_ID_UNGROUPED
                } else {
                    _dropdownGroupId.value
                }

                _dropdownGroupId.value = targetGroupId

                _contacts.value = repository.getContacts(_selectedGroupId.value)
                loadDropdownGroupContacts(targetGroupId)
            } catch (_: SecurityException) {
                // Permission not granted
            } catch (_: Exception) {
                // Ignore
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedTabIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setContactsSelectedTab(index)
        }
    }

    fun selectGroup(groupId: String?) {
        _selectedGroupId.value = groupId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _contacts.value = repository.getContacts(groupId)
            } catch (_: Exception) {
                // Ignore
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDropdownGroup(groupId: String) {
        _dropdownGroupId.value = groupId
        loadDropdownGroupContacts(groupId)
        viewModelScope.launch {
            try {
                if (settingsRepository.rememberLastGroupFlow.first()) {
                    settingsRepository.setContactsSelectedGroupId(groupId)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun createGroup(
        title: String,
        accountName: String? = null,
        accountType: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = repository.createGroup(title, accountName, accountType)
                if (result.isSuccess) {
                    loadData()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループ作成に失敗しました")
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
        viewModelScope.launch {
            try {
                val result = repository.updateGroupTitle(groupId, newTitle)
                if (result.isSuccess) {
                    loadData()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループ名変更に失敗しました")
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
        viewModelScope.launch {
            try {
                val result = repository.deleteGroup(groupId)
                if (result.isSuccess) {
                    loadData()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "グループ削除に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "エラーが発生しました")
            }
        }
    }

    private fun loadDropdownGroupContacts(groupId: String) {
        viewModelScope.launch {
            try {
                _dropdownGroupContacts.value = repository.getContacts(groupId)
            } catch (_: Exception) {
                _dropdownGroupContacts.value = emptyList()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun processImageUriToByteArray(uri: Uri): ByteArray? {
        return repository.processImageUriToByteArray(uri)
    }

    fun loadContactDetail(contactId: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            try {
                _selectedContactDetail.value = repository.getContactDetail(contactId)
            } catch (e: Exception) {
                _selectedContactDetail.value = null
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun clearContactDetail() {
        _selectedContactDetail.value = null
    }

    fun addContact(
        name: String,
        phoneNumbers: List<LabeledValue>,
        emails: List<LabeledValue> = emptyList(),
        company: String? = null,
        jobTitle: String? = null,
        department: String? = null,
        address: String? = null,
        websites: List<String> = emptyList(),
        note: String? = null,
        groupId: String? = null,
        photoBytes: ByteArray? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isSavingContact.value = true
            try {
                val result = repository.addContact(
                    name = name,
                    phoneNumbers = phoneNumbers,
                    emails = emails,
                    company = company,
                    jobTitle = jobTitle,
                    department = department,
                    address = address,
                    websites = websites,
                    note = note,
                    groupId = groupId,
                    photoBytes = photoBytes
                )
                if (result.isSuccess) {
                    loadData()
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "連絡先の追加に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "連絡先の追加に失敗しました")
            } finally {
                _isSavingContact.value = false
            }
        }
    }

    fun updateContact(
        contactId: String,
        name: String,
        phoneNumbers: List<LabeledValue>,
        emails: List<LabeledValue> = emptyList(),
        company: String? = null,
        jobTitle: String? = null,
        department: String? = null,
        address: String? = null,
        websites: List<String> = emptyList(),
        note: String? = null,
        groupId: String? = null,
        photoBytes: ByteArray? = null,
        removePhoto: Boolean = false,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isSavingContact.value = true
            try {
                val result = repository.updateContact(
                    contactId = contactId,
                    name = name,
                    phoneNumbers = phoneNumbers,
                    emails = emails,
                    company = company,
                    jobTitle = jobTitle,
                    department = department,
                    address = address,
                    websites = websites,
                    note = note,
                    groupId = groupId,
                    photoBytes = photoBytes,
                    removePhoto = removePhoto
                )
                if (result.isSuccess) {
                    loadData()
                    _selectedContactDetail.value = repository.getContactDetail(contactId)
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "連絡先の更新に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "連絡先の更新に失敗しました")
            } finally {
                _isSavingContact.value = false
            }
        }
    }

    fun deleteContact(contactId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSavingContact.value = true
            try {
                val result = repository.deleteContact(contactId)
                if (result.isSuccess) {
                    loadData()
                    _selectedContactDetail.value = null
                    onComplete(true, null)
                } else {
                    onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "連絡先の削除に失敗しました")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "連絡先の削除に失敗しました")
            } finally {
                _isSavingContact.value = false
            }
        }
    }
}

class ContactsViewModelFactory(
    private val repository: ContactsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
