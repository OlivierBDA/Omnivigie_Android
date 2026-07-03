package com.olivierbda.omnivigie.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
