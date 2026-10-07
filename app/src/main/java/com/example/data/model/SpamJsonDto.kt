package com.example.data.model

import com.example.data.local.SpamNumberEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class SpamResponseDto(
    @Json(name = "calling_code")
    val callingCode: String? = null,
    @Json(name = "country_iso")
    val countryIso: String? = null,
    @Json(name = "date")
    val date: String? = null,
    @Json(name = "ttl_days")
    val ttlDays: Int? = null,
    @Json(name = "added")
    val added: List<SpamItemDto>? = null,
    @Json(name = "revoked")
    val revoked: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SpamItemDto(
    @Json(name = "number")
    val number: String,
    @Json(name = "category")
    val category: String,
    @Json(name = "target_entity")
    val targetEntity: String? = null,
    @Json(name = "risk_score")
    val riskScore: Int = 1
)

/**
 * フォールバックパーサー（JSON形式の揺らぎに対応）
 */
object SpamJsonParser {
    fun parse(jsonString: String): SpamResponseDto {
        val root = JSONObject(jsonString)
        val callingCode = root.optString("calling_code", "81")
        val countryIso = root.optString("country_iso", "JP")
        val date = root.optString("date", "")
        val ttlDays = root.optInt("ttl_days", 90)

        val addedList = mutableListOf<SpamItemDto>()
        val addedArray = root.optJSONArray("added")
        if (addedArray != null) {
            for (i in 0 until addedArray.length()) {
                val obj = addedArray.optJSONObject(i)
                if (obj != null) {
                    val rawNum = obj.optString("number", "").trim()
                    val category = obj.optString("category", "迷惑電話")
                    val targetEntity = if (obj.has("target_entity") && !obj.isNull("target_entity")) {
                        obj.optString("target_entity")
                    } else {
                        null
                    }
                    val riskScore = obj.optInt("risk_score", 1)
                    if (rawNum.isNotEmpty()) {
                        addedList.add(
                            SpamItemDto(
                                number = rawNum,
                                category = category,
                                targetEntity = targetEntity,
                                riskScore = riskScore
                            )
                        )
                    }
                }
            }
        }

        val revokedList = mutableListOf<String>()
        val revokedArray = root.optJSONArray("revoked")
        if (revokedArray != null) {
            for (i in 0 until revokedArray.length()) {
                val str = revokedArray.optString(i, "").trim()
                if (str.isNotEmpty()) {
                    revokedList.add(str)
                }
            }
        }

        return SpamResponseDto(
            callingCode = callingCode,
            countryIso = countryIso,
            date = date,
            ttlDays = ttlDays,
            added = addedList,
            revoked = revokedList
        )
    }
}
