package com.example.attendancetracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.local.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.time.LocalDate

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val defaultTarget: Float = 85f,
    val autoMarkTime: String = "5:00 PM",
    val isReminderEnabled: Boolean = true,
    val reminderTime: String = "8:00 PM",
    val startDate: LocalDate? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AttendanceApplication
    private val repo = app.repository
    private val themePrefs = app.themePreferences

    val uiState: StateFlow<SettingsUiState> = combine(
        themePrefs.themeMode,
        themePrefs.defaultTarget,
        themePrefs.startDate
    ) { mode, target, start ->
        SettingsUiState(
            themeMode     = mode,
            defaultTarget = target,
            startDate     = start
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: AppThemeMode) {
        themePrefs.setThemeMode(mode)
    }

    fun setDefaultTarget(target: Float) {
        themePrefs.setDefaultTarget(target)
    }

    fun setStartDate(date: LocalDate?) {
        themePrefs.setStartDate(date)
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAllData()
            themePrefs.setStartDate(null)
            onComplete()
        }
    }
}
