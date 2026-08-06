package com.example.attendancetracker.data

import com.example.attendancetracker.data.db.AttendanceDao
import com.example.attendancetracker.data.db.SubjectDao
import com.example.attendancetracker.data.db.TimetableDao
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AttendanceRepository(
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableDao,
    private val attendanceDao: AttendanceDao
) {
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Subjects ──────────────────────────────────────────────
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun insertSubject(subject: Subject) = subjectDao.insertSubject(subject)
    suspend fun insertAllSubjects(subjects: List<Subject>) = subjectDao.insertAllSubjects(subjects)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Int) = subjectDao.getSubjectById(id)
    suspend fun getSubjectByName(name: String) = subjectDao.getSubjectByName(name)

    // ── Timetable ─────────────────────────────────────────────
    val allSlots: Flow<List<TimetableSlot>> = timetableDao.getAllSlots()

    fun getSlotsForDay(day: Int): Flow<List<TimetableSlot>> = timetableDao.getSlotsForDay(day)
    suspend fun insertSlot(slot: TimetableSlot) = timetableDao.insertSlot(slot)
    suspend fun insertAllSlots(slots: List<TimetableSlot>) = timetableDao.insertAllSlots(slots)
    suspend fun deleteSlot(slot: TimetableSlot) = timetableDao.deleteSlot(slot)
    suspend fun deleteAllSlots() = timetableDao.deleteAllSlots()
    suspend fun deleteAllSubjects() = subjectDao.deleteAllSubjects()
    suspend fun deleteAllRecords() = attendanceDao.deleteAllRecords()

    /**
     * Check if the given time range overlaps with any existing slot on the same day.
     * @param day 1=Monday…6=Saturday
     * @param startTime "HH:mm"
     * @param endTime "HH:mm"
     * @param excludeSlotId The slot ID to exclude from the check (for edit scenarios). Use -1 if not applicable.
     * @return The overlapping TimetableSlot, or null if no conflict.
     */
    suspend fun findOverlappingSlot(
        day: Int,
        startTime: String,
        endTime: String,
        excludeSlotId: Int = -1
    ): TimetableSlot? {
        val existingSlots = timetableDao.getSlotsForDayExcluding(day, excludeSlotId)
        val newStart = LocalTime.parse(startTime)
        val newEnd   = LocalTime.parse(endTime)
        return existingSlots.firstOrNull { slot ->
            val existStart = LocalTime.parse(slot.startTime)
            val existEnd   = LocalTime.parse(slot.endTime)
            // Overlap: new starts before existing ends AND new ends after existing starts
            newStart < existEnd && newEnd > existStart
        }
    }

    suspend fun deleteAllData() {
        attendanceDao.deleteAllRecords()
        timetableDao.deleteAllSlots()
        subjectDao.deleteAllSubjects()
    }

    // ── Attendance Records ────────────────────────────────────
    fun getRecordsForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsForDate(date)

    fun getAllRecords(): Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()

    suspend fun getRecord(slotId: Int, date: String) = attendanceDao.getRecord(slotId, date)

    suspend fun insertAllRecords(records: List<AttendanceRecord>) =
        attendanceDao.insertAllRecords(records)

    /** Mark slot explicitly as PRESENT, ABSENT (Missed), or REASSIGNED */
    suspend fun markAttendance(
        slotId: Int,
        date: String,
        status: AttendanceStatus,
        overrideSubjectId: Int? = null
    ) {
        val existing = attendanceDao.getRecord(slotId, date)
        val recordToSave = existing?.copy(status = status, overrideSubjectId = overrideSubjectId)
            ?: AttendanceRecord(slotId = slotId, date = date, status = status, overrideSubjectId = overrideSubjectId)
        attendanceDao.insertRecord(recordToSave)
    }

    /**
     * Feature 1: Automatically confirm pending classes at 5:00 PM (17:00).
     */
    suspend fun checkAndAutoConfirm5PM(date: LocalDate, startDate: LocalDate? = null) {
        if (startDate != null && date.isBefore(startDate)) {
            return
        }
        val today = LocalDate.now()
        val now   = LocalTime.now()
        val isPastDate   = date.isBefore(today)
        val isTodayAfter5 = (date == today && now.hour >= 17)

        if (isPastDate || isTodayAfter5) {
            val dayOfWeek = date.dayOfWeek.value
            if (dayOfWeek <= 6) {
                val slotsForDay = timetableDao.getSlotsForDay(dayOfWeek).first()
                val dateStr     = date.format(isoFmt)
                slotsForDay.forEach { slot ->
                    val existing = attendanceDao.getRecord(slot.id, dateStr)
                    if (existing == null) {
                        attendanceDao.insertRecord(
                            AttendanceRecord(
                                slotId            = slot.id,
                                date              = dateStr,
                                status            = AttendanceStatus.PRESENT,
                                overrideSubjectId = null
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Compute statistics for a specific subject: (total, attended, missed)
     */
    fun getSubjectDetailedStats(subjectId: Int): Flow<Triple<Int, Int, Int>> {
        return combine(allSlots, getAllRecords()) { slots, records ->
            var total = 0
            var attended = 0
            var missed = 0

            // 1. Slots belonging to this subject
            val mySlots = slots.filter { it.subjectId == subjectId }
            mySlots.forEach { slot ->
                val matchingRecords = records.filter { it.slotId == slot.id }
                matchingRecords.forEach { record ->
                    if (record.status == AttendanceStatus.PRESENT) {
                        total += 1
                        attended += 1
                    } else if (record.status == AttendanceStatus.ABSENT) {
                        total += 1
                        missed += 1
                    }
                }
            }

            // 2. Slots belonging to OTHER subjects that were reassigned to THIS subject
            records.filter { it.status == AttendanceStatus.REASSIGNED && it.overrideSubjectId == subjectId }.forEach { _ ->
                total += 1
                attended += 1
            }

            Triple(total, attended, missed)
        }
    }

    fun getSubjectStats(subjectId: Int): Flow<Pair<Int, Int>> {
        return getSubjectDetailedStats(subjectId).map { Pair(it.first, it.second) }
    }

    /**
     * Compute overall stats across all subjects
     */
    val overallStats: Flow<Pair<Int, Int>> = combine(allSlots, getAllRecords()) { slots, records ->
        var total = 0
        var attended = 0

        records.forEach { record ->
            if (record.status == AttendanceStatus.PRESENT) {
                total += 1
                attended += 1
            } else if (record.status == AttendanceStatus.ABSENT) {
                total += 1
            } else if (record.status == AttendanceStatus.REASSIGNED) {
                total += 1
                attended += 1
            }
        }
        Pair(total, attended)
    }

    val totalClasses: Flow<Int> = overallStats.map { it.first }
    val attendedClasses: Flow<Int> = overallStats.map { it.second }
}
