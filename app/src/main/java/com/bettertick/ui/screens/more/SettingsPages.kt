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
    }
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
fun GeneralSettingsScreen(onBack: () -> Unit) {
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
