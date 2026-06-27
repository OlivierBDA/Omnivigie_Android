package com.olivierbda.omnivigie.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.olivierbda.omnivigie.BuildConfig
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID
    private val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"

    suspend fun signIn(activity: Activity): GoogleIdTokenCredential? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                credential
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in failed", e)
            null
        }
    }

    suspend fun authorizeGmail(activity: Activity): String? {
        val requestedScopes = listOf(Scope(GMAIL_SCOPE))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        return try {
            val result = Identity.getAuthorizationClient(activity)
                .authorize(authorizationRequest)
                .await()
            
            if (result.hasResolution() && result.pendingIntent != null) {
                // If resolution is required, the UI component will have to handle the intent
                // We'll return null here and let the caller handle the resolution
                null
            } else {
                result.accessToken
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Authorization failed", e)
            null
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
