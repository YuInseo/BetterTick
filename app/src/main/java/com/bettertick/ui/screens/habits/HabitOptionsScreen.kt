package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val OptionsBlue = Color(0xFF4A90E2)
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayFormatter = DateTimeFormatter.ofPattern("M월 d일")

private val defaultGroups = listOf("기타", "오전", "오후", "밤")
private val targetDayPresets = listOf(0, 7, 21, 30, 100, 365)  // 0 = 영원히
private val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
private val dayValues = listOf(7, 1, 2, 3, 4, 5, 6)  // Sun=7, Mon=1..Sat=6 (to match ISO + custom)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitOptionsScreen(
    initialDraft: HabitDraft,
    onBack: () -> Unit,
    onSave: (HabitDraft) -> Unit
) {
    var frequency by remember { mutableStateOf(initialDraft.frequency) }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(initialDraft.targetDays) } }
    var weeklyCount by remember { mutableIntStateOf(initialDraft.weeklyCount) }
    var intervalDays by remember { mutableIntStateOf(initialDraft.intervalDays) }
    var goalType by remember { mutableStateOf(initialDraft.goalType) }
    var startDate by remember {
        mutableStateOf(
            initialDraft.startDate.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }
                ?: LocalDate.now()
        )
    }
    var targetDayCount by remember { mutableIntStateOf(initialDraft.targetDayCount) }
    val groups = remember { mutableStateListOf<String>().apply { addAll(defaultGroups) } }
    var selectedGroup by remember { mutableStateOf(initialDraft.group) }
    val reminders = remember { mutableStateListOf<String>().apply { addAll(initialDraft.reminders) } }
    var autoShowLog by remember { mutableStateOf(initialDraft.autoShowLog) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var showTargetDaysDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "새로운 습관",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // --- 빈도 card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("빈도", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))

                // Frequency tabs
                Row {
                    listOf("daily" to "매일", "weekly" to "주간", "interval" to "반복").forEach { (key, label) ->
                        val selected = frequency == key
                        Column(
                            modifier = Modifier
                                .clickable { frequency = key }
                                .padding(end = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                color = if (selected) OptionsBlue else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(if (selected) OptionsBlue else Color.Transparent)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (frequency) {
                    "daily" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayLabels.forEachIndexed { idx, label ->
                                val dayValue = dayValues[idx]
                                val isSelected = selectedDays.contains(dayValue)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) OptionsBlue
                                            else DarkSurfaceVariant
                                        )
                                        .clickable {
                                            if (isSelected) selectedDays.remove(dayValue)
                                            else selectedDays.add(dayValue)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    "weekly" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("매주", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                            Spacer(Modifier.width(12.dp))
                            Stepper(weeklyCount, 1, 7) { weeklyCount = it }
                            Spacer(Modifier.width(12.dp))
                            Text("회", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                        }
                    }
                    "interval" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("매", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                            Spacer(Modifier.width(12.dp))
                            Stepper(intervalDays, 2, 365) { intervalDays = it }
                            Spacer(Modifier.width(12.dp))
                            Text("일마다", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                        }
                    }
                }
            }

            // --- 목표 card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
            ) {
                OptionRow(
                    label = "목표",
                    value = if (goalType == "complete_all") "모두 달성" else "일정액에 도달하다",
                    onClick = { showGoalDialog = true }
                )
                OptionRow(
                    label = "시작 날짜",
                    value = startDate.format(displayFormatter),
                    onClick = { showDatePicker = true }
                )
                OptionRow(
                    label = "목표 일수",
                    value = if (targetDayCount == 0) "영원히" else "${targetDayCount}일",
                    onClick = { showTargetDaysDialog = true },
                    showInfo = true
                )
            }

            // --- 소속 그룹 card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "소속 그룹",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showAddGroupDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "그룹 추가", tint = OptionsBlue)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        val isSelected = group == selectedGroup
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) OptionsBlue else DarkSurfaceVariant)
                                .clickable { selectedGroup = group }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                group,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // --- 알림 card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("알림", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                reminders.forEachIndexed { idx, time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(time, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { reminders.removeAt(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .clickable { showTimePicker = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OptionsBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("추가", color = OptionsBlue)
                }
            }

            // --- 자동 표시 toggle ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "습관 로그 자동 표시",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoShowLog,
                    onCheckedChange = { autoShowLog = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OptionsBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF555555)
                    )
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        // Save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    onSave(
                        initialDraft.copy(
                            frequency = frequency,
                            targetDays = selectedDays.toList().sorted(),
                            weeklyCount = weeklyCount,
                            intervalDays = intervalDays,
                            goalType = goalType,
                            startDate = startDate.format(dateFormatter),
                            targetDayCount = targetDayCount,
                            group = selectedGroup,
                            reminders = reminders.toList(),
                            autoShowLog = autoShowLog
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OptionsBlue)
            ) {
                Text("저장", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }

    // --- Dialogs ---

    if (showGoalDialog) {
        RadioSelectDialog(
            title = "목표",
            options = listOf("complete_all" to "모두 달성", "reach_amount" to "일정액에 도달하다"),
            selectedKey = goalType,
            onConfirm = { goalType = it; showGoalDialog = false },
            onDismiss = { showGoalDialog = false }
        )
    }

    if (showTargetDaysDialog) {
        val options = targetDayPresets.map {
            it.toString() to if (it == 0) "영원히" else "$it days"
        } + listOf("custom" to "사용자 정의")
        RadioSelectDialog(
            title = "목표 일수",
            options = options,
            selectedKey = if (targetDayPresets.contains(targetDayCount)) targetDayCount.toString() else "custom",
            onConfirm = { key ->
                showTargetDaysDialog = false
                if (key == "custom") {
                    showCustomTargetDialog = true
                } else {
                    targetDayCount = key.toIntOrNull() ?: 0
                }
            },
            onDismiss = { showTargetDaysDialog = false }
        )
    }

    if (showCustomTargetDialog) {
        var customText by remember { mutableStateOf(targetDayCount.takeIf { it > 0 }?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text("사용자 정의", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.filter { c -> c.isDigit() }.take(4) },
                    placeholder = { Text("일수 입력") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OptionsBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = OptionsBlue,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    targetDayCount = customText.toIntOrNull() ?: 0
                    showCustomTargetDialog = false
                }) { Text("확인", color = OptionsBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        startDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("확인", color = OptionsBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = DarkCard)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddGroupDialog) {
        var newGroup by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("새로운 그룹", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = newGroup,
                    onValueChange = { newGroup = it },
                    placeholder = { Text("그룹 이름") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OptionsBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = OptionsBlue,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newGroup.trim()
                    if (trimmed.isNotBlank() && !groups.contains(trimmed)) {
                        groups.add(trimmed)
                        selectedGroup = trimmed
                    }
                    showAddGroupDialog = false
                }) { Text("추가", color = OptionsBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("알림 시간", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour.toString().padStart(2, '0')
                    val m = timePickerState.minute.toString().padStart(2, '0')
                    val time = "$h:$m"
                    if (!reminders.contains(time)) reminders.add(time)
                    showTimePicker = false
                }) { Text("추가", color = OptionsBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showInfo: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(end = 6.dp))
        if (showInfo) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(value, color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(">", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .clickable { if (value > min) onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(
            value.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .clickable { if (value < max) onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun RadioSelectDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempSelected = key }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempSelected == key,
                            onClick = { tempSelected = key },
                            colors = RadioButtonDefaults.colors(selectedColor = OptionsBlue)
                        )
                        Text(label, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelected) }) {
                Text("확인", color = OptionsBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}
