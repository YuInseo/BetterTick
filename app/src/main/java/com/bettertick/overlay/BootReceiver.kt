package com.bettertick.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.bettertick.LockScreenBar
import com.bettertick.location.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        // Alarms are wiped on reboot. Application.onCreate re-syncs task
        // reminders from the task stream; the daily summary is re-armed here.
        com.bettertick.reminder.ReminderScheduler.scheduleDailySummary(context)

        val prefs = context.getSharedPreferences("bettertick_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("lock_screen_bar", true)) return

        LockScreenBar.show(context)
        if (Settings.canDrawOverlays(context)) {
            FloatingOverlayService.start(context)
        }
        if (prefs.getBoolean("location_tracking", false)) {
            LocationTrackingService.start(context)
        }
    }
}
