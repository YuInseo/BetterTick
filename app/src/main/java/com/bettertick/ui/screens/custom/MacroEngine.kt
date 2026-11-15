package com.bettertick.ui.screens.custom

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bettertick.data.model.Task
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.occursOn
import com.bettertick.data.model.shiftKindOn
import com.bettertick.ui.screens.habits.isDueOn
import com.bettertick.ui.screens.tasks.HabitTodayItem
import com.bettertick.ui.screens.tasks.MoneyEntry
import com.bettertick.ui.screens.tasks.MoneyPrefs
import com.bettertick.widget.WidgetServiceLocator
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Runs 매크로 blocks: from their button, and by themselves when one of their
 * events happens (정해진 시각 · 앱/탭 열기 · 할 일·습관 다 끝냄 · 변수 조건).
 * Every run first checks the macro's 실행 조건.
 */
object MacroEngine {
    const val ACTION_TIME = "com.bettertick.macro.TIME"
    const val EXTRA_TAB = "tab"
    const val EXTRA_BLOCK = "block"
    const val EXTRA_TRIGGER = "trigger"
    private const val PREFS = "macro_runs"
    private const val CHANNEL = "macro_runs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Every macro block across the user's tabs. */
    private fun macros(): List<Triple<CustomTab, DashBlock, MacroConfig>> =
        CustomTabsPrefs.tabs.flatMap { tab ->
            tab.blocks.filter { it.type == DashBlockType.MACRO }.map { Triple(tab, it, MacroConfig.decode(it.text)) }
        }

    fun conditionsMet(config: MacroConfig, ctx: DashContext): Boolean =
        config.conditions.all { c -> compareValue(DashVars.value(c.variable, ctx), c.op, c.value) }

    /** The data a run reads, from the app-wide caches (works without a screen open). */
    suspend fun liveContext(): DashContext? {
        if (!WidgetServiceLocator.isInitialized) return null
        return withTimeoutOrNull(8_000) {
            val tasks = WidgetServiceLocator.tasksCache.value.ifEmpty {
                runCatching { WidgetServiceLocator.taskRepository.observeAllTasks().first() }.getOrDefault(emptyList())
            }
            val lists = WidgetServiceLocator.listsCache.value
            val today = LocalDate.now()
            val habits = runCatching {
                val all = WidgetServiceLocator.habitRepository.observeHabits().first()
                val done = WidgetServiceLocator.habitRepository.observeHabitLogs(today.toString(), today.toString()).first()
                    .filter { it.isCompleted }.map { it.habitId }.toSet()
                all.filter { !it.isArchived && it.isDueOn(today) }.map { HabitTodayItem(it, it.id in done) }
            }.getOrDefault(emptyList())
            DashContext(tasks, lists, habits, null, today)
        }
    }

    /**
     * Runs [config]'s steps in order; returns a short result. A 화면 이동 step runs
     * last, and only when [onNavigate] is given (i.e. the app is on screen).
     */
    suspend fun execute(tabId: String, config: MacroConfig, ctx: DashContext, onNavigate: ((String) -> Unit)?,
                        report: (String) -> Unit): Int {
        var navigateTo: String? = null
        var done = 0
        var reported = false
        val now = LocalTime.now()
        val tasksRepo = if (WidgetServiceLocator.isInitialized) WidgetServiceLocator.taskRepository else null
        val habitRepo = if (WidgetServiceLocator.isInitialized) WidgetServiceLocator.habitRepository else null
        for (step in config.steps) {
            val ok = runCatching {
                when (step.type) {
                    MacroType.ADD_TASK -> {
                        val title = DashVars.fill(step.text, ctx).trim()
                        require(title.isNotBlank())
                        val day = ctx.today.plusDays(step.amount)
                        val time = runCatching { LocalTime.parse(step.time) }.getOrNull()
                        val at = (if (time != null) day.atTime(time) else day.atStartOfDay()).atZone(ZoneId.systemDefault())
                        requireNotNull(tasksRepo).addTask(Task(title = title, listId = step.target,
                            dueDate = Timestamp(Date.from(at.toInstant())), sortOrder = System.currentTimeMillis()))
                    }
                    MacroType.COMPLETE_TODAY -> ctx.tasks.filter {
                        !it.isShiftTask() && it.parentId.isBlank() && !it.isAbandoned &&
                            it.occursOn(ctx.today) && !it.completedOn(ctx.today)
                    }.forEach { requireNotNull(tasksRepo).toggleCompleteOn(it, ctx.today) }
                    MacroType.CHECK_HABITS -> ctx.habits.filter { !it.done }
                        .forEach { requireNotNull(habitRepo).toggleHabitLog(it.habit.id, ctx.today.toString()) }
                    MacroType.ADD_MONEY -> {
                        require(step.amount > 0)
                        MoneyPrefs.saveEntry(MoneyEntry(date = ctx.today, amount = step.amount, income = step.flag,
                            category = step.target.ifBlank { "기타" }, memo = DashVars.fill(step.text, ctx)))
                    }
                    MacroType.APPEND_MEMO -> {
                        val tab = CustomTabsPrefs.tab(tabId) ?: error("no tab")
                        val memo = tab.blocks.firstOrNull { it.id == step.target && it.type == DashBlockType.MEMO }
                            ?: tab.blocks.firstOrNull { it.type == DashBlockType.MEMO }
                            ?: error("no memo block")
                        val line = "%02d:%02d ".format(now.hour, now.minute) + DashVars.fill(step.text, ctx)
                        val text = if (memo.text.isBlank()) line else memo.text.trimEnd() + "\n" + line
                        CustomTabsPrefs.update(tab.copy(blocks = tab.blocks.map { if (it.id == memo.id) it.copy(text = text) else it }))
                    }
                    MacroType.TOAST -> { report(DashVars.fill(step.text, ctx)); reported = true }
                    MacroType.NAVIGATE -> { navigateTo = step.target }
                    else -> error("unknown step")
                }
                Unit
            }.isSuccess
            if (ok) done++
        }
        if (!reported) {
            val failed = config.steps.size - done
            report(if (failed == 0) "${config.label} 완료" else "${config.label}: ${failed}단계 실패")
        }
        navigateTo?.takeIf { it.isNotBlank() }?.let { target -> onNavigate?.invoke(target) }
        return done
    }

    /** Checks conditions, runs, and remembers the run (for "하루 한 번" and 최근 실행). */
    fun fire(context: Context, tabId: String, blockId: String, config: MacroConfig, source: String,
             ctx: DashContext, onNavigate: ((String) -> Unit)? = null) {
        val app = context.applicationContext
        if (!conditionsMet(config, ctx)) {
            if (source == "버튼") report(app, "${config.label}: 실행 조건이 맞지 않아요")
            return
        }
        scope.launch {
            execute(tabId, config, ctx, onNavigate) { report(app, it) }
            val now = LocalDateTime.now()
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("last|$blockId", "${now.monthValue}/${now.dayOfMonth} %02d:%02d · $source".format(now.hour, now.minute))
                .apply()
        }
    }

    /** "11/15 08:30 · 정해진 시각" — the macro's latest run, or null. */
    fun lastRun(context: Context, blockId: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last|$blockId", null)

    /** True once per day per trigger: marks it as fired today. */
    private fun firstToday(context: Context, blockId: String, triggerId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "day|$blockId|$triggerId"
        val today = LocalDate.now().toString()
        if (prefs.getString(key, null) == today) return false
        prefs.edit().putString(key, today).apply()
        return true
    }

    private fun report(context: Context, message: String) {
        if (com.bettertick.update.AppUpdater.isForeground) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!canPost) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(CHANNEL) == null) {
                nm?.createNotificationChannel(NotificationChannel(CHANNEL, "매크로 실행", NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.bettertick.R.drawable.ic_stat_bettertick)
            .setContentTitle("매크로")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(message.hashCode(), n) }
    }

    // ── 이벤트 ──

    /** 앱을 열 때 / 이 탭을 열 때. */
    fun onOpen(context: Context, type: String, tabId: String? = null, ctx: DashContext? = null,
               onNavigate: ((String) -> Unit)? = null) {
        val targets = macros().filter { (tab, _, cfg) ->
            (tabId == null || tab.id == tabId) && cfg.triggers.any { it.enabled && it.type == type }
        }
        if (targets.isEmpty()) return
        scope.launch {
            val data = ctx ?: liveContext() ?: return@launch
            targets.forEach { (tab, block, cfg) ->
                val trigger = cfg.triggers.first { it.enabled && it.type == type }
                if (trigger.oncePerDay && !firstToday(context, block.id, trigger.id)) return@forEach
                fire(context, tab.id, block.id, cfg, TriggerType.label(type).removeSuffix("에").removeSuffix(" 때"), data, onNavigate)
            }
        }
    }

    /** Data changed (tasks / habits): 다 끝냈을 때 and 변수 조건 triggers, once a day each. */
    fun onDataChanged(context: Context) {
        val targets = macros().filter { (_, _, cfg) ->
            cfg.triggers.any { it.enabled && it.type in setOf(TriggerType.TASKS_DONE, TriggerType.HABITS_DONE, TriggerType.VALUE) }
        }
        if (targets.isEmpty()) return
        scope.launch {
            val ctx = liveContext() ?: return@launch
            val todayTasks = ctx.tasks.filter { !it.isShiftTask() && it.parentId.isBlank() && !it.isAbandoned && it.occursOn(ctx.today) }
            targets.forEach { (tab, block, cfg) ->
                cfg.triggers.filter { it.enabled }.forEach { t ->
                    val hit = when (t.type) {
                        TriggerType.TASKS_DONE -> todayTasks.isNotEmpty() && todayTasks.all { it.completedOn(ctx.today) }
                        TriggerType.HABITS_DONE -> ctx.habits.isNotEmpty() && ctx.habits.all { it.done }
                        TriggerType.VALUE -> compareValue(DashVars.value(t.variable, ctx), t.op, t.value)
                        else -> false
                    }
                    if (hit && firstToday(context, block.id, t.id)) {
                        fire(context, tab.id, block.id, cfg, TriggerType.label(t.type).removeSuffix(" 때"), ctx)
                    }
                }
            }
        }
    }

    // ── 정해진 시각 (알람) ──

    private fun code(blockId: String, triggerId: String) = "macro|$blockId|$triggerId".hashCode()

    private fun nextFire(t: MacroTrigger, now: LocalDateTime): LocalDateTime? {
        val time = runCatching { LocalTime.parse(t.time.trim()) }.getOrNull() ?: return null
        return generateSequence(now.toLocalDate()) { it.plusDays(1) }.take(8)
            .map { it.atTime(time) }
            .firstOrNull { at -> at.isAfter(now) && (t.days.isBlank() || at.dayOfWeek.value.toString() in t.days) }
    }

    /** Re-arms an alarm for every enabled 정해진 시각 trigger; cancels the rest. */
    fun syncAlarms(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet("codes", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val now = LocalDateTime.now()
        val wanted = mutableSetOf<Int>()
        macros().forEach { (tab, block, cfg) ->
            cfg.triggers.filter { it.enabled && it.type == TriggerType.TIME }.forEach { t ->
                val at = nextFire(t, now) ?: return@forEach
                val c = code(block.id, t.id)
                wanted += c
                val intent = Intent(app, MacroAlarmReceiver::class.java).apply {
                    action = ACTION_TIME
                    putExtra(EXTRA_TAB, tab.id); putExtra(EXTRA_BLOCK, block.id); putExtra(EXTRA_TRIGGER, t.id)
                }
                val pi = PendingIntent.getBroadcast(app, c, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
                runCatching {
                    if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                }
            }
        }
        (previous - wanted).forEach { c ->
            PendingIntent.getBroadcast(app, c, Intent(app, MacroAlarmReceiver::class.java).setAction(ACTION_TIME),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { am.cancel(it); it.cancel() }
        }
        prefs.edit().putStringSet("codes", wanted.map { it.toString() }.toSet()).apply()
    }

    /** An alarm went off: check its 근무 filter, run, and arm the next one. */
    suspend fun onAlarm(context: Context, tabId: String, blockId: String, triggerId: String) {
        try {
            val tab = CustomTabsPrefs.tab(tabId) ?: return
            val block = tab.blocks.firstOrNull { it.id == blockId } ?: return
            val cfg = MacroConfig.decode(block.text)
            val t = cfg.triggers.firstOrNull { it.id == triggerId && it.enabled } ?: return
            val ctx = liveContext() ?: return
            if (t.shift.isNotBlank()) {
                val kind = ctx.tasks.firstOrNull { it.isShiftTask() && it.occursOn(ctx.today) }?.shiftKindOn(ctx.today)
                if (kind != t.shift) return
            }
            fire(context, tabId, blockId, cfg, "정해진 시각 ${t.time}", ctx)
        } finally {
            syncAlarms(context)
        }
    }
}

/** Wakes a 정해진 시각 macro. */
class MacroAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MacroEngine.ACTION_TIME) return
        val tab = intent.getStringExtra(MacroEngine.EXTRA_TAB) ?: return
        val block = intent.getStringExtra(MacroEngine.EXTRA_BLOCK) ?: return
        val trigger = intent.getStringExtra(MacroEngine.EXTRA_TRIGGER) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                MacroEngine.onAlarm(context.applicationContext, tab, block, trigger)
                // Let the run's own coroutine finish its writes before the process may be frozen.
                kotlinx.coroutines.delay(3_000)
            } finally {
                pending.finish()
            }
        }
    }
}
