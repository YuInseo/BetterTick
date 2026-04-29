package com.bettertick.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Receives the PackageInstaller session result. The two cases that matter:
 *  - STATUS_PENDING_USER_ACTION: system needs the user to confirm the
 *    install — start the wrapped intent so the system dialog appears.
 *  - STATUS_SUCCESS: install committed. Try to relaunch the app so the new
 *    binary is the one running. Background launch may be denied on Android
 *    10+, in which case the user just opens the app manually.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirm != null) context.startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> relaunchApp(context)
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "install failed status=$status msg=$msg")
            }
        }
    }

    private fun relaunchApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { context.startActivity(launch) }
                .onFailure { Log.w(TAG, "relaunch failed", it) }
        }, 400)
    }

    companion object {
        const val ACTION = "com.bettertick.update.INSTALL_RESULT"
        private const val TAG = "InstallResultReceiver"
    }
}
