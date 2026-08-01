package com.olivierbda.omnivigie.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.gson.Gson
import com.olivierbda.omnivigie.BuildConfig
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiQualification(
    val interest: Boolean,
    val themes: List<String>,
    val explanation: String
)

data class PodcastRecommendation(
    val recommendedArticleIds: List<Int>,
    val explanation: String
)


class GeminiRepository {
    private val modelName = BuildConfig.GEMINI_MODEL
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val gson = Gson()

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey
    )

    suspend fun qualifyArticle(
        article: ArticleEntity,
        criteria: String,
        themes: List<String>
    ): AiQualification? = withContext(Dispatchers.IO) {
        val prompt = """
            Tu es un assistant expert en veille technologique spécialisé en Data et Intelligence Artificielle.
            Ta mission est d'évaluer la pertinence d'un article pour un professionnel du domaine.

            CRITÈRES DE VEILLE :
            $criteria

            THÈMES POSSIBLES :
            ${themes.joinToString(", ")}

            ARTICLE À ÉVALUER :
            Titre : ${article.title}
            Résumé : ${article.summary}
            Source : ${article.source}

            INSTRUCTIONS :
            1. Détermine si l'article est intéressant (interest: true/false) selon les critères fournis.
            2. Assigne un ou plusieurs thèmes parmi la liste des thèmes possibles.
            3. Fournis une courte explication (max 2 phrases) justifiant ton choix.
            
            RÉPONDS EXCLUSIVEMENT AU FORMAT JSON SUIVANT :
            {
              "interest": boolean,
              "themes": ["theme1", "theme2"],
              "explanation": "string"
            }
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val jsonResponse = response.text?.let { extractJson(it) }
            if (jsonResponse != null) {
                gson.fromJson(jsonResponse, AiQualification::class.java)
            } else {
                null
            }
        } catch (e: QuotaExceededException) {
            throw e
        } catch (e: Exception) {
            if (isQuotaException(e)) {
                throw e
            }
            e.printStackTrace()
            null
        }
    }

    suspend fun recommendArticlesForPodcast(
        theme: String,
        articles: List<ArticleEntity>
    ): PodcastRecommendation? = withContext(Dispatchers.IO) {
        val articlesFormatted = articles.joinToString("\n---\n") { article ->
            "ID: ${article.id}\nTitre: ${article.title}\nRésumé: ${article.summary}\nSource: ${article.source}"
        }

        val prompt = """
            Tu es un assistant expert en curation de contenu et création de podcasts technologiques.
            
            CONTEXTE ET OBJECTIF :
            Nous voulons créer un podcast audio "Deep Dive" fluide et passionnant sur le thème "$theme".
            Parmi la liste d'articles ci-dessous (tous rattachés au thème "$theme"), trouve entre 5 et 8 articles (ou tous les articles si la liste en compte moins de 5) qui partagent un sujet commun, un fil conducteur fort ou des angles complémentaires particulièrement intéressants à synthétiser ensemble.
            
            LISTE DES ARTICLES CANDIDATS :
            $articlesFormatted

            INSTRUCTIONS :
            1. Analyse les thèmes secondaires et les sujets abordés dans les résumés.
            2. Sélectionne entre 5 et 8 articles qui formeront un épisode de podcast cohérent et percutant.
            3. Rédige une explication claire et synthétique (en français, 2 à 4 phrases) expliquant pourquoi ces articles ont été choisis et quel est leur fil conducteur commun.
            
            RÉPONDS EXCLUSIVEMENT AU FORMAT JSON SUIVANT :
            {
              "recommendedArticleIds": [12, 15, 18, 22, 30],
              "explanation": "Ces articles ont été sélectionnés car ils abordent tous..."
            }
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val jsonResponse = response.text?.let { extractJson(it) }
            if (jsonResponse != null) {
                gson.fromJson(jsonResponse, PodcastRecommendation::class.java)
            } else {
                null
            }
        } catch (e: QuotaExceededException) {
            throw e
        } catch (e: Exception) {
            if (isQuotaException(e)) {
                throw e
            }
            e.printStackTrace()
            null
        }
    }

    private fun isQuotaException(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val className = current.javaClass.name
            val message = current.message ?: ""
            if (current is QuotaExceededException ||
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

    private fun extractJson(text: String): String? {
        val start = text.indexOf("{")
        val end = text.lastIndexOf("}")
        return if (start != -1 && end != -1 && end > start) {
            text.substring(start, end + 1)
        } else {
            null
        }
    }
}
