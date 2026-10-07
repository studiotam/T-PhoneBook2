package com.example.domain.model

import java.util.UUID

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val isStarred: Boolean
)

data class LabeledValue(
    val id: String = UUID.randomUUID().toString(),
    val value: String,
    val type: Int = 0,
    val label: String = ""
)

data class ContactDetail(
    val id: String,
    val name: String,
    val phoneNumbers: List<LabeledValue> = emptyList(),
    val emails: List<LabeledValue> = emptyList(),
    val company: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val address: String? = null,
    val websites: List<String> = emptyList(),
    val note: String? = null,
    val photoUri: String? = null,
    val groupId: String? = null,
    val isStarred: Boolean = false
) {
    val phoneNumber: String
        get() = phoneNumbers.firstOrNull()?.value ?: ""

    val email: String?
        get() = emails.firstOrNull()?.value
}

data class ContactFieldDisplayConfig(
    val showCompanyInMain: Boolean = true,
    val showEmailInMain: Boolean = true,
    val showAddressInMain: Boolean = false,
    val showWebsitesInMain: Boolean = false,
    val showGroupInMain: Boolean = true,
    val showNoteInMain: Boolean = false
)

data class IncomingCallDisplayConfig(
    val enabled: Boolean = true,
    val showPhoto: Boolean = true,
    val showPhoneNumber: Boolean = true,
    val showCompany: Boolean = true,
    val showSpamWarning: Boolean = true,
    val showCallTimer: Boolean = true,
    val showMuteButton: Boolean = true,
    val showSpeakerButton: Boolean = true,
    val showKeypadButton: Boolean = true,
    val showRecordButton: Boolean = true,
    val showQuickSms: Boolean = true,
    val theme: String = "dark"
)

data class ActiveCallDisplayConfig(
    val enabled: Boolean = true,
    val showPhoto: Boolean = true,
    val showPhoneNumber: Boolean = true,
    val showCompany: Boolean = true,
    val showCallTimer: Boolean = true,
    val showMuteButton: Boolean = true,
    val showSpeakerButton: Boolean = true,
    val showKeypadButton: Boolean = true,
    val showRecordButton: Boolean = true,
    val showAudioDeviceBadge: Boolean = true,
    val theme: String = "dark"
)

data class OutgoingCallDisplayConfig(
    val enabled: Boolean = true,
    val showPhoto: Boolean = true,
    val showPhoneNumber: Boolean = true,
    val showCompany: Boolean = true,
    val showPrefixInfo: Boolean = true,
    val showSimInfo: Boolean = true,
    val showCallTimer: Boolean = true,
    val showMuteButton: Boolean = true,
    val showSpeakerButton: Boolean = true,
    val showKeypadButton: Boolean = true,
    val showRecordButton: Boolean = true,
    val theme: String = "dark"
)

data class Group(
    val id: String,
    val title: String,
    val accountName: String,
    val accountType: String
)

data class PhonePrefix(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prefix: String,
    val targetSim: String = TARGET_SIM_BOTH
) {
    companion object {
        const val TARGET_SIM_BOTH = "both"
        const val TARGET_SIM_1 = "sim1"
        const val TARGET_SIM_2 = "sim2"

        fun getSimLabel(targetSim: String): String {
            return when (targetSim) {
                TARGET_SIM_1 -> "SIM 1専用"
                TARGET_SIM_2 -> "SIM 2専用"
                else -> "SIM共通"
            }
        }
    }
}

data class ExcludedPrefix(
    val id: String = UUID.randomUUID().toString(),
    val pattern: String,
    val description: String = ""
)
