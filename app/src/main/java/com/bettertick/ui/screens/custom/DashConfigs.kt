package com.bettertick.ui.screens.custom

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** Colours a value / macro block can take. */
val DashColors: List<Pair<String, Color>> = listOf(
    "accent" to Color(0xFF4772FA), "red" to Color(0xFFE5483F), "orange" to Color(0xFFF2994A),
    "yellow" to Color(0xFFE2B33C), "green" to Color(0xFF35B36A), "teal" to Color(0xFF2BB3A6),
    "purple" to Color(0xFF9D68D3), "pink" to Color(0xFFD15796), "gray" to Color(0xFF9B9A97)
)

/** The colour a block was given (블록 설정 › 블록 색); null = 기본. Read by every block's card. */
val LocalBlockTint = androidx.compose.runtime.compositionLocalOf<Color?> { null }

/** A block's card background: the plain card, tinted when the block has a colour. */
@androidx.compose.runtime.Composable
fun blockBackground(): Color = LocalBlockTint.current
    ?.let { androidx.compose.ui.graphics.lerp(com.bettertick.ui.theme.GroupedCard, it, 0.16f) }
    ?: com.bettertick.ui.theme.GroupedCard

/** A block's heading colour: its own colour, else the usual grey. */
@androidx.compose.runtime.Composable
fun blockHeadingColor(): Color = LocalBlockTint.current ?: com.bettertick.ui.theme.TextSecondary

fun dashColor(key: String, accent: Color): Color =
    if (key == "accent" || key.isBlank()) accent else DashColors.firstOrNull { it.first == key }?.second ?: accent

/** How a value block draws its number. */
object ValueDisplay {
    const val NUMBER = "number"
    const val BAR = "bar"
    const val RING = "ring"
    const val TEXT = "text"
    const val CHIP = "chip"
    const val TREND = "trend"
    val all = listOf(NUMBER to "큰 숫자", BAR to "진행 막대", RING to "원형 게이지", TREND to "7일 추이", TEXT to "한 줄 텍스트", CHIP to "칩")
}

/** "값이 [op] [threshold]이면 [color]" — first matching rule wins. */
data class ColorRule(val op: String = ">=", val threshold: Double = 0.0, val color: String = "red")

/** 값 블록: which variable, how to show it, and how it reacts to its value. */
data class ValueConfig(
    val variable: String = "today.left",
    val display: String = ValueDisplay.NUMBER,
    val prefix: String = "",
    val suffix: String = "",
    /** Gauge full value; 0 = automatic (100 for %, the matching total otherwise). */
    val target: Double = 0.0,
    /** Gauge full value from another variable (e.g. 오늘 할 일 전체); wins over [target]. */
    val targetVariable: String = "",
    val color: String = "accent",
    val rules: List<ColorRule> = emptyList(),
    /** Screen opened on tap, "" = none. */
    val tapTarget: String = ""
) {
    fun encode(): String = JSONObject().apply {
        put("variable", variable); put("display", display); put("prefix", prefix); put("suffix", suffix)
        put("target", target); put("targetVariable", targetVariable); put("color", color); put("tapTarget", tapTarget)
        put("rules", JSONArray().apply {
            rules.forEach { r -> put(JSONObject().apply { put("op", r.op); put("threshold", r.threshold); put("color", r.color) }) }
        })
    }.toString()

    companion object {
        fun decode(raw: String): ValueConfig = runCatching {
            val o = JSONObject(raw)
            val rules = o.optJSONArray("rules") ?: JSONArray()
            ValueConfig(
                variable = o.optString("variable", "today.left"),
                display = o.optString("display", ValueDisplay.NUMBER),
                prefix = o.optString("prefix"), suffix = o.optString("suffix"),
                target = o.optDouble("target", 0.0).let { if (it.isNaN()) 0.0 else it },
                targetVariable = o.optString("targetVariable"),
                color = o.optString("color", "accent"),
                tapTarget = o.optString("tapTarget"),
                rules = (0 until rules.length()).map { i ->
                    val r = rules.getJSONObject(i)
                    ColorRule(r.optString("op", ">="), r.optDouble("threshold", 0.0), r.optString("color", "red"))
                }
            )
        }.getOrDefault(ValueConfig())
    }
}

/** 텍스트 템플릿: a sentence with {변수} in it. */
data class TemplateConfig(val text: String = "오늘 할 일 {today.left}개 남았어요", val size: String = "medium",
                          val center: Boolean = false) {
    fun encode(): String = JSONObject().apply { put("text", text); put("size", size); put("center", center) }.toString()

    companion object {
        fun decode(raw: String): TemplateConfig = runCatching {
            val o = JSONObject(raw)
            TemplateConfig(o.optString("text"), o.optString("size", "medium"), o.optBoolean("center"))
        }.getOrDefault(TemplateConfig())
    }
}

/** One action of a macro. Which fields matter depends on [type]. */
data class MacroStep(
    val type: String,
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    /** 할 일 제목 · 메모 문구 · 알림 문구 · 가계부 메모 ({변수} 사용 가능). */
    val text: String = "",
    /** 목록 id · 이동할 화면 · 가계부 분류 · 메모 블록 id. */
    val target: String = "",
    /** 날짜: 0 = 오늘, 1 = 내일 … · 가계부 금액. */
    val amount: Long = 0,
    /** 할 일 시각 "HH:mm" ("" = 시간 없음). */
    val time: String = "",
    /** 가계부: 수입이면 true. */
    val flag: Boolean = false
)

object MacroType {
    const val ADD_TASK = "add_task"
    const val COMPLETE_TODAY = "complete_today"
    const val CHECK_HABITS = "check_habits"
    const val ADD_MONEY = "add_money"
    const val APPEND_MEMO = "append_memo"
    const val TOAST = "toast"
    const val NAVIGATE = "navigate"

    val all = listOf(
        ADD_TASK to "할 일 추가",
        COMPLETE_TODAY to "오늘 할 일 모두 완료",
        CHECK_HABITS to "오늘 습관 모두 체크",
        ADD_MONEY to "가계부 기록",
        APPEND_MEMO to "메모 블록에 한 줄 기록",
        TOAST to "알림 메시지 띄우기",
        NAVIGATE to "화면 이동"
    )

    fun label(type: String) = all.firstOrNull { it.first == type }?.second ?: type
}

/** When a macro runs by itself ("이벤트"). Fields used depend on [type]. */
data class MacroTrigger(
    val type: String,
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    /** TIME: "HH:mm". */
    val time: String = "09:00",
    /** TIME: weekdays 1(월)..7(일) as digits, "" = every day. */
    val days: String = "",
    /** TIME: only on days whose shift is this (주/야/비/휴/당직/일근), "" = any day. */
    val shift: String = "",
    /** APP_OPEN / TAB_OPEN: at most once a day. */
    val oncePerDay: Boolean = true,
    /** VALUE: variable, operator and value to compare with. */
    val variable: String = "today.left",
    val op: String = "==",
    val value: String = "0",
    val enabled: Boolean = true
)

object TriggerType {
    const val TIME = "time"
    const val APP_OPEN = "app_open"
    const val TAB_OPEN = "tab_open"
    const val TASKS_DONE = "tasks_done"
    const val HABITS_DONE = "habits_done"
    const val VALUE = "value"

    val all = listOf(
        TIME to "정해진 시각에",
        APP_OPEN to "앱을 열 때",
        TAB_OPEN to "이 탭을 열 때",
        TASKS_DONE to "오늘 할 일을 다 끝냈을 때",
        HABITS_DONE to "오늘 습관을 다 체크했을 때",
        VALUE to "변수 값이 조건에 맞을 때"
    )

    fun label(type: String) = all.firstOrNull { it.first == type }?.second ?: type
}

/** "실행 조건": the macro only runs while every condition holds. */
data class MacroCondition(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val variable: String = "shift.today",
    val op: String = "==",
    val value: String = ""
)

/** Compares a variable's value with [value]: as numbers when both are, else as text. */
fun compareValue(v: DashValue, op: String, value: String): Boolean {
    val right = value.trim().replace(",", "").toDoubleOrNull()
    val left = v.number
    if (left != null && right != null) return when (op) {
        ">=" -> left >= right; "<=" -> left <= right; ">" -> left > right; "<" -> left < right
        "!=" -> left != right; else -> left == right
    }
    val a = v.text.trim()
    val b = value.trim()
    return when (op) {
        "!=" -> a != b
        "contains" -> b.isNotEmpty() && a.contains(b)
        else -> a == b
    }
}

val CompareOps = listOf("==" to "같음", "!=" to "다름", ">=" to "이상", "<=" to "이하", ">" to "초과", "<" to "미만", "contains" to "포함")

/** 매크로 버튼: a label, the steps it runs in order, and what starts it. */
data class MacroConfig(
    val label: String = "매크로 실행",
    val color: String = "accent",
    val confirm: Boolean = false,
    val steps: List<MacroStep> = emptyList(),
    val triggers: List<MacroTrigger> = emptyList(),
    val conditions: List<MacroCondition> = emptyList()
) {
    fun encode(): String = JSONObject().apply {
        put("label", label); put("color", color); put("confirm", confirm)
        put("steps", JSONArray().apply {
            steps.forEach { s ->
                put(JSONObject().apply {
                    put("type", s.type); put("id", s.id); put("text", s.text); put("target", s.target)
                    put("amount", s.amount); put("time", s.time); put("flag", s.flag)
                })
            }
        })
        put("triggers", JSONArray().apply {
            triggers.forEach { t ->
                put(JSONObject().apply {
                    put("type", t.type); put("id", t.id); put("time", t.time); put("days", t.days)
                    put("shift", t.shift); put("oncePerDay", t.oncePerDay); put("variable", t.variable)
                    put("op", t.op); put("value", t.value); put("enabled", t.enabled)
                })
            }
        })
        put("conditions", JSONArray().apply {
            conditions.forEach { c ->
                put(JSONObject().apply { put("id", c.id); put("variable", c.variable); put("op", c.op); put("value", c.value) })
            }
        })
    }.toString()

    companion object {
        fun decode(raw: String): MacroConfig = runCatching {
            val o = JSONObject(raw)
            val steps = o.optJSONArray("steps") ?: JSONArray()
            val triggers = o.optJSONArray("triggers") ?: JSONArray()
            val conditions = o.optJSONArray("conditions") ?: JSONArray()
            MacroConfig(
                triggers = (0 until triggers.length()).map { i ->
                    val t = triggers.getJSONObject(i)
                    MacroTrigger(
                        type = t.optString("type"), id = t.optString("id").ifBlank { "t$i" },
                        time = t.optString("time", "09:00"), days = t.optString("days"), shift = t.optString("shift"),
                        oncePerDay = t.optBoolean("oncePerDay", true), variable = t.optString("variable", "today.left"),
                        op = t.optString("op", "=="), value = t.optString("value", "0"), enabled = t.optBoolean("enabled", true)
                    )
                },
                conditions = (0 until conditions.length()).map { i ->
                    val c = conditions.getJSONObject(i)
                    MacroCondition(c.optString("id").ifBlank { "c$i" }, c.optString("variable", "shift.today"),
                        c.optString("op", "=="), c.optString("value"))
                },
                label = o.optString("label", "매크로 실행"),
                color = o.optString("color", "accent"),
                confirm = o.optBoolean("confirm"),
                steps = (0 until steps.length()).map { i ->
                    val s = steps.getJSONObject(i)
                    MacroStep(s.optString("type"), s.optString("id").ifBlank { "s$i" }, s.optString("text"), s.optString("target"),
                        s.optLong("amount"), s.optString("time"), s.optBoolean("flag"))
                }
            )
        }.getOrDefault(MacroConfig())
    }
}
