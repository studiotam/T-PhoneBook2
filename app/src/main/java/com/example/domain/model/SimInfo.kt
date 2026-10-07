package com.example.domain.model

/**
 * SIM情報モデル
 */
data class SimInfo(
    val slotIndex: Int,          // 0: SIM 1, 1: SIM 2
    val subscriptionId: Int,     // Android subscriptionId
    val displayName: String,     // e.g. "SIM 1"
    val carrierName: String,     // e.g. "NTT DOCOMO", "au", "SoftBank", "Rakuten"
    val phoneNumber: String? = null,
    val isActive: Boolean = true // OS上でアクティブ（有効）かどうか
)

