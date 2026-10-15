package com.bettertick.ui.screens.calendar

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.ui.theme.PriorityHigh
import com.bettertick.ui.theme.PriorityLow
import com.bettertick.ui.theme.PriorityMedium
import com.bettertick.ui.theme.ThemeState
import java.time.ZoneId
import java.time.ZonedDateTime

/** 캘린더 ⋮ > 옵션 보기 > 색상 > 색상별. */
enum class CalendarColorBy(val key: String, val label: String, val description: String) {
    LIST("list", "목록", "작업은 목록의 색상으로 표시됩니다 (텍스트의 명확성을 보장하기 위해 색상이 약간 조정됩니다)."),
    TAG("tag", "태그", "작업은 태그 색상으로 표시됩니다 (텍스트의 명확성을 위해 색상이 약간 조정됩니다)."),
    PRIORITY("priority", "우선순위", "작업은 우선순위 색상으로 표시됩니다 (텍스트의 명확성을 위해 색상이 약간 조정됩니다).")
}

/** 캘린더 ⋮ > 옵션 보기 > 스타일 — how task chips/blocks are painted. */
enum class CalendarChipStyle(val key: String, val label: String, val description: String) {
    FILLED("filled", "채우기", "색상으로 채운 블록에 흰 글씨"),
    TINTED("tinted", "연한 배경", "연한 색 배경에 색상 글씨"),
    OUTLINED("outlined", "테두리", "왼쪽 색 막대와 연한 배경")
}

/**
 * Calendar view options mirroring TickTick's calendar ⋮ menu
 * (필터 보기 범위 / 옵션 보기). Snapshot state so every calendar composable
 * recomposes on change; persisted to SharedPreferences.
 */
object CalendarPrefs {
    private const val PREFS = "calendar_view_prefs"

    var colorBy by mutableStateOf(CalendarColorBy.LIST)
        private set
    var chipStyle by mutableStateOf(CalendarChipStyle.FILLED)
        private set
    var showDetails by mutableStateOf(true)
        private set
    var showCompleted by mutableStateOf(true)
        private set
    var showCheckItems by mutableStateOf(false)
        private set
    var showRepeatCycles by mutableStateOf(true)
        private set
    var showFocusRecords by mutableStateOf(false)
        private set
    var secondaryTzEnabled by mutableStateOf(false)
        private set
    /** Extra zone ids added in 시간대 설정; the first one is drawn as the
     *  secondary time axis in week / 3-day / day views. */
    var secondaryZones by mutableStateOf<List<String>>(emptyList())
        private set
    /** List ids hidden from the calendar ("" = 기본함). */
    var hiddenListIds by mutableStateOf<Set<String>>(emptySet())
        private set
    /** Empty = every task; otherwise only tasks carrying one of these tags. */
    var tagFilter by mutableStateOf<Set<String>>(emptySet())
        private set
    /** null = follow the theme accent. */
    var inboxColor by mutableStateOf<Int?>(null)
        private set
    var noTagColor by mutableStateOf<Int?>(null)
        private set
    /** Priority (0 none … 3 high) → ARGB; missing = default. */
    var priorityColors by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set
    /** Colors the user added under 컬러 픽 > 사용자 정의. */
    var customColors by mutableStateOf<List<Int>>(emptyList())
        private set

    /** Transient (not persisted): hides the global + button while the
     *  할 일 배정하기 panel occupies the bottom of the calendar. */
    var arrangePanelOpen by mutableStateOf(false)

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        colorBy = CalendarColorBy.entries.firstOrNull { it.key == p.getString("color_by", null) } ?: CalendarColorBy.LIST
        chipStyle = CalendarChipStyle.entries.firstOrNull { it.key == p.getString("chip_style", null) } ?: CalendarChipStyle.FILLED
        showDetails = p.getBoolean("show_details", true)
        showCompleted = p.getBoolean("show_completed", true)
        showCheckItems = p.getBoolean("show_check_items", false)
        showRepeatCycles = p.getBoolean("show_repeat_cycles", true)
        showFocusRecords = p.getBoolean("show_focus_records", false)
        secondaryTzEnabled = p.getBoolean("secondary_tz", false)
        secondaryZones = p.getString("secondary_zones", "").orEmpty().split(",").filter { it.isNotBlank() }
        hiddenListIds = p.getStringSet("hidden_lists", emptySet()).orEmpty().map { if (it == INBOX_KEY) "" else it }.toSet()
        tagFilter = p.getStringSet("tag_filter", emptySet()).orEmpty().toSet()
        inboxColor = if (p.contains("inbox_color")) p.getInt("inbox_color", 0) else null
        noTagColor = if (p.contains("no_tag_color")) p.getInt("no_tag_color", 0) else null
        priorityColors = (0..3).mapNotNull { lvl ->
            if (p.contains("priority_color_$lvl")) lvl to p.getInt("priority_color_$lvl", 0) else null
        }.toMap()
        customColors = p.getString("custom_colors", "").orEmpty().split(",")
            .mapNotNull { it.toLongOrNull()?.toInt() }
    }

    private const val INBOX_KEY = "__inbox__"

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun updateColorBy(v: CalendarColorBy) { colorBy = v; edit { putString("color_by", v.key) } }
    fun updateChipStyle(v: CalendarChipStyle) { chipStyle = v; edit { putString("chip_style", v.key) } }
    fun updateShowDetails(v: Boolean) { showDetails = v; edit { putBoolean("show_details", v) } }
    fun updateShowCompleted(v: Boolean) { showCompleted = v; edit { putBoolean("show_completed", v) } }
    fun updateShowCheckItems(v: Boolean) { showCheckItems = v; edit { putBoolean("show_check_items", v) } }
    fun updateShowRepeatCycles(v: Boolean) { showRepeatCycles = v; edit { putBoolean("show_repeat_cycles", v) } }
    fun updateShowFocusRecords(v: Boolean) { showFocusRecords = v; edit { putBoolean("show_focus_records", v) } }
    fun updateSecondaryTzEnabled(v: Boolean) { secondaryTzEnabled = v; edit { putBoolean("secondary_tz", v) } }

    fun addSecondaryZone(id: String) {
        if (id in secondaryZones) return
        secondaryZones = secondaryZones + id
        edit { putString("secondary_zones", secondaryZones.joinToString(",")) }
    }

    fun removeSecondaryZone(id: String) {
        secondaryZones = secondaryZones - id
        edit { putString("secondary_zones", secondaryZones.joinToString(",")) }
    }

    fun updateFilter(hiddenLists: Set<String>, tags: Set<String>) {
        hiddenListIds = hiddenLists
        tagFilter = tags
        edit {
            putStringSet("hidden_lists", hiddenLists.map { it.ifEmpty { INBOX_KEY } }.toSet())
            putStringSet("tag_filter", tags)
        }
    }

    fun updateInboxColor(c: Color?) {
        inboxColor = c?.toArgb()
        edit { if (c == null) remove("inbox_color") else putInt("inbox_color", c.toArgb()) }
    }

    fun updateNoTagColor(c: Color?) {
        noTagColor = c?.toArgb()
        edit { if (c == null) remove("no_tag_color") else putInt("no_tag_color", c.toArgb()) }
    }

    fun updatePriorityColor(level: Int, c: Color?) {
        priorityColors = if (c == null) priorityColors - level else priorityColors + (level to c.toArgb())
        edit { if (c == null) remove("priority_color_$level") else putInt("priority_color_$level", c.toArgb()) }
    }

    fun addCustomColor(c: Color) {
        val argb = c.toArgb()
        if (argb in customColors) return
        customColors = customColors + argb
        edit { putString("custom_colors", customColors.joinToString(",") { (it.toLong() and 0xFFFFFFFFL).toString() }) }
    }

    // ── Color resolution ────────────────────────────────────────────────

    fun defaultPriorityColor(level: Int): Color = when (level) {
        3 -> PriorityHigh
        2 -> PriorityMedium
        1 -> PriorityLow
        else -> ThemeState.accentColor
    }

    fun priorityColorFor(level: Int): Color =
        priorityColors[level]?.let { Color(it) } ?: defaultPriorityColor(level)

    fun inboxColorValue(): Color = inboxColor?.let { Color(it) } ?: ThemeState.accentColor
    fun noTagColorValue(): Color = noTagColor?.let { Color(it) } ?: ThemeState.accentColor

    /** Base color a task is painted with, according to 색상별. */
    fun colorFor(task: Task, lists: List<TaskList>, tags: List<Tag>): Color = when (colorBy) {
        CalendarColorBy.LIST -> if (task.listId.isBlank()) inboxColorValue()
            else lists.firstOrNull { it.id == task.listId }?.color?.let(::parseColorOrNull) ?: inboxColorValue()
        CalendarColorBy.TAG -> task.tagIds.firstNotNullOfOrNull { id ->
            tags.firstOrNull { it.id == id }?.color?.let(::parseColorOrNull)
        } ?: noTagColorValue()
        CalendarColorBy.PRIORITY -> priorityColorFor(task.priority)
    }

    /** Applies 필터 보기 범위 + 완료된 할일 보기. */
    fun passesFilter(task: Task): Boolean {
        if (!showCompleted && task.isCompleted) return false
        if (task.listId in hiddenListIds) return false
        if (tagFilter.isNotEmpty() && task.tagIds.none { it in tagFilter }) return false
        return true
    }
}

fun parseColorOrNull(hex: String): Color? =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()

fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** Resolves the paint color for a task inside calendar views. */
val LocalTaskColor = compositionLocalOf<(Task) -> Color> { { ThemeState.accentColor } }

data class ChipColors(val background: Color, val content: Color, val bar: Color? = null)

/**
 * Background / text colors for a task chip or timeline block, following the
 * chosen 스타일. Light base colors get dark text so labels stay readable
 * (TickTick: "텍스트의 명확성을 보장하기 위해 색상이 약간 조정됩니다").
 */
fun chipColors(base: Color, completed: Boolean): ChipColors {
    val dark = ThemeState.isDark
    return when (CalendarPrefs.chipStyle) {
        CalendarChipStyle.FILLED -> {
            val bg = if (completed) base.copy(alpha = 0.4f) else base
            val fg = if (base.luminance() > 0.62f && !completed) Color(0xFF191919) else Color.White
            ChipColors(bg, if (completed) fg.copy(alpha = 0.8f) else fg)
        }
        CalendarChipStyle.TINTED -> ChipColors(
            background = base.copy(alpha = if (completed) 0.10f else if (dark) 0.28f else 0.18f),
            content = readableOn(base, dark).copy(alpha = if (completed) 0.55f else 1f)
        )
        CalendarChipStyle.OUTLINED -> ChipColors(
            background = base.copy(alpha = if (completed) 0.06f else 0.12f),
            content = readableOn(base, dark).copy(alpha = if (completed) 0.55f else 1f),
            bar = base.copy(alpha = if (completed) 0.45f else 1f)
        )
    }
}

/** Darkens (light theme) or lightens (dark theme) [c] for text use. */
private fun readableOn(c: Color, dark: Boolean): Color {
    fun mix(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f
    )
    return if (dark) mix(c, Color.White, 0.35f) else mix(c, Color.Black, if (c.luminance() > 0.5f) 0.45f else 0.15f)
}

// ── Color pick palettes (sampled from TickTick 컬러 픽) ─────────────────

data class ColorPalette(val name: String, val colors: List<Color?>)

val TickColorPalettes: List<ColorPalette> = listOf(
    ColorPalette("추천", listOf(null) + listOf(0xFFFF6160, 0xFFFFAC38, 0xFFFED324, 0xFFE5EA48, 0xFF35D770, 0xFF4CA1FE, 0xFFCF72F4).map { Color(it) }),
    ColorPalette("마카롱", listOf(0xFFF68F8A, 0xFFF8C3B3, 0xFFFCDEAA, 0xFFC2DED0, 0xFF63C5D2, 0xFF70AAF3, 0xFFC09EEB, 0xFFF778AD).map { Color(it) }),
    ColorPalette("모란디 색상", listOf(0xFFD27762, 0xFFF6C988, 0xFFDBDE6B, 0xFFA7C9B9, 0xFF7BC9D3, 0xFF86AADE, 0xFFAC94C6, 0xFFC67B9A).map { Color(it) }),
    ColorPalette("로코코", listOf(0xFFBE7471, 0xFFDD882E, 0xFFE3CE7B, 0xFF76A591, 0xFF5DAE9B, 0xFF4682B4, 0xFF838BB0, 0xFFCB8A90).map { Color(it) }),
    ColorPalette("클래식", listOf(0xFFEC6665, 0xFFF1B04A, 0xFFFED966, 0xFFDCE359, 0xFF5DD2A8, 0xFF4AA6EF, 0xFFCF66F7, 0xFFEC70A4).map { Color(it) }),
    ColorPalette("멤피스 색조", listOf(0xFFEF2D24, 0xFFFF7D5D, 0xFFFFD457, 0xFF02DAB1, 0xFF34BAC5, 0xFF655EFA, 0xFFCE89FE, 0xFFF964C2).map { Color(it) }),
)

// ── Time zones ──────────────────────────────────────────────────────────

/** Common zones offered by 시간대 추가, with Korean city labels. */
val TimeZoneChoices: List<Pair<String, String>> = listOf(
    "Asia/Seoul" to "서울", "Asia/Tokyo" to "도쿄", "Asia/Shanghai" to "베이징",
    "Asia/Hong_Kong" to "홍콩", "Asia/Taipei" to "타이베이", "Asia/Singapore" to "싱가포르",
    "Asia/Bangkok" to "방콕", "Asia/Ho_Chi_Minh" to "호찌민", "Asia/Jakarta" to "자카르타",
    "Asia/Kolkata" to "뉴델리", "Asia/Dubai" to "두바이", "Europe/Moscow" to "모스크바",
    "Europe/Istanbul" to "이스탄불", "Europe/Berlin" to "베를린", "Europe/Paris" to "파리",
    "Europe/London" to "런던", "Atlantic/Reykjavik" to "레이캬비크", "America/Sao_Paulo" to "상파울루",
    "America/New_York" to "뉴욕", "America/Toronto" to "토론토", "America/Chicago" to "시카고",
    "America/Denver" to "덴버", "America/Los_Angeles" to "로스앤젤레스", "America/Vancouver" to "밴쿠버",
    "Pacific/Honolulu" to "호놀룰루", "Australia/Sydney" to "시드니", "Pacific/Auckland" to "오클랜드",
    "UTC" to "협정 세계시"
)

fun zoneCityName(id: String): String =
    TimeZoneChoices.firstOrNull { it.first == id }?.second ?: id.substringAfterLast('/').replace('_', ' ')

/** "GMT+9", "GMT-3:30" style label for the zone's current offset. */
fun zoneOffsetLabel(id: String): String {
    val seconds = runCatching { ZonedDateTime.now(ZoneId.of(id)).offset.totalSeconds }.getOrDefault(0)
    if (seconds == 0) return "GMT"
    val sign = if (seconds > 0) "+" else "-"
    val abs = kotlin.math.abs(seconds)
    val h = abs / 3600
    val m = (abs % 3600) / 60
    return if (m == 0) "GMT$sign$h" else "GMT$sign$h:%02d".format(m)
}
