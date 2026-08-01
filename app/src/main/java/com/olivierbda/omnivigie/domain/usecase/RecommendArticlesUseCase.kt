package com.olivierbda.omnivigie.domain.usecase

import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.repository.GeminiRepository
import com.olivierbda.omnivigie.data.repository.PodcastRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RecommendationResult {
    object Idle : RecommendationResult()
    object Loading : RecommendationResult()
    data class Success(val recommendation: PodcastRecommendation) : RecommendationResult()
    data class Error(val message: String) : RecommendationResult()
}

class RecommendArticlesUseCase(
    private val geminiRepository: GeminiRepository
) {
    suspend fun execute(
        theme: String,
        articles: List<ArticleEntity>
    ): RecommendationResult = withContext(Dispatchers.IO) {
        if (articles.isEmpty()) {
            return@withContext RecommendationResult.Error("Aucun article disponible dans ce thème.")
        }

        if (articles.size <= 5) {
            val allIds = articles.map { it.id }
            return@withContext RecommendationResult.Success(
                PodcastRecommendation(
                    recommendedArticleIds = allIds,
                    explanation = "Le thème contient ${articles.size} article(s) (≤ 5). Tous les articles ont été automatiquement sélectionnés."
                )
            )
        }

        try {
            val result = geminiRepository.recommendArticlesForPodcast(theme, articles)
            if (result != null && result.recommendedArticleIds.isNotEmpty()) {
                RecommendationResult.Success(result)
            } else {
                RecommendationResult.Error("L'IA n'a pas pu générer de recommandation valide.")
            }
        } catch (e: Exception) {
            val isQuota = e.message?.contains("429") == true || 
                          e.message?.contains("Quota") == true || 
                          e is com.google.ai.client.generativeai.type.QuotaExceededException
            val errorMsg = if (isQuota) {
                "Quota API Gemini dépassé. Veuillez réessayer ultérieurement."
            } else {
                "Erreur lors de la recommandation d'articles : ${e.localizedMessage}"
            }
            RecommendationResult.Error(errorMsg)
        }
    }
}
