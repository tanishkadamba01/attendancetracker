package com.example.attendancetracker.data

import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import com.example.attendancetracker.data.timetable.TimetableSerializer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TimetableSerializerTest {

    @Test
    fun testBuildTimetableJsonAndParseBack() {
        val subjects = listOf(
            Subject(id = 1, name = "Data Structures", code = "CS201", colorHex = "#6366F1", targetPercentage = 80f),
            Subject(id = 2, name = "Algorithms", code = "CS202", colorHex = "#10B981", targetPercentage = 85f)
        )
        val slots = listOf(
            TimetableSlot(id = 1, subjectId = 1, dayOfWeek = 1, startTime = "09:00", endTime = "10:00", room = "301"),
            TimetableSlot(id = 2, subjectId = 2, dayOfWeek = 2, startTime = "11:00", endTime = "12:00", room = "302")
        )
        val startDate = LocalDate.of(2026, 8, 1)

        val jsonString = TimetableSerializer.buildTimetableJson(subjects, slots, startDate)
        assertTrue(jsonString.contains("Data Structures"))
        assertTrue(jsonString.contains("schemaVersion"))
        assertTrue(jsonString.contains("2026-08-01"))

        val result = TimetableSerializer.parseTimetableJson(jsonString)
        assertTrue(result is TimetableSerializer.ParseResult.Success)

        val parsed = (result as TimetableSerializer.ParseResult.Success).data
        assertEquals(2, parsed.subjects.size)
        assertEquals(2, parsed.slots.size)
        assertEquals(LocalDate.of(2026, 8, 1), parsed.classStartDate)
        assertEquals("Data Structures", parsed.slots[0].subjectName)
        assertEquals("09:00", parsed.slots[0].startTime)
        assertEquals("10:00", parsed.slots[0].endTime)
    }

    @Test
    fun testAiTemplateGenerationAndParsing() {
        val templateJson = TimetableSerializer.buildAiTemplateJson()
        assertTrue(templateJson.contains("aiPromptInstructions"))
        assertTrue(templateJson.contains("Data Structures & Algorithms"))

        val result = TimetableSerializer.parseTimetableJson(templateJson)
        assertTrue(result is TimetableSerializer.ParseResult.Success)
        val parsed = (result as TimetableSerializer.ParseResult.Success).data
        assertTrue(parsed.subjects.isNotEmpty())
        assertTrue(parsed.slots.isNotEmpty())
    }

    @Test
    fun testAiGeneratedFlexibleFormats() {
        // AI variant with AM/PM 12-hour format, string days, combined time strings, and synonym keys
        val aiJson = """
        {
            "class_start_date": "2026-08-15",
            "courses": [
                { "name": "Machine Learning", "code": "CS401", "target": 75.0 }
            ],
            "schedule": [
                {
                    "course": "Machine Learning",
                    "day": "Monday",
                    "time": "9:00 AM - 10:30 AM",
                    "classroom": "Hall A"
                },
                {
                    "course": "Cloud Computing",
                    "weekday": "wed",
                    "startTime": "2:00 PM",
                    "endTime": "3:30 PM",
                    "location": "Lab 3"
                }
            ]
        }
        """.trimIndent()

        val result = TimetableSerializer.parseTimetableJson(aiJson)
        assertTrue(result is TimetableSerializer.ParseResult.Success)

        val parsed = (result as TimetableSerializer.ParseResult.Success).data
        assertEquals(2, parsed.subjects.size)
        assertEquals(2, parsed.slots.size)
        assertEquals(LocalDate.of(2026, 8, 15), parsed.classStartDate)

        val slot1 = parsed.slots[0]
        assertEquals("Machine Learning", slot1.subjectName)
        assertEquals(1, slot1.dayOfWeek)
        assertEquals("09:00", slot1.startTime)
        assertEquals("10:30", slot1.endTime)
        assertEquals("Hall A", slot1.room)

        val slot2 = parsed.slots[1]
        assertEquals("Cloud Computing", slot2.subjectName)
        assertEquals(3, slot2.dayOfWeek)
        assertEquals("14:00", slot2.startTime)
        assertEquals("15:30", slot2.endTime)
        assertEquals("Lab 3", slot2.room)
    }

    @Test
    fun testLegacyDayKeyedFormatParsing() {
        val legacyJson = """
        {
            "timetable": {
                "Monday": [
                    { "subject": "Math", "time": "09:00-10:00", "room": "101" }
                ],
                "Friday": [
                    { "subject": "Physics", "time": "11:00-12:00", "room": "201" }
                ]
            }
        }
        """.trimIndent()

        val result = TimetableSerializer.parseTimetableJson(legacyJson)
        assertTrue(result is TimetableSerializer.ParseResult.Success)

        val parsed = (result as TimetableSerializer.ParseResult.Success).data
        assertEquals(2, parsed.slots.size)
        assertEquals(1, parsed.slots[0].dayOfWeek)
        assertEquals("Math", parsed.slots[0].subjectName)
        assertEquals(5, parsed.slots[1].dayOfWeek)
        assertEquals("Physics", parsed.slots[1].subjectName)
    }

    @Test
    fun testParseInvalidJsonReturnsError() {
        val badJson = "this is not json"
        val result = TimetableSerializer.parseTimetableJson(badJson)
        assertTrue(result is TimetableSerializer.ParseResult.Error)
    }
}
