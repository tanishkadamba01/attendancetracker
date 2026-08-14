package com.example.attendancetracker.ui.timetable

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import com.example.attendancetracker.data.timetable.TimetableSerializer
import com.example.attendancetracker.theme.SubjectColorHexList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class SlotWithSubject(
    val slot: TimetableSlot,
    val subject: Subject?
)

data class TimetableUiState(
    val selectedDay: Int = 1,  // 1=Monday…6=Saturday
    val slotsForDay: List<SlotWithSubject> = emptyList(),
    val allSubjects: List<Subject> = emptyList(),
    val startDate: LocalDate? = null,
    val isExportingOrImporting: Boolean = false
)

sealed class ImportExportResult {
    data class Success(val message: String) : ImportExportResult()
    data class Error(val message: String) : ImportExportResult()
}

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AttendanceApplication
    private val repo = app.repository
    private val themePrefs = app.themePreferences

    private val _selectedDay = MutableStateFlow(1)
    private val _isBusy = MutableStateFlow(false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _slotsForDay: Flow<List<SlotWithSubject>> = _selectedDay.flatMapLatest { day ->
        combine(repo.getSlotsForDay(day), repo.allSubjects) { slots, subjects ->
            slots.map { slot ->
                SlotWithSubject(slot = slot, subject = subjects.find { it.id == slot.subjectId })
            }
        }
    }

    val uiState: StateFlow<TimetableUiState> = combine(
        _selectedDay,
        _slotsForDay,
        repo.allSubjects,
        themePrefs.startDate,
        _isBusy
    ) { day, slots, subjects, start, busy ->
        TimetableUiState(
            selectedDay  = day,
            slotsForDay  = slots,
            allSubjects  = subjects,
            startDate    = start,
            isExportingOrImporting = busy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimetableUiState())

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun setStartDate(date: LocalDate?) {
        themePrefs.setStartDate(date)
    }

    fun addSubject(
        name: String,
        colorHex: String,
        code: String = "",
        targetPct: Float = 85f,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repo.getSubjectByName(name)
            if (existing != null) {
                onResult(false, "Subject '$name' already exists.")
                return@launch
            }
            repo.insertSubject(
                Subject(
                    name = name,
                    colorHex = colorHex,
                    code = code,
                    targetPercentage = targetPct
                )
            )
            onResult(true, "Added '$name'")
        }
    }

    fun updateSubject(subject: Subject, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repo.updateSubject(subject)
            onResult(true, "Updated '${subject.name}'")
        }
    }

    fun deleteSubject(subject: Subject, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            repo.deleteSubject(subject)
            onResult?.invoke(true, "Deleted '${subject.name}' and its timetable slots.")
        }
    }

    fun addSlot(
        subjectId: Int,
        day: Int,
        startTime: String,
        endTime: String,
        room: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val conflict = repo.findOverlappingSlot(day, startTime, endTime)
            if (conflict != null) {
                val conflictingSubject = repo.getSubjectById(conflict.subjectId)
                val name = conflictingSubject?.name ?: "another class"
                onResult(
                    false,
                    "Time conflict: Overlaps with '$name' (${conflict.startTime} - ${conflict.endTime})"
                )
                return@launch
            }
            repo.insertSlot(
                TimetableSlot(
                    subjectId = subjectId,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = endTime,
                    room = room
                )
            )
            onResult(true, "Class added to schedule!")
        }
    }

    fun deleteSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repo.deleteSlot(slot)
        }
    }

    // ── Import / Export Logic ─────────────────────────────────────────────────

    fun generateExportFileName(isTemplate: Boolean = false): String {
        return TimetableSerializer.generateFileName(isTemplate)
    }

    suspend fun exportTimetableJson(isTemplate: Boolean = false): String {
        return if (isTemplate) {
            TimetableSerializer.buildAiTemplateJson()
        } else {
            val subjects = repo.allSubjects.first()
            val slots = repo.allSlots.first()
            val startDate = themePrefs.startDate.first()
            if (subjects.isEmpty() && slots.isEmpty()) {
                TimetableSerializer.buildAiTemplateJson()
            } else {
                TimetableSerializer.buildTimetableJson(subjects, slots, startDate)
            }
        }
    }

    /**
     * Write timetable JSON directly to the given SAF URI.
     */
    fun exportToUri(
        context: Context,
        uri: Uri,
        isTemplate: Boolean,
        onComplete: (ImportExportResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val result = try {
                val json = exportTimetableJson(isTemplate)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("Could not open file destination for writing.")
                ImportExportResult.Success(
                    if (isTemplate) "Sample AI template saved successfully!"
                    else "Timetable schedule JSON exported successfully!"
                )
            } catch (e: Exception) {
                ImportExportResult.Error("Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
            _isBusy.value = false
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    /**
     * Reads and parses a JSON timetable file from a SAF Uri.
     */
    fun readTimetableFromUri(
        context: Context,
        uri: Uri,
        onResult: (TimetableSerializer.ParseResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val jsonText = context.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.readBytes().toString(Charsets.UTF_8)
                } ?: throw Exception("Could not read file.")
                TimetableSerializer.parseTimetableJson(jsonText)
            } catch (e: Exception) {
                TimetableSerializer.ParseResult.Error("Failed to read file: ${e.localizedMessage ?: "Unknown error"}")
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun parseTimetableText(jsonText: String): TimetableSerializer.ParseResult {
        return TimetableSerializer.parseTimetableJson(jsonText)
    }

    /**
     * Applies parsed timetable data to the database.
     */
    suspend fun applyParsedTimetable(
        parsed: TimetableSerializer.ParsedTimetable,
        replaceExisting: Boolean,
        startDate: LocalDate? = null
    ): ImportExportResult = withContext(Dispatchers.IO) {
        try {
            val effectiveStartDate = startDate ?: parsed.classStartDate
            if (effectiveStartDate != null) {
                themePrefs.setStartDate(effectiveStartDate)
            }

            if (replaceExisting) {
                repo.deleteAllSlots()
                repo.deleteAllSubjects()
            }

            var importedSubjects = 0
            var importedSlots = 0
            var skippedSlots = 0

            val subjectIdByName = mutableMapOf<String, Int>()

            // 1. Upsert subjects
            for (parsedSub in parsed.subjects) {
                val name = parsedSub.name.trim()
                if (name.isNotBlank()) {
                    val colorHex = if (parsedSub.colorHex.isNotBlank()) {
                        parsedSub.colorHex
                    } else {
                        pickColor(subjectIdByName.size)
                    }

                    val existing = repo.getSubjectByName(name)
                    if (existing != null) {
                        repo.updateSubject(
                            existing.copy(
                                colorHex = colorHex,
                                code = if (parsedSub.code.isNotBlank()) parsedSub.code else existing.code,
                                targetPercentage = parsedSub.targetPercentage
                            )
                        )
                        subjectIdByName[name] = existing.id
                    } else {
                        val newId = repo.insertSubject(
                            Subject(
                                name = name,
                                colorHex = colorHex,
                                code = parsedSub.code,
                                targetPercentage = parsedSub.targetPercentage
                            )
                        )
                        subjectIdByName[name] = newId.toInt()
                        importedSubjects++
                    }
                }
            }

            // 2. Insert slots
            for (slot in parsed.slots) {
                val subjectName = slot.subjectName.trim()
                if (subjectName.isBlank() || slot.dayOfWeek !in 1..7 || slot.startTime.isBlank() || slot.endTime.isBlank()) {
                    skippedSlots++
                    continue
                }

                val subjectId = subjectIdByName.getOrPut(subjectName) {
                    val existing = repo.getSubjectByName(subjectName)
                    if (existing != null) {
                        existing.id
                    } else {
                        val color = pickColor(subjectIdByName.size)
                        val newId = repo.insertSubject(Subject(name = subjectName, colorHex = color))
                        importedSubjects++
                        newId.toInt()
                    }
                }

                repo.insertSlot(
                    TimetableSlot(
                        subjectId = subjectId,
                        dayOfWeek = slot.dayOfWeek,
                        startTime = slot.startTime,
                        endTime   = slot.endTime,
                        room      = slot.room
                    )
                )
                importedSlots++
            }

            val msg = buildString {
                append("Imported $importedSubjects subject${if (importedSubjects == 1) "" else "s"}")
                append(", $importedSlots slot${if (importedSlots == 1) "" else "s"}")
                if (skippedSlots > 0) append(" ($skippedSlots invalid slots skipped)")
            }
            ImportExportResult.Success(msg)
        } catch (e: Exception) {
            ImportExportResult.Error("Import error: ${e.localizedMessage ?: "Database insertion failed"}")
        }
    }

    suspend fun importTimetableJson(
        jsonText: String,
        replaceExisting: Boolean,
        startDate: LocalDate? = null
    ): ImportExportResult {
        return when (val parseResult = TimetableSerializer.parseTimetableJson(jsonText)) {
            is TimetableSerializer.ParseResult.Success -> {
                applyParsedTimetable(parseResult.data, replaceExisting, startDate)
            }
            is TimetableSerializer.ParseResult.Error -> {
                ImportExportResult.Error(parseResult.message)
            }
        }
    }

    private fun pickColor(index: Int): String =
        SubjectColorHexList[index % SubjectColorHexList.size]
}
