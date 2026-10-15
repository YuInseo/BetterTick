package com.bettertick.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.bettertick.data.model.Task
import com.bettertick.data.model.occursOn
import com.bettertick.ui.theme.AppPrefs
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Task reminders (TickTick 알림): turns each task's [Task.reminders] offsets
 * into exact alarms. [syncAll] is called with the full task list whenever it
 * changes, cancels alarms that are no longer wanted and (re)schedules the rest.
 */
object ReminderScheduler {
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val ACTION_REMIND = "com.bettertick.reminder.REMIND"
    const val ACTION_DAILY = "com.bettertick.reminder.DAILY"
    const val ACTION_COMPLETE = "com.bettertick.reminder.COMPLETE"
    const val ACTION_SNOOZE = "com.bettertick.reminder.SNOOZE"

    private const val STATE_PREFS = "reminder_state"
    private const val DAILY_REQUEST = 7_770_001
    /** All-day tasks are reminded relative to this time. */
    private val ALL_DAY_TIME: LocalTime = LocalTime.of(9, 0)

    data class Fire(val code: Int, val at: Long, val task: Task, val occurrence: LocalDateTime)

    /** Upcoming alarm instants for [task] (next occurrence only for repeats). */
    fun firesFor(task: Task, now: LocalDateTime = LocalDateTime.now()): List<Fire> {
        if (task.reminders.isEmpty() || task.isCompleted || task.isAbandoned) return emptyList()
        val due = task.dueDate ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val dueTime = due.toDate().toInstant().atZone(zone).toLocalTime()
        val time = if (dueTime == LocalTime.MIDNIGHT) ALL_DAY_TIME else dueTime
        val maxOffset = task.reminders.maxOrNull() ?: 0

        val dates: Sequence<LocalDate> = if (task.repeatRule.isNullOrBlank()) {
            sequenceOf(due.toLocalDate())
        } else {
            // Next occurrences whose reminder can still fire.
            val from = now.toLocalDate()
            generateSequence(from) { it.plusDays(1) }
                .take(400)
                .filter { task.occursOn(it) }
        }
        for (date in dates) {
            val occ = date.atTime(time)
            val fires = task.reminders.distinct().mapNotNull { off ->
                val at = occ.minusMinutes(off.toLong())
                if (!at.isAfter(now)) null
                else Fire(requestCode(task.id, off, date), at.atZone(zone).toInstant().toEpochMilli(), task, occ)
            }
            if (fires.isNotEmpty()) return fires
            // Every reminder of this occurrence is in the past → try the next one.
            if (task.repeatRule.isNullOrBlank()) return emptyList()
            if (occ.plusMinutes(maxOffset.toLong()).isAfter(now.plusYears(1))) break
        }
        return emptyList()
    }

    private fun requestCode(taskId: String, offset: Int, date: LocalDate): Int =
        "$taskId|$offset|$date".hashCode()

    fun syncAll(context: Context, tasks: List<Task>) {
        val now = LocalDateTime.now()
        val wanted = tasks.flatMap { firesFor(it, now) }
        val prefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet("codes", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val wantedCodes = wanted.map { it.code }.toSet()
        (previous - wantedCodes).forEach { cancel(context, it) }
        wanted.forEach { f ->
            schedule(
                context, f.code, f.at, f.task.id, f.task.title,
                bodyFor(f.occurrence, allDay = f.task.dueDate?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())?.toLocalTime() == LocalTime.MIDNIGHT)
            )
        }
        prefs.edit().putStringSet("codes", wantedCodes.map { it.toString() }.toSet()).apply()
    }

    fun bodyFor(occ: LocalDateTime, allDay: Boolean): String {
        val today = LocalDate.now()
        val day = when (occ.toLocalDate()) {
            today -> "오늘"
            today.plusDays(1) -> "내일"
            else -> "${occ.monthValue}월 ${occ.dayOfMonth}일"
        }
        return if (allDay) day else "$day ${AppPrefs.formatTime(occ.toLocalTime())}"
    }

    private fun remindIntent(context: Context, code: Int, taskId: String, title: String, body: String) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIFICATION_ID, code)
        }

    private fun pending(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context, code: Int, at: Long, taskId: String, title: String, body: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pending(context, code, remindIntent(context, code, taskId, title, body))
        setAlarm(am, at, pi)
    }

    private fun setAlarm(am: AlarmManager, at: Long, pi: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, code: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context, code, Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    /** "10분 후 다시 알림". Uses its own request code so a later [syncAll]
     *  (which cancels fired, no-longer-wanted codes) can't drop it. */
    fun snooze(context: Context, taskId: String, title: String, body: String, minutes: Long = 10) {
        val code = "snooze|$taskId".hashCode()
        schedule(context, code, System.currentTimeMillis() + minutes * 60_000, taskId, title, body)
    }

    /** 설정 > 사운드 & 알림 > 매일 알림. */
    fun scheduleDailySummary(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_DAILY)
        val pi = pending(context, DAILY_REQUEST, intent)
        if (!AppPrefs.dailySummary) {
            am.cancel(pi)
            return
        }
        val m = AppPrefs.dailySummaryMinutes
        var next = LocalDate.now().atTime(m / 60, m % 60)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        setAlarm(am, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }

    /** Notification channel whose sound follows 알림 벨소리. A channel's sound
     *  is fixed once created, so the id carries a hash of the chosen tone. */
    fun channelId(context: Context): String {
        val tone = AppPrefs.reminderRingtone
        val id = "task_reminders_" + (tone?.hashCode()?.toString(16) ?: "default")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return id
            nm.notificationChannels
                .filter { it.id.startsWith("task_reminders_") && it.id != id }
                .forEach { nm.deleteNotificationChannel(it.id) }
            if (nm.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(id, "할일 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "할일 마감·시작 알림"
                    enableVibration(true)
                    when {
                        tone == null -> Unit // system default sound
                        tone.isEmpty() -> setSound(null, null)
                        else -> setSound(
                            Uri.parse(tone),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                }
                nm.createNotificationChannel(ch)
            }
        }
        return id
    }
}
