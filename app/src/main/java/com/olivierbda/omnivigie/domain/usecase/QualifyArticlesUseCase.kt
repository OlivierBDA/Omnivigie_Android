package com.olivierbda.omnivigie.domain.usecase

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

import com.olivierbda.omnivigie.data.local.dao.SettingDao
import com.olivierbda.omnivigie.data.repository.AiCoreRepository
import com.olivierbda.omnivigie.data.repository.AiCoreStatus

class QualifyArticlesUseCase(
    private val context: Context,
    private val articleDao: ArticleDao,
    private val settingDao: SettingDao,
    private val geminiRepository: GeminiRepository,
    private val aiCoreRepository: AiCoreRepository
) {
    private val gson = Gson()

    fun execute(): Flow<String> = flow {
        emit("Récupération des critères...")
        val customCriteria = withContext(Dispatchers.IO) { settingDao.getSettingValue("qualification_criteria") }
        val criteria = if (!customCriteria.isNullOrBlank()) customCriteria else readAsset("criteria.md")

        val customThemesJson = withContext(Dispatchers.IO) { settingDao.getSettingValue("qualification_themes") }
        val themes: List<String> = if (!customThemesJson.isNullOrBlank()) {
            try {
                gson.fromJson(customThemesJson, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                gson.fromJson(readAsset("themes.json"), object : TypeToken<List<String>>() {}.type)
            }
        } else {
            gson.fromJson(readAsset("themes.json"), object : TypeToken<List<String>>() {}.type)
        }

        val customMinReadingTime = withContext(Dispatchers.IO) { settingDao.getSettingValue("min_reading_time") }
        val minReadingTime = customMinReadingTime?.toIntOrNull() ?: 5

        val customRetentionDays = withContext(Dispatchers.IO) { settingDao.getSettingValue("article_retention_days") }
        val retentionDays = customRetentionDays?.toIntOrNull() ?: 30

        if (retentionDays > 0) {
            val cutoffTimestamp = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
            val oldArticles = withContext(Dispatchers.IO) {
                articleDao.getArticlesOlderThan(cutoffTimestamp)
            }
            if (oldArticles.isNotEmpty()) {
                emit("Exclusion de ${oldArticles.size} articles trop anciens (> $retentionDays jours)...")
                val updatedOldArticles = oldArticles.map {
                    it.copy(
                        aiInterest = false,
                        aiThemes = listOf("Exclus"),
                        aiExplanation = "Article trop ancien (> $retentionDays jours).",
                        isQualified = true
                    )
                }
                withContext(Dispatchers.IO) {
                    articleDao.updateArticles(updatedOldArticles)
                }
            }
        }

        val articles = withContext(Dispatchers.IO) {
            articleDao.getUnqualifiedArticles()
        }
        
        if (articles.isEmpty()) {
            emit("Aucun article à qualifier.")
            return@flow
        }

        // Determine AI engine (Local AICore TPU vs Cloud Gemini API)
        val engineSetting = withContext(Dispatchers.IO) { settingDao.getSettingValue("classification_engine") } ?: "aicore"
        var useAiCore = false

        if (engineSetting == "aicore") {
            emit("Vérification d'Android AICore (TPU Pixel 10 Pro)...")
            val status = aiCoreRepository.checkStatus()
            when (status) {
                is AiCoreStatus.Available -> {
                    useAiCore = true
                    emit("Classification locale sur TPU Pixel 10 Pro (Gemini Nano 4 Fast)")
                }
                is AiCoreStatus.Downloadable -> {
                    emit("AICore : Modèle à télécharger -> Bascule automatique vers l'API Gemini Cloud")
                }
                is AiCoreStatus.Downloading -> {
                    emit("AICore : Téléchargement en cours -> Bascule automatique vers l'API Gemini Cloud")
                }
                is AiCoreStatus.Unavailable -> {
                    emit("AICore : Non disponible sur l'appareil -> Bascule automatique vers l'API Gemini Cloud")
                }
                is AiCoreStatus.Error -> {
                    emit("AICore indisponible (${status.message}) -> Bascule automatique vers l'API Gemini Cloud")
                }
            }
        } else {
            emit("Classification distante via Google Gemini Cloud API...")
        }

        emit("Qualification de ${articles.size} articles...")

        articles.forEachIndexed { index, article ->
            emit("Analyse article ${index + 1}/${articles.size} : ${article.title}")

            // Level 1 Pre-filtering: exclude if reading time < minReadingTime min, N/A, blank, or sponsor
            val readingTimeValue = extractMinutes(article.readingTime)
            val isNaOrEmpty = article.readingTime.isBlank() || 
                              article.readingTime.contains("N/A", ignoreCase = true) || 
                              readingTimeValue == null
            
            val updatedArticle = if (isNaOrEmpty || article.isSponsor) {
                article.copy(
                    aiInterest = false,
                    aiThemes = listOf("Exclus"),
                    aiExplanation = if (article.isSponsor) "Publicité ou contenu sponsorisé." else "Article sans temps de lecture (N/A) / publicité.",
                    isQualified = true
                )
            } else if (minReadingTime > 0 && readingTimeValue < minReadingTime) {
                article.copy(
                    aiInterest = false,
                    aiThemes = listOf("Exclus"),
                    aiExplanation = "Article trop court (< $minReadingTime min).",
                    isQualified = true
                )
            } else {
                // Level 2: AI Qualification (AICore Local TPU with fallback to Gemini Cloud)
                try {
                    val qualification = if (useAiCore) {
                        try {
                            aiCoreRepository.qualifyArticle(article, criteria, themes)
                        } catch (e: Exception) {
                            emit("Erreur TPU sur cet article -> Fallback API Gemini Cloud...")
                            geminiRepository.qualifyArticle(article, criteria, themes)
                        }
                    } else {
                        geminiRepository.qualifyArticle(article, criteria, themes)
                    }

                    if (qualification != null) {
                        val isInteresting = qualification.interest
                        val assignedThemes = if (!isInteresting || qualification.themes.isEmpty()) {
                            listOf("Exclus")
                        } else {
                            qualification.themes
                        }
                        article.copy(
                            aiInterest = isInteresting,
                            aiThemes = assignedThemes,
                            aiExplanation = qualification.explanation,
                            isQualified = true
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    if (isQuotaException(e)) {
                        emit("Qualification interrompue (quota API LLM atteint)")
                        return@flow
                    }
                    null
                }
            }

            if (updatedArticle != null) {
                withContext(Dispatchers.IO) {
                    articleDao.updateArticle(updatedArticle)
                }
            }
        }
        
        emit("Qualification terminée.")
    }.flowOn(Dispatchers.IO)

    private fun isQuotaException(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val className = current.javaClass.name
            val message = current.message ?: ""
            if (current is com.google.ai.client.generativeai.type.QuotaExceededException ||
                className.contains("QuotaExceededException", ignoreCase = true) ||
                message.contains("Quota exceeded", ignoreCase = true) ||
                message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("429", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun readAsset(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun extractMinutes(readingTime: String): Int? {
        val pattern = Pattern.compile("(\\d+)")
        val matcher = pattern.matcher(readingTime)
        return if (matcher.find()) {
            matcher.group(1)?.toIntOrNull()
        } else {
            null
        }
    }
}
