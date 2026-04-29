package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val CustomRepeatAccent = Color(0xFF4A90E2)

/**
 * Full-screen "사용자 설정" editor for custom recurrence. Two layouts,
 * selected by the top dropdown (반복 유형):
 *
 *  - ByDueDate / ByCompletionDate:
 *      frequency section (wheel: "매 N [일/주/개월/Year]")
 *      if unit == Week, weekday chip row ("매주 X에")
 *
 *  - BySpecificDates:
 *      scrollable month calendar with multi-select
 *      "N일이 선택되었습니다." counter
 *
 * Top bar: X (cancel) · "사용자 설정" title · ✓ (save)
 */
@Composable
fun CustomRepeatDialog(
    initial: RepeatChoice.Custom?,
    referenceDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (RepeatChoice.Custom) -> Unit
) {
    var type by remember {
        mutableStateOf(initial?.type ?: CustomRepeatType.ByDueDate)
    }
    var num by remember { mutableIntStateOf(initial?.frequencyNum ?: 1) }
    var unit by remember {
        mutableStateOf(initial?.frequencyUnit ?: CustomRepeatUnit.Week)
    }
    var selectedDows by remember {
        mutableStateOf(
            initial?.daysOfWeek?.takeIf { it.isNotEmpty() } ?: setOf(referenceDate.dayOfWeek)
        )
    }
    var specificDates by remember {
        mutableStateOf(initial?.specificDates ?: emptySet())
    }
    var showInfo by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onCancel = onDismiss,
                    onConfirm = {
                        onConfirm(
                            RepeatChoice.Custom(
                                type = type,
                                frequencyNum = num,
                                frequencyUnit = unit,
                                daysOfWeek = if (type != CustomRepeatType.BySpecificDates &&
                                    unit == CustomRepeatUnit.Week
                                ) selectedDows else emptySet(),
                                specificDates = if (type == CustomRepeatType.BySpecificDates) {
                                    specificDates
                                } else emptySet()
                            )
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    RepeatTypeRow(
                        type = type,
                        onTypeChange = { type = it },
                        onInfoClick = { showInfo = true }
                    )

                    Spacer(Modifier.height(16.dp))

                    when (type) {
                        CustomRepeatType.ByDueDate,
                        CustomRepeatType.ByCompletionDate -> {
                            FrequencySection(
                                num = num,
                                unit = unit,
                                onNumChange = { num = it },
                                onUnitChange = { unit = it }
                            )
                            if (unit == CustomRepeatUnit.Week) {
                                Spacer(Modifier.height(16.dp))
                                WeekdaysSection(
                                    selected = selectedDows,
                                    onToggle = { dow ->
                                        selectedDows = if (dow in selectedDows) {
                                            (selectedDows - dow).ifEmpty { selectedDows }
                                        } else {
                                            selectedDows + dow
                                        }
                                    }
                                )
                            }
                        }

                        CustomRepeatType.BySpecificDates -> {
                            SpecificDatesSection(
                                referenceDate = referenceDate,
                                selected = specificDates,
                                onToggle = { date ->
                                    specificDates = if (date in specificDates) {
                                        specificDates - date
                                    } else {
                                        specificDates + date
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        RepeatTypeInfoDialog(onDismiss = { showInfo = false })
    }
}

@Composable
private fun TopBar(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel",
                tint = Color.White
            )
        }
        Text(
            text = "사용자 설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onConfirm) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = Color.White
            )
        }
    }
}

/** Card with the "반복 유형" label + dropdown trigger + ? help button. */
@Composable
private fun RepeatTypeRow(
    type: CustomRepeatType,
    onTypeChange: (CustomRepeatType) -> Unit,
    onInfoClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "반복 유형",
            fontSize = 15.sp,
            color = Color.White
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = "반복 유형 도움말",
            tint = TextSecondary,
            modifier = Modifier
                .size(18.dp)
                .clickable { onInfoClick() }
        )
        Spacer(Modifier.weight(1f))

        // Dropdown is anchored to the row; DropdownMenu positions itself
        // below the anchor via Compose's internal PopupPositionProvider.
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = type.label,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                CustomRepeatType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (option == type) CustomRepeatAccent else Color.White
                            )
                        },
                        trailingIcon = {
                            if (option == type) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CustomRepeatAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onTypeChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** "빈도" wheel section: a static "매" label + number wheel (1..99) + unit
 *  wheel (일 / 주 / 개월 / Year). */
@Composable
private fun FrequencySection(
    num: Int,
    unit: CustomRepeatUnit,
    onNumChange: (Int) -> Unit,
    onUnitChange: (CustomRepeatUnit) -> Unit
) {
    val numbers = remember { (1..99).map { it.toString() } }
    val units = remember { CustomRepeatUnit.entries.map { it.label } }

    Text(
        text = "빈도",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Static "매" sits in the centre slot of a fake wheel — matches
            // the reference where it stays put while the two wheels on the
            // right scroll independently.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "매",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            WheelPicker(
                items = numbers,
                selectedIndex = (num - 1).coerceIn(0, numbers.lastIndex),
                onSelectedIndexChange = { onNumChange(it + 1) },
                modifier = Modifier.weight(1f)
            )
            WheelPicker(
                items = units,
                selectedIndex = unit.ordinal,
                onSelectedIndexChange = { onUnitChange(CustomRepeatUnit.entries[it]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekdaysSection(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit
) {
    val order = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    Text(
        text = "매주 " + selected.sortedBy { order.indexOf(it) }
            .joinToString(" ") { dayOfWeekKo(it) } + "에",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = "주",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        // 7 chips wrapped to 4 + 3 to match the reference's two-row layout.
        val row1 = order.take(4)
        val row2 = order.drop(4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1.forEach { dow ->
                DowChip(
                    dow = dow,
                    selected = dow in selected,
                    onClick = { onToggle(dow) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2.forEach { dow ->
                DowChip(
                    dow = dow,
                    selected = dow in selected,
                    onClick = { onToggle(dow) },
                    modifier = Modifier.weight(1f)
                )
            }
            // Spacer weight so row2 (3 items) aligns with row1 (4 items)
            // and the chips stay the same width between rows.
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DowChip(
    dow: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) CustomRepeatAccent else Color.Transparent
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(bg)
                else Modifier.border(1.dp, TextTertiary.copy(alpha = 0.5f), CircleShape)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayOfWeekKo(dow),
            fontSize = 14.sp,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun SpecificDatesSection(
    referenceDate: LocalDate,
    selected: Set<LocalDate>,
    onToggle: (LocalDate) -> Unit
) {
    var visibleMonth by remember(referenceDate) {
        mutableStateOf(YearMonth.from(referenceDate))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${visibleMonth.monthValue}월",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = TextSecondary
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        val firstDay = visibleMonth.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val daysInMonth = visibleMonth.lengthOfMonth()
        val weeks = (startOffset + daysInMonth + 6) / 7

        for (w in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (d in 0..6) {
                    val cellIdx = w * 7 + d
                    val dayNum = cellIdx - startOffset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = visibleMonth.atDay(dayNum)
                            val isSelected = date in selected
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) Modifier.background(CustomRepeatAccent)
                                        else Modifier
                                    )
                                    .clickable { onToggle(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    Text(
        text = "${selected.size}일이 선택되었습니다.",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
