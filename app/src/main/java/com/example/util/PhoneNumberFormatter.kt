package com.example.util

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

object PhoneNumberFormatter {

    data class CountryInfo(
        val countryIso: String,
        val callingCode: String,
        val nationalPrefix: String
    )

    private val countryMap = mapOf(
        "JP" to CountryInfo("JP", "81", "0"),
        "US" to CountryInfo("US", "1", "1"),
        "CA" to CountryInfo("CA", "1", "1"),
        "GB" to CountryInfo("GB", "44", "0"),
        "KR" to CountryInfo("KR", "82", "0"),
        "AU" to CountryInfo("AU", "61", "0"),
        "CN" to CountryInfo("CN", "86", "0"),
        "TW" to CountryInfo("TW", "886", "0"),
        "DE" to CountryInfo("DE", "49", "0"),
        "FR" to CountryInfo("FR", "33", "0"),
        "IT" to CountryInfo("IT", "39", ""),
        "ES" to CountryInfo("ES", "34", ""),
        "BR" to CountryInfo("BR", "55", "0"),
        "IN" to CountryInfo("IN", "91", "0"),
        "SG" to CountryInfo("SG", "65", ""),
        "HK" to CountryInfo("HK", "852", ""),
        "PH" to CountryInfo("PH", "63", "0"),
        "TH" to CountryInfo("TH", "66", "0"),
        "VN" to CountryInfo("VN", "84", "0"),
        "ID" to CountryInfo("ID", "62", "0"),
        "MY" to CountryInfo("MY", "60", "0"),
        "NZ" to CountryInfo("NZ", "64", "0")
    )

    /**
     * 端末の現在の国コード (2桁ISO、例: "JP") を取得
     */
    fun getDeviceCountryIso(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            var country = tm?.simCountryIso
            if (country.isNullOrBlank()) {
                country = tm?.networkCountryIso
            }
            if (country.isNullOrBlank()) {
                country = Locale.getDefault().country
            }
            country?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "JP"
        } catch (_: Exception) {
            "JP"
        }
    }

    /**
     * 自国の国番号（例: 日本なら +81, 0081, 01081）が付いている電話番号を、
     * 発信やダイヤラー入力に適した国内電話番号形式（例: 09012345678）に変換します。
     * 他国の国際番号（例: +1..., +44...）はそのまま保持します。
     */
    fun toNationalDigits(context: Context, rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val trimmed = rawNumber.trim()

        val countryIso = getDeviceCountryIso(context)
        val countryInfo = countryMap[countryIso] ?: countryMap["JP"]!!

        // 数字と先頭の+のみ抽出
        val digitsOnly = trimmed.replace("[^0-9+]".toRegex(), "")
        val callingCode = countryInfo.callingCode
        val nationalPrefix = countryInfo.nationalPrefix

        return when {
            digitsOnly.startsWith("+$callingCode") -> {
                val remainder = digitsOnly.removePrefix("+$callingCode")
                if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            digitsOnly.startsWith("00$callingCode") -> {
                val remainder = digitsOnly.removePrefix("00$callingCode")
                if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            digitsOnly.startsWith("010$callingCode") -> {
                val remainder = digitsOnly.removePrefix("010$callingCode")
                if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            else -> {
                if (digitsOnly.startsWith("+")) {
                    digitsOnly
                } else {
                    digitsOnly.replace("+", "")
                }
            }
        }
    }

    /**
     * 自分の国番号（例: 日本なら +81）が付いている電話番号を、自国内の国内表記（例: 090-XXXX-XXXX）に変更します。
     * 他国の場合はその国の標準フォーマットに準拠します。
     */
    fun formatForDisplay(context: Context, rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val trimmed = rawNumber.trim()

        val countryIso = getDeviceCountryIso(context)
        val countryInfo = countryMap[countryIso] ?: countryMap["JP"]!!

        // 正規化（数字と先頭の+のみ抽出）
        val digitsOnly = trimmed.replace("[^0-9+]".toRegex(), "")

        val callingCode = countryInfo.callingCode
        val nationalPrefix = countryInfo.nationalPrefix

        var nationalNumber: String? = null

        // 自分の国番号（例: +81, 0081, 01081）で始まっているか判定
        when {
            digitsOnly.startsWith("+$callingCode") -> {
                val remainder = digitsOnly.removePrefix("+$callingCode")
                nationalNumber = if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            digitsOnly.startsWith("00$callingCode") -> {
                val remainder = digitsOnly.removePrefix("00$callingCode")
                nationalNumber = if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            digitsOnly.startsWith("010$callingCode") -> {
                val remainder = digitsOnly.removePrefix("010$callingCode")
                nationalNumber = if (nationalPrefix.isNotEmpty() && !remainder.startsWith(nationalPrefix)) {
                    "$nationalPrefix$remainder"
                } else {
                    remainder
                }
            }
            // 国番号が付いていないがそのまま国内番号として扱える場合
            digitsOnly.startsWith(nationalPrefix) -> {
                nationalNumber = digitsOnly
            }
        }

        // 国内番号に変換された場合、その国の標準形式（例: ハイフン区切り）に整形
        if (nationalNumber != null) {
            val formatted = formatNationalNumber(nationalNumber, countryIso)
            if (formatted.isNotBlank()) {
                return formatted
            }
            return nationalNumber
        }

        // 相手が海外番号の場合は、国際表記フォーマットで整形
        val formattedIntl = try {
            PhoneNumberUtils.formatNumber(trimmed, countryIso)
        } catch (_: Exception) {
            null
        }

        return formattedIntl ?: trimmed
    }

    /**
     * 国内電話番号（例: 09012345678 や 0312345678）を適切なハイフン区切りで整形
     */
    private fun formatNationalNumber(number: String, countryIso: String): String {
        val cleanNumber = number.replace("[^0-9]".toRegex(), "")

        // システム標準の PhoneNumberUtils による整形を試行
        val sysFormatted = try {
            PhoneNumberUtils.formatNumber(cleanNumber, countryIso)
        } catch (_: Exception) {
            null
        }

        if (!sysFormatted.isNullOrBlank() && sysFormatted != cleanNumber) {
            return sysFormatted
        }

        // 日本国内電話番号 (JP) のフォールバック整形
        if (countryIso == "JP") {
            return formatJapaneseNumber(cleanNumber)
        }

        return cleanNumber
    }

    /**
     * 日本の電話番号を美しくハイフン整形するフォールバック
     */
    private fun formatJapaneseNumber(clean: String): String {
        return when {
            // 携帯電話・IP電話 (11桁: 090, 080, 070, 050, 020)
            clean.length == 11 && (clean.startsWith("090") || clean.startsWith("080") ||
                    clean.startsWith("070") || clean.startsWith("050") || clean.startsWith("020")) -> {
                "${clean.substring(0, 3)}-${clean.substring(3, 7)}-${clean.substring(7)}"
            }
            // フリーダイヤル (10桁: 0120, 0800)
            clean.length == 10 && clean.startsWith("0120") -> {
                "${clean.substring(0, 4)}-${clean.substring(4, 7)}-${clean.substring(7)}"
            }
            clean.length == 10 && clean.startsWith("0800") -> {
                "${clean.substring(0, 4)}-${clean.substring(4, 7)}-${clean.substring(7)}"
            }
            // ナビダイヤル (10桁: 0570)
            clean.length == 10 && clean.startsWith("0570") -> {
                "${clean.substring(0, 4)}-${clean.substring(4, 7)}-${clean.substring(7)}"
            }
            // 東京(03)・大阪(06) 10桁
            clean.length == 10 && (clean.startsWith("03") || clean.startsWith("06")) -> {
                "${clean.substring(0, 2)}-${clean.substring(2, 6)}-${clean.substring(6)}"
            }
            // 3桁市外局番 (横浜045, 名古屋052, 札幌011, 福岡092, 神戸078, 京都075 等) 10桁
            clean.length == 10 && clean.startsWith("0") -> {
                "${clean.substring(0, 3)}-${clean.substring(3, 6)}-${clean.substring(6)}"
            }
            // 一般の市外局番 10桁
            clean.length == 10 -> {
                "${clean.substring(0, 3)}-${clean.substring(3, 6)}-${clean.substring(6)}"
            }
            // 11桁固定電話等のケース
            clean.length == 11 -> {
                "${clean.substring(0, 3)}-${clean.substring(3, 7)}-${clean.substring(7)}"
            }
            else -> clean
        }
    }
}
