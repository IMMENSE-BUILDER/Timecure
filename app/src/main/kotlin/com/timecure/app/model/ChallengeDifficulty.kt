package com.timecure.app.model

enum class ChallengeDifficulty(val displayName: String, val level: Int) {
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3);

    fun nextLevel(): ChallengeDifficulty = when (this) {
        EASY -> MEDIUM
        MEDIUM -> HARD
        HARD -> HARD
    }

    companion object {
        fun fromLevel(level: Int): ChallengeDifficulty = when (level) {
            1 -> EASY
            2 -> MEDIUM
            else -> HARD
        }
    }
}
