package com.example.data.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import com.example.domain.model.Contact
import com.example.domain.model.ContactDetail
import com.example.domain.model.Group
import com.example.domain.model.LabeledValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ContactsRepository(private val context: Context) {

    companion object {
        const val GROUP_ID_UNGROUPED = "__ungrouped__"
    }

    suspend fun getContacts(groupId: String? = null): List<Contact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<Contact>()
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (groupId != null) {
            if (groupId == GROUP_ID_UNGROUPED) {
                val groupedIds = getAllContactIdsWithAnyGroup()
                if (groupedIds.isNotEmpty()) {
                    val placeholders = groupedIds.joinToString(",") { "?" }
                    selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} NOT IN ($placeholders)"
                    selectionArgs = groupedIds.toTypedArray()
                }
            } else {
                val groupContacts = getContactIdsInGroup(groupId)
                if (groupContacts.isEmpty()) return@withContext emptyList()
                
                val placeholders = groupContacts.joinToString(",") { "?" }
                selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)"
                selectionArgs = groupContacts.toTypedArray()
            }
        }

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (it.moveToNext()) {
                contactsList.add(
                    Contact(
                        id = it.getString(idIndex),
                        name = it.getString(nameIndex) ?: "",
                        phoneNumber = it.getString(numberIndex) ?: "",
                        photoUri = it.getString(photoIndex),
                        isStarred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false
                    )
                )
            }
        }
        
        contactsList.distinctBy { it.id }
    }

    suspend fun getContactDetail(contactId: String): ContactDetail? = withContext(Dispatchers.IO) {
        var name = ""
        val phoneNumbers = mutableListOf<LabeledValue>()
        val emails = mutableListOf<LabeledValue>()
        val websites = mutableListOf<String>()
        var company: String? = null
        var jobTitle: String? = null
        var department: String? = null
        var address: String? = null
        var note: String? = null
        var photoUri: String? = null
        var groupId: String? = null
        var isStarred = false

        // 1. 電話番号一覧 & 基本情報
        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        phoneCursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
            val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (it.moveToNext()) {
                if (name.isBlank() && nameIdx >= 0) {
                    name = it.getString(nameIdx) ?: ""
                }
                if (photoUri == null && photoIdx >= 0) {
                    photoUri = it.getString(photoIdx)
                }
                if (starIdx >= 0) {
                    isStarred = it.getInt(starIdx) == 1
                }
                if (numIdx >= 0) {
                    val num = it.getString(numIdx) ?: ""
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = if (labelIdx >= 0) it.getString(labelIdx) ?: "" else ""
                    if (num.isNotBlank()) {
                        phoneNumbers.add(LabeledValue(value = num, type = type, label = label))
                    }
                }
            }
        }

        // 2. メールアドレス一覧
        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL
            ),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        emailCursor?.use {
            val dataIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
            val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)
            while (it.moveToNext()) {
                val mail = it.getString(dataIdx) ?: ""
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Email.TYPE_WORK
                val label = if (labelIdx >= 0) it.getString(labelIdx) ?: "" else ""
                if (mail.isNotBlank()) {
                    emails.add(LabeledValue(value = mail, type = type, label = label))
                }
            }
        }

        // 3. 会社・役職・部署 (Organization)
        val orgCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE,
                ContactsContract.CommonDataKinds.Organization.DEPARTMENT
            ),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )
        orgCursor?.use {
            if (it.moveToFirst()) {
                company = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
                jobTitle = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE))
                department = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.DEPARTMENT))
            }
        }

        // 4. 住所 (StructuredPostal)
        val postalCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE),
            null
        )
        postalCursor?.use {
            if (it.moveToFirst()) {
                address = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
            }
        }

        // 5. Webサイト (Website)
        val websiteCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Website.URL),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE),
            null
        )
        websiteCursor?.use {
            val urlIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)
            while (it.moveToNext()) {
                val url = it.getString(urlIdx) ?: ""
                if (url.isNotBlank()) websites.add(url)
            }
        }

        // 6. メモ (Note)
        val noteCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Note.NOTE),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
            null
        )
        noteCursor?.use {
            if (it.moveToFirst()) {
                note = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
            }
        }

        // 7. グループID
        val groupCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )
        groupCursor?.use {
            if (it.moveToFirst()) {
                groupId = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID))
            }
        }

        if (name.isBlank() && phoneNumbers.isEmpty()) return@withContext null

        ContactDetail(
            id = contactId,
            name = name,
            phoneNumbers = phoneNumbers.distinctBy { it.value },
            emails = emails.distinctBy { it.value },
            company = company,
            jobTitle = jobTitle,
            department = department,
            address = address,
            websites = websites.distinct(),
            note = note,
            photoUri = photoUri,
            groupId = groupId,
            isStarred = isStarred
        )
    }

    suspend fun getGroups(includeUngrouped: Boolean = true): List<Group> = withContext(Dispatchers.IO) {
        val groupsList = mutableListOf<Group>()

        if (includeUngrouped) {
            groupsList.add(
                Group(
                    id = GROUP_ID_UNGROUPED,
                    title = "未グループ",
                    accountName = "グループなし",
                    accountType = ""
                )
            )
        }

        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.ACCOUNT_NAME,
            ContactsContract.Groups.ACCOUNT_TYPE
        )
        val selection = "${ContactsContract.Groups.DELETED} = 0"

        val cursor = context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            selection,
            null,
            ContactsContract.Groups.TITLE + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Groups._ID)
            val titleIndex = it.getColumnIndex(ContactsContract.Groups.TITLE)
            val nameIndex = it.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)
            val typeIndex = it.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                if (!title.isNullOrBlank()) {
                    groupsList.add(
                        Group(
                            id = it.getString(idIndex),
                            title = title,
                            accountName = it.getString(nameIndex) ?: "",
                            accountType = it.getString(typeIndex) ?: ""
                        )
                    )
                }
            }
        }
        groupsList
    }

    /**
     * 新しい連絡先グループを作成
     */
    suspend fun createGroup(
        title: String,
        accountName: String? = null,
        accountType: String? = null
    ): Result<Group> = withContext(Dispatchers.IO) {
        if (title.trim().isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("グループ名を入力してください"))
        }
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Groups.TITLE, title.trim())
                put(ContactsContract.Groups.GROUP_VISIBLE, 1)
                if (!accountName.isNullOrBlank()) {
                    put(ContactsContract.Groups.ACCOUNT_NAME, accountName)
                    put(ContactsContract.Groups.ACCOUNT_TYPE, accountType ?: "com.google")
                }
            }
            val uri = context.contentResolver.insert(ContactsContract.Groups.CONTENT_URI, values)
            if (uri != null) {
                val newId = uri.lastPathSegment ?: ""
                Result.success(
                    Group(
                        id = newId,
                        title = title.trim(),
                        accountName = accountName ?: "",
                        accountType = accountType ?: ""
                    )
                )
            } else {
                Result.failure(Exception("グループの作成に失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 既存グループの名前を変更
     */
    suspend fun updateGroupTitle(groupId: String, newTitle: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (groupId == GROUP_ID_UNGROUPED) {
            return@withContext Result.failure(IllegalArgumentException("「未グループ」の名前は変更できません"))
        }
        if (newTitle.trim().isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("グループ名を入力してください"))
        }
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Groups.TITLE, newTitle.trim())
            }
            val count = context.contentResolver.update(
                ContactsContract.Groups.CONTENT_URI,
                values,
                "${ContactsContract.Groups._ID} = ?",
                arrayOf(groupId)
            )
            if (count > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("グループ名の変更に失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * グループを削除
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (groupId == GROUP_ID_UNGROUPED) {
            return@withContext Result.failure(IllegalArgumentException("「未グループ」は削除できません"))
        }
        try {
            val uri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId.toLongOrNull() ?: return@withContext Result.failure(Exception("無効なグループIDです")))
                .buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build()
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                Result.success(Unit)
            } else {
                val fallbackCount = context.contentResolver.delete(
                    ContactsContract.Groups.CONTENT_URI,
                    "${ContactsContract.Groups._ID} = ?",
                    arrayOf(groupId)
                )
                if (fallbackCount > 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("グループの削除に失敗しました"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getContactIdsInGroup(groupId: String): List<String> {
        val ids = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE} = ?"
        val selectionArgs = arrayOf(groupId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)

        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
            while (it.moveToNext()) {
                ids.add(it.getString(idIndex))
            }
        }
        return ids.distinct()
    }

    private fun getAllContactIdsWithAnyGroup(): List<String> {
        val ids = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)

        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
            while (it.moveToNext()) {
                ids.add(it.getString(idIndex))
            }
        }
        return ids.distinct()
    }

    private fun getRawContactId(contactId: String): Long? {
        val cursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
            } else null
        }
    }

    /**
     * Uriから画像を読み込み、JPEG byte array (512x512) に変換
     */
    fun processImageUriToByteArray(uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null
            if (original == null) return null

            val maxDim = 512
            val width = original.width
            val height = original.height
            val scale = (maxDim.toFloat() / maxOf(width, height)).coerceAtMost(1f)
            val scaled = Bitmap.createScaledBitmap(
                original,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 新しい連絡先を追加（複数電話・複数メール・会社・住所・Web・メモ・グループ・写真対応）
     */
    suspend fun addContact(
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
        photoBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = 0

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // Multiple Phone Numbers
            phoneNumbers.filter { it.value.isNotBlank() }.forEach { phone ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.value)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, if (phone.type != 0) phone.type else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
            }

            // Multiple Emails
            emails.filter { it.value.isNotBlank() }.forEach { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email.value)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, if (email.type != 0) email.type else ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .build()
                )
            }

            // Organization
            if (!company.isNullOrBlank() || !jobTitle.isNullOrBlank() || !department.isNullOrBlank()) {
                val orgOp = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                if (!company.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                if (!jobTitle.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                if (!department.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, department)
                ops.add(orgOp.build())
            }

            // Address
            if (!address.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                        .build()
                )
            }

            // Websites
            websites.filter { it.isNotBlank() }.forEach { url ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, url)
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
                        .build()
                )
            }

            // Note
            if (!note.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
                        .build()
                )
            }

            // Group
            if (!groupId.isNullOrBlank() && groupId != GROUP_ID_UNGROUPED) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                        .build()
                )
            }

            // Photo
            if (photoBytes != null && photoBytes.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 既存の連絡先情報を更新（複数電話・複数メール・会社・住所・Web・メモ・グループ・写真）
     */
    suspend fun updateContact(
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
        removePhoto: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rawContactId = getRawContactId(contactId)
                ?: return@withContext Result.failure(Exception("Raw contact ID が見つかりませんでした"))

            val ops = ArrayList<ContentProviderOperation>()

            // 1. 名前更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 2. 電話番号更新（一度全削除して再挿入）
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            phoneNumbers.filter { it.value.isNotBlank() }.forEach { phone ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.value)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, if (phone.type != 0) phone.type else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
            }

            // 3. メール更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            emails.filter { it.value.isNotBlank() }.forEach { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email.value)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, if (email.type != 0) email.type else ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .build()
                )
            }

            // 4. 会社・役職・部署更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (!company.isNullOrBlank() || !jobTitle.isNullOrBlank() || !department.isNullOrBlank()) {
                val orgOp = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                if (!company.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                if (!jobTitle.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                if (!department.isNullOrBlank()) orgOp.withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, department)
                ops.add(orgOp.build())
            }

            // 5. 住所更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (!address.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                        .build()
                )
            }

            // 6. Webサイト更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            websites.filter { it.isNotBlank() }.forEach { url ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, url)
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
                        .build()
                )
            }

            // 7. メモ更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (!note.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
                        .build()
                )
            }

            // 8. グループ更新
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (!groupId.isNullOrBlank() && groupId != GROUP_ID_UNGROUPED) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                        .build()
                )
            }

            // 9. 写真更新 / 削除
            if (removePhoto) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
            } else if (photoBytes != null && photoBytes.isNotEmpty()) {
                val photoCursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
                    null
                )
                val photoRowExists = (photoCursor?.count ?: 0) > 0
                photoCursor?.close()

                if (photoRowExists) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            )
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 連絡先を削除
     */
    suspend fun deleteContact(contactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            context.contentResolver.delete(uri, null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
