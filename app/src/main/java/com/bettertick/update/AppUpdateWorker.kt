package com.bettertick.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bettertick.R
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val release = AppUpdater.fetchLatest() ?: return Result.success()
        val current = AppUpdater.currentVersionCode(context)
        if (!AppUpdater.isNewer(release.versionCode, current)) return Result.success()

        val apk = AppUpdater.downloadApk(context, release) ?: return Result.retry()

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context, 0, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BetterTick 업데이트 준비 완료")
            .setContentText("v${release.versionName} 다운로드 완료 — 탭하여 설치")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIF_ID, notif)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "bettertick_update"
        private const val NOTIF_ID = 9001
        private const val WORK_NAME = "bettertick_app_update"

        fun createNotificationChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                "앱 업데이트",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "새 버전 출시 시 알림"
            }
            nm.createNotificationChannel(ch)
        }

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
