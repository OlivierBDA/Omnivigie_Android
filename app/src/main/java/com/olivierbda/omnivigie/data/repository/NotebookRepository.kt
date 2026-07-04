package com.olivierbda.omnivigie.data.repository

import com.olivierbda.omnivigie.data.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class NotebookRepository(private val sessionManager: SessionManager) {

    private val client = OkHttpClient()

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        val cookies = sessionManager.getNotebookCookies() ?: return@withContext false
        val snlm0e = sessionManager.getNotebookSnlm0e() ?: return@withContext false

        try {
            // Minimal request to check if session is still valid
            val request = Request.Builder()
                .url("https://notebooklm.google.com/")
                .addHeader("Cookie", cookies)
                .build()

            client.newCall(request).execute().use { response ->
                // If we get 200 and the response contains SNlM0e, we are likely still logged in
                val body = response.body?.string() ?: ""
                response.isSuccessful && body.contains("SNlM0e")
            }
        } catch (e: Exception) {
            false
        }
    }
}
