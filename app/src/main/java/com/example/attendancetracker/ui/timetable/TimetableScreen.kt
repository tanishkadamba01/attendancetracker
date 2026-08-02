package com.example.attendancetracker.ui.timetable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.CalendarToday
import com.example.attendancetracker.theme.Amber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.theme.Coral
import com.example.attendancetracker.theme.Indigo60
import com.example.attendancetracker.theme.Mint
import com.example.attendancetracker.theme.SubjectColorHexList
import kotlinx.coroutines.launch

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val DAY_FULL  = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(modifier: Modifier = Modifier) {
    val vm: TimetableViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject       by remember { mutableStateOf<Subject?>(null) }
    var showAddSlotDialog    by remember { mutableStateOf(false) }
    var showExportDialog     by remember { mutableStateOf(false) }
    var showImportDialog     by remember { mutableStateOf(false) }
    var exportedJson         by remember { mutableStateOf("") }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            existingSubjects = state.allSubjects,
            onDismiss = { showAddSubjectDialog = false },
            onAdd     = { name, color, code, targetPct ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.addSubject(name, color, code, targetPct) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showAddSubjectDialog = false
                }
            }
        )
    }

    if (editingSubject != null) {
        EditSubjectDialog(
            subject   = editingSubject!!,
            onDismiss = { editingSubject = null },
            onUpdate  = { updated ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.updateSubject(updated) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) editingSubject = null
                }
            }
        )
    }

    if (showAddSlotDialog) {
        AddSlotDialog(
            subjects  = state.allSubjects,
            day       = state.selectedDay,
            onDismiss = { showAddSlotDialog = false },
            onAdd     = { subjectId, day, start, end ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.addSlot(subjectId, day, start, end)
                showAddSlotDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportTimetableDialog(
            jsonText  = exportedJson,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog) {
        ImportTimetableDialog(
            initialStartDate     = state.startDate,
            hasExistingTimetable = state.allSubjects.isNotEmpty(),
            onDismiss            = { showImportDialog = false },
            onImport             = { jsonText, replace, selectedStart ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    val result = vm.importTimetableJson(jsonText, replace, selectedStart)
                    when (result) {
                        is ImportExportResult.Success -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            showImportDialog = false
                        }
                        is ImportExportResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "Timetable",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "Manage your weekly schedule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        exportedJson = vm.exportTimetableJson()
                        showExportDialog = true
                    }
                }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export", tint = Indigo60)
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showImportDialog = true
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = Mint)
                }
            }
        }

        // Class Start Date Header Card
        val startDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = if (state.startDate == null) Amber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
            border   = BorderStroke(1.dp, if (state.startDate == null) Amber else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (state.startDate == null) Amber else Indigo60, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text       = if (state.startDate == null) "Classes Start Date: Not set" else "Classes Start: ${state.startDate?.format(startDateFmt)}",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.startDate == null) {
                            Text("Set when classes begin so attendance tracking starts on day 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val initial = state.startDate ?: LocalDate.now()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                vm.setStartDate(LocalDate.of(year, month + 1, dayOfMonth))
                            },
                            initial.year,
                            initial.monthValue - 1,
                            initial.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (state.startDate == null) "Set Start Date" else "Edit", fontSize = 11.sp)
                }
            }
        }

        // Subjects Section
        SubjectsSection(
            subjects        = state.allSubjects,
            onAddSubject    = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddSubjectDialog = true
            },
            onEditSubject   = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                editingSubject = it
            },
            onDeleteSubject = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.deleteSubject(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Day Tabs
        ScrollableTabRow(
            selectedTabIndex = state.selectedDay - 1,
            containerColor   = MaterialTheme.colorScheme.surfaceVariant,
            contentColor     = Indigo60,
            edgePadding      = 16.dp
        ) {
            DAY_NAMES.forEachIndexed { idx, name ->
                Tab(
                    selected = state.selectedDay == idx + 1,
                    onClick  = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.selectDay(idx + 1)
                    },
                    text     = { Text(name, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slot List
        LazyColumn(
            modifier            = Modifier.weight(1f),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.slotsForDay.isEmpty()) {
                item {
                    EngagingTimetableEmptyState(
                        day = DAY_FULL[state.selectedDay - 1],
                        onAddSubject = { showAddSubjectDialog = true },
                        onImport = { showImportDialog = true }
                    )
                }
            } else {
                items(state.slotsForDay, key = { it.slot.id }) { sws ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(tween(300)) + slideInVertically(tween(300))
                    ) {
                        SlotCard(sws = sws, onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.deleteSlot(sws.slot)
                        })
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (state.allSubjects.isNotEmpty()) Indigo60.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = state.allSubjects.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddSlotDialog = true
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Add,
                            contentDescription = null,
                            tint               = if (state.allSubjects.isNotEmpty()) Indigo60 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = if (state.allSubjects.isEmpty()) "Add a subject first to add slots" else "Add Class Slot",
                            color      = if (state.allSubjects.isNotEmpty()) Indigo60 else MaterialTheme.colorScheme.onSurfaceVariant,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsSection(
    subjects: List<Subject>,
    onAddSubject: () -> Unit,
    onEditSubject: (Subject) -> Unit,
    onDeleteSubject: (Subject) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Subjects",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onAddSubject) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Indigo60)
            }
        }
        if (subjects.isEmpty()) {
            Text(
                text  = "No subjects yet. Tap + to create your first subject!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    SubjectChip(
                        subject  = subject,
                        onEdit   = { onEditSubject(subject) },
                        onDelete = { onDeleteSubject(subject) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectChip(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Indigo60 }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(subject.name, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = color.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        Box(
            modifier         = Modifier.size(16.dp).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = color, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun SlotCard(sws: SlotWithSubject, onDelete: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(sws.subject?.colorHex ?: "#6650A4")) } catch (e: Exception) { Indigo60 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = sws.subject?.name ?: "Unknown",
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "${sws.slot.startTime} – ${sws.slot.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete slot", tint = Coral.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun EngagingTimetableEmptyState(day: String, onAddSubject: () -> Unit, onImport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📚", fontSize = 48.sp)
            Text(
                text       = "Ready to track your attendance?",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text      = "No class slots added for $day yet. Create subjects above or import your timetable to get started!",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = onImport) {
                    Text("📥 Import JSON")
                }
                Button(onClick = onAddSubject) {
                    Text("+ Add Subject")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSubjectDialog(
    subject: Subject,
    onDismiss: () -> Unit,
    onUpdate: (Subject) -> Unit
) {
    var name             by remember { mutableStateOf(subject.name) }
    var code             by remember { mutableStateOf(subject.code) }
    var selectedColor    by remember { mutableStateOf(subject.colorHex) }
    var targetPct        by remember { mutableStateOf(subject.targetPercentage) }
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColor,
            onDismiss       = { showCustomPicker = false },
            onColorSelected = { customHex ->
                selectedColor    = customHex
                showCustomPicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Edit Subject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Subject Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = code,
                    onValueChange = { code = it },
                    label         = { Text("Subject Code (e.g. CS101)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Attendance", style = MaterialTheme.typography.bodyMedium)
                    Text("%.0f%%".format(targetPct), fontWeight = FontWeight.Bold, color = Indigo60)
                }
                Slider(
                    value         = targetPct,
                    onValueChange = { targetPct = it },
                    valueRange    = 50f..95f,
                    steps         = 8,
                    colors        = SliderDefaults.colors(thumbColor = Indigo60, activeTrackColor = Indigo60)
                )

                Text("Select Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SubjectColorHexList.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Indigo60 }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick  = {
                            if (name.isNotBlank()) {
                                onUpdate(subject.copy(name = name.trim(), code = code.trim(), colorHex = selectedColor, targetPercentage = targetPct))
                            }
                        },
                        enabled  = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun ExportTimetableDialog(jsonText: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Export Timetable (AI Compatible)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp).verticalScroll(rememberScrollState())
                ) {
                    Text(jsonText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Mint)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Timetable JSON", jsonText))
                            Toast.makeText(context, "Copied JSON to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy JSON", fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, jsonText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Timetable JSON"))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share Text", fontSize = 12.sp) }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
private fun ImportTimetableDialog(
    initialStartDate: LocalDate?,
    hasExistingTimetable: Boolean,
    onDismiss: () -> Unit,
    onImport: (jsonText: String, replace: Boolean, startDate: LocalDate) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var selectedStartDate by remember { mutableStateOf(initialStartDate ?: LocalDate.now()) }
    val context = LocalContext.current
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Import Timetable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Class Start Date Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Classes Start Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        selectedStartDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    },
                                    selectedStartDate.year,
                                    selectedStartDate.monthValue - 1,
                                    selectedStartDate.dayOfMonth
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Indigo60, modifier = Modifier.size(18.dp))
                            Text(selectedStartDate.format(dateFmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Text("Change", style = MaterialTheme.typography.labelSmall, color = Indigo60, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste JSON Content Here") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    maxLines = 10
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { if (jsonInput.isNotBlank()) onImport(jsonInput.trim(), hasExistingTimetable, selectedStartDate) },
                        enabled = jsonInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubjectDialog(existingSubjects: List<Subject>, onDismiss: () -> Unit, onAdd: (String, String, String, Float) -> Unit) {
    val autoAssignedColor = remember(existingSubjects) {
        val usedColors = existingSubjects.map { it.colorHex.uppercase() }.toSet()
        val available = SubjectColorHexList.filter { it.uppercase() !in usedColors }
        if (available.isNotEmpty()) available.random() else SubjectColorHexList.random()
    }
    var name          by remember { mutableStateOf("") }
    var code          by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(autoAssignedColor) }
    var targetPct     by remember { mutableStateOf(85f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add Subject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Subject Code (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { if (name.isNotBlank()) onAdd(name.trim(), selectedColor, code.trim(), targetPct) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun CustomColorPickerDialog(initialColorHex: String, onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    var hexInput by remember { mutableStateOf(initialColorHex) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Custom Color Wheel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = hexInput, onValueChange = { hexInput = it }, label = { Text("Hex Code (#RRGGBB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onColorSelected(if (!hexInput.startsWith("#")) "#$hexInput" else hexInput) }, modifier = Modifier.weight(1f)) { Text("Select") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSlotDialog(subjects: List<Subject>, day: Int, onDismiss: () -> Unit, onAdd: (Int, Int, String, String) -> Unit) {
    var selectedSubjectIdx by remember { mutableIntStateOf(0) }
    var selectedDay        by remember { mutableIntStateOf(day) }
    var startTime          by remember { mutableStateOf("09:00") }
    var endTime            by remember { mutableStateOf("10:00") }
    var subjectExpanded    by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Class Slot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(value = subjects.getOrNull(selectedSubjectIdx)?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("Subject") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        subjects.forEachIndexed { idx, subject ->
                            DropdownMenuItem(text = { Text(subject.name) }, onClick = { selectedSubjectIdx = idx; subjectExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { if (subjects.isNotEmpty()) onAdd(subjects[selectedSubjectIdx].id, selectedDay, startTime, endTime) }, modifier = Modifier.weight(1f)) { Text("Add") }
                }
            }
        }
    }
}
