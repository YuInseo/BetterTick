package com.bettertick.ui.screens.tasks.components

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.TaskList
import com.bettertick.ui.components.MarkdownInlineTransformation
import com.bettertick.ui.screens.tasks.QuickAddDraft
import com.bettertick.ui.screens.tasks.QuickAddParser
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.priorityColor
import java.time.LocalDate
import java.time.LocalTime

private const val QUICK_ADD_PREFS = "quick_add_prefs"

/**
 * TickTick quick-add sheet.
 *
 *  - 제목 + 설명 (설명은 체크리스트 모드로 바꿀 수 있음)
 *  - 툴바: 날짜(시간·반복 포함) · 우선순위 · 태그 · 목록 · 더보기 · 음성/추가
 *  - 스마트 인식: 제목의 "내일 오후 3시", "#태그", "!높음", "^목록", "매주 월요일"
 *    등을 인식해 필드로 바꾸고 제목에서 지움 (더보기에서 켜고 끔)
 */
@Composable
fun TaskInputSheet(
    onAddTask: (QuickAddDraft) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate? = LocalDate.now(),
    lists: List<TaskList> = emptyList(),
    tags: List<Tag> = emptyList(),
    contextListId: String = "",
    onCreateTag: suspend (String) -> String = { "" }
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(QUICK_ADD_PREFS, Context.MODE_PRIVATE) }
    var smartRecognition by remember { mutableStateOf(prefs.getBoolean("smart_recognition", true)) }

    var text by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var checklistMode by remember { mutableStateOf(false) }

    // Manual picks; a manual pick always wins over smart recognition.
    var dateTouched by remember { mutableStateOf(false) }
    var manualDate by remember { mutableStateOf(initialDate) }
    var manualTime by remember { mutableStateOf<LocalTime?>(null) }
    var duration by remember { mutableStateOf(60) }
    var manualRepeat by remember { mutableStateOf<String?>(null) }
    var repeatEnd by remember { mutableStateOf<String?>(null) }
    var reminders by remember { mutableStateOf<List<Int>?>(null) }
    var manualPriority by remember { mutableStateOf<Int?>(null) }
    var tagIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualListId by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var priorityMenu by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val markdownTransformation = remember { MarkdownInlineTransformation() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val parsed = remember(text, smartRecognition) {
        if (smartRecognition && text.isNotBlank()) QuickAddParser.parse(text) else null
    }

    // Effective values (manual > recognized > defaults).
    val effDate: LocalDate? = if (dateTouched) manualDate else parsed?.date ?: manualDate
    val effTime: LocalTime? = if (dateTouched) manualTime else parsed?.time ?: manualTime
    val effRepeat: String? = if (dateTouched) manualRepeat else parsed?.repeatRule ?: manualRepeat
    val effPriority: Int = manualPriority ?: parsed?.priority ?: 0
    val parsedList = parsed?.listName?.let { name -> lists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
    val effListId: String = manualListId ?: parsedList?.id ?: contextListId
    val parsedExistingTagIds = parsed?.tagNames.orEmpty().mapNotNull { n ->
        tags.firstOrNull { it.name.equals(n, ignoreCase = true) }?.id
    }
    val parsedNewTagNames = parsed?.tagNames.orEmpty().filter { n ->
        tags.none { it.name.equals(n, ignoreCase = true) }
    }
    val allTagIds = (tagIds + parsedExistingTagIds).distinct()

    fun submit() {
        val title = (parsed?.title ?: text).trim()
        if (title.isBlank()) return
        val notes = if (checklistMode) {
            description.lines().map { it.trim() }.filter { it.isNotEmpty() }
                .joinToString("\n") { "- [ ] ${it.removePrefix("- [ ] ")}" }
        } else description
        onAddTask(
            QuickAddDraft(
                title = title,
                notes = notes,
                date = effDate,
                time = effTime,
                durationMinutes = duration,
                repeatRule = effRepeat,
                repeatEnd = repeatEnd,
                // Default like TickTick: a timed task reminds on time.
                reminders = reminders ?: if (effTime != null) listOf(0) else emptyList(),
                priority = effPriority,
                tagIds = allTagIds,
                newTagNames = parsedNewTagNames,
                listId = effListId
            )
        )
    }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            text = if (text.isBlank()) spoken else "$text $spoken"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("무엇을 하고 싶으신가요?", color = TextSecondary) },
            colors = sheetFieldColors(),
            visualTransformation = markdownTransformation,
            // Single-line title — Enter / 완료 reliably submits.
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = {
                Text(
                    if (checklistMode) "체크리스트 항목 (한 줄에 하나씩)" else "설명",
                    color = TextTertiary
                )
            },
            leadingIcon = if (checklistMode) {
                { Icon(Icons.Outlined.Checklist, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) }
            } else null,
            colors = sheetFieldColors(),
            visualTransformation = markdownTransformation,
            modifier = Modifier.fillMaxWidth()
        )

        // Recognized tokens + picked tags/list, TickTick-style pills.
        val pills = buildList {
            parsed?.recognized?.forEach { add(it to true) }
            allTagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                .filter { t -> parsed?.tagNames?.any { it.equals(t.name, true) } != true }
                .forEach { add("#${it.name}" to false) }
            if (manualListId != null) {
                add("^${lists.firstOrNull { it.id == manualListId }?.name ?: "기본함"}" to false)
            }
        }
        if (pills.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pills.forEach { (label, smart) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (smart) {
                            Icon(
                                Icons.Outlined.AutoAwesome, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Toolbar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateChip(
                date = effDate,
                time = effTime,
                repeating = effRepeat != null,
                onClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box {
                ToolbarIcon(
                    icon = if (effPriority > 0) Icons.Filled.Flag else Icons.Outlined.Flag,
                    description = "우선순위",
                    tint = if (effPriority > 0) priorityColor(effPriority) else TextSecondary,
                    onClick = { priorityMenu = true }
                )
                DropdownMenu(
                    expanded = priorityMenu,
                    onDismissRequest = { priorityMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    listOf(3 to "높은 우선순위", 2 to "중간 우선순위", 1 to "낮은 우선순위", 0 to "우선순위 없음")
                        .forEach { (lvl, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        if (lvl > 0) Icons.Filled.Flag else Icons.Outlined.Flag,
                                        null,
                                        tint = priorityColor(lvl)
                                    )
                                },
                                trailingIcon = {
                                    if (lvl == effPriority) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = { manualPriority = lvl; priorityMenu = false }
                            )
                        }
                }
            }
            ToolbarIcon(
                icon = Icons.AutoMirrored.Outlined.Label,
                description = "태그",
                tint = if (allTagIds.isNotEmpty() || parsedNewTagNames.isNotEmpty()) MaterialTheme.colorScheme.primary else TextSecondary,
                onClick = { showTagPicker = true }
            )
            // List — shows the target list name once it differs from the
            // screen's own list, the way TickTick labels the move button.
            val listName = when {
                effListId.isEmpty() -> null
                else -> lists.firstOrNull { it.id == effListId }?.name
            }
            if (listName != null && effListId != contextListId) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showListPicker = true }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MoveToInbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        listName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(56.dp)
                    )
                }
            } else {
                ToolbarIcon(
                    icon = Icons.Outlined.MoveToInbox,
                    description = "목록",
                    tint = TextSecondary,
                    onClick = { showListPicker = true }
                )
            }
            Box {
                ToolbarIcon(
                    icon = Icons.Outlined.MoreHoriz,
                    description = "더보기",
                    tint = TextSecondary,
                    onClick = { moreMenu = true }
                )
                DropdownMenu(
                    expanded = moreMenu,
                    onDismissRequest = { moreMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (checklistMode) "텍스트로 전환" else "체크리스트로 전환", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.Checklist, null, tint = TextSecondary) },
                        onClick = { checklistMode = !checklistMode; moreMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("스마트 인식", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = TextSecondary) },
                        trailingIcon = {
                            if (smartRecognition) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            smartRecognition = !smartRecognition
                            prefs.edit().putBoolean("smart_recognition", smartRecognition).apply()
                            moreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("날짜 지우기", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, tint = TextSecondary) },
                        onClick = {
                            dateTouched = true; manualDate = null; manualTime = null; manualRepeat = null
                            moreMenu = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Mic when the title is empty, Send once there's something to add.
            val canSubmit = text.isNotBlank()
            IconButton(
                onClick = {
                    if (canSubmit) submit() else startSpeech(context) { speechLauncher.launch(it) }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (canSubmit) Icons.AutoMirrored.Filled.Send else Icons.Outlined.Mic,
                    contentDescription = if (canSubmit) "추가" else "음성 입력",
                    tint = if (canSubmit) MaterialTheme.colorScheme.primary else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        TaskDatePickerSheet(
            initialDate = effDate ?: LocalDate.now(),
            initialTime = effTime,
            initialDurationMinutes = duration,
            initialRepeat = parseRepeatRule(effRepeat),
            initialRepeatEnd = parseRepeatEnd(repeatEnd),
            initialReminders = reminders,
            onRemindersChosen = { reminders = it },
            onDismiss = { showDatePicker = false },
            onDelete = {
                dateTouched = true
                manualDate = null; manualTime = null; manualRepeat = null; repeatEnd = null
                showDatePicker = false
            },
            onConfirm = { date, time, dur, repeat, end ->
                dateTouched = true
                manualDate = date
                manualTime = time
                duration = dur
                manualRepeat = repeat.toRule()
                repeatEnd = end.toPersisted()
                showDatePicker = false
            }
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            tags = tags,
            selectedIds = allTagIds,
            onToggle = { id -> tagIds = if (id in tagIds) tagIds - id else tagIds + id },
            onCreate = onCreateTag,
            onDismiss = { showTagPicker = false }
        )
    }

    if (showListPicker) {
        MoveTaskListSheet(
            lists = lists,
            currentListId = effListId,
            onDismiss = { showListPicker = false },
            onPick = { id -> manualListId = id; showListPicker = false }
        )
    }
}

@Composable
private fun sheetFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun ToolbarIcon(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Date chip: "오늘", "내일 15:00", "10/20" + repeat glyph; red when overdue. */
@Composable
private fun DateChip(date: LocalDate?, time: LocalTime?, repeating: Boolean, onClick: () -> Unit) {
    val today = LocalDate.now()
    val accent = MaterialTheme.colorScheme.primary
    val color = when {
        date == null -> TextSecondary
        date.isBefore(today) -> OverdueRed
        else -> accent
    }
    val dayLabel = when (date) {
        null -> "날짜"
        today -> "오늘"
        today.plusDays(1) -> "내일"
        today.plusDays(2) -> "모레"
        else -> if (date.year == today.year) "${date.monthValue}/${date.dayOfMonth}" else "${date.year}/${date.monthValue}/${date.dayOfMonth}"
    }
    val label = if (time != null && date != null) "$dayLabel %d:%02d".format(time.hour, time.minute) else dayLabel
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (date == null) Color.Transparent else color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            if (date == null) Icons.Outlined.CalendarToday else Icons.Default.CalendarToday,
            contentDescription = "날짜",
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        if (date != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (repeating) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.Outlined.Repeat, null, tint = color, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Launches the system speech recognizer (Korean); toasts when missing. */
private fun startSpeech(context: Context, launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "할일을 말해보세요")
    }
    runCatching { launch(intent) }.onFailure {
        Toast.makeText(context, "이 기기에서 음성 입력을 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
    }
}
