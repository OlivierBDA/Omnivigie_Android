package com.olivierbda.omnivigie.data.repository

import com.olivierbda.omnivigie.data.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotebookRepository(private val sessionManager: SessionManager) {

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        sessionManager.isNotebookConnected()
    }
}
