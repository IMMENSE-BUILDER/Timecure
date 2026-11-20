package com.timecure.app.data.local

import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.model.StroopSpec
import java.util.Locale
import kotlin.random.Random

/**
 * Deterministic offline cognitive engine. Every answer is computed in code so a question
 * can never be unsolvable due to LLM arithmetic hallucination.
 */
object ChallengeGenerator {

    val LOCAL_TYPES = listOf(
        ChallengeCategory.MATH,
        ChallengeCategory.LOGIC,
        ChallengeCategory.MEMORY,
        ChallengeCategory.WORDS,
        ChallengeCategory.STROOP,
        ChallengeCategory.TIME_MATH
    )

    fun generate(
        category: ChallengeCategory,
        difficulty: ChallengeDifficulty,
        random: Random = Random.Default,
        excludeCategory: ChallengeCategory? = null
    ): CognitiveChallenge = when (category) {
        ChallengeCategory.MATH -> arithmetic(difficulty, random)
        ChallengeCategory.LOGIC -> sequence(difficulty, random)
        ChallengeCategory.MEMORY -> reverseDigits(difficulty, random)
        ChallengeCategory.WORDS -> wordScramble(difficulty, random)
        ChallengeCategory.STROOP -> stroopTest(difficulty, random)
        ChallengeCategory.TIME_MATH -> timeMath(difficulty, random)
        ChallengeCategory.MIXED, ChallengeCategory.GOAL_AFFIRMATION -> {
            val pool = LOCAL_TYPES.filter { it != excludeCategory }.ifEmpty { LOCAL_TYPES }
            generate(pool.random(random), difficulty, random)
        }
    }

    // ---------- Arithmetic ----------

    private fun arithmetic(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val (question, answer, hint) = when (d) {
            ChallengeDifficulty.EASY -> {
                val a = r.nextInt(3, 10); val b = r.nextInt(3, 10); val c = r.nextInt(2, 21)
                if (r.nextBoolean()) {
                    Triple("What is ($a × $b) + $c?", a * b + c, "$a × $b = ${a * b}")
                } else {
                    val cc = r.nextInt(1, a * b)
                    Triple("What is ($a × $b) − $cc?", a * b - cc, "$a × $b = ${a * b}")
                }
            }
            ChallengeDifficulty.MEDIUM -> {
                val a = r.nextInt(11, 26); val b = r.nextInt(3, 10)
                val d2 = r.nextInt(2, 10); val q = r.nextInt(2, 13)
                Triple("What is ($a × $b) − (${d2 * q} ÷ $d2)?", a * b - q, "$a × $b = ${a * b}, ${d2 * q} ÷ $d2 = $q")
            }
            ChallengeDifficulty.HARD -> {
                val a = r.nextInt(12, 40); val b = r.nextInt(3, 10)
                val c = r.nextInt(6, 20); val e = r.nextInt(3, 10); val f = r.nextInt(10, 61)
                Triple("What is ($a × $b) + ($c × $e) − $f?", a * b + c * e - f, "${a * b} + ${c * e} = ${a * b + c * e}")
            }
        }
        return CognitiveChallenge("math_${r.nextInt()}", question, answer.toString(), hint, ChallengeCategory.MATH, d)
    }

    // ---------- Sequences ----------

    private fun sequence(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val terms = 5
        val (values, hint) = when (d) {
            ChallengeDifficulty.EASY -> if (r.nextBoolean()) {
                val start = r.nextInt(1, 30); val step = r.nextInt(2, 10)
                List(terms + 1) { start + it * step } to "Add $step each time"
            } else {
                val start = r.nextInt(1, 6); val mul = r.nextInt(2, 4)
                var v = start
                List(terms + 1) { i -> if (i == 0) v else { v *= mul; v } } to "Multiply by $mul each time"
            }
            ChallengeDifficulty.MEDIUM -> when (r.nextInt(3)) {
                0 -> {
                    val start = r.nextInt(1, 10)
                    var v = start
                    List(terms + 1) { i -> if (i == 0) v else { v += i; v } } to "The gap grows by 1: +1, +2, +3..."
                }
                1 -> {
                    val start = r.nextInt(1, 20); val a = r.nextInt(2, 6); val b = r.nextInt(7, 12)
                    var v = start
                    List(terms + 1) { i -> if (i == 0) v else { v += if (i % 2 == 1) a else b; v } } to "Alternate adding $a and $b"
                }
                else -> {
                    val start = r.nextInt(1, 6)
                    var v = start
                    List(terms + 1) { i -> if (i == 0) v else { v = v * 2 + 1; v } } to "Double it, then add 1"
                }
            }
            ChallengeDifficulty.HARD -> when (r.nextInt(3)) {
                0 -> {
                    val x = r.nextInt(1, 6); val y = r.nextInt(2, 8)
                    val list = mutableListOf(x, y)
                    while (list.size < terms + 1) list.add(list[list.size - 1] + list[list.size - 2])
                    list to "Each term is the sum of the previous two"
                }
                1 -> {
                    val k = r.nextInt(1, 10)
                    List(terms + 1) { (it + 2) * (it + 2) + k } to "Squares (2², 3², 4²...) plus $k"
                }
                else -> {
                    val start = r.nextInt(2, 8)
                    var v = start
                    List(terms + 1) { i -> if (i == 0) v else { v = v * 2 - i; v } } to "Double it, then subtract the step number (1, 2, 3...)"
                }
            }
        }
        val shown = values.take(terms).joinToString(", ")
        return CognitiveChallenge(
            "seq_${r.nextInt()}",
            "What number comes next?\n$shown, ?",
            values.last().toString(),
            hint,
            ChallengeCategory.LOGIC,
            d
        )
    }

    // ---------- Reverse digits (working memory) ----------

    private fun reverseDigits(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val length = when (d) { ChallengeDifficulty.EASY -> 4; ChallengeDifficulty.MEDIUM -> 5; ChallengeDifficulty.HARD -> 7 }
        val digits = List(length) { r.nextInt(0, 10) }
        val safe = if (digits == digits.reversed()) digits.toMutableList().also { it[0] = (it[0] + 1) % 10 } else digits
        return CognitiveChallenge(
            "mem_${r.nextInt()}",
            "Type these digits in reverse order:\n${safe.joinToString("  ")}",
            safe.reversed().joinToString(""),
            "Start from the last digit and work backwards",
            ChallengeCategory.MEMORY,
            d
        )
    }

    // ---------- Stroop Color-Word Interference ----------

    private val STROOP_COLORS = listOf("BLUE", "ORANGE", "GREEN", "RED", "PURPLE", "TEAL")

    private fun stroopTest(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val poolSize = when (d) { ChallengeDifficulty.EASY -> 4; ChallengeDifficulty.MEDIUM -> 5; ChallengeDifficulty.HARD -> 6 }
        val pool = STROOP_COLORS.take(poolSize)
        val ink = pool.random(r)
        val word = pool.filter { it != ink }.random(r)
        val distractors = pool.filter { it != ink && it != word }.shuffled(r).take(2)
        val options = (listOf(ink, word) + distractors).distinct().shuffled(r)
        return CognitiveChallenge(
            id = "stroop_${r.nextInt()}",
            question = "Tap the INK COLOR of the word below (ignore what the word says):",
            answer = ink,
            hint = "Ignore the text \"$word\" — focus strictly on the color of the letters.",
            category = ChallengeCategory.STROOP,
            difficulty = d,
            stroop = StroopSpec(word = word, inkColorId = ink, options = options)
        )
    }

    // ---------- Morning Schedule Time Math ----------

    private fun timeMath(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val startHour = r.nextInt(5, 11)
        val startMin = when (d) {
            ChallengeDifficulty.EASY -> listOf(0, 15, 30, 45).random(r)
            ChallengeDifficulty.MEDIUM -> listOf(5, 10, 20, 25, 35, 40, 50, 55).random(r)
            ChallengeDifficulty.HARD -> r.nextInt(1, 59)
        }
        val addMinutes = when (d) {
            ChallengeDifficulty.EASY -> listOf(20, 25, 30, 40, 45).random(r)
            ChallengeDifficulty.MEDIUM -> listOf(35, 45, 50, 55, 70, 75, 85).random(r)
            ChallengeDifficulty.HARD -> r.nextInt(47, 138)
        }
        val total = startHour * 60 + startMin + addMinutes
        val endHour = (total / 60).let { if (it > 12) it - 12 else it }
        val endMin = total % 60
        val startStr = String.format(Locale.US, "%d:%02d", startHour, startMin)
        val endStr = String.format(Locale.US, "%d:%02d", endHour, endMin)
        return CognitiveChallenge(
            id = "time_${r.nextInt()}",
            question = "If it is $startStr and you do a $addMinutes-minute focus sprint, what time does it end? (H:MM)",
            answer = endStr,
            hint = "Add ${addMinutes / 60}h ${addMinutes % 60}m to $startStr",
            category = ChallengeCategory.TIME_MATH,
            difficulty = d
        )
    }

    // ---------- Word scramble ----------

    internal val WORDS: Map<ChallengeDifficulty, List<Pair<String, String>>> = mapOf(
        ChallengeDifficulty.EASY to listOf(
            "FOCUS" to "Sharp attention", "AWAKE" to "Not asleep", "SHINE" to "What the sun does",
            "BEGIN" to "Start", "POWER" to "Strength", "BRAIN" to "Thinks inside your skull",
            "SUNNY" to "Bright weather", "LIGHT" to "Opposite of dark"
        ),
        ChallengeDifficulty.MEDIUM to listOf(
            "ENERGY" to "Fuel for movement", "EFFORT" to "Trying hard", "MOTIVE" to "Reason to act",
            "GROWTH" to "Getting better", "VISION" to "Picture of the future", "MORNING" to "Start of the day",
            "SUNRISE" to "Dawn", "HUSTLE" to "Work hard"
        ),
        ChallengeDifficulty.HARD to listOf(
            "DISCIPLINE" to "Doing it when you don't feel like it", "RESILIENCE" to "Bouncing back",
            "MINDFULNESS" to "Present-moment awareness", "DETERMINED" to "Won't quit",
            "CONSISTENCY" to "Showing up every day", "MOMENTUM" to "Motion that builds",
            "PRODUCTIVITY" to "Output per effort"
        )
    )

    private fun wordScramble(d: ChallengeDifficulty, r: Random): CognitiveChallenge {
        val (word, clue) = WORDS.getValue(d).random(r)
        var letters = word.toList().shuffled(r)
        var guard = 0
        while (letters.joinToString("") == word && guard++ < 20) letters = word.toList().shuffled(r)
        return CognitiveChallenge(
            "word_${r.nextInt()}",
            "Unscramble this word:\n${letters.joinToString(" ")}",
            word,
            "Clue: $clue",
            ChallengeCategory.WORDS,
            d
        )
    }
}
