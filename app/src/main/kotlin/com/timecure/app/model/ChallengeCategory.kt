package com.timecure.app.model

enum class ChallengeCategory(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    MIXED("Mixed rotation", "Mixed", "Rotates through math, logic, memory, stroop, and AI"),
    MATH("Arithmetic chains", "Math", "Multi-step mental computation activating working memory"),
    LOGIC("Number sequences", "Logic", "Pattern recognition and deductive projection"),
    MEMORY("Reverse digits", "Memory", "Hold a digit sequence in working memory and flip it"),
    WORDS("Word scramble", "Words", "Unscramble letters to engage verbal retrieval"),
    STROOP("Stroop color test", "Stroop", "Tap the ink color, not the word — activates executive control"),
    TIME_MATH("Schedule math", "Time", "Calculate elapsed morning times to orient your brain"),
    GOAL_AFFIRMATION("AI Coach", "AI Coach", "AI reasoning puzzle tailored to your goal (offline fallback)");

    companion object {
        fun fromString(value: String): ChallengeCategory =
            entries.firstOrNull { it.name == value } ?: MIXED
    }
}
