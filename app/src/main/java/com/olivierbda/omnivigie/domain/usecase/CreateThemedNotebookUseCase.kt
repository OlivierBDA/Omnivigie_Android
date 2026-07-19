package com.olivierbda.omnivigie.domain.usecase

import android.util.Log
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateThemedNotebookUseCase(
    private val notebookRepository: NotebookLmRepository,
    private val articleDao: ArticleDao
) {
    private val TAG = "CreateThemedNotebook"

    fun execute(idToken: String, theme: String, articleIds: List<Int>): Flow<String> = flow {
        if (articleIds.isEmpty()) {
            emit("Erreur : Aucun article sélectionné.")
            return@flow
        }

        // 1. Récupération des articles par IDs directement
        val pendingArticles = withContext(Dispatchers.IO) {
            articleDao.getArticlesByIds(articleIds)
        }

        if (pendingArticles.isEmpty()) {
            emit("Erreur : Les articles sélectionnés ne sont plus disponibles.")
            return@flow
        }

        // 2. Formatage du nom officiel du Notebook
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notebookName = "[AI] $dateString TLDR-$theme"

        emit("Création du carnet via GCP Backend...")
        
        // 3. Appel au Backend pour la création du carnet
        val notebookId = withContext(Dispatchers.IO) {
            notebookRepository.createNotebook(idToken, notebookName)
        }
        
        if (notebookId == null) {
            emit("Échec de la création du carnet sur le backend GCP.")
            return@flow
        }

        // 4. Ajout en BATCH des sources URL (Optimisé pour le Backend)
        val urls = pendingArticles.map { it.url }
        emit("Ajout de ${urls.size} sources (Batch)...")
        
        val success = withContext(Dispatchers.IO) {
            notebookRepository.addUrlsBatch(idToken, notebookId, urls)
        }

        if (!success) {
            emit("Erreur lors de l'ajout des sources via le backend.")
            return@flow
        }

        // 5. Attente pour l'indexation par NotebookLM
        emit("Attente de l'indexation (30s)...")
        delay(30000)

        // 6. Déclenchement de la génération du Podcast
        emit("Lancement de la génération du podcast...")
        val podcastStarted = withContext(Dispatchers.IO) {
            notebookRepository.generatePodcast(idToken, notebookId)
        }

        // 7. Mise à jour de l'état local en base Room
        if (podcastStarted) {
            emit("Mise à jour de la base de données...")
            withContext(Dispatchers.IO) {
                articleDao.markArticlesAsProcessedInNotebook(
                    articleIds = articleIds,
                    notebookId = notebookId,
                    notebookName = notebookName
                )
            }
            emit("Terminé ! Carnet créé et podcast lancé.")
        } else {
            emit("Le carnet est créé mais la génération du podcast a échoué.")
        }
    }.flowOn(Dispatchers.IO)
}
