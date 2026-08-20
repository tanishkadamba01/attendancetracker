package com.example.attendancetracker.ui.onboarding

import com.example.attendancetracker.data.model.AttendanceStatus

data class DemoClass(
    val subjectName: String,
    val subjectColor: String,
    val time: String,
    val room: String,
    val status: AttendanceStatus,
    val overrideSubjectName: String? = null
)

object TutorialDemoData {
    val demoClasses = listOf(
        DemoClass(
            subjectName = "Mathematics",
            subjectColor = "#6366F1",
            time = "09:00 - 10:00",
            room = "Room 201",
            status = AttendanceStatus.PRESENT
        ),
        DemoClass(
            subjectName = "Physics",
            subjectColor = "#10B981",
            time = "10:15 - 11:15",
            room = "Lab 3",
            status = AttendanceStatus.REASSIGNED,
            overrideSubjectName = "Chemistry"
        ),
        DemoClass(
            subjectName = "Computer Networks",
            subjectColor = "#EC4899",
            time = "11:30 - 12:30",
            room = "Room 105",
            status = AttendanceStatus.ABSENT
        )
    )
}
