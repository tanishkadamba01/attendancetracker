package com.example.attendancetracker.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_attendance_reminder"

    fun scheduleDaily(context: Context, time24h: String) {
        val parsedTime = try {
            val parts = time24h.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            LocalTime.of(20, 0)
        }

        val now = LocalDateTime.now()
        var targetDateTime = now.with(parsedTime)
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val initialDelayMinutes = Duration.between(now, targetDateTime).toMinutes().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<AttendanceReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
