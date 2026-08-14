package com.example.attendancetracker.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AppThemeMode { LIGHT, DARK, AMOLED }

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode

    private val _defaultTarget = MutableStateFlow(prefs.getFloat("default_target", 85f))
    val defaultTarget: StateFlow<Float> = _defaultTarget

    private val _startDate = MutableStateFlow(getSavedStartDate())
    val startDate: StateFlow<LocalDate?> = _startDate

    private fun getSavedStartDate(): LocalDate? {
        val str = prefs.getString("class_start_date", null) ?: return null
        return try { LocalDate.parse(str, isoFmt) } catch (e: Exception) { null }
    }

    fun setStartDate(date: LocalDate?) {
        if (date == null) {
            prefs.edit().remove("class_start_date").apply()
        } else {
            prefs.edit().putString("class_start_date", date.format(isoFmt)).apply()
        }
        _startDate.value = date
    }

    private fun getSavedThemeMode(): AppThemeMode {
        val saved = prefs.getString("theme_mode", AppThemeMode.DARK.name)
        return try { AppThemeMode.valueOf(saved ?: AppThemeMode.DARK.name) } catch (e: Exception) { AppThemeMode.DARK }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultTarget(target: Float) {
        prefs.edit().putFloat("default_target", target).apply()
        _defaultTarget.value = target
    }

    fun getLastSundayPromptDate(): String? = prefs.getString("last_sunday_update_prompt_date", null)

    fun setLastSundayPromptDate(dateStr: String) {
        prefs.edit().putString("last_sunday_update_prompt_date", dateStr).apply()
    }
}
