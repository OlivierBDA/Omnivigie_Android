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
            <table>
                <tr>
                    <td>
                        <a href="https://example.com/ai-news?utm_source=tldr"><strong>Gemini 2.5 Flash (5 minute read)</strong></a>
                        <br><br>
                        <span>Google just released a new version of Gemini that is 10x faster and better.</span>
                    </td>
                </tr>
                <tr>
                    <td>
                        <a href="https://example.com/sponsor"><strong>Amazing AI Tool (Sponsor)</strong></a>
                        <br><br>
                        <span>This sponsored tool will change your life with automation.</span>
                    </td>
                </tr>
                <tr>
                    <td>
                        <a href="https://twitter.com/tldr">Follow us on Twitter</a>
                    </td>
                </tr>
            </table>
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
        assertTrue(first.summary.startsWith("Google just released"))
        assertEquals(false, first.isSponsor)

        // Verify Sponsor
        val second = articles[1]
        assertEquals("Amazing AI Tool", second.title)
        assertEquals(true, second.isSponsor)
        assertTrue(second.summary.contains("sponsored tool"))
    }
}
