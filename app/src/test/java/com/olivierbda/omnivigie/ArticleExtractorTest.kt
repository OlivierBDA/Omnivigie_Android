package com.olivierbda.omnivigie

import com.olivierbda.omnivigie.data.extraction.ArticleExtractor
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleExtractorTest {

    @Test
    fun testExtractArticlesFromTldrHtml() {
        val extractor = ArticleExtractor()
        val html = """
            <div class="text-block">
                <h1><strong>Headlines & Launches</strong></h1>
            </div>
            <div class="text-block">
                <a href="https://example.com/ai-news?utm_source=tldr">
                    <strong>Gemini 2.5 Flash (5 minute read)</strong>
                </a>
                <br><br>
                <span style="font-family: 'Helvetica'">Google just released a new version of Gemini that is 10x faster.</span>
            </div>
            <div class="text-block">
                <a href="https://example.com/sponsor">
                    <strong>Amazing AI Tool (Sponsor)</strong>
                </a>
                <br><br>
                <span style="font-family: 'Helvetica'">This sponsored tool will change your life.</span>
            </div>
        """.trimIndent()

        val email = EmailEntity(
            id = "test_id",
            receivedDate = System.currentTimeMillis(),
            sender = "dan@tldrnewsletter.com",
            subject = "TLDR AI",
            bodyHtml = html
        )

        val articles = extractor.extractArticles(email)

        // Verify extraction
        assertEquals(2, articles.size)
        
        // Verify First Article
        val first = articles[0]
        assertEquals("Gemini 2.5 Flash", first.title)
        assertEquals("https://example.com/ai-news", first.url)
        assertEquals("5 minute read", first.readingTime)
        assertEquals("Google just released a new version of Gemini that is 10x faster.", first.summary)
        assertEquals(false, first.isSponsor)

        // Verify Sponsor
        val second = articles[1]
        assertEquals("Amazing AI Tool", second.title)
        assertEquals(true, second.isSponsor)
    }
}
