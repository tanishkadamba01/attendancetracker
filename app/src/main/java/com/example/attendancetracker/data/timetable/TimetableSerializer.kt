package com.example.attendancetracker.data.timetable

import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Handles serialization, template generation, and AI-compatible parsing for Timetable / Schedule.
 */
object TimetableSerializer {

    const val SCHEMA_VERSION = 1
    const val APP_NAME = "AttendanceTracker"

    data class ParsedSubject(
        val name: String,
        val code: String = "",
        val colorHex: String = "",
        val targetPercentage: Float = 85f
    )

    data class ParsedSlot(
        val subjectName: String,
        val dayOfWeek: Int, // 1=Monday ... 7=Sunday
        val startTime: String, // "HH:mm" 24h format
        val endTime: String,   // "HH:mm" 24h format
        val room: String = ""
    )

    data class ParsedTimetable(
        val subjects: List<ParsedSubject>,
        val slots: List<ParsedSlot>,
        val classStartDate: LocalDate? = null,
        val schemaVersion: Int = 1
    )

    sealed class ParseResult {
        data class Success(val data: ParsedTimetable) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun getDayName(dayOfWeek: Int): String {
        return if (dayOfWeek in 1..7) dayNames[dayOfWeek - 1] else "Day $dayOfWeek"
    }

    // ── Export Serialization ──────────────────────────────────────────────────

    /**
     * Serializes actual user timetable into clean, AI-understandable JSON format.
     */
    fun buildTimetableJson(
        subjects: List<Subject>,
        slots: List<TimetableSlot>,
        classStartDate: LocalDate? = null
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("app", APP_NAME)
        root.put("exportedAt", LocalDate.now().toString())
        root.put(
            "_instructions",
            "This is an Attendance Tracker timetable schedule JSON. " +
            "dayOfWeek: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday. " +
            "startTime and endTime are in 24-hour 'HH:mm' format."
        )

        if (classStartDate != null) {
            root.put("classStartDate", classStartDate.toString())
        }

        // Subjects array
        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            val obj = JSONObject()
            obj.put("name", s.name)
            if (s.code.isNotBlank()) obj.put("code", s.code)
            obj.put("colorHex", s.colorHex)
            obj.put("targetPercentage", s.targetPercentage.toDouble())
            subjectsArr.put(obj)
        }
        root.put("subjects", subjectsArr)

        // Slots array
        val slotsArr = JSONArray()
        slots.forEach { sl ->
            val sub = subjects.find { it.id == sl.subjectId }
            val obj = JSONObject()
            obj.put("subjectName", sub?.name ?: "Subject")
            obj.put("dayOfWeek", sl.dayOfWeek)
            obj.put("dayName", getDayName(sl.dayOfWeek))
            obj.put("startTime", sl.startTime)
            obj.put("endTime", sl.endTime)
            if (sl.room.isNotBlank()) obj.put("room", sl.room)
            slotsArr.put(obj)
        }
        root.put("slots", slotsArr)

        return root.toString(2)
    }

    /**
     * Builds a predefined template JSON complete with example subjects, slots, and AI prompts.
     * Perfect for users to upload or copy into ChatGPT/Gemini/Claude alongside a photo of their timetable.
     */
    fun buildAiTemplateJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("app", APP_NAME)
        root.put(
            "aiPromptInstructions",
            "Please convert the timetable image into this exact JSON format. " +
            "Ensure all weekly class slots are listed with correct 24-hour startTime and endTime (HH:mm), " +
            "dayOfWeek (1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday), " +
            "and matching subjectName in both the subjects and slots lists."
        )
        root.put("classStartDate", LocalDate.now().toString())

        // Sample Subjects
        val subjectsArr = JSONArray()
        val sampleSubjects = listOf(
            Triple("Data Structures & Algorithms", "CS201", "#6366F1"),
            Triple("Operating Systems", "CS202", "#10B981"),
            Triple("Computer Networks", "CS203", "#F59E0B"),
            Triple("Database Management Systems", "CS204", "#EC4899")
        )
        sampleSubjects.forEach { (name, code, color) ->
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("code", code)
            obj.put("colorHex", color)
            obj.put("targetPercentage", 85.0)
            subjectsArr.put(obj)
        }
        root.put("subjects", subjectsArr)

        // Sample Slots
        val slotsArr = JSONArray()
        val sampleSlots = listOf(
            Triple("Data Structures & Algorithms", 1, Pair("09:00", "10:00")),
            Triple("Operating Systems", 1, Pair("10:15", "11:15")),
            Triple("Computer Networks", 2, Pair("09:00", "10:00")),
            Triple("Database Management Systems", 2, Pair("11:30", "12:30")),
            Triple("Data Structures & Algorithms", 3, Pair("14:00", "16:00")), // Lab
            Triple("Operating Systems", 4, Pair("09:00", "10:00")),
            Triple("Database Management Systems", 5, Pair("10:15", "11:15"))
        )
        sampleSlots.forEach { (subjectName, day, times) ->
            val obj = JSONObject()
            obj.put("subjectName", subjectName)
            obj.put("dayOfWeek", day)
            obj.put("dayName", getDayName(day))
            obj.put("startTime", times.first)
            obj.put("endTime", times.second)
            obj.put("room", "Room 101")
            slotsArr.put(obj)
        }
        root.put("slots", slotsArr)

        return root.toString(2)
    }

    fun generateFileName(isTemplate: Boolean = false): String {
        return if (isTemplate) {
            "timetable_template.json"
        } else {
            "timetable_schedule_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"
        }
    }

    // ── Resilient AI-Tolerant Parser ──────────────────────────────────────────

    fun parseTimetableJson(jsonText: String): ParseResult {
        if (jsonText.isBlank()) {
            return ParseResult.Error("JSON input is empty.")
        }

        return try {
            val root = JSONObject(jsonText.trim())

            // Check for class start date
            val startDateStr = optAnyString(
                root,
                "classStartDate", "class_start_date",
                "startDate", "start_date",
                "semesterStartDate", "semester_start_date",
                "termStartDate", "term_start_date"
            )
            val classStartDate = parseFlexibleDate(startDateStr)

            val parsedSubjects = mutableListOf<ParsedSubject>()
            val parsedSlots = mutableListOf<ParsedSlot>()

            // 1. Check for standard V1 format or synonym arrays ("slots", "schedule", "timetableSlots", "sessions")
            val slotsArray = optAnyArray(root, "slots", "schedule", "timetableSlots", "timetable_slots", "sessions", "classes")
            val subjectsArray = optAnyArray(root, "subjects", "courses", "subjectList", "subject_list")

            // Parse explicit subjects array if present
            if (subjectsArray != null) {
                for (i in 0 until subjectsArray.length()) {
                    val obj = subjectsArray.optJSONObject(i) ?: continue
                    val name = optAnyString(obj, "name", "subjectName", "subject_name", "subject", "title", "courseName", "course_name", "course") ?: ""
                    if (name.isBlank()) continue

                    val code = optAnyString(obj, "code", "subjectCode", "subject_code", "courseCode", "course_code") ?: ""
                    val colorHex = optAnyString(obj, "colorHex", "color_hex", "color", "hexColor", "hex_color") ?: ""
                    val targetPct = optAnyDouble(obj, 85.0, "targetPercentage", "target_percentage", "target", "targetPct", "target_pct", "minAttendance", "min_attendance").toFloat()

                    parsedSubjects.add(
                        ParsedSubject(
                            name = name.trim(),
                            code = code.trim(),
                            colorHex = colorHex.trim(),
                            targetPercentage = targetPct
                        )
                    )
                }
            }

            if (slotsArray != null) {
                // Parse slots array
                for (i in 0 until slotsArray.length()) {
                    val obj = slotsArray.optJSONObject(i) ?: continue
                    val slot = parseSlotObject(obj)
                    if (slot != null) {
                        parsedSlots.add(slot)
                    }
                }
            } else if (root.has("timetable") && root.optJSONObject("timetable") != null) {
                // Legacy / Day-keyed format: "timetable": { "Monday": [...], "Tuesday": [...] }
                val timetableObj = root.getJSONObject("timetable")
                parseDayKeyedObject(timetableObj, parsedSlots)
            } else {
                // Check if the root object itself has day names as keys (e.g. { "Monday": [...], "Tuesday": [...] })
                val hasDayKeys = dayNames.any { root.has(it) || root.has(it.lowercase()) || root.has(it.take(3)) }
                if (hasDayKeys) {
                    parseDayKeyedObject(root, parsedSlots)
                }
            }

            // Ensure any subjects mentioned in slots are in the subjects list
            val knownSubjectNames = parsedSubjects.map { it.name.lowercase() }.toMutableSet()
            parsedSlots.forEach { slot ->
                if (slot.subjectName.isNotBlank() && slot.subjectName.lowercase() !in knownSubjectNames) {
                    parsedSubjects.add(ParsedSubject(name = slot.subjectName))
                    knownSubjectNames.add(slot.subjectName.lowercase())
                }
            }

            if (parsedSlots.isEmpty() && parsedSubjects.isEmpty()) {
                return ParseResult.Error("No valid subjects or schedule slots found in the JSON.")
            }

            ParseResult.Success(
                ParsedTimetable(
                    subjects = parsedSubjects,
                    slots = parsedSlots,
                    classStartDate = classStartDate,
                    schemaVersion = root.optInt("schemaVersion", 1)
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse JSON: ${e.localizedMessage ?: "Invalid structure"}")
        }
    }

    private fun parseSlotObject(obj: JSONObject): ParsedSlot? {
        val subjectName = optAnyString(obj, "subjectName", "subject", "name", "course", "courseName", "title")?.trim() ?: ""
        if (subjectName.isBlank() || subjectName.equals("Break", ignoreCase = true) || subjectName.equals("Lunch", ignoreCase = true)) {
            return null
        }

        // Determine day of week
        val dayVal = obj.opt("dayOfWeek") ?: obj.opt("day") ?: obj.opt("weekday") ?: obj.opt("dayName")
        val dayOfWeek = parseFlexibleDay(dayVal) ?: return null

        // Determine start and end time
        var startTime = optAnyString(obj, "startTime", "start_time", "start", "from")
        var endTime = optAnyString(obj, "endTime", "end_time", "end", "to")

        // If separate times not found, check unified "time" key e.g. "09:00 - 10:00"
        if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
            val combinedTime = optAnyString(obj, "time", "slotTime", "timing", "hours")
            if (!combinedTime.isNullOrBlank()) {
                val split = splitTimeRange(combinedTime)
                if (split != null) {
                    startTime = split.first
                    endTime = split.second
                }
            }
        }

        val normStart = normalizeTime(startTime) ?: return null
        val normEnd = normalizeTime(endTime) ?: return null
        val room = optAnyString(obj, "room", "classroom", "location", "venue", "room_number") ?: ""

        return ParsedSlot(
            subjectName = subjectName,
            dayOfWeek = dayOfWeek,
            startTime = normStart,
            endTime = normEnd,
            room = room.trim()
        )
    }

    private fun parseDayKeyedObject(parentObj: JSONObject, outSlots: MutableList<ParsedSlot>) {
        dayNames.forEachIndexed { index, dayName ->
            val dayInt = index + 1
            val dayArray = optAnyArray(
                parentObj,
                dayName,
                dayName.lowercase(),
                dayName.uppercase(),
                dayName.take(3),
                dayName.take(3).lowercase()
            ) ?: return@forEachIndexed

            for (i in 0 until dayArray.length()) {
                val item = dayArray.optJSONObject(i) ?: continue
                val subjectName = optAnyString(item, "subject", "subjectName", "name", "course", "title")?.trim() ?: ""
                if (subjectName.isBlank() || subjectName.equals("Break", ignoreCase = true) || subjectName.equals("Lunch", ignoreCase = true)) {
                    continue
                }

                var startTime = optAnyString(item, "startTime", "start")
                var endTime = optAnyString(item, "endTime", "end")

                if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
                    val combinedTime = optAnyString(item, "time", "timing", "hours")
                    if (!combinedTime.isNullOrBlank()) {
                        val split = splitTimeRange(combinedTime)
                        if (split != null) {
                            startTime = split.first
                            endTime = split.second
                        }
                    }
                }

                val normStart = normalizeTime(startTime) ?: "09:00"
                val normEnd = normalizeTime(endTime) ?: "10:00"
                val room = optAnyString(item, "room", "classroom", "location") ?: ""

                outSlots.add(
                    ParsedSlot(
                        subjectName = subjectName,
                        dayOfWeek = dayInt,
                        startTime = normStart,
                        endTime = normEnd,
                        room = room.trim()
                    )
                )
            }
        }
    }

    // ── Helper parsing functions ──────────────────────────────────────────────

    fun parseFlexibleDay(value: Any?): Int? {
        if (value == null) return null
        if (value is Number) {
            val num = value.toInt()
            return when {
                num in 1..7 -> num
                num == 0 -> 7 // 0-indexed Sunday -> 7
                else -> null
            }
        }
        val str = value.toString().trim()
        val num = str.toIntOrNull()
        if (num != null) {
            return when {
                num in 1..7 -> num
                num == 0 -> 7
                else -> null
            }
        }
        val lower = str.lowercase(Locale.ENGLISH)
        return when {
            lower.startsWith("mon") -> 1
            lower.startsWith("tue") -> 2
            lower.startsWith("wed") -> 3
            lower.startsWith("thu") -> 4
            lower.startsWith("fri") -> 5
            lower.startsWith("sat") -> 6
            lower.startsWith("sun") -> 7
            else -> null
        }
    }

    /**
     * Splits combined time strings like "09:00 - 10:00", "9:00 AM to 10:30 AM", "09:00-10:00"
     */
    fun splitTimeRange(timeStr: String): Pair<String, String>? {
        val delimiters = listOf(" - ", "-", " to ", " – ", " — ", " .. ")
        for (delim in delimiters) {
            if (timeStr.contains(delim)) {
                val parts = timeStr.split(delim, limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }
        return null
    }

    /**
     * Converts times like "9:00", "09:00", "9:00 AM", "1:30 PM", "13:30:00" into 24-hour "HH:mm".
     */
    fun normalizeTime(timeStr: String?): String? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim().uppercase(Locale.ENGLISH)

        // Try standard LocalTime parses
        val timePatterns = listOf(
            "H:mm",
            "HH:mm",
            "H:mm:ss",
            "HH:mm:ss",
            "h:mm a",
            "h:mma",
            "hh:mm a",
            "hh:mma",
            "h a",
            "ha"
        )

        for (pattern in timePatterns) {
            try {
                val formatter = DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .toFormatter(Locale.ENGLISH)
                val lt = LocalTime.parse(clean, formatter)
                return "%02d:%02d".format(lt.hour, lt.minute)
            } catch (_: Exception) {
            }
        }

        // Fallback for simple "9" or "09" meaning 9:00
        val singleHour = clean.toIntOrNull()
        if (singleHour != null && singleHour in 0..23) {
            return "%02d:00".format(singleHour)
        }

        return null
    }

    private fun parseFlexibleDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()
        val datePatterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy", "d MMM yyyy", "MMMM d, yyyy")
        for (pattern in datePatterns) {
            try {
                val fmt = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                return LocalDate.parse(clean, fmt)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun optAnyString(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (obj.has(key)) {
                val v = obj.optString(key, "")
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun optAnyArray(obj: JSONObject, vararg keys: String): JSONArray? {
        for (key in keys) {
            if (obj.has(key)) {
                val arr = obj.optJSONArray(key)
                if (arr != null) return arr
            }
        }
        return null
    }

    private fun optAnyDouble(obj: JSONObject, default: Double, vararg keys: String): Double {
        for (key in keys) {
            if (obj.has(key)) {
                val d = obj.optDouble(key, Double.NaN)
                if (!d.isNaN()) return d
            }
        }
        return default
    }
}
