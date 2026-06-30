package com.olivierbda.omnivigie.data.repository

import android.util.Base64
import android.util.Log
import com.olivierbda.omnivigie.data.extraction.ArticleExtractor
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.local.dao.EmailDao
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import com.olivierbda.omnivigie.data.remote.GmailService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GmailRepository(
    private val emailDao: EmailDao,
    private val articleDao: ArticleDao
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gmail.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val gmailService = retrofit.create(GmailService::class.java)
    private val articleExtractor = ArticleExtractor()

    suspend fun syncEmails(accessToken: String, fullQuery: String): Int {
        var count = 0
        try {
            val authHeader = "Bearer $accessToken"
            Log.d("GmailRepository", "Syncing with query: $fullQuery")
            
            val response = gmailService.listMessages(authHeader, query = fullQuery)
            val messages = response.messages ?: emptyList()

            for (msgSummary in messages) {
                val existing = emailDao.getEmailById(msgSummary.id)
                if (existing == null) {
                    val msg = gmailService.getMessage(authHeader, id = msgSummary.id)
                    val emailEntity = mapToEntity(msg)
                    
                    emailDao.insertEmail(emailEntity)
                    
                    val articles = articleExtractor.extractArticles(emailEntity)
                    if (articles.isNotEmpty()) {
                        articleDao.insertArticles(articles)
                    }
                    
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e("GmailRepository", "Sync failed", e)
        }
        return count
    }

    private fun mapToEntity(msg: com.olivierbda.omnivigie.data.remote.GmailMessage): EmailEntity {
        val headers = msg.payload?.headers ?: emptyList()
        val subject = headers.find { it.name == "Subject" }?.value ?: "(No Subject)"
        val sender = headers.find { it.name == "From" }?.value ?: "Unknown"
        val receivedDate = msg.internalDate ?: System.currentTimeMillis()
        
        val htmlBody = findHtmlPart(msg.payload) ?: ""
        
        return EmailEntity(
            id = msg.id,
            receivedDate = receivedDate,
            sender = sender,
            subject = subject,
            bodyHtml = htmlBody
        )
    }

    private fun findHtmlPart(payload: com.olivierbda.omnivigie.data.remote.GmailPayload?): String? {
        if (payload == null) return null
        
        if (payload.mimeType == "text/html" && payload.body?.data != null) {
            return try {
                String(Base64.decode(payload.body.data, Base64.URL_SAFE))
            } catch (e: Exception) {
                ""
            }
        }
        
        payload.parts?.forEach { part ->
            val result = findHtmlPart(part)
            if (result != null) return result
        }
        
        return null
    }
}
