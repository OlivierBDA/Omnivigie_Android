package com.olivierbda.omnivigie.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveNotebookSession(storageStateJson: String) {
        sharedPreferences.edit().apply {
            putString("notebook_storage_state", storageStateJson)
            putLong("notebook_last_auth", System.currentTimeMillis())
            apply()
        }
    }

    fun getNotebookStorageState(): String? = sharedPreferences.getString("notebook_storage_state", null)

    fun isNotebookConnected(): Boolean {
        return getNotebookStorageState() != null
    }

    fun clearNotebookSession() {
        sharedPreferences.edit().apply {
            remove("notebook_storage_state")
            remove("notebook_last_auth")
            apply()
        }
    }
}
