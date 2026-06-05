package com.bettertick.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class QuickOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, QuickOverlayService::class.java).apply {
            action = intent.action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_QUICK_ADD = "com.bettertick.overlay.QUICK_ADD"
    }
}
