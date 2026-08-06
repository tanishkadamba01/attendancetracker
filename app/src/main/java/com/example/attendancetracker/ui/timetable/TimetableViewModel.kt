package com.example.attendancetracker.ui.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import com.example.attendancetracker.theme.SubjectColorHexList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

import java.time.LocalDate

data class SlotWithSubject(
    val slot: TimetableSlot,
    val subject: Subject?
)

data class TimetableUiState(
    val selectedDay: Int = 1,  // 1=Monday…6=Saturday
    val slotsForDay: List<SlotWithSubject> = emptyList(),
    val allSubjects: List<Subject> = emptyList(),
    val startDate: LocalDate? = null
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
        themePrefs.startDate
    ) { day, slots, subjects, start ->
        TimetableUiState(
            selectedDay  = day,
            slotsForDay  = slots,
            allSubjects  = subjects,
            startDate    = start
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimetableUiState())

    fun selectDay(day: Int) { _selectedDay.value = day }

    fun setStartDate(date: LocalDate?) {
        themePrefs.setStartDate(date)
    }

    fun addSubject(name: String, colorHex: String, code: String, targetPct: Float, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repo.getSubjectByName(name)
            if (existing != null) {
                onResult(false, "A subject named '$name' already exists.")
            } else {
                repo.insertSubject(Subject(name = name, colorHex = colorHex, code = code, targetPercentage = targetPct))
                onResult(true, "Subject added!")
            }
        }
    }

    fun updateSubject(subject: Subject, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repo.getSubjectByName(subject.name)
            if (existing != null && existing.id != subject.id) {
                onResult(false, "A subject named '${subject.name}' already exists.")
            } else {
                repo.updateSubject(subject)
                onResult(true, "Subject updated!")
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { repo.deleteSubject(subject) }
    }

    /**
     * Add a new class slot with overlap validation.
     * @param onResult callback: success=true with message, or success=false with error message.
     */
    fun addSlot(
        subjectId: Int,
        day: Int,
        startTime: String,
        endTime: String,
        room: String = "",
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            // Validate end > start
            try {
                val start = java.time.LocalTime.parse(startTime)
                val end   = java.time.LocalTime.parse(endTime)
                if (!end.isAfter(start)) {
                    onResult?.invoke(false, "End time must be after start time.")
                    return@launch
                }
            } catch (e: Exception) {
                onResult?.invoke(false, "Invalid time format.")
                return@launch
            }

            // Check for overlaps
            val overlapping = repo.findOverlappingSlot(day, startTime, endTime)
            if (overlapping != null) {
                val subjectName = repo.getSubjectById(overlapping.subjectId)?.name ?: "another class"
                val dayName = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday").getOrElse(day-1){"day"}
                onResult?.invoke(false, "⚠️ Conflict on $dayName: overlaps with '$subjectName' (${overlapping.startTime}–${overlapping.endTime})")
                return@launch
            }

            repo.insertSlot(TimetableSlot(subjectId = subjectId, dayOfWeek = day, startTime = startTime, endTime = endTime, room = room.trim()))
            onResult?.invoke(true, "Class slot added!")
        }
    }

    fun deleteSlot(slot: TimetableSlot) {
        viewModelScope.launch { repo.deleteSlot(slot) }
    }

    // ── Import / Export Logic ─────────────────────────────────

    private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    suspend fun exportTimetableJson(): String {
        val subjects = repo.allSubjects.first()
        val slots    = repo.allSlots.first()

        val json = JSONObject()
        json.put("semester", "Semester 1")

        val workingDaysArr = JSONArray()
        dayNames.forEach { workingDaysArr.put(it) }
        json.put("workingDays", workingDaysArr)

        val timeSlotsArr = JSONArray()
        val defaultTimes = listOf("09:00-10:00", "10:00-11:00", "11:15-12:15", "01:00-02:00", "02:00-03:00")
        defaultTimes.forEach { timeSlotsArr.put(it) }
        json.put("timeSlots", timeSlotsArr)

        val timetableObj = JSONObject()
        dayNames.forEachIndexed { idx, dayName ->
            val dayInt = idx + 1
            val daySlots = slots.filter { it.dayOfWeek == dayInt }.sortedBy { it.startTime }
            val dayArr = JSONArray()
            daySlots.forEach { slot ->
                val sub = subjects.find { it.id == slot.subjectId }
                val slotObj = JSONObject()
                slotObj.put("subject", sub?.name ?: "Unknown")
                slotObj.put("time", "${slot.startTime}-${slot.endTime}")
                if (slot.room.isNotBlank()) slotObj.put("room", slot.room)
                dayArr.put(slotObj)
            }
            timetableObj.put(dayName, dayArr)
        }
        json.put("timetable", timetableObj)

        return json.toString(2)
    }

    suspend fun importTimetableJson(jsonText: String, replaceExisting: Boolean, startDate: LocalDate? = null): ImportExportResult {
        return try {
            val json = JSONObject(jsonText)

            if (!json.has("timetable")) {
                return ImportExportResult.Error("Validation Error: Missing required 'timetable' section in JSON.")
            }

            val timetableObj = json.getJSONObject("timetable")
            if (timetableObj.length() == 0) {
                return ImportExportResult.Error("Validation Error: The 'timetable' section is empty.")
            }

            if (startDate != null) {
                themePrefs.setStartDate(startDate)
            }

            if (replaceExisting) {
                repo.deleteAllSlots()
                repo.deleteAllSubjects()
            }

            val defaultTimePairs = listOf(
                Pair("09:00", "10:00"),
                Pair("10:00", "11:00"),
                Pair("11:15", "12:15"),
                Pair("13:00", "14:00"),
                Pair("14:00", "15:00")
            )

            var importedSlotsCount = 0
            var importedSubjectsCount = 0

            dayNames.forEachIndexed { idx, dayName ->
                val dayInt = idx + 1
                if (timetableObj.has(dayName)) {
                    val dayArr = timetableObj.getJSONArray(dayName)
                    for (i in 0 until dayArr.length()) {
                        val slotItem = dayArr.getJSONObject(i)
                        val subjectName = slotItem.optString("subject", "").trim()

                        if (subjectName.isNotBlank() && !subjectName.equals("Break", ignoreCase = true)) {
                            // Find or create subject
                            var subject = repo.getSubjectByName(subjectName)
                            if (subject == null) {
                                val existingSubjects = repo.allSubjects.first()
                                val usedColors = existingSubjects.map { it.colorHex.uppercase() }.toSet()
                                val availableColors = SubjectColorHexList.filter { it.uppercase() !in usedColors }
                                val color = if (availableColors.isNotEmpty()) availableColors.random() else SubjectColorHexList.random()

                                val newId = repo.insertSubject(Subject(name = subjectName, colorHex = color))
                                subject = Subject(id = newId.toInt(), name = subjectName, colorHex = color)
                                importedSubjectsCount++
                            }

                            // Extract start/end time
                            var startTime = defaultTimePairs.getOrElse(i) { Pair("09:00", "10:00") }.first
                            var endTime   = defaultTimePairs.getOrElse(i) { Pair("09:00", "10:00") }.second

                            if (slotItem.has("time")) {
                                val timeStr = slotItem.getString("time")
                                val parts = timeStr.split("-")
                                if (parts.size == 2) {
                                    startTime = parts[0].trim()
                                    endTime   = parts[1].trim()
                                }
                            }

                            val room = slotItem.optString("room", "")

                            repo.insertSlot(
                                TimetableSlot(
                                    subjectId = subject.id,
                                    dayOfWeek = dayInt,
                                    startTime = startTime,
                                    endTime   = endTime,
                                    room      = room
                                )
                            )
                            importedSlotsCount++
                        }
                    }
                }
            }

            ImportExportResult.Success("Successfully imported timetable with $importedSubjectsCount subjects and $importedSlotsCount slots!")
        } catch (e: Exception) {
            ImportExportResult.Error("JSON Parse Error: ${e.localizedMessage ?: "Invalid format"}")
        }
    }
}
