package com.example.attendancetracker.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot

class AttendanceConverters {
    @TypeConverter
    fun fromStatus(status: AttendanceStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}

@Database(
    entities = [Subject::class, TimetableSlot::class, AttendanceRecord::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(AttendanceConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
