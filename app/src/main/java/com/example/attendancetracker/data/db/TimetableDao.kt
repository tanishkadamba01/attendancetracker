package com.example.attendancetracker.data.db

import androidx.room.*
import com.example.attendancetracker.data.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getSlotsForDay(day: Int): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)

    @Query("SELECT * FROM timetable_slots WHERE id = :id")
    suspend fun getSlotById(id: Int): TimetableSlot?

    @Query("DELETE FROM timetable_slots")
    suspend fun deleteAllSlots()
}
