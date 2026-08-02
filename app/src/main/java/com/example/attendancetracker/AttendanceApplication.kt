package com.example.attendancetracker

import android.app.Application
import com.example.attendancetracker.data.AttendanceRepository
import com.example.attendancetracker.data.db.AppDatabase
import com.example.attendancetracker.data.local.ThemePreferences

class AttendanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val themePreferences by lazy { ThemePreferences(this) }
    val repository by lazy {
        AttendanceRepository(
            database.subjectDao(),
            database.timetableDao(),
            database.attendanceDao()
        )
    }
}
