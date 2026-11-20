package com.timecure.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.timecure.app.BuildConfig
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.ui.theme.AppThemePalette

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(s: String?): ThemeMode = entries.firstOrNull { it.name == s } ?: SYSTEM
    }
}

data class AiModelOption(
    val id: String,
    val displayName: String,
    val subtitle: String
)

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Defaults come from local.properties via BuildConfig, so no secret lives in source control. */
    var gatewayUrl: String
        get() = prefs.getString(KEY_GATEWAY_URL, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.AI_GATEWAY_URL
        set(value) = prefs.edit().putString(KEY_GATEWAY_URL, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.AI_GATEWAY_API_KEY
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isEmpty() || trimmed == BuildConfig.AI_GATEWAY_API_KEY) {
                prefs.edit().remove(KEY_API_KEY).apply()
            } else {
                prefs.edit().putString(KEY_API_KEY, trimmed).apply()
            }
        }

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.AI_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value.trim()).apply()

    var aiSystemInstructions: String
        get() = prefs.getString(KEY_AI_SYSTEM_INSTRUCTIONS, null)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AI_SYSTEM_INSTRUCTIONS
        set(value) = prefs.edit().putString(KEY_AI_SYSTEM_INSTRUCTIONS, value.trim()).apply()

    var aiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()

    /** Human-readable result of the last AI call, shown in Settings. Empty = never called. */
    var lastAiStatus: String
        get() = prefs.getString(KEY_LAST_AI_STATUS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_AI_STATUS, value).apply()

    /** After a failure the AI is skipped for a while so a dead key never delays an alarm. */
    var aiBlockedUntil: Long
        get() = prefs.getLong(KEY_AI_BLOCKED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_AI_BLOCKED_UNTIL, value).apply()

    var defaultGoal: String
        get() = prefs.getString(KEY_DEFAULT_GOAL, DEFAULT_GOAL) ?: DEFAULT_GOAL
        set(value) = prefs.edit().putString(KEY_DEFAULT_GOAL, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var defaultDifficulty: ChallengeDifficulty
        get() = ChallengeDifficulty.entries.firstOrNull {
            it.name == prefs.getString(KEY_DEFAULT_DIFFICULTY, null)
        } ?: ChallengeDifficulty.MEDIUM
        set(value) = prefs.edit().putString(KEY_DEFAULT_DIFFICULTY, value.name).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null))
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var themePalette: AppThemePalette
        get() = AppThemePalette.fromString(prefs.getString(KEY_THEME_PALETTE, null))
        set(value) = prefs.edit().putString(KEY_THEME_PALETTE, value.name).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    companion object {
        private const val PREFS_NAME = "timecure_preferences"
        const val DEFAULT_GOAL = "Win the morning"
        const val DEFAULT_AI_SYSTEM_INSTRUCTIONS =
            "You are Timecure's disciplined, high-agency personal performance coach. Help the user beat morning inertia, organize college & deep-work routines cleanly, recommend science-backed study methods (Pomodoro, Active Recall, Spaced Repetition), and take direct action via tools."

        val SUPPORTED_MODELS = listOf(
            AiModelOption(
                id = "minimax/minimax-m3.1-flash",
                displayName = "MiniMax M3.1 Flash",
                subtitle = "Fast multimodal reasoning & tool calling"
            ),
            AiModelOption(
                id = "meta/muse-ember",
                displayName = "Meta Muse Ember",
                subtitle = "Vercel Gateway multimodal reasoning model"
            )
        )

        private const val KEY_GATEWAY_URL = "gateway_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_AI_SYSTEM_INSTRUCTIONS = "ai_system_instructions"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_LAST_AI_STATUS = "last_ai_status"
        private const val KEY_AI_BLOCKED_UNTIL = "ai_blocked_until"
        private const val KEY_DEFAULT_GOAL = "default_goal"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEFAULT_DIFFICULTY = "default_difficulty"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_PALETTE = "theme_palette"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
