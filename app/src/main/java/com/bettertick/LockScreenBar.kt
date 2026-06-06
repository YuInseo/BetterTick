package com.bettertick

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

/**
 * 잠금화면에서 PIN 없이 할일·메모를 추가할 수 있는 고정 알림.
 *
 * RemoteInput 인라인 입력을 사용해 알림에서 바로 텍스트 입력 → Activity 실행 없이
 * BroadcastReceiver가 저장. Android 12+ PIN 차단 우회.
 */
object LockScreenBar {

    private const val CHANNEL_ID = "bettertick_lockscreen"
    const val NOTIF_ID = 9901

    @SuppressLint("MissingPermission")
    fun show(context: Context) {
        if (!hasNotificationPermission(context)) return
        createChannel(context)

        val taskRemoteInput = RemoteInput.Builder(LockScreenInputReceiver.KEY_TASK_TEXT)
            .setLabel("할일을 입력하세요...")
            .build()

        val taskReplyPendingIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, LockScreenInputReceiver::class.java).apply {
                action = "com.bettertick.QUICK_ADD_TASK"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        PendingIntent.FLAG_MUTABLE
                    else 0
        )

        val taskAction = NotificationCompat.Action.Builder(0, "+ 할일", taskReplyPendingIntent)
            .addRemoteInput(taskRemoteInput)
            .setAllowGeneratedReplies(false)
            .build()

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

        val contentIntent = PendingIntent.getActivity(
            context, 2,
            Intent(context, QuickAddActivity::class.java).apply {
                action = "com.bettertick.QUICK_ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BetterTick")
            .setContentText("+ 할일 버튼으로 잠금화면에서 바로 추가")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .addAction(taskAction)
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
