package com.olivierbda.omnivigie.ui.viewmodel

import android.app.Application
import android.app.Activity
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olivierbda.omnivigie.data.auth.AuthManager
import com.olivierbda.omnivigie.data.local.OmnivigieDatabase
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import com.olivierbda.omnivigie.data.repository.GmailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = OmnivigieDatabase.getDatabase(application)
    private val articleDao = db.articleDao()
    private val emailDao = db.emailDao()
    
    private val authManager = AuthManager(application)
    private val gmailRepository = GmailRepository(emailDao)

    val articles: StateFlow<List<ArticleEntity>> = articleDao.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    fun syncGmail(activity: Activity) {
        viewModelScope.launch {
            _syncStatus.value = "Authentification..."
            val credential = authManager.signIn(activity)
            if (credential == null) {
                _syncStatus.value = "Échec de connexion Google"
                return@launch
            }

            _syncStatus.value = "Autorisation Gmail..."
            val token = authManager.authorizeGmail(activity)
            if (token == null) {
                _syncStatus.value = "Veuillez autoriser l'accès à Gmail"
                return@launch
            }

            _syncStatus.value = "Synchronisation des emails..."
            val count = gmailRepository.syncEmails(token, "2026/06/24")
            _syncStatus.value = "$count nouveaux emails récupérés"
        }
    }

    fun insertMockData() {
        viewModelScope.launch {
            val emailId = "msg_${System.currentTimeMillis()}"
            val email = EmailEntity(
                id = emailId,
                receivedDate = System.currentTimeMillis(),
                sender = "tldr@tldrnewsletter.com",
                subject = "TLDR AI 2026",
                bodyHtml = "<html>...</html>"
            )
            emailDao.insertEmail(email)

            val mockArticles = listOf(
                ArticleEntity(
                    emailId = emailId,
                    title = "Database Ready: Room Implementation",
                    url = "https://developer.android.com/room",
                    source = "Omnivigie System",
                    readingTime = "2 min",
                    summary = "The Room database is now fully functional and integrated with the UI.",
                    isSponsor = false,
                    aiInterest = true,
                    aiThemes = listOf("Android", "Room")
                ),
                ArticleEntity(
                    emailId = emailId,
                    title = "Persistent Storage Active",
                    url = "https://github.com",
                    source = "Omnivigie System",
                    readingTime = "1 min",
                    summary = "Data is now saved locally and survives app restarts.",
                    isSponsor = false,
                    aiInterest = true,
                    aiThemes = listOf("Database", "Architecture")
                )
            )
            articleDao.insertArticles(mockArticles)
        }
    }
}
