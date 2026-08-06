package com.example.attendancetracker.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.attendancetracker.data.model.AttendanceRecord
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.data.model.TimetableSlot
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Handles all serialization and deserialization for the full app backup.
 *
 * Schema version history:
 *  v1 — Initial backup format (subjects, timetableSlots, attendanceRecords, preferences)
 */
object BackupSerializer {

    private const val SCHEMA_VERSION = 1
    private const val APP_VERSION    = "1.0"

    // ── Export ────────────────────────────────────────────────────────────────

    fun buildBackupJson(
        subjects:   List<Subject>,
        slots:      List<TimetableSlot>,
        records:    List<AttendanceRecord>,
        prefs:      SharedPreferences
    ): String {
        val root = JSONObject()

        // --- meta ---
        val meta = JSONObject()
        meta.put("schemaVersion", SCHEMA_VERSION)
        meta.put("appVersion", APP_VERSION)
        meta.put(
            "exportedAt",
            Instant.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
        meta.put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        root.put("meta", meta)

        // --- preferences ---
        val prefsObj = JSONObject()
        prefsObj.put("themeMode",               prefs.getString("theme_mode", "DARK") ?: "DARK")
        prefsObj.put("defaultTargetPercentage",  prefs.getFloat("default_target", 85f))
        prefsObj.put("classStartDate",          prefs.getString("class_start_date", null) ?: JSONObject.NULL)
        root.put("preferences", prefsObj)

        // --- subjects ---
        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            val obj = JSONObject()
            obj.put("id",               s.id)
            obj.put("name",             s.name)
            obj.put("colorHex",         s.colorHex)
            obj.put("code",             s.code)
            obj.put("targetPercentage", s.targetPercentage)
            subjectsArr.put(obj)
        }
        root.put("subjects", subjectsArr)

        // --- timetableSlots ---
        val slotsArr = JSONArray()
        slots.forEach { sl ->
            val obj = JSONObject()
            obj.put("id",        sl.id)
            obj.put("subjectId", sl.subjectId)
            obj.put("dayOfWeek", sl.dayOfWeek)
            obj.put("startTime", sl.startTime)
            obj.put("endTime",   sl.endTime)
            obj.put("room",      sl.room)
            slotsArr.put(obj)
        }
        root.put("timetableSlots", slotsArr)

        // --- attendanceRecords ---
        val recordsArr = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("id",                r.id)
            obj.put("slotId",            r.slotId)
            obj.put("date",              r.date)
            obj.put("status",            r.status.name)
            if (r.overrideSubjectId != null) obj.put("overrideSubjectId", r.overrideSubjectId)
            else obj.put("overrideSubjectId", JSONObject.NULL)
            recordsArr.put(obj)
        }
        root.put("attendanceRecords", recordsArr)

        return root.toString(2) // pretty-printed
    }

    // ── Import ────────────────────────────────────────────────────────────────

    data class BackupData(
        val schemaVersion: Int,
        val themeMode: String,
        val defaultTarget: Float,
        val classStartDate: String?,
        val subjects: List<Subject>,
        val slots: List<TimetableSlot>,
        val records: List<AttendanceRecord>
    )

    sealed class ParseResult {
        data class Success(val data: BackupData) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    fun parseBackupJson(jsonText: String): ParseResult {
        return try {
            val root = JSONObject(jsonText)

            // Validate structure
            if (!root.has("meta"))              return ParseResult.Error("Invalid backup file: missing 'meta' section.")
            if (!root.has("subjects"))          return ParseResult.Error("Invalid backup file: missing 'subjects' section.")
            if (!root.has("timetableSlots"))    return ParseResult.Error("Invalid backup file: missing 'timetableSlots' section.")
            if (!root.has("attendanceRecords")) return ParseResult.Error("Invalid backup file: missing 'attendanceRecords' section.")
            if (!root.has("preferences"))       return ParseResult.Error("Invalid backup file: missing 'preferences' section.")

            val meta = root.getJSONObject("meta")
            val schemaVersion = meta.optInt("schemaVersion", 0)
            if (schemaVersion < 1) return ParseResult.Error("Incompatible backup file: unknown schema version $schemaVersion.")
            if (schemaVersion > SCHEMA_VERSION) return ParseResult.Error("This backup was created with a newer version of the app (schema v$schemaVersion). Please update the app to restore it.")

            val prefsObj     = root.getJSONObject("preferences")
            val themeMode    = prefsObj.optString("themeMode", "DARK")
            val defaultTarget = prefsObj.optDouble("defaultTargetPercentage", 85.0).toFloat()
            val classStartDate = if (prefsObj.isNull("classStartDate")) null else prefsObj.optString("classStartDate", null)

            // Parse subjects
            val subjectsArr = root.getJSONArray("subjects")
            val subjects = (0 until subjectsArr.length()).map { i ->
                val o = subjectsArr.getJSONObject(i)
                Subject(
                    id               = o.getInt("id"),
                    name             = o.getString("name"),
                    colorHex         = o.optString("colorHex", "#6650A4"),
                    code             = o.optString("code", ""),
                    targetPercentage = o.optDouble("targetPercentage", 85.0).toFloat()
                )
            }

            // Parse timetable slots
            val slotsArr = root.getJSONArray("timetableSlots")
            val slots = (0 until slotsArr.length()).map { i ->
                val o = slotsArr.getJSONObject(i)
                TimetableSlot(
                    id        = o.getInt("id"),
                    subjectId = o.getInt("subjectId"),
                    dayOfWeek = o.getInt("dayOfWeek"),
                    startTime = o.getString("startTime"),
                    endTime   = o.getString("endTime"),
                    room      = o.optString("room", "")
                )
            }

            // Parse attendance records
            val recordsArr = root.getJSONArray("attendanceRecords")
            val records = (0 until recordsArr.length()).map { i ->
                val o = recordsArr.getJSONObject(i)
                val statusStr = o.optString("status", "PRESENT")
                val status = try { AttendanceStatus.valueOf(statusStr) } catch (e: Exception) { AttendanceStatus.PRESENT }
                AttendanceRecord(
                    id                = o.getInt("id"),
                    slotId            = o.getInt("slotId"),
                    date              = o.getString("date"),
                    status            = status,
                    overrideSubjectId = if (o.isNull("overrideSubjectId")) null else o.optInt("overrideSubjectId")
                )
            }

            ParseResult.Success(
                BackupData(
                    schemaVersion  = schemaVersion,
                    themeMode      = themeMode,
                    defaultTarget  = defaultTarget,
                    classStartDate = classStartDate,
                    subjects       = subjects,
                    slots          = slots,
                    records        = records
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse backup file: ${e.localizedMessage ?: "Unknown error"}. The file may be corrupted or not a valid backup.")
        }
    }

    /** Generate a descriptive filename for the backup */
    fun generateFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val timestamp = java.time.LocalDateTime.now().format(formatter)
        return "AttendanceTracker_Backup_$timestamp.json"
    }
}
