package com.bettertick.widget.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.bettertick.data.model.Tag
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.ui.screens.calendar.TickSwitch
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.GroupedBackground
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import com.bettertick.widget.WidgetServiceLocator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 위젯 설정 (TickTick widget settings): 테마 · 글자 크기 · 투명도 ·
 * 필터 보기 범위(목록 / 태그 / 우선순위) · 정렬 · 표시 옵션.
 * ✓ saves and repaints every reminder widget, ✕ discards.
 */
@AndroidEntryPoint
class WidgetSettingsActivity : ComponentActivity() {

    @Inject lateinit var listRepository: ListRepository
    @Inject lateinit var tagRepository: TagRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val listsFlow = runCatching { listRepository.observeLists() }.getOrNull()
        val tagsFlow = runCatching { tagRepository.observeTags() }.getOrNull()
        // Snapshot taken outside composition; the live flow below takes over.
        val cachedLists: List<TaskList> = WidgetServiceLocator.listsCache.value
        setContent {
            BetterTickTheme {
                val ctx = this
                var settings by remember { mutableStateOf(WidgetServiceLocator.reminderSettings(ctx)) }
                var filterPage by remember { mutableStateOf(false) }
                val lists by remember {
                    (listsFlow ?: kotlinx.coroutines.flow.flowOf(cachedLists))
                        .catch { emit(cachedLists) }
                }.collectAsState(initial = cachedLists)
                val tags by remember {
                    (tagsFlow ?: kotlinx.coroutines.flow.flowOf(emptyList<Tag>()))
                        .catch { emit(emptyList()) }
                }.collectAsState(initial = emptyList())

                BackHandler(enabled = filterPage) { filterPage = false }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GroupedBackground)
                        .systemBarsPadding()
                ) {
                    if (filterPage) {
                        FilterScopePage(
                            settings = settings,
                            lists = lists,
                            tags = tags,
                            onChange = { settings = it },
                            onBack = { filterPage = false }
                        )
                    } else {
                        TopBar(
                            onClose = { finish() },
                            onConfirm = {
                                WidgetServiceLocator.saveReminderSettings(ctx, settings)
                                refreshWidgets(ctx.applicationContext)
                                finish()
                            }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Card {
                                ChoiceRow(
                                    title = "테마",
                                    value = settings.theme,
                                    options = listOf("black" to "블랙", "gray" to "다크 그레이", "white" to "화이트"),
                                    onPick = { settings = settings.copy(theme = it) }
                                )
                                ChoiceRow(
                                    title = "글자 크기",
                                    value = settings.fontSize,
                                    options = listOf("small" to "작게", "normal" to "보통", "large" to "크게"),
                                    onPick = { settings = settings.copy(fontSize = it) }
                                )
                                OpacityRow(settings.opacity) { settings = settings.copy(opacity = it) }
                            }
                            Spacer(Modifier.height(16.dp))
                            Card {
                                NavRow(
                                    title = "필터 보기 범위",
                                    value = filterSummary(settings, lists, tags),
                                    onClick = { filterPage = true }
                                )
                                ChoiceRow(
                                    title = "정렬",
                                    value = settings.sort,
                                    options = listOf("time" to "시간순", "priority" to "우선순위", "title" to "제목"),
                                    onPick = { settings = settings.copy(sort = it) }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Card {
                                ToggleRow("주간 달력 표시", null, settings.showWeek) {
                                    settings = settings.copy(showWeek = it)
                                }
                                ToggleRow("완료된 할일 보기", null, settings.showCompleted) {
                                    settings = settings.copy(showCompleted = it)
                                }
                                ToggleRow(
                                    "기한 지난 할일 포함",
                                    "오늘을 볼 때 지난 날짜의 미완료 할일도 함께 표시합니다.",
                                    settings.showOverdue
                                ) { settings = settings.copy(showOverdue = it) }
                                ToggleRow(
                                    "자세히 보기",
                                    "날짜·시간과 목록 이름을 표시합니다.",
                                    settings.showDetail
                                ) { settings = settings.copy(showDetail = it) }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Saves happen in prefs; bump each widget's Glance state so it recomposes. */
private fun refreshWidgets(appContext: android.content.Context) {
    // Detached scope: the activity finishes right away.
    CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
        runCatching {
            val mgr = GlanceAppWidgetManager(appContext)
            val widget = ReminderWidget()
            for (id in mgr.getGlanceIds(ReminderWidget::class.java)) {
                updateAppWidgetState(appContext, id) { prefs ->
                    prefs[SETTINGS_VERSION_KEY] = System.currentTimeMillis()
                }
                widget.update(appContext, id)
            }
        }
    }
}

private fun filterSummary(s: WidgetServiceLocator.ReminderSettings, lists: List<TaskList>, tags: List<Tag>): String {
    if (!s.isFiltered) return "전체"
    val parts = mutableListOf<String>()
    if (s.lists.isNotEmpty()) {
        val names = s.lists.map { id -> if (id.isEmpty()) "기본함" else lists.firstOrNull { it.id == id }?.name ?: "목록" }
        parts += if (names.size <= 2) names.joinToString(", ") else "목록 ${names.size}개"
    }
    if (s.tags.isNotEmpty()) {
        val names = s.tags.map { id -> "#" + (tags.firstOrNull { it.id == id }?.name ?: "태그") }
        parts += if (names.size <= 2) names.joinToString(" ") else "태그 ${names.size}개"
    }
    if (s.minPriority > 0) parts += priorityLabel(s.minPriority)
    return parts.joinToString(" · ")
}

private fun priorityLabel(p: Int) = when (p) {
    3 -> "높은 우선순위"
    2 -> "보통 이상"
    1 -> "낮음 이상"
    else -> "전체"
}

// ── 필터 보기 범위 ──────────────────────────────────────────────────────────

@Composable
private fun FilterScopePage(
    settings: WidgetServiceLocator.ReminderSettings,
    lists: List<TaskList>,
    tags: List<Tag>,
    onChange: (WidgetServiceLocator.ReminderSettings) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = TextPrimary)
        }
        Text("필터 보기 범위", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Card {
            CheckRow("전체", null, checked = !settings.isFiltered) {
                onChange(settings.copy(lists = emptySet(), tags = emptySet(), minPriority = 0))
            }
        }

        SectionLabel("목록")
        Card {
            CheckRow("기본함", "#4772FA", checked = "" in settings.lists) {
                onChange(settings.copy(lists = settings.lists.toggle("")))
            }
            lists.sortedBy { it.sortOrder }.forEach { l ->
                CheckRow(l.name, l.color, checked = l.id in settings.lists) {
                    onChange(settings.copy(lists = settings.lists.toggle(l.id)))
                }
            }
        }

        SectionLabel("태그")
        Card {
            if (tags.isEmpty()) {
                Text(
                    "태그가 없습니다", fontSize = 14.sp, color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                )
            }
            tags.sortedBy { it.name.lowercase() }.forEach { t ->
                CheckRow("#${t.name}", t.color, checked = t.id in settings.tags) {
                    onChange(settings.copy(tags = settings.tags.toggle(t.id)))
                }
            }
        }

        SectionLabel("우선순위")
        Card {
            listOf(0, 1, 2, 3).forEach { p ->
                CheckRow(priorityLabel(p), null, checked = settings.minPriority == p, radio = true) {
                    onChange(settings.copy(minPriority = p))
                }
            }
        }
        Text(
            "목록과 태그를 여러 개 고를 수 있습니다. 목록·태그·우선순위를 함께 고르면 모두 만족하는 할일만 보입니다.",
            fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun Set<String>.toggle(v: String): Set<String> = if (v in this) this - v else this + v

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 13.sp, color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun CheckRow(title: String, colorHex: String?, checked: Boolean, radio: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (colorHex != null) {
            val c = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(TextTertiary)
            Box(Modifier.size(10.dp).clip(CircleShape).background(c))
            Spacer(Modifier.width(12.dp))
        }
        Text(title, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (radio) {
            if (checked) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor)
        } else {
            Checkbox(
                checked = checked,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ThemeState.accentColor,
                    uncheckedColor = TextTertiary
                )
            )
        }
    }
}

// ── shared rows ─────────────────────────────────────────────────────────────

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GroupedCard),
        content = content
    )
}

@Composable
private fun TopBar(onClose: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "닫기", tint = TextPrimary)
        }
        Text("위젯 설정", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(48.dp).clickable { onConfirm() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = "저장", tint = ThemeState.accentColor)
        }
    }
}

@Composable
private fun NavRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = TextTertiary)
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(options.firstOrNull { it.first == value }?.second ?: value, fontSize = 14.sp, color = TextSecondary)
            Icon(Icons.Default.KeyboardArrowRight, null, tint = TextTertiary)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(GroupedCard)) {
                options.forEach { (key, label) ->
                    val sel = key == value
                    DropdownMenuItem(
                        text = { Text(label, color = if (sel) ThemeState.accentColor else TextPrimary) },
                        trailingIcon = { if (sel) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor) },
                        onClick = { onPick(key); open = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpacityRow(value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("투명도", fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.width(12.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = ThemeState.accentColor,
                activeTrackColor = ThemeState.accentColor,
                inactiveTrackColor = TextTertiary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text("${value}%", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextTertiary, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        TickSwitch(checked = checked, onCheckedChange = onChange)
    }
}

// Monotonic key inside Glance's per-widget state store. Settings live in
// SharedPreferences (shared across widgets), so changing them doesn't touch
// Glance state on its own — bumping this forces the widget to recompose.
private val SETTINGS_VERSION_KEY = longPreferencesKey("settings_version")
