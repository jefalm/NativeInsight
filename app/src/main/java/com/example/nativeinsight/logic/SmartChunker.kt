package com.example.nativeinsight.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SmartChunker {
    // Neon/Pastel palette
    private val chunkPalette = listOf(
        Color(0xFF8BE9FD), // Cyan
        Color(0xFFFFB86C), // Orange
        Color(0xFF50FA7B), // Green
        Color(0xFFFF79C6), // Pink
        Color(0xFF4143E7)  // Blue
    )

    // 1. Define Connectors (Standard Prepositions)
    private val ptConnectors = listOf(
        "de", "do", "da", "dos", "das",
        "em", "no", "na", "nos", "nas",
        "por", "pelo", "pela",
        "com", "sem", "pra", "pro", "ao", "à"
    ).joinToString("|")

    private val enConnectors = "of|in|on|at|to|for|with|by|up|out"

    // 2. Define Splitters (Breaks)
    private val ptSplitters = listOf(
        "e", "mas", "ou", "porque", "pois", "então", "se", "que", "como", "é"
    ).joinToString("|") { "\\b$it\\b" }

    private val enSplitters = listOf("and", "but", "or", "so", "if", "that", "which", "is", "are", "whether").joinToString("|") { "\\b$it\\b" }

    fun colorize(text: String, isEnglish: Boolean = false): AnnotatedString {
        if (text.isBlank()) return buildAnnotatedString { append(text) }

        val connectors = if (isEnglish) enConnectors else ptConnectors
        val splitters = if (isEnglish) enSplitters else ptSplitters

        var processedText = text

        // --- STEP 1: Start-of-Sentence Glue (Infinitives/Openers) ---
        // Catches "To discuss...", "By doing...", "The idea..." at the very start
        // Regex matches: Start(^) + (Prep/Article) + Space + Word
        val startGlue = """^([Tt]o|[Ff]or|[Bb]y|[Tt]he|[Aa]|[Aa]n)\s+([\p{L}\p{N}'’_]+)""".toRegex()
        processedText = startGlue.replace(processedText) {
            "${it.groupValues[1]}|${it.groupValues[2]}"
        }

        // --- STEP 2: Contextual Article Glue (High-Frequency Nouns) ---
        // Catches "begs the question", "piece of cake", "break the ice"
        // Only glues 'the/a/an' if followed by specific short nouns common in idioms.
        // Nouns: question, cake, eye, ice, bullet, nail, beans, way, end
        val articleIdiom = """\b(the|a|an)\s+(question|cake|eye|ice|bullet|nail|beans|way|end)\b""".toRegex(RegexOption.IGNORE_CASE)
        processedText = articleIdiom.replace(processedText) {
            it.value.replace(" ", "|")
        }

        // --- STEP 3: Standard Connector Glue ---
        // Matches: Word + Connector + Word
        // Updated Pattern: [\p{L}\p{N}'’_]+ includes Unicode letters, numbers, apostrophes, and underscores (for blanks).
        val wordPattern = """[\p{L}\p{N}'’_]+"""
        val gluePattern = """($wordPattern)\s+($connectors)\s+($wordPattern)""".toRegex(RegexOption.IGNORE_CASE)

        // Loop twice to handle chains like "State of the Art"
        repeat(2) {
            processedText = gluePattern.replace(processedText) { match ->
                match.value.replace(" ", "|")
            }
        }

        // --- STEP 4: Split & Render ---
        val splitPattern = """($splitters|[,.;:?!])""".toRegex(RegexOption.IGNORE_CASE)

        return buildAnnotatedString {
            var lastMatchIndex = 0
            var colorIndex = 0
            val matches = splitPattern.findAll(processedText)

            matches.forEach { match ->
                val matchStart = match.range.first

                // Add the chunk BEFORE the splitter
                if (matchStart > lastMatchIndex) {
                    val segment = processedText.substring(lastMatchIndex, matchStart)
                    if (segment.isNotBlank()) {
                        withStyle(SpanStyle(color = chunkPalette[colorIndex % chunkPalette.size], fontWeight = FontWeight.SemiBold)) {
                            // Unmask pipes back to spaces
                            append(segment.replace("|", " "))
                        }
                        colorIndex++
                    }
                }

                // Add the splitter itself (Neutral Color)
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) {
                    append(match.value.replace("|", " "))
                }

                lastMatchIndex = match.range.last + 1
            }

            // Append remaining text
            if (lastMatchIndex < processedText.length) {
                withStyle(SpanStyle(color = chunkPalette[colorIndex % chunkPalette.size], fontWeight = FontWeight.SemiBold)) {
                    append(processedText.substring(lastMatchIndex).replace("|", " "))
                }
            }
        }
    }
}