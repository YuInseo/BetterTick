package com.bettertick.recorder

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle

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
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATE, "앱 업데이트", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "새 버전 설치 안내"
            }
        )
        if (Settings.get(this).autoBackup) BackupScheduler.schedulePeriodic(this)
        trackForeground()
    }

    /** 화면에 떠 있는지 센다. 마지막 화면을 나가면 대기 중인 조용한 업데이트를 설치. */
    private fun trackForeground() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: Activity) {
                started++
                Updater.isForeground = true
            }
            override fun onActivityStopped(activity: Activity) {
                started--
                if (started <= 0) {
                    Updater.isForeground = false
                    Updater.onAppBackground(this@RecorderApp)
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_BACKUP = "backup"
        const val CHANNEL_UPDATE = "update"
    }
}
