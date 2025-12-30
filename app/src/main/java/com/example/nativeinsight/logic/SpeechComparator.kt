package com.example.nativeinsight.logic

import kotlin.math.max

object SpeechComparator {

    data class Result(
        val isSuccess: Boolean,
        val score: Float, // [NEW] Added to track specific accuracy (0.0 to 1.0)
        val maskedText: String? // The text with blanks, e.g., "I _____ go"
    )

    fun evaluate(target: String, spoken: String): Result {
        // 1. Normalize: Lowercase and remove punctuation for comparison
        val tWords = clean(target)
        val sWords = clean(spoken)

        if (tWords.isEmpty()) return Result(false, 0f, null)

        // 2. Find matching words (LCS)
        val matches = getLCS(tWords, sWords)

        // 3. Calculate Accuracy
        val accuracy = matches.size.toFloat() / tWords.size.toFloat()

        // 4. Determine Outcome (Threshold: 50%)
        if (accuracy >= 0.5) {
            // Build the "Masked" string
            val resultBuilder = StringBuilder()
            var matchIndex = 0

            for (word in tWords) {
                if (matchIndex < matches.size && word == matches[matchIndex]) {
                    resultBuilder.append(word).append(" ")
                    matchIndex++
                } else {
                    resultBuilder.append("_____ ")
                }
            }
            // Return Success + Score + Masked Text
            return Result(true, accuracy, resultBuilder.toString().trim())
        } else {
            return Result(false, accuracy, null)
        }
    }

    // Helper: Split string into clean word list
    private fun clean(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z ]"), "") // Remove non-letters
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }

    // Standard LCS Algorithm (Dynamic Programming)
    private fun getLCS(list1: List<String>, list2: List<String>): List<String> {
        val dp = Array(list1.size + 1) { IntArray(list2.size + 1) }
        for (i in 1..list1.size) {
            for (j in 1..list2.size) {
                if (list1[i - 1] == list2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        // Backtrack to find the actual words
        val result = mutableListOf<String>()
        var i = list1.size
        var j = list2.size
        while (i > 0 && j > 0) {
            if (list1[i - 1] == list2[j - 1]) {
                result.add(list1[i - 1])
                i--
                j--
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }
        return result.reversed()
    }
}