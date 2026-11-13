package com.bettertick.ui.screens.more

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.calendar.OptionCard
import com.bettertick.ui.screens.calendar.OptionNavRow
import com.bettertick.ui.screens.calendar.OptionPage
import com.bettertick.ui.screens.tasks.LeaveHistoryPeriod
import com.bettertick.ui.screens.tasks.LeavePrefs
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate

private val LeaveKinds = listOf("지휴", "특휴", "연차", "병가")
private val DangerRed = Color(0xFFE5483F)

private fun koDay(d: LocalDate): String =
    "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일 (${"월화수목금토일"[d.dayOfWeek.value - 1]})"

/**
 * 설정 > 일반 > 근무지 · 휴가 기록. Each workplace has its own 휴가 규칙, and
 * every leave record counts toward the workplace whose dates contain it.
 */
@Composable
fun WorkplacesScreen(onBack: () -> Unit) {
    var detailId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }

    detailId?.let { id ->
        BackHandler { detailId = null }
        WorkplaceDetailScreen(id = id, onBack = { detailId = null })
        return
    }

    val currentId = LeavePrefs.currentWorkplace.id
    OptionPage(title = "근무지", onBack = onBack) {
        Text(
            "휴가는 그 날짜가 속한 회사의 규칙으로 계산됩니다. 회사를 옮기면 새 회사를 추가하고, 이전 회사에 퇴사일을 넣으세요.",
            fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        OptionCard {
            LeavePrefs.workplaces.sortedByDescending { it.start ?: LocalDate.MIN }.forEach { wp ->
                OptionNavRow(
                    title = wp.name + if (wp.id == currentId) " · 현재" else "",
                    subtitle = wp.rangeLabel
                ) { detailId = wp.id }
            }
        }
        OptionCard {
            OptionNavRow("근무지 추가") { adding = true }
        }
    }

    if (adding) {
        NameDialog(
            title = "근무지 추가",
            initial = "",
            onDismiss = { adding = false },
            onConfirm = { name ->
                adding = false
                detailId = LeavePrefs.addWorkplace(name, start = null, end = null).id
            }
        )
    }
}

@Composable
private fun WorkplaceDetailScreen(id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val wp = LeavePrefs.workplaces.firstOrNull { it.id == id }
    if (wp == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var renaming by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var leaveType by remember { mutableStateOf<String?>(null) }
    var openPeriod by remember { mutableStateOf<String?>(null) }

    leaveType?.let { type ->
        BackHandler { leaveType = null }
        LeaveTypeSettingsScreen(type = type, onBack = { leaveType = null })
        return
    }

    // Recomputed whenever leave is recounted or the workplaces change.
    val history: List<LeaveHistoryPeriod> = remember(LeavePrefs.usage, LeavePrefs.workplaces, id) {
        LeavePrefs.history(id)
    }

    OptionPage(title = wp.name, onBack = onBack) {
        OptionCard {
            OptionNavRow("이름", value = wp.name) { renaming = true }
            OptionNavRow("근무 시작일", value = wp.start?.toString() ?: "설정 안 함") { pickStart = true }
            OptionNavRow("퇴사일", value = wp.end?.toString() ?: "재직 중") { pickEnd = true }
        }

        SectionLabel("휴가 규칙")
        OptionCard {
            LeaveKinds.forEach { type ->
                OptionNavRow(type, subtitle = LeavePrefs.ruleSummary(wp, type)) {
                    LeavePrefs.selectEditing(wp.id)
                    leaveType = type
                }
            }
        }

        SectionLabel("휴가 기록")
        if (history.isEmpty()) {
            Text(
                "이 회사 기간에 쓴 휴가가 없습니다.",
                fontSize = 13.sp, color = TextTertiary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            OptionCard {
                history.forEach { h ->
                    val key = "${h.type}|${h.label}"
                    val open = openPeriod == key
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { openPeriod = if (open) null else key }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(h.type, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(h.label, fontSize = 12.sp, color = TextTertiary)
                            }
                            Text(
                                "${h.used}일 사용" + (h.allowance?.let { " / ${it}일" } ?: ""),
                                fontSize = 14.sp,
                                color = if (h.allowance != null && h.used > h.allowance) DangerRed else TextSecondary
                            )
                        }
                        if (open) {
                            h.days.forEach { d ->
                                Text(
                                    koDay(d),
                                    fontSize = 13.sp, color = TextSecondary,
                                    modifier = Modifier.padding(start = 36.dp, end = 20.dp, bottom = 6.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        if (LeavePrefs.workplaces.size > 1) {
            OptionCard {
                Text(
                    "근무지 삭제",
                    fontSize = 16.sp, color = DangerRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmDelete = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }

    if (renaming) {
        NameDialog(
            title = "근무지 이름",
            initial = wp.name,
            onDismiss = { renaming = false },
            onConfirm = { LeavePrefs.renameWorkplace(wp.id, it); renaming = false }
        )
    }
    if (pickStart) {
        WorkStartPickerDialog(
            initial = wp.start,
            title = "근무 시작일",
            onDismiss = { pickStart = false },
            onPick = { day ->
                if (wp.end != null && day.isAfter(wp.end)) {
                    Toast.makeText(context, "근무 시작일은 퇴사일보다 앞이어야 합니다", Toast.LENGTH_SHORT).show()
                } else {
                    LeavePrefs.updateWorkplaceDates(wp.id, day, wp.end)
                    pickStart = false
                }
            }
        )
    }
    if (pickEnd) {
        WorkStartPickerDialog(
            initial = wp.end,
            title = "퇴사일",
            clearLabel = "재직 중",
            onClear = { LeavePrefs.updateWorkplaceDates(wp.id, wp.start, null); pickEnd = false },
            onDismiss = { pickEnd = false },
            onPick = { day ->
                if (wp.start != null && day.isBefore(wp.start)) {
                    Toast.makeText(context, "퇴사일은 근무 시작일보다 뒤여야 합니다", Toast.LENGTH_SHORT).show()
                } else {
                    LeavePrefs.updateWorkplaceDates(wp.id, wp.start, day)
                    pickEnd = false
                }
            }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("'${wp.name}'을(를) 삭제할까요?") },
            text = { Text("휴가 기록은 지워지지 않고, 그 기간의 휴가는 남은 근무지 규칙으로 다시 계산됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    LeavePrefs.deleteWorkplace(wp.id)
                    onBack()
                }) { Text("삭제", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 20.dp))
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                singleLine = true,
                placeholder = { Text("회사 이름") }
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
