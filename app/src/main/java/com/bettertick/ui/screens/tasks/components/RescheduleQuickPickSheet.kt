package com.bettertick.ui.screens.tasks.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Quick-pick sheet shown when the user taps the "reschedule" action on a
 * task row. Six shortcut actions in a 3×2 grid — the full date picker is
 * reachable through the "날짜 선택" tile.
 *
 * Rendered as a centered [Dialog] (not a bottom sheet) to match the
 * reference: the modal floats in the middle of the screen with a scrim
 * behind it instead of docking to the bottom edge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RescheduleQuickPickSheet(
    onDismiss: () -> Unit,
    onToday: () -> Unit,
    onTomorrow: () -> Unit,
    onNextMonday: () -> Unit,
    onPickDate: () -> Unit,
    onSkipRecurrence: () -> Unit,
    onDelete: () -> Unit,
    onCustomize: () -> Unit = {}
) {
    val today = LocalDate.now()
    val nextMondayShort = twoLetter(DayOfWeek.MONDAY)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                // Long-press anywhere on the card opens the customization
                // page, matching the hint shown at the bottom of the modal.
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onCustomize
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            QuickPickRow(
                listOf(
                    QuickPickItem(
                        label = "오늘",
                        icon = QuickPickIcon.DayNumber(today.dayOfMonth),
                        onClick = onToday
                    ),
                    QuickPickItem(
                        label = "내일",
                        icon = QuickPickIcon.Vector(Icons.Outlined.WbTwilight),
                        onClick = onTomorrow
                    ),
                    QuickPickItem(
                        label = "다음 월요일",
                        icon = QuickPickIcon.DayText(nextMondayShort),
                        onClick = onNextMonday
                    )
                )
            )

            Spacer(Modifier.height(20.dp))

            QuickPickRow(
                listOf(
                    QuickPickItem(
                        label = "날짜 선택",
                        icon = QuickPickIcon.Vector(Icons.Outlined.EditCalendar),
                        onClick = onPickDate
                    ),
                    QuickPickItem(
                        label = "반복 건너뛰기",
                        icon = QuickPickIcon.Vector(Icons.Outlined.SkipNext),
                        iconTinted = false,
                        onClick = onSkipRecurrence
                    ),
                    QuickPickItem(
                        label = "삭제",
                        icon = QuickPickIcon.Vector(Icons.Outlined.Close),
                        onClick = onDelete
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "설정을 사용자 지정하려면 길게 누르십시오",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickPickRow(items: List<QuickPickItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            QuickPickTile(item = item, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickPickTile(
    item: QuickPickItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { item.onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val icon = item.icon) {
                is QuickPickIcon.Vector -> {
                    Icon(
                        imageVector = icon.image,
                        contentDescription = null,
                        tint = if (item.iconTinted) QuickPickAccent else TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                is QuickPickIcon.DayNumber -> {
                    FrameIcon {
                        Text(
                            text = icon.value.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickPickAccent
                        )
                    }
                }
                is QuickPickIcon.DayText -> {
                    FrameIcon {
                        Text(
                            text = icon.value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickPickAccent
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.label,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** The two text-based icons (15, Mo) sit inside a small framed calendar-ish
 *  badge with a colored stripe up top, matching the reference. */
@Composable
private fun FrameIcon(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 28.dp, height = 30.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(1.5.dp, QuickPickAccent, RoundedCornerShape(5.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(QuickPickAccent)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

/** "Mo" / "Tu" / ... — matches the reference where "다음 월요일" shows a
 *  two-letter label inside a calendar frame. */
private fun twoLetter(day: DayOfWeek): String {
    val full = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    return if (full.length >= 2) full.substring(0, 2) else full
}

private data class QuickPickItem(
    val label: String,
    val icon: QuickPickIcon,
    val iconTinted: Boolean = true,
    val onClick: () -> Unit
)

private sealed interface QuickPickIcon {
    data class Vector(val image: ImageVector) : QuickPickIcon
    data class DayNumber(val value: Int) : QuickPickIcon
    data class DayText(val value: String) : QuickPickIcon
}

private val QuickPickAccent = Color(0xFF4A90E2)

/**
 * Full-screen dialog reached via long-press on [RescheduleQuickPickSheet].
 * User taps one of the six preset slots at the top to choose which one to
 * edit; the four tabs below (기한/시간/반복/연기) show the currently-selected
 * option for that slot and persist changes via [SharedPreferences].
 */
@Composable
fun QuickDateSettingsDialog(
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(QUICK_DATE_PREFS, Context.MODE_PRIVATE)
    }

    // Fixed-slot presets: 6 slots; indices 0..2 configurable presets, 3..5
    // are stock actions. Slots 6..8 are user-assignable "custom" slots (see
    // customSlots below).
    val configs = remember {
        mutableStateListOf<PresetConfig>().apply {
            addAll((0 until SLOT_COUNT).map { loadPresetConfig(prefs, it) })
        }
    }
    val customSlots = remember {
        mutableStateListOf<CustomSlotOption?>().apply {
            addAll((0 until CUSTOM_SLOT_COUNT).map { loadCustomSlot(prefs, it) })
        }
    }

    // Default to slot 2 ("다음 월요일") so the reference screenshot's state
    // matches what the user sees when they first open this page.
    var editingSlot by rememberSaveable { mutableStateOf(2) }
    var activeTab by rememberSaveable { mutableStateOf(QuickDateTab.DEADLINE) }
    var advancedMode by rememberSaveable { mutableStateOf(false) }

    val slotLabels = listOf(
        "오늘", "내일", "다음 월요일",
        "날짜 선택", "반복 건너뛰기", "삭제"
    )
    val slotIcons = listOf(
        QuickPickIcon.DayNumber(today.dayOfMonth),
        QuickPickIcon.Vector(Icons.Outlined.WbTwilight),
        QuickPickIcon.DayText(twoLetter(DayOfWeek.MONDAY)),
        QuickPickIcon.Vector(Icons.Outlined.EditCalendar),
        QuickPickIcon.Vector(Icons.Outlined.SkipNext),
        QuickPickIcon.Vector(Icons.Outlined.Close)
    )

    fun updateConfig(transform: (PresetConfig) -> PresetConfig) {
        val next = transform(configs[editingSlot])
        configs[editingSlot] = next
        savePresetConfig(prefs, editingSlot, next)
    }

    // Writes to either the fixed-slot config or the custom-slot assignment,
    // depending on which slot the user is currently editing.
    fun assignOption(option: CustomSlotOption) {
        if (editingSlot < SLOT_COUNT) {
            when (option) {
                is CustomSlotOption.Deadline -> updateConfig { it.copy(deadline = option.opt) }
                is CustomSlotOption.Time -> updateConfig { it.copy(time = option.opt) }
                is CustomSlotOption.Repeat -> updateConfig { it.copy(repeat = option.opt) }
                is CustomSlotOption.Postpone -> updateConfig { it.copy(postpone = option.opt) }
            }
        } else {
            val idx = editingSlot - SLOT_COUNT
            customSlots[idx] = option
            saveCustomSlot(prefs, idx, option)
        }
    }

    fun isOptionSelected(option: CustomSlotOption): Boolean {
        return if (editingSlot < SLOT_COUNT) {
            val c = configs[editingSlot]
            when (option) {
                is CustomSlotOption.Deadline -> c.deadline == option.opt
                is CustomSlotOption.Time -> c.time == option.opt
                is CustomSlotOption.Repeat -> c.repeat == option.opt
                is CustomSlotOption.Postpone -> c.postpone == option.opt
            }
        } else {
            customSlots[editingSlot - SLOT_COUNT] == option
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0B))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onDismiss() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "빠른 날짜 사용자 지정",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (advancedMode) "일반 모드로 전환하기" else "고급 모드로 전환하기",
                color = QuickPickAccent,
                fontSize = 14.sp,
                modifier = Modifier.clickable { advancedMode = !advancedMode }
            )
            Spacer(Modifier.height(16.dp))

            if (advancedMode) {
                AdvancedModeBody()
                return@Column
            }

            // Slots panel — clicking a tile makes it the one being edited.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(16.dp)
            ) {
                val selectSlot: (Int) -> Unit = { slot ->
                    editingSlot = slot
                    // Jump to the tab that best represents this slot so the
                    // bottom panel immediately shows a recognisable selection.
                    activeTab = primaryTabForSlot(slot)
                }
                SettingsSlotRow(
                    range = 0..2,
                    editingSlot = editingSlot,
                    labels = slotLabels,
                    icons = slotIcons,
                    onSelect = selectSlot
                )
                Spacer(Modifier.height(18.dp))
                SettingsSlotRow(
                    range = 3..5,
                    editingSlot = editingSlot,
                    labels = slotLabels,
                    icons = slotIcons,
                    onSelect = selectSlot
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (0 until CUSTOM_SLOT_COUNT).forEach { i ->
                        val globalIdx = SLOT_COUNT + i
                        val option = customSlots[i]
                        CustomSlotTile(
                            option = option,
                            today = today,
                            selected = editingSlot == globalIdx,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editingSlot = globalIdx
                                option?.let { activeTab = it.tab() }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tabs panel — the currently-selected option reflects the
            // config for whichever slot is being edited.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row {
                    QuickDateTab.values().forEach { tab ->
                        Column(
                            modifier = Modifier
                                .padding(end = 20.dp)
                                .clickable { activeTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val active = tab == activeTab
                            Text(
                                text = tab.label,
                                color = if (active) QuickPickAccent else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 22.dp, height = 2.dp)
                                    .background(
                                        if (active) QuickPickAccent else Color.Transparent
                                    )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                when (activeTab) {
                    QuickDateTab.DEADLINE ->
                        DeadlineOption.values().forEach { opt ->
                            val wrapped = CustomSlotOption.Deadline(opt)
                            OptionRow(
                                label = opt.label,
                                selected = isOptionSelected(wrapped),
                                onClick = { assignOption(wrapped) }
                            )
                        }
                    QuickDateTab.TIME -> {
                        TimeOption.values().forEach { opt ->
                            val wrapped = CustomSlotOption.Time(opt)
                            OptionRow(
                                label = opt.label,
                                selected = isOptionSelected(wrapped),
                                onClick = { assignOption(wrapped) }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "자주 쓰는 시간 사용자 설정",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    QuickDateTab.REPEAT ->
                        RepeatOption.values().forEach { opt ->
                            val wrapped = CustomSlotOption.Repeat(opt)
                            OptionRow(
                                label = opt.label,
                                selected = isOptionSelected(wrapped),
                                trailingInfo = opt == RepeatOption.SKIP,
                                onClick = { assignOption(wrapped) }
                            )
                        }
                    QuickDateTab.POSTPONE ->
                        PostponeOption.values().forEach { opt ->
                            val wrapped = CustomSlotOption.Postpone(opt)
                            OptionRow(
                                label = opt.label,
                                selected = isOptionSelected(wrapped),
                                onClick = { assignOption(wrapped) }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SettingsSlotRow(
    range: IntRange,
    editingSlot: Int,
    labels: List<String>,
    icons: List<QuickPickIcon>,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        range.forEach { idx ->
            val selected = idx == editingSlot
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) DarkSurfaceVariant else Color.Transparent
                    )
                    .clickable { onSelect(idx) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val icon = icons[idx]) {
                        is QuickPickIcon.Vector -> Icon(
                            imageVector = icon.image,
                            contentDescription = null,
                            tint = QuickPickAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        is QuickPickIcon.DayNumber -> FrameIcon {
                            Text(
                                text = icon.value.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuickPickAccent
                            )
                        }
                        is QuickPickIcon.DayText -> FrameIcon {
                            Text(
                                text = icon.value,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuickPickAccent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = labels[idx],
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A user-assignable slot tile. When unassigned, shows an empty outlined
 * placeholder; once the user picks any option from the tabs below, that
 * option's icon + label fill the tile.
 */
@Composable
private fun CustomSlotTile(
    option: CustomSlotOption?,
    today: LocalDate,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (option == null) {
        Box(
            modifier = modifier
                .padding(horizontal = 2.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) DarkSurfaceVariant else Color.Transparent
                )
                .border(
                    1.dp,
                    TextSecondary.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
        )
        return
    }

    val icon = option.icon(today)
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) DarkSurfaceVariant else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is QuickPickIcon.Vector -> Icon(
                    imageVector = icon.image,
                    contentDescription = null,
                    tint = QuickPickAccent,
                    modifier = Modifier.size(32.dp)
                )
                is QuickPickIcon.DayNumber -> FrameIcon {
                    Text(
                        text = icon.value.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuickPickAccent
                    )
                }
                is QuickPickIcon.DayText -> FrameIcon {
                    Text(
                        text = icon.value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuickPickAccent
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.label(),
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    trailingInfo: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (selected) QuickPickAccent else TextSecondary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(QuickPickAccent)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailingInfo) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Advanced-mode body — static UI that mirrors the reference screenshot.
 * Chip selection and tab state are purely visual (no behavior wired up).
 */
@Composable
private fun AdvancedModeBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(vertical = 16.dp)
    ) {
        val today = LocalDate.now()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AdvancedIconLabel(QuickPickIcon.DayNumber(today.dayOfMonth), "오늘")
            AdvancedIconLabel(QuickPickIcon.Vector(Icons.Outlined.WbTwilight), "내일")
            AdvancedIconLabel(QuickPickIcon.Vector(Icons.Outlined.Loop), "반복")
            AdvancedIconLabel(QuickPickIcon.Vector(Icons.Outlined.CalendarViewMonth), "날짜 선택")
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    TextSecondary.copy(alpha = 0.25f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            ChipGridRow(
                listOf("종일" to true, "오전 9:00" to false, "오후 1:00" to false, "오후 5:00" to false)
            )
            ChipGridRow(
                listOf("+10분" to false, "+1시" to false, "+3시" to false, "+1일" to false)
            )
            ChipGridRow(
                listOf("-10분" to false, "-1시" to false, "-3시" to false, "-1일" to false)
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row {
            listOf("시간" to true, "미리/지연됨" to false).forEach { (label, active) ->
                Column(
                    modifier = Modifier.padding(end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (active) QuickPickAccent else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 2.dp)
                            .background(if (active) QuickPickAccent else Color.Transparent)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            AdvancedRadio(selected = true)
            Spacer(Modifier.width(14.dp))
            Text("종일", color = Color.White, fontSize = 15.sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            AdvancedRadio(selected = false)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("기한", color = Color.White, fontSize = 15.sp)
                Text(
                    text = "오후 3:00",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AdvancedIconLabel(icon: QuickPickIcon, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is QuickPickIcon.Vector -> Icon(
                    imageVector = icon.image,
                    contentDescription = null,
                    tint = QuickPickAccent,
                    modifier = Modifier.size(30.dp)
                )
                is QuickPickIcon.DayNumber -> FrameIcon {
                    Text(
                        text = icon.value.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuickPickAccent
                    )
                }
                is QuickPickIcon.DayText -> FrameIcon {
                    Text(
                        text = icon.value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuickPickAccent
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ChipGridRow(items: List<Pair<String, Boolean>>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { idx, (label, selected) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) DarkSurfaceVariant else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AdvancedRadio(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                2.dp,
                if (selected) QuickPickAccent else TextSecondary,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(QuickPickAccent)
            )
        }
    }
}

private enum class QuickDateTab(val label: String) {
    DEADLINE("기한"),
    TIME("시간"),
    REPEAT("반복"),
    POSTPONE("연기")
}

private enum class DeadlineOption(val label: String) {
    TODAY("오늘"),
    TOMORROW("내일"),
    NEXT_MONDAY("다음 월요일"),
    THIS_SATURDAY("이번 주 토요일"),
    THIS_SUNDAY("이번 주 일요일"),
    PICK_DATE("날짜 선택"),
    DELETE("삭제")
}

private enum class TimeOption(val label: String, val slotLabel: String) {
    SMART("스마트 시간", "스마트 시간"),
    TODAY_MORNING("오늘 아침 (오전 9:00)", "오늘 아침"),
    TODAY_NOON("오늘 오후 (오후 1:00)", "오늘 오후"),
    TODAY_EVENING("오늘 저녁 (오후 5:00)", "오늘 저녁"),
    TODAY_NIGHT("오늘 밤 (오후 8:00)", "오늘 밤"),
    TOMORROW_MORNING("내일 아침 (오전 9:00)", "내일 아침")
}

private enum class RepeatOption(val label: String) {
    NONE("없음"),
    REPEAT("반복"),
    SKIP("반복 건너뛰기")
}

private enum class PostponeOption(val label: String) {
    NONE("없음"),
    CUSTOM("사용자 정의"),
    MIN_10("Postpone 10 mins"),
    MIN_30("Postpone 30 mins"),
    HOUR_1("Postpone 1 hour"),
    HOUR_2("Postpone 2 hours"),
    HOUR_3("Postpone 3 hours"),
    DAY_1("Postpone 1 day"),
    DAY_2("Postpone 2 days"),
    DAY_3("Postpone 3 days"),
    WEEK_1("Postpone 1 week"),
    MONTH_1("Postpone 1 month")
}

private data class PresetConfig(
    val deadline: DeadlineOption,
    val time: TimeOption,
    val repeat: RepeatOption,
    val postpone: PostponeOption
)

private const val SLOT_COUNT = 6
private const val QUICK_DATE_PREFS = "quick_date_presets"

private fun primaryTabForSlot(slot: Int): QuickDateTab = when (slot) {
    4 -> QuickDateTab.REPEAT
    else -> QuickDateTab.DEADLINE
}

private fun defaultConfigForSlot(slot: Int): PresetConfig = when (slot) {
    0 -> PresetConfig(DeadlineOption.TODAY, TimeOption.SMART, RepeatOption.NONE, PostponeOption.NONE)
    1 -> PresetConfig(DeadlineOption.TOMORROW, TimeOption.SMART, RepeatOption.NONE, PostponeOption.NONE)
    2 -> PresetConfig(DeadlineOption.NEXT_MONDAY, TimeOption.SMART, RepeatOption.NONE, PostponeOption.NONE)
    3 -> PresetConfig(DeadlineOption.PICK_DATE, TimeOption.SMART, RepeatOption.NONE, PostponeOption.NONE)
    4 -> PresetConfig(DeadlineOption.TODAY, TimeOption.SMART, RepeatOption.SKIP, PostponeOption.NONE)
    else -> PresetConfig(DeadlineOption.DELETE, TimeOption.SMART, RepeatOption.NONE, PostponeOption.NONE)
}

private fun loadPresetConfig(prefs: SharedPreferences, slot: Int): PresetConfig {
    val default = defaultConfigForSlot(slot)
    return PresetConfig(
        deadline = prefs.getString("slot_${slot}_deadline", null)
            ?.let { runCatching { DeadlineOption.valueOf(it) }.getOrNull() }
            ?: default.deadline,
        time = prefs.getString("slot_${slot}_time", null)
            ?.let { runCatching { TimeOption.valueOf(it) }.getOrNull() }
            ?: default.time,
        repeat = prefs.getString("slot_${slot}_repeat", null)
            ?.let { runCatching { RepeatOption.valueOf(it) }.getOrNull() }
            ?: default.repeat,
        postpone = prefs.getString("slot_${slot}_postpone", null)
            ?.let { runCatching { PostponeOption.valueOf(it) }.getOrNull() }
            ?: default.postpone
    )
}

private fun savePresetConfig(prefs: SharedPreferences, slot: Int, config: PresetConfig) {
    prefs.edit()
        .putString("slot_${slot}_deadline", config.deadline.name)
        .putString("slot_${slot}_time", config.time.name)
        .putString("slot_${slot}_repeat", config.repeat.name)
        .putString("slot_${slot}_postpone", config.postpone.name)
        .apply()
}

private const val CUSTOM_SLOT_COUNT = 3

/** One assigned option in one of the four category tabs — drives the icon
 *  and label shown on user-customizable slots. */
private sealed interface CustomSlotOption {
    data class Deadline(val opt: DeadlineOption) : CustomSlotOption
    data class Time(val opt: TimeOption) : CustomSlotOption
    data class Repeat(val opt: RepeatOption) : CustomSlotOption
    data class Postpone(val opt: PostponeOption) : CustomSlotOption
}

private fun CustomSlotOption.tab(): QuickDateTab = when (this) {
    is CustomSlotOption.Deadline -> QuickDateTab.DEADLINE
    is CustomSlotOption.Time -> QuickDateTab.TIME
    is CustomSlotOption.Repeat -> QuickDateTab.REPEAT
    is CustomSlotOption.Postpone -> QuickDateTab.POSTPONE
}

private fun CustomSlotOption.label(): String = when (this) {
    is CustomSlotOption.Deadline -> opt.label
    is CustomSlotOption.Time -> opt.slotLabel
    is CustomSlotOption.Repeat -> opt.label
    is CustomSlotOption.Postpone -> opt.label
}

private fun CustomSlotOption.icon(today: LocalDate): QuickPickIcon = when (this) {
    is CustomSlotOption.Deadline -> deadlineIcon(opt, today)
    is CustomSlotOption.Time -> timeIcon(opt)
    is CustomSlotOption.Repeat -> repeatIcon(opt)
    is CustomSlotOption.Postpone -> postponeIcon(opt)
}

private fun deadlineIcon(opt: DeadlineOption, today: LocalDate): QuickPickIcon = when (opt) {
    DeadlineOption.TODAY -> QuickPickIcon.DayNumber(today.dayOfMonth)
    DeadlineOption.TOMORROW -> QuickPickIcon.Vector(Icons.Outlined.WbTwilight)
    DeadlineOption.NEXT_MONDAY -> QuickPickIcon.DayText(twoLetter(DayOfWeek.MONDAY))
    DeadlineOption.THIS_SATURDAY -> QuickPickIcon.DayText(twoLetter(DayOfWeek.SATURDAY))
    DeadlineOption.THIS_SUNDAY -> QuickPickIcon.DayText(twoLetter(DayOfWeek.SUNDAY))
    DeadlineOption.PICK_DATE -> QuickPickIcon.Vector(Icons.Outlined.EditCalendar)
    DeadlineOption.DELETE -> QuickPickIcon.Vector(Icons.Outlined.Close)
}

private fun timeIcon(opt: TimeOption): QuickPickIcon = when (opt) {
    TimeOption.SMART -> QuickPickIcon.Vector(Icons.Outlined.Schedule)
    TimeOption.TODAY_MORNING -> QuickPickIcon.Vector(Icons.Outlined.WbSunny)
    TimeOption.TODAY_NOON -> QuickPickIcon.Vector(Icons.Outlined.LightMode)
    TimeOption.TODAY_EVENING -> QuickPickIcon.Vector(Icons.Outlined.DarkMode)
    TimeOption.TODAY_NIGHT -> QuickPickIcon.Vector(Icons.Outlined.Bedtime)
    TimeOption.TOMORROW_MORNING -> QuickPickIcon.Vector(Icons.Outlined.WbTwilight)
}

private fun repeatIcon(opt: RepeatOption): QuickPickIcon = when (opt) {
    RepeatOption.NONE -> QuickPickIcon.Vector(Icons.Outlined.Close)
    RepeatOption.REPEAT -> QuickPickIcon.Vector(Icons.Outlined.Loop)
    RepeatOption.SKIP -> QuickPickIcon.Vector(Icons.Outlined.SkipNext)
}

private fun postponeIcon(opt: PostponeOption): QuickPickIcon = when (opt) {
    PostponeOption.NONE -> QuickPickIcon.Vector(Icons.Outlined.Close)
    PostponeOption.CUSTOM -> QuickPickIcon.Vector(Icons.Outlined.Tune)
    PostponeOption.MIN_10, PostponeOption.MIN_30 ->
        QuickPickIcon.Vector(Icons.Outlined.Schedule)
    PostponeOption.HOUR_1, PostponeOption.HOUR_2, PostponeOption.HOUR_3 ->
        QuickPickIcon.Vector(Icons.Outlined.AvTimer)
    PostponeOption.DAY_1, PostponeOption.DAY_2, PostponeOption.DAY_3 ->
        QuickPickIcon.Vector(Icons.Outlined.CalendarToday)
    PostponeOption.WEEK_1 -> QuickPickIcon.Vector(Icons.Outlined.DateRange)
    PostponeOption.MONTH_1 -> QuickPickIcon.Vector(Icons.Outlined.CalendarMonth)
}

private fun loadCustomSlot(prefs: SharedPreferences, slot: Int): CustomSlotOption? {
    val kind = prefs.getString("custom_${slot}_kind", null) ?: return null
    val value = prefs.getString("custom_${slot}_value", null) ?: return null
    return runCatching {
        when (kind) {
            "D" -> CustomSlotOption.Deadline(DeadlineOption.valueOf(value))
            "T" -> CustomSlotOption.Time(TimeOption.valueOf(value))
            "R" -> CustomSlotOption.Repeat(RepeatOption.valueOf(value))
            "P" -> CustomSlotOption.Postpone(PostponeOption.valueOf(value))
            else -> null
        }
    }.getOrNull()
}

private fun saveCustomSlot(prefs: SharedPreferences, slot: Int, option: CustomSlotOption?) {
    val editor = prefs.edit()
    if (option == null) {
        editor.remove("custom_${slot}_kind").remove("custom_${slot}_value")
    } else {
        val (kind, value) = when (option) {
            is CustomSlotOption.Deadline -> "D" to option.opt.name
            is CustomSlotOption.Time -> "T" to option.opt.name
            is CustomSlotOption.Repeat -> "R" to option.opt.name
            is CustomSlotOption.Postpone -> "P" to option.opt.name
        }
        editor.putString("custom_${slot}_kind", kind)
            .putString("custom_${slot}_value", value)
    }
    editor.apply()
}
