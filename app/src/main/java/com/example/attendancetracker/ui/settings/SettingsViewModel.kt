package com.example.attendancetracker.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.backup.BackupSerializer
import com.example.attendancetracker.data.local.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val defaultTarget: Float = 85f,
    val autoMarkTime: String = "5:00 PM",
    val isReminderEnabled: Boolean = true,
    val reminderTime: String = "8:00 PM",
    val startDate: LocalDate? = null
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

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

    val backupResult = MutableStateFlow<BackupResult?>(null)
    val isBackupInProgress = MutableStateFlow(false)

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

    // ── Export ─────────────────────────────────────────────────────────────────

    /**
     * Build the full backup JSON string (suspend, call from coroutine).
     */
    suspend fun buildBackupJson(): String {
        val subjects = repo.allSubjects.first()
        val slots    = repo.allSlots.first()
        val records  = repo.getAllRecords().first()
        val sharedPrefs = getApplication<Application>()
            .getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
        return BackupSerializer.buildBackupJson(subjects, slots, records, sharedPrefs)
    }

    /**
     * Write backup JSON to the given URI (from system file picker).
     */
    fun exportToUri(context: Context, uri: Uri, onComplete: (BackupResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isBackupInProgress.value = true
            val result = try {
                val json = buildBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("Could not open file for writing.")
                BackupResult.Success("Backup saved successfully!")
            } catch (e: Exception) {
                BackupResult.Error("Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
            isBackupInProgress.value = false
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    /**
     * Generate a filename for the backup document picker.
     */
    fun generateBackupFileName(): String = BackupSerializer.generateFileName()

    // ── Import ─────────────────────────────────────────────────────────────────

    /**
     * Read and parse the backup file at [uri], return parsed data for confirmation.
     * Does NOT write to the database yet.
     */
    fun readBackupFromUri(
        context: Context,
        uri: Uri,
        onResult: (BackupSerializer.ParseResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val jsonText = context.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.readBytes().toString(Charsets.UTF_8)
                } ?: throw Exception("Could not read file.")
                BackupSerializer.parseBackupJson(jsonText)
            } catch (e: Exception) {
                BackupSerializer.ParseResult.Error("Failed to read file: ${e.localizedMessage ?: "Unknown error"}")
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    /**
     * Perform the actual restore: clear all data, then insert from backup.
     * Restores SharedPreferences immediately.
     */
    fun restoreFromBackup(
        context: Context,
        data: BackupSerializer.BackupData,
        onComplete: (BackupResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isBackupInProgress.value = true
            val result = try {
                // 1. Wipe existing data (FK-safe order: records → slots → subjects)
                repo.deleteAllData()

                // 2. Insert backup data (subjects first, then slots, then records)
                repo.insertAllSubjects(data.subjects)
                repo.insertAllSlots(data.slots)
                repo.insertAllRecords(data.records)

                // 3. Restore preferences
                val sharedPrefs = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("theme_mode", data.themeMode)
                    .putFloat("default_target", data.defaultTarget)
                    .apply {
                        if (data.classStartDate != null)
                            putString("class_start_date", data.classStartDate)
                        else
                            remove("class_start_date")
                    }
                    .apply()

                // 4. Reflect preferences in live StateFlow
                withContext(Dispatchers.Main) {
                    val themeMode = try {
                        AppThemeMode.valueOf(data.themeMode)
                    } catch (e: Exception) { AppThemeMode.DARK }
                    themePrefs.setThemeMode(themeMode)
                    themePrefs.setDefaultTarget(data.defaultTarget)
                    if (data.classStartDate != null) {
                        val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE
                        val date = try { LocalDate.parse(data.classStartDate, isoFmt) } catch (e: Exception) { null }
                        themePrefs.setStartDate(date)
                    } else {
                        themePrefs.setStartDate(null)
                    }
                }

                BackupResult.Success(
                    "Restored ${data.subjects.size} subjects, ${data.slots.size} slots, and ${data.records.size} attendance records."
                )
            } catch (e: Exception) {
                BackupResult.Error("Restore failed: ${e.localizedMessage ?: "Unknown error"}. Your data may be in an inconsistent state. Please try again.")
            }
            isBackupInProgress.value = false
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }
}
