package com.example.attendancetracker.data.db

import androidx.room.*
import com.example.attendancetracker.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Query("SELECT * FROM subjects WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSubjectByName(name: String): Subject?

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}
