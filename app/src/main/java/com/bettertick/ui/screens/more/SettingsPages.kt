package com.bettertick.ui.screens.more

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.calendar.OptionCard
import com.bettertick.ui.screens.calendar.OptionNavRow
import com.bettertick.ui.screens.calendar.OptionPage
import com.bettertick.ui.screens.calendar.OptionSwitchRow
import com.bettertick.ui.screens.calendar.zoneCityName
import com.bettertick.ui.screens.calendar.zoneOffsetLabel
import com.bettertick.ui.screens.tasks.SmartList
import com.bettertick.ui.screens.tasks.SmartListPrefs
import com.bettertick.ui.screens.tasks.SmartVisibility
import com.bettertick.ui.screens.tasks.SwipeAction
import com.bettertick.ui.screens.tasks.LeavePeriod
import com.bettertick.ui.screens.tasks.LeavePrefs
import com.bettertick.ui.screens.tasks.TaskListPrefs
import com.bettertick.ui.theme.AppPrefs
import com.bettertick.ui.theme.DefaultDueDate
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import com.bettertick.ui.theme.TimeFormat
import java.time.DayOfWeek
import java.time.ZoneId

/** Row showing a value that opens a dropdown of choices. */
@Composable
fun <T> OptionChoiceRow(
    title: String,
    subtitle: String? = null,
    value: T,
    options: List<T>,
    label: (T) -> String,
    onPick: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = TextPrimary)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = TextTertiary)
                }
            }
            Text(label(value), fontSize = 14.sp, color = TextSecondary)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.background(GroupedCard)
            ) {
                options.forEach { o ->
                    val sel = o == value
                    DropdownMenuItem(
                        text = { Text(label(o), color = if (sel) ThemeState.accentColor else TextPrimary) },
                        trailingIcon = { if (sel) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor) },
                        onClick = { onPick(o); open = false }
                    )
                }
            }
        }
    }
}

// ── 설정 > 스마트 목록 ─────────────────────────────────────────────────────

@Composable
fun SmartListSettingsScreen(onBack: () -> Unit) {
    OptionPage(title = "스마트 목록", onBack = onBack) {
        OptionCard {
            SmartList.entries.forEach { s ->
                OptionChoiceRow(
                    title = s.label,
                    value = SmartListPrefs.visibility[s] ?: SmartVisibility.SHOW,
                    options = SmartVisibility.entries,
                    label = { it.label },
                    onPick = { SmartListPrefs.update(s, it) }
                )
            }
        }
        Text(
            "왼쪽 메뉴에 표시할 스마트 목록을 고릅니다. '비어 있지 않으면 표시'는 할일이 있을 때만 보입니다.",
            fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

// ── 설정 > 날짜 & 시간 ────────────────────────────────────────────────────

@Composable
fun DateTimeSettingsScreen(onBack: () -> Unit, onOpenTimeZone: () -> Unit) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault().id }
    OptionPage(title = "날짜 & 시간", onBack = onBack) {
        OptionCard {
            OptionChoiceRow(
                title = "주 시작 요일",
                value = AppPrefs.weekStart,
                options = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY),
                label = {
                    when (it) {
                        DayOfWeek.MONDAY -> "월요일"
                        DayOfWeek.SATURDAY -> "토요일"
                        else -> "일요일"
                    }
                },
                onPick = AppPrefs::updateWeekStart
            )
            OptionChoiceRow(
                title = "시간 형식",
                value = AppPrefs.timeFormat,
                options = TimeFormat.entries,
                label = { it.label },
                onPick = AppPrefs::updateTimeFormat
            )
        }
        OptionCard {
            OptionNavRow("시간대", value = "${zoneCityName(zone)}, ${zoneOffsetLabel(zone)}", onClick = onOpenTimeZone)
        }
        OptionCard {
            OptionChoiceRow(
                title = "기본 알림",
                subtitle = "시간이 있는 새 할일에 자동으로 설정됩니다.",
                value = AppPrefs.defaultReminder,
                options = listOf(-1, 0, 5, 15, 30, 60, 1440),
                label = ::reminderOffsetLabel,
                onPick = AppPrefs::updateDefaultReminder
            )
            OptionSwitchRow(
                title = "종일 할일 알림",
                subtitle = "날짜만 있는 새 할일도 당일에 알려줍니다.",
                checked = AppPrefs.allDayReminder,
                onCheckedChange = AppPrefs::updateAllDayReminder
            )
            OptionChoiceRow(
                title = "종일 할일 알림 시간",
                value = AppPrefs.allDayReminderMinutes,
                options = (5..22).flatMap { listOf(it * 60, it * 60 + 30) },
                label = { m -> AppPrefs.formatTime(java.time.LocalTime.of(m / 60, m % 60)) },
                onPick = { m ->
                    AppPrefs.updateAllDayReminderMinutes(m)
                    // Re-arm existing all-day reminders at the new time.
                    if (com.bettertick.widget.WidgetServiceLocator.isInitialized) runCatching {
                        com.bettertick.reminder.ReminderScheduler.syncAll(
                            context, com.bettertick.widget.WidgetServiceLocator.tasksCache.value
                        )
                    }
                }
            )
        }
    }
}

private fun reminderOffsetLabel(m: Int): String = when {
    m < 0 -> "없음"
    m == 0 -> "정시"
    m % 1440 == 0 -> "${m / 1440}일 전"
    m % 60 == 0 -> "${m / 60}시간 전"
    else -> "${m}분 전"
}

// ── 설정 > 사운드 & 알림 ──────────────────────────────────────────────────

@Composable
fun SoundSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var ringtoneName by remember { mutableStateOf(ringtoneTitle(context, AppPrefs.reminderRingtone)) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        @Suppress("DEPRECATION")
        val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (result.data != null) {
            AppPrefs.updateReminderRingtone(uri?.toString())
            ringtoneName = ringtoneTitle(context, uri?.toString())
        }
    }
    OptionPage(title = "사운드 & 알림", onBack = onBack) {
        OptionCard {
            OptionNavRow("알림 권한", value = "시스템 설정") {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
            OptionNavRow("알림 벨소리", value = ringtoneName) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알림 벨소리")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    AppPrefs.reminderRingtone?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                }
                runCatching { picker.launch(intent) }
            }
        }
        OptionCard {
            OptionSwitchRow(
                title = "완료 효과음",
                subtitle = "할일을 완료하면 짧은 소리를 냅니다.",
                checked = AppPrefs.completionSound,
                onCheckedChange = AppPrefs::updateCompletionSound
            )
            OptionSwitchRow(
                title = "완료 시 진동",
                checked = AppPrefs.completionVibrate,
                onCheckedChange = AppPrefs::updateCompletionVibrate
            )
        }
        OptionCard {
            OptionSwitchRow(
                title = "매일 알림",
                subtitle = "매일 정해진 시간에 오늘의 할일을 알려줍니다.",
                checked = AppPrefs.dailySummary,
                onCheckedChange = {
                    AppPrefs.updateDailySummary(it)
                    com.bettertick.reminder.ReminderScheduler.scheduleDailySummary(context)
                }
            )
            if (AppPrefs.dailySummary) {
                OptionChoiceRow(
                    title = "알림 시간",
                    value = AppPrefs.dailySummaryMinutes,
                    options = (5..22).map { it * 60 },
                    label = { m -> AppPrefs.formatTime(java.time.LocalTime.of(m / 60, m % 60)) },
                    onPick = {
                        AppPrefs.updateDailySummaryMinutes(it)
                        com.bettertick.reminder.ReminderScheduler.scheduleDailySummary(context)
                    }
                )
            }
        }
    }
}

private fun ringtoneTitle(context: android.content.Context, uri: String?): String {
    if (uri == null) return "기본 알림음"
    if (uri.isEmpty()) return "무음"
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
        .getOrNull() ?: "사용자 지정"
}

// ── 설정 > 일반 ──────────────────────────────────────────────────────────

@Composable
fun GeneralSettingsScreen(onBack: () -> Unit, onExportTasks: (() -> Unit)? = null) {
    var leaveType by remember { mutableStateOf<String?>(null) }
    leaveType?.let { type ->
        androidx.activity.compose.BackHandler { leaveType = null }
        LeaveTypeSettingsScreen(type = type, onBack = { leaveType = null })
        return
    }
    val tabs = listOf(
        "tasks" to "할일", "calendar" to "캘린더", "matrix" to "아이젠하워 매트릭스",
        "focus" to "뽀모도로", "habits" to "습관"
    )
    OptionPage(title = "일반", onBack = onBack) {
        OptionCard {
            OptionChoiceRow(
                title = "시작 화면",
                subtitle = "앱을 열 때 처음 보여줄 탭",
                value = AppPrefs.startTab,
                options = tabs.map { it.first },
                label = { key -> tabs.firstOrNull { it.first == key }?.second ?: key },
                onPick = AppPrefs::updateStartTab
            )
        }
        OptionCard {
            OptionChoiceRow(
                title = "새 할일의 기본 날짜",
                value = AppPrefs.defaultDueDate,
                options = DefaultDueDate.entries,
                label = { it.label },
                onPick = AppPrefs::updateDefaultDueDate
            )
            OptionSwitchRow(
                title = "새 할일을 맨 위에 추가",
                subtitle = "사용자 지정 정렬에서 새 할일을 목록 맨 위에 둡니다.",
                checked = AppPrefs.newTaskOnTop,
                onCheckedChange = AppPrefs::updateNewTaskOnTop
            )
        }
        Text(
            "휴가 일수",
            fontSize = 13.sp, color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp)
        )
        OptionCard {
            val today = java.time.LocalDate.now()
            listOf("지휴", "연차", "병가").forEach { type ->
                LeaveSummaryRow(type = type, today = today, onClick = { leaveType = type })
            }
        }
        Text(
            "근무(주/야/비/휴) 할일의 병가·지휴·연차 버튼에 남은 일수가 표시됩니다. 항목을 눌러 주기와 일수, 연도별 일수를 정하세요.",
            fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            "스와이프 동작",
            fontSize = 13.sp, color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp)
        )
        OptionCard {
            TaskListPrefs.swipeRight.forEachIndexed { i, a ->
                OptionChoiceRow(
                    title = "오른쪽으로 스와이프 ${i + 1}",
                    subtitle = if (i == 0) "할일을 오른쪽으로 밀면 왼쪽에 나타나는 버튼" else null,
                    value = a,
                    options = SwipeAction.entries,
                    label = { it.label },
                    onPick = { TaskListPrefs.updateSwipeRight(i, it) }
                )
            }
            TaskListPrefs.swipeLeft.forEachIndexed { i, a ->
                OptionChoiceRow(
                    title = "왼쪽으로 스와이프 ${i + 1}",
                    subtitle = if (i == 0) "할일을 왼쪽으로 밀면 오른쪽에 나타나는 버튼" else null,
                    value = a,
                    options = SwipeAction.entries,
                    label = { it.label },
                    onPick = { TaskListPrefs.updateSwipeLeft(i, it) }
                )
            }
        }
        if (onExportTasks != null) {
            OptionCard {
                OptionNavRow("할일 내보내기 (CSV)", value = "공유", onClick = onExportTasks)
            }
        }
        OptionCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("언어", fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("한국어 (시스템)", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}


// ── 설정 > 일반 > 휴가 일수 ────────────────────────────────────────────────

private fun daysLabel(type: String, days: Int): String {
    if (days <= 0) return "설정 안 함"
    return when (LeavePrefs.periodOf(type)) {
        LeavePeriod.MONTH -> "월 ${days}일"
        LeavePeriod.YEAR -> "연 ${days}일"
        LeavePeriod.NONE -> "총 ${days}일"
    }
}

@Composable
private fun LeaveSummaryRow(type: String, today: java.time.LocalDate, onClick: () -> Unit) {
    val q = LeavePrefs.quota(type, today)
    val used = LeavePrefs.used(type, today)
    val custom = LeavePrefs.periodOf(type) != LeavePeriod.NONE && LeavePrefs.yearQuota(type, today.year) != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(type, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                (if (LeavePrefs.isAuto(type)) "빨간날 근무 자동 · " else "") +
                    "${LeavePrefs.periodOf(type).label} · ${LeavePrefs.periodName(type)} ${used}일 사용" +
                    (if (q > 0 || LeavePrefs.isAuto(type)) " · ${q - used}일 남음" else "") +
                    (if (custom) " · ${today.year}년 별도 설정" else ""),
                fontSize = 12.sp, color = TextTertiary
            )
        }
        Text(daysLabel(type, q), fontSize = 14.sp, color = TextSecondary)
        Icon(Icons.Default.KeyboardArrowDown, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
    }
}

/** One leave kind: 주기, 기본 일수 and optional 연도별 일수. */
@Composable
fun LeaveTypeSettingsScreen(type: String, onBack: () -> Unit) {
    val today = remember { java.time.LocalDate.now() }
    var editBase by remember { mutableStateOf(false) }
    var editYear by remember { mutableStateOf<Int?>(null) }
    var pickRedDay by remember { mutableStateOf(false) }
    var showEarned by remember { mutableStateOf(false) }
    val period = LeavePrefs.periodOf(type)
    val base = LeavePrefs.baseQuota(type)
    val auto = LeavePrefs.isAuto(type)
    // In auto mode the user's own number is added on top of what was earned.
    val ownLabel = when {
        auto -> "추가 일수"
        period == LeavePeriod.NONE -> "총 일수"
        else -> "기본 일수"
    }

    OptionPage(title = type, onBack = onBack) {
        if (type == "지휴") {
            OptionCard {
                OptionSwitchRow(
                    title = "빨간날 근무로 자동 계산",
                    subtitle = "한국 달력의 공휴일·대체공휴일에 주간/야간/당직 근무를 하면 그날마다 지휴 1일이 생깁니다. 그날 휴가를 쓰면 세지 않습니다.",
                    checked = LeavePrefs.autoJihyu,
                    onCheckedChange = { LeavePrefs.updateAutoJihyu(it) }
                )
                if (auto) {
                    OptionSwitchRow(
                        title = "일요일도 빨간날로",
                        subtitle = "끄면 공휴일 근무만 셉니다.",
                        checked = LeavePrefs.includeSunday,
                        onCheckedChange = { LeavePrefs.updateIncludeSunday(it) }
                    )
                    val earnedNow = LeavePrefs.earnedDaysIn(today)
                    OptionNavRow(
                        "${LeavePrefs.periodName(type)} 빨간날 근무",
                        value = "${earnedNow.size}일"
                    ) { showEarned = !showEarned }
                    if (showEarned) {
                        if (earnedNow.isEmpty()) {
                            Text(
                                "아직 빨간날 근무가 없습니다.",
                                fontSize = 13.sp, color = TextTertiary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        earnedNow.forEach { d ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${d.monthValue}월 ${d.dayOfMonth}일 (${koDow(d.dayOfWeek)})",
                                    fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f)
                                )
                                Text(
                                    (LeavePrefs.redDayName(d) ?: "") + if (d.isAfter(today)) " · 예정" else "",
                                    fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFE5483F)
                                )
                            }
                        }
                    }
                    OptionNavRow(
                        "빨간날 직접 지정",
                        value = LeavePrefs.redOverrides.size.takeIf { it > 0 }?.let { "${it}일" }
                    ) { pickRedDay = true }
                }
            }
            if (auto && LeavePrefs.redOverrides.isNotEmpty()) {
                OptionCard {
                    LeavePrefs.redOverrides.entries.sortedBy { it.key }.forEach { (d, red) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일 (${koDow(d.dayOfWeek)})",
                                fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (red) "빨간날로" else "빨간날 아님",
                                fontSize = 13.sp,
                                color = if (red) androidx.compose.ui.graphics.Color(0xFFE5483F) else TextSecondary
                            )
                            androidx.compose.material3.TextButton(onClick = { LeavePrefs.setRedOverride(d, null) }) {
                                Text("되돌리기", color = ThemeState.accentColor, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        OptionCard {
            OptionChoiceRow(
                title = "주기",
                subtitle = when (period) {
                    LeavePeriod.MONTH -> "매달 1일에 다시 채워집니다."
                    LeavePeriod.YEAR -> "매년 1월 1일에 다시 채워집니다."
                    LeavePeriod.NONE -> "다시 채워지지 않고 전체 사용 일수로 셉니다."
                },
                value = period,
                options = LeavePeriod.entries,
                label = { it.label },
                onPick = { LeavePrefs.updatePeriod(type, it) }
            )
            OptionNavRow(ownLabel, value = daysLabel(type, base)) { editBase = true }
        }

        val q = LeavePrefs.quota(type, today)
        val used = LeavePrefs.used(type, today)
        Text(
            (if (auto) "빨간날 근무 ${LeavePrefs.earnedIn(today)}일" +
                (if (q - LeavePrefs.earnedIn(today) > 0) " + 추가 ${q - LeavePrefs.earnedIn(today)}일" else "") + " · " else "") +
                "${LeavePrefs.periodName(type)} ${used}일 사용" +
                (if (q > 0 || auto) " · ${q}일 중 ${q - used}일 남음" else " · 일수를 정하면 남은 일수가 표시됩니다."),
            fontSize = 13.sp, color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (period != LeavePeriod.NONE) {
            Text(
                "연도별 일수",
                fontSize = 13.sp, color = TextSecondary,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp)
            )
            OptionCard {
                val set = LeavePrefs.yearQuotas[type].orEmpty().keys
                val years = ((today.year - 1)..(today.year + 3)).toSet() + set
                years.sorted().forEach { y ->
                    val v = LeavePrefs.yearQuota(type, y)
                    OptionNavRow(
                        "${y}년" + if (y == today.year) " (올해)" else "",
                        value = v?.let { daysLabel(type, it) } ?: "기본값 · ${daysLabel(type, base)}"
                    ) { editYear = y }
                }
            }
            Text(
                "해마다 일수가 다르면 그 해만 따로 입력하세요. 비워 두면 기본 일수를 씁니다." +
                    if (period == LeavePeriod.MONTH) " (매달 주기는 그 해의 달마다 적용됩니다.)" else "",
                fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    if (editBase) {
        LeaveDaysDialog(
            title = "$type $ownLabel",
            initial = base,
            clearLabel = null,
            onDismiss = { editBase = false },
            onConfirm = { LeavePrefs.updateQuota(type, it ?: 0); editBase = false }
        )
    }
    if (pickRedDay) {
        RedDayPickerDialog(
            onDismiss = { pickRedDay = false },
            onPick = { d ->
                LeavePrefs.setRedOverride(d, !LeavePrefs.isRedDay(d))
                pickRedDay = false
            }
        )
    }
    editYear?.let { y ->
        LeaveDaysDialog(
            title = "${y}년 $type 일수",
            initial = LeavePrefs.yearQuota(type, y),
            clearLabel = "기본값 사용",
            onDismiss = { editYear = null },
            onConfirm = { LeavePrefs.updateYearQuota(type, y, it); editYear = null }
        )
    }
}

private fun koDow(d: DayOfWeek): String = "월화수목금토일"[d.value - 1].toString()

/** Pick a day to flip between 빨간날 and not — for holidays the table misses. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RedDayPickerDialog(onDismiss: () -> Unit, onPick: (java.time.LocalDate) -> Unit) {
    val state = androidx.compose.material3.rememberDatePickerState()
    val picked = state.selectedDateMillis?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    }
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = picked != null,
                onClick = { picked?.let(onPick) }
            ) {
                Text(
                    when {
                        picked == null -> "날짜 선택"
                        LeavePrefs.isRedDay(picked) -> "빨간날 아님으로"
                        else -> "빨간날로 지정"
                    },
                    color = ThemeState.accentColor
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
        },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = GroupedCard)
    ) {
        androidx.compose.material3.DatePicker(
            state = state,
            title = {
                Text(
                    "회사 휴일·새로 생긴 공휴일 등을 지정하세요",
                    fontSize = 13.sp, color = TextSecondary,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}

/** Number entry (0–365). [clearLabel] adds a button that answers null. */
@Composable
private fun LeaveDaysDialog(
    title: String,
    initial: Int?,
    clearLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var text by remember { mutableStateOf(initial?.takeIf { it > 0 }?.toString() ?: "") }
    val value = text.toIntOrNull()
    val valid = text.isEmpty() || (value != null && value in 0..365)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    suffix = { Text("일") },
                    placeholder = { Text("0 = 설정 안 함") },
                    isError = !valid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                if (clearLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.material3.TextButton(onClick = { onConfirm(null) }) {
                        Text(clearLabel, color = ThemeState.accentColor)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = valid,
                onClick = { onConfirm(value ?: 0) }
            ) { Text("저장", color = ThemeState.accentColor) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
        },
        containerColor = GroupedCard
    )
}
