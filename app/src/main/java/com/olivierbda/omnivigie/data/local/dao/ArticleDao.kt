package com.olivierbda.omnivigie.data.local.dao

import androidx.room.*
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("SELECT * FROM articles ORDER BY id DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE aiInterest = 1 ORDER BY id DESC")
    fun getInterestingArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE emailId = :emailId")
    suspend fun getArticlesByEmail(emailId: String): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE isQualified = 0 AND isSponsor = 0")
    suspend fun getUnqualifiedArticles(): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles WHERE isQualified = 0 AND isSponsor = 0")
    fun getUnqualifiedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE aiInterest = 1 AND isSentToNotebook = 0")
    fun getPendingQualifiedCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT notebookName) FROM articles WHERE isSentToNotebook = 1 AND notebookName IS NOT NULL")
    fun getNotebooksCount(): Flow<Int>

    @Query("SELECT * FROM articles WHERE isSentToNotebook = 1 AND notebookName IS NOT NULL ORDER BY id DESC")
    fun getProcessedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isSentToNotebook = 0 ORDER BY id DESC")
    fun getAllUnsentArticles(): Flow<List<ArticleEntity>>

    @Query("DELETE FROM articles WHERE aiInterest = 0 OR isSentToNotebook = 1 OR aiExplanation LIKE '%trop court%' OR aiExplanation LIKE '%Publicité%'")
    suspend fun cleanupArticles()

    @Query("SELECT * FROM articles WHERE aiInterest = 1 AND isSentToNotebook = 0 AND aiThemes LIKE :themePattern ORDER BY id DESC")
    suspend fun getInterestingPendingArticlesByTheme(themePattern: String): List<ArticleEntity>

    @Query("UPDATE articles SET isSentToNotebook = 1, notebookId = :notebookId, notebookName = :notebookName WHERE id IN (:articleIds)")
    suspend fun markArticlesAsProcessedInNotebook(
        articleIds: List<Int>,
        notebookId: String,
        notebookName: String
    )

    @Query("SELECT * FROM articles WHERE id IN (:articleIds)")
    suspend fun getArticlesByIds(articleIds: List<Int>): List<ArticleEntity>

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}
