package com.bettertick.recorder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val ONCE = "drive-backup"
    private const val PERIODIC = "drive-backup-periodic"

    private fun constraints(context: Context) = Constraints.Builder()
        .setRequiredNetworkType(
            if (Settings.get(context).wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        )
        .build()

    /** 네트워크가 되면 바로 백업. 이미 예약돼 있으면 그 뒤에 한 번 더. */
    fun backupNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints(context))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    /** 놓친 파일을 위해 6시간마다 한 번씩 확인. */
    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints(context))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ONCE)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    /** 설정(와이파이만 등)이 바뀌면 다시 예약. */
    fun reschedule(context: Context) {
        if (Settings.get(context).autoBackup) {
            schedulePeriodic(context)
            backupNow(context)
        } else {
            cancelAll(context)
        }
    }
}

/** 아직 안 올라간 녹음 파일을 드라이브 "Daydrift 녹음" 폴더에 올린다. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settings = Settings.get(ctx)
        val store = RecordingStore.get(ctx)
        if (settings.driveAccount == null) return Result.success()

        val current = RecorderState.status.value.currentFile
        val pending = (store.dir.listFiles { f -> f.isFile && f.name.endsWith(".m4a") } ?: emptyArray())
            .filter { it.absolutePath != current && !store.isUploaded(it.name) && it.length() > 0 }
            .sortedBy { it.name }
        if (pending.isEmpty()) return Result.success()

        var token = try {
            DriveClient.token(ctx)
        } catch (e: DriveClient.NeedsSignInException) {
            settings.setLastBackupMessage("드라이브에 다시 연결해 주세요")
            notifyNeedsSignIn(ctx)
            return Result.failure()
        } catch (e: Exception) {
            settings.setLastBackupMessage("백업 실패: ${e.message}")
            return Result.retry()
        }

        var done = 0
        try {
            var folder = try {
                DriveClient.folderId(ctx, token)
            } catch (e: DriveClient.HttpException) {
                if (e.code != 401) throw e
                DriveClient.clearToken(ctx, token)
                token = DriveClient.token(ctx)
                DriveClient.folderId(ctx, token)
            }
            for (file in pending) {
                if (isStopped) break
                if (!file.exists()) continue
                val id = try {
                    DriveClient.upload(file, folder, token)
                } catch (e: DriveClient.HttpException) {
                    when (e.code) {
                        401 -> {
                            DriveClient.clearToken(ctx, token)
                            token = DriveClient.token(ctx)
                            DriveClient.upload(file, folder, token)
                        }
                        404 -> {
                            // 폴더가 지워졌으면 다시 만든다.
                            settings.driveFolderId = null
                            folder = DriveClient.folderId(ctx, token)
                            DriveClient.upload(file, folder, token)
                        }
                        else -> throw e
                    }
                }
                store.markUploaded(file.name, id)
                if (settings.deleteAfterUpload) store.delete(file)
                done++
            }
        } catch (e: DriveClient.NeedsSignInException) {
            settings.setLastBackupMessage("드라이브에 다시 연결해 주세요")
            notifyNeedsSignIn(ctx)
            return Result.failure()
        } catch (e: Exception) {
            settings.setLastBackupMessage("백업 중단 (${done}개 완료): ${e.message}")
            return Result.retry()
        }

        val time = SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(Date())
        settings.setLastBackupMessage("$time 백업 완료 · ${done}개 올림")
        return Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notifyNeedsSignIn(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 10, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, RecorderApp.CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("드라이브 백업이 멈췄어요")
            .setContentText("앱을 열어 구글 드라이브에 다시 연결해 주세요")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(2, n)
    }
}
