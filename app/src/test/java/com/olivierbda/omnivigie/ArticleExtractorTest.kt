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
            <html>
                <body>
                    <div>
                        <p>
                            <a href="https://example.com/ai-news?utm_source=tldr">Gemini 2.5 Flash</a> 
                            (5 minute read) 
                            Google just released a new version of Gemini that is 10x faster.
                        </p>
                    </div>
                    <div>
                        <p>
                            <a href="https://example.com/sponsor">Amazing AI Tool [Sponsor]</a> 
                            This tool will change your life.
                        </p>
                    </div>
                    <div>
                        <a href="https://twitter.com/tldr">Follow us on Twitter</a>
                    </div>
                </body>
            </html>
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
        assertEquals("https://example.com/ai-news", first.url) // UTM stripped
        assertEquals("5 minute read", first.readingTime)
        assertTrue(first.summary.contains("Google just released"))
        assertEquals(false, first.isSponsor)

        // Verify Sponsor
        val second = articles[1]
        assertEquals("Amazing AI Tool [Sponsor]", second.title)
        assertEquals(true, second.isSponsor)
    }
}
