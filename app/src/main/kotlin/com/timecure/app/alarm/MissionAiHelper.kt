package com.timecure.app.alarm

import android.content.Context
import android.net.Uri
import android.util.Log
import com.timecure.app.BuildConfig
import com.timecure.app.data.remote.TimecureAiAgent
import com.timecure.app.data.remote.VercelAiClient
import com.timecure.app.model.WakeMissionCatalog
import com.timecure.app.util.ImageEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ItemVerifyResult(
    val found: Boolean,
    val detected: String,
    val reason: String,
    val offlineFallback: Boolean = false
)

object MissionAiHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /**
     * Evaluates a camera photo against [targetItem] using Timecure's server-side Vision endpoint.
     * Falls back gracefully if offline so an alarm never soft-locks the user.
     */
    suspend fun verifyHouseholdItem(
        context: Context,
        targetItem: String,
        imageUri: Uri
    ): ItemVerifyResult = withContext(Dispatchers.IO) {
        val encoded = ImageEncoder.encodeUri(context, imageUri, maxDimension = 960)
            ?: return@withContext ItemVerifyResult(
                found = false,
                detected = "Image error",
                reason = "Could not read photo — please snap another clear picture.",
                offlineFallback = true
            )

        runCatching {
            val contentArray = JSONArray().apply {
                put(
                    JSONObject()
                        .put("type", "text")
                        .put(
                            "text",
                            """
                            You are verifying an alarm wake-up Household Item Hunt photo.
                            Required object: "$targetItem".
                            Does this photo clearly show a real $targetItem?
                            Reply ONLY with strict JSON (no markdown fences):
                            {"found": true or false, "detected": "main object seen in 1-3 words", "reason": "short 6-10 word explanation"}
                            """.trimIndent()
                        )
                )
                put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", encoded.dataUrl))
                )
            }

            val body = JSONObject().apply {
                put("temperature", 0.2)
                put("max_tokens", 1200)
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().put("role", "user").put("content", contentArray))
                    }
                )
            }

            val request = Request.Builder()
                .url(BuildConfig.TIMECURE_AI_ENDPOINT)
                .post(body.toString().toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val rawContent = VercelAiClient.extractReplyContent(rawBody)
                parseItemVerifyJson(rawContent)
            }
        }.getOrElse { err ->
            Log.w(TAG, "AI item verification fallback: ${err.message}")
            ItemVerifyResult(
                found = false,
                detected = "Offline",
                reason = "AI vision unreachable (${err.message ?: "offline"}) — you can retry or confirm offline.",
                offlineFallback = true
            )
        }
    }

    fun parseItemVerifyJson(raw: String): ItemVerifyResult {
        val cleaned = TimecureAiAgent.stripThinkTags(raw)
        val s = cleaned.indexOf('{')
        val e = cleaned.lastIndexOf('}')
        require(s in 0 until e) { "No JSON object returned" }
        val obj = JSONObject(cleaned.substring(s, e + 1))
        return ItemVerifyResult(
            found = obj.optBoolean("found", false),
            detected = obj.optString("detected", "Unknown object").ifBlank { "Object" },
            reason = obj.optString("reason", "").ifBlank {
                if (obj.optBoolean("found")) "Item confirmed!" else "Target item not clearly visible."
            },
            offlineFallback = false
        )
    }

    suspend fun generateTypingPhrase(
        difficultyLevel: Int,
        goal: String
    ): String = withContext(Dispatchers.IO) {
        val lengthGuide = when {
            difficultyLevel <= 1 -> "4 to 6 words"
            difficultyLevel <= 3 -> "7 to 11 words"
            else -> "12 to 16 words"
        }

        runCatching {
            val body = JSONObject().apply {
                put("temperature", 0.7)
                put("max_tokens", 1200)
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject().put("role", "system").put(
                                "content",
                                "Output ONLY one crisp, plain ASCII motivational wake-up sentence ($lengthGuide) with standard punctuation. No quotes, no markdown."
                            )
                        )
                        put(
                            JSONObject().put("role", "user").put(
                                "content",
                                "Write a typing wake-up sentence for goal: ${goal.ifBlank { "Get up for college" }}"
                            )
                        )
                    }
                )
            }
            val request = Request.Builder()
                .url(BuildConfig.TIMECURE_AI_ENDPOINT)
                .post(body.toString().toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val rawBody = resp.body?.string().orEmpty()
                val clean = VercelAiClient.extractReplyContent(rawBody)
                    .trim()
                    .trim('"', '\'')
                if (clean.length in 10..140 && !clean.contains('\n')) clean
                else WakeMissionCatalog.pickOfflineTypingPhrase(difficultyLevel, goal)
            }
        }.getOrElse {
            WakeMissionCatalog.pickOfflineTypingPhrase(difficultyLevel, goal)
        }
    }

    private const val TAG = "MissionAiHelper"
}
