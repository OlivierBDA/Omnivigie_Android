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
import com.olivierbda.omnivigie.data.local.entities.SettingEntity
import com.olivierbda.omnivigie.data.repository.GmailRepository
import com.olivierbda.omnivigie.data.repository.GeminiRepository
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository
import com.olivierbda.omnivigie.domain.usecase.CreateThemedNotebookUseCase
import com.olivierbda.omnivigie.domain.usecase.QualifyArticlesUseCase
import com.olivierbda.omnivigie.data.remote.GcpFunctionApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotebookSummary(
    val name: String,
    val articleCount: Int,
    val themes: List<String>,
    val notebookId: String? = null
)

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
    
    private val gcpFunctionApiService = Retrofit.Builder()
        .baseUrl("https://omnivigie-python-backend-306370227717.europe-west1.run.app")
        .client(OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GcpFunctionApiService::class.java)

    private val notebookLmRepository = NotebookLmRepository(gcpFunctionApiService, sessionManager)
    private val createThemedNotebookUseCase = CreateThemedNotebookUseCase(notebookLmRepository, articleDao)

    private val DEFAULT_FILTER = "from:dan@tldrnewsletter.com OR from:tldr@tldrnewsletter.com after:2026/06/24"

    val articles: StateFlow<List<ArticleEntity>> = articleDao.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsentArticles: StateFlow<List<ArticleEntity>> = articleDao.getAllUnsentArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastSyncTimestamp: StateFlow<String> = settingDao.getSetting("last_gmail_sync")
        .map { it ?: "Aujourd'hui à 08:30" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Aujourd'hui à 08:30")

    val unqualifiedCount: StateFlow<Int> = articleDao.getUnqualifiedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingQualifiedCount: StateFlow<Int> = articleDao.getPendingQualifiedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notebooksCount: StateFlow<Int> = articleDao.getNotebooksCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentNotebooks: StateFlow<List<NotebookSummary>> = articleDao.getProcessedArticles()
        .map { articles ->
            articles.groupBy { it.notebookName ?: "Sans nom" }
                .map { (name, group) ->
                    val notebookId = group.firstOrNull()?.notebookId
                    val themes = group.flatMap { it.aiThemes }.distinct()
                    NotebookSummary(
                        name = name,
                        articleCount = group.size,
                        themes = themes,
                        notebookId = notebookId
                    )
                }
                .take(3)
        }
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

    private val _gcpStatus = MutableStateFlow("Prêt")
    val gcpStatus = _gcpStatus.asStateFlow()

    private val _authorizationPendingIntent = MutableStateFlow<PendingIntent?>(null)
    val authorizationPendingIntent = _authorizationPendingIntent.asStateFlow()

    init {
        refreshNotebookStatus()
    }

    fun refreshNotebookStatus() {
        viewModelScope.launch {
            val ageHours = sessionManager.getNotebookAuthAgeHours()
            if (sessionManager.isNotebookConnected()) {
                if (ageHours != null && ageHours > 24) {
                    _notebookStatus.value = "A renouveler"
                } else {
                    _notebookStatus.value = "Connecté"
                }
            } else {
                _notebookStatus.value = "Non Connecté"
            }
        }
    }

    fun reauthGcp(activity: Activity) {
        viewModelScope.launch {
            _syncStatus.value = "Demande d'authentification IAM GCP..."
            val idToken = authManager.getGcpIdToken(activity)
            if (idToken != null) {
                _gcpStatus.value = "Token OK"
                _syncStatus.value = "Authentification GCP réussie !"
            } else {
                _gcpStatus.value = "Erreur"
                _syncStatus.value = "Échec du renouvellement IAM GCP"
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
    
    fun syncAndProcessVeille(activity: Activity) {
        viewModelScope.launch {
            _syncStatus.value = "Authentification Gmail..."
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
                qualifyArticles()
            }
        }
    }

    private suspend fun startSync(token: String) {
        _syncStatus.value = "Synchronisation des emails..."
        val currentFilter = gmailFilter.value
        val count = gmailRepository.syncEmails(token, currentFilter)
        
        val timeFormat = SimpleDateFormat("'Aujourd''hui à' HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        settingDao.insertSetting(SettingEntity("last_gmail_sync", formattedTime))

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

    fun createNotebook(activity: Activity, theme: String, selectedIds: List<Int>) {
        viewModelScope.launch {
            if (selectedIds.isEmpty()) {
                _syncStatus.value = "Erreur : Aucun article sélectionné"
                return@launch
            }

            _syncStatus.value = "Authentification IAM GCP..."
            val idToken = authManager.getGcpIdToken(activity)
            if (idToken == null) {
                _syncStatus.value = "Échec de l'authentification IAM"
                return@launch
            }

            createThemedNotebookUseCase.execute(idToken, theme, selectedIds).collectLatest { status ->
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
