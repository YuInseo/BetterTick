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
import com.bettertick.data.model.Habit
import com.bettertick.ui.screens.habits.HabitPrefs
import com.bettertick.ui.screens.habits.isDueOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Habit reminders (TickTick 습관 알림): each "HH:mm" in [Habit.reminders]
 * fires on the next day the habit is due. [syncAll] runs whenever the habit
 * list changes and again after every reminder fires.
 */
object HabitReminderScheduler {
    const val ACTION_HABIT = "com.bettertick.reminder.HABIT"
    const val ACTION_HABIT_CHECK_IN = "com.bettertick.reminder.HABIT_CHECK_IN"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_HABIT_NAME = "habit_name"
    const val EXTRA_DATE = "date"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    private const val STATE_PREFS = "habit_reminder_state"

    data class Fire(val code: Int, val at: Long, val habit: Habit, val date: LocalDate)

    fun parseTime(s: String): LocalTime? = runCatching { LocalTime.parse(s.trim()) }.getOrNull()

    fun firesFor(habit: Habit, now: LocalDateTime = LocalDateTime.now()): List<Fire> {
        if (habit.isArchived || habit.reminders.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        return habit.reminders.distinct().mapNotNull { raw ->
            val time = parseTime(raw) ?: return@mapNotNull null
            val date = generateSequence(now.toLocalDate()) { it.plusDays(1) }
                .take(60)
                .firstOrNull { d -> habit.isDueOn(d) && d.atTime(time).isAfter(now) }
                ?: return@mapNotNull null
            Fire(
                code = "habit|${habit.id}|$raw".hashCode(),
                at = date.atTime(time).atZone(zone).toInstant().toEpochMilli(),
                habit = habit,
                date = date
            )
        }
    }

    fun syncAll(context: Context, habits: List<Habit>) {
        val wanted = habits.flatMap { firesFor(it) }
        val prefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet("codes", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val wantedCodes = wanted.map { it.code }.toSet()
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        (previous - wantedCodes).forEach { code ->
            PendingIntent.getBroadcast(
                context, code, Intent(context, ReminderReceiver::class.java).setAction(ACTION_HABIT),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it); it.cancel() }
        }
        wanted.forEach { f ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_HABIT
                putExtra(EXTRA_HABIT_ID, f.habit.id)
                putExtra(EXTRA_HABIT_NAME, f.habit.name)
                putExtra(EXTRA_DATE, f.date.toString())
                putExtra(EXTRA_NOTIFICATION_ID, f.code)
            }
            val pi = PendingIntent.getBroadcast(
                context, f.code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            runCatching {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, f.at, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, f.at, pi)
            }
        }
        prefs.edit().putStringSet("codes", wantedCodes.map { it.toString() }.toSet()).apply()
    }

    /** Channel whose sound follows 습관 설정 > 습관 벨소리. */
    fun channelId(context: Context): String {
        val tone = HabitPrefs.ringtone
        val id = "habit_reminders_" + (tone?.hashCode()?.toString(16) ?: "default")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return id
            nm.notificationChannels
                .filter { it.id.startsWith("habit_reminders_") && it.id != id }
                .forEach { nm.deleteNotificationChannel(it.id) }
            if (nm.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(id, "습관 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "습관 체크인 알림"
                    enableVibration(true)
                    when {
                        tone == null -> Unit
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
