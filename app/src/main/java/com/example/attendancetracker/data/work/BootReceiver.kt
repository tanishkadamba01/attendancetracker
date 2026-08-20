package com.example.attendancetracker.data.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendancetracker.AttendanceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? AttendanceApplication ?: return
            val prefs = app.themePreferences

            CoroutineScope(Dispatchers.IO).launch {
                if (prefs.reminderEnabled.first()) {
                    val time = prefs.reminderTime.first()
                    ReminderScheduler.scheduleDaily(context, time)
                }
            }
        }
    }
}
