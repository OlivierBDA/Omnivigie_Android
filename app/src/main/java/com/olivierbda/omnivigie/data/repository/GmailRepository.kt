package com.olivierbda.omnivigie.data.repository

import android.util.Base64
import android.util.Log
import com.olivierbda.omnivigie.data.local.dao.EmailDao
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import com.olivierbda.omnivigie.data.remote.GmailService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GmailRepository(private val emailDao: EmailDao) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gmail.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val gmailService = retrofit.create(GmailService::class.java)

    suspend fun syncEmails(accessToken: String, dateFilter: String): Int {
        var count = 0
        try {
            val authHeader = "Bearer $accessToken"
            // Example query: "from:dan@tldrnewsletter.com after:2026/06/24"
            val query = "from:dan@tldrnewsletter.com OR from:tldr@tldrnewsletter.com after:$dateFilter"
            
            val response = gmailService.listMessages(authHeader, query = query)
            val messages = response.messages ?: emptyList()

            for (msgSummary in messages) {
                val existing = emailDao.getEmailById(msgSummary.id)
                if (existing == null) {
                    val msg = gmailService.getMessage(authHeader, id = msgSummary.id)
                    val emailEntity = mapToEntity(msg)
                    emailDao.insertEmail(emailEntity)
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
        
        // Find HTML part
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
            return String(Base64.decode(payload.body.data, Base64.URL_SAFE))
        }
        
        payload.parts?.forEach { part ->
            val result = findHtmlPart(part)
            if (result != null) return result
        }
        
        return null
    }
}
