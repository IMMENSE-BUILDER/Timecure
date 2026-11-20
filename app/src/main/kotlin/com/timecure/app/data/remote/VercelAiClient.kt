package com.timecure.app.data.remote

import android.util.Log
import com.timecure.app.BuildConfig
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.ChallengeSource
import com.timecure.app.model.CognitiveChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface AiResult {
    data class Success(val challenge: CognitiveChallenge) : AiResult
    data class Failure(val reason: String) : AiResult
}

/**
 * Generates goal-aligned wake-up cognitive puzzles via Timecure's server-side AI endpoint.
 * Never throws; offline or transient server errors return [AiResult.Failure].
 */
class VercelAiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    /** Generates one goal-themed riddle. Never throws; failures are reported as [AiResult.Failure]. */
    suspend fun generate(difficulty: ChallengeDifficulty, goal: String): AiResult =
        withContext(Dispatchers.IO) {
            val system = """
                You are Timecure's wake-up cognitive challenge generator inside an alarm screen.
                The user is groggy, so wake their prefrontal cortex with ONE sharp reasoning question at ${difficulty.displayName.uppercase()} difficulty.
                Weave in the user's goal naturally when relevant. Rules:
                - The answer must be a single integer or a single lowercase word (no sentences).
                - The question must have exactly one unambiguous answer; double-check any arithmetic.
                - Reply with ONLY raw JSON (no markdown fences): {"question": "...", "answer": "...", "hint": "..."}
            """.trimIndent()
            val user = "My morning goal: $goal. Give me one ${difficulty.displayName} question."
            call(system, user, maxTokens = 1200)
                .mapCatching { parseChallenge(it, difficulty) }
                .fold(
                    onSuccess = { AiResult.Success(it) },
                    onFailure = { AiResult.Failure(it.message ?: "Unknown AI error") }
                )
        }

    private fun call(system: String, user: String, maxTokens: Int): Result<String> = runCatching {
        val body = JSONObject().apply {
            put("temperature", 0.6)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", user))
            })
        }
        val request = Request.Builder()
            .url(BuildConfig.TIMECURE_AI_ENDPOINT)
            .post(body.toString().toRequestBody(json))
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error(describeHttpFailure(resp.code, text))
            }
            extractReplyContent(text)
                .also { require(it.isNotBlank()) { "AI returned an empty reply" } }
        }
    }.onFailure { Log.w(TAG, "AI call failed: ${it.message}") }

    companion object {
        private const val TAG = "VercelAiClient"

        fun describeHttpFailure(code: Int, body: String): String {
            val parsedError = runCatching {
                JSONObject(body).optString("error").trim()
            }.getOrNull().orEmpty()
            if (parsedError.isNotBlank()) return "HTTP $code: $parsedError"
            return when (code) {
                429 -> "HTTP 429: AI service busy — retry shortly"
                503 -> "HTTP 503: AI service temporarily unavailable"
                else -> "HTTP $code: AI service temporarily unavailable"
            }
        }

        /**
         * Extracts assistant text from Timecure's server proxy `{ content, ... }`
         * with backward-compatible fallback to OpenAI `{ choices: [{ message: { content } }] }`.
         */
        fun extractReplyContent(rawJson: String): String {
            val root = JSONObject(rawJson)
            if (root.has("content")) {
                return TimecureAiAgent.stripThinkTags(root.optString("content"))
            }
            val msgContent = root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            return TimecureAiAgent.stripThinkTags(msgContent)
        }

        /**
         * Reasoning responses may include prose or <think> blocks, so grab the outermost {...}.
         */
        fun parseChallenge(raw: String, difficulty: ChallengeDifficulty): CognitiveChallenge {
            val cleaned = TimecureAiAgent.stripThinkTags(raw)
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            require(start in 0 until end) { "Reply contained no JSON" }
            val obj = JSONObject(cleaned.substring(start, end + 1))
            val question = obj.optString("question").trim()
            val answer = obj.optString("answer").trim()
            require(question.isNotEmpty() && answer.isNotEmpty()) { "JSON missing question or answer" }
            require(answer.length <= 40 && answer.count { it.isWhitespace() } <= 2) { "Answer is not short enough to type" }
            return CognitiveChallenge(
                id = "ai_" + UUID.randomUUID().toString().take(8),
                question = question,
                answer = answer,
                hint = obj.optString("hint").trim(),
                category = ChallengeCategory.GOAL_AFFIRMATION,
                difficulty = difficulty,
                source = ChallengeSource.AI
            )
        }
    }
}
