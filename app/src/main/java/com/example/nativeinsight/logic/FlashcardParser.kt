package com.example.nativeinsight.logic

import com.example.nativeinsight.data.Flashcard
import java.io.InputStream

object FlashcardParser {
    fun parse(inputStream: InputStream): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()

        // Default category if none is found
        var currentCategory = "General"

        // State variables for the card currently being built
        var concept = ""
        var front = ""
        var back = ""
        var literal = ""

        // Helper to save the pending card to the list
        fun savePendingCard() {
            if (concept.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {

                // [RESTORED] Idiom Density Calculation
                // Using regex split to be safe against multiple spaces, or use split(" ") as originally intended
                val frontWords = front.trim().split("\\s+".toRegex()).size.toFloat()
                val backWords = back.trim().split("\\s+".toRegex()).size.toFloat()
                val density = if (frontWords > 0) backWords / frontWords else 1.0f

                flashcards.add(
                    Flashcard(
                        concept = concept.trim(),
                        frontPt = front.trim(),
                        backEn = back.trim(),
                        literalLogic = literal.trim(),
                        category = currentCategory,
                        idiomDensity = density // Saved correctly
                    )
                )
            }
            // Reset state for the next card
            concept = ""
            front = ""
            back = ""
            literal = ""
        }

        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()

                when {
                    // 1. Skip empty lines or separators
                    trimmed.isEmpty() || trimmed == "---" -> {
                        // Do nothing
                    }

                    // 2. New Concept Found -> Save the PREVIOUS card, start new one
                    trimmed.startsWith("Concept:", ignoreCase = true) -> {
                        savePendingCard()
                        concept = trimmed.substringAfter(":").trim()
                    }

                    // 3. Parse Fields
                    trimmed.startsWith("Front (PT):", ignoreCase = true) -> {
                        front = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("Back (EN):", ignoreCase = true) -> {
                        back = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("Literal:", ignoreCase = true) -> {
                        literal = trimmed.substringAfter(":").trim()
                    }

                    // 4. Category Header Detected
                    else -> {
                        // It is a category header (e.g., "Music & Urban Culture")
                        savePendingCard() // Save any pending card first
                        currentCategory = trimmed
                    }
                }
            }
        }

        // Final save for the very last card in the file
        savePendingCard()

        return flashcards
    }
}