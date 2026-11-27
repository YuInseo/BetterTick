package com.bettertick.ui.screens.tasks.components

import com.bettertick.ui.theme.TextPrimary
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoCamera
import com.bettertick.ui.components.DropdownMenu
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.bettertick.data.model.isShiftRotationOf
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.leaveOn
import com.bettertick.data.model.shiftKindOn
import com.bettertick.data.model.withDayShift
import com.bettertick.data.model.shiftDay
import com.bettertick.data.model.withLeaveToggled
import com.bettertick.data.model.NoteBlockKind
import com.bettertick.data.model.TaskMemo
import com.bettertick.data.model.noteBlocks
import com.bettertick.data.model.savedNoteBlocks
import com.bettertick.data.model.commonNoteText
import com.bettertick.data.model.noteAttachments
import com.bettertick.data.model.attachmentNoteBlock
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.ui.components.MarkdownInlineTransformation
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.util.DateUtils.toLocalDate
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/** Accent blue used on the date row and Notion link — same shade as
 *  TaskDatePickerSheet's DatePickerAccent so the two surfaces feel related. */
private val DetailAccent: Color get() = com.bettertick.ui.theme.ThemeState.accentColor

/**
 * Bottom-sheet view of a single task. Shows the list the task belongs to,
 * its date/time line (tappable — reuses [TaskDatePickerSheet]), a big
 * title, schedule properties and an ordered native block document.
 *
 * Edits are local until the sheet is dismissed; we fire a single
 * [onUpdateTask] when the sheet closes if anything has changed, so we
 * don't write on every keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    listName: String,
    onDismiss: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onToggleComplete: () -> Unit,
    tags: List<Tag> = emptyList(),
    onCreateTag: suspend (String) -> String = { "" },
    /** Lists for the header's 목록 picker (empty = 기본함 only). */
    lists: List<com.bettertick.data.model.TaskList> = emptyList(),
    /** ⋯ > 복제 — creates a copy of the (edited) task. */
    onDuplicate: ((Task) -> Unit)? = null,
    /** ⋯ > 삭제. */
    onDelete: ((Task) -> Unit)? = null,
    /** Day this sheet was opened for (calendar) — 근무 휴가 기록의 기준일. */
    occurrenceDate: java.time.LocalDate? = null,
    /** 하위 할일: the parent's title, shown with a 분리 button. */
    parentTitle: String? = null,
    /** 하위 페이지: titles of the pages above this one, top first (shown under the date). */
    breadcrumb: List<String> = emptyList(),
    /** Sub-page swiped down: close the pages above it too (the whole stack). */
    onSwipeDismiss: (() -> Unit)? = null,
    /** Open as the full page (the page this sub-page was opened from was full). */
    startAsPage: Boolean = false,
    onUnnest: (() -> Unit)? = null
) {
    // Local edit state — keyed by task.id so re-opening a different task
    // pulls in fresh values instead of leaking from the previous one.
    val context = LocalContext.current
    var title by remember(task.id) { mutableStateOf(task.title) }
    // 개별 메모: 반복 할일을 특정 날짜에서 열었을 때 그 날에만 보이는 메모.
    val dayKey = occurrenceDate?.takeIf { !task.repeatRule.isNullOrBlank() }?.toString()
    val blocks = remember(task.id) {
        androidx.compose.runtime.mutableStateListOf<TaskMemo>().apply { addAll(task.noteBlocks().map { block ->
            if (block.attachment.isBlank() && block.kind != NoteBlockKind.DIVIDER)
                block.copy(text = compactNoteBlockText(block.text)) else block
        }) }
    }
    var focusBlockId by remember(task.id) { mutableStateOf<String?>(null) }
    var requestAttachment by remember { mutableStateOf(false) }
    var attachmentDay by remember(task.id, dayKey) { mutableStateOf(dayKey.orEmpty()) }
    // 키보드 위 블록 바 (노션처럼): 편집 중인 블록과 블록 고르기 패널.
    var activeBlockId by remember(task.id) { mutableStateOf<String?>(null) }
    var notePanel by remember { mutableStateOf(NotePanel.NONE) }
    val blockPanel = notePanel != NotePanel.NONE
    var formatRequest by remember { mutableStateOf<Pair<String, InlineMark>?>(null) }
    var formatTick by remember { mutableIntStateOf(0) }
    // 실행 취소 / 다시 실행: snapshots of the document, taken after typing pauses.
    val undoStack = remember(task.id) { androidx.compose.runtime.mutableStateListOf<List<TaskMemo>>() }
    val redoStack = remember(task.id) { androidx.compose.runtime.mutableStateListOf<List<TaskMemo>>() }
    var lastSnapshot by remember(task.id) { mutableStateOf<List<TaskMemo>>(emptyList()) }
    LaunchedEffect(task.id) {
        lastSnapshot = blocks.toList()
        snapshotFlow { blocks.toList() }.collectLatest { now ->
            kotlinx.coroutines.delay(500)
            if (now != lastSnapshot) {
                undoStack.add(lastSnapshot)
                if (undoStack.size > 100) undoStack.removeAt(0)
                redoStack.clear()
                lastSnapshot = now
            }
        }
    }
    fun restoreDocument(from: MutableList<List<TaskMemo>>, to: MutableList<List<TaskMemo>>) {
        val now = blocks.toList()
        if (now != lastSnapshot) { undoStack.add(lastSnapshot); redoStack.clear(); lastSnapshot = now }
        val target = from.removeLastOrNull() ?: return
        to.add(blocks.toList())
        lastSnapshot = target
        blocks.clear(); blocks.addAll(target)
    }
    /** Notion-style: an empty block becomes [kind]; otherwise [kind] goes right below it. */
    fun applyBlockKind(kind: String) {
        val at = blocks.indexOfFirst { it.id == activeBlockId }
        val current = blocks.getOrNull(at)
        val day = current?.day ?: dayKey.orEmpty()
        val static = NoteBlockKind.isStatic(kind)
        if (current != null && current.text.isEmpty() && current.attachment.isBlank() &&
            !NoteBlockKind.isStatic(current.kind) && !static) {
            blocks[at] = current.copy(kind = kind, checked = NoteBlockKind.isToggle(kind))
            return
        }
        if (blocks.size >= 999) {
            android.widget.Toast.makeText(context, "블록은 최대 1,000개까지 추가할 수 있어요", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val indent = current?.indent ?: 0
        val block = TaskMemo(kind = kind, day = day, indent = indent, checked = NoteBlockKind.isToggle(kind))
        var insertAt = if (at >= 0) at + 1 else blocks.size
        if (at >= 0) while (insertAt < blocks.size && blocks[insertAt].indent > indent) insertAt++
        blocks.add(insertAt, block)
        if (static) {
            val after = TaskMemo(kind = NoteBlockKind.TEXT, day = day, indent = indent)
            blocks.add(insertAt + 1, after)
            focusBlockId = after.id
        } else focusBlockId = block.id
    }
    fun addBlock(kind: String, day: String) {
        if (blocks.size >= 1000) {
            android.widget.Toast.makeText(context, "블록은 최대 1,000개까지 추가할 수 있어요", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val block = TaskMemo(kind = kind, day = day, checked = NoteBlockKind.isToggle(kind))
        blocks.add(block)
        focusBlockId = block.id
        if (NoteBlockKind.isStatic(kind) && blocks.size < 1000) {
            val after = TaskMemo(kind = NoteBlockKind.TEXT, day = day)
            blocks.add(after)
            focusBlockId = after.id
        }
    }
    // 페이지 블록 (하위 페이지): a 하위 할일 opened as its own page on top.
    val pageScope = androidx.compose.runtime.rememberCoroutineScope()
    val subPages = remember { lazy { SubPages(context) } }
    var openPage by remember { mutableStateOf<Task?>(null) }
    // Whether this sheet is the full page right now (sub-pages open the same way).
    var isFullPage by remember { mutableStateOf(false) }
    fun createPage(replaceBlockId: String?) {
        pageScope.launch {
            val parent = task.copy(title = title)
            val page = runCatching { subPages.value.create(parent) }.getOrNull()
            if (page == null) {
                android.widget.Toast.makeText(context, "페이지를 만들지 못했어요", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val at = blocks.indexOfFirst { it.id == replaceBlockId }
            val current = blocks.getOrNull(at)
            val block = TaskMemo(kind = NoteBlockKind.PAGE, pageId = page.id,
                day = current?.day ?: dayKey.orEmpty(), indent = current?.indent ?: 0)
            when {
                // An empty text block becomes the page (like Notion's /페이지).
                current != null && current.text.isEmpty() && !NoteBlockKind.isStatic(current.kind) &&
                    current.attachment.isBlank() -> blocks[at] = block
                at >= 0 -> blocks.add(at + 1, block)
                else -> blocks.add(block)
            }
            openPage = page
        }
    }
    fun openPageById(id: String) {
        pageScope.launch {
            val page = runCatching { subPages.value.load(id) }.getOrNull()
            if (page == null) android.widget.Toast.makeText(context, "삭제된 페이지예요", android.widget.Toast.LENGTH_SHORT).show()
            else openPage = page
        }
    }
    /** Aa: the edited block becomes [kind], text kept. */
    fun turnActiveInto(kind: String) {
        val at = blocks.indexOfFirst { it.id == activeBlockId }
        val current = blocks.getOrNull(at) ?: return
        if (current.attachment.isNotBlank() || NoteBlockKind.isStatic(current.kind)) return
        blocks[at] = current.copy(kind = kind, checked = if (NoteBlockKind.isToggle(kind)) true else
            current.checked && kind == NoteBlockKind.TODO)
        focusBlockId = current.id
    }
    // 근무 할일: leave and per-day shift edits remain task properties.
    var leaves by remember(task.id) { mutableStateOf(task.leaves) }
    var dayShifts by remember(task.id) { mutableStateOf(task.dayShifts) }
    var notionUrl by remember(task.id) { mutableStateOf(task.notionUrl) }
    var dueTimestamp by remember(task.id) { mutableStateOf(task.dueDate) }
    var duration by remember(task.id) { mutableStateOf(task.durationMinutes) }
    var repeatRule by remember(task.id) { mutableStateOf(task.repeatRule) }
    var repeatEnd by remember(task.id) { mutableStateOf(task.repeatEnd) }
    var tagIds by remember(task.id) { mutableStateOf(task.tagIds) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var reminders by remember(task.id) { mutableStateOf(task.reminders) }
    var listId by remember(task.id) { mutableStateOf(task.listId) }
    var isPinned by remember(task.id) { mutableStateOf(task.isPinned) }
    var isAbandoned by remember(task.id) { mutableStateOf(task.isAbandoned) }
    var checkWholeSeries by remember(task.id) { mutableStateOf(task.checkWholeSeries) }
    var showBlockActions by remember(task.id) { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val shownListName = if (listId == task.listId) listName
        else lists.firstOrNull { it.id == listId }?.name ?: "기본함"

    var showDatePicker by remember { mutableStateOf(false) }
    // 반복 > 근무 패턴 반복 — 이 블록 날짜 주변의 근무 입력으로 패턴을 찾아 자동 반복.
    var shiftAnchor by remember { mutableStateOf<LocalDate?>(null) }
    var shiftDetection by remember { mutableStateOf<com.bettertick.data.model.ShiftDetection?>(null) }
    shiftAnchor?.let { anchor ->
        val shiftViewModel: ShiftRotationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        LaunchedEffect(anchor) { shiftDetection = shiftViewModel.detect(anchor) }
    }
    var showNotionInput by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var priorityMenuOpen by remember { mutableStateOf(false) }

    // A single editor stays composed while its container follows the drag.
    val contentScroll = rememberScrollState()

    fun commitAndClose() {
        val newMemos = blocks.toList().savedNoteBlocks()
        val notes = newMemos.commonNoteText()
        val attachmentsList = newMemos.noteAttachments()
        val changed =
            newMemos != task.memos || task.dayNotes.isNotEmpty() ||
            title != task.title ||
                notes != task.notes ||
                notionUrl != task.notionUrl ||
                dueTimestamp != task.dueDate ||
                duration != task.durationMinutes ||
                repeatRule != task.repeatRule ||
                repeatEnd != task.repeatEnd ||
                tagIds != task.tagIds ||
                priority != task.priority ||
                reminders != task.reminders ||
                listId != task.listId ||
                isPinned != task.isPinned ||
                isAbandoned != task.isAbandoned ||
                checkWholeSeries != task.checkWholeSeries ||
                leaves != task.leaves ||
                dayShifts != task.dayShifts ||
                attachmentsList != task.attachments
        if (changed) {
            onUpdateTask(
                task.copy(
                    title = title,
                    leaves = leaves,
                    dayShifts = dayShifts,
                    notes = notes,
                    dayNotes = emptyMap(),
                    memos = newMemos,
                    notionUrl = notionUrl,
                    dueDate = dueTimestamp,
                    durationMinutes = duration,
                    repeatRule = repeatRule,
                    repeatEnd = repeatEnd,
                    tagIds = tagIds,
                    priority = priority,
                    attachments = attachmentsList,
                    reminders = reminders,
                    listId = listId,
                    isPinned = isPinned,
                    isAbandoned = isAbandoned,
                    checkWholeSeries = checkWholeSeries,
                    abandonedAt = if (isAbandoned && !task.isAbandoned) com.google.firebase.Timestamp.now() else task.abandonedAt
                )
            )
        }
        onDismiss()
    }
    // A sub-page above was swiped closed: slide this page away too.
    var swipeCloseTick by remember { mutableIntStateOf(0) }
    /** Swipe-close: save, then let the pages above close as well. */
    fun swipeCloseAll() {
        commitAndClose()
        onSwipeDismiss?.invoke()
    }
    // 하위 페이지 유지 (설정): reopen the sub-page that was left open last time.
    LaunchedEffect(task.id) {
        if (com.bettertick.ui.theme.AppPrefs.reopenLastSubPage) {
            val last = com.bettertick.ui.theme.AppPrefs.lastSubPage(task.id)
            if (last.isNotBlank()) openPageById(last)
        }
    }

    // Where the big title ends inside the scrolling content (px), for the pinned page bar.
    var titleBottomPx by remember { mutableIntStateOf(0) }
    // derivedStateOf: scrolling only recomposes when the answer flips, not every frame.
    val titleScrolledAway by remember { androidx.compose.runtime.derivedStateOf { titleBottomPx > 0 && contentScroll.value > titleBottomPx } }
    // 날짜 줄: inside the sheet in drawer mode, in the pinned top bar in page mode.
    val dateRow: @Composable (Modifier) -> Unit = { rowModifier ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = rowModifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    // 날짜와 시간을 두 줄로 — 아이콘 4개와 같이 있어도 어색하게 꺾이지 않게.
                    val dueLabel = formatDueLabel(dueTimestamp, duration, occurrenceDate.takeIf {
                        !repeatRule.isNullOrBlank() && dueTimestamp == task.dueDate
                    })
                    val dayPart = dueLabel.substringBefore(" · ")
                    val timePart = dueLabel.substringAfter(" · ", "")
                    Column {
                        Text(dayPart, color = if (dueTimestamp == null) TextSecondary else TextPrimary,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (timePart.isNotEmpty()) {
                            Text(timePart, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (!repeatRule.isNullOrBlank()) Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Repeat, "반복 일정", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                if (reminders.isNotEmpty()) Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NotificationsActive, "알림 설정됨", tint = DetailAccent, modifier = Modifier.size(18.dp))
                }
                // 우선순위 · 더보기: 날짜 줄 오른쪽 끝에 함께.
                Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val flagTint = priorityColor(priority)
                    IconButton(onClick = { priorityMenuOpen = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (priority > 0) Icons.Filled.Flag
                                else Icons.Outlined.Flag,
                            contentDescription = "우선순위",
                            tint = flagTint, modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = priorityMenuOpen,
                        onDismissRequest = { priorityMenuOpen = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        PriorityMenuItem(
                            label = "높음",
                            level = 3,
                            current = priority,
                            onClick = {
                                priority = 3
                                priorityMenuOpen = false
                            }
                        )
                        PriorityMenuItem(
                            label = "중간",
                            level = 2,
                            current = priority,
                            onClick = {
                                priority = 2
                                priorityMenuOpen = false
                            }
                        )
                        PriorityMenuItem(
                            label = "낮음",
                            level = 1,
                            current = priority,
                            onClick = {
                                priority = 1
                                priorityMenuOpen = false
                            }
                        )
                        PriorityMenuItem(
                            label = "없음",
                            level = 0,
                            current = priority,
                            onClick = {
                                priority = 0
                                priorityMenuOpen = false
                            }
                        )
                    }
                }
                Box {
                    IconButton(onClick = { moreMenuOpen = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Outlined.MoreHoriz,
                            contentDescription = "더 보기",
                            tint = TextSecondary, modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuOpen,
                        onDismissRequest = { moreMenuOpen = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        if (blocks.isNotEmpty()) DetailMenuItem(
                            if (showBlockActions) "블록 관리 종료" else "블록 관리", Icons.Outlined.MoreHoriz
                        ) {
                            showBlockActions = !showBlockActions
                            moreMenuOpen = false
                        }
                        DetailMenuItem(if (isPinned) "고정 해제" else "고정", Icons.Outlined.PushPin) {
                            isPinned = !isPinned; moreMenuOpen = false
                        }
                        if (!task.copy(title = title).isShiftTask()) {
                            DetailMenuItem(
                                if (task.isCompleted) "완료 취소" else "완료",
                                Icons.Outlined.CheckCircle
                            ) {
                                moreMenuOpen = false
                                onToggleComplete()
                            }
                        }
                        DetailMenuItem(if (isAbandoned) "하지 않음 취소" else "하지 않음", Icons.Outlined.DoNotDisturbOn) {
                            isAbandoned = !isAbandoned; moreMenuOpen = false
                        }
                        // 반복 할일: 날짜별로 체크할지, 한 번에 반복 전체를 체크할지.
                        if (!repeatRule.isNullOrBlank()) {
                            DetailMenuItem(
                                if (checkWholeSeries) "체크: 날짜별로 하기" else "체크: 반복 전체 한 번에",
                                Icons.Outlined.Repeat
                            ) {
                                checkWholeSeries = !checkWholeSeries
                                moreMenuOpen = false
                                android.widget.Toast.makeText(
                                    context,
                                    if (checkWholeSeries) "체크하면 반복 전체가 완료됩니다" else "날짜마다 따로 체크합니다",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        if (onDuplicate != null) {
                            DetailMenuItem("복제", Icons.Outlined.ContentCopy) {
                                moreMenuOpen = false
                                val document = blocks.toList().savedNoteBlocks()
                                onDuplicate(task.copy(title = title, notes = document.commonNoteText(),
                                    memos = document, dayNotes = emptyMap(), attachments = document.noteAttachments(), dueDate = dueTimestamp,
                                    durationMinutes = duration, repeatRule = repeatRule, repeatEnd = repeatEnd,
                                    tagIds = tagIds, priority = priority, reminders = reminders, listId = listId))
                                android.widget.Toast.makeText(context, "할일을 복제했습니다", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        DetailMenuItem("목록 이동", Icons.AutoMirrored.Outlined.DriveFileMove) {
                            moreMenuOpen = false; showListPicker = true
                        }
                        DetailMenuItem("공유", Icons.Outlined.Share) {
                            moreMenuOpen = false
                            val notes = blocks.filter { it.day.isBlank() || dayKey == null || it.day == dayKey }
                                .map { it.copy(day = "") }.commonNoteText()
                            com.bettertick.ui.screens.tasks.shareText(context, if (notes.isBlank()) title else "$title\n\n$notes")
                        }
                        // 하위 할일: 상위에서 분리 (예전 안내 칸 대신 메뉴에).
                        if (onUnnest != null) {
                            DetailMenuItem(
                                "'${parentTitle ?: "상위"}'에서 분리",
                                Icons.AutoMirrored.Outlined.DriveFileMove
                            ) {
                                moreMenuOpen = false
                                onUnnest()
                            }
                        }
                        if (onDelete != null) {
                            DetailMenuItem("삭제", Icons.Outlined.Delete, tint = com.bettertick.ui.theme.OverdueRed) {
                                moreMenuOpen = false; confirmDelete = true
                            }
                        }
                    }
                }
                }
            }
    }
    ExpandableTaskDetail(onClose = { commitAndClose() },
        onSwipeClose = if (onSwipeDismiss != null) ({ swipeCloseAll() }) else null,
        swipeCloseTick = swipeCloseTick,
        hidden = openPage != null,
        animateBackClose = onSwipeDismiss == null,
        startAsPage = startAsPage,
        pageTitle = title.ifBlank { "제목 없음" }, pageSubtitle = shownListName,
        titleScrolledAway = titleScrolledAway,
        pageBar = { dateRow(Modifier.fillMaxWidth()) }) {
        val scrollbarColor = TextSecondary.copy(alpha = 0.45f)
        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .detailScrollbar(contentScroll, scrollbarColor)
                .verticalScroll(contentScroll)
                .padding(bottom = 12.dp)
        ) {
            // 날짜 한 줄 + 오른쪽 아이콘 묶음 (반복 · 알림 · 우선순위 · 더보기),
            // 아이콘은 모두 같은 크기 · 같은 간격.
            // In page mode this row moves up beside the back button (pinned bar).
            val pageExpansion = LocalDetailExpansion.current
            SideEffect { isFullPage = pageExpansion > 0.5f }
            if (pageExpansion < 0.5f) dateRow(Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(1f - pageExpansion * 2f))
            // 페이지 안의 페이지: the pages above it, in a row (비 › 저녁 › …).
            if (breadcrumb.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 2.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    breadcrumb.forEachIndexed { i, crumb ->
                        if (i > 0) Text("›", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                        Icon(Icons.Outlined.Description, null, tint = TextTertiary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(crumb.ifBlank { "제목 없음" }, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
                    }
                    Text("›", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
            // ── Compact list / parent context directly below the date ──────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { showListPicker = true }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Folder, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = shownListName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 90.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = "리스트 선택",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 태그 (예전 하단 버튼): 목록 바로 옆에 작게.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { showTagPicker = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = "태그", tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("태그", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                }
                if (!parentTitle.isNullOrBlank()) {
                    Text(" / ", fontSize = 11.sp, color = TextTertiary)
                    Text(parentTitle, color = TextSecondary, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                // 근무 일정: 근무 · 휴가 칩을 목록 줄 오른쪽에.
                val asShift = task.copy(title = title, leaves = leaves, dayShifts = dayShifts)
                if (asShift.isShiftTask()) {
                    val day = asShift.shiftDay(occurrenceDate)
                    ShiftDayPickers(
                        day = day,
                        kind = asShift.shiftKindOn(day),
                        onPickKind = if (day != null) {
                            { kind -> dayShifts = asShift.withDayShift(day, kind).dayShifts }
                        } else null,
                        used = asShift.leaveOn(day),
                        savedUsed = task.leaveOn(day),
                        onToggleLeave = if (day != null) {
                            { type ->
                                val t = asShift.withLeaveToggled(day, type)
                                title = t.title
                                leaves = t.leaves
                            }
                        } else null
                    )
                }

            }

            Spacer(Modifier.height(4.dp))

            // 제목만 — 완료는 목록의 체크박스나 ⋯ 메뉴에서.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.onGloballyPositioned {
                titleBottomPx = (it.positionInParent().y + it.size.height).toInt()
            }) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 24.sp, lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── 근무 일정: 지휴 · 병가 · 연차 (one straight row) ────────────
            // ── Selected-tag chips ───────────────────────────────────────
            val selectedTags = remember(tagIds, tags) {
                // Preserve the stored order so the chip row is stable even
                // when the tag collection re-sorts.
                tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
            }
            if (selectedTags.isNotEmpty()) {
                TagChipFlow(
                    tags = selectedTags,
                    onRemove = { tag -> tagIds = tagIds - tag.id }
                )
            }

            TaskNoteEditor(blocks = blocks, day = dayKey, focusId = focusBlockId,
                showBlockActions = showBlockActions,
                onAdd = ::addBlock,
                onFocusBlock = { focusBlockId = it },
                onAttach = { day -> attachmentDay = day; requestAttachment = true },
                onActiveBlock = { id, focused ->
                    if (focused) activeBlockId = id
                    else if (activeBlockId == id && !blockPanel) activeBlockId = null
                },
                formatRequest = formatRequest, formatTick = formatTick,
                onCreatePage = ::createPage,
                onOpenPage = ::openPageById)
        }
            // ── 키보드 위 블록 바 + 블록 고르기 패널 (편집 중일 때만) ─────────
            // Like Notion, the panel takes exactly the keyboard's place: while the
            // keyboard slides away the panel grows by the same amount (and the
            // other way round), so the bar never jumps.
            val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            val imeInsets = WindowInsets.ime
            val navInsets = WindowInsets.navigationBars
            val imePx = (imeInsets.getBottom(density) - navInsets.getBottom(density)).coerceAtLeast(0)
            var keyboardPx by remember { mutableIntStateOf(0) }
            // The height the keyboard is heading to (known at the start of its
            // animation). Remember that — not the largest height ever seen —
            // so a keyboard that comes back shorter (no emoji row, …) doesn't
            // leave a strip of panel between the bar and the keys.
            val imeTargetPx = (WindowInsets.imeAnimationTarget.getBottom(density) -
                navInsets.getBottom(density)).coerceAtLeast(0)
            if (imeTargetPx > 0 && imeTargetPx != keyboardPx) keyboardPx = imeTargetPx
            else if (keyboardPx == 0 && imePx > 0) keyboardPx = imePx
            val panelPx = if (keyboardPx > 0) keyboardPx else with(density) { 280.dp.roundToPx() }
            // Panel last shown, kept on screen while the keyboard comes back over it.
            var lastPanel by remember { mutableStateOf(NotePanel.NONE) }
            var restoring by remember { mutableStateOf(false) }
            if (notePanel != NotePanel.NONE) lastPanel = notePanel
            // While going back to the keyboard the panel also folds away on its own,
            // quickly — so a keyboard that animates slowly (or jumps in late) never
            // leaves the panel hanging there.
            var collapseCap by remember { androidx.compose.runtime.mutableFloatStateOf(Float.MAX_VALUE) }
            LaunchedEffect(restoring) {
                if (restoring) {
                    kotlinx.coroutines.coroutineScope {
                        launch {
                            androidx.compose.animation.core.animate(panelPx.toFloat(), 0f,
                                animationSpec = androidx.compose.animation.core.tween(200,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing)) { v, _ -> collapseCap = v }
                        }
                        // keyboardPx is state: if the keyboard turns out shorter, the target follows.
                        kotlinx.coroutines.withTimeoutOrNull(400) {
                            snapshotFlow { imeInsets.getBottom(density) - navInsets.getBottom(density) >= keyboardPx - 2 }.first { it }
                        }
                    }
                    restoring = false
                }
                collapseCap = Float.MAX_VALUE
            }
            // Tapping a block while a panel is open brings the keyboard back: then the
            // panel gives way to it (otherwise both would show).
            LaunchedEffect(notePanel) {
                if (notePanel == NotePanel.NONE) return@LaunchedEffect
                val keyboardNow = { imeInsets.getBottom(density) - navInsets.getBottom(density) }
                kotlinx.coroutines.withTimeoutOrNull(1_000) { snapshotFlow { keyboardNow() }.first { it <= 0 } }
                snapshotFlow { keyboardNow() }.first { it > 0 }
                restoring = true
                notePanel = NotePanel.NONE
            }
            if (activeBlockId != null || blockPanel) {
                fun closePanel() { restoring = notePanel != NotePanel.NONE; notePanel = NotePanel.NONE; keyboard?.show() }
                fun pickAttachment() {
                    attachmentDay = blocks.firstOrNull { it.id == activeBlockId }?.day ?: dayKey.orEmpty()
                    requestAttachment = true; notePanel = NotePanel.NONE; restoring = false
                }
                // Full-bleed (past the sheet's side padding) and flush with the keyboard.
                Column(Modifier.layout { measurable, constraints ->
                    val extra = 40.dp.roundToPx()
                    val width = constraints.maxWidth + extra
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                }.background(DarkSurfaceVariant.copy(alpha = 0.35f))) {
                BlockKeyboardBar(
                    panel = notePanel,
                    canUndo = undoStack.isNotEmpty() || blocks.toList() != lastSnapshot,
                    canRedo = redoStack.isNotEmpty(),
                    onPanel = { p ->
                        if (notePanel == p) closePanel()
                        else { notePanel = p; restoring = false; keyboard?.hide() }
                    },
                    onFormat = { mark ->
                        activeBlockId?.let { formatRequest = it to mark; formatTick++ }
                    },
                    onIndent = { delta ->
                        activeBlockId?.let { id ->
                            shiftNoteIndent(blocks.toList(), id, delta)?.let { blocks.clear(); blocks.addAll(it) }
                        }
                    },
                    onUndo = { restoreDocument(undoStack, redoStack) },
                    onRedo = { restoreDocument(redoStack, undoStack) },
                    onQuick = { kind -> applyBlockKind(kind); closePanel() },
                    onImage = ::pickAttachment,
                    onDone = { notePanel = NotePanel.NONE; restoring = false; keyboard?.hide(); activeBlockId = null }
                )
                val shown = if (notePanel != NotePanel.NONE) notePanel else if (restoring) lastPanel else NotePanel.NONE
                if (shown != NotePanel.NONE) {
                    val visiblePx = minOf((panelPx - imePx).coerceAtLeast(0).toFloat(), collapseCap).toInt()
                    Box(Modifier.fillMaxWidth().height(with(density) { visiblePx.toDp() }).clipToBounds()) {
                        Box(Modifier.fillMaxWidth().requiredHeight(with(density) { panelPx.toDp() })
                            .align(Alignment.TopCenter).padding(horizontal = 12.dp)) {
                            when (shown) {
                                NotePanel.ADD -> BlockPickerPanel(turnInto = false,
                                    onPick = { kind ->
                                        if (kind == NoteBlockKind.PAGE) { notePanel = NotePanel.NONE; createPage(activeBlockId) }
                                        else { applyBlockKind(kind); closePanel() }
                                    }, onAttach = ::pickAttachment)
                                NotePanel.TURN -> BlockPickerPanel(turnInto = true,
                                    onPick = { kind -> turnActiveInto(kind); closePanel() }, onAttach = ::pickAttachment)
                                NotePanel.COLOR -> BlockColorPanel(
                                    current = blocks.firstOrNull { it.id == activeBlockId }?.color.orEmpty(),
                                    onPick = { color ->
                                        val at = blocks.indexOfFirst { it.id == activeBlockId }
                                        if (at >= 0) blocks[at] = blocks[at].copy(color = color)
                                    })
                                NotePanel.NONE -> Unit
                            }
                        }
                    }
                }
                }
            }
            // ── No visible bottom toolbar: closing the sheet saves, 태그 sits by
            // the list, check/attach live in 블록 추가. This keeps only the
            // attachment pickers (opened from 블록 추가 › 이미지·파일).
            BottomToolbar(
                onTagClick = { showTagPicker = true },
                onDone = { commitAndClose() },
                onListClick = { addBlock(NoteBlockKind.TODO, dayKey.orEmpty()) },
                requestAttachment = requestAttachment,
                onAttachmentRequestHandled = { requestAttachment = false },
                onAttachmentPicked = { entry ->
                    if (blocks.size < 1000 && (blocks.noteAttachments().size < 100 || entry in blocks.noteAttachments())) {
                        val block = attachmentNoteBlock(entry).copy(day = attachmentDay)
                        blocks.add(block)
                        focusBlockId = block.id
                    } else android.widget.Toast.makeText(context, "첨부파일 또는 블록 개수 한도에 도달했어요", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

        }
    }

    // 하위 페이지 — the same sheet, for the child; its title flows back into the block.
    openPage?.let { page ->
        TaskDetailSheet(
            task = page,
            listName = listName,
            // Back / ← from the sub-page: back to this page, nothing to remember.
            onDismiss = {
                openPage = null
                com.bettertick.ui.theme.AppPrefs.setLastSubPage(task.id, "")
            },
            // Swiped down in the sub-page: remember it, and close this page too.
            onSwipeDismiss = {
                com.bettertick.ui.theme.AppPrefs.setLastSubPage(task.id, page.id)
                // This page is hidden behind the sub-page, so it closes at once —
                // the sub-page's slide is the only animation.
                swipeCloseAll()
            },
            onUpdateTask = { updated ->
                pageScope.launch { runCatching { subPages.value.update(updated) } }
                for (i in blocks.indices) if (blocks[i].pageId == updated.id && blocks[i].text != updated.title) {
                    blocks[i] = blocks[i].copy(text = updated.title)
                }
            },
            onToggleComplete = { pageScope.launch { runCatching { subPages.value.toggle(page) } } },
            tags = tags,
            onCreateTag = onCreateTag,
            lists = lists,
            // Shown as the page path under the date instead of "/ 부모".
            breadcrumb = breadcrumb + title,
            // From a full page the sub-page opens full too; back returns to this page.
            startAsPage = isFullPage
        )
    }

    if (showDatePicker) {
        // Reuse the existing full date picker, seeding the time + duration
        // so the Duration tab reflects what's saved instead of a hardcoded
        // 14:00 default. The callback returns everything we need to rebuild
        // the timestamp and commit a new durationMinutes.
        val zoned = dueTimestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
        val initialDate = occurrenceDate?.takeIf { !repeatRule.isNullOrBlank() && dueTimestamp == task.dueDate }
            ?: zoned?.toLocalDate() ?: LocalDate.now()
        val initialTime = zoned?.toLocalTime()?.takeIf { it != LocalTime.MIDNIGHT }
        TaskDatePickerSheet(
            initialDate = initialDate,
            initialTime = initialTime,
            initialDurationMinutes = duration,
            initialRepeat = parseRepeatRule(repeatRule),
            initialRepeatEnd = parseRepeatEnd(repeatEnd),
            initialReminders = if (initialTime != null || reminders.isNotEmpty()) reminders else null,
            onRemindersChosen = { reminders = it },
            onDismiss = { showDatePicker = false },
            onDelete = {
                dueTimestamp = null
                repeatRule = null
                repeatEnd = null
                reminders = emptyList()
                showDatePicker = false
            },
            onConfirm = { date, time, durationMinutes, repeat, end ->
                // A null time means the user picked "all-day" (or cleared the
                // time row) — store midnight so toLocalTime() reads as
                // MIDNIGHT and the UI knows to hide the hour label.
                val mergedLocal = date.atTime(time ?: LocalTime.MIDNIGHT)
                val merged = mergedLocal.atZone(ZoneId.systemDefault()).toInstant()
                dueTimestamp = Timestamp(Date.from(merged))
                duration = durationMinutes
                repeatRule = repeat.toRule()
                repeatEnd = end.toPersisted()
                showDatePicker = false
            },
            onShiftPatternRequested = { date ->
                showDatePicker = false
                shiftAnchor = date
            }
        )
    }

    shiftDetection?.let { detection ->
        val shiftViewModel: ShiftRotationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        ShiftRotationDialog(
            detection = detection,
            onDismiss = {
                shiftDetection = null
                shiftAnchor = null
            },
            onApply = { start, pattern, end ->
                shiftViewModel.apply(start, pattern, detection.run, end)
                shiftDetection = null
                shiftAnchor = null
                // 이 블록이 예전 근무 반복이면 방금 잘리거나 지워졌으므로,
                // 오래된 사본을 다시 저장하지 않도록 커밋 없이 닫는다.
                val labels = pattern.filter { it.isNotBlank() }.toSet()
                if (task.isShiftRotationOf(labels)) onDismiss()
            }
        )
    }

    if (showNotionInput) {
        NotionUrlDialog(
            initialUrl = notionUrl,
            onDismiss = { showNotionInput = false },
            onConfirm = { value ->
                notionUrl = value.trim()
                showNotionInput = false
            }
        )
    }

    if (showListPicker) {
        MoveTaskListSheet(
            lists = lists,
            currentListId = listId,
            onDismiss = { showListPicker = false },
            onPick = { id -> listId = id; showListPicker = false }
        )
    }

    if (confirmDelete && onDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DarkSurface,
            title = { Text("할일을 삭제할까요?", color = TextPrimary) },
            text = { Text("'$title'을(를) 삭제합니다.", color = TextSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDelete = false
                    onDelete(task)
                    onDismiss()
                }) { Text("삭제", color = com.bettertick.ui.theme.OverdueRed) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            tags = tags,
            selectedIds = tagIds,
            onToggle = { id ->
                tagIds = if (id in tagIds) tagIds - id else tagIds + id
            },
            onCreate = onCreateTag,
            onDismiss = { showTagPicker = false }
        )
    }
}

// ── Notion card ──────────────────────────────────────────────────────────

@Composable
private fun NotionCard(
    url: String,
    onEdit: () -> Unit,
    onOpen: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .clickable { onEdit() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Minimal Notion glyph — letter "N" inside a rounded square so we
        // don't depend on a bundled brand asset.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "노션",
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (url.isNotBlank()) {
                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        // Outlined pill "보기로 가기" — tapping this opens the link and
        // short-circuits the whole-row tap (which would otherwise open the
        // edit dialog).
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, TextTertiary, RoundedCornerShape(10.dp))
                .clickable { onOpen() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "보기로 가기",
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Bottom toolbar ───────────────────────────────────────────────────────

@Composable
private fun BottomToolbar(
    onTagClick: () -> Unit,
    onListClick: () -> Unit,
    onDone: () -> Unit,
    requestAttachment: Boolean = false,
    onAttachmentRequestHandled: () -> Unit = {},
    onAttachmentPicked: (entry: String) -> Unit
) {
    var attachMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(requestAttachment) {
        if (requestAttachment) { attachMenuOpen = true; onAttachmentRequestHandled() }
    }
    val context = LocalContext.current

    // Gallery picker — PickVisualMedia grants a persistable read permission
    // so the content:// URI stays valid after process restart. No upload —
    // the URI points to the user's on-device media store entry.
    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            tryPersistRead(context, uri)
            onAttachmentPicked("image|$uri|${displayNameFor(context, uri)}")
        }
    }
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            tryPersistRead(context, uri)
            val mime = context.contentResolver.getType(uri).orEmpty()
            val kind = when {
                mime.startsWith("image/") -> "image"
                mime.startsWith("audio/") -> "audio"
                else -> "file"
            }
            onAttachmentPicked("$kind|$uri|${displayNameFor(context, uri)}")
        }
    }
    // TakePicturePreview returns only a low-res Bitmap (no URI). Save it to
    // cache so we have a stable local file:// URI that survives process
    // death — still no upload, matches the "local only" spec.
    val takePhoto = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedUri = saveCapturedBitmap(context, bitmap)
            if (savedUri != null) {
                onAttachmentPicked("image|$savedUri|사진 ${bitmap.width}×${bitmap.height}")
            }
        }
    }
    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val scanResult = com.google.mlkit.vision.documentscanner
                .GmsDocumentScanningResult.fromActivityResultIntent(data)
            scanResult?.pages?.forEachIndexed { idx, page ->
                onAttachmentPicked("image|${page.imageUri}|스캔 ${idx + 1}쪽")
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("task-detail-actions")
    ) {
        Box {
            com.bettertick.ui.components.DropdownMenu(
                expanded = attachMenuOpen,
                onDismissRequest = { attachMenuOpen = false },
                modifier = Modifier.background(DarkSurfaceVariant)
            ) {
                AttachMenuItem("사진 찍기", Icons.Outlined.PhotoCamera) {
                    attachMenuOpen = false
                    runCatching { takePhoto.launch(null) }
                        .onFailure { toastFallback(context, "사진 찍기") }
                }
                AttachMenuItem("사진 선택", Icons.Outlined.Image) {
                    attachMenuOpen = false
                    pickImage.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
                AttachMenuItem("녹음", Icons.Outlined.Mic) {
                    attachMenuOpen = false
                    // Delegate to whatever recorder the user has installed.
                    // The external recorder doesn't return a URI to us, so
                    // after recording the user can attach the resulting file
                    // via "파일" — keeps us out of the RECORD_AUDIO permission
                    // loop and avoids any Firebase upload.
                    val intent = Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                    runCatching {
                        context.startActivity(intent)
                    }.onFailure { toastFallback(context, "녹음") }
                }
                AttachMenuItem("파일", Icons.Outlined.Folder) {
                    attachMenuOpen = false
                    pickFile.launch(arrayOf("*/*"))
                }
                AttachMenuItem("문서 스캔", Icons.Outlined.DocumentScanner) {
                    attachMenuOpen = false
                    launchDocumentScan(context, scanLauncher) {
                        toastFallback(context, "문서 스캔")
                    }
                }
            }
        }
    }
}


@Composable
private fun AttachMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(label, color = TextPrimary, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, contentDescription = label, tint = TextPrimary) },
        onClick = onClick
    )
}

private fun launchDocumentScan(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>,
    onFallback: () -> Unit
) {
    val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(10)
        .setResultFormats(
            com.google.mlkit.vision.documentscanner
                .GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
        )
        .setScannerMode(
            com.google.mlkit.vision.documentscanner
                .GmsDocumentScannerOptions.SCANNER_MODE_FULL
        )
        .build()
    val client = com.google.mlkit.vision.documentscanner
        .GmsDocumentScanning.getClient(options)
    val activity = context as? android.app.Activity ?: return onFallback()
    client.getStartScanIntent(activity)
        .addOnSuccessListener { intentSender ->
            launcher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
            )
        }
        .addOnFailureListener { onFallback() }
}

private fun toastFallback(context: android.content.Context, label: String) {
    android.widget.Toast.makeText(
        context, "$label: 기기에서 실행할 앱을 찾지 못했어요", android.widget.Toast.LENGTH_SHORT
    ).show()
}

private fun displayNameFor(context: android.content.Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: uri.toString()
}

// Try to hold a persistable read grant so the URI survives process death.
// Silently no-ops for non-SAF URIs (e.g., media-store picks already grant
// long-lived access on modern Android).
private fun tryPersistRead(context: android.content.Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

// TakePicturePreview returns a tiny Bitmap only. Persist it to the app's
// cache dir so we get a stable file:// URI (no Firebase, no FileProvider
// needed for in-app Coil display).
private fun saveCapturedBitmap(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap
): Uri? = runCatching {
    val dir = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
    val file = java.io.File(dir, "photo_${System.currentTimeMillis()}.jpg")
    java.io.FileOutputStream(file).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
    }
    Uri.fromFile(file)
}.getOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipFlow(
    tags: List<Tag>,
    onRemove: (Tag) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            TagChip(tag = tag, onRemove = { onRemove(tag) })
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    val chipColor = runCatching {
        Color(android.graphics.Color.parseColor(tag.color))
    }.getOrDefault(DetailAccent)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipColor.copy(alpha = 0.32f))
            .clickable { onRemove() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.name,
            fontSize = 12.sp,
            color = TextPrimary
        )
    }
}

@Composable
internal fun TagPickerDialog(
    tags: List<Tag>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onCreate: suspend (String) -> String,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Case-insensitive contains — matches how the drawer picker filters.
    val filtered = remember(query, tags) {
        if (query.isBlank()) tags
        else tags.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val exactMatch = remember(query, tags) {
        val q = query.trim()
        q.isNotEmpty() && tags.any { it.name.equals(q, ignoreCase = true) }
    }
    val canCreate = query.trim().isNotEmpty() && !exactMatch

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(
                    text = "태그",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filtered.forEach { tag ->
                        TagPickerRow(
                            tag = tag,
                            selected = tag.id in selectedIds,
                            onClick = { onToggle(tag.id) }
                        )
                    }
                    if (filtered.isEmpty() && query.isBlank()) {
                        Text(
                            text = "아직 태그가 없어요.",
                            color = TextTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "태그를 입력하세요",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (canCreate) {
                        IconButton(
                            onClick = {
                                val name = query.trim()
                                scope.launch {
                                    val newId = onCreate(name)
                                    if (newId.isNotEmpty()) {
                                        onToggle(newId)
                                    }
                                    query = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "추가",
                                tint = DetailAccent
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "완료",
                            color = DetailAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/** Flag tint by priority level — red / orange / blue / gray for none. Keeps
 *  the same palette the matrix screens use so the cue is consistent across
 *  every surface that shows priority. */
private fun priorityColor(level: Int): Color = when (level) {
    3 -> com.bettertick.ui.theme.PriorityHigh
    2 -> com.bettertick.ui.theme.PriorityMedium
    1 -> com.bettertick.ui.theme.PriorityLow
    else -> TextSecondary
}

@Composable
private fun PriorityMenuItem(
    label: String,
    level: Int,
    current: Int,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = TextPrimary) },
        leadingIcon = {
            Icon(
                imageVector = if (level > 0) Icons.Filled.Flag else Icons.Outlined.Flag,
                contentDescription = null,
                tint = priorityColor(level)
            )
        },
        trailingIcon = {
            if (level == current) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DetailAccent
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun TagPickerRow(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dotColor = runCatching {
        Color(android.graphics.Color.parseColor(tag.color))
    }.getOrDefault(DetailAccent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = tag.name,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = DetailAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun String.ensureScheme(): String =
    if (startsWith("http://") || startsWith("https://")) this else "https://$this"

@Composable
private fun DetailMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = tint, fontSize = 15.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (tint == TextPrimary) TextSecondary else tint) },
        onClick = onClick
    )
}

/** A small visible thumb makes the bounded sheet body discoverably scrollable. */
private fun Modifier.detailScrollbar(state: ScrollState, tint: Color): Modifier = drawWithContent {
    drawContent()
    val range = state.maxValue
    if (range > 0 && range < Int.MAX_VALUE && size.height > 0f) {
        val height = size.height
        val thumb = (height * height / (height + range)).coerceIn(24.dp.toPx().coerceAtMost(height), height)
        val top = (height - thumb) * state.value / range
        drawRoundRect(tint, topLeft = Offset(size.width - 3.dp.toPx(), top),
            size = Size(2.dp.toPx(), thumb), cornerRadius = CornerRadius(1.dp.toPx()))
    }
}
