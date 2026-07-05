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

    fun saveNotebookSession(cookies: String, snlm0e: String, fdrfje: String) {
        sharedPreferences.edit().apply {
            putString("notebook_cookies", cookies)
            putString("notebook_snlm0e", snlm0e)
            putString("notebook_fdrfje", fdrfje)
            putLong("notebook_last_auth", System.currentTimeMillis())
            apply()
        }
    }

    fun getNotebookCookies(): String? = sharedPreferences.getString("notebook_cookies", null)
    fun getNotebookSnlm0e(): String? = sharedPreferences.getString("notebook_snlm0e", null)
    fun getNotebookFdrfje(): String? = sharedPreferences.getString("notebook_fdrfje", null)

    data class NotebookSession(
        val cookies: String,
        val snlm0e: String,
        val fdrfje: String
    )

    fun getNotebookSession(): NotebookSession? {
        val cookies = getNotebookCookies()
        val snlm0e = getNotebookSnlm0e()
        val fdrfje = getNotebookFdrfje()
        return if (cookies != null && snlm0e != null && fdrfje != null) {
            NotebookSession(cookies, snlm0e, fdrfje)
        } else null
    }

    fun isNotebookConnected(): Boolean {
        return getNotebookCookies() != null && getNotebookSnlm0e() != null
    }

    fun clearNotebookSession() {
        sharedPreferences.edit().apply {
            remove("notebook_cookies")
            remove("notebook_snlm0e")
            remove("notebook_fdrfje")
            remove("notebook_last_auth")
            apply()
        }
    }
}
