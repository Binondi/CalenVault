package com.devsorg.calenvault.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.devsorg.calenvault.data.model.EventCategory
import com.google.android.material.color.DynamicColors
import java.util.Calendar

enum class ThemeMode(val value: Int, val titleResName: String) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "System default"),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, "Light"),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, "Dark");

    companion object {
        fun fromValue(value: Int): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

enum class TimeFormatPref(val value: Int) {
    SYSTEM(0),
    FORMAT_12H(1),
    FORMAT_24H(2);

    companion object {
        fun fromValue(value: Int): TimeFormatPref {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "calenvault_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLORS = "key_dynamic_colors"
        private const val KEY_FIRST_DAY_OF_WEEK = "key_first_day_of_week"
        private const val KEY_TIME_FORMAT = "key_time_format"
        private const val KEY_DEFAULT_DURATION = "key_default_duration"
        private const val KEY_DEFAULT_CATEGORY = "key_default_category"
        private const val KEY_HIGHLIGHT_WEEKENDS = "key_highlight_weekends"
        private const val KEY_SHOW_EVENT_DOTS = "key_show_event_dots"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var themeMode: ThemeMode
        get() {
            val value = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            return ThemeMode.fromValue(value)
        }
        set(mode) {
            prefs.edit().putInt(KEY_THEME_MODE, mode.value).apply()
            applyTheme(mode)
        }

    var isDynamicColorsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLORS, true)
        set(enabled) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
        }

    var firstDayOfWeek: Int
        get() = prefs.getInt(KEY_FIRST_DAY_OF_WEEK, Calendar.SUNDAY)
        set(day) {
            prefs.edit().putInt(KEY_FIRST_DAY_OF_WEEK, day).apply()
        }

    var timeFormat: TimeFormatPref
        get() {
            val value = prefs.getInt(KEY_TIME_FORMAT, TimeFormatPref.SYSTEM.value)
            return TimeFormatPref.fromValue(value)
        }
        set(pref) {
            prefs.edit().putInt(KEY_TIME_FORMAT, pref.value).apply()
        }

    var defaultDurationMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 60)
        set(duration) {
            prefs.edit().putInt(KEY_DEFAULT_DURATION, duration).apply()
        }

    var defaultCategory: EventCategory
        get() {
            val id = prefs.getString(KEY_DEFAULT_CATEGORY, EventCategory.WORK.id)
            return EventCategory.fromId(id)
        }
        set(category) {
            prefs.edit().putString(KEY_DEFAULT_CATEGORY, category.id).apply()
        }

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHT_WEEKENDS, true)
        set(enabled) {
            prefs.edit().putBoolean(KEY_HIGHLIGHT_WEEKENDS, enabled).apply()
        }

    var showEventDots: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EVENT_DOTS, true)
        set(enabled) {
            prefs.edit().putBoolean(KEY_SHOW_EVENT_DOTS, enabled).apply()
        }

    fun applyTheme(mode: ThemeMode = themeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.value)
    }

    fun isDynamicColorsSupported(): Boolean {
        return DynamicColors.isDynamicColorAvailable()
    }
}
