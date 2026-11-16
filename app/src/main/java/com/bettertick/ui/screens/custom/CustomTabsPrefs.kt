package com.bettertick.ui.screens.custom

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject

/** One block on a user-made tab (오늘 할 일, 이번 주 근무, 메모, 바로가기 …). */
data class DashBlock(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val type: String,
    /** Per-type data: 메모 text, 바로가기 target, 목록 id, 디데이 "yyyy-MM-dd|제목". */
    val text: String = "",
    /** The user's own heading; blank = the type's default. */
    val title: String = "",
    /** Half width: two half blocks in a row sit side by side. */
    val half: Boolean = false
)

/** A tab the user built: a name, an icon and blocks in the order they arranged. */
data class CustomTab(
    val id: String,
    val name: String,
    val icon: String = "dashboard",
    val blocks: List<DashBlock> = emptyList()
) {
    val route: String get() = "custom/$id"
}

object DashBlockType {
    const val TODAY = "today"
    const val TODAY_TASKS = "today_tasks"
    const val WEEK_SHIFTS = "week_shifts"
    const val UPCOMING = "upcoming"
    const val MONEY = "money"
    const val MEMO = "memo"
    const val SHORTCUT = "shortcut"
    const val HABITS = "habits"
    const val MINI_CALENDAR = "mini_calendar"
    const val DIARY = "diary"
    const val DDAY = "dday"
    const val LIST_TASKS = "list_tasks"
    const val QUICK_ADD = "quick_add"
    const val STATS = "stats"
    const val VALUE = "value"
    const val TEMPLATE = "template"
    const val MACRO = "macro"

    /** What the "블록 추가" sheet offers, in order. */
    val catalog = listOf(
        VALUE to "값 블록 (변수 연결)",
        TEMPLATE to "텍스트 템플릿",
        MACRO to "매크로 버튼",
        TODAY to "오늘 (날짜·근무)",
        TODAY_TASKS to "할 일 (오늘)",
        QUICK_ADD to "빠른 할 일 추가",
        LIST_TASKS to "할 일 (필터 직접 설정)",
        HABITS to "오늘 습관",
        MINI_CALENDAR to "이번 달 달력",
        WEEK_SHIFTS to "이번 주 근무",
        UPCOMING to "다가오는 일정",
        DDAY to "디데이",
        STATS to "이번 주 달성률",
        DIARY to "오늘 일기",
        MONEY to "이번 달 가계부",
        MEMO to "메모",
        SHORTCUT to "바로가기 버튼"
    )

    /** One-line explanation under each entry in the add sheet. */
    fun hint(type: String) = when (type) {
        VALUE -> "변수 하나를 숫자·막대·원형·칩으로, 조건부 색까지"
        TEMPLATE -> "{변수}가 들어간 문장 — 예: 오늘 {shift.today}, 할 일 {today.left}개"
        MACRO -> "버튼 한 번에 여러 동작: 할 일 추가·완료, 습관 체크, 가계부 기록…"
        TODAY -> "날짜, 남은 할 일 수, 오늘 근무"
        TODAY_TASKS -> "탭하면 상세 열기 · 체크 · 바로 추가, 기간·정렬·묶기 설정"
        QUICK_ADD -> "입력하고 엔터 → 오늘 할 일로 추가"
        LIST_TASKS -> "기간·목록·태그·중요도로 거르고 정렬·묶기 — 나만의 스마트 목록"
        HABITS -> "오늘 할 습관을 탭해서 체크"
        MINI_CALENDAR -> "근무 색과 일정 점이 찍힌 이번 달"
        WEEK_SHIFTS -> "월~일 근무 한눈에"
        UPCOMING -> "2주 안의 일정"
        DDAY -> "날짜까지 남은 날 (D-12)"
        STATS -> "이번 주 할 일 완료 비율"
        DIARY -> "오늘 쓴 일기 미리보기"
        MONEY -> "수입·지출·남은 돈"
        MEMO -> "자유롭게 적는 메모"
        SHORTCUT -> "다른 화면으로 바로 이동"
        else -> ""
    }

    /** Blocks that need a choice before they can show anything. */
    fun needsSetup(type: String) = type == DDAY || type == LIST_TASKS || type == SHORTCUT ||
        type == VALUE || type == TEMPLATE || type == MACRO

    /** Blocks that read well at half width by default. */
    fun halfByDefault(type: String) = type == DDAY || type == STATS || type == SHORTCUT || type == VALUE

    fun label(type: String) = catalog.firstOrNull { it.first == type }?.second ?: type
}

/** Screens a 바로가기 block can open. */
val ShortcutTargets = listOf(
    "tasks" to "할 일",
    "calendar" to "달력",
    "money" to "가계부",
    "diary" to "일기",
    "habits" to "습관",
    "pomodoro" to "집중",
    "eisenhower" to "매트릭스"
)

/** Icons a user tab can wear. */
val CustomTabIcons: List<Pair<String, ImageVector>> = listOf(
    "dashboard" to Icons.Outlined.Dashboard,
    "star" to Icons.Outlined.Star,
    "heart" to Icons.Outlined.Favorite,
    "home" to Icons.Outlined.Home,
    "work" to Icons.Outlined.Work,
    "book" to Icons.Outlined.Book,
    "fitness" to Icons.Outlined.FitnessCenter,
    "sleep" to Icons.Outlined.Bedtime,
    "school" to Icons.Outlined.School,
    "cafe" to Icons.Outlined.LocalCafe,
    "pets" to Icons.Outlined.Pets,
    "flight" to Icons.Outlined.Flight,
    "bolt" to Icons.Outlined.Bolt
)

fun customTabIcon(key: String): ImageVector =
    CustomTabIcons.firstOrNull { it.first == key }?.second ?: Icons.Outlined.Dashboard

/** User-made tabs, kept on this device (like the other screen settings). */
object CustomTabsPrefs {
    private const val PREFS = "custom_tabs"
    const val ID_PREFIX = "custom_"

    var tabs by mutableStateOf<List<CustomTab>>(emptyList())
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tabs", null)
        tabs = raw?.let { runCatching { decode(it) }.getOrNull() } ?: emptyList()
    }

    /** The tab just made from 탭 바 settings: it opens in edit mode once. */
    var justCreated: String? = null

    fun tab(id: String): CustomTab? = tabs.firstOrNull { it.id == id }

    /** Creates a tab from [template]'s blocks; returns it. */
    fun create(name: String, icon: String, template: CustomTabTemplate = CustomTabTemplates.first()): CustomTab {
        val tab = CustomTab(
            id = ID_PREFIX + java.util.UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "내 탭" },
            icon = icon,
            blocks = template.blocks.map { (type, half) -> DashBlock(type = type, half = half) }
        )
        save(tabs + tab)
        justCreated = tab.id
        return tab
    }

    fun update(tab: CustomTab) = save(tabs.map { if (it.id == tab.id) tab else it })

    fun delete(id: String) = save(tabs.filterNot { it.id == id })

    private fun save(list: List<CustomTab>) {
        val before = timeTriggerSignature(tabs)
        tabs = list
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString("tabs", encode(list))?.apply()
        // 정해진 시각 매크로: re-arm alarms only when those triggers changed (not on every memo keystroke).
        if (timeTriggerSignature(list) != before) appContext?.let { MacroEngine.syncAlarms(it) }
    }

    private fun timeTriggerSignature(list: List<CustomTab>): String = list.flatMap { tab ->
        tab.blocks.filter { it.type == DashBlockType.MACRO }.flatMap { b ->
            MacroConfig.decode(b.text).triggers.filter { it.type == TriggerType.TIME }
                .map { "${tab.id}|${b.id}|${it.id}|${it.time}|${it.days}|${it.enabled}" }
        }
    }.joinToString(";")

    private fun encode(list: List<CustomTab>): String = JSONArray().apply {
        list.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id); put("name", t.name); put("icon", t.icon)
                put("blocks", JSONArray().apply {
                    t.blocks.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id); put("type", b.type); put("text", b.text)
                            put("title", b.title); put("half", b.half)
                        })
                    }
                })
            })
        }
    }.toString()

    private fun decode(raw: String): List<CustomTab> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val blocks = o.optJSONArray("blocks") ?: JSONArray()
            CustomTab(
                id = o.getString("id"),
                name = o.optString("name", "내 탭"),
                icon = o.optString("icon", "dashboard"),
                blocks = (0 until blocks.length()).map { j ->
                    val b = blocks.getJSONObject(j)
                    DashBlock(id = b.optString("id"), type = b.optString("type"), text = b.optString("text"),
                        title = b.optString("title"), half = b.optBoolean("half"))
                }
            )
        }
    }
}

/** Starting points offered by "내 탭 만들기". */
data class CustomTabTemplate(val name: String, val icon: String, val description: String,
                             val blocks: List<Pair<String, Boolean>>)

val CustomTabTemplates = listOf(
    CustomTabTemplate("오늘 한눈에", "dashboard", "오늘 · 할 일 · 습관 · 다가오는 일정", listOf(
        DashBlockType.TODAY to false, DashBlockType.TODAY_TASKS to false,
        DashBlockType.HABITS to false, DashBlockType.UPCOMING to false)),
    CustomTabTemplate("근무자용", "work", "오늘 근무 · 이번 주 근무 · 이번 달 달력 · 가계부", listOf(
        DashBlockType.TODAY to false, DashBlockType.WEEK_SHIFTS to false,
        DashBlockType.MINI_CALENDAR to false, DashBlockType.MONEY to false)),
    CustomTabTemplate("집중 모드", "bolt", "빠른 추가 · 할 일 · 달성률 · 집중 바로가기", listOf(
        DashBlockType.QUICK_ADD to false, DashBlockType.TODAY_TASKS to false,
        DashBlockType.STATS to true, DashBlockType.HABITS to true)),
    CustomTabTemplate("기록장", "book", "오늘 일기 · 메모 · 습관", listOf(
        DashBlockType.DIARY to false, DashBlockType.MEMO to false, DashBlockType.HABITS to false)),
    CustomTabTemplate("빈 탭", "star", "블록 없이 직접 채우기", emptyList())
)
