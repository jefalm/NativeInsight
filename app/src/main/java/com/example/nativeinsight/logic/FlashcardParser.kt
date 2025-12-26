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
        var currentLiteralBuilder = StringBuilder()

        fun saveCard() {
            if (currentConcept.isNotBlank()) {
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
                        literalLogic = currentLiteralBuilder.toString().trim()
                    )
                )
            }
            // Reset
            currentConcept = ""
            currentFront = ""
            currentBack = ""
            currentLiteralBuilder.clear()
        }

        val lines = rawContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("Concept:", ignoreCase = true) -> {
                    saveCard()
                    currentConcept = trimmed.substringAfter("Concept:").trim()
                }
                trimmed.startsWith("Front (PT):", ignoreCase = true) -> {
                    currentFront = trimmed.substringAfter("Front (PT):").trim()
                }
                trimmed.startsWith("Back (EN):", ignoreCase = true) -> {
                    currentBack = trimmed.substringAfter("Back (EN):").trim()
                }
                trimmed.startsWith("Literal:", ignoreCase = true) -> {
                    currentLiteralBuilder.append(trimmed.substringAfter("Literal:").trim())
                }
                else -> {
                    val isHeaderOrSeparator =
                        trimmed == "---" ||
                                trimmed.startsWith("Flashcard:", ignoreCase = true) ||
                                trimmed.contains("Flashcards", ignoreCase = true)

                    if (isHeaderOrSeparator) {
                        if (trimmed != "---" && !trimmed.startsWith("Flashcard:")) {
                            currentCategory = trimmed
                                .replace("(Finalized)", "")
                                .replace(Regex("""\[.*?\]"""), "")
                                .trim()
                        }
                    } else if (currentLiteralBuilder.isNotEmpty()) {
                        currentLiteralBuilder.append(" ").append(trimmed)
                    } else {
                        currentCategory = trimmed.replace("(Finalized)", "").trim()
                    }
                }
            }
        }
        saveCard()
        Log.d("Parser", "Parsed ${flashcards.size} cards.")
        return flashcards
    }
}