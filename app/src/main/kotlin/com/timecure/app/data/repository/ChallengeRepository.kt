package com.timecure.app.data.repository

import com.timecure.app.data.local.ChallengeGenerator
import com.timecure.app.data.local.PreferencesManager
import com.timecure.app.data.remote.AiResult
import com.timecure.app.data.remote.VercelAiClient
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge

class ChallengeRepository(
    private val ai: VercelAiClient,
    private val prefs: PreferencesManager
) {
    /**
     * Local questions are instant and deterministically verified. The AI supplies personalized
     * reasoning puzzles for AI Coach (and occasionally in Mixed mode), and any failure degrades
     * cleanly to a local challenge while cooling down AI calls briefly.
     */
    suspend fun next(
        category: ChallengeCategory,
        difficulty: ChallengeDifficulty,
        goal: String,
        previousCategory: ChallengeCategory? = null
    ): CognitiveChallenge {
        val effective = when (category) {
            ChallengeCategory.MIXED -> {
                val all = ChallengeGenerator.LOCAL_TYPES +
                    if (aiAvailable()) listOf(ChallengeCategory.GOAL_AFFIRMATION) else emptyList()
                val pool = all.filter { it != previousCategory }.ifEmpty { all }
                pool.random()
            }
            else -> category
        }

        if (effective == ChallengeCategory.GOAL_AFFIRMATION && aiAvailable()) {
            when (val r = ai.generate(difficulty, goal)) {
                is AiResult.Success -> {
                    prefs.aiBlockedUntil = 0L
                    return r.challenge
                }
                is AiResult.Failure -> {
                    prefs.aiBlockedUntil = System.currentTimeMillis() + AI_COOLDOWN_MS
                }
            }
        }
        return ChallengeGenerator.generate(effective, difficulty, excludeCategory = previousCategory)
    }

    fun resetAiCooldown() {
        prefs.aiBlockedUntil = 0L
    }

    private fun aiAvailable(): Boolean = System.currentTimeMillis() >= prefs.aiBlockedUntil

    private companion object {
        const val AI_COOLDOWN_MS = 3 * 60 * 1000L
    }
}
