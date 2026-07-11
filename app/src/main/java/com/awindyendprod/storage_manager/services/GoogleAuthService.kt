package com.awindyendprod.storage_manager.services

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

sealed class AuthResult {
    data class Success(val token: String) : AuthResult()
    data class RecoverableConsent(val intent: Intent) : AuthResult()
    data class Failure(val cause: Throwable) : AuthResult()
}

class GoogleAuthService(private val context: Context) {

    fun buildSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun getAccessToken(account: GoogleSignInAccount): AuthResult = withContext(Dispatchers.IO) {
        val androidAccount = account.account
            ?: return@withContext AuthResult.Failure(IllegalStateException("Signed-in account has no linked Android Account"))
        try {
            val token = GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$DRIVE_APPDATA_SCOPE")
            AuthResult.Success(token)
        } catch (e: UserRecoverableAuthException) {
            val consentIntent = e.intent
            if (consentIntent != null) AuthResult.RecoverableConsent(consentIntent) else AuthResult.Failure(e)
        } catch (e: GoogleAuthException) {
            AuthResult.Failure(e)
        } catch (e: IOException) {
            AuthResult.Failure(e)
        }
    }

    suspend fun clearCachedToken(token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: GoogleAuthException) {
        } catch (e: IOException) {
        }
    }

    fun signOut(onComplete: () -> Unit) {
        buildSignInClient().signOut().addOnCompleteListener { onComplete() }
    }
}
