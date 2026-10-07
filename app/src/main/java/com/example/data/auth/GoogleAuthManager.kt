package com.example.data.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class OAuthTokenResult {
    data class Success(val token: String) : OAuthTokenResult()
    data class NeedsUserConsent(val consentIntent: Intent) : OAuthTokenResult()
    data class Error(val message: String) : OAuthTokenResult()
}

class GoogleAuthManager(private val context: Context) {
    private val accountManager: AccountManager = AccountManager.get(context)

    companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val OAUTH_CLIENT_ID = "324244330586-gg20ctb2sb1d7ds6obt77vt15e7iu9v1.apps.googleusercontent.com"
        
        // Google Drive AppData & File, Profile Scopes
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val USERINFO_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
        const val USERINFO_PROFILE_SCOPE = "https://www.googleapis.com/auth/userinfo.profile"

        const val OAUTH_SCOPES = "oauth2:$DRIVE_APPDATA_SCOPE $DRIVE_FILE_SCOPE $USERINFO_EMAIL_SCOPE $USERINFO_PROFILE_SCOPE"
    }

    /**
     * 端末のAccountManagerに登録されているGoogleアカウント（メールアドレス）一覧を取得する
     */
    fun getDeviceGoogleAccounts(): List<String> {
        return try {
            accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE).map { it.name }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 端末標準のGoogleアカウント選択ダイアログを表示するためのIntentを作成する
     */
    fun createChooseAccountIntent(selectedAccountName: String? = null): Intent {
        val selectedAccount = if (!selectedAccountName.isNullOrBlank()) {
            Account(selectedAccountName, GOOGLE_ACCOUNT_TYPE)
        } else {
            null
        }

        return AccountManager.newChooseAccountIntent(
            selectedAccount,
            null,
            arrayOf(GOOGLE_ACCOUNT_TYPE),
            null,
            null,
            null,
            null
        )
    }

    /**
     * 選択されたGoogleアカウントのOAuth2アクセストークンを非同期で取得する
     */
    suspend fun getOAuthAccessToken(email: String): OAuthTokenResult = withContext(Dispatchers.IO) {
        if (email.isBlank()) {
            return@withContext OAuthTokenResult.Error("Googleアカウントが選択されていません")
        }

        try {
            val account = Account(email, GOOGLE_ACCOUNT_TYPE)
            val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPES)
            Log.d("GoogleAuthManager", "OAuth access token acquired successfully for $email")
            OAuthTokenResult.Success(token)
        } catch (e: UserRecoverableAuthException) {
            Log.w("GoogleAuthManager", "User consent required for OAuth token: ${e.message}")
            val intent = e.intent
            if (intent != null) {
                OAuthTokenResult.NeedsUserConsent(intent)
            } else {
                OAuthTokenResult.Error("ユーザー認証の承認が必要です: ${e.localizedMessage}")
            }
        } catch (e: GoogleAuthException) {
            Log.e("GoogleAuthManager", "GoogleAuthException: ${e.message}", e)
            OAuthTokenResult.Error("認証エラー: ${e.localizedMessage ?: "Google認証に失敗しました"}")
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Unexpected exception while getting token: ${e.message}", e)
            OAuthTokenResult.Error("エラーが発生しました: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /**
     * キャッシュされたトークンを無効化し次回リフレッシュさせる
     */
    suspend fun invalidateToken(token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "Failed to clear token", e)
        }
    }
}
