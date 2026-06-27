package com.olivierbda.omnivigie

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.olivierbda.omnivigie.data.local.OmnivigieDatabase
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.local.dao.EmailDao
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var emailDao: EmailDao
    private lateinit var articleDao: ArticleDao
    private lateinit var db: OmnivigieDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, OmnivigieDatabase::class.java
        ).build()
        emailDao = db.emailDao()
        articleDao = db.articleDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeEmailAndReadInList() = runBlocking {
        val email = EmailEntity(
            id = "msg123",
            receivedDate = System.currentTimeMillis(),
            sender = "tldr@tldrnewsletter.com",
            subject = "TLDR AI 2026-06-27",
            bodyHtml = "<html><body>Some content</body></html>"
        )
        emailDao.insertEmail(email)
        val allEmails = emailDao.getAllEmails().first()
        assertEquals(allEmails[0].id, email.id)
    }

    @Test
    @Throws(Exception::class)
    fun writeArticleAndReadInList() = runBlocking {
        val email = EmailEntity(
            id = "msg123",
            receivedDate = System.currentTimeMillis(),
            sender = "tldr@tldrnewsletter.com",
            subject = "TLDR AI 2026-06-27",
            bodyHtml = "<html><body>Some content</body></html>"
        )
        emailDao.insertEmail(email)

        val article = ArticleEntity(
            emailId = "msg123",
            title = "Gemini 2.5 Released",
            url = "https://google.com/gemini",
            source = "Google Blog",
            readingTime = "5 min",
            summary = "New Gemini features.",
            isSponsor = false,
            aiInterest = true,
            aiThemes = listOf("AI", "LLM")
        )
        articleDao.insertArticle(article)
        
        val allArticles = articleDao.getAllArticles().first()
        assertNotNull(allArticles)
        assertEquals(allArticles.size, 1)
        assertEquals(allArticles[0].title, "Gemini 2.5 Released")
        assertEquals(allArticles[0].aiThemes.size, 2)
    }
}
