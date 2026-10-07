package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 迷惑電話番号テーブル
 */
@Entity(tableName = "spam_numbers")
data class SpamNumberEntity(
    @PrimaryKey
    val number: String, // E.164規格 e.g. "+815012345678"
    @ColumnInfo(name = "calling_code")
    val callingCode: String = "81", // 国番号プレフィックス
    val category: String, // カテゴリ (例: "Persistent Telemarketing", "Scam", "Robocall")
    @ColumnInfo(name = "target_entity")
    val targetEntity: String? = null, // 対象事業者・名目
    @ColumnInfo(name = "risk_score")
    val riskScore: Int = 1, // 危険度スコア (1〜5)
    @ColumnInfo(name = "last_confirmed_date")
    val lastConfirmedDate: String? = null, // 最終確認日
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis() // 更新日時
)
