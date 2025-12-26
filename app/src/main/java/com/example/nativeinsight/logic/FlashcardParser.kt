package com.example.nativeinsight.logic

import com.example.nativeinsight.data.Flashcard
import android.util.Log

object FlashcardParser {

    fun parse(rawContent: String): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()

        // --- State Variables ---
        var currentCategory = "General"
        var currentConcept = ""
        var currentFront = ""
        var currentBack = ""
        var currentLiteralBuilder = StringBuilder() // Builds multi-line literals safely

        // Helper to save the current buffer to the list
        fun saveCard() {
            if (currentConcept.isNotBlank()) {
                // Calculate Idiom Density
                val frontWords = currentFront.split(" ").size.toFloat()
                val backWords = currentBack.split(" ").size.toFloat()
                val density = if (frontWords > 0) backWords / frontWords else 1.0f

                flashcards.add(
                    Flashcard(
                        concept = currentConcept.trim(),
                        category = currentCategory,
                        idiomDensity = density,
                        frontPt = currentFront.trim(),
                        backEn = currentBack.trim(),
                        literal = currentLiteralBuilder.toString().trim()
                    )
                )
            }
            // Reset for next card
            currentConcept = ""
            currentFront = ""
            currentBack = ""
            currentLiteralBuilder.clear()
        }

        // --- The State Machine Loop ---
        val lines = rawContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                // 1. New Card Trigger: "Concept:"
                trimmed.startsWith("Concept:", ignoreCase = true) -> {
                    saveCard() // Save the previous card first
                    currentConcept = trimmed.substringAfter("Concept:").trim()
                }

                // 2. Field: Front (PT)
                trimmed.startsWith("Front (PT):", ignoreCase = true) -> {
                    currentFront = trimmed.substringAfter("Front (PT):").trim()
                }

                // 3. Field: Back (EN)
                trimmed.startsWith("Back (EN):", ignoreCase = true) -> {
                    currentBack = trimmed.substringAfter("Back (EN):").trim()
                }

                // 4. Field: Literal Logic
                trimmed.startsWith("Literal:", ignoreCase = true) -> {
                    currentLiteralBuilder.append(trimmed.substringAfter("Literal:").trim())
                }

                // 5. Handling Gaps, Headers, and Multi-line Literals
                else -> {
                    // We are in the "space between fields".
                    // Is this a continuation of the Literal? Or a new Header?

                    val isHeaderOrSeparator =
                        trimmed == "---" ||
                                trimmed.startsWith("Flashcard:", ignoreCase = true) ||
                                trimmed.contains("Flashcards", ignoreCase = true)

                    if (isHeaderOrSeparator) {
                        // It's a separator or header. STOP reading Literal.
                        // If it has text (e.g. "Havaianas Case Flashcards"), update Category.
                        if (trimmed != "---" && !trimmed.startsWith("Flashcard:")) {
                            currentCategory = trimmed
                                .replace("(Finalized)", "")
                                .replace(Regex("""\[.*?\]"""), "") // remove [source]
                                .trim()
                        }
                    } else if (currentLiteralBuilder.isNotEmpty()) {
                        // It's NOT a header, so it must be more text for the Literal.
                        currentLiteralBuilder.append(" ").append(trimmed)
                    } else {
                        // It's a header appearing at the very start of the file
                        currentCategory = trimmed.replace("(Finalized)", "").trim()
                    }
                }
            }
        }

        saveCard() // Don't forget the very last card!

        Log.d("Parser", "Parsed ${flashcards.size} cards.")
        return flashcards
    }
}