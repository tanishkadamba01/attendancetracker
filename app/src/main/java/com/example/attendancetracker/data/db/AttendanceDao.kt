package com.example.attendancetracker.data.db

import androidx.room.*
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY slotId ASC")
    fun getRecordsForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE slotId = :slotId AND date = :date LIMIT 1")
    suspend fun getRecord(slotId: Int, date: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE status = 'PRESENT'")
    fun getAttendedClasses(): Flow<Int>

    @Query("SELECT * FROM attendance_records WHERE overrideSubjectId = :subjectId")
    fun getReassignedRecordsForSubject(subjectId: Int): Flow<List<AttendanceRecord>>

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)
}
