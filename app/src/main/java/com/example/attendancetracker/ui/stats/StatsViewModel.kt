package com.example.attendancetracker.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.data.AttendanceCalculations
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SubjectStats(
    val subject: Subject,
    val total: Int,
    val attended: Int,
    val missed: Int,
    val percentage: Float,
    val targetPercentage: Float,
    val requiredToTarget: Int, // positive = need to attend; negative = safe to skip
    val safeToSkip: Int
)

data class SubjectHistoryEntry(
    val date: String,
    val status: AttendanceStatus,
    val slotTime: String,
    val isReassigned: Boolean = false
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as AttendanceApplication).repository

    // Subject-wise stats stream (No overall stats)
    val subjectStats: StateFlow<List<SubjectStats>> = repo.allSubjects.flatMapLatest { subjects ->
        if (subjects.isEmpty()) return@flatMapLatest flowOf(emptyList())
        combine(subjects.map { subject ->
            repo.getSubjectDetailedStats(subject.id).map { (total, attended, missed) ->
                val pct = if (total > 0) attended.toFloat() / total * 100f else 0f
                val target = subject.targetPercentage
                val needed = AttendanceCalculations.computeNeeded(total, attended, target)
                // safeToSkip uses its own formula: A/(T+M) >= P => M = floor(A/P - T)
                // Only meaningful when current attendance is at or above target.
                val safeSkip = if (needed <= 0) AttendanceCalculations.computeSafeToSkip(total, attended, target) else 0

                SubjectStats(
                    subject          = subject,
                    total            = total,
                    attended         = attended,
                    missed           = missed,
                    percentage       = pct,
                    targetPercentage = target,
                    requiredToTarget = needed,
                    safeToSkip       = safeSkip
                )
            }
        }) { it.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Query attendance history log for a specific subject */
    fun getSubjectHistoryLog(subjectId: Int): Flow<List<SubjectHistoryEntry>> {
        return combine(repo.allSlots, repo.getAllRecords()) { slots, records ->
            val mySlots = slots.filter { it.subjectId == subjectId }
            val mySlotIds = mySlots.map { it.id }.toSet()

            val entries = mutableListOf<SubjectHistoryEntry>()

            records.forEach { record ->
                if (record.slotId in mySlotIds) {
                    val matchingSlot = mySlots.find { it.id == record.slotId }
                    val slotTime = if (matchingSlot != null) "${matchingSlot.startTime}-${matchingSlot.endTime}" else "Class"
                    entries.add(
                        SubjectHistoryEntry(
                            date         = record.date,
                            status       = record.status,
                            slotTime     = slotTime,
                            isReassigned = record.status == AttendanceStatus.REASSIGNED
                        )
                    )
                } else if (record.status == AttendanceStatus.REASSIGNED && record.overrideSubjectId == subjectId) {
                    val matchingSlot = slots.find { it.id == record.slotId }
                    val slotTime = if (matchingSlot != null) "${matchingSlot.startTime}-${matchingSlot.endTime}" else "Class"
                    entries.add(
                        SubjectHistoryEntry(
                            date         = record.date,
                            status       = AttendanceStatus.PRESENT,
                            slotTime     = slotTime,
                            isReassigned = true
                        )
                    )
                }
            }

            entries.sortedByDescending { it.date }
        }
    }

    // Calculation logic moved to AttendanceCalculations.kt for testability.
}
