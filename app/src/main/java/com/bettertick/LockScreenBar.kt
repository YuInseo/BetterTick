package com.bettertick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 잠금화면에서 암호 없이 QuickAdd/QuickMemo를 열 수 있도록
 * VISIBILITY_PUBLIC 고정 알림을 유지한다.
 *
 * 홈 화면 위젯/버튼은 keyguard를 통과해야 하지만,
 * 알림 액션은 showWhenLocked Activity와 결합하면
 * PIN/생체인증 없이 잠금화면 위에 팝업을 띄울 수 있다.
 */
object LockScreenBar {

    private const val CHANNEL_ID = "bettertick_lockscreen"
    private const val NOTIF_ID = 9901

    fun show(context: Context) {
        if (!hasNotificationPermission(context)) return
        createChannel(context)

        val taskIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, QuickAddActivity::class.java).apply {
                action = "com.bettertick.QUICK_ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val memoIntent = PendingIntent.getActivity(
            context, 1,
            Intent(context, QuickMemoActivity::class.java).apply {
                action = "com.bettertick.QUICK_MEMO"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BetterTick")
            .setContentText("탭해서 할일·메모를 빠르게 추가")
            // 잠금화면에서도 알림 내용 전체 표시 (버튼 포함)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // 사용자가 직접 지울 수 없는 고정 알림
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(taskIntent)
            .addAction(0, "+ 할일", taskIntent)
            .addAction(0, "✏ 메모", memoIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "잠금화면 빠른 추가",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "잠금화면에서 할일·메모를 바로 추가할 수 있는 고정 알림"
                setShowBadge(false)
                // 채널 자체도 잠금화면에 공개
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
