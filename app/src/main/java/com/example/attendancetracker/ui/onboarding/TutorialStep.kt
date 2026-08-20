package com.example.attendancetracker.ui.onboarding

enum class TooltipPosition {
    TOP, BOTTOM, CENTER
}

data class TutorialStep(
    val stepIndex: Int,
    val title: String,
    val description: String,
    val targetTab: Int, // 0 = Home, 1 = Schedule, 2 = Stats
    val highlightTag: String,
    val tooltipPosition: TooltipPosition = TooltipPosition.BOTTOM,
    val demoActionText: String? = null
)

object TutorialSteps {
    val steps = listOf(
        TutorialStep(
            stepIndex = 0,
            title = "1. Day & Week Selector",
            description = "Swipe and tap any day to view your timetable schedule and update attendance records for that specific date.",
            targetTab = 0,
            highlightTag = "day_slider",
            tooltipPosition = TooltipPosition.BOTTOM
        ),
        TutorialStep(
            stepIndex = 1,
            title = "2. Class Attendance Actions",
            description = "Classes are auto-marked Attended at 5:00 PM. Tap 'Missed Class' if you were absent, or 'Taken by Another' if another subject substituted the period.",
            targetTab = 0,
            highlightTag = "class_status",
            tooltipPosition = TooltipPosition.TOP,
            demoActionText = "Taken by Another tracks attendance under the replacement subject, keeping records 100% accurate."
        ),
        TutorialStep(
            stepIndex = 2,
            title = "3. Import & Export Timetable",
            description = "Easily import your timetable JSON from AI prompts or photos (using ChatGPT/Claude), or export a backup of your full schedule.",
            targetTab = 1,
            highlightTag = "import_export_section",
            tooltipPosition = TooltipPosition.BOTTOM
        ),
        TutorialStep(
            stepIndex = 3,
            title = "4. Add Subjects & Classes",
            description = "Add subjects with custom colors and targets, then add weekly class timings to build your timetable.",
            targetTab = 1,
            highlightTag = "manual_setup_section",
            tooltipPosition = TooltipPosition.TOP
        ),
        TutorialStep(
            stepIndex = 4,
            title = "5. Statistics & What-If Simulator",
            description = "Track subject percentages, safe-to-skip margins, and test future attendance scenarios with the built-in Simulator & Calendar!",
            targetTab = 2,
            highlightTag = "stats_section",
            tooltipPosition = TooltipPosition.BOTTOM
        )
    )
}
