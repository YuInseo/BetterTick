package com.bettertick.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val prefs = context.getSharedPreferences("bettertick_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("lock_screen_bar", true)) return
        if (!Settings.canDrawOverlays(context)) return

        FloatingOverlayService.start(context)
    }
}
