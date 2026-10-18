package com.bettertick.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bettertick.MainActivity
import com.bettertick.R

/**
 * Result of an [AppUpdater.install] session. When the system wants the user
 * to confirm, its confirm screen opens right away if the app is on screen,
 * else a notification carries it. Also posts "업데이트 완료" once the new
 * version is in place (MY_PACKAGE_REPLACED), since a silent update closes
 * the app.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            notifyDone(context)
            return
        }
        val version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                } ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val shown = AppUpdater.isForeground && runCatching { context.startActivity(confirm) }.isSuccess
                if (!shown) {
                    notify(
                        context,
                        "BetterTick 업데이트 준비 완료",
                        "v$version — 탭하여 설치",
                        PendingIntent.getActivity(
                            context, 0, confirm,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit // 사용자가 취소
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                Log.w("UpdateInstall", "install failed: $msg")
                notify(context, "BetterTick 업데이트 설치 실패", msg, openApp(context))
            }
        }
    }

    private fun notifyDone(context: Context) {
        val name = runCatching { AppUpdater.currentVersionName(context) }.getOrDefault("")
        notify(context, "BetterTick 업데이트 완료", "v$name — 탭하여 열기", openApp(context))
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notify(context: Context, title: String, text: String, pi: PendingIntent) {
        runCatching {
            AppUpdateWorker.createNotificationChannels(context)
            val n = NotificationCompat.Builder(context, AppUpdateWorker.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, n)
        }
    }

    companion object {
        const val EXTRA_VERSION = "com.bettertick.update.VERSION"
        private const val NOTIF_ID = 9001
    }
}
