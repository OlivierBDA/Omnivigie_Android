package com.olivierbda.omnivigie.data.extraction

import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import org.jsoup.Jsoup
import java.util.regex.Pattern

class ArticleExtractor {

    fun extractArticles(email: EmailEntity): List<ArticleEntity> {
        val doc = Jsoup.parse(email.bodyHtml)
        val articles = mutableListOf<ArticleEntity>()
        
        var currentSection = "Intro"
        
        // Porting Python logic: iterate over div.text-block
        val textBlocks = doc.select("div.text-block")
        
        for (textBlock in textBlocks) {
            // Check for section headers (H1 > Strong)
            val h1 = textBlock.selectFirst("h1")
            if (h1 != null && h1.selectFirst("strong") != null) {
                currentSection = h1.text().trim()
                continue
            }
            
            // Find <a> tag
            val aTag = textBlock.selectFirst("a") ?: continue
            
            // Find <strong> inside <a>
            val strong = aTag.selectFirst("strong") ?: continue
            
            val titleFull = strong.text().trim()
            val urlTracking = aTag.attr("href")
            val urlClean = cleanUrl(urlTracking)
            
            // Filter out internal TLDR actions
            if (urlClean.contains("tldrnewsletter.com/actions")) {
                continue
            }
            
            // Extract reading time using regex: \(([^)]+read)\)$
            var title = titleFull
            var readingTime = "N/A"
            val timePattern = Pattern.compile("\\(([^)]+read)\\)$", Pattern.CASE_INSENSITIVE)
            val timeMatcher = timePattern.matcher(titleFull)
            
            if (timeMatcher.find()) {
                readingTime = timeMatcher.group(1) ?: "N/A"
                title = titleFull.substring(0, timeMatcher.start()).trim()
            }
            
            // Extract Sponsor flag
            var isSponsor = false
            if (title.contains("(Sponsor)", ignoreCase = true)) {
                isSponsor = true
                title = title.replace("(?i)\\s*\\(Sponsor\\)".toRegex(), "").trim()
            }

            // Find summary: Look for span with font-family style (Python logic)
            var summary = ""
            val spans = textBlock.select("span")
            for (span in spans) {
                val style = span.attr("style")
                if (style.contains("font-family", ignoreCase = true)) {
                    val text = span.text().trim()
                    if (text.isNotEmpty() && text != titleFull) {
                        summary = text
                        break
                    }
                }
            }
            
            // Fallback if no span found
            if (summary.isEmpty()) {
                summary = textBlock.text().replace(titleFull, "").trim()
            }
            
            if (isValidArticle(title, urlClean)) {
                articles.add(
                    ArticleEntity(
                        emailId = email.id,
                        title = title,
                        url = urlClean,
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
        if (title.isBlank() || title.length < 3) return false
        if (!url.startsWith("http")) return false
        
        val excludeKeywords = listOf(
            "unsubscribe", 
            "view in browser", 
            "twitter", 
            "linkedin", 
            "facebook", 
            "privacy policy", 
            "advertise",
            "Apply here" // Added as requested
        )
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
