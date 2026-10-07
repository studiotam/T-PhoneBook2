package com.example.domain.model

enum class CallType {
    OUTGOING,
    INCOMING,
    MISSED,
    REJECTED,
    OTHER
}

data class CallRecord(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long,
    val durationSeconds: Long = 0,
    val callType: CallType = CallType.OUTGOING,
    val simDisplayName: String? = null
)
