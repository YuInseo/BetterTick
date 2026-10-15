package com.bettertick.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bettertick.MainActivity
import com.bettertick.data.model.occursOn
import com.bettertick.widget.WidgetServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** Posts task reminders and the 매일 알림 summary. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> showTaskReminder(context, intent)
            ReminderScheduler.ACTION_DAILY -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        showDailySummary(context)
                    } finally {
                        ReminderScheduler.scheduleDailySummary(context)
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun openAppIntent(context: Context, code: Int): PendingIntent =
        PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    @android.annotation.SuppressLint("MissingPermission") // guarded by canNotify()
    private fun showTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY).orEmpty()
        val notifId = taskId.hashCode()

        fun action(act: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = act
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                putExtra(ReminderScheduler.EXTRA_TITLE, title)
                putExtra(ReminderScheduler.EXTRA_BODY, body)
                putExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, ReminderScheduler.channelId(context))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, notifId))
            .addAction(0, "완료", action(ReminderScheduler.ACTION_COMPLETE, notifId + 1))
            .addAction(0, "10분 후 다시 알림", action(ReminderScheduler.ACTION_SNOOZE, notifId + 2))
            .build()
        if (canNotify(context)) {
            runCatching { NotificationManagerCompat.from(context).notify(notifId, n) }
        }
    }

    @android.annotation.SuppressLint("MissingPermission") // guarded by canNotify()
    private suspend fun showDailySummary(context: Context) {
        if (!WidgetServiceLocator.isInitialized) return
        val tasks = withTimeoutOrNull(8_000) {
            WidgetServiceLocator.taskRepository.observeAllTasks().first()
        } ?: return
        val today = LocalDate.now()
        val due = tasks.filter { t ->
            !t.isCompleted && !t.isAbandoned && t.dueDate != null &&
                (t.occursOn(today) || (t.repeatRule.isNullOrBlank() &&
                    t.dueDate.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(today)))
        }
        val text = if (due.isEmpty()) "오늘 예정된 할일이 없습니다." else "오늘 할일 ${due.size}개"
        val style = NotificationCompat.InboxStyle().also { s -> due.take(6).forEach { s.addLine("• ${it.title}") } }
        val n = NotificationCompat.Builder(context, ReminderScheduler.channelId(context))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("오늘의 할일")
            .setContentText(text)
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, 7_770_002))
            .build()
        if (canNotify(context)) {
            runCatching { NotificationManagerCompat.from(context).notify(7_770_002, n) }
        }
    }
}

private fun canNotify(context: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

/** Notification buttons: 완료 / 10분 후 다시 알림. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, taskId.hashCode())
        NotificationManagerCompat.from(context).cancel(notifId)
        when (intent.action) {
            ReminderScheduler.ACTION_SNOOZE -> ReminderScheduler.snooze(
                context, taskId,
                intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(ReminderScheduler.EXTRA_BODY).orEmpty()
            )
            ReminderScheduler.ACTION_COMPLETE -> {
                if (!WidgetServiceLocator.isInitialized) return
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        WidgetServiceLocator.taskRepository.toggleComplete(taskId, true)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
