package com.bettertick.recorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class RecorderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORDING, "녹음 중", NotificationManager.IMPORTANCE_LOW).apply {
                description = "녹음이 이어지는 동안 표시됩니다"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BACKUP, "드라이브 백업", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "백업에 다시 로그인이 필요할 때 알립니다"
            }
        )
        if (Settings.get(this).autoBackup) BackupScheduler.schedulePeriodic(this)
    }

    companion object {
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_BACKUP = "backup"
    }
}
