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

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}
