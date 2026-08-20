package com.example.attendancetracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class SlotWithDetails(
    val slot: TimetableSlot,
    val subject: Subject?,
    val record: AttendanceRecord?,
    val overrideSubject: Subject? = null
)

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val slotsWithDetails: List<SlotWithDetails> = emptyList(),
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0,
    val attendancePercentage: Float = 0f,
    val allSubjects: List<Subject> = emptyList(),
    val isAfter5PM: Boolean = LocalTime.now().hour >= 17,
    val startDate: LocalDate? = null,
    val canGoPrevWeek: Boolean = true,
    val isBeforeStartDate: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AttendanceApplication
    private val repo = app.repository
    private val themePrefs = app.themePreferences
    private val fmt  = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            themePrefs.startDate.collect { start ->
                val current = _selectedDate.value
                if (start != null) {
                    val startWeekMonday = start.with(DayOfWeek.MONDAY)
                    if (current.with(DayOfWeek.MONDAY).isBefore(startWeekMonday)) {
                        _selectedDate.value = start
                    }
                }
                repo.checkAndAutoConfirm5PM(_selectedDate.value, start)
            }
        }
    }

    private val _slotsWithDetails: Flow<List<SlotWithDetails>> = combine(_selectedDate, themePrefs.startDate) { date, start ->
        Pair(date, start)
    }.flatMapLatest { (date, start) ->
        viewModelScope.launch {
            repo.checkAndAutoConfirm5PM(date, start)
        }
        val dayOfWeek = date.dayOfWeek.value // 1=Monday … 7=Sunday
        if (dayOfWeek > 6) return@flatMapLatest flowOf(emptyList()) // Sunday has no classes
        combine(
            repo.getSlotsForDay(dayOfWeek),
            repo.allSubjects,
            repo.getRecordsForDate(date.format(fmt))
        ) { slots, subjects, records ->
            slots.map { slot ->
                val rec = records.find { it.slotId == slot.id }
                val overrideSub = if (rec?.overrideSubjectId != null) {
                    subjects.find { it.id == rec.overrideSubjectId }
                } else null

                SlotWithDetails(
                    slot            = slot,
                    subject         = subjects.find { it.id == slot.subjectId },
                    record          = rec,
                    overrideSubject = overrideSub
                )
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        _slotsWithDetails,
        repo.allSubjects,
        themePrefs.startDate
    ) { date, slots, subjects, start ->
        val now = LocalTime.now()
        val isBeforeStart = start != null && date.isBefore(start)
        val isAfter5PM = if (isBeforeStart) false else (date.isBefore(LocalDate.now()) || (date == LocalDate.now() && now.hour >= 17))

        val startWeekMonday = start?.with(DayOfWeek.MONDAY)
        val selectedMonday = date.with(DayOfWeek.MONDAY)
        val canPrev = startWeekMonday == null || selectedMonday.isAfter(startWeekMonday)

        // Cancelled classes are NOT counted towards total or attended
        val activeSlots = slots.filter { it.record?.status != AttendanceStatus.CANCELLED }
        val totalForDay = activeSlots.size
        val attendedForDay = activeSlots.count { swd ->
            val status = swd.record?.status
            when {
                status == AttendanceStatus.PRESENT    -> true
                status == AttendanceStatus.REASSIGNED -> true
                isAfter5PM && (status == null || status == AttendanceStatus.PENDING) -> true
                else -> false
            }
        }
        val pctForDay = if (totalForDay > 0) (attendedForDay.toFloat() / totalForDay) * 100f else 0f

        HomeUiState(
            selectedDate         = date,
            slotsWithDetails     = slots,
            totalClasses         = totalForDay,
            attendedClasses      = attendedForDay,
            attendancePercentage = pctForDay,
            allSubjects          = subjects,
            isAfter5PM           = isAfter5PM,
            startDate            = start,
            canGoPrevWeek        = canPrev,
            isBeforeStartDate    = isBeforeStart
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setDate(date: LocalDate) {
        val start = themePrefs.startDate.value
        if (start != null) {
            val startWeekMonday = start.with(DayOfWeek.MONDAY)
            if (date.with(DayOfWeek.MONDAY).isBefore(startWeekMonday)) {
                return
            }
        }
        _selectedDate.value = date
        viewModelScope.launch {
            repo.checkAndAutoConfirm5PM(date, start)
        }
    }

    fun previousWeek() {
        val start = themePrefs.startDate.value
        val minMonday = start?.with(DayOfWeek.MONDAY)
        val targetDate = _selectedDate.value.minusWeeks(1)
        if (minMonday != null && targetDate.with(DayOfWeek.MONDAY).isBefore(minMonday)) {
            return
        }
        setDate(targetDate)
    }

    fun nextWeek() {
        setDate(_selectedDate.value.plusWeeks(1))
    }

    /**
     * Toggle the missed/attended state of a slot, correctly handling all source states.
     *
     * Transitions:
     *  - Any status except ABSENT  →  ABSENT  (mark as missed; override cleared by repository guard)
     *  - ABSENT                    →  PRESENT (mark as attended / un-miss)
     *
     * Receiving the full [currentStatus] (instead of a derived boolean) means the transition is
     * unambiguous regardless of whether the slot is currently REASSIGNED, PRESENT, PENDING, etc.
     */
    fun toggleMissed(slotId: Int, currentStatus: AttendanceStatus?) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            val newStatus = if (currentStatus == AttendanceStatus.ABSENT) {
                AttendanceStatus.PRESENT   // un-miss → restore to attended
            } else {
                AttendanceStatus.ABSENT    // mark missed (clears any reassignment via repo guard)
            }
            repo.markAttendance(slotId, dateStr, newStatus)
        }
    }

    fun toggleCancelled(slotId: Int, isCurrentlyCancelled: Boolean) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            val newStatus = if (isCurrentlyCancelled) AttendanceStatus.PRESENT else AttendanceStatus.CANCELLED
            repo.markAttendance(slotId, dateStr, newStatus)
        }
    }

    fun reassignSlot(slotId: Int, overrideSubjectId: Int) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            repo.markAttendance(slotId, dateStr, AttendanceStatus.REASSIGNED, overrideSubjectId)
        }
    }

    fun revertSlot(slotId: Int) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            repo.markAttendance(slotId, dateStr, AttendanceStatus.PRESENT, null)
        }
    }

    fun shouldShowSundayUpdatePrompt(): Boolean {
        val today = LocalDate.now()
        if (today.dayOfWeek != DayOfWeek.SUNDAY) return false
        val todayStr = today.format(fmt)
        val lastPromptDate = themePrefs.getLastSundayPromptDate()
        return lastPromptDate != todayStr
    }

    fun dismissSundayUpdatePrompt() {
        val today = LocalDate.now()
        themePrefs.setLastSundayPromptDate(today.format(fmt))
    }
}
