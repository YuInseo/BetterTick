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
    /** 메모 text, or the route of a 바로가기 block. */
    val text: String = ""
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

    /** What the "블록 추가" sheet offers, in order. */
    val catalog = listOf(
        TODAY to "오늘 (날짜·근무)",
        TODAY_TASKS to "오늘 할 일",
        WEEK_SHIFTS to "이번 주 근무",
        UPCOMING to "다가오는 일정",
        MONEY to "이번 달 가계부",
        MEMO to "메모",
        SHORTCUT to "바로가기 버튼"
    )

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

    /** Creates a tab with a few useful blocks to start from; returns it. */
    fun create(name: String, icon: String): CustomTab {
        val tab = CustomTab(
            id = ID_PREFIX + java.util.UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "내 탭" },
            icon = icon,
            blocks = listOf(DashBlock(type = DashBlockType.TODAY), DashBlock(type = DashBlockType.TODAY_TASKS))
        )
        save(tabs + tab)
        justCreated = tab.id
        return tab
    }

    fun update(tab: CustomTab) = save(tabs.map { if (it.id == tab.id) tab else it })

    fun delete(id: String) = save(tabs.filterNot { it.id == id })

    private fun save(list: List<CustomTab>) {
        tabs = list
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString("tabs", encode(list))?.apply()
    }

    private fun encode(list: List<CustomTab>): String = JSONArray().apply {
        list.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id); put("name", t.name); put("icon", t.icon)
                put("blocks", JSONArray().apply {
                    t.blocks.forEach { b -> put(JSONObject().apply { put("id", b.id); put("type", b.type); put("text", b.text) }) }
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
                    DashBlock(id = b.optString("id"), type = b.optString("type"), text = b.optString("text"))
                }
            )
        }
    }
}
