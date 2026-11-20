package com.timecure.app.model

enum class ChallengeSource { LOCAL, AI }

/**
 * Optional visual payload for Stroop color-conflict challenges.
 * [word] is the text printed (e.g. "ORANGE"), [inkColorId] is the semantic ink color key
 * (e.g. "BLUE") that the user must identify instead of reading the word.
 */
data class StroopSpec(
    val word: String,
    val inkColorId: String,
    val options: List<String>
)

data class CognitiveChallenge(
    val id: String,
    val question: String,
    val answer: String,
    val hint: String = "",
    val category: ChallengeCategory = ChallengeCategory.MATH,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.EASY,
    val source: ChallengeSource = ChallengeSource.LOCAL,
    val stroop: StroopSpec? = null
) {
    /** True when the expected answer is typed with digits/sign/decimal only. */
    val wantsNumericKeyboard: Boolean
        get() = stroop == null && answer.trim().matches(Regex("-?[0-9]+([.,][0-9]+)?"))

    fun isAnswerCorrect(input: String): Boolean {
        val expected = answer.trim()
        val given = input.trim()
        if (expected.isEmpty() || given.isEmpty()) return false

        // Time answers like "8:35" or "08:35"
        if (expected.matches(TIME_PATTERN) && given.matches(TIME_PATTERN)) {
            return normalizeTime(expected) == normalizeTime(given)
        }

        // Canonical numbers (no leading zeros) compared by exact numeric value
        if (expected.matches(CANONICAL_NUMBER)) {
            val e = expected.replace(',', '.').toBigDecimalOrNull() ?: return false
            val g = given.replace(',', '.').toBigDecimalOrNull() ?: return false
            return e.compareTo(g) == 0
        }

        val e = normalize(expected)
        val g = normalize(given)
        if (e.isEmpty() || g.isEmpty()) return false
        if (e == g) return true

        // Digit sequences (e.g. reverse digits) require exact match; 5+ letter words tolerate 1 typo
        val isDigits = e.all { it.isDigit() }
        return !isDigits && stroop == null && e.length >= 5 && levenshtein(e, g) <= 1
    }

    private fun normalize(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    private fun normalizeTime(s: String): Pair<Int, Int>? {
        val parts = s.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h to m
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[a.length][b.length]
    }

    private companion object {
        val CANONICAL_NUMBER = Regex("-?(0|[1-9][0-9]*)([.,][0-9]+)?")
        val TIME_PATTERN = Regex("\\d{1,2}:\\d{2}")
    }
}
