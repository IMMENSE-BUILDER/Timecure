package com.timecure.app.data.remote

import android.util.Log
import com.timecure.app.data.local.PreferencesManager
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

class VercelAiClient(private val prefs: PreferencesManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    /** Generates one goal-themed riddle. Never throws; failures are reported as [AiResult.Failure]. */
    suspend fun generate(difficulty: ChallengeDifficulty, goal: String): AiResult =
        withContext(Dispatchers.IO) {
            val system = """
                ${prefs.aiSystemInstructions}

                Right now you are acting as the wake-up cognitive challenge generator inside the alarm.
                The user is groggy, so wake their prefrontal cortex with ONE short reasoning question at ${difficulty.displayName.uppercase()} difficulty.
                Weave in the user's goal when natural. Rules:
                - The answer must be a single integer or a single lowercase word (no sentences).
                - The question must have exactly one correct answer; double-check any arithmetic.
                - Reply with ONLY a JSON object: {"question": "...", "answer": "...", "hint": "..."}
            """.trimIndent()
            val user = "My goal this morning: $goal. Give me one ${difficulty.displayName} question."
            call(system, user, maxTokens = 400)
                .mapCatching { parseChallenge(it, difficulty) }
                .fold(
                    onSuccess = { AiResult.Success(it) },
                    onFailure = { AiResult.Failure(it.message ?: "Unknown AI error") }
                )
        }

    /** Used by Settings: a tiny real request that reports exactly what the gateway said. */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        call("Reply with the single word OK.", "ping", maxTokens = 20)
            .map { "Connected. Model replied: ${it.trim().take(40)}" }
    }

    private fun call(system: String, user: String, maxTokens: Int): Result<String> = runCatching {
        val key = prefs.apiKey
        val url = prefs.gatewayUrl
        require(key.isNotBlank()) { "No API key set" }
        require(url.isNotBlank()) { "No gateway URL set" }

        val body = JSONObject().apply {
            put("model", prefs.modelName)
            put("temperature", 0.6)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", user))
            })
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(json))
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error(describeHttpFailure(resp.code, text))
            }
            JSONObject(text).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content")
                .also { require(it.isNotBlank()) { "Model returned an empty reply" } }
        }
    }.onFailure { Log.w(TAG, "AI call failed: ${it.message}") }

    companion object {
        private const val TAG = "VercelAiClient"

        fun describeHttpFailure(code: Int, body: String): String = when (code) {
            401, 403 -> "HTTP $code: the gateway rejected this API key (invalid, revoked, or no AI Gateway access)"
            402 -> "HTTP 402: the AI Gateway account has no credit"
            404 -> "HTTP 404: unknown model or wrong gateway URL"
            429 -> "HTTP 429: rate limited"
            else -> "HTTP $code${body.take(120).let { if (it.isBlank()) "" else ": $it" }}"
        }

        /**
         * Reasoning models may wrap JSON in prose or <think> blocks, so grab the outermost {...}.
         */
        fun parseChallenge(raw: String, difficulty: ChallengeDifficulty): CognitiveChallenge {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            require(start in 0 until end) { "Reply contained no JSON" }
            val obj = JSONObject(raw.substring(start, end + 1))
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
