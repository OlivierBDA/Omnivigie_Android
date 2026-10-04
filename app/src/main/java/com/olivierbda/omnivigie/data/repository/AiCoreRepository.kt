package com.olivierbda.omnivigie.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class AiCoreStatus {
    object Available : AiCoreStatus()
    object Downloadable : AiCoreStatus()
    object Downloading : AiCoreStatus()
    object Unavailable : AiCoreStatus()
    data class Error(val message: String) : AiCoreStatus()
}

class AiCoreRepository {
    private val gson = Gson()
    private var generativeModel: GenerativeModel? = null

    @Synchronized
    fun getClient(): GenerativeModel {
        if (generativeModel == null) {
            val config = generationConfig {
                modelConfig = modelConfig {
                    releaseStage = ModelReleaseStage.PREVIEW
                    preference = ModelPreference.FAST
                }
            }
            generativeModel = Generation.getClient(config)
        }
        return generativeModel!!
    }

    suspend fun checkStatus(): AiCoreStatus = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            when (client.checkStatus()) {
                FeatureStatus.AVAILABLE -> AiCoreStatus.Available
                FeatureStatus.DOWNLOADABLE -> AiCoreStatus.Downloadable
                FeatureStatus.DOWNLOADING -> AiCoreStatus.Downloading
                FeatureStatus.UNAVAILABLE -> AiCoreStatus.Unavailable
                else -> AiCoreStatus.Unavailable
            }
        } catch (e: Exception) {
            Log.e("AiCoreRepository", "checkStatus failed", e)
            AiCoreStatus.Error(e.localizedMessage ?: e.message ?: "Erreur AICore")
        }
    }

    fun downloadModel(): Flow<DownloadStatus> {
        val client = getClient()
        return client.download()
    }

    suspend fun testConnection(): String = withContext(Dispatchers.Default) {
        val client = getClient()
        val status = checkStatus()
        if (status !is AiCoreStatus.Available) {
            throw IllegalStateException("Modèle AICore non prêt (Statut: $status)")
        }

        val prompt = "Réponds brièvement en une phrase : Je suis Gemini Nano 4 Fast et je fonctionne en local sur le TPU."
        val request = generateContentRequest(TextPart(prompt)) {
            temperature = 0.1f
        }
        val response = client.generateContent(request)
        response.candidates.firstOrNull()?.text?.trim()
            ?: "Connexion TPU réussie (réponse vide)."
    }

    suspend fun qualifyArticle(
        article: ArticleEntity,
        criteria: String,
        themes: List<String>
    ): AiQualification? = withContext(Dispatchers.Default) {
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
            val client = getClient()
            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.1f
            }
            val response = client.generateContent(request)
            val responseText = response.candidates.firstOrNull()?.text
            val jsonResponse = responseText?.let { extractJson(it) }
            if (jsonResponse != null) {
                gson.fromJson(jsonResponse, AiQualification::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AiCoreRepository", "AICore qualifyArticle failed", e)
            throw e
        }
    }

    private fun extractJson(text: String): String? {
        val trimmed = text.trim()
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            null
        }
    }

    fun close() {
        try {
            generativeModel?.close()
            generativeModel = null
        } catch (e: Exception) {
            Log.w("AiCoreRepository", "Error closing GenerativeModel", e)
        }
    }
}
