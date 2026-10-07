package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.domain.model.SimInfo

object SimHelper {

    /**
     * 電話状態・SIM情報の読み取り権限があるか確認
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        val phoneState = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val phoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return phoneState || phoneNumbers
    }

    /**
     * 端末上の利用可能なSIM情報を取得します。
     * デュアルSIM端末、シングルSIM端末、eSIM、エミュレータ環境に対応
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun getAvailableSims(
        context: Context,
        customSim1Number: String? = null,
        customSim2Number: String? = null
    ): List<SimInfo> {
        val hasPermission = hasPhoneStatePermission(context)
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        if (hasPermission) {
            try {
                val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SubscriptionManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SubscriptionManager.from(context)
                }

                val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                if (!activeList.isNullOrEmpty()) {
                    val activeSlotMap = activeList.associateBy { it.simSlotIndex }
                    val result = mutableListOf<SimInfo>()

                    val maxSlots = maxOf(2, activeList.maxOfOrNull { it.simSlotIndex + 1 } ?: 2)

                    for (slot in 0 until maxSlots) {
                        val info = activeSlotMap[slot]
                        if (info != null) {
                            val subId = info.subscriptionId
                            val subTelephony = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                telephonyManager?.createForSubscriptionId(subId)
                            } else {
                                telephonyManager
                            }

                            val pCarrierId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                subTelephony?.simCarrierIdName?.toString()?.takeIf { it.isNotBlank() }
                            } else {
                                null
                            }

                            val carrierName: String = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                                ?: subTelephony?.simOperatorName?.takeIf { it.isNotBlank() }
                                ?: subTelephony?.networkOperatorName?.takeIf { it.isNotBlank() }
                                ?: pCarrierId
                                ?: "SIM ${slot + 1}"

                            val displayName: String = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                                ?: carrierName

                            // 電話番号の取得
                            val systemNumber = try {
                                var num: String? = null
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    num = subscriptionManager.getPhoneNumber(subId)
                                }
                                if (num.isNullOrBlank()) {
                                    num = subTelephony?.line1Number
                                }
                                if (num.isNullOrBlank()) {
                                    @Suppress("DEPRECATION")
                                    num = info.number
                                }
                                num?.takeIf { it.isNotBlank() }
                            } catch (_: Exception) {
                                null
                            }

                            val resolvedNumber = when (slot) {
                                0 -> customSim1Number?.takeIf { it.isNotBlank() } ?: systemNumber
                                1 -> customSim2Number?.takeIf { it.isNotBlank() } ?: systemNumber
                                else -> systemNumber
                            }

                            result.add(
                                SimInfo(
                                    slotIndex = slot,
                                    subscriptionId = subId,
                                    displayName = displayName,
                                    carrierName = carrierName,
                                    phoneNumber = resolvedNumber,
                                    isActive = true
                                )
                            )
                        } else {
                            val customNumber = if (slot == 0) customSim1Number else customSim2Number
                            result.add(
                                SimInfo(
                                    slotIndex = slot,
                                    subscriptionId = -1,
                                    displayName = "SIM ${slot + 1}",
                                    carrierName = "無効 (未装着)",
                                    phoneNumber = customNumber?.takeIf { it.isNotBlank() },
                                    isActive = false
                                )
                            )
                        }
                    }
                    return result
                } else {
                    val simState = telephonyManager?.simState
                    if (simState == TelephonyManager.SIM_STATE_READY) {
                        val carrier: String = telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
                            ?: telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() }
                            ?: "SIM 1"
                        val line1 = try { telephonyManager.line1Number } catch (_: Exception) { null }
                        val resolvedNumber = customSim1Number?.takeIf { it.isNotBlank() } ?: line1

                        return listOf(
                            SimInfo(
                                slotIndex = 0,
                                subscriptionId = 1,
                                displayName = carrier,
                                carrierName = carrier,
                                phoneNumber = resolvedNumber,
                                isActive = true
                            ),
                            SimInfo(
                                slotIndex = 1,
                                subscriptionId = -1,
                                displayName = "SIM 2",
                                carrierName = "無効 (未装着)",
                                phoneNumber = customSim2Number?.takeIf { it.isNotBlank() },
                                isActive = false
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        return listOf(
            SimInfo(
                slotIndex = 0,
                subscriptionId = 1,
                displayName = "SIM 1",
                carrierName = "SIM 1",
                phoneNumber = customSim1Number?.takeIf { it.isNotBlank() },
                isActive = true
            ),
            SimInfo(
                slotIndex = 1,
                subscriptionId = 2,
                displayName = "SIM 2",
                carrierName = "SIM 2",
                phoneNumber = customSim2Number?.takeIf { it.isNotBlank() },
                isActive = true
            )
        )
    }

    /**
     * 指定されたsubscriptionIdに対応するPhoneAccountHandleを取得
     */
    @SuppressLint("MissingPermission")
    fun getPhoneAccountHandleForSubscription(context: Context, subscriptionId: Int): PhoneAccountHandle? {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return null
            val handles = telecomManager.callCapablePhoneAccounts
            for (handle in handles) {
                if (handle.id.contains(subscriptionId.toString())) {
                    return handle
                }
            }
            if (handles.isNotEmpty()) {
                return handles.firstOrNull()
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * 指定の電話番号とSIMスロット/サブスクリプションIDで電話を発信
     */
    fun createCallIntent(
        context: Context,
        numberToCall: String,
        simSlotIndex: Int?,
        subscriptionId: Int?
    ): Intent {
        return Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${Uri.encode(numberToCall)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (simSlotIndex != null) {
                putExtra("com.android.phone.extra.slot", simSlotIndex)
                putExtra("simSlot", simSlotIndex)
                putExtra("sim_slot", simSlotIndex)
                putExtra("slot", simSlotIndex)
                putExtra("Cdma_SimId", simSlotIndex)
                putExtra("com.android.phone.DialingMode", simSlotIndex)
            }

            if (subscriptionId != null && subscriptionId != -1) {
                putExtra("subscription", subscriptionId)
                putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subscriptionId)
                val handle = getPhoneAccountHandleForSubscription(context, subscriptionId)
                if (handle != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
            }
        }
    }

    /**
     * 日本国内の環境（ロケール、SIM、ネットワーク）であるかを判定
     */
    fun isJapan(context: Context): Boolean {
        try {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = telephony?.simCountryIso
            val netCountry = telephony?.networkCountryIso
            val localeCountry = java.util.Locale.getDefault().country
            val localeLang = java.util.Locale.getDefault().language

            if (simCountry?.equals("jp", ignoreCase = true) == true ||
                netCountry?.equals("jp", ignoreCase = true) == true ||
                localeCountry.equals("JP", ignoreCase = true) ||
                localeLang.equals("ja", ignoreCase = true)
            ) {
                return true
            }
            if (hasRakutenSim(context)) {
                return true
            }
        } catch (_: Exception) {}
        return true // 日本語端末環境で利用されることが想定されるため安全側に判定
    }

    /**
     * 楽天モバイル (Rakuten Mobile) のSIMが装着・有効化されているか判定
     */
    fun hasRakutenSim(
        context: Context,
        customSim1Number: String? = null,
        customSim2Number: String? = null
    ): Boolean {
        try {
            val sims = getAvailableSims(context, customSim1Number, customSim2Number)
            for (sim in sims) {
                if (sim.isActive) {
                    val combinedName = "${sim.carrierName} ${sim.displayName}".lowercase()
                    if (combinedName.contains("rakuten") || combinedName.contains("楽天")) {
                        return true
                    }
                }
            }

            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simOp = telephonyManager?.simOperator ?: ""
            val simOpName = telephonyManager?.simOperatorName?.lowercase() ?: ""
            val netOpName = telephonyManager?.networkOperatorName?.lowercase() ?: ""

            if (simOp == "44011" || simOp == "44053" ||
                simOpName.contains("rakuten") || simOpName.contains("楽天") ||
                netOpName.contains("rakuten") || netOpName.contains("楽天")
            ) {
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    const val RAKUTEN_LINK_PACKAGE = "jp.co.rakuten.mobile.rcs"

    /**
     * 楽天Link (Rakuten Link) アプリを起動して発信
     */
    fun launchRakutenLink(context: Context, rawNumber: String): Boolean {
        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        val pm = context.packageManager

        // 1. rakutenlink://call?number=... による直接呼び出し
        val directUriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("rakutenlink://call?number=${Uri.encode(cleanNumber)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setPackage(RAKUTEN_LINK_PACKAGE)
        }

        // 2. tel: スキーム + 楽天Linkパッケージ指定呼び出し
        val telPackageIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setPackage(RAKUTEN_LINK_PACKAGE)
        }

        // 3. 一般 URI (パッケージ無指定)
        val generalUriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("rakutenlink://call?number=${Uri.encode(cleanNumber)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            if (directUriIntent.resolveActivity(pm) != null) {
                context.startActivity(directUriIntent)
                return true
            }
        } catch (_: Exception) {}

        try {
            if (telPackageIntent.resolveActivity(pm) != null) {
                context.startActivity(telPackageIntent)
                return true
            }
        } catch (_: Exception) {}

        try {
            if (generalUriIntent.resolveActivity(pm) != null) {
                context.startActivity(generalUriIntent)
                return true
            }
        } catch (_: Exception) {}

        // 4. パッケージ起動インテント + extras
        try {
            val launchIntent = pm.getLaunchIntentForPackage(RAKUTEN_LINK_PACKAGE)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("number", cleanNumber)
                putExtra("phone_number", cleanNumber)
                data = Uri.parse("tel:${Uri.encode(cleanNumber)}")
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return true
            }
        } catch (_: Exception) {}

        // 5. 楽天Linkが見つからない場合は標準ダイヤルへフォールバック
        return try {
            val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            true
        } catch (_: Exception) {
            false
        }
    }
}
