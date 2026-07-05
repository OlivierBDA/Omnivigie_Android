package com.olivierbda.omnivigie.domain.usecase

import android.util.Log  
import com.olivierbda.omnivigie.data.local.dao.ArticleDao  
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository  
import kotlinx.coroutines.Dispatchers  
import kotlinx.coroutines.withContext  
import java.text.SimpleDateFormat  
import java.util.Date  
import java.util.Locale

class CreateThemedNotebookUseCase(  
    private val notebookRepository: NotebookLmRepository,  
    private val articleDao: ArticleDao  
) {  
    private val TAG = "CreateThemedNotebook"

    suspend fun execute(theme: String): Result<String> = withContext(Dispatchers.IO) {  
        // 1. Recherche des articles qualifiés en local  
        val pattern = "%$theme%"  
        val pendingArticles = articleDao.getInterestingPendingArticlesByTheme(pattern)

        if (pendingArticles.isEmpty()) {  
            return@withContext Result.failure(Exception("Aucun article pertinent et non traité pour le thème '$theme'"))  
        }

        // 2. Formatage du nom officiel du Notebook  
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())  
        val notebookName = "[AI] $dateString TLDR-$theme"

        Log.d(TAG, "Lancement de la création du carnet : $notebookName avec ${pendingArticles.size} articles.")

        // 3. Appel à NotebookLM pour la création du carnet de notes  
        val notebookId = notebookRepository.createNotebook(notebookName)  
            ?: return@withContext Result.failure(Exception("Échec de l'initialisation du carnet auprès de NotebookLM (problème réseau ou session expirée)."))

        // 4. Ajout itératif des sources URL dans le carnet  
        val successProcessedIds = mutableListOf<Int>()  
        for (article in pendingArticles) {  
            val isAdded = notebookRepository.addUrlSource(  
                notebookId = notebookId,  
                title = article.title,  
                url = article.url  
            )  
            if (isAdded) {  
                // Utilisation de l'id auto-généré de l'entité Room  
                successProcessedIds.add(article.id)  
            }  
        }

        // 5. Mise à jour de l'état local en base Room  
        if (successProcessedIds.isNotEmpty()) {  
            articleDao.markArticlesAsProcessedInNotebook(  
                articleIds = successProcessedIds,  
                notebookId = notebookId,  
                notebookName = notebookName  
            )  
            Log.d(TAG, "DB locale mise à jour. ${successProcessedIds.size} articles marqués traités.")  
        } else {  
            return@withContext Result.failure(Exception("Le carnet a été créé mais aucun article n'a pu y être injecté en tant que source."))  
        }

        Result.success(notebookName)  
    }  
}
