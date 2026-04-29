package com.bettertick.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bettertick.R
import java.io.File

/**
 * 업데이트 fetch + APK 다운로드를 Activity 라이프사이클 밖에서 수행한다.
 * 사용자가 앱 화면을 닫거나 다른 화면으로 가도 다운로드가 끊기지 않고,
 * 끝나면 알림을 띄워 탭 한 번으로 시스템 설치 프롬프트로 진입.
 */
class UpdateWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForegroundInfo("업데이트 확인 중…")

    override suspend fun doWork(): Result {
        return try {
            ensureChannel(appContext)
            setForeground(buildForegroundInfo("최신 버전 확인 중…"))

            val release = AppUpdater.fetchLatest()
                ?: return Result.failure()

            val current = AppUpdater.currentVersionCode(appContext)
            if (!AppUpdater.isNewer(release.versionCode, current)) {
                return Result.success()
            }

            setForeground(buildForegroundInfo("v${release.versionName} 다운로드 중…"))
            val apk = AppUpdater.downloadApk(appContext, release)
                ?: return Result.failure()

            postInstallReady(apk, release)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "UpdateWorker failed", e)
            Result.failure()
        }
    }

    private fun buildForegroundInfo(text: String): ForegroundInfo {
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("BetterTick 업데이트")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIF_PROGRESS_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_PROGRESS_ID, notif)
        }
    }

    private fun postInstallReady(apk: File, release: AppUpdater.Release) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apk
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        val pi = PendingIntent.getActivity(
            appContext,
            0,
            installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("업데이트 준비 완료")
            .setContentText("v${release.versionName} — 탭하여 설치")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_INSTALL_ID, notif)
    }

    companion object {
        private const val TAG = "UpdateWorker"
        private const val CHANNEL_ID = "bettertick_update"
        private const val NOTIF_PROGRESS_ID = 4101
        private const val NOTIF_INSTALL_ID = 4102
        const val WORK_NAME = "bettertick_update_check"

        fun enqueue(context: Context) {
            ensureChannel(context)
            val req = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            // KEEP: 이미 같은 작업이 돌고 있으면 새로 띄우지 않는다
            // (자동 체크 + 사용자 수동 탭이 겹쳐도 한 번만 실행).
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, req)
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val ch = NotificationChannel(
                CHANNEL_ID,
                "앱 업데이트",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "최신 APK 다운로드 진행 상태와 설치 알림"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }
}
