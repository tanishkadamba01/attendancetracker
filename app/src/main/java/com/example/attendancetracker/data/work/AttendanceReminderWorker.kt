package com.example.attendancetracker.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.attendancetracker.AttendanceApplication
import com.example.attendancetracker.MainActivity
import com.example.attendancetracker.data.model.AttendanceStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "attendance_reminders_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? AttendanceApplication ?: return Result.success()
        val prefs = app.themePreferences

        if (!prefs.reminderEnabled.first()) {
            return Result.success()
        }

        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1=Monday ... 7=Sunday
        if (dayOfWeek > 6) {
            // Sunday - usually no scheduled classes
            return Result.success()
        }

        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val slots = app.database.timetableDao().getSlotsForDay(dayOfWeek).first()
        if (slots.isEmpty()) {
            return Result.success()
        }

        val records = app.database.attendanceDao().getRecordsForDate(todayIso).first()
        val recordMap = records.associateBy { it.slotId }

        // Find slots that are unconfirmed (no record, or PENDING)
        val unconfirmedSlots = slots.filter { slot ->
            val rec = recordMap[slot.id]
            rec == null || rec.status == AttendanceStatus.PENDING
        }

        if (unconfirmedSlots.isNotEmpty()) {
            val subjects = app.database.subjectDao().getAllSubjects().first().associateBy { it.id }
            val subjectNames = unconfirmedSlots.mapNotNull { subjects[it.subjectId]?.name }.distinct()
            val subjectText = if (subjectNames.isNotEmpty()) {
                " (" + subjectNames.take(3).joinToString(", ") + if (subjectNames.size > 3) "..." else "" + ")"
            } else ""

            sendNotification(
                title = "Update Today's Attendance",
                message = "You have ${unconfirmedSlots.size} class${if (unconfirmedSlots.size > 1) "es" else ""} today$subjectText. Don't forget to mark your attendance!"
            )
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Attendance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record class attendance"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
