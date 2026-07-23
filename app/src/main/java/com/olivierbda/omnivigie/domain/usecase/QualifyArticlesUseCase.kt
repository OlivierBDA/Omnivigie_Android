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

class QualifyArticlesUseCase(
    private val context: Context,
    private val articleDao: ArticleDao,
    private val settingDao: SettingDao,
    private val geminiRepository: GeminiRepository
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

        val articles = withContext(Dispatchers.IO) {
            articleDao.getUnqualifiedArticles()
        }
        
        if (articles.isEmpty()) {
            emit("Aucun article à qualifier.")
            return@flow
        }

        emit("Qualification de ${articles.size} articles...")

        articles.forEachIndexed { index, article ->
            emit("Analyse article ${index + 1}/${articles.size} : ${article.title}")

            // Pre-filtering: exclude if reading time < minReadingTime min or N/A
            val readingTimeValue = extractMinutes(article.readingTime)
            
            val updatedArticle = if (article.readingTime.contains("N/A", ignoreCase = true)) {
                article.copy(
                    aiInterest = false,
                    aiExplanation = "Publicité ou contenu non qualifié (N/A).",
                    isQualified = true
                )
            } else if (readingTimeValue != null && minReadingTime > 0 && readingTimeValue < minReadingTime) {
                article.copy(
                    aiInterest = false,
                    aiExplanation = "Article trop court (< $minReadingTime min).",
                    isQualified = true
                )
            } else {
                val qualification = geminiRepository.qualifyArticle(article, criteria, themes)
                if (qualification != null) {
                    article.copy(
                        aiInterest = qualification.interest,
                        aiThemes = qualification.themes,
                        aiExplanation = qualification.explanation,
                        isQualified = true
                    )
                } else {
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
