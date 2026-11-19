package com.bettertick.ui.screens.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.abandonedOn
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.occursOn
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.TextSecondary
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** What a 할 일 블록 shows and how: a small saved filter, like a smart list. */
data class TaskViewConfig(
    val range: String = "today",
    val listIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    /** 0 = any, else at least this priority (1 낮음 · 2 보통 · 3 높음). */
    val minPriority: Int = 0,
    val showDone: Boolean = true,
    val sort: String = "time",
    val group: String = "none",
    /** 0 = no limit. */
    val limit: Int = 10,
    val showDate: Boolean = true,
    val showList: Boolean = false,
    val inlineAdd: Boolean = true
) {
    fun encode(): String = JSONObject().apply {
        put("range", range); put("listIds", JSONArray(listIds)); put("tagIds", JSONArray(tagIds))
        put("minPriority", minPriority); put("showDone", showDone); put("sort", sort); put("group", group)
        put("limit", limit); put("showDate", showDate); put("showList", showList); put("inlineAdd", inlineAdd)
    }.toString()

    companion object {
        /** Old blocks stored a bare list id (목록 할 일) or nothing (오늘 할 일). */
        fun decode(raw: String, type: String): TaskViewConfig {
            val fallback = if (type == DashBlockType.TODAY_TASKS) TaskViewConfig()
            else TaskViewConfig(range = "open", listIds = listOfNotNull(raw.takeIf { it.isNotBlank() && !it.startsWith("{") }),
                showDone = false, showDate = true)
            if (!raw.trim().startsWith("{")) return fallback
            return runCatching {
                val o = JSONObject(raw)
                fun arr(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
                TaskViewConfig(
                    range = o.optString("range", fallback.range), listIds = arr("listIds"), tagIds = arr("tagIds"),
                    minPriority = o.optInt("minPriority"), showDone = o.optBoolean("showDone", true),
                    sort = o.optString("sort", "time"), group = o.optString("group", "none"), limit = o.optInt("limit", 10),
                    showDate = o.optBoolean("showDate", true), showList = o.optBoolean("showList"),
                    inlineAdd = o.optBoolean("inlineAdd", true)
                )
            }.getOrDefault(fallback)
        }
    }
}

val TaskRanges = listOf(
    "today" to "오늘", "today_overdue" to "오늘 + 밀린 것", "tomorrow" to "내일", "week" to "이번 주",
    "next7" to "다음 7일", "overdue" to "밀린 것", "open" to "남은 할 일 전부", "nodate" to "날짜 없음"
)
private val TaskSorts = listOf("time" to "시간순", "priority" to "중요도순", "title" to "이름순", "list" to "목록순")
private val TaskGroups = listOf("none" to "묶지 않음", "date" to "날짜별", "list" to "목록별", "priority" to "중요도별")
private val TaskLimits = listOf(5 to "5개", 10 to "10개", 20 to "20개", 0 to "전부")

private fun Task.localDue(): Pair<LocalDate, LocalTime?>? {
    val z = dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault()) ?: return null
    val t = z.toLocalTime()
    return z.toLocalDate() to (if (t == LocalTime.MIDNIGHT) null else t)
}

/** One row of the block: the task and the day it is shown for. */
data class TaskViewRow(val task: Task, val day: LocalDate?)

fun taskViewRows(cfg: TaskViewConfig, tasks: List<Task>, today: LocalDate = LocalDate.now()): List<TaskViewRow> {
    val base = tasks.filter { t ->
        !t.isShiftTask() && t.parentId.isBlank() && !t.isAbandoned &&
            (cfg.listIds.isEmpty() || t.listId in cfg.listIds) &&
            (cfg.tagIds.isEmpty() || t.tagIds.any { it in cfg.tagIds }) &&
            (cfg.minPriority == 0 || t.priority >= cfg.minPriority)
    }
    fun on(day: LocalDate) = base.filter { it.occursOn(day) }.map { TaskViewRow(it, day) }
    val overdue = base.filter { t ->
        t.repeatRule.isNullOrBlank() && !t.isCompleted && t.localDue()?.first?.isBefore(today) == true
    }.map { TaskViewRow(it, it.localDue()?.first) }
    val rows = when (cfg.range) {
        "today" -> on(today)
        "today_overdue" -> overdue + on(today)
        "tomorrow" -> on(today.plusDays(1))
        "week" -> { val m = today.with(DayOfWeek.MONDAY); (0L until 7L).flatMap { on(m.plusDays(it)) } }
        "next7" -> (0L until 7L).flatMap { on(today.plusDays(it)) }
        "overdue" -> overdue
        "nodate" -> base.filter { it.dueDate == null }.map { TaskViewRow(it, null) }
        else -> base.filter { !it.isCompleted }.map { TaskViewRow(it, it.localDue()?.first) }
    }
    val shown = if (cfg.showDone) rows else rows.filterNot { done(it) }
    val order: Comparator<TaskViewRow> = when (cfg.sort) {
        "priority" -> compareByDescending<TaskViewRow> { it.task.priority }.thenBy { it.day ?: LocalDate.MAX }
        "title" -> compareBy { it.task.title }
        "list" -> compareBy<TaskViewRow> { it.task.listId }.thenBy { it.day ?: LocalDate.MAX }
        else -> compareBy<TaskViewRow> { it.day ?: LocalDate.MAX }
            .thenBy { it.task.localDue()?.second ?: LocalTime.MAX }
    }
    return shown.sortedWith(compareBy<TaskViewRow> { done(it) }.then(order))
}

private fun done(r: TaskViewRow) =
    if (r.day != null) r.task.completedOn(r.day) || r.task.abandonedOn(r.day) else r.task.isCompleted

private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "오늘"
    today.plusDays(1) -> "내일"
    today.minusDays(1) -> "어제"
    else -> "${day.monthValue}/${day.dayOfMonth}(${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
}

private fun priorityColor(p: Int): Color? = when (p) {
    3 -> Color(0xFFE5483F); 2 -> Color(0xFFF5A623); 1 -> Color(0xFF4772FA); else -> null
}

/** 할 일 블록: filtered tasks, tap to open, check to complete, add right there. */
@Composable
internal fun TaskViewBlockView(
    block: DashBlock,
    tasks: List<Task>,
    lists: List<TaskList>,
    fill: Boolean,
    onToggle: (Task, LocalDate) -> Unit,
    onOpen: (Task, LocalDate?) -> Unit,
    onAdd: (title: String, listId: String, date: LocalDate?) -> Unit,
    today: LocalDate = LocalDate.now()
) {
    val cfg = remember(block.text, block.type) { TaskViewConfig.decode(block.text, block.type) }
    val rows = remember(tasks, cfg, today) { taskViewRows(cfg, tasks, today) }
    var expanded by rememberSaveable(block.id) { mutableStateOf(false) }
    val visible = if (cfg.limit == 0 || expanded) rows else rows.take(cfg.limit)
    val title = block.title.ifBlank {
        val r = TaskRanges.firstOrNull { it.first == cfg.range }?.second ?: "할 일"
        val l = cfg.listIds.singleOrNull()?.let { id -> lists.firstOrNull { it.id == id }?.name }
        if (l != null) "$l · $r" else r
    }
    val doneCount = rows.count { done(it) }

    Column(
        Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(blockBackground()).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = blockHeadingColor(), fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (rows.isNotEmpty()) Text("$doneCount/${rows.size}", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) Text(
            when (cfg.range) {
                "today", "today_overdue" -> "오늘 할 일이 없어요"
                "tomorrow" -> "내일 할 일이 없어요"
                "overdue" -> "밀린 할 일이 없어요 👍"
                else -> "조건에 맞는 할 일이 없어요"
            }, color = TextSecondary, modifier = Modifier.padding(vertical = 4.dp))
        else if (doneCount == rows.size && cfg.range in setOf("today", "today_overdue")) Text(
            "오늘 할 일을 다 끝냈어요 🎉", color = Color(0xFF35B36A), fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp))

        var lastGroup: String? = null
        visible.forEach { row ->
            val group = when (cfg.group) {
                "date" -> row.day?.let { dayLabel(it, today) } ?: "날짜 없음"
                "list" -> lists.firstOrNull { it.id == row.task.listId }?.name ?: "기본함"
                "priority" -> when (row.task.priority) { 3 -> "높음"; 2 -> "보통"; 1 -> "낮음"; else -> "없음" }
                else -> null
            }
            if (group != null && group != lastGroup) {
                Text(group, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (lastGroup == null) 0.dp else 10.dp, bottom = 2.dp))
                lastGroup = group
            }
            TaskViewRowItem(row, cfg, lists, today, onToggle, onOpen, showDay = cfg.group != "date")
        }
        if (cfg.limit != 0 && rows.size > cfg.limit) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expanded = !expanded }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = TextSecondary,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "접기" else "${rows.size - cfg.limit}개 더 보기", color = TextSecondary, fontSize = 13.sp)
            }
        }
        if (cfg.inlineAdd) {
            var text by remember { mutableStateOf("") }
            val listForAdd = cfg.listIds.firstOrNull().orEmpty()
            // New tasks land where this block shows them: 내일 → tomorrow, 날짜 없음 → no date, else today.
            val addDate = when (cfg.range) { "tomorrow" -> today.plusDays(1); "nodate" -> null; else -> today }
            fun submit() { if (text.isNotBlank()) { onAdd(text, listForAdd, addDate); text = "" } }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) Text("할 일 추가", color = TextSecondary)
                    BasicTextField(
                        value = text, onValueChange = { text = it }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (text.isNotBlank()) Text("추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { submit() }.padding(8.dp))
            }
        }
    }
}

@Composable
private fun TaskViewRowItem(
    row: TaskViewRow, cfg: TaskViewConfig, lists: List<TaskList>, today: LocalDate,
    onToggle: (Task, LocalDate) -> Unit, onOpen: (Task, LocalDate?) -> Unit, showDay: Boolean
) {
    val t = row.task
    val isDone = done(row)
    val due = t.localDue()
    val overdue = !isDone && row.day != null && row.day.isBefore(today)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOpen(t, row.day) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val pc = priorityColor(t.priority)
        Icon(if (isDone) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, "완료",
            tint = if (isDone) TextSecondary else pc ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).clickable { onToggle(t, row.day ?: today) })
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title.ifBlank { "제목 없음" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isDone) TextSecondary else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (isDone) TextDecoration.LineThrough else null)
            if (cfg.showList) {
                Text(lists.firstOrNull { it.id == t.listId }?.name ?: "기본함", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
        if (pc != null && !isDone) {
            Icon(Icons.Outlined.Flag, null, tint = pc, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        val label = buildString {
            if (cfg.showDate && showDay && row.day != null && !(cfg.range == "today" && row.day == today)) append(dayLabel(row.day, today))
            due?.second?.let { if (isNotEmpty()) append(' '); append("%02d:%02d".format(it.hour, it.minute)) }
        }
        if (label.isNotEmpty()) Text(label, fontSize = 12.sp, color = if (overdue) Color(0xFFE5483F) else TextSecondary, maxLines = 1)
    }
}

// ── 설정 ──

@Composable
private fun SLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun SChips(choices: List<Pair<String, String>>, isOn: (String) -> Boolean, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { (value, label) ->
            val on = isOn(value)
            Text(label, fontSize = 13.sp,
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else DarkSurfaceVariant)
                    .clickable { onPick(value) }.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun SSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun TaskViewSettings(tabId: String, block: DashBlock, lists: List<TaskList>, tags: List<Tag>, tasks: List<Task>) {
    val cfg = TaskViewConfig.decode(block.text, block.type)
    fun save(c: TaskViewConfig) = updateDashBlock(tabId, block.id) { it.copy(text = c.encode()) }
    fun toggle(list: List<String>, v: String) = if (v in list) list - v else list + v

    SLabel("기간")
    SChips(TaskRanges, { it == cfg.range }) { save(cfg.copy(range = it)) }
    SLabel("목록 · 안 고르면 전부")
    SChips(lists.map { it.id to it.name }, { it in cfg.listIds }) { save(cfg.copy(listIds = toggle(cfg.listIds, it))) }
    if (tags.isNotEmpty()) {
        SLabel("태그 · 하나라도 있으면 표시")
        SChips(tags.map { it.id to "#${it.name}" }, { it in cfg.tagIds }) { save(cfg.copy(tagIds = toggle(cfg.tagIds, it))) }
    }
    SLabel("중요도")
    SChips(listOf("0" to "전부", "1" to "낮음 이상", "2" to "보통 이상", "3" to "높음만"), { it == cfg.minPriority.toString() }) {
        save(cfg.copy(minPriority = it.toInt()))
    }
    SLabel("정렬")
    SChips(TaskSorts, { it == cfg.sort }) { save(cfg.copy(sort = it)) }
    SLabel("묶기")
    SChips(TaskGroups, { it == cfg.group }) { save(cfg.copy(group = it)) }
    SLabel("처음에 보일 개수")
    SChips(TaskLimits.map { it.first.toString() to it.second }, { it == cfg.limit.toString() }) { save(cfg.copy(limit = it.toInt())) }
    SSwitch("완료한 할 일도 보기", cfg.showDone) { save(cfg.copy(showDone = it)) }
    SSwitch("날짜 표시", cfg.showDate) { save(cfg.copy(showDate = it)) }
    SSwitch("목록 이름 표시", cfg.showList) { save(cfg.copy(showList = it)) }
    SSwitch("블록 안에서 바로 추가", cfg.inlineAdd) { save(cfg.copy(inlineAdd = it)) }
    val count = taskViewRows(cfg, tasks).size
    Text("지금 조건에 맞는 할 일 ${count}개", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
        modifier = Modifier.padding(top = 12.dp))
}
