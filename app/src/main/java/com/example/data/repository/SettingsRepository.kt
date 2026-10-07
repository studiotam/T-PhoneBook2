package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ContactFieldDisplayConfig
import com.example.domain.model.ExcludedPrefix
import com.example.domain.model.PhonePrefix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        const val METHOD_SLIDE = "slide"
        const val METHOD_DIALOG = "dialog"
        const val METHOD_LONG_PRESS = "long_press"
        const val METHOD_COUNTDOWN = "countdown"
        const val METHOD_NONE = "none"

        const val SIM_MODE_ASK_EVERY_TIME = "ask_every_time" // 通話ごとに選択する（毎回確認）
        const val SIM_MODE_SIM1 = "sim1"                     // 常にSIM 1で発信
        const val SIM_MODE_SIM2 = "sim2"                     // 常にSIM 2で発信
    }

    private val PREF_PREFIX = stringPreferencesKey("prefix")
    private val PREF_PREFIXES_JSON = stringPreferencesKey("prefixes_json")
    private val PREF_SELECTED_PREFIX_ID = stringPreferencesKey("selected_prefix_id")
    private val PREF_SELECTED_PREFIX_ID_SIM1 = stringPreferencesKey("selected_prefix_id_sim1")
    private val PREF_SELECTED_PREFIX_ID_SIM2 = stringPreferencesKey("selected_prefix_id_sim2")
    private val PREF_SIM1_PHONE_NUMBER = stringPreferencesKey("sim1_phone_number")
    private val PREF_SIM2_PHONE_NUMBER = stringPreferencesKey("sim2_phone_number")
    private val PREF_EXCLUDED_PREFIXES_JSON = stringPreferencesKey("excluded_prefixes_json")
    private val PREF_CONFIRM_CALL = booleanPreferencesKey("confirm_call")
    private val PREF_CALL_CONFIRM_METHOD = stringPreferencesKey("call_confirm_method")
    private val PREF_DEFAULT_SIM_MODE = stringPreferencesKey("default_sim_mode")
    private val PREF_USE_RAKUTEN_LINK = booleanPreferencesKey("use_rakuten_link")
    private val PREF_THEME_MODE = stringPreferencesKey("theme_mode")
    private val PREF_USER_EMAIL = stringPreferencesKey("user_email")
    private val PREF_USER_NAME = stringPreferencesKey("user_name")
    private val PREF_USER_PHOTO = stringPreferencesKey("user_photo")
    private val PREF_OAUTH_TOKEN = stringPreferencesKey("oauth_token")
    private val PREF_OAUTH_TOKEN_TIME = stringPreferencesKey("oauth_token_time")
    private val PREF_CONTACTS_SELECTED_TAB = intPreferencesKey("contacts_selected_tab")
    private val PREF_CONTACTS_SELECTED_GROUP_ID = stringPreferencesKey("contacts_selected_group_id")
    private val PREF_REMEMBER_LAST_GROUP = booleanPreferencesKey("remember_last_group")
    private val PREF_HIDDEN_GROUP_IDS = androidx.datastore.preferences.core.stringSetPreferencesKey("hidden_group_ids")
    private val PREF_GROUP_ORDER_IDS = stringPreferencesKey("group_order_ids")
    private val PREF_FIELD_SHOW_COMPANY_MAIN = booleanPreferencesKey("field_show_company_main")
    private val PREF_FIELD_SHOW_EMAIL_MAIN = booleanPreferencesKey("field_show_email_main")
    private val PREF_FIELD_SHOW_ADDRESS_MAIN = booleanPreferencesKey("field_show_address_main")
    private val PREF_FIELD_SHOW_WEBSITES_MAIN = booleanPreferencesKey("field_show_websites_main")
    private val PREF_FIELD_SHOW_GROUP_MAIN = booleanPreferencesKey("field_show_group_main")
    private val PREF_FIELD_SHOW_NOTE_MAIN = booleanPreferencesKey("field_show_note_main")

    // 着信画面カスタマイズ設定
    private val PREF_INCOMING_CALL_ENABLED = booleanPreferencesKey("incoming_call_enabled")
    private val PREF_INCOMING_SHOW_PHOTO = booleanPreferencesKey("incoming_show_photo")
    private val PREF_INCOMING_SHOW_PHONE_NUMBER = booleanPreferencesKey("incoming_show_phone_number")
    private val PREF_INCOMING_SHOW_COMPANY = booleanPreferencesKey("incoming_show_company")
    private val PREF_INCOMING_SHOW_SPAM_WARNING = booleanPreferencesKey("incoming_show_spam_warning")
    private val PREF_INCOMING_SHOW_CALL_TIMER = booleanPreferencesKey("incoming_show_call_timer")
    private val PREF_INCOMING_SHOW_MUTE_BTN = booleanPreferencesKey("incoming_show_mute_btn")
    private val PREF_INCOMING_SHOW_SPEAKER_BTN = booleanPreferencesKey("incoming_show_speaker_btn")
    private val PREF_INCOMING_SHOW_KEYPAD_BTN = booleanPreferencesKey("incoming_show_keypad_btn")
    private val PREF_INCOMING_SHOW_RECORD_BTN = booleanPreferencesKey("incoming_show_record_btn")
    private val PREF_INCOMING_SHOW_QUICK_SMS = booleanPreferencesKey("incoming_show_quick_sms")
    private val PREF_INCOMING_THEME = stringPreferencesKey("incoming_theme")

    // 通話中画面カスタマイズ設定
    private val PREF_ACTIVE_CALL_ENABLED = booleanPreferencesKey("active_call_enabled")
    private val PREF_ACTIVE_SHOW_PHOTO = booleanPreferencesKey("active_show_photo")
    private val PREF_ACTIVE_SHOW_PHONE_NUMBER = booleanPreferencesKey("active_show_phone_number")
    private val PREF_ACTIVE_SHOW_COMPANY = booleanPreferencesKey("active_show_company")
    private val PREF_ACTIVE_SHOW_CALL_TIMER = booleanPreferencesKey("active_show_call_timer")
    private val PREF_ACTIVE_SHOW_MUTE_BTN = booleanPreferencesKey("active_show_mute_btn")
    private val PREF_ACTIVE_SHOW_SPEAKER_BTN = booleanPreferencesKey("active_show_speaker_btn")
    private val PREF_ACTIVE_SHOW_KEYPAD_BTN = booleanPreferencesKey("active_show_keypad_btn")
    private val PREF_ACTIVE_SHOW_RECORD_BTN = booleanPreferencesKey("active_show_record_btn")
    private val PREF_ACTIVE_SHOW_AUDIO_BADGE = booleanPreferencesKey("active_show_audio_badge")
    private val PREF_ACTIVE_THEME = stringPreferencesKey("active_theme")

    // 発信画面カスタマイズ設定
    private val PREF_OUTGOING_CALL_ENABLED = booleanPreferencesKey("outgoing_call_enabled")
    private val PREF_OUTGOING_SHOW_PHOTO = booleanPreferencesKey("outgoing_show_photo")
    private val PREF_OUTGOING_SHOW_PHONE_NUMBER = booleanPreferencesKey("outgoing_show_phone_number")
    private val PREF_OUTGOING_SHOW_COMPANY = booleanPreferencesKey("outgoing_show_company")
    private val PREF_OUTGOING_SHOW_PREFIX_INFO = booleanPreferencesKey("outgoing_show_prefix_info")
    private val PREF_OUTGOING_SHOW_SIM_INFO = booleanPreferencesKey("outgoing_show_sim_info")
    private val PREF_OUTGOING_SHOW_CALL_TIMER = booleanPreferencesKey("outgoing_show_call_timer")
    private val PREF_OUTGOING_SHOW_MUTE_BTN = booleanPreferencesKey("outgoing_show_mute_btn")
    private val PREF_OUTGOING_SHOW_SPEAKER_BTN = booleanPreferencesKey("outgoing_show_speaker_btn")
    private val PREF_OUTGOING_SHOW_KEYPAD_BTN = booleanPreferencesKey("outgoing_show_keypad_btn")
    private val PREF_OUTGOING_SHOW_RECORD_BTN = booleanPreferencesKey("outgoing_show_record_btn")
    private val PREF_OUTGOING_THEME = stringPreferencesKey("outgoing_theme")

    // 自動通話録音設定
    private val PREF_AUTO_RECORD_CALLS = booleanPreferencesKey("auto_record_calls")

    val autoRecordCallsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PREF_AUTO_RECORD_CALLS] ?: false
    }

    val incomingCallDisplayConfigFlow: Flow<com.example.domain.model.IncomingCallDisplayConfig> = context.dataStore.data.map { prefs ->
        com.example.domain.model.IncomingCallDisplayConfig(
            enabled = prefs[PREF_INCOMING_CALL_ENABLED] ?: true,
            showPhoto = prefs[PREF_INCOMING_SHOW_PHOTO] ?: true,
            showPhoneNumber = prefs[PREF_INCOMING_SHOW_PHONE_NUMBER] ?: true,
            showCompany = prefs[PREF_INCOMING_SHOW_COMPANY] ?: true,
            showSpamWarning = prefs[PREF_INCOMING_SHOW_SPAM_WARNING] ?: true,
            showCallTimer = prefs[PREF_INCOMING_SHOW_CALL_TIMER] ?: true,
            showMuteButton = prefs[PREF_INCOMING_SHOW_MUTE_BTN] ?: true,
            showSpeakerButton = prefs[PREF_INCOMING_SHOW_SPEAKER_BTN] ?: true,
            showKeypadButton = prefs[PREF_INCOMING_SHOW_KEYPAD_BTN] ?: true,
            showRecordButton = prefs[PREF_INCOMING_SHOW_RECORD_BTN] ?: true,
            showQuickSms = prefs[PREF_INCOMING_SHOW_QUICK_SMS] ?: true,
            theme = prefs[PREF_INCOMING_THEME] ?: "dark"
        )
    }

    val activeCallDisplayConfigFlow: Flow<com.example.domain.model.ActiveCallDisplayConfig> = context.dataStore.data.map { prefs ->
        com.example.domain.model.ActiveCallDisplayConfig(
            enabled = prefs[PREF_ACTIVE_CALL_ENABLED] ?: true,
            showPhoto = prefs[PREF_ACTIVE_SHOW_PHOTO] ?: true,
            showPhoneNumber = prefs[PREF_ACTIVE_SHOW_PHONE_NUMBER] ?: true,
            showCompany = prefs[PREF_ACTIVE_SHOW_COMPANY] ?: true,
            showCallTimer = prefs[PREF_ACTIVE_SHOW_CALL_TIMER] ?: true,
            showMuteButton = prefs[PREF_ACTIVE_SHOW_MUTE_BTN] ?: true,
            showSpeakerButton = prefs[PREF_ACTIVE_SHOW_SPEAKER_BTN] ?: true,
            showKeypadButton = prefs[PREF_ACTIVE_SHOW_KEYPAD_BTN] ?: true,
            showRecordButton = prefs[PREF_ACTIVE_SHOW_RECORD_BTN] ?: true,
            showAudioDeviceBadge = prefs[PREF_ACTIVE_SHOW_AUDIO_BADGE] ?: true,
            theme = prefs[PREF_ACTIVE_THEME] ?: "dark"
        )
    }

    val outgoingCallDisplayConfigFlow: Flow<com.example.domain.model.OutgoingCallDisplayConfig> = context.dataStore.data.map { prefs ->
        com.example.domain.model.OutgoingCallDisplayConfig(
            enabled = prefs[PREF_OUTGOING_CALL_ENABLED] ?: true,
            showPhoto = prefs[PREF_OUTGOING_SHOW_PHOTO] ?: true,
            showPhoneNumber = prefs[PREF_OUTGOING_SHOW_PHONE_NUMBER] ?: true,
            showCompany = prefs[PREF_OUTGOING_SHOW_COMPANY] ?: true,
            showPrefixInfo = prefs[PREF_OUTGOING_SHOW_PREFIX_INFO] ?: true,
            showSimInfo = prefs[PREF_OUTGOING_SHOW_SIM_INFO] ?: true,
            showCallTimer = prefs[PREF_OUTGOING_SHOW_CALL_TIMER] ?: true,
            showMuteButton = prefs[PREF_OUTGOING_SHOW_MUTE_BTN] ?: true,
            showSpeakerButton = prefs[PREF_OUTGOING_SHOW_SPEAKER_BTN] ?: true,
            showKeypadButton = prefs[PREF_OUTGOING_SHOW_KEYPAD_BTN] ?: true,
            showRecordButton = prefs[PREF_OUTGOING_SHOW_RECORD_BTN] ?: true,
            theme = prefs[PREF_OUTGOING_THEME] ?: "dark"
        )
    }

    val contactFieldDisplayConfigFlow: Flow<ContactFieldDisplayConfig> = context.dataStore.data.map { prefs ->
        ContactFieldDisplayConfig(
            showCompanyInMain = prefs[PREF_FIELD_SHOW_COMPANY_MAIN] ?: true,
            showEmailInMain = prefs[PREF_FIELD_SHOW_EMAIL_MAIN] ?: true,
            showAddressInMain = prefs[PREF_FIELD_SHOW_ADDRESS_MAIN] ?: false,
            showWebsitesInMain = prefs[PREF_FIELD_SHOW_WEBSITES_MAIN] ?: false,
            showGroupInMain = prefs[PREF_FIELD_SHOW_GROUP_MAIN] ?: true,
            showNoteInMain = prefs[PREF_FIELD_SHOW_NOTE_MAIN] ?: false
        )
    }

    val hiddenGroupIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PREF_HIDDEN_GROUP_IDS] ?: emptySet()
    }

    val groupOrderIdsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[PREF_GROUP_ORDER_IDS]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val contactsSelectedTabFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PREF_CONTACTS_SELECTED_TAB] ?: 0 // 0: グループ一覧 (デフォルト), 1: すべて
    }

    val contactsSelectedGroupIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PREF_CONTACTS_SELECTED_GROUP_ID]?.takeIf { it.isNotBlank() && it != "none" }
    }

    val rememberLastGroupFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PREF_REMEMBER_LAST_GROUP] ?: true
    }

    val prefixesFlow: Flow<List<PhonePrefix>> = context.dataStore.data.map { prefs ->
        parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX])
    }

    val selectedPrefixIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val id = prefs[PREF_SELECTED_PREFIX_ID]
        if (id.isNullOrBlank() || id == "none") null else id
    }

    val selectedPrefixIdSim1Flow: Flow<String?> = context.dataStore.data.map { prefs ->
        val id = prefs[PREF_SELECTED_PREFIX_ID_SIM1]
        if (id != null) {
            if (id.isBlank() || id == "none") null else id
        } else {
            // Fallback to global selected prefix if valid for SIM1
            val globalId = prefs[PREF_SELECTED_PREFIX_ID]
            val list = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX])
            val globalObj = list.firstOrNull { it.id == globalId }
            if (globalObj != null && (globalObj.targetSim == PhonePrefix.TARGET_SIM_BOTH || globalObj.targetSim == PhonePrefix.TARGET_SIM_1)) {
                globalId
            } else {
                null
            }
        }
    }

    val selectedPrefixIdSim2Flow: Flow<String?> = context.dataStore.data.map { prefs ->
        val id = prefs[PREF_SELECTED_PREFIX_ID_SIM2]
        if (id != null) {
            if (id.isBlank() || id == "none") null else id
        } else {
            // Fallback to global selected prefix if valid for SIM2
            val globalId = prefs[PREF_SELECTED_PREFIX_ID]
            val list = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX])
            val globalObj = list.firstOrNull { it.id == globalId }
            if (globalObj != null && (globalObj.targetSim == PhonePrefix.TARGET_SIM_BOTH || globalObj.targetSim == PhonePrefix.TARGET_SIM_2)) {
                globalId
            } else {
                null
            }
        }
    }

    val sim1PhoneNumberFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PREF_SIM1_PHONE_NUMBER]?.takeIf { it.isNotBlank() }
    }

    val sim2PhoneNumberFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PREF_SIM2_PHONE_NUMBER]?.takeIf { it.isNotBlank() }
    }

    val prefixFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val selectedId = prefs[PREF_SELECTED_PREFIX_ID]
        val list = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX])
        if (selectedId.isNullOrBlank() || selectedId == "none") {
            ""
        } else {
            list.firstOrNull { it.id == selectedId }?.prefix ?: ""
        }
    }

    val excludedPrefixesFlow: Flow<List<ExcludedPrefix>> = context.dataStore.data.map { prefs ->
        parseExcludedPrefixes(prefs[PREF_EXCLUDED_PREFIXES_JSON])
    }

    val callConfirmMethodFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PREF_CALL_CONFIRM_METHOD] ?: METHOD_SLIDE
    }

    val defaultSimModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PREF_DEFAULT_SIM_MODE] ?: SIM_MODE_ASK_EVERY_TIME
    }

    val confirmCallFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val method = prefs[PREF_CALL_CONFIRM_METHOD] ?: METHOD_SLIDE
        method != METHOD_NONE
    }

    val useRakutenLinkFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PREF_USE_RAKUTEN_LINK] ?: false
    }
    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[PREF_THEME_MODE] ?: "system" }
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { it[PREF_USER_EMAIL] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[PREF_USER_NAME] }
    val userPhotoFlow: Flow<String?> = context.dataStore.data.map { it[PREF_USER_PHOTO] }
    val oauthTokenFlow: Flow<String?> = context.dataStore.data.map { it[PREF_OAUTH_TOKEN] }
    val oauthTokenTimeFlow: Flow<String?> = context.dataStore.data.map { it[PREF_OAUTH_TOKEN_TIME] }

    private fun parsePrefixes(json: String?, legacyPrefix: String?): List<PhonePrefix> {
        if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<PhonePrefix>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val name = obj.optString("name", "")
                    val prefix = obj.optString("prefix", "")
                    val targetSim = obj.optString("targetSim", PhonePrefix.TARGET_SIM_BOTH)
                    if (name.isNotBlank() || prefix.isNotBlank()) {
                        list.add(PhonePrefix(id = id, name = name, prefix = prefix, targetSim = targetSim))
                    }
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Default list with legacy migration or initial default
        val initialList = mutableListOf<PhonePrefix>()
        if (!legacyPrefix.isNullOrBlank()) {
            initialList.add(PhonePrefix(id = "legacy_prefix", name = "非通知 (184)", prefix = legacyPrefix, targetSim = PhonePrefix.TARGET_SIM_BOTH))
        } else {
            initialList.add(PhonePrefix(id = "default_184", name = "非通知 (184)", prefix = "184", targetSim = PhonePrefix.TARGET_SIM_BOTH))
        }
        return initialList
    }

    private fun parseExcludedPrefixes(json: String?): List<ExcludedPrefix> {
        if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<ExcludedPrefix>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val pattern = obj.optString("pattern", "")
                    val description = obj.optString("description", "")
                    if (pattern.isNotBlank()) {
                        list.add(ExcludedPrefix(id = id, pattern = pattern, description = description))
                    }
                }
                return list
            } catch (_: Exception) {}
        }
        return getDefaultExcludedPrefixes()
    }

    private fun excludedPrefixesToJson(list: List<ExcludedPrefix>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("pattern", item.pattern)
            obj.put("description", item.description)
            array.put(obj)
        }
        return array.toString()
    }

    fun getDefaultExcludedPrefixes(): List<ExcludedPrefix> {
        return listOf(
            ExcludedPrefix(id = "ex_110", pattern = "110", description = "警察 (緊急通報)"),
            ExcludedPrefix(id = "ex_119", pattern = "119", description = "消防・救急 (緊急通報)"),
            ExcludedPrefix(id = "ex_118", pattern = "118", description = "海上保安庁 (緊急通報)"),
            ExcludedPrefix(id = "ex_0120", pattern = "0120", description = "フリーダイヤル (通話料無料)"),
            ExcludedPrefix(id = "ex_0800", pattern = "0800", description = "フリーコール (通話料無料)"),
            ExcludedPrefix(id = "ex_0570", pattern = "0570", description = "ナビダイヤル (プレフィックス非対応)"),
            ExcludedPrefix(id = "ex_0180", pattern = "0180", description = "テレドーム"),
            ExcludedPrefix(id = "ex_184", pattern = "184", description = "発信者番号非通知特番"),
            ExcludedPrefix(id = "ex_186", pattern = "186", description = "発信者番号通知特番"),
            ExcludedPrefix(id = "ex_171", pattern = "171", description = "災害用伝言ダイヤル"),
            ExcludedPrefix(id = "ex_177", pattern = "177", description = "天気予報特番"),
            ExcludedPrefix(id = "ex_911", pattern = "911", description = "緊急通報 (北米等)"),
            ExcludedPrefix(id = "ex_112", pattern = "112", description = "国際緊急通報 (EU/GSM)"),
            ExcludedPrefix(id = "ex_999", pattern = "999", description = "緊急通報 (英国等)")
        )
    }

    suspend fun addExcludedPrefix(pattern: String, description: String = "") {
        val newId = UUID.randomUUID().toString()
        val newItem = ExcludedPrefix(id = newId, pattern = pattern.trim(), description = description.trim())
        context.dataStore.edit { prefs ->
            val current = parseExcludedPrefixes(prefs[PREF_EXCLUDED_PREFIXES_JSON]).toMutableList()
            current.add(newItem)
            prefs[PREF_EXCLUDED_PREFIXES_JSON] = excludedPrefixesToJson(current)
        }
    }

    suspend fun updateExcludedPrefix(id: String, pattern: String, description: String = "") {
        context.dataStore.edit { prefs ->
            val current = parseExcludedPrefixes(prefs[PREF_EXCLUDED_PREFIXES_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(pattern = pattern.trim(), description = description.trim())
                prefs[PREF_EXCLUDED_PREFIXES_JSON] = excludedPrefixesToJson(current)
            }
        }
    }

    suspend fun deleteExcludedPrefix(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseExcludedPrefixes(prefs[PREF_EXCLUDED_PREFIXES_JSON]).toMutableList()
            current.removeAll { it.id == id }
            prefs[PREF_EXCLUDED_PREFIXES_JSON] = excludedPrefixesToJson(current)
        }
    }

    suspend fun resetDefaultExcludedPrefixes() {
        context.dataStore.edit { prefs ->
            prefs[PREF_EXCLUDED_PREFIXES_JSON] = excludedPrefixesToJson(getDefaultExcludedPrefixes())
        }
    }

    suspend fun clearAllExcludedPrefixes() {
        context.dataStore.edit { prefs ->
            prefs[PREF_EXCLUDED_PREFIXES_JSON] = excludedPrefixesToJson(emptyList())
        }
    }

    private fun prefixesToJson(list: List<PhonePrefix>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("prefix", item.prefix)
            obj.put("targetSim", item.targetSim)
            array.put(obj)
        }
        return array.toString()
    }

    suspend fun addPrefix(
        name: String,
        prefix: String,
        targetSim: String = PhonePrefix.TARGET_SIM_BOTH,
        setAsSelected: Boolean = false
    ) {
        val newId = UUID.randomUUID().toString()
        val newPrefix = PhonePrefix(
            id = newId,
            name = name.trim(),
            prefix = prefix.trim(),
            targetSim = targetSim
        )
        context.dataStore.edit { prefs ->
            val current = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX]).toMutableList()
            current.add(newPrefix)
            prefs[PREF_PREFIXES_JSON] = prefixesToJson(current)
            if (setAsSelected) {
                prefs[PREF_SELECTED_PREFIX_ID] = newId
                when (targetSim) {
                    PhonePrefix.TARGET_SIM_1 -> prefs[PREF_SELECTED_PREFIX_ID_SIM1] = newId
                    PhonePrefix.TARGET_SIM_2 -> prefs[PREF_SELECTED_PREFIX_ID_SIM2] = newId
                    else -> {
                        prefs[PREF_SELECTED_PREFIX_ID_SIM1] = newId
                        prefs[PREF_SELECTED_PREFIX_ID_SIM2] = newId
                    }
                }
            }
        }
    }

    suspend fun updatePrefix(
        id: String,
        name: String,
        prefix: String,
        targetSim: String = PhonePrefix.TARGET_SIM_BOTH
    ) {
        context.dataStore.edit { prefs ->
            val current = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX]).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(
                    name = name.trim(),
                    prefix = prefix.trim(),
                    targetSim = targetSim
                )
                prefs[PREF_PREFIXES_JSON] = prefixesToJson(current)
            }
        }
    }

    suspend fun deletePrefix(id: String) {
        context.dataStore.edit { prefs ->
            val current = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX]).toMutableList()
            current.removeAll { it.id == id }
            prefs[PREF_PREFIXES_JSON] = prefixesToJson(current)
            if (prefs[PREF_SELECTED_PREFIX_ID] == id) {
                prefs.remove(PREF_SELECTED_PREFIX_ID)
            }
            if (prefs[PREF_SELECTED_PREFIX_ID_SIM1] == id) {
                prefs.remove(PREF_SELECTED_PREFIX_ID_SIM1)
            }
            if (prefs[PREF_SELECTED_PREFIX_ID_SIM2] == id) {
                prefs.remove(PREF_SELECTED_PREFIX_ID_SIM2)
            }
        }
    }

    suspend fun setSelectedPrefixId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null || id == "none") {
                prefs[PREF_SELECTED_PREFIX_ID] = "none"
            } else {
                prefs[PREF_SELECTED_PREFIX_ID] = id
            }
        }
    }

    suspend fun setSelectedPrefixIdForSim(simSlot: Int, id: String?) {
        context.dataStore.edit { prefs ->
            val key = if (simSlot == 0) PREF_SELECTED_PREFIX_ID_SIM1 else PREF_SELECTED_PREFIX_ID_SIM2
            if (id == null || id == "none") {
                prefs[key] = "none"
            } else {
                prefs[key] = id
            }
        }
    }

    suspend fun saveGoogleUser(email: String, name: String?, photoUrl: String?, token: String? = null, tokenTime: String? = null) {
        context.dataStore.edit {
            it[PREF_USER_EMAIL] = email
            if (name != null) it[PREF_USER_NAME] = name else it.remove(PREF_USER_NAME)
            if (photoUrl != null) it[PREF_USER_PHOTO] = photoUrl else it.remove(PREF_USER_PHOTO)
            if (token != null) it[PREF_OAUTH_TOKEN] = token else it.remove(PREF_OAUTH_TOKEN)
            if (tokenTime != null) it[PREF_OAUTH_TOKEN_TIME] = tokenTime else it.remove(PREF_OAUTH_TOKEN_TIME)
        }
    }

    suspend fun clearGoogleUser() {
        context.dataStore.edit {
            it.remove(PREF_USER_EMAIL)
            it.remove(PREF_USER_NAME)
            it.remove(PREF_USER_PHOTO)
            it.remove(PREF_OAUTH_TOKEN)
            it.remove(PREF_OAUTH_TOKEN_TIME)
        }
    }

    suspend fun setPrefix(prefix: String) {
        context.dataStore.edit { prefs ->
            prefs[PREF_PREFIX] = prefix
            val current = parsePrefixes(prefs[PREF_PREFIXES_JSON], prefs[PREF_PREFIX]).toMutableList()
            if (current.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                current.add(PhonePrefix(id = newId, name = "カスタム", prefix = prefix))
                prefs[PREF_SELECTED_PREFIX_ID] = newId
            } else {
                val selectedId = prefs[PREF_SELECTED_PREFIX_ID]
                val index = current.indexOfFirst { it.id == selectedId }
                if (index != -1) {
                    current[index] = current[index].copy(prefix = prefix)
                } else {
                    current[0] = current[0].copy(prefix = prefix)
                    prefs[PREF_SELECTED_PREFIX_ID] = current[0].id
                }
            }
            prefs[PREF_PREFIXES_JSON] = prefixesToJson(current)
        }
    }

    suspend fun setCallConfirmMethod(method: String) {
        context.dataStore.edit {
            it[PREF_CALL_CONFIRM_METHOD] = method
            it[PREF_CONFIRM_CALL] = (method != METHOD_NONE)
        }
    }

    suspend fun setDefaultSimMode(mode: String) {
        context.dataStore.edit {
            it[PREF_DEFAULT_SIM_MODE] = mode
        }
    }

    suspend fun setUseRakutenLink(enabled: Boolean) {
        context.dataStore.edit {
            it[PREF_USE_RAKUTEN_LINK] = enabled
        }
    }

    suspend fun setConfirmCall(confirm: Boolean) {
        context.dataStore.edit {
            it[PREF_CONFIRM_CALL] = confirm
            if (!confirm) {
                it[PREF_CALL_CONFIRM_METHOD] = METHOD_NONE
            } else {
                val currentMethod = it[PREF_CALL_CONFIRM_METHOD]
                if (currentMethod == null || currentMethod == METHOD_NONE) {
                    it[PREF_CALL_CONFIRM_METHOD] = METHOD_SLIDE
                }
            }
        }
    }

    suspend fun setSimPhoneNumber(simSlot: Int, number: String?) {
        context.dataStore.edit { prefs ->
            val key = if (simSlot == 0) PREF_SIM1_PHONE_NUMBER else PREF_SIM2_PHONE_NUMBER
            if (number.isNullOrBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = number.trim()
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[PREF_THEME_MODE] = mode }
    }

    suspend fun setGroupVisibility(groupId: String, isVisible: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[PREF_HIDDEN_GROUP_IDS] ?: emptySet()).toMutableSet()
            if (isVisible) {
                current.remove(groupId)
            } else {
                current.add(groupId)
            }
            prefs[PREF_HIDDEN_GROUP_IDS] = current
        }
    }

    suspend fun setAllGroupsVisibility(groupIds: List<String>, isVisible: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[PREF_HIDDEN_GROUP_IDS] ?: emptySet()).toMutableSet()
            if (isVisible) {
                current.removeAll(groupIds.toSet())
            } else {
                current.addAll(groupIds)
            }
            prefs[PREF_HIDDEN_GROUP_IDS] = current
        }
    }

    suspend fun resetGroupVisibility() {
        context.dataStore.edit { prefs ->
            prefs.remove(PREF_HIDDEN_GROUP_IDS)
        }
    }

    suspend fun setGroupOrderIds(orderIds: List<String>) {
        context.dataStore.edit { prefs ->
            val array = JSONArray()
            orderIds.forEach { array.put(it) }
            prefs[PREF_GROUP_ORDER_IDS] = array.toString()
        }
    }

    suspend fun resetGroupOrder() {
        context.dataStore.edit { prefs ->
            prefs.remove(PREF_GROUP_ORDER_IDS)
        }
    }

    suspend fun setContactsSelectedTab(tabIndex: Int) {
        context.dataStore.edit { it[PREF_CONTACTS_SELECTED_TAB] = tabIndex }
    }

    suspend fun setContactsSelectedGroupId(groupId: String?) {
        context.dataStore.edit {
            if (groupId.isNullOrBlank()) {
                it.remove(PREF_CONTACTS_SELECTED_GROUP_ID)
            } else {
                it[PREF_CONTACTS_SELECTED_GROUP_ID] = groupId
            }
        }
    }

    suspend fun setRememberLastGroup(enabled: Boolean) {
        context.dataStore.edit {
            it[PREF_REMEMBER_LAST_GROUP] = enabled
        }
    }

    suspend fun setContactFieldDisplayConfig(config: ContactFieldDisplayConfig) {
        context.dataStore.edit { prefs ->
            prefs[PREF_FIELD_SHOW_COMPANY_MAIN] = config.showCompanyInMain
            prefs[PREF_FIELD_SHOW_EMAIL_MAIN] = config.showEmailInMain
            prefs[PREF_FIELD_SHOW_ADDRESS_MAIN] = config.showAddressInMain
            prefs[PREF_FIELD_SHOW_WEBSITES_MAIN] = config.showWebsitesInMain
            prefs[PREF_FIELD_SHOW_GROUP_MAIN] = config.showGroupInMain
            prefs[PREF_FIELD_SHOW_NOTE_MAIN] = config.showNoteInMain
        }
    }

    suspend fun setAutoRecordCalls(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PREF_AUTO_RECORD_CALLS] = enabled
        }
    }

    suspend fun setIncomingCallDisplayConfig(config: com.example.domain.model.IncomingCallDisplayConfig) {
        context.dataStore.edit { prefs ->
            prefs[PREF_INCOMING_CALL_ENABLED] = config.enabled
            prefs[PREF_INCOMING_SHOW_PHOTO] = config.showPhoto
            prefs[PREF_INCOMING_SHOW_PHONE_NUMBER] = config.showPhoneNumber
            prefs[PREF_INCOMING_SHOW_COMPANY] = config.showCompany
            prefs[PREF_INCOMING_SHOW_SPAM_WARNING] = config.showSpamWarning
            prefs[PREF_INCOMING_SHOW_CALL_TIMER] = config.showCallTimer
            prefs[PREF_INCOMING_SHOW_MUTE_BTN] = config.showMuteButton
            prefs[PREF_INCOMING_SHOW_SPEAKER_BTN] = config.showSpeakerButton
            prefs[PREF_INCOMING_SHOW_KEYPAD_BTN] = config.showKeypadButton
            prefs[PREF_INCOMING_SHOW_RECORD_BTN] = config.showRecordButton
            prefs[PREF_INCOMING_SHOW_QUICK_SMS] = config.showQuickSms
            prefs[PREF_INCOMING_THEME] = config.theme
        }
    }

    suspend fun resetIncomingCallDisplayConfig() {
        setIncomingCallDisplayConfig(com.example.domain.model.IncomingCallDisplayConfig())
    }

    suspend fun setActiveCallDisplayConfig(config: com.example.domain.model.ActiveCallDisplayConfig) {
        context.dataStore.edit { prefs ->
            prefs[PREF_ACTIVE_CALL_ENABLED] = config.enabled
            prefs[PREF_ACTIVE_SHOW_PHOTO] = config.showPhoto
            prefs[PREF_ACTIVE_SHOW_PHONE_NUMBER] = config.showPhoneNumber
            prefs[PREF_ACTIVE_SHOW_COMPANY] = config.showCompany
            prefs[PREF_ACTIVE_SHOW_CALL_TIMER] = config.showCallTimer
            prefs[PREF_ACTIVE_SHOW_MUTE_BTN] = config.showMuteButton
            prefs[PREF_ACTIVE_SHOW_SPEAKER_BTN] = config.showSpeakerButton
            prefs[PREF_ACTIVE_SHOW_KEYPAD_BTN] = config.showKeypadButton
            prefs[PREF_ACTIVE_SHOW_RECORD_BTN] = config.showRecordButton
            prefs[PREF_ACTIVE_SHOW_AUDIO_BADGE] = config.showAudioDeviceBadge
            prefs[PREF_ACTIVE_THEME] = config.theme
        }
    }

    suspend fun resetActiveCallDisplayConfig() {
        setActiveCallDisplayConfig(com.example.domain.model.ActiveCallDisplayConfig())
    }

    suspend fun setOutgoingCallDisplayConfig(config: com.example.domain.model.OutgoingCallDisplayConfig) {
        context.dataStore.edit { prefs ->
            prefs[PREF_OUTGOING_CALL_ENABLED] = config.enabled
            prefs[PREF_OUTGOING_SHOW_PHOTO] = config.showPhoto
            prefs[PREF_OUTGOING_SHOW_PHONE_NUMBER] = config.showPhoneNumber
            prefs[PREF_OUTGOING_SHOW_COMPANY] = config.showCompany
            prefs[PREF_OUTGOING_SHOW_PREFIX_INFO] = config.showPrefixInfo
            prefs[PREF_OUTGOING_SHOW_SIM_INFO] = config.showSimInfo
            prefs[PREF_OUTGOING_SHOW_CALL_TIMER] = config.showCallTimer
            prefs[PREF_OUTGOING_SHOW_MUTE_BTN] = config.showMuteButton
            prefs[PREF_OUTGOING_SHOW_SPEAKER_BTN] = config.showSpeakerButton
            prefs[PREF_OUTGOING_SHOW_KEYPAD_BTN] = config.showKeypadButton
            prefs[PREF_OUTGOING_SHOW_RECORD_BTN] = config.showRecordButton
            prefs[PREF_OUTGOING_THEME] = config.theme
        }
    }

    suspend fun resetOutgoingCallDisplayConfig() {
        setOutgoingCallDisplayConfig(com.example.domain.model.OutgoingCallDisplayConfig())
    }

    /**
     * すべての設定をJSON文字列としてエクスポート
     */
    suspend fun exportAllSettingsJson(): String {
        var resultJson = "{}"
        context.dataStore.edit { prefs ->
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            prefs[PREF_PREFIXES_JSON]?.let { root.put("prefixes_json", it) }
            prefs[PREF_SELECTED_PREFIX_ID]?.let { root.put("selected_prefix_id", it) }
            prefs[PREF_SELECTED_PREFIX_ID_SIM1]?.let { root.put("selected_prefix_id_sim1", it) }
            prefs[PREF_SELECTED_PREFIX_ID_SIM2]?.let { root.put("selected_prefix_id_sim2", it) }
            prefs[PREF_SIM1_PHONE_NUMBER]?.let { root.put("sim1_phone_number", it) }
            prefs[PREF_SIM2_PHONE_NUMBER]?.let { root.put("sim2_phone_number", it) }
            prefs[PREF_EXCLUDED_PREFIXES_JSON]?.let { root.put("excluded_prefixes_json", it) }
            prefs[PREF_CALL_CONFIRM_METHOD]?.let { root.put("call_confirm_method", it) }
            prefs[PREF_DEFAULT_SIM_MODE]?.let { root.put("default_sim_mode", it) }
            prefs[PREF_THEME_MODE]?.let { root.put("theme_mode", it) }
            root.put("auto_record_calls", prefs[PREF_AUTO_RECORD_CALLS] ?: false)

            // Field display configs
            root.put("field_show_company_main", prefs[PREF_FIELD_SHOW_COMPANY_MAIN] ?: true)
            root.put("field_show_email_main", prefs[PREF_FIELD_SHOW_EMAIL_MAIN] ?: true)
            root.put("field_show_address_main", prefs[PREF_FIELD_SHOW_ADDRESS_MAIN] ?: false)
            root.put("field_show_websites_main", prefs[PREF_FIELD_SHOW_WEBSITES_MAIN] ?: false)
            root.put("field_show_group_main", prefs[PREF_FIELD_SHOW_GROUP_MAIN] ?: true)
            root.put("field_show_note_main", prefs[PREF_FIELD_SHOW_NOTE_MAIN] ?: false)

            // Incoming call configs
            root.put("incoming_call_enabled", prefs[PREF_INCOMING_CALL_ENABLED] ?: true)
            root.put("incoming_show_photo", prefs[PREF_INCOMING_SHOW_PHOTO] ?: true)
            root.put("incoming_show_phone_number", prefs[PREF_INCOMING_SHOW_PHONE_NUMBER] ?: true)
            root.put("incoming_show_company", prefs[PREF_INCOMING_SHOW_COMPANY] ?: true)
            root.put("incoming_show_spam_warning", prefs[PREF_INCOMING_SHOW_SPAM_WARNING] ?: true)
            root.put("incoming_show_call_timer", prefs[PREF_INCOMING_SHOW_CALL_TIMER] ?: true)
            root.put("incoming_show_mute_btn", prefs[PREF_INCOMING_SHOW_MUTE_BTN] ?: true)
            root.put("incoming_show_speaker_btn", prefs[PREF_INCOMING_SHOW_SPEAKER_BTN] ?: true)
            root.put("incoming_show_keypad_btn", prefs[PREF_INCOMING_SHOW_KEYPAD_BTN] ?: true)
            root.put("incoming_show_record_btn", prefs[PREF_INCOMING_SHOW_RECORD_BTN] ?: true)
            root.put("incoming_show_quick_sms", prefs[PREF_INCOMING_SHOW_QUICK_SMS] ?: true)
            prefs[PREF_INCOMING_THEME]?.let { root.put("incoming_theme", it) }

            // Outgoing call configs
            root.put("outgoing_call_enabled", prefs[PREF_OUTGOING_CALL_ENABLED] ?: true)
            root.put("outgoing_show_photo", prefs[PREF_OUTGOING_SHOW_PHOTO] ?: true)
            root.put("outgoing_show_phone_number", prefs[PREF_OUTGOING_SHOW_PHONE_NUMBER] ?: true)
            root.put("outgoing_show_company", prefs[PREF_OUTGOING_SHOW_COMPANY] ?: true)
            root.put("outgoing_show_prefix_info", prefs[PREF_OUTGOING_SHOW_PREFIX_INFO] ?: true)
            root.put("outgoing_show_sim_info", prefs[PREF_OUTGOING_SHOW_SIM_INFO] ?: true)
            root.put("outgoing_show_call_timer", prefs[PREF_OUTGOING_SHOW_CALL_TIMER] ?: true)
            root.put("outgoing_show_mute_btn", prefs[PREF_OUTGOING_SHOW_MUTE_BTN] ?: true)
            root.put("outgoing_show_speaker_btn", prefs[PREF_OUTGOING_SHOW_SPEAKER_BTN] ?: true)
            root.put("outgoing_show_keypad_btn", prefs[PREF_OUTGOING_SHOW_KEYPAD_BTN] ?: true)
            root.put("outgoing_show_record_btn", prefs[PREF_OUTGOING_SHOW_RECORD_BTN] ?: true)
            prefs[PREF_OUTGOING_THEME]?.let { root.put("outgoing_theme", it) }

            val hiddenGroups = prefs[PREF_HIDDEN_GROUP_IDS] ?: emptySet()
            if (hiddenGroups.isNotEmpty()) {
                val array = JSONArray()
                hiddenGroups.forEach { array.put(it) }
                root.put("hidden_group_ids", array)
            }

            prefs[PREF_GROUP_ORDER_IDS]?.let { root.put("group_order_ids", it) }

            resultJson = root.toString(2)
        }
        return resultJson
    }

    /**
     * JSON文字列から設定をインポート・復元
     */
    suspend fun restoreSettingsFromJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            context.dataStore.edit { prefs ->
                if (root.has("prefixes_json")) {
                    prefs[PREF_PREFIXES_JSON] = root.getString("prefixes_json")
                }
                if (root.has("selected_prefix_id")) {
                    prefs[PREF_SELECTED_PREFIX_ID] = root.getString("selected_prefix_id")
                }
                if (root.has("selected_prefix_id_sim1")) {
                    prefs[PREF_SELECTED_PREFIX_ID_SIM1] = root.getString("selected_prefix_id_sim1")
                }
                if (root.has("selected_prefix_id_sim2")) {
                    prefs[PREF_SELECTED_PREFIX_ID_SIM2] = root.getString("selected_prefix_id_sim2")
                }
                if (root.has("sim1_phone_number")) {
                    prefs[PREF_SIM1_PHONE_NUMBER] = root.getString("sim1_phone_number")
                }
                if (root.has("sim2_phone_number")) {
                    prefs[PREF_SIM2_PHONE_NUMBER] = root.getString("sim2_phone_number")
                }
                if (root.has("excluded_prefixes_json")) {
                    prefs[PREF_EXCLUDED_PREFIXES_JSON] = root.getString("excluded_prefixes_json")
                }
                if (root.has("call_confirm_method")) {
                    prefs[PREF_CALL_CONFIRM_METHOD] = root.getString("call_confirm_method")
                }
                if (root.has("default_sim_mode")) {
                    prefs[PREF_DEFAULT_SIM_MODE] = root.getString("default_sim_mode")
                }
                if (root.has("theme_mode")) {
                    prefs[PREF_THEME_MODE] = root.getString("theme_mode")
                }
                if (root.has("auto_record_calls")) {
                    prefs[PREF_AUTO_RECORD_CALLS] = root.getBoolean("auto_record_calls")
                }
                if (root.has("field_show_company_main")) {
                    prefs[PREF_FIELD_SHOW_COMPANY_MAIN] = root.getBoolean("field_show_company_main")
                }
                if (root.has("field_show_email_main")) {
                    prefs[PREF_FIELD_SHOW_EMAIL_MAIN] = root.getBoolean("field_show_email_main")
                }
                if (root.has("field_show_address_main")) {
                    prefs[PREF_FIELD_SHOW_ADDRESS_MAIN] = root.getBoolean("field_show_address_main")
                }
                if (root.has("field_show_websites_main")) {
                    prefs[PREF_FIELD_SHOW_WEBSITES_MAIN] = root.getBoolean("field_show_websites_main")
                }
                if (root.has("field_show_group_main")) {
                    prefs[PREF_FIELD_SHOW_GROUP_MAIN] = root.getBoolean("field_show_group_main")
                }
                if (root.has("field_show_note_main")) {
                    prefs[PREF_FIELD_SHOW_NOTE_MAIN] = root.getBoolean("field_show_note_main")
                }

                // Incoming call configs restore
                if (root.has("incoming_call_enabled")) {
                    prefs[PREF_INCOMING_CALL_ENABLED] = root.getBoolean("incoming_call_enabled")
                }
                if (root.has("incoming_show_photo")) {
                    prefs[PREF_INCOMING_SHOW_PHOTO] = root.getBoolean("incoming_show_photo")
                }
                if (root.has("incoming_show_phone_number")) {
                    prefs[PREF_INCOMING_SHOW_PHONE_NUMBER] = root.getBoolean("incoming_show_phone_number")
                }
                if (root.has("incoming_show_company")) {
                    prefs[PREF_INCOMING_SHOW_COMPANY] = root.getBoolean("incoming_show_company")
                }
                if (root.has("incoming_show_spam_warning")) {
                    prefs[PREF_INCOMING_SHOW_SPAM_WARNING] = root.getBoolean("incoming_show_spam_warning")
                }
                if (root.has("incoming_show_call_timer")) {
                    prefs[PREF_INCOMING_SHOW_CALL_TIMER] = root.getBoolean("incoming_show_call_timer")
                }
                if (root.has("incoming_show_mute_btn")) {
                    prefs[PREF_INCOMING_SHOW_MUTE_BTN] = root.getBoolean("incoming_show_mute_btn")
                }
                if (root.has("incoming_show_speaker_btn")) {
                    prefs[PREF_INCOMING_SHOW_SPEAKER_BTN] = root.getBoolean("incoming_show_speaker_btn")
                }
                if (root.has("incoming_show_keypad_btn")) {
                    prefs[PREF_INCOMING_SHOW_KEYPAD_BTN] = root.getBoolean("incoming_show_keypad_btn")
                }
                if (root.has("incoming_show_record_btn")) {
                    prefs[PREF_INCOMING_SHOW_RECORD_BTN] = root.getBoolean("incoming_show_record_btn")
                }
                if (root.has("incoming_show_quick_sms")) {
                    prefs[PREF_INCOMING_SHOW_QUICK_SMS] = root.getBoolean("incoming_show_quick_sms")
                }
                if (root.has("incoming_theme")) {
                    prefs[PREF_INCOMING_THEME] = root.getString("incoming_theme")
                }

                // Outgoing call configs restore
                if (root.has("outgoing_call_enabled")) {
                    prefs[PREF_OUTGOING_CALL_ENABLED] = root.getBoolean("outgoing_call_enabled")
                }
                if (root.has("outgoing_show_photo")) {
                    prefs[PREF_OUTGOING_SHOW_PHOTO] = root.getBoolean("outgoing_show_photo")
                }
                if (root.has("outgoing_show_phone_number")) {
                    prefs[PREF_OUTGOING_SHOW_PHONE_NUMBER] = root.getBoolean("outgoing_show_phone_number")
                }
                if (root.has("outgoing_show_company")) {
                    prefs[PREF_OUTGOING_SHOW_COMPANY] = root.getBoolean("outgoing_show_company")
                }
                if (root.has("outgoing_show_prefix_info")) {
                    prefs[PREF_OUTGOING_SHOW_PREFIX_INFO] = root.getBoolean("outgoing_show_prefix_info")
                }
                if (root.has("outgoing_show_sim_info")) {
                    prefs[PREF_OUTGOING_SHOW_SIM_INFO] = root.getBoolean("outgoing_show_sim_info")
                }
                if (root.has("outgoing_show_call_timer")) {
                    prefs[PREF_OUTGOING_SHOW_CALL_TIMER] = root.getBoolean("outgoing_show_call_timer")
                }
                if (root.has("outgoing_show_mute_btn")) {
                    prefs[PREF_OUTGOING_SHOW_MUTE_BTN] = root.getBoolean("outgoing_show_mute_btn")
                }
                if (root.has("outgoing_show_speaker_btn")) {
                    prefs[PREF_OUTGOING_SHOW_SPEAKER_BTN] = root.getBoolean("outgoing_show_speaker_btn")
                }
                if (root.has("outgoing_show_keypad_btn")) {
                    prefs[PREF_OUTGOING_SHOW_KEYPAD_BTN] = root.getBoolean("outgoing_show_keypad_btn")
                }
                if (root.has("outgoing_show_record_btn")) {
                    prefs[PREF_OUTGOING_SHOW_RECORD_BTN] = root.getBoolean("outgoing_show_record_btn")
                }
                if (root.has("outgoing_theme")) {
                    prefs[PREF_OUTGOING_THEME] = root.getString("outgoing_theme")
                }

                if (root.has("hidden_group_ids")) {
                    val array = root.getJSONArray("hidden_group_ids")
                    val set = mutableSetOf<String>()
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                    prefs[PREF_HIDDEN_GROUP_IDS] = set
                }

                if (root.has("group_order_ids")) {
                    prefs[PREF_GROUP_ORDER_IDS] = root.getString("group_order_ids")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
