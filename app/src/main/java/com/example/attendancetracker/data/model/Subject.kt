package com.example.attendancetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#6650A4",
    val code: String = "",
    val targetPercentage: Float = 85f
)
