package com.timecure.app.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

enum class WakeMissionType(
    val displayName: String,
    val shortName: String,
    val section: String,
    val badge: String?
) {
    ITEM_HUNT("Household Item Hunt", "Item hunt", "Popular mission", "AI"),
    SAY_WORD_ON_BEAT("Say the word on beat", "On beat", "Popular mission", "NEW"),
    FIND_COLOR_TILES("Find Color Tiles", "Color tiles", "Wake your brain", null),
    TYPING("Typing", "Typing", "Wake your brain", null),
    MATH("Math", "Math", "Wake your brain", "BEST"),
    STEP("Step", "Step", "Wake your body", "HOT");

    companion object {
        fun fromString(raw: String?): WakeMissionType? =
            entries.firstOrNull { it.name == raw }
    }
}

enum class StepUnit(val label: String) {
    STEPS("Steps"),
    METERS("Meters");

    companion object {
        fun fromString(raw: String?): StepUnit =
            entries.firstOrNull { it.name == raw } ?: STEPS
    }
}

data class WakeMissionConfig(
    val type: WakeMissionType,
    val difficultyLevel: Int = 1,
    val rounds: Int = 2,
    val stepTarget: Int = 20,
    val stepUnit: StepUnit = StepUnit.STEPS,
    val selectedItems: List<String> = WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS,
    val selectedTopics: List<String> = WakeMissionCatalog.DEFAULT_BEAT_TOPICS,
    val useAiPhrase: Boolean = true
) {
    fun difficultyLabel(): String =
        WakeMissionCatalog.DIFFICULTY_LABELS.getOrElse(difficultyLevel.coerceIn(0, 4)) { "Easy" }

    fun summaryText(): String = when (type) {
        WakeMissionType.ITEM_HUNT -> "${selectedItems.size} items • ${rounds}x"
        WakeMissionType.SAY_WORD_ON_BEAT -> "${selectedTopics.size} topics • ${rounds}x"
        WakeMissionType.FIND_COLOR_TILES -> "${difficultyLabel()} • $rounds rounds"
        WakeMissionType.TYPING -> "${difficultyLabel()} • ${rounds}x"
        WakeMissionType.MATH -> "${difficultyLabel()} • $rounds times"
        WakeMissionType.STEP -> "$stepTarget ${stepUnit.label}"
    }

    companion object {
        fun encodeList(missions: List<WakeMissionConfig>): String {
            if (missions.isEmpty()) return ""
            val arr = JSONArray()
            missions.take(5).forEach { m ->
                arr.put(JSONObject().apply {
                    put("type", m.type.name)
                    put("difficultyLevel", m.difficultyLevel)
                    put("rounds", m.rounds)
                    put("stepTarget", m.stepTarget)
                    put("stepUnit", m.stepUnit.name)
                    put("selectedItems", JSONArray(m.selectedItems))
                    put("selectedTopics", JSONArray(m.selectedTopics))
                    put("useAiPhrase", m.useAiPhrase)
                })
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<WakeMissionConfig> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val type = WakeMissionType.fromString(obj.optString("type")) ?: continue
                        val itemsArr = obj.optJSONArray("selectedItems")
                        val items = if (itemsArr != null && itemsArr.length() > 0) {
                            (0 until itemsArr.length()).mapNotNull { idx ->
                                itemsArr.optString(idx).takeIf { it.isNotBlank() }
                            }
                        } else {
                            WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS
                        }
                        val topicsArr = obj.optJSONArray("selectedTopics")
                        val topics = if (topicsArr != null && topicsArr.length() > 0) {
                            (0 until topicsArr.length()).mapNotNull { idx ->
                                topicsArr.optString(idx).takeIf { it.isNotBlank() }
                            }
                        } else {
                            WakeMissionCatalog.DEFAULT_BEAT_TOPICS
                        }
                        add(
                            WakeMissionConfig(
                                type = type,
                                difficultyLevel = obj.optInt("difficultyLevel", 1).coerceIn(0, 4),
                                rounds = obj.optInt("rounds", 2).coerceIn(1, 10),
                                stepTarget = obj.optInt("stepTarget", 20).coerceIn(5, 500),
                                stepUnit = StepUnit.fromString(obj.optString("stepUnit")),
                                selectedItems = items,
                                selectedTopics = topics,
                                useAiPhrase = obj.optBoolean("useAiPhrase", true)
                            )
                        )
                    }
                }.take(5)
            }.getOrDefault(emptyList())
        }
    }
}

object WakeMissionCatalog {
    val DIFFICULTY_LABELS = listOf("Very easy", "Easy", "Medium", "Hard", "Very hard")

    val DEFAULT_HOUSEHOLD_ITEMS = listOf(
        "Cup", "Toothbrush", "Book", "Laptop", "Water Bottle",
        "Shoes", "Backpack", "Keys", "Pillow", "Soap",
        "Towel", "Pen", "Chair", "Mirror", "Watch",
        "Bowl", "Spoon", "Plant", "Headphones"
    )

    val BEAT_WORD_SETS: Map<String, List<String>> = linkedMapOf(
        "Morning Motivation" to listOf("wake", "up", "right", "now", "do", "not", "be", "late"),
        "College & Study" to listOf("rise", "shine", "pack", "your", "bag", "win", "every", "class", "today"),
        "Focus Sprint" to listOf("feet", "on", "floor", "clear", "your", "mind", "start", "the", "work"),
        "Gym & Energy" to listOf("hydrate", "stretch", "breathe", "deep", "move", "fast", "stay", "strong"),
        "Calm Gratitude" to listOf("fresh", "light", "new", "day", "ready", "calm", "sharp", "awake"),
        "Action First" to listOf("drop", "the", "phone", "stand", "tall", "build", "your", "future"),
        "Code & Create" to listOf("open", "mind", "write", "clean", "code", "ship", "great", "work"),
        "Zero Snooze" to listOf("one", "two", "three", "four", "out", "of", "bed", "now")
    )

    val DEFAULT_BEAT_TOPICS = BEAT_WORD_SETS.keys.toList()

    fun stepsRequiredFor(target: Int, unit: StepUnit): Int = when (unit) {
        StepUnit.STEPS -> target.coerceIn(5, 500)
        StepUnit.METERS -> (target.coerceIn(10, 500) * 1.35).toInt().coerceAtLeast(10)
    }

    /**
     * Matches recognized speech tokens against remaining target words in [targetWords].
     * Allows a 1-character typo tolerance for words of length >= 4.
     */
    fun matchSpokenWords(
        targetWords: List<String>,
        alreadyMatched: Set<Int>,
        transcript: String
    ): Set<Int> {
        val tokens = transcript.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return alreadyMatched

        val updated = alreadyMatched.toMutableSet()
        targetWords.forEachIndexed { idx, rawWord ->
            if (idx !in updated) {
                val target = rawWord.lowercase()
                val hit = tokens.any { tok ->
                    tok == target || (
                        target.length >= 4 &&
                            (editDistance(tok, target) <= 1 || isSingleTransposition(tok, target))
                        )
                }
                if (hit) updated += idx
            }
        }
        return updated
    }

    private fun isSingleTransposition(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        val diffIdx = (a.indices).filter { a[it] != b[it] }
        return diffIdx.size == 2 &&
            diffIdx[1] == diffIdx[0] + 1 &&
            a[diffIdx[0]] == b[diffIdx[1]] &&
            a[diffIdx[1]] == b[diffIdx[0]]
    }

    private fun editDistance(a: String, b: String): Int {
        if (abs(a.length - b.length) > 1) return 2
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) prev
                else 1 + minOf(prev, dp[j], dp[j - 1])
                prev = temp
            }
        }
        return dp[b.length]
    }

    fun pickOfflineTypingPhrase(difficultyLevel: Int, goal: String): String {
        val cleanGoal = goal.trim().ifBlank { "win the morning" }
        val shortPhrases = listOf(
            "I am awake and ready.",
            "No snooze today.",
            "Feet on the floor.",
            "Eyes open, mind sharp."
        )
        val mediumPhrases = listOf(
            "Today I choose action over comfort.",
            "Time to get up for college and lock in.",
            "Small morning discipline builds unstoppable momentum.",
            "My focus starts the moment I silence this alarm."
        )
        val longPhrases = listOf(
            "I am wide awake and committed to $cleanGoal without cutting corners.",
            "Every second spent upright right now puts me miles ahead of yesterday.",
            "Comfort fades quickly, but the pride of keeping my morning promise lasts all day."
        )
        return when {
            difficultyLevel <= 1 -> shortPhrases.random()
            difficultyLevel <= 3 -> mediumPhrases.random()
            else -> longPhrases.random()
        }
    }
}
