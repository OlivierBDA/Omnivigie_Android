package com.olivierbda.omnivigie.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.olivierbda.omnivigie.BuildConfig
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID
    private val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"

    suspend fun getGcpIdToken(activity: Activity): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdTokenCredential.idToken
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signIn(activity: Activity): GoogleIdTokenCredential? {
        Log.d("AuthManager", "Starting signIn flow with ClientID: $WEB_CLIENT_ID")
        
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
            
            Log.d("AuthManager", "Credential type received: ${credential::class.java.simpleName}")
            
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.d("AuthManager", "Successfully parsed GoogleIdToken for: ${googleIdTokenCredential.id}")
                googleIdTokenCredential
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to parse GoogleIdToken from credential data: ${e.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in exception: ${e.message}", e)
            null
        }
    }

    suspend fun authorizeGmail(activity: Activity): AuthorizationResult? {
        Log.d("AuthManager", "Requesting Gmail authorization...")
        val requestedScopes = listOf(Scope(GMAIL_SCOPE))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        return try {
            val result = Identity.getAuthorizationClient(activity)
                .authorize(authorizationRequest)
                .await()
            result
        } catch (e: Exception) {
            Log.e("AuthManager", "Gmail Authorization exception: ${e.message}", e)
            null
        }
    }
    
    fun getAuthorizationResult(activity: Activity, data: android.content.Intent?): AuthorizationResult {
        return Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
    }

    suspend fun signOut() {
        Log.d("AuthManager", "Signing out...")
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out failed", e)
        }
    }
}
