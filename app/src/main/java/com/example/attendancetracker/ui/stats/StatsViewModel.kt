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

data class DayAttendanceSummary(
    val date: java.time.LocalDate,
    val totalClasses: Int,
    val attendedClasses: Int,
    val missedClasses: Int,
    val cancelledClasses: Int,
    val isPresentOnly: Boolean,
    val hasMissed: Boolean,
    val records: List<ClassRecordSummary>
)

data class ClassRecordSummary(
    val slotId: Int,
    val subjectName: String,
    val subjectColor: String,
    val slotTime: String,
    val status: AttendanceStatus,
    val isReassigned: Boolean = false,
    val overrideSubjectName: String? = null
)

data class PredictionResult(
    val extraAttended: Int,
    val extraMissed: Int,
    val projectedTotal: Int,
    val projectedAttended: Int,
    val projectedPercentage: Float,
    val isTargetMet: Boolean,
    val deltaPct: Float
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

    /** Query all calendar attendance summaries grouped by ISO date string */
    val calendarSummaries: StateFlow<Map<String, DayAttendanceSummary>> = combine(
        repo.allSlots,
        repo.allSubjects,
        repo.getAllRecords()
    ) { slots, subjects, records ->
        val subjectMap = subjects.associateBy { it.id }
        val slotMap = slots.associateBy { it.id }

        val recordsByDate = records.groupBy { it.date }
        val summaryMap = mutableMapOf<String, DayAttendanceSummary>()

        recordsByDate.forEach { (dateStr, dateRecords) ->
            val date = try { java.time.LocalDate.parse(dateStr) } catch (e: Exception) { return@forEach }
            var total = 0
            var attended = 0
            var missed = 0
            var cancelled = 0

            val classSummaries = mutableListOf<ClassRecordSummary>()

            dateRecords.forEach { rec ->
                val slot = slotMap[rec.slotId]
                val origSubject = slot?.let { subjectMap[it.subjectId] }
                val overrideSubject = rec.overrideSubjectId?.let { subjectMap[it] }

                val effectiveName = when {
                    rec.status == AttendanceStatus.REASSIGNED && overrideSubject != null -> overrideSubject.name
                    rec.status == AttendanceStatus.ABSENT && overrideSubject != null -> overrideSubject.name
                    else -> origSubject?.name ?: "Class"
                }

                val effectiveColor = when {
                    rec.status == AttendanceStatus.REASSIGNED && overrideSubject != null -> overrideSubject.colorHex
                    rec.status == AttendanceStatus.ABSENT && overrideSubject != null -> overrideSubject.colorHex
                    else -> origSubject?.colorHex ?: "#6650A4"
                }

                val timeStr = slot?.let { "${it.startTime} - ${it.endTime}" } ?: "Class"

                when (rec.status) {
                    AttendanceStatus.PRESENT -> {
                        total++
                        attended++
                    }
                    AttendanceStatus.REASSIGNED -> {
                        total++
                        attended++
                    }
                    AttendanceStatus.ABSENT -> {
                        total++
                        missed++
                    }
                    AttendanceStatus.CANCELLED -> {
                        cancelled++
                    }
                    AttendanceStatus.PENDING -> { /* no impact */ }
                }

                classSummaries.add(
                    ClassRecordSummary(
                        slotId               = rec.slotId,
                        subjectName          = effectiveName,
                        subjectColor         = effectiveColor,
                        slotTime             = timeStr,
                        status               = rec.status,
                        isReassigned         = rec.status == AttendanceStatus.REASSIGNED || rec.overrideSubjectId != null,
                        overrideSubjectName  = overrideSubject?.name
                    )
                )
            }

            summaryMap[dateStr] = DayAttendanceSummary(
                date             = date,
                totalClasses     = total,
                attendedClasses  = attended,
                missedClasses    = missed,
                cancelledClasses = cancelled,
                isPresentOnly    = missed == 0 && attended > 0,
                hasMissed        = missed > 0,
                records          = classSummaries
            )
        }
        summaryMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Calculate attendance projection for what-if scenarios */
    fun calculateProjection(
        currentTotal: Int,
        currentAttended: Int,
        targetPercentage: Float,
        extraAttended: Int,
        extraMissed: Int
    ): PredictionResult {
        val newTotal = currentTotal + extraAttended + extraMissed
        val newAttended = currentAttended + extraAttended
        val newPct = if (newTotal > 0) (newAttended.toFloat() / newTotal) * 100f else 0f
        val currentPct = if (currentTotal > 0) (currentAttended.toFloat() / currentTotal) * 100f else 0f

        return PredictionResult(
            extraAttended       = extraAttended,
            extraMissed         = extraMissed,
            projectedTotal      = newTotal,
            projectedAttended   = newAttended,
            projectedPercentage = newPct,
            isTargetMet         = newPct >= targetPercentage,
            deltaPct            = newPct - currentPct
        )
    }

    /** Query attendance history log for a specific subject */
    fun getSubjectHistoryLog(subjectId: Int): Flow<List<SubjectHistoryEntry>> {
        return combine(repo.allSlots, repo.getAllRecords()) { slots, records ->
            val mySlots = slots.filter { it.subjectId == subjectId }
            val mySlotIds = mySlots.map { it.id }.toSet()

            val entries = mutableListOf<SubjectHistoryEntry>()

            records.forEach { record ->
                if (record.slotId in mySlotIds) {
                    if (record.status == AttendanceStatus.ABSENT && record.overrideSubjectId != null) return@forEach

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
                } else if (record.overrideSubjectId == subjectId) {
                    val matchingSlot = slots.find { it.id == record.slotId }
                    val slotTime = if (matchingSlot != null) "${matchingSlot.startTime}-${matchingSlot.endTime}" else "Class"
                    when (record.status) {
                        AttendanceStatus.REASSIGNED -> entries.add(
                            SubjectHistoryEntry(
                                date         = record.date,
                                status       = AttendanceStatus.PRESENT,
                                slotTime     = slotTime,
                                isReassigned = true
                            )
                        )
                        AttendanceStatus.ABSENT -> entries.add(
                            SubjectHistoryEntry(
                                date         = record.date,
                                status       = AttendanceStatus.ABSENT,
                                slotTime     = slotTime,
                                isReassigned = true
                            )
                        )
                        else -> { }
                    }
                }
            }

            entries.sortedByDescending { it.date }
        }
    }
}
