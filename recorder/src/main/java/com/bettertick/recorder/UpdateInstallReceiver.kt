package com.bettertick.recorder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * [Updater.install] 세션 결과를 받는다. 시스템이 사용자 확인을 요구하면
 * 앱이 떠 있을 땐 확인창을 바로 띄우고, 아니면 알림으로 넘긴다.
 * 조용히 교체되면(ACTION_MY_PACKAGE_REPLACED) "업데이트 완료" 알림을 띄운다.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val name = runCatching { Updater.currentVersionName(context) }.getOrDefault("")
            notify(context, "녹음 앱 업데이트 완료", "v$name — 탭하여 열기", openApp(context))
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
                val shown = Updater.isForeground && runCatching { context.startActivity(confirm) }.isSuccess
                if (!shown) {
                    notify(
                        context,
                        "녹음 앱 업데이트 준비 완료",
                        "v$version — 탭하여 설치",
                        PendingIntent.getActivity(
                            context, 0, confirm,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                Log.w("RecUpdateInstall", "install failed: $msg")
                notify(context, "녹음 앱 업데이트 설치 실패", msg, openApp(context))
            }
        }
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, title: String, text: String, pi: PendingIntent) {
        runCatching {
            val n = NotificationCompat.Builder(context, RecorderApp.CHANNEL_UPDATE)
                .setSmallIcon(R.drawable.ic_sync)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, n)
        }
    }

    companion object {
        const val EXTRA_VERSION = "com.bettertick.recorder.VERSION"
        private const val NOTIF_ID = 9001
    }
}
