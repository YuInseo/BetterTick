package com.bettertick.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.bettertick.LockScreenBar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        // Alarms are wiped on reboot. Application.onCreate re-syncs task
        // reminders from the task stream; the daily summary is re-armed here.
        com.bettertick.reminder.ReminderScheduler.scheduleDailySummary(context)

        // 잠금화면 빠른 추가 was removed from the app; nothing else to start.
    }
}
