package com.olivierbda.omnivigie.data.repository

import android.util.Log
import com.google.gson.Gson
import com.olivierbda.omnivigie.data.auth.SessionManager
import com.olivierbda.omnivigie.data.remote.GcpFunctionApiService
import com.olivierbda.omnivigie.data.remote.GcpFunctionRequest

class NotebookLmRepository(
    private val gcpApiService: GcpFunctionApiService,
    private val sessionManager: SessionManager
) {
    private val TAG = "NotebookLmRepository"
    private val gson = Gson()

    /**
     * Crée un nouveau carnet via la Cloud Function GCP.
     */
    suspend fun createNotebook(idToken: String, title: String): String? {
        val storageStateJson = sessionManager.getNotebookStorageState() ?: return null
        val storageState = gson.fromJson(storageStateJson, Any::class.java)

        val request = GcpFunctionRequest(
            action = "create_notebook",
            notebookName = title,
            storageState = storageState
        )

        return try {
            val response = gcpApiService.executeAction("Bearer $idToken", request)
            if (response.error != null) {
                Log.e(TAG, "Erreur backend GCP: ${response.error}")
                null
            } else {
                response.notebookId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de createNotebook: ${e.message}")
            null
        }
    }

    /**
     * Ajoute une liste d'URLs via la Cloud Function GCP.
     */
    suspend fun addUrlsBatch(idToken: String, notebookId: String, urls: List<String>): Boolean {
        val storageStateJson = sessionManager.getNotebookStorageState() ?: return false
        val storageState = gson.fromJson(storageStateJson, Any::class.java)

        val request = GcpFunctionRequest(
            action = "add_sources",
            notebookId = notebookId,
            urls = urls,
            storageState = storageState
        )

        return try {
            val response = gcpApiService.executeAction("Bearer $idToken", request)
            if (response.error != null) {
                Log.e(TAG, "Erreur backend GCP (add_sources): ${response.error}")
                false
            } else {
                val successCount = response.addedUrls?.size ?: 0
                Log.d(TAG, "$successCount URLs ajoutées avec succès.")
                successCount > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de addUrlsBatch: ${e.message}")
            false
        }
    }
}
