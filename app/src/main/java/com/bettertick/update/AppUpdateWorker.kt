package com.bettertick.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
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
        // Same single-run path as the app-open check and the settings button.
        val outcome = AppUpdater.run(context, AppUpdater.Trigger.BACKGROUND) { release ->
            // Promote to foreground so the OS doesn't kill the download.
            runCatching { setForeground(createDownloadForegroundInfo(release.versionName)) }
        }
        return if (outcome is AppUpdater.Outcome.Failed) Result.retry() else Result.success()
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
        private const val DOWNLOAD_NOTIF_ID = 9002
        private const val PERIODIC_WORK_NAME = "bettertick_app_update"

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
    }
}
