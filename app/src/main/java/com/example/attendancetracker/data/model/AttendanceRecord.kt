package com.example.attendancetracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus { PRESENT, ABSENT, REASSIGNED, PENDING, CANCELLED }

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = TimetableSlot::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("slotId"),
        Index(value = ["slotId", "date"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val slotId: Int,
    val date: String,               // ISO date e.g. "2026-08-02"
    val status: AttendanceStatus,
    val overrideSubjectId: Int? = null
)
