package com.olivierbda.omnivigie.domain.usecase

import android.util.Log
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateThemedNotebookUseCase(
    private val notebookRepository: NotebookLmRepository,
    private val articleDao: ArticleDao
) {
    private val TAG = "CreateThemedNotebook"

    fun execute(theme: String, articleIds: List<Int>): Flow<String> = flow {
        if (articleIds.isEmpty()) {
            emit("Erreur : Aucun article sélectionné.")
            return@flow
        }

        // 1. Récupération des articles par IDs directement
        val pendingArticles = articleDao.getArticlesByIds(articleIds)

        if (pendingArticles.isEmpty()) {
            emit("Erreur : Les articles sélectionnés ne sont plus disponibles.")
            return@flow
        }

        // 2. Formatage du nom officiel du Notebook
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notebookName = "[AI] $dateString TLDR-$theme"

        emit("Création du carnet : $notebookName...")
        Log.d(TAG, "Lancement de la création du carnet : $notebookName avec ${pendingArticles.size} articles.")

        // 3. Appel à NotebookLM pour la création du carnet de notes
        val notebookId = notebookRepository.createNotebook(notebookName)
        if (notebookId == null) {
            emit("Échec de la création du carnet (vérifiez votre connexion).")
            return@flow
        }

        // 4. Ajout itératif des sources URL dans le carnet
        val successProcessedIds = mutableListOf<Int>()
        pendingArticles.forEachIndexed { index, article ->
            emit("Ajout source ${index + 1}/${pendingArticles.size} : ${article.title}")
            
            val isAdded = notebookRepository.addUrlSource(
                notebookId = notebookId,
                title = article.title,
                url = article.url
            )
            if (isAdded) {
                successProcessedIds.add(article.id)
            } else {
                Log.e(TAG, "Échec de l'ajout de la source : ${article.title}")
            }
        }

        // 5. Mise à jour de l'état local en base Room
        if (successProcessedIds.isNotEmpty()) {
            emit("Mise à jour de la base de données...")
            articleDao.markArticlesAsProcessedInNotebook(
                articleIds = successProcessedIds,
                notebookId = notebookId,
                notebookName = notebookName
            )
            emit("Terminé ! Carnet créé avec ${successProcessedIds.size} sources.")
        } else {
            emit("Erreur : Aucun article n'a pu être injecté dans le carnet.")
        }
    }
}
