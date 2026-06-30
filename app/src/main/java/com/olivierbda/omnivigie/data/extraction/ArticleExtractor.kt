package com.olivierbda.omnivigie.data.extraction

import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import org.jsoup.Jsoup

class ArticleExtractor {

    fun extractArticles(email: EmailEntity): List<ArticleEntity> {
        val doc = Jsoup.parse(email.bodyHtml)
        val articles = mutableListOf<ArticleEntity>()

        // TLDR strategy: Find all <a> tags
        val links = doc.select("a")
        
        for (link in links) {
            val title = link.text().trim()
            val rawUrl = link.attr("href")
            val url = cleanUrl(rawUrl)
            
            if (isValidArticle(title, url)) {
                val parent = link.parent() ?: continue
                val textContent = parent.text()
                
                // Extract reading time (e.g. "5 minute read")
                val readingTimeRegex = """(\d+\s+min(ute)?\s+read)""".toRegex(RegexOption.IGNORE_CASE)
                val readingTimeMatch = readingTimeRegex.find(textContent)
                val readingTime = readingTimeMatch?.value ?: "N/A"
                
                // Summary extraction
                var summary = textContent.replace(title, "").trim()
                if (readingTime != "N/A") {
                    summary = summary.replace(readingTime, "").trim()
                }
                summary = summary.removePrefix("(").removeSuffix(")").trim()

                val isSponsor = title.contains("Sponsor", ignoreCase = true) || 
                               textContent.contains("Sponsor", ignoreCase = true)

                articles.add(
                    ArticleEntity(
                        emailId = email.id,
                        title = title,
                        url = url,
                        source = email.sender,
                        readingTime = readingTime,
                        summary = summary.take(500),
                        isSponsor = isSponsor
                    )
                )
            }
        }

        return articles.distinctBy { it.url }
    }

    private fun isValidArticle(title: String, url: String): Boolean {
        if (title.isBlank() || title.length < 5) return false
        if (!url.startsWith("http")) return false
        
        val excludeKeywords = listOf("unsubscribe", "view in browser", "twitter", "linkedin", "facebook", "privacy policy")
        if (excludeKeywords.any { title.contains(it, ignoreCase = true) }) return false
        
        return true
    }

    private fun cleanUrl(url: String): String {
        return try {
            // Manual cleaning to avoid Android Uri dependency in unit tests
            if (url.contains("?")) {
                url.substringBefore("?")
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }
}
