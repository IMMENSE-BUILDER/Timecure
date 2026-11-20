package com.timecure.app.data.local

import android.content.Context
import android.content.SharedPreferences
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

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        val legacyKeys = listOf(
            "gateway_url",
            "api_key",
            "model_name",
            "ai_system_instructions",
            "ai_enabled",
            "last_ai_status"
        )
        if (legacyKeys.any { prefs.contains(it) }) {
            prefs.edit().apply {
                legacyKeys.forEach { remove(it) }
                remove(KEY_AI_BLOCKED_UNTIL)
            }.apply()
        }
    }

    /** After a transient server failure the alarm challenge engine cools down briefly offline. */
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
