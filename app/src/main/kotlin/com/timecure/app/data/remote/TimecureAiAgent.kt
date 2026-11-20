package com.timecure.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.timecure.app.alarm.AlarmActions
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.data.local.PreferencesManager
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.data.repository.AlarmRepository
import com.timecure.app.data.repository.TaskPlanRepository
import com.timecure.app.model.AiTaskItem
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.PomodoroPreset
import com.timecure.app.pomodoro.PomodoroEngine
import com.timecure.app.ui.theme.AppThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiChatMessage(
    val id: Long = System.nanoTime(),
    val isUser: Boolean,
    val text: String,
    val imagePreview: Bitmap? = null,
    val toolBadges: List<String> = emptyList(),
    val generatedTasks: List<AiTaskItem> = emptyList(),
    val sourceLabel: String = ""
)

data class AgentTurnResult(
    val reply: String,
    val toolBadges: List<String>,
    val generatedTasks: List<AiTaskItem> = emptyList(),
    val usedLiveModel: Boolean,
    val openFocusTab: Boolean = false,
    val updatedThemeMode: ThemeMode? = null,
    val updatedPalette: AppThemePalette? = null,
    val updatedUserName: String? = null
)

/**
 * Multimodal + Tool-Calling AI Assistant powered by Vercel AI Gateway (minimax/minimax-m3.1-flash).
 *
 * Supports:
 * - Native Vision input (photos of college timetables, study materials, handwritten goals, routines)
 *   encoded as OpenAI-compatible `image_url` data URIs.
 * - 6 real function tools: `create_alarm`, `toggle_alarm`, `delete_alarm`, `start_pomodoro`,
 *   `update_preferences`, and `save_task_table` (generates structured task tables with study
 *   techniques & suggested alarm times persisted in SQLite).
 * - Automatic on-device fallback if offline or gateway returns an auth error.
 */
class TimecureAiAgent(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pomodoroEngine: PomodoroEngine,
    private val taskPlanRepository: TaskPlanRepository
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        userText: String,
        imageDataUrl: String? = null,
        recentHistory: List<AiChatMessage> = emptyList()
    ): AgentTurnResult = withContext(Dispatchers.IO) {
        val alarms = alarmRepository.getAll()
        val liveAttempt = runLiveToolLoop(userText, imageDataUrl, recentHistory, alarms)
        if (liveAttempt.isSuccess) {
            return@withContext liveAttempt.getOrThrow()
        }

        val reason = liveAttempt.exceptionOrNull()?.message ?: "Gateway unavailable"
        Log.w(TAG, "Falling back to on-device router: $reason")
        runLocalFallback(userText, imageDataUrl != null, reason)
    }

    private suspend fun runLiveToolLoop(
        userText: String,
        imageDataUrl: String?,
        history: List<AiChatMessage>,
        alarms: List<Alarm>
    ): Result<AgentTurnResult> = runCatching {
        val key = prefs.apiKey
        val url = prefs.gatewayUrl
        require(key.isNotBlank()) { "No API key configured" }
        require(url.isNotBlank()) { "No gateway URL configured" }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", buildSystemPrompt(alarms)))
            history.takeLast(6).forEach { msg ->
                put(JSONObject().put("role", if (msg.isUser) "user" else "assistant").put("content", msg.text))
            }
            if (imageDataUrl != null) {
                val prompt = userText.ifBlank {
                    "Analyze this image (timetable, routine, study notes, or goals). Call save_task_table to build a structured task & study technique plan, and suggest or set any helpful alarms."
                }
                val contentParts = JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", prompt))
                    put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", imageDataUrl))
                    )
                }
                put(JSONObject().put("role", "user").put("content", contentParts))
            } else {
                put(JSONObject().put("role", "user").put("content", userText))
            }
        }

        val badges = mutableListOf<String>()
        val createdTasks = mutableListOf<AiTaskItem>()
        var openFocus = false
        var newTheme: ThemeMode? = null
        var newPalette: AppThemePalette? = null
        var newName: String? = null

        val firstMsg = callGateway(url, key, messages, includeTools = true)
        val toolCalls = firstMsg.optJSONArray("tool_calls")

        if (toolCalls != null && toolCalls.length() > 0) {
            messages.put(firstMsg)
            for (i in 0 until toolCalls.length()) {
                val tc = toolCalls.getJSONObject(i)
                val tcId = tc.optString("id", "call_$i")
                val fn = tc.getJSONObject("function")
                val fnName = fn.getString("name")
                val argsRaw = fn.optString("arguments", "{}")
                val args = runCatching { JSONObject(argsRaw) }.getOrDefault(JSONObject())

                val exec = executeTool(fnName, args)
                badges += exec.badge
                createdTasks += exec.generatedTasks
                if (exec.openFocusTab) openFocus = true
                if (exec.updatedThemeMode != null) newTheme = exec.updatedThemeMode
                if (exec.updatedPalette != null) newPalette = exec.updatedPalette
                if (exec.updatedUserName != null) newName = exec.updatedUserName

                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", tcId)
                        .put("name", fnName)
                        .put("content", exec.toolResultJson)
                )
            }

            val finalMsg = runCatching { callGateway(url, key, messages, includeTools = false) }.getOrNull()
            val finalReply = stripThinkTags(finalMsg?.optString("content").orEmpty())
                .ifBlank { "Done! Completed: ${badges.joinToString(", ")}." }

            prefs.lastAiStatus = "OK — ${if (imageDataUrl != null) "Vision + Tool" else "Tool"} via ${prefs.modelName}"
            prefs.aiBlockedUntil = 0L
            AgentTurnResult(
                reply = finalReply,
                toolBadges = badges,
                generatedTasks = createdTasks,
                usedLiveModel = true,
                openFocusTab = openFocus,
                updatedThemeMode = newTheme,
                updatedPalette = newPalette,
                updatedUserName = newName
            )
        } else {
            val content = stripThinkTags(firstMsg.optString("content").orEmpty())
                .ifBlank { "Let me know if you want me to schedule alarms, build a task table from a photo, or start a Pomodoro block." }
            prefs.lastAiStatus = "OK — ${if (imageDataUrl != null) "Vision" else "Chat"} via ${prefs.modelName}"
            prefs.aiBlockedUntil = 0L
            AgentTurnResult(reply = content, toolBadges = emptyList(), usedLiveModel = true)
        }
    }

    private fun callGateway(url: String, key: String, messages: JSONArray, includeTools: Boolean): JSONObject {
        val body = JSONObject().apply {
            put("model", prefs.modelName)
            put("temperature", 0.3)
            put("max_tokens", 900)
            put("messages", messages)
            if (includeTools) {
                put("tools", buildToolSchemas())
                put("tool_choice", "auto")
            }
        }
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(jsonType))
            .build()

        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = VercelAiClient.describeHttpFailure(resp.code, text)
                prefs.lastAiStatus = msg
                error(msg)
            }
            return JSONObject(text)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
        }
    }

    private data class ToolExecOutcome(
        val badge: String,
        val toolResultJson: String,
        val generatedTasks: List<AiTaskItem> = emptyList(),
        val openFocusTab: Boolean = false,
        val updatedThemeMode: ThemeMode? = null,
        val updatedPalette: AppThemePalette? = null,
        val updatedUserName: String? = null
    )

    private suspend fun executeTool(name: String, args: JSONObject): ToolExecOutcome {
        return when (name) {
            "create_alarm" -> {
                val hour = args.optInt("hour", 7).coerceIn(0, 23)
                val minute = args.optInt("minute", 0).coerceIn(0, 59)
                val days = parseDayCodes(args.optJSONArray("days_of_week"))
                val goal = args.optString("goal", prefs.defaultGoal).ifBlank { prefs.defaultGoal }
                val label = args.optString("label", goal)
                val wakePrompt = args.optString("wake_prompt", "")
                    .ifBlank { "$goal — wake up sharp and own the day." }
                val diff = ChallengeDifficulty.entries.firstOrNull {
                    it.name.equals(args.optString("difficulty"), ignoreCase = true)
                } ?: prefs.defaultDifficulty
                val cat = ChallengeCategory.fromString(args.optString("category", "MIXED").uppercase())
                val qCount = args.optInt("questions_required", 2).coerceIn(1, 5)
                val wakeCheck = args.optBoolean("wake_check_enabled", true)

                val alarm = Alarm(
                    hour = hour, minute = minute, daysOfWeek = days,
                    label = label, goal = goal, wakePrompt = wakePrompt,
                    difficulty = diff, category = cat, questionsRequired = qCount,
                    isEnabled = true, wakeCheckEnabled = wakeCheck
                )
                val newId = alarmRepository.insert(alarm)
                val saved = alarm.copy(id = newId)
                alarmScheduler.schedule(saved)

                ToolExecOutcome(
                    badge = "Alarm ${saved.formattedTime} ${saved.amPm} • ${saved.goal}",
                    toolResultJson = JSONObject()
                        .put("status", "created")
                        .put("alarm_id", newId)
                        .put("time", "${saved.formattedTime} ${saved.amPm}")
                        .put("days", saved.daysSummary)
                        .put("goal", saved.goal)
                        .toString()
                )
            }

            "save_task_table" -> {
                val planTitle = args.optString("plan_title", "Smart Study & Routine Plan")
                    .ifBlank { "Smart Study & Routine Plan" }
                val arr = args.optJSONArray("tasks") ?: JSONArray()
                val items = mutableListOf<AiTaskItem>()
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONObject(i) ?: continue
                    val taskText = row.optString("task").trim()
                    if (taskText.isEmpty()) continue
                    val slot = row.optString("time_slot", "Flexible").ifBlank { "Flexible" }
                    val tech = row.optString("technique", "Pomodoro 25/5").ifBlank { "Pomodoro 25/5" }
                    val h = if (row.has("alarm_hour") && !row.isNull("alarm_hour")) row.optInt("alarm_hour", -1) else -1
                    val m = if (row.has("alarm_minute") && !row.isNull("alarm_minute")) row.optInt("alarm_minute", -1) else -1
                    items += AiTaskItem(
                        planTitle = planTitle,
                        timeSlot = slot,
                        taskText = taskText,
                        technique = tech,
                        suggestedAlarmHour = if (h in 0..23) h else null,
                        suggestedAlarmMinute = if (m in 0..59) m else null
                    )
                }
                if (items.isNotEmpty()) {
                    taskPlanRepository.replacePlan(items)
                }
                ToolExecOutcome(
                    badge = "Task Table (${items.size} tasks) • $planTitle",
                    toolResultJson = JSONObject()
                        .put("status", "saved")
                        .put("plan_title", planTitle)
                        .put("task_count", items.size)
                        .toString(),
                    generatedTasks = items
                )
            }

            "toggle_alarm" -> {
                val targetId = args.optLong("alarm_id", -1L)
                val enable = args.optBoolean("is_enabled", true)
                val all = alarmRepository.getAll()
                val targets = if (targetId <= 0L) all else all.filter { it.id == targetId }
                targets.forEach { a ->
                    val updated = a.copy(isEnabled = enable)
                    alarmRepository.update(updated)
                    if (enable) alarmScheduler.schedule(updated) else alarmScheduler.cancel(a.id)
                }
                val verb = if (enable) "Enabled" else "Paused"
                ToolExecOutcome(
                    badge = "$verb ${targets.size} alarm(s)",
                    toolResultJson = JSONObject().put("status", "updated").put("count", targets.size).toString()
                )
            }

            "delete_alarm" -> {
                val targetId = args.optLong("alarm_id", -1L)
                if (targetId > 0L) {
                    alarmScheduler.cancel(targetId)
                    alarmRepository.delete(targetId)
                }
                ToolExecOutcome(
                    badge = "Deleted alarm #$targetId",
                    toolResultJson = JSONObject().put("status", "deleted").put("alarm_id", targetId).toString()
                )
            }

            "start_pomodoro" -> {
                val goal = args.optString("goal", prefs.defaultGoal).ifBlank { prefs.defaultGoal }
                val presetName = args.optString("preset", "CLASSIC").uppercase()
                val preset = PomodoroPreset.entries.firstOrNull { it.name == presetName } ?: PomodoroPreset.CLASSIC
                val autoStart = args.optBoolean("auto_start", true)
                if (autoStart) {
                    pomodoroEngine.launchSprintForGoal(goal, preset)
                } else {
                    pomodoroEngine.selectPreset(preset)
                    pomodoroEngine.setGoal(goal)
                }
                ToolExecOutcome(
                    badge = "Pomodoro ${preset.displayName} • $goal",
                    toolResultJson = JSONObject().put("status", "ready").put("preset", preset.displayName).toString(),
                    openFocusTab = true
                )
            }

            "update_preferences" -> {
                val updates = mutableListOf<String>()
                var updatedTheme: ThemeMode? = null
                var updatedPalette: AppThemePalette? = null
                var updatedName: String? = null

                args.optString("user_name").takeIf { it.isNotBlank() }?.let {
                    prefs.userName = it; updatedName = it; updates += "Name: $it"
                }
                args.optString("default_goal").takeIf { it.isNotBlank() }?.let {
                    prefs.defaultGoal = it; updates += "Goal: $it"
                }
                args.optString("default_difficulty").takeIf { it.isNotBlank() }?.let { raw ->
                    ChallengeDifficulty.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.let {
                        prefs.defaultDifficulty = it; updates += "Difficulty: ${it.displayName}"
                    }
                }
                args.optString("theme_mode").takeIf { it.isNotBlank() }?.let { raw ->
                    ThemeMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.let {
                        prefs.themeMode = it; updatedTheme = it; updates += "Mode: ${it.label}"
                    }
                }
                args.optString("theme_palette").takeIf { it.isNotBlank() }?.let { raw ->
                    AppThemePalette.entries.firstOrNull {
                        it.name.equals(raw, ignoreCase = true) || it.displayName.equals(raw, ignoreCase = true)
                    }?.let {
                        prefs.themePalette = it; updatedPalette = it; updates += "Theme: ${it.displayName}"
                    }
                }

                ToolExecOutcome(
                    badge = if (updates.isEmpty()) "Preferences checked" else updates.joinToString(" • "),
                    toolResultJson = JSONObject().put("status", "updated").put("changes", updates.joinToString(", ")).toString(),
                    updatedThemeMode = updatedTheme,
                    updatedPalette = updatedPalette,
                    updatedUserName = updatedName
                )
            }

            "ultimate_morning_unlock" -> {
                AlarmActions.silence(context)
                val all = alarmRepository.getAll()
                all.forEach { a ->
                    alarmScheduler.cancel(a.id)
                    if (a.isEnabled) alarmRepository.setEnabled(a.id, false)
                }
                taskPlanRepository.clearAll()
                val note = args.optString("verification_note", "Washed-up morning face verified")
                ToolExecOutcome(
                    badge = "Ultimate Wake Verified • All alarms unlocked & tasks cleared",
                    toolResultJson = JSONObject()
                        .put("status", "unlocked_all")
                        .put("alarms_unlocked", all.size)
                        .put("tasks_cleared", true)
                        .put("note", note)
                        .toString()
                )
            }

            else -> ToolExecOutcome(
                badge = "Unknown tool: $name",
                toolResultJson = JSONObject().put("error", "Unknown tool $name").toString()
            )
        }
    }

    private suspend fun runLocalFallback(
        userText: String,
        hadImage: Boolean,
        gatewayError: String
    ): AgentTurnResult {
        val parsed = parseLocalIntent(userText, prefs.defaultGoal, hadImage)
        val badges = mutableListOf<String>()
        val createdTasks = mutableListOf<AiTaskItem>()
        var openFocus = false
        var newTheme: ThemeMode? = null
        var newPalette: AppThemePalette? = null
        var newName: String? = null

        for ((toolName, args) in parsed) {
            val res = executeTool(toolName, args)
            badges += res.badge
            createdTasks += res.generatedTasks
            if (res.openFocusTab) openFocus = true
            if (res.updatedThemeMode != null) newTheme = res.updatedThemeMode
            if (res.updatedPalette != null) newPalette = res.updatedPalette
            if (res.updatedUserName != null) newName = res.updatedUserName
        }

        val reply = when {
            hadImage && createdTasks.isNotEmpty() ->
                "Image attached, but live Vision on ${prefs.modelName} returned: $gatewayError.\n\nI created a starter structured routine table below so you can test the 1-tap Alarm & Pomodoro controls right away — update your Vercel key in Settings for live OCR & timetable Vision reading!"
            badges.isNotEmpty() ->
                "Executed directly (${badges.joinToString("; ")}).\n\nNote: Live ${prefs.modelName} returned: $gatewayError — update your Vercel key in Settings for cloud Vision & reasoning."
            else ->
                "Could not reach ${prefs.modelName} ($gatewayError). Try:\n• Upload a timetable photo & ask to build a task table\n• \"Set a 7:15 AM weekday alarm to get up for college\"\n• \"Start a 25m focus sprint for physics\" or \"Switch to Emerald theme\""
        }

        return AgentTurnResult(
            reply = reply,
            toolBadges = badges,
            generatedTasks = createdTasks,
            usedLiveModel = false,
            openFocusTab = openFocus,
            updatedThemeMode = newTheme,
            updatedPalette = newPalette,
            updatedUserName = newName
        )
    }

    private fun buildSystemPrompt(alarms: List<Alarm>): String {
        val nowStr = SimpleDateFormat("EEEE, MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        val alarmSummary = if (alarms.isEmpty()) "None" else alarms.joinToString("\n") {
            "- id=${it.id}: ${it.formattedTime} ${it.amPm} (${it.daysSummary}), enabled=${it.isEnabled}, goal=\"${it.goal}\""
        }
        return """
            ${prefs.aiSystemInstructions}

            Active Model: ${prefs.modelName}
            Current local time: $nowStr
            User name: ${prefs.userName.ifBlank { "not set" }}
            Default morning goal: ${prefs.defaultGoal}
            Existing alarms:
            $alarmSummary

            Capabilities & rules:
            1. ULTIMATE FACE-WASH WAKE TEST (Vision): If the user scans or uploads a photo of their face and it appears as a freshly washed ("washed-up") morning face (e.g. wet/damp skin, water droplets, fresh post-wash appearance), they have passed the ultimate wake-up test. Immediately call `ultimate_morning_unlock` without requiring any answers — this silences/unlocks all alarms and clears all tasks — and congratulate them warmly.
            2. When the user uploads an image of a timetable, routine, study syllabus, textbook/notes, or goals:
               - Read every time slot, subject, deadline, or priority carefully using Vision.
               - Call `save_task_table` with a structured, actionable plan (exact time_slot, concrete task, evidence-based study technique like "Pomodoro 50/10", "Active Recall", "Feynman Method", or "Spaced Repetition", plus suggested alarm_hour & alarm_minute for key wake/start blocks).
               - If the user asks you to set alarms from the timetable, also call `create_alarm` directly!
            3. For text requests, invoke the right tools (`create_alarm`, `toggle_alarm`, `delete_alarm`, `start_pomodoro`, `update_preferences`, `save_task_table`, `ultimate_morning_unlock`).
            4. Keep replies crisp, encouraging, and grounded in the tools you executed.
        """.trimIndent()
    }

    companion object {
        private const val TAG = "TimecureAiAgent"

        fun stripThinkTags(raw: String): String =
            raw.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()

        fun parseDayCodes(arr: JSONArray?): Set<Int> {
            if (arr == null) return emptySet()
            val map = mapOf(
                "SUN" to Calendar.SUNDAY, "SUNDAY" to Calendar.SUNDAY,
                "MON" to Calendar.MONDAY, "MONDAY" to Calendar.MONDAY,
                "TUE" to Calendar.TUESDAY, "TUESDAY" to Calendar.TUESDAY,
                "WED" to Calendar.WEDNESDAY, "WEDNESDAY" to Calendar.WEDNESDAY,
                "THU" to Calendar.THURSDAY, "THURSDAY" to Calendar.THURSDAY,
                "FRI" to Calendar.FRIDAY, "FRIDAY" to Calendar.FRIDAY,
                "SAT" to Calendar.SATURDAY, "SATURDAY" to Calendar.SATURDAY
            )
            val set = mutableSetOf<Int>()
            for (i in 0 until arr.length()) {
                map[arr.optString(i).trim().uppercase()]?.let { set += it }
            }
            return set
        }

        fun parseLocalIntent(
            userText: String,
            defaultGoal: String,
            hadImage: Boolean = false
        ): List<Pair<String, JSONObject>> {
            val lower = userText.lowercase()
            val actions = mutableListOf<Pair<String, JSONObject>>()

            // Image upload / timetable fallback plan
            if (hadImage || "task table" in lower || "timetable" in lower || "study plan" in lower) {
                val tasks = JSONArray().apply {
                    put(JSONObject().put("time_slot", "07:00 AM").put("task", "Get up for college & morning hydration").put("technique", "Cognitive Alarm + 5m check").put("alarm_hour", 7).put("alarm_minute", 0))
                    put(JSONObject().put("time_slot", "08:00 AM").put("task", "Core lecture / first high-priority subject").put("technique", "Active Recall Notes").put("alarm_hour", 8).put("alarm_minute", 0))
                    put(JSONObject().put("time_slot", "05:30 PM").put("task", "Review today's notes & solve practice problems").put("technique", "Deep Work 50/10"))
                    put(JSONObject().put("time_slot", "09:00 PM").put("task", "15-min spaced-repetition recap & prep bag").put("technique", "Sprint 15/3"))
                }
                actions += "save_task_table" to JSONObject()
                    .put("plan_title", if ("college" in lower) "College Timetable Plan" else "Smart Daily Task Table")
                    .put("tasks", tasks)
            }

            // Preferences (mode, palette, name, goal)
            val prefArgs = JSONObject()
            if ("dark mode" in lower || "dark theme" in lower) prefArgs.put("theme_mode", "DARK")
            else if ("light mode" in lower || "light theme" in lower) prefArgs.put("theme_mode", "LIGHT")
            else if ("system theme" in lower) prefArgs.put("theme_mode", "SYSTEM")

            AppThemePalette.entries.firstOrNull {
                it.displayName.lowercase() in lower || it.name.lowercase().replace("_", " ") in lower
            }?.let { prefArgs.put("theme_palette", it.name) }

            Regex("(?:call me|my name is)\\s+([a-zA-Z]{2,20})", RegexOption.IGNORE_CASE)
                .find(userText)?.groupValues?.get(1)?.let { prefArgs.put("user_name", it) }

            Regex("(?:default goal|my goal)(?:\\s+is|\\s+to)?\\s+([^.,;!?]+)", RegexOption.IGNORE_CASE)
                .find(userText)?.groupValues?.get(1)?.trim()?.let { prefArgs.put("default_goal", it) }

            if (prefArgs.length() > 0) actions += "update_preferences" to prefArgs

            // Pomodoro intent
            if ("pomodoro" in lower || "focus" in lower || "sprint" in lower || "study session" in lower) {
                val preset = when {
                    "90" in lower -> "ULTRADIAN"
                    "50" in lower -> "DEEP_WORK"
                    "15" in lower -> "SPRINT"
                    else -> "CLASSIC"
                }
                val goal = Regex("(?:for|on)\\s+([^.,;!?]+)", RegexOption.IGNORE_CASE)
                    .find(userText)?.groupValues?.get(1)?.trim()
                    ?: if ("college" in lower) "College study sprint" else defaultGoal
                actions += "start_pomodoro" to JSONObject()
                    .put("goal", goal).put("preset", preset).put("auto_start", true)
            }

            // Toggle alarms off
            if (("turn off" in lower || "disable" in lower || "pause" in lower) && "alarm" in lower) {
                actions += "toggle_alarm" to JSONObject().put("alarm_id", -1L).put("is_enabled", false)
                return actions
            }

            // Create alarm intent
            val timeMatch = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
                .findAll(lower)
                .firstOrNull { m ->
                    val h = m.groupValues[1].toIntOrNull() ?: return@firstOrNull false
                    (m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty()) && h in 0..23
                }

            val wantsAlarm = "alarm" in lower || "wake" in lower || "get up" in lower || timeMatch != null
            if (wantsAlarm && timeMatch != null) {
                var hour = timeMatch.groupValues[1].toInt()
                val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
                val meridiem = timeMatch.groupValues[3].lowercase()
                if (meridiem == "pm" && hour in 1..11) hour += 12
                if (meridiem == "am" && hour == 12) hour = 0

                val days = JSONArray()
                when {
                    "every day" in lower || "everyday" in lower || "daily" in lower ->
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { days.put(it) }
                    "weekday" in lower || "college" in lower || "school" in lower || "work" in lower ->
                        listOf("MON", "TUE", "WED", "THU", "FRI").forEach { days.put(it) }
                    "weekend" in lower ->
                        listOf("SAT", "SUN").forEach { days.put(it) }
                }

                val goal = when {
                    "college" in lower -> "Get up for college"
                    "gym" in lower || "workout" in lower || "run" in lower -> "Gym & Morning Run"
                    "exam" in lower || "study" in lower -> "Exam & Study Prep"
                    "code" in lower || "coding" in lower -> "Early Coding"
                    else -> Regex("(?:for|to)\\s+([^.,;!?]+)", RegexOption.IGNORE_CASE)
                        .find(userText)?.groupValues?.get(1)?.trim()?.take(48) ?: defaultGoal
                }
                val prompt = when {
                    "college" in lower -> "Get up for college — attendance and morning momentum start now."
                    else -> "$goal — feet on the floor, no excuses."
                }
                val diff = when {
                    "hard" in lower || "brutal" in lower -> "HARD"
                    "easy" in lower -> "EASY"
                    else -> "MEDIUM"
                }

                actions += "create_alarm" to JSONObject()
                    .put("hour", hour).put("minute", minute).put("days_of_week", days)
                    .put("label", goal).put("goal", goal).put("wake_prompt", prompt)
                    .put("difficulty", diff).put("category", "MIXED")
                    .put("questions_required", if (diff == "HARD") 3 else 2)
                    .put("wake_check_enabled", true)
            }

            return actions
        }

        fun buildToolSchemas(): JSONArray = JSONArray().apply {
            put(
                toolSpec(
                    name = "create_alarm",
                    description = "Create and schedule a new unstoppable wake-up alarm with a personal goal and lock-screen wake prompt.",
                    props = JSONObject()
                        .put("hour", JSONObject().put("type", "integer").put("description", "24-hour clock hour 0..23"))
                        .put("minute", JSONObject().put("type", "integer").put("description", "Minute 0..59"))
                        .put("days_of_week", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string").put("enum", JSONArray(listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")))))
                        .put("label", JSONObject().put("type", "string"))
                        .put("goal", JSONObject().put("type", "string"))
                        .put("wake_prompt", JSONObject().put("type", "string"))
                        .put("difficulty", JSONObject().put("type", "string").put("enum", JSONArray(listOf("EASY", "MEDIUM", "HARD"))))
                        .put("category", JSONObject().put("type", "string").put("enum", JSONArray(listOf("MIXED", "MATH", "LOGIC", "MEMORY", "WORDS", "STROOP", "TIME_MATH", "GOAL_AFFIRMATION"))))
                        .put("questions_required", JSONObject().put("type", "integer"))
                        .put("wake_check_enabled", JSONObject().put("type", "boolean")),
                    required = listOf("hour", "minute")
                )
            )
            put(
                toolSpec(
                    name = "save_task_table",
                    description = "Save a structured task & study technique table extracted from an uploaded timetable, routine, study notes, or goals.",
                    props = JSONObject()
                        .put("plan_title", JSONObject().put("type", "string").put("description", "Short title for the generated plan"))
                        .put(
                            "tasks",
                            JSONObject().put("type", "array").put(
                                "items",
                                JSONObject().put("type", "object").put(
                                    "properties",
                                    JSONObject()
                                        .put("time_slot", JSONObject().put("type", "string").put("description", "e.g. 07:00 AM or Block 1"))
                                        .put("task", JSONObject().put("type", "string").put("description", "Concrete task or class"))
                                        .put("technique", JSONObject().put("type", "string").put("description", "Recommended study/focus method, e.g. Pomodoro 50/10, Active Recall"))
                                        .put("alarm_hour", JSONObject().put("type", "integer").put("description", "Optional suggested alarm hour 0..23"))
                                        .put("alarm_minute", JSONObject().put("type", "integer").put("description", "Optional suggested alarm minute 0..59"))
                                ).put("required", JSONArray(listOf("time_slot", "task", "technique")))
                            )
                        ),
                    required = listOf("plan_title", "tasks")
                )
            )
            put(
                toolSpec(
                    name = "toggle_alarm",
                    description = "Enable or disable a specific alarm by alarm_id, or pass alarm_id = -1 to toggle all alarms.",
                    props = JSONObject()
                        .put("alarm_id", JSONObject().put("type", "integer"))
                        .put("is_enabled", JSONObject().put("type", "boolean")),
                    required = listOf("alarm_id", "is_enabled")
                )
            )
            put(
                toolSpec(
                    name = "delete_alarm",
                    description = "Delete an existing alarm by its alarm_id.",
                    props = JSONObject().put("alarm_id", JSONObject().put("type", "integer")),
                    required = listOf("alarm_id")
                )
            )
            put(
                toolSpec(
                    name = "start_pomodoro",
                    description = "Configure and optionally launch a Pomodoro focus session for a goal.",
                    props = JSONObject()
                        .put("goal", JSONObject().put("type", "string"))
                        .put("preset", JSONObject().put("type", "string").put("enum", JSONArray(listOf("SPRINT", "CLASSIC", "DEEP_WORK", "ULTRADIAN"))))
                        .put("auto_start", JSONObject().put("type", "boolean")),
                    required = listOf("goal")
                )
            )
            put(
                toolSpec(
                    name = "update_preferences",
                    description = "Update user preferences: name, default goal, difficulty, theme mode, or color palette.",
                    props = JSONObject()
                        .put("user_name", JSONObject().put("type", "string"))
                        .put("default_goal", JSONObject().put("type", "string"))
                        .put("default_difficulty", JSONObject().put("type", "string").put("enum", JSONArray(listOf("EASY", "MEDIUM", "HARD"))))
                        .put("theme_mode", JSONObject().put("type", "string").put("enum", JSONArray(listOf("SYSTEM", "LIGHT", "DARK"))))
                        .put("theme_palette", JSONObject().put("type", "string").put("enum", JSONArray(AppThemePalette.entries.map { it.name }))),
                    required = emptyList()
                )
            )
            put(
                toolSpec(
                    name = "ultimate_morning_unlock",
                    description = "Ultimate wake-up verification: call immediately when a scanned face photo clearly shows a freshly washed morning face. Silences and unlocks all alarms and clears all tasks without requiring any answers.",
                    props = JSONObject()
                        .put("verification_note", JSONObject().put("type", "string").put("description", "Brief note confirming washed morning face detection")),
                    required = emptyList()
                )
            )
        }

        private fun toolSpec(
            name: String,
            description: String,
            props: JSONObject,
            required: List<String>
        ): JSONObject = JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put("properties", props)
                            .put("required", JSONArray(required))
                    )
            )
    }
}
