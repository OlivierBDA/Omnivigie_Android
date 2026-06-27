package com.olivierbda.omnivigie.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olivierbda.omnivigie.data.local.OmnivigieDatabase
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = OmnivigieDatabase.getDatabase(application)
    private val articleDao = db.articleDao()
    private val emailDao = db.emailDao()

    val articles: StateFlow<List<ArticleEntity>> = articleDao.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    
    fun clearData() {
        // Simple implementation for demo
        viewModelScope.launch {
            // Need a more robust clear in production
        }
    }
}
