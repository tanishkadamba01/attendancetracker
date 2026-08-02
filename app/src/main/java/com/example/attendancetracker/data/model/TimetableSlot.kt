package com.example.attendancetracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val dayOfWeek: Int,      // 1=Monday … 6=Saturday
    val startTime: String,   // "09:00"
    val endTime: String      // "10:00"
)
