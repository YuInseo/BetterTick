package com.bettertick.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
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

        // Promote to foreground service so OS doesn't kill us during download
        setForeground(createDownloadForegroundInfo(release.versionName))

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
        val notif = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BetterTick 업데이트 준비 완료")
            .setContentText("v${release.versionName} 다운로드 완료 — 탭하여 설치")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(UPDATE_NOTIF_ID, notif)
        return Result.success()
    }

    private fun createDownloadForegroundInfo(version: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BetterTick 업데이트 다운로드 중")
            .setContentText("v$version 다운로드 중...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(DOWNLOAD_NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(DOWNLOAD_NOTIF_ID, notification)
        }
    }

    companion object {
        const val UPDATE_CHANNEL_ID = "bettertick_update"
        const val DOWNLOAD_CHANNEL_ID = "bettertick_update_download"
        private const val UPDATE_NOTIF_ID = 9001
        private const val DOWNLOAD_NOTIF_ID = 9002
        private const val PERIODIC_WORK_NAME = "bettertick_app_update"
        private const val IMMEDIATE_WORK_NAME = "bettertick_app_update_now"

        fun createNotificationChannels(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(UPDATE_CHANNEL_ID, "앱 업데이트", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "새 버전 출시 시 알림" }
            )
            nm.createNotificationChannel(
                NotificationChannel(DOWNLOAD_CHANNEL_ID, "업데이트 다운로드", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "업데이트 다운로드 진행 중 표시" }
            )
        }

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Run an immediate check on first launch or after update. */
        fun runImmediately(context: Context) {
            val request = OneTimeWorkRequestBuilder<AppUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
