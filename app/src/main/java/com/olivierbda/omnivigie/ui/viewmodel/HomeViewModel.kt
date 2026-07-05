package com.olivierbda.omnivigie.ui.viewmodel

import android.app.Application
import android.app.Activity
import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olivierbda.omnivigie.data.auth.AuthManager
import com.olivierbda.omnivigie.data.auth.SessionManager
import com.olivierbda.omnivigie.data.local.OmnivigieDatabase
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import com.olivierbda.omnivigie.data.local.entities.SettingEntity
import com.olivierbda.omnivigie.data.repository.GmailRepository
import com.olivierbda.omnivigie.data.repository.GeminiRepository
import com.olivierbda.omnivigie.data.repository.NotebookRepository
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository
import com.olivierbda.omnivigie.domain.usecase.CreateThemedNotebookUseCase
import com.olivierbda.omnivigie.domain.usecase.QualifyArticlesUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = OmnivigieDatabase.getDatabase(application)
    private val articleDao = db.articleDao()
    private val emailDao = db.emailDao()
    private val settingDao = db.settingDao()
    
    private val authManager = AuthManager(application)
    private val gmailRepository = GmailRepository(emailDao, articleDao)
    private val geminiRepository = GeminiRepository()
    private val qualifyArticlesUseCase = QualifyArticlesUseCase(application, articleDao, geminiRepository)
    
    private val sessionManager = SessionManager(application)
    private val notebookRepository = NotebookRepository(sessionManager)
    
    private val notebookLmApiService = Retrofit.Builder()
        .baseUrl("https://notebooklm.google.com/")
        .client(OkHttpClient.Builder()
            .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build())
        .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
        .build()
        .create(com.olivierbda.omnivigie.data.remote.NotebookLmApiService::class.java)

    private val notebookLmRepository = NotebookLmRepository(notebookLmApiService, sessionManager)
    private val createThemedNotebookUseCase = CreateThemedNotebookUseCase(notebookLmRepository, articleDao)

    private val DEFAULT_FILTER = "from:dan@tldrnewsletter.com OR from:tldr@tldrnewsletter.com after:2026/06/24"

    val articles: StateFlow<List<ArticleEntity>> = articleDao.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsentArticles: StateFlow<List<ArticleEntity>> = articleDao.getAllUnsentArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedArticles = MutableStateFlow<Set<Int>>(emptySet())
    val selectedArticles = _selectedArticles.asStateFlow()

    fun toggleArticleSelection(articleId: Int) {
        _selectedArticles.value = if (_selectedArticles.value.contains(articleId)) {
            _selectedArticles.value - articleId
        } else {
            _selectedArticles.value + articleId
        }
    }

    fun selectAll(articleIds: List<Int>) {
        _selectedArticles.value = articleIds.toSet()
    }

    fun deselectAll() {
        _selectedArticles.value = emptySet()
    }

    fun deleteArticle(article: ArticleEntity) {
        viewModelScope.launch {
            articleDao.deleteArticle(article)
        }
    }

    val gmailFilter: StateFlow<String> = settingDao.getSetting("gmail_filter")
        .map { it ?: DEFAULT_FILTER }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FILTER)

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _notebookStatus = MutableStateFlow("Vérification...")
    val notebookStatus = _notebookStatus.asStateFlow()

    private val _authorizationPendingIntent = MutableStateFlow<PendingIntent?>(null)
    val authorizationPendingIntent = _authorizationPendingIntent.asStateFlow()

    init {
        refreshNotebookStatus()
    }

    fun refreshNotebookStatus() {
        viewModelScope.launch {
            if (sessionManager.isNotebookConnected()) {
                val isValid = notebookRepository.checkConnection()
                _notebookStatus.value = if (isValid) "Connecté" else "Session expirée"
            } else {
                _notebookStatus.value = "Non Connecté (Authentification requise)"
            }
        }
    }

    fun syncGmail(activity: Activity) {
        viewModelScope.launch {
            _syncStatus.value = "Authentification..."
            val credential = authManager.signIn(activity)
            if (credential == null) {
                _syncStatus.value = "Échec de connexion Google"
                return@launch
            }

            _syncStatus.value = "Vérification des autorisations..."
            val authResult = authManager.authorizeGmail(activity)
            
            if (authResult == null) {
                _syncStatus.value = "Erreur lors de l'autorisation Gmail"
                return@launch
            }

            if (authResult.hasResolution()) {
                _syncStatus.value = "Action requise : Autorisez l'accès à Gmail"
                _authorizationPendingIntent.value = authResult.pendingIntent
            } else {
                startSync(authResult.accessToken!!)
            }
        }
    }
    
    fun onAuthorizationResult(activity: Activity, data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val result = authManager.getAuthorizationResult(activity, data)
                if (result.accessToken != null) {
                    startSync(result.accessToken!!)
                } else {
                    _syncStatus.value = "Autorisation Gmail refusée"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to process authorization result", e)
                _syncStatus.value = "Échec de l'autorisation"
            } finally {
                _authorizationPendingIntent.value = null
            }
        }
    }
    
    private suspend fun startSync(token: String) {
        _syncStatus.value = "Synchronisation des emails..."
        val currentFilter = gmailFilter.value
        val count = gmailRepository.syncEmails(token, currentFilter)
        _syncStatus.value = "$count nouveaux emails récupérés"
    }

    fun updateGmailFilter(newFilter: String) {
        viewModelScope.launch {
            settingDao.insertSetting(SettingEntity("gmail_filter", newFilter))
        }
    }

    fun clearData() {
        viewModelScope.launch {
            try {
                articleDao.deleteAllArticles()
                emailDao.deleteAllEmails()
                _syncStatus.value = "Base de données réinitialisée"
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to clear database", e)
                _syncStatus.value = "Erreur lors de la réinitialisation"
            }
        }
    }

    fun qualifyArticles() {
        viewModelScope.launch {
            qualifyArticlesUseCase.execute().collectLatest { status ->
                _syncStatus.value = status
            }
        }
    }

    fun createNotebook(theme: String) {
        viewModelScope.launch {
            val selectedIds = _selectedArticles.value.toList()
            if (selectedIds.isEmpty()) {
                _syncStatus.value = "Erreur : Aucun article sélectionné"
                return@launch
            }

            createThemedNotebookUseCase.execute(theme, selectedIds).collectLatest { status ->
                _syncStatus.value = status
                if (status.startsWith("Terminé")) {
                    deselectAll()
                }
            }
        }
    }

    fun cleanupArticles() {
        viewModelScope.launch {
            try {
                articleDao.cleanupArticles()
                _syncStatus.value = "Nettoyage terminé"
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Cleanup failed", e)
                _syncStatus.value = "Erreur lors du nettoyage"
            }
        }
    }
}
