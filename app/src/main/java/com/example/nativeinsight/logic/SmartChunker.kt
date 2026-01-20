package com.example.nativeinsight.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SmartChunker {
    // Neon/Pastel palette optimized for Dark Mode visibility
    private val chunkPalette = listOf(
        Color(0xFF8BE9FD), // Cyan
        Color(0xFFFFB86C), // Orange
        Color(0xFF50FA7B), // Green
        Color(0xFFFF79C6), // Pink
        Color(0xFFBD93F9)  // Purple
    )

    // Portuguese Trigger Words (Prepositions/Conjunctions)
    private val ptTriggers = listOf(
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "por", "pelo", "pela", "para", "pra", "com", "sem", "que", "se", 
        "como", "e", "mas", "ou", "ao", "aos"
    ).joinToString("|") { "\\b$it\\b" }

    // English Trigger Words
    private val enTriggers = listOf(
        "of", "in", "on", "at", "to", "for", "by", "with", "from", "about",
        "that", "which", "who", "and", "but", "or", "as", "if"
    ).joinToString("|") { "\\b$it\\b" }

    fun colorize(text: String, isEnglish: Boolean = false): AnnotatedString {
        // Select the correct regex based on the language flag
        val triggers = if (isEnglish) enTriggers else ptTriggers
        val splitPattern = "($triggers|[,.;:?!])".toRegex(RegexOption.IGNORE_CASE)
        
        return buildAnnotatedString {
            // If text is too short, don't chunk it
            if (text.length < 15) {
                append(text)
                return@buildAnnotatedString
            }

            var lastMatchIndex = 0
            var colorIndex = 0
            val matches = splitPattern.findAll(text)
            
            matches.forEach { match ->
                val matchStart = match.range.first
                // Only split if the chunk has significant length (prevents chopping small words)
                if (matchStart - lastMatchIndex > 2) {
                    val segment = text.substring(lastMatchIndex, matchStart)
                    withStyle(SpanStyle(color = chunkPalette[colorIndex % chunkPalette.size], fontWeight = FontWeight.SemiBold)) {
                        append(segment)
                    }
                    colorIndex++
                    lastMatchIndex = matchStart
                }
            }
            // Append the remaining text
            if (lastMatchIndex < text.length) {
                withStyle(SpanStyle(color = chunkPalette[colorIndex % chunkPalette.size], fontWeight = FontWeight.SemiBold)) {
                    append(text.substring(lastMatchIndex))
                }
            }
        }
    }
}
