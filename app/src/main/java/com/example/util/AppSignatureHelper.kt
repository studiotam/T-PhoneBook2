package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

import android.util.Base64

data class SignatureInfo(
    val packageName: String,
    val runtimeSha1: String,
    val runtimeSha256: String,
    val runtimeKeyHashBase64: String,
    val releaseSha1: String,
    val releaseSha256: String,
    val debugSha1: String,
    val debugSha256: String
) {
    val runtimeSha1NoColons: String get() = runtimeSha1.replace(":", "")
    val runtimeSha256NoColons: String get() = runtimeSha256.replace(":", "")
    val releaseSha1NoColons: String get() = releaseSha1.replace(":", "")
    val releaseSha256NoColons: String get() = releaseSha256.replace(":", "")
}

object AppSignatureHelper {

    const val KNOWN_RELEASE_SHA1 = "11:F7:96:30:C4:ED:4B:24:A7:7C:B5:A6:3F:C1:6C:6F:DA:34:5B:7A"
    const val KNOWN_RELEASE_SHA256 = "C2:D4:76:53:FD:0E:10:E9:10:42:B4:E4:C2:ED:39:5D:4F:92:F8:E6:E5:F7:6B:B1:E8:05:01:44:09:6C:DC:8D"
    const val KNOWN_DEBUG_SHA1 = "62:D9:0C:A5:42:1D:EE:FF:05:1D:FA:53:82:C9:B2:7C:AD:5A:A7:F6"
    const val KNOWN_DEBUG_SHA256 = "5C:DB:1E:1E:E5:5D:18:1B:F3:7B:C3:E0:08:38:E3:7E:3B:08:E2:6F:C9:CD:AC:EB:E7:C8:26:13:21:A5:E9:38"

    fun getSignatureInfo(context: Context): SignatureInfo {
        val runtimeSha1 = getCertificateFingerprint(context, "SHA-1") ?: KNOWN_DEBUG_SHA1
        val runtimeSha256 = getCertificateFingerprint(context, "SHA-256") ?: KNOWN_DEBUG_SHA256
        val runtimeKeyHashBase64 = getCertificateBase64KeyHash(context) ?: "N/A"

        return SignatureInfo(
            packageName = context.packageName,
            runtimeSha1 = runtimeSha1,
            runtimeSha256 = runtimeSha256,
            runtimeKeyHashBase64 = runtimeKeyHashBase64,
            releaseSha1 = KNOWN_RELEASE_SHA1,
            releaseSha256 = KNOWN_RELEASE_SHA256,
            debugSha1 = KNOWN_DEBUG_SHA1,
            debugSha256 = KNOWN_DEBUG_SHA256
        )
    }

    private fun getCertificateBase64KeyHash(context: Context): String? {
        return try {
            val cert = getRawCertificate(context) ?: return null
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun getRawCertificate(context: Context): ByteArray? {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.firstOrNull()?.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun getCertificateFingerprint(context: Context, algorithm: String): String? {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            null
        }
    }
}
