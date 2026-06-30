package com.olivierbda.omnivigie.data.extraction

import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import org.jsoup.Jsoup

class ArticleExtractor {

    fun extractArticles(email: EmailEntity): List<ArticleEntity> {
        val doc = Jsoup.parse(email.bodyHtml)
        val articles = mutableListOf<ArticleEntity>()

        // Strategy based on user feedback and sample analysis:
        // 1. Find all <a> tags that are inside <strong> tags (titles are bold links)
        // 2. Title must end with something in parentheses (e.g., "(1 minute read)", "(Sponsor)")
        // 3. Summary is the text in the next available block (usually after some <br> or in a sibling span/div)
        
        val boldLinks = doc.select("strong a, a strong")
        
        for (element in boldLinks) {
            val titleElement = if (element.tagName() == "strong") element.selectFirst("a") else element
            if (titleElement == null) continue
            
            val fullTitleText = titleElement.text().trim()
            val url = cleanUrl(titleElement.attr("href"))
            
            // Check if title ends with parentheses (Reading time or Sponsor)
            val parenthesesRegex = """\(.*?\)$""".toRegex()
            if (!parenthesesRegex.containsMatchIn(fullTitleText)) continue

            // Split title and metadata (reading time/sponsor)
            val metadata = parenthesesRegex.find(fullTitleText)?.value ?: ""
            val cleanTitle = fullTitleText.replace(metadata, "").trim()
            
            // Extraction of reading time and sponsor flag
            val readingTime = if (metadata.contains("read", ignoreCase = true)) {
                metadata.removePrefix("(").removeSuffix(")")
            } else {
                "N/A"
            }
            val isSponsor = metadata.contains("Sponsor", ignoreCase = true)

            // Find summary: It's usually in a following <span> or after <br>s within the same container
            // In the provided sample, TLDR often uses:
            // <a><strong>Title (Time)</strong></a><br><br><span>Summary</span>
            
            var summary = ""
            
            // Look for the summary in siblings
            var nextSibling = element.parent()?.nextElementSibling() 
            if (nextSibling == null) {
                // Try to find text after the link in the same parent if structure varies
                val parentText = element.parent()?.text() ?: ""
                summary = parentText.replace(fullTitleText, "").trim()
            } else {
                summary = nextSibling.text().trim()
            }
            
            // Final validation: Avoid navigation links
            if (isValidArticle(cleanTitle, url)) {
                articles.add(
                    ArticleEntity(
                        emailId = email.id,
                        title = cleanTitle,
                        url = url,
                        source = email.sender,
                        readingTime = readingTime,
                        summary = summary.take(1000),
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
        
        val excludeKeywords = listOf("unsubscribe", "view in browser", "twitter", "linkedin", "facebook", "privacy policy", "advertise")
        if (excludeKeywords.any { title.contains(it, ignoreCase = true) }) return false
        
        return true
    }

    private fun cleanUrl(url: String): String {
        return try {
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
