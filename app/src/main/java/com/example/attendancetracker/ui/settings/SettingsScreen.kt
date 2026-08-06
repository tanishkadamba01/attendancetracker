package com.example.attendancetracker.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancetracker.data.backup.BackupSerializer
import com.example.attendancetracker.data.local.AppThemeMode
import com.example.attendancetracker.theme.Coral
import com.example.attendancetracker.theme.Indigo60
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val isBackupInProgress by vm.isBackupInProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showResetDialog     by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }
    var showTermsDialog     by remember { mutableStateOf(false) }
    var showAboutDialog     by remember { mutableStateOf(false) }

    // Parsed backup awaiting user confirmation for replace
    var pendingImportData   by remember { mutableStateOf<BackupSerializer.BackupData?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // ── File Picker Launchers ─────────────────────────────────────────────────

    // Export: system file picker to create/save a new file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.exportToUri(context, uri) { result ->
            when (result) {
                is BackupResult.Success -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    // Also offer share sheet
                    scope.launch {
                        try {
                            val json = vm.buildBackupJson()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, "Attendance Tracker Backup")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
                        } catch (e: Exception) { /* share is optional */ }
                    }
                }
                is BackupResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Import: system file picker to open an existing file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.readBackupFromUri(context, uri) { parseResult ->
            when (parseResult) {
                is BackupSerializer.ParseResult.Success -> {
                    pendingImportData = parseResult.data
                    showImportConfirmDialog = true
                }
                is BackupSerializer.ParseResult.Error -> {
                    Toast.makeText(context, parseResult.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️ Reset All Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Coral)
                    Text("Are you sure you want to reset all subjects, timetable slots, and attendance records? This action cannot be undone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showResetDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.resetAllData {
                                    showResetDialog = false
                                    Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors   = ButtonDefaults.buttonColors(containerColor = Coral),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }

    // Import replace-existing confirmation dialog
    if (showImportConfirmDialog && pendingImportData != null) {
        val data = pendingImportData!!
        Dialog(onDismissRequest = {
            showImportConfirmDialog = false
            pendingImportData = null
        }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📦 Import Backup?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Indigo60)
                    Text(
                        "This backup contains:\n• ${data.subjects.size} subjects\n• ${data.slots.size} class slots\n• ${data.records.size} attendance records\n\nAll existing data will be replaced. This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                showImportConfirmDialog = false
                                pendingImportData = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showImportConfirmDialog = false
                                vm.restoreFromBackup(context, data) { result ->
                                    pendingImportData = null
                                    when (result) {
                                        is BackupResult.Success -> Toast.makeText(context, "✅ ${result.message}", Toast.LENGTH_LONG).show()
                                        is BackupResult.Error   -> Toast.makeText(context, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors   = ButtonDefaults.buttonColors(containerColor = Indigo60),
                            modifier = Modifier.weight(1f)
                        ) { Text("Replace & Restore") }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Attendance Tracker operates 100% offline. All your timetable data and attendance logs remain safely stored on your local device. We do not collect, transmit, or share any user data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showPrivacyDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
            }
        }
    }

    if (showTermsDialog) {
        Dialog(onDismissRequest = { showTermsDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Terms of Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("This application is provided for personal attendance tracking. You are free to use, export, and import your data anytime.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showTermsDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
            }
        }
    }

    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅 Attendance Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Indigo60)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Built with Jetpack Compose & Room for smart weekly attendance tracking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Button(onClick = { showAboutDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("OK") }
                }
            }
        }
    }

    // Progress overlay during backup operations
    if (isBackupInProgress) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color    = Indigo60,
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Processing backup…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar Header
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Appearance & Theme ────────────────────────
            item {
                SettingsCategoryCard(title = "Appearance", icon = Icons.Default.Palette) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ThemeOptionRow("☀️ Light Mode", state.themeMode == AppThemeMode.LIGHT) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setThemeMode(AppThemeMode.LIGHT)
                        }
                        ThemeOptionRow("🌙 Dark Mode", state.themeMode == AppThemeMode.DARK) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setThemeMode(AppThemeMode.DARK)
                        }
                        ThemeOptionRow("⚫ AMOLED Mode (Pure Black)", state.themeMode == AppThemeMode.AMOLED) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setThemeMode(AppThemeMode.AMOLED)
                        }
                    }
                }
            }

            // ── Section 2: Attendance ────────────────────────────────
            item {
                SettingsCategoryCard(title = "Attendance Target & Rules", icon = Icons.Default.Schedule) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Default Target", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("%.0f%%".format(state.defaultTarget), fontWeight = FontWeight.Bold, color = Indigo60)
                        }
                        Slider(
                            value         = state.defaultTarget,
                            onValueChange = { vm.setDefaultTarget(it) },
                            valueRange    = 50f..95f,
                            steps         = 8,
                            colors        = SliderDefaults.colors(thumbColor = Indigo60, activeTrackColor = Indigo60)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        val startDateStr = state.startDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "Not set"
                        SettingsClickableRow("Classes Start Date: $startDateStr") {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val initial = state.startDate ?: java.time.LocalDate.now()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    vm.setStartDate(java.time.LocalDate.of(year, month + 1, dayOfMonth))
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth
                            ).show()
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        SettingsRow("Auto Mark Time", state.autoMarkTime)
                        SettingsRow("Reminders", "Enabled (8:00 PM)")
                    }
                }
            }

            // ── Section 3: Data Management ──────────────────────────
            item {
                SettingsCategoryCard(title = "Data Management", icon = Icons.Default.SwapVert) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Export
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isBackupInProgress) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    exportLauncher.launch(vm.generateBackupFileName())
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Indigo60, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Export Attendance Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Save a full backup to your device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Import
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isBackupInProgress) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Indigo60, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Import Attendance Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Restore from a backup file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        SettingsClickableRow("Reset All Data", textColor = Coral) {
                            showResetDialog = true
                        }
                    }
                }
            }

            // ── Section 4: General ───────────────────────────────────
            item {
                SettingsCategoryCard(title = "General", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsClickableRow("About") { showAboutDialog = true }
                        SettingsClickableRow("Privacy Policy") { showPrivacyDialog = true }
                        SettingsClickableRow("Terms of Service") { showTermsDialog = true }
                        SettingsClickableRow("Feedback / Contact Developer") {
                            Toast.makeText(context, "Contact developer@example.com", Toast.LENGTH_LONG).show()
                        }
                        SettingsClickableRow("Rate App") {
                            Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = Indigo60, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(selectedColor = Indigo60)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SettingsClickableRow(label: String, textColor: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColor)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
