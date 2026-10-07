package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.PhonebookApplication
import com.example.data.repository.ContactsRepository
import com.example.domain.model.Contact
import com.example.domain.model.ContactDetail
import com.example.domain.model.ContactFieldDisplayConfig
import com.example.domain.model.Group
import com.example.domain.model.LabeledValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    onCallContact: (String) -> Unit = {},
    viewModel: ContactsViewModel = viewModel(
        factory = ContactsViewModelFactory(
            (LocalContext.current.applicationContext as PhonebookApplication).contactsRepository,
            (LocalContext.current.applicationContext as PhonebookApplication).settingsRepository
        )
    )
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val dropdownGroupId by viewModel.dropdownGroupId.collectAsStateWithLifecycle()
    val dropdownGroupContacts by viewModel.dropdownGroupContacts.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSavingContact by viewModel.isSavingContact.collectAsStateWithLifecycle()
    val selectedContactDetail by viewModel.selectedContactDetail.collectAsStateWithLifecycle()
    val fieldConfig by viewModel.fieldDisplayConfig.collectAsStateWithLifecycle()

    val tabs = listOf("グループ一覧", "すべて")
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var editingContactDetail by remember { mutableStateOf<ContactDetail?>(null) }
    var contactToDelete by remember { mutableStateOf<ContactDetail?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.loadData()
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.setSearchQuery("")
    }

    val filteredAllContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.replace("-", "").contains(searchQuery.replace("-", ""))
            }
        }
    }

    val filteredGroupContacts = remember(dropdownGroupContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            dropdownGroupContacts
        } else {
            dropdownGroupContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.replace("-", "").contains(searchQuery.replace("-", ""))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("名前または電話番号で検索") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "クリア")
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "連絡先",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "検索を閉じる")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "検索")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "連絡先を追加")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isSearchActive) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { viewModel.setSelectedTabIndex(index) },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when {
                    isSearchActive -> {
                        AllContactsTabContent(
                            contacts = filteredAllContacts,
                            searchQuery = searchQuery,
                            onContactClick = { contact -> viewModel.loadContactDetail(contact.id) },
                            onCallContact = onCallContact
                        )
                    }
                    selectedTabIndex == 0 -> {
                        GroupContactsTabContent(
                            groups = groups,
                            selectedGroupId = dropdownGroupId,
                            contacts = filteredGroupContacts,
                            searchQuery = searchQuery,
                            onGroupSelected = { viewModel.selectDropdownGroup(it) },
                            onAddGroupClick = { showAddGroupDialog = true },
                            onEditGroupClick = { grp -> groupToEdit = grp },
                            onDeleteGroupClick = { grp -> groupToDelete = grp },
                            onContactClick = { contact -> viewModel.loadContactDetail(contact.id) },
                            onCallContact = onCallContact
                        )
                    }
                    else -> {
                        AllContactsTabContent(
                            contacts = filteredAllContacts,
                            searchQuery = searchQuery,
                            onContactClick = { contact -> viewModel.loadContactDetail(contact.id) },
                            onCallContact = onCallContact
                        )
                    }
                }
            }
        }
    }

    // 連絡先詳細シート
    if (selectedContactDetail != null) {
        val detail = selectedContactDetail!!
        ContactDetailBottomSheet(
            detail = detail,
            fieldConfig = fieldConfig,
            onDismiss = { viewModel.clearContactDetail() },
            onCall = { num -> onCallContact(num) },
            onEdit = {
                editingContactDetail = detail
                viewModel.clearContactDetail()
            },
            onDelete = {
                contactToDelete = detail
                viewModel.clearContactDetail()
            }
        )
    }

    // 連絡先編集ダイアログ
    if (editingContactDetail != null) {
        val detail = editingContactDetail!!
        EditContactDialog(
            contactDetail = detail,
            fieldConfig = fieldConfig,
            groups = groups,
            isLoading = isSavingContact,
            onProcessPhoto = { uri -> viewModel.processImageUriToByteArray(uri) },
            onDismiss = { editingContactDetail = null },
            onSave = { name, phoneNumbers, emails, company, jobTitle, department, address, websites, note, groupId, photoBytes, removePhoto ->
                viewModel.updateContact(
                    contactId = detail.id,
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
                ) { success, errMsg ->
                    if (success) {
                        editingContactDetail = null
                        Toast.makeText(context, "連絡先を更新しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, errMsg ?: "更新に失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 連絡先追加ダイアログ
    if (showAddDialog) {
        AddContactDialog(
            fieldConfig = fieldConfig,
            groups = groups,
            isLoading = isSavingContact,
            defaultGroupId = if (selectedTabIndex == 0 && dropdownGroupId != ContactsRepository.GROUP_ID_UNGROUPED) dropdownGroupId else null,
            onProcessPhoto = { uri -> viewModel.processImageUriToByteArray(uri) },
            onDismiss = { showAddDialog = false },
            onSave = { name, phoneNumbers, emails, company, jobTitle, department, address, websites, note, groupId, photoBytes ->
                viewModel.addContact(
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
                ) { success, errMsg ->
                    if (success) {
                        showAddDialog = false
                        Toast.makeText(context, "連絡先を追加しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, errMsg ?: "追加に失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 連絡先削除確認ダイアログ
    if (contactToDelete != null) {
        val detail = contactToDelete!!
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("連絡先を削除") },
            text = { Text("${detail.name} を連絡先から削除しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(detail.id) { success, errMsg ->
                            contactToDelete = null
                            if (success) {
                                Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, errMsg ?: "削除に失敗しました", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 新規グループ追加ダイアログ
    if (showAddGroupDialog) {
        var groupTitleText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("新規グループを追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "グループ名を入力してください。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = groupTitleText,
                        onValueChange = { groupTitleText = it },
                        label = { Text("グループ名 *") },
                        placeholder = { Text("例: 仕事, 家族, 友人") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupTitleText.trim().isNotBlank()) {
                            viewModel.createGroup(groupTitleText.trim()) { success, msg ->
                                showAddGroupDialog = false
                                if (success) {
                                    Toast.makeText(context, "グループ「${groupTitleText.trim()}」を追加しました", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg ?: "追加に失敗しました", Toast.LENGTH_SHORT).show()
                                }
                            }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            viewModel.updateGroupTitle(targetGroup.id, editTitleText.trim()) { success, msg ->
                                groupToEdit = null
                                if (success) {
                                    Toast.makeText(context, "グループ名を「${editTitleText.trim()}」に変更しました", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg ?: "変更に失敗しました", Toast.LENGTH_SHORT).show()
                                }
                            }
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
                        viewModel.deleteGroup(targetGroup.id) { success, msg ->
                            groupToDelete = null
                            if (success) {
                                Toast.makeText(context, "グループ「${targetGroup.title}」を削除しました", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, msg ?: "削除に失敗しました", Toast.LENGTH_SHORT).show()
                            }
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupContactsTabContent(
    groups: List<Group>,
    selectedGroupId: String,
    contacts: List<Contact>,
    searchQuery: String,
    onGroupSelected: (String) -> Unit,
    onAddGroupClick: () -> Unit = {},
    onEditGroupClick: (Group) -> Unit = {},
    onDeleteGroupClick: (Group) -> Unit = {},
    onContactClick: (Contact) -> Unit,
    onCallContact: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedGroup = remember(selectedGroupId, groups) {
        groups.firstOrNull { it.id == selectedGroupId }
    }

    val isUngrouped = selectedGroupId == ContactsRepository.GROUP_ID_UNGROUPED

    val selectedGroupTitle = remember(selectedGroupId, groups) {
        if (isUngrouped) {
            "未グループ"
        } else {
            selectedGroup?.title ?: "未グループ"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isUngrouped) Icons.Default.FolderSpecial else Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "$selectedGroupTitle (${contacts.size}件)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (group.id == ContactsRepository.GROUP_ID_UNGROUPED) Icons.Default.FolderSpecial else Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = { Text(group.title, fontWeight = if (group.id == selectedGroupId) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onGroupSelected(group.id)
                                    dropdownExpanded = false
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            text = { Text("新規グループを追加...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dropdownExpanded = false
                                onAddGroupClick()
                            }
                        )
                    }
                }

                // 選択中グループの編集・削除クイックアクションボタン
                if (!isUngrouped && selectedGroup != null) {
                    IconButton(
                        onClick = { onEditGroupClick(selectedGroup) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "グループ名を変更",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteGroupClick(selectedGroup) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "グループを削除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ContactsList(
            contacts = contacts,
            emptyMessage = if (searchQuery.isNotEmpty()) "一致する連絡先はありません" else "このグループに連絡先はありません",
            onContactClick = onContactClick,
            onCallContact = onCallContact
        )
    }
}

@Composable
fun AllContactsTabContent(
    contacts: List<Contact>,
    searchQuery: String,
    onContactClick: (Contact) -> Unit,
    onCallContact: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isNotEmpty()) "検索結果 (${contacts.size}件)" else "すべての連絡先 (${contacts.size}件)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ContactsList(
            contacts = contacts,
            emptyMessage = if (searchQuery.isNotEmpty()) "一致する連絡先はありません" else "連絡先が登録されていません",
            onContactClick = onContactClick,
            onCallContact = onCallContact
        )
    }
}

@Composable
fun ContactsList(
    contacts: List<Contact>,
    emptyMessage: String = "連絡先がありません",
    onContactClick: (Contact) -> Unit = {},
    onCallContact: (String) -> Unit = {}
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyMessage,
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
            items(contacts, key = { it.id }) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onContactClick(contact) },
                    onCall = { onCallContact(contact.phoneNumber) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit = {},
    onCall: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = contact.photoUri,
            name = contact.name,
            size = 46.dp
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (contact.phoneNumber.isNotEmpty()) {
                val context = LocalContext.current
                val formattedNumber = remember(contact.phoneNumber) {
                    com.example.util.PhoneNumberFormatter.formatForDisplay(context, contact.phoneNumber)
                }
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (contact.phoneNumber.isNotEmpty()) {
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
                    contentDescription = "${contact.name} に電話を発信",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ContactAvatar(
    photoUri: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "$name の写真",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailBottomSheet(
    detail: ContactDetail,
    fieldConfig: ContactFieldDisplayConfig,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var isAccordionExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                photoUri = detail.photoUri,
                name = detail.name,
                size = 96.dp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!detail.company.isNullOrBlank() || !detail.jobTitle.isNullOrBlank()) {
                val compTitle = listOfNotNull(detail.company, detail.jobTitle).joinToString(" • ")
                Text(
                    text = compTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 電話番号（強制表示・複数表示対応）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "電話番号 (強制表示)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    detail.phoneNumbers.forEachIndexed { idx, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (idx == 0) "メイン番号" else "追加番号 $idx",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = phone.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(
                                onClick = { onCall(phone.value) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "発信", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (idx < detail.phoneNumbers.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // 表（メイン）に設定された項目の表示
            val hasMainContent = (fieldConfig.showEmailInMain && detail.emails.isNotEmpty()) ||
                    (fieldConfig.showCompanyInMain && (!detail.department.isNullOrBlank())) ||
                    (fieldConfig.showAddressInMain && !detail.address.isNullOrBlank()) ||
                    (fieldConfig.showWebsitesInMain && detail.websites.isNotEmpty()) ||
                    (fieldConfig.showNoteInMain && !detail.note.isNullOrBlank())

            if (hasMainContent) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (fieldConfig.showEmailInMain && detail.emails.isNotEmpty()) {
                            detail.emails.forEach { mail ->
                                DetailRow(icon = Icons.Default.Email, label = "メールアドレス", value = mail.value)
                            }
                        }
                        if (fieldConfig.showCompanyInMain && !detail.department.isNullOrBlank()) {
                            DetailRow(icon = Icons.Default.Business, label = "部署", value = detail.department!!)
                        }
                        if (fieldConfig.showAddressInMain && !detail.address.isNullOrBlank()) {
                            DetailRow(icon = Icons.Default.Home, label = "住所", value = detail.address!!)
                        }
                        if (fieldConfig.showWebsitesInMain && detail.websites.isNotEmpty()) {
                            detail.websites.forEach { site ->
                                DetailRow(icon = Icons.Default.Language, label = "Webサイト", value = site)
                            }
                        }
                        if (fieldConfig.showNoteInMain && !detail.note.isNullOrBlank()) {
                            DetailRow(icon = Icons.Default.Notes, label = "メモ", value = detail.note!!)
                        }
                    }
                }
            }

            // アコーディオン（設定で表OFFにされた項目のうち、データが存在するもの）
            val hasAccordionContent = (!fieldConfig.showEmailInMain && detail.emails.isNotEmpty()) ||
                    (!fieldConfig.showCompanyInMain && (!detail.company.isNullOrBlank() || !detail.jobTitle.isNullOrBlank() || !detail.department.isNullOrBlank())) ||
                    (!fieldConfig.showAddressInMain && !detail.address.isNullOrBlank()) ||
                    (!fieldConfig.showWebsitesInMain && detail.websites.isNotEmpty()) ||
                    (!fieldConfig.showNoteInMain && !detail.note.isNullOrBlank())

            if (hasAccordionContent) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAccordionExpanded = !isAccordionExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "詳細情報・その他の項目 (アコーディオン)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (isAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(
                            visible = isAccordionExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                if (!fieldConfig.showCompanyInMain && !detail.company.isNullOrBlank()) {
                                    DetailRow(icon = Icons.Default.Business, label = "会社名", value = detail.company!!)
                                }
                                if (!fieldConfig.showCompanyInMain && !detail.jobTitle.isNullOrBlank()) {
                                    DetailRow(icon = Icons.Default.Work, label = "役職", value = detail.jobTitle!!)
                                }
                                if (!fieldConfig.showCompanyInMain && !detail.department.isNullOrBlank()) {
                                    DetailRow(icon = Icons.Default.Business, label = "部署", value = detail.department!!)
                                }
                                if (!fieldConfig.showEmailInMain && detail.emails.isNotEmpty()) {
                                    detail.emails.forEach { mail ->
                                        DetailRow(icon = Icons.Default.Email, label = "メールアドレス", value = mail.value)
                                    }
                                }
                                if (!fieldConfig.showAddressInMain && !detail.address.isNullOrBlank()) {
                                    DetailRow(icon = Icons.Default.Home, label = "住所", value = detail.address!!)
                                }
                                if (!fieldConfig.showWebsitesInMain && detail.websites.isNotEmpty()) {
                                    detail.websites.forEach { site ->
                                        DetailRow(icon = Icons.Default.Language, label = "Webサイト", value = site)
                                    }
                                }
                                if (!fieldConfig.showNoteInMain && !detail.note.isNullOrBlank()) {
                                    DetailRow(icon = Icons.Default.Notes, label = "メモ", value = detail.note!!)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // アクションボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onCall(detail.phoneNumber) },
                    modifier = Modifier.weight(1f),
                    enabled = detail.phoneNumber.isNotBlank()
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("発信")
                }

                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("編集")
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "削除")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactDialog(
    contactDetail: ContactDetail,
    fieldConfig: ContactFieldDisplayConfig,
    groups: List<Group>,
    isLoading: Boolean,
    onProcessPhoto: (Uri) -> ByteArray?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phoneNumbers: List<LabeledValue>,
        emails: List<LabeledValue>,
        company: String?,
        jobTitle: String?,
        department: String?,
        address: String?,
        websites: List<String>,
        note: String?,
        groupId: String?,
        photoBytes: ByteArray?,
        removePhoto: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(contactDetail.name) }
    val phoneNumbers = remember {
        mutableStateListOf<String>().apply {
            if (contactDetail.phoneNumbers.isNotEmpty()) {
                addAll(contactDetail.phoneNumbers.map { it.value })
            } else {
                add("")
            }
        }
    }
    val emails = remember {
        mutableStateListOf<String>().apply {
            if (contactDetail.emails.isNotEmpty()) {
                addAll(contactDetail.emails.map { it.value })
            }
        }
    }
    var company by remember { mutableStateOf(contactDetail.company ?: "") }
    var jobTitle by remember { mutableStateOf(contactDetail.jobTitle ?: "") }
    var department by remember { mutableStateOf(contactDetail.department ?: "") }
    var address by remember { mutableStateOf(contactDetail.address ?: "") }
    val websites = remember {
        mutableStateListOf<String>().apply {
            addAll(contactDetail.websites)
        }
    }
    var note by remember { mutableStateOf(contactDetail.note ?: "") }
    var selectedGroupId by remember { mutableStateOf(contactDetail.groupId) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var removePhotoFlag by remember { mutableStateOf(false) }
    var newPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }

    var isAccordionExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            removePhotoFlag = false
            newPhotoBytes = onProcessPhoto(uri)
        }
    }

    val availableGroups = remember(groups) {
        groups.filter { it.id != ContactsRepository.GROUP_ID_UNGROUPED }
    }
    val selectedGroupTitle = remember(selectedGroupId, availableGroups) {
        if (selectedGroupId == null || selectedGroupId == ContactsRepository.GROUP_ID_UNGROUPED) {
            "未グループ (グループなし)"
        } else {
            availableGroups.firstOrNull { it.id == selectedGroupId }?.title ?: "未グループ"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("連絡先を編集", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 強制表示: 写真アバター
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "選択した写真",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!contactDetail.photoUri.isNullOrBlank() && !removePhotoFlag) {
                            AsyncImage(
                                model = contactDetail.photoUri,
                                contentDescription = "現在の写真",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name.take(1).ifEmpty { "?" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "写真を変更",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("写真を変更", fontSize = 12.sp)
                    }

                    if ((!contactDetail.photoUri.isNullOrBlank() && !removePhotoFlag) || selectedImageUri != null) {
                        TextButton(
                            onClick = {
                                selectedImageUri = null
                                newPhotoBytes = null
                                removePhotoFlag = true
                            }
                        ) {
                            Text("写真を削除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }

                // 2. 強制表示: 氏名 (必須)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("氏名 (必須・強制表示)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. 強制表示: 電話番号（追加・削除対応）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "電話番号 (必須・強制表示)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    phoneNumbers.forEachIndexed { index, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phoneNumbers[index] = it },
                                label = { Text(if (index == 0) "メイン番号 (必須)" else "追加番号 $index") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (index > 0) {
                                IconButton(onClick = { phoneNumbers.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { phoneNumbers.add("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("電話番号を追加")
                    }
                }

                // 4. 設定で「表に表示」が選択されている項目
                ContactFieldsFormSection(
                    fieldConfig = fieldConfig,
                    isMainSection = true,
                    company = company, onCompanyChange = { company = it },
                    jobTitle = jobTitle, onJobTitleChange = { jobTitle = it },
                    department = department, onDepartmentChange = { department = it },
                    emails = emails,
                    address = address, onAddressChange = { address = it },
                    websites = websites,
                    note = note, onNoteChange = { note = it },
                    selectedGroupId = selectedGroupId,
                    selectedGroupTitle = selectedGroupTitle,
                    availableGroups = availableGroups,
                    groupDropdownExpanded = groupDropdownExpanded,
                    onGroupDropdownChange = { groupDropdownExpanded = it },
                    onSelectGroup = { selectedGroupId = it }
                )

                // 5. 設定で「アコーディオン内」が選択されている項目
                val hasAccordionFields = !fieldConfig.showCompanyInMain ||
                        !fieldConfig.showEmailInMain ||
                        !fieldConfig.showAddressInMain ||
                        !fieldConfig.showWebsitesInMain ||
                        !fieldConfig.showGroupInMain ||
                        !fieldConfig.showNoteInMain

                if (hasAccordionFields) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAccordionExpanded = !isAccordionExpanded }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "その他の項目 (アコーディオン)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (isAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(
                                visible = isAccordionExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ContactFieldsFormSection(
                                        fieldConfig = fieldConfig,
                                        isMainSection = false,
                                        company = company, onCompanyChange = { company = it },
                                        jobTitle = jobTitle, onJobTitleChange = { jobTitle = it },
                                        department = department, onDepartmentChange = { department = it },
                                        emails = emails,
                                        address = address, onAddressChange = { address = it },
                                        websites = websites,
                                        note = note, onNoteChange = { note = it },
                                        selectedGroupId = selectedGroupId,
                                        selectedGroupTitle = selectedGroupTitle,
                                        availableGroups = availableGroups,
                                        groupDropdownExpanded = groupDropdownExpanded,
                                        onGroupDropdownChange = { groupDropdownExpanded = it },
                                        onSelectGroup = { selectedGroupId = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val phoneList = phoneNumbers.filter { it.isNotBlank() }.map { LabeledValue(value = it.trim()) }
                    val emailList = emails.filter { it.isNotBlank() }.map { LabeledValue(value = it.trim()) }
                    val websiteList = websites.filter { it.isNotBlank() }.map { it.trim() }

                    onSave(
                        name.trim(),
                        phoneList,
                        emailList,
                        company.trim().ifBlank { null },
                        jobTitle.trim().ifBlank { null },
                        department.trim().ifBlank { null },
                        address.trim().ifBlank { null },
                        websiteList,
                        note.trim().ifBlank { null },
                        selectedGroupId,
                        newPhotoBytes,
                        removePhotoFlag
                    )
                },
                enabled = name.isNotBlank() && phoneNumbers.any { it.isNotBlank() } && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("キャンセル")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
    fieldConfig: ContactFieldDisplayConfig,
    groups: List<Group>,
    isLoading: Boolean,
    defaultGroupId: String? = null,
    onProcessPhoto: (Uri) -> ByteArray?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phoneNumbers: List<LabeledValue>,
        emails: List<LabeledValue>,
        company: String?,
        jobTitle: String?,
        department: String?,
        address: String?,
        websites: List<String>,
        note: String?,
        groupId: String?,
        photoBytes: ByteArray?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val phoneNumbers = remember { mutableStateListOf("") }
    val emails = remember { mutableStateListOf<String>() }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val websites = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(defaultGroupId) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isAccordionExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            photoBytes = onProcessPhoto(uri)
        }
    }

    val availableGroups = remember(groups) {
        groups.filter { it.id != ContactsRepository.GROUP_ID_UNGROUPED }
    }
    val selectedGroupTitle = remember(selectedGroupId, availableGroups) {
        if (selectedGroupId == null || selectedGroupId == ContactsRepository.GROUP_ID_UNGROUPED) {
            "未グループ (グループなし)"
        } else {
            availableGroups.firstOrNull { it.id == selectedGroupId }?.title ?: "未グループ"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("連絡先の追加", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 強制表示: 写真
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "選択した写真",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "写真を追加",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text(if (selectedImageUri != null) "写真を変更" else "写真を追加 (強制表示項目)", fontSize = 12.sp)
                }

                // 2. 強制表示: 氏名
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("氏名 (必須・強制表示)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. 強制表示: 電話番号
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "電話番号 (必須・強制表示)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    phoneNumbers.forEachIndexed { index, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phoneNumbers[index] = it },
                                label = { Text(if (index == 0) "メイン番号 (必須)" else "追加番号 $index") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (index > 0) {
                                IconButton(onClick = { phoneNumbers.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { phoneNumbers.add("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("電話番号を追加")
                    }
                }

                // 4. 表に表示する項目 (showInMain == true)
                ContactFieldsFormSection(
                    fieldConfig = fieldConfig,
                    isMainSection = true,
                    company = company, onCompanyChange = { company = it },
                    jobTitle = jobTitle, onJobTitleChange = { jobTitle = it },
                    department = department, onDepartmentChange = { department = it },
                    emails = emails,
                    address = address, onAddressChange = { address = it },
                    websites = websites,
                    note = note, onNoteChange = { note = it },
                    selectedGroupId = selectedGroupId,
                    selectedGroupTitle = selectedGroupTitle,
                    availableGroups = availableGroups,
                    groupDropdownExpanded = groupDropdownExpanded,
                    onGroupDropdownChange = { groupDropdownExpanded = it },
                    onSelectGroup = { selectedGroupId = it }
                )

                // 5. アコーディオンに表示する項目 (showInMain == false)
                val hasAccordionFields = !fieldConfig.showCompanyInMain ||
                        !fieldConfig.showEmailInMain ||
                        !fieldConfig.showAddressInMain ||
                        !fieldConfig.showWebsitesInMain ||
                        !fieldConfig.showGroupInMain ||
                        !fieldConfig.showNoteInMain

                if (hasAccordionFields) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAccordionExpanded = !isAccordionExpanded }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "その他の項目 (アコーディオン)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (isAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(
                                visible = isAccordionExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ContactFieldsFormSection(
                                        fieldConfig = fieldConfig,
                                        isMainSection = false,
                                        company = company, onCompanyChange = { company = it },
                                        jobTitle = jobTitle, onJobTitleChange = { jobTitle = it },
                                        department = department, onDepartmentChange = { department = it },
                                        emails = emails,
                                        address = address, onAddressChange = { address = it },
                                        websites = websites,
                                        note = note, onNoteChange = { note = it },
                                        selectedGroupId = selectedGroupId,
                                        selectedGroupTitle = selectedGroupTitle,
                                        availableGroups = availableGroups,
                                        groupDropdownExpanded = groupDropdownExpanded,
                                        onGroupDropdownChange = { groupDropdownExpanded = it },
                                        onSelectGroup = { selectedGroupId = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val phoneList = phoneNumbers.filter { it.isNotBlank() }.map { LabeledValue(value = it.trim()) }
                    val emailList = emails.filter { it.isNotBlank() }.map { LabeledValue(value = it.trim()) }
                    val websiteList = websites.filter { it.isNotBlank() }.map { it.trim() }

                    onSave(
                        name.trim(),
                        phoneList,
                        emailList,
                        company.trim().ifBlank { null },
                        jobTitle.trim().ifBlank { null },
                        department.trim().ifBlank { null },
                        address.trim().ifBlank { null },
                        websiteList,
                        note.trim().ifBlank { null },
                        selectedGroupId,
                        photoBytes
                    )
                },
                enabled = name.isNotBlank() && phoneNumbers.any { it.isNotBlank() } && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("キャンセル")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactFieldsFormSection(
    fieldConfig: ContactFieldDisplayConfig,
    isMainSection: Boolean,
    company: String, onCompanyChange: (String) -> Unit,
    jobTitle: String, onJobTitleChange: (String) -> Unit,
    department: String, onDepartmentChange: (String) -> Unit,
    emails: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    address: String, onAddressChange: (String) -> Unit,
    websites: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    note: String, onNoteChange: (String) -> Unit,
    selectedGroupId: String?,
    selectedGroupTitle: String,
    availableGroups: List<Group>,
    groupDropdownExpanded: Boolean,
    onGroupDropdownChange: (Boolean) -> Unit,
    onSelectGroup: (String?) -> Unit
) {
    // 会社・役職・部署
    if (fieldConfig.showCompanyInMain == isMainSection) {
        OutlinedTextField(
            value = company,
            onValueChange = onCompanyChange,
            label = { Text("会社・所属") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = jobTitle,
                onValueChange = onJobTitleChange,
                label = { Text("役職") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = department,
                onValueChange = onDepartmentChange,
                label = { Text("部署") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // メールアドレス (複数追加対応)
    if (fieldConfig.showEmailInMain == isMainSection) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "メールアドレス",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            emails.forEachIndexed { index, email ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { emails[index] = it },
                        label = { Text(if (index == 0) "メールアドレス" else "追加メール $index") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { emails.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TextButton(
                onClick = { emails.add("") },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("メールアドレスを追加")
            }
        }
    }

    // 住所
    if (fieldConfig.showAddressInMain == isMainSection) {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("住所 (所在地)") },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
            singleLine = false,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Webサイト (複数追加対応)
    if (fieldConfig.showWebsitesInMain == isMainSection) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Webサイト",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            websites.forEachIndexed { index, site ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = site,
                        onValueChange = { websites[index] = it },
                        label = { Text("WebサイトURL (${index + 1})") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { websites.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TextButton(
                onClick = { websites.add("") },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Webサイトを追加")
            }
        }
    }

    // 所属グループ
    if (fieldConfig.showGroupInMain == isMainSection) {
        ExposedDropdownMenuBox(
            expanded = groupDropdownExpanded,
            onExpandedChange = onGroupDropdownChange
        ) {
            OutlinedTextField(
                value = selectedGroupTitle,
                onValueChange = {},
                readOnly = true,
                label = { Text("所属グループ") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = groupDropdownExpanded,
                onDismissRequest = { onGroupDropdownChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("未グループ (グループなし)") },
                    onClick = {
                        onSelectGroup(null)
                        onGroupDropdownChange(false)
                    }
                )
                availableGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.title) },
                        onClick = {
                            onSelectGroup(group.id)
                            onGroupDropdownChange(false)
                        }
                    )
                }
            }
        }
    }

    // メモ
    if (fieldConfig.showNoteInMain == isMainSection) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("メモ・備考") },
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
