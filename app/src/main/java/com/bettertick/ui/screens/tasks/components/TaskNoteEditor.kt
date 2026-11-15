package com.bettertick.ui.screens.tasks.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.bettertick.data.model.*
import com.bettertick.ui.theme.*

/** One continuous native document. Rows are keyed by block identity, including during reorder. */
@Composable
internal fun TaskNoteEditor(
    blocks: SnapshotStateList<TaskMemo>,
    day: String?,
    focusId: String?,
    onAdd: (kind: String, day: String) -> Unit,
    onAttach: (String) -> Unit,
    onFocusBlock: (String) -> Unit,
    /** Which block is being edited (null when none) — drives the keyboard block bar. */
    onActiveBlock: (id: String, focused: Boolean) -> Unit = { _, _ -> },
    /** Inline style to apply to a block's selection (from the keyboard bar), bumped by [formatTick]. */
    formatRequest: Pair<String, InlineMark>? = null,
    formatTick: Int = 0
) {
    var addOpen by remember { mutableStateOf(false) }
    var scopeOpen by remember { mutableStateOf(false) }
    var addingDay by remember(day) { mutableStateOf(day.orEmpty()) }
    var removed by remember { mutableStateOf<Pair<Int, TaskMemo>?>(null) }
    // Caret to place after Backspace merges/deletes: (block id, offset), bumped by tick.
    var cursorRequest by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var cursorTick by remember { mutableIntStateOf(0) }
    val onPage = blocks.filter { it.day.isBlank() || day == null || it.day == day }
    val folded = collapsedNoteIds(onPage)
    val visible = onPage.filter { it.id !in folded }
    val hidden = blocks.size - onPage.size
    val headings = onPage.filter { NoteBlockKind.headingLevel(it.kind) > 0 && it.text.isNotBlank() }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (day != null) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { scopeOpen = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (addingDay.isBlank()) "새 블록: 공통" else "새 블록: ${dateLabel(day)}", fontSize = 11.sp, color = TextSecondary)
                        Icon(Icons.Outlined.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = scopeOpen, onDismissRequest = { scopeOpen = false }) {
                        DropdownMenuItem(text = { Text("공통 · 모든 반복에 표시") }, onClick = { addingDay = ""; scopeOpen = false })
                        DropdownMenuItem(text = { Text("${dateLabel(day)}에만 표시") }, onClick = { addingDay = day; scopeOpen = false })
                    }
                }
            } else Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { addOpen = true }) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp)); Text("블록 추가", fontSize = 12.sp)
                }
                DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                    NoteSlashCatalog.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { onAdd(item.kind, addingDay); addOpen = false })
                    }
                    DropdownMenuItem(text = { Text("이미지 · 파일") }, onClick = { addOpen = false; onAttach(addingDay) })
                }
            }
        }
        visible.forEachIndexed { index, block ->
            key(block.id) {
                if ((day != null || block.day.isNotBlank()) && (index == 0 || visible[index - 1].day != block.day)) {
                    Text(if (block.day.isBlank()) "공통" else dateLabel(block.day), fontSize = 10.sp,
                        color = TextTertiary, modifier = Modifier.padding(start = 32.dp, top = 6.dp, bottom = 2.dp))
                }
                NoteBlockRow(block, focusId == block.id,
                    number = if (block.kind == NoteBlockKind.NUMBERED) noteListNumber(visible, index) else 0,
                    headings = if (block.kind == NoteBlockKind.TOC) headings else emptyList(),
                    onJump = onFocusBlock,
                    formatRequest = formatRequest?.takeIf { it.first == block.id }?.second,
                    formatTick = formatTick,
                    onFocused = { f -> onActiveBlock(block.id, f) },
                    onChange = { changed -> val at = blocks.indexOfFirst { it.id == block.id }; if (at >= 0) blocks[at] = changed },
                    onMove = { direction -> val moved = blocks.toList().moveNoteBlock(block.id, direction, day); blocks.clear(); blocks.addAll(moved) },
                    onIndent = { delta ->
                        shiftNoteIndent(blocks.toList(), block.id, delta)?.let { blocks.clear(); blocks.addAll(it) }
                    },
                    canMoveUp = index > 0, canMoveDown = index < visible.lastIndex,
                    onDuplicate = {
                        val at = blocks.indexOfFirst { it.id == block.id }
                        if (at >= 0 && blocks.size < 1000) blocks.add(at + 1, block.copy(id = java.util.UUID.randomUUID().toString()))
                    },
                    onRemove = { val at = blocks.indexOfFirst { it.id == block.id }; if (at >= 0) { removed = at to block; blocks.removeAt(at) } },
                    selectedDay = day,
                    canInsert = blocks.size < 1000,
                    cursorRequest = cursorRequest?.takeIf { it.first == block.id }?.second,
                    cursorTick = cursorTick,
                    onBackspaceAtStart = {
                        val prev = visible.getOrNull(index - 1)
                        val at = blocks.indexOfFirst { it.id == block.id }
                        if (at >= 0) when (noteBackspaceAtStart(block, prev)) {
                            NoteBackspace.TO_TEXT -> blocks[at] = block.copy(kind = NoteBlockKind.TEXT, checked = false)
                            NoteBackspace.OUTDENT -> blocks[at] = block.copy(indent = block.indent - 1)
                            NoteBackspace.DELETE_FOCUS_PREVIOUS -> {
                                blocks.removeAt(at)
                                prev?.let { cursorRequest = it.id to it.text.length; cursorTick++; onFocusBlock(it.id) }
                            }
                            NoteBackspace.MERGE_INTO_PREVIOUS -> prev?.let { p ->
                                val pAt = blocks.indexOfFirst { it.id == p.id }
                                if (pAt >= 0) {
                                    blocks[pAt] = p.copy(text = p.text + block.text)
                                    blocks.removeAt(blocks.indexOfFirst { it.id == block.id })
                                    cursorRequest = p.id to p.text.length; cursorTick++; onFocusBlock(p.id)
                                }
                            }
                            NoteBackspace.REMOVE_PREVIOUS -> prev?.let { p ->
                                blocks.indexOfFirst { it.id == p.id }.takeIf { it >= 0 }?.let { blocks.removeAt(it) }
                            }
                            NoteBackspace.DELETE -> blocks.removeAt(at)
                            NoteBackspace.NONE -> Unit
                        }
                    },
                    onInsertAfter = { kind, text, indent ->
                        val at = blocks.indexOfFirst { it.id == block.id }
                        if (at >= 0 && blocks.size < 1000) {
                            val next = TaskMemo(kind = kind, text = text, day = block.day, indent = indent)
                            // A new sibling goes after this block's (possibly folded) children.
                            var insertAt = at + 1
                            if (indent <= block.indent) while (insertAt < blocks.size && blocks[insertAt].indent > block.indent) insertAt++
                            blocks.add(insertAt, next)
                            onFocusBlock(next.id)
                        }
                    })
            }
        }
        if (visible.isEmpty()) {
            TextButton(onClick = { onAdd(NoteBlockKind.TEXT, addingDay) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text("내용을 적거나 블록을 추가하세요", fontSize = 14.sp, color = TextTertiary)
            }
        }
        if (removed != null) TextButton(onClick = {
            removed?.let { (at, block) -> if (blocks.size < 1000) blocks.add(at.coerceIn(0, blocks.size), block) }
            removed = null
        }) { Text("블록 삭제 취소", fontSize = 12.sp) }
        if (hidden > 0) Text("다른 날짜의 블록 ${hidden}개는 보관 중", fontSize = 10.sp, color = TextTertiary,
            modifier = Modifier.padding(start = 32.dp, top = 8.dp))
        Spacer(Modifier.height(8.dp))
    }
}

/** Font size / line height / weight of a block's text. */
private fun blockTextStyle(kind: String): Triple<Int, Int, FontWeight> = when (NoteBlockKind.headingLevel(kind)) {
    1 -> Triple(23, 31, FontWeight.Bold)
    2 -> Triple(19, 27, FontWeight.SemiBold)
    3 -> Triple(17, 24, FontWeight.SemiBold)
    else -> if (kind == NoteBlockKind.CODE) Triple(13, 20, FontWeight.Normal) else Triple(15, 22, FontWeight.Normal)
}

@Composable
private fun NoteBlockRow(
    block: TaskMemo, focus: Boolean,
    number: Int, headings: List<TaskMemo>, onJump: (String) -> Unit,
    formatRequest: InlineMark?, formatTick: Int,
    onFocused: (Boolean) -> Unit, onChange: (TaskMemo) -> Unit,
    onMove: (Int) -> Unit, onIndent: (Int) -> Unit, canMoveUp: Boolean, canMoveDown: Boolean,
    onDuplicate: () -> Unit, onRemove: () -> Unit, selectedDay: String?,
    canInsert: Boolean,
    cursorRequest: Int?, cursorTick: Int, onBackspaceAtStart: () -> Unit,
    onInsertAfter: (kind: String, text: String, indent: Int) -> Unit
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var turnMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val media = block.attachment.isNotBlank()
    val static = media || NoteBlockKind.isStatic(block.kind)
    val isCode = block.kind == NoteBlockKind.CODE
    val isToggle = NoteBlockKind.isToggle(block.kind)
    // The field holds an invisible sentinel before the text, so Backspace at the
    // start (even in an empty block, where keyboards send nothing) deletes it and
    // we can see it. Everything outside the field works on the text without it.
    var fieldValue by remember(block.id) {
        mutableStateOf(withSentinel(TextFieldValue(block.text, TextRange(if (focus) 0 else block.text.length))))
    }
    var focused by remember { mutableStateOf(false) }
    var softBreak by remember { mutableStateOf(false) }
    var dismissedSlash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(block.text) {
        if (!block.text.startsWith("/")) dismissedSlash = null
        val shown = withoutSentinel(fieldValue)
        if (shown.text != block.text) fieldValue = withSentinel(TextFieldValue(block.text,
            TextRange(shown.selection.end.coerceIn(0, block.text.length))))
    }
    val slashQuery = block.text.takeIf { !isCode && it.startsWith("/") && '\n' !in it && it.length <= 40 }
    val slashItems = noteSlashItems(slashQuery?.drop(1).orEmpty())
    LaunchedEffect(focus) { if (focus && !static) focusRequester.requestFocus() }
    // After Backspace merged into / deleted toward this block: caret at the join.
    LaunchedEffect(cursorTick) {
        val at = cursorRequest ?: return@LaunchedEffect
        if (static) return@LaunchedEffect
        fieldValue = withSentinel(TextFieldValue(block.text, TextRange(at.coerceIn(0, block.text.length))))
        focusRequester.requestFocus()
    }
    // Bold / italic / … from the keyboard bar, applied to this block's selection.
    LaunchedEffect(formatTick) {
        val mark = formatRequest ?: return@LaunchedEffect
        if (static || isCode) return@LaunchedEffect
        val next = toggleInlineMark(withoutSentinel(fieldValue), mark)
        fieldValue = withSentinel(next)
        onChange(block.copy(text = next.text))
        focusRequester.requestFocus()
    }
    val textColor = noteColor(block.color)?.takeIf { !block.color.startsWith("bg_") } ?: TextPrimary
    val background = noteColor(block.color)?.takeIf { block.color.startsWith("bg_") }?.copy(alpha = 0.18f)
    val (fontSize, lineHeight, weight) = blockTextStyle(block.kind)
    val heading = NoteBlockKind.headingLevel(block.kind) > 0
    val primary = MaterialTheme.colorScheme.primary
    val inlineFormat = remember(primary) {
        InlineFormatTransformation(primary, Color(0x33888888), Color(0x55CA9849))
    }
    Row(Modifier.fillMaxWidth().padding(start = (block.indent.coerceIn(0, MAX_NOTE_INDENT) * 22).dp,
        top = if (heading) 6.dp else 0.dp, bottom = if (heading) 2.dp else 0.dp)
        .then(if (background != null) Modifier.clip(RoundedCornerShape(6.dp)).background(background) else Modifier),
        verticalAlignment = Alignment.Top) {
        Box {
            Box(Modifier.size(width = 32.dp, height = 26.dp).clickable { menu = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DragIndicator, "블록 메뉴", tint = TextTertiary, modifier = Modifier.size(17.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false; turnMenu = false }) {
                if (turnMenu) {
                    NoteSlashCatalog.filter { !NoteBlockKind.isStatic(it.kind) || it.kind == NoteBlockKind.DIVIDER }.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = {
                            onChange(block.copy(kind = item.kind, checked = if (NoteBlockKind.isToggle(item.kind)) true else false))
                            menu = false; turnMenu = false
                        })
                    }
                } else {
                    DropdownMenuItem(text = { Text("아래에 글 추가") }, enabled = canInsert,
                        onClick = { onInsertAfter(NoteBlockKind.TEXT, "", block.indent); menu = false })
                    if (!media) DropdownMenuItem(text = { Text("블록 종류 바꾸기 ›") }, onClick = { turnMenu = true })
                    DropdownMenuItem(text = { Text("들여쓰기") }, onClick = { onIndent(1); menu = false })
                    DropdownMenuItem(text = { Text("내어쓰기") }, enabled = block.indent > 0, onClick = { onIndent(-1); menu = false })
                    DropdownMenuItem(text = { Text("위로 이동") }, enabled = canMoveUp, onClick = { onMove(-1); menu = false })
                    DropdownMenuItem(text = { Text("아래로 이동") }, enabled = canMoveDown, onClick = { onMove(1); menu = false })
                    if (selectedDay != null || block.day.isNotBlank()) DropdownMenuItem(
                        text = { Text(if (block.day.isBlank()) "${dateLabel(selectedDay.orEmpty())}에만 표시" else "공통으로 표시") },
                        onClick = { onChange(block.copy(day = if (block.day.isBlank()) selectedDay.orEmpty() else "")); menu = false })
                    DropdownMenuItem(text = { Text("복제") }, onClick = { onDuplicate(); menu = false })
                    DropdownMenuItem(text = { Text("삭제", color = OverdueRed) }, onClick = { onRemove(); menu = false })
                }
            }
        }
        // ── Leading marker: checkbox, bullet, number, toggle arrow, callout icon ──
        val markerHeight = (lineHeight + 4).dp
        when {
            block.kind == NoteBlockKind.TODO ->
                Box(Modifier.size(width = 30.dp, height = 26.dp).clickable { onChange(block.copy(checked = !block.checked)) }, contentAlignment = Alignment.Center) {
                    TickCheckbox(0, block.checked, size = 18.dp)
                }
            block.kind == NoteBlockKind.BULLET -> Box(Modifier.size(width = 24.dp, height = 26.dp), contentAlignment = Alignment.Center) {
                Text(when (block.indent % 3) { 0 -> "•"; 1 -> "◦"; else -> "▪" }, color = textColor, fontSize = 17.sp)
            }
            block.kind == NoteBlockKind.NUMBERED -> Box(Modifier.widthIn(min = 24.dp).height(26.dp).padding(end = 4.dp), contentAlignment = Alignment.CenterEnd) {
                Text("$number.", color = textColor, fontSize = 15.sp)
            }
            isToggle -> Box(Modifier.size(width = 26.dp, height = markerHeight).clickable { onChange(block.copy(checked = !block.checked)) },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ChevronRight, if (block.checked) "접기" else "펼치기", tint = textColor,
                    modifier = Modifier.size(20.dp).rotate(if (block.checked) 90f else 0f))
            }
        }
        val quoteBar = TextSecondary
        Column(Modifier.weight(1f).then(when (block.kind) {
            NoteBlockKind.QUOTE -> Modifier.drawBehind {
                drawRect(quoteBar, topLeft = Offset(0f, 2.dp.toPx()), size = Size(3.dp.toPx(), size.height - 4.dp.toPx()))
            }.padding(start = 12.dp)
            NoteBlockKind.CALLOUT -> Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                .background(if (background == null) DarkSurfaceVariant.copy(alpha = 0.55f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp)
            NoteBlockKind.CODE -> Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 8.dp)
            else -> Modifier
        })) {
            when {
                media -> NoteMediaBlock(block)
                block.kind == NoteBlockKind.DIVIDER -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 15.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                block.kind == NoteBlockKind.TOC -> Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    if (headings.isEmpty()) Text("제목 블록을 추가하면 여기에 목차가 생겨요", fontSize = 13.sp, color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp))
                    headings.forEach { h ->
                        Text(h.text, fontSize = 14.sp, color = TextSecondary, textDecoration = TextDecoration.Underline,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clickable { onJump(h.id) }
                                .padding(start = ((NoteBlockKind.headingLevel(h.kind) - 1) * 16).dp, top = 3.dp, bottom = 3.dp))
                    }
                }
                else -> Row(verticalAlignment = Alignment.Top) {
                    if (block.kind == NoteBlockKind.CALLOUT) Text("💡", fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth()) {
                    BasicTextField(
                    value = fieldValue,
                    onValueChange = { raw ->
                        if (!raw.text.startsWith(NOTE_SENTINEL)) {
                            // Sentinel gone = Backspace at the very start of the block.
                            fieldValue = withSentinel(withoutSentinel(fieldValue).copy(selection = TextRange(0)))
                            onBackspaceAtStart()
                            return@BasicTextField
                        }
                        val previous = withoutSentinel(fieldValue)
                        val next = withoutSentinel(raw)
                        // Markdown shortcuts ("- ", "1. ", "# ", "[] ", "> ", "```", "---") in a text block.
                        val shortcut = if (block.kind.isBlank() || block.kind == NoteBlockKind.TEXT)
                            markdownShortcut(previous.text, next.text) else null
                        if (shortcut != null && next.composition == null) {
                            val (kind, rest) = shortcut
                            if (kind == NoteBlockKind.DIVIDER) {
                                fieldValue = withSentinel(TextFieldValue(""))
                                onChange(block.copy(kind = kind, text = ""))
                                if (canInsert) onInsertAfter(NoteBlockKind.TEXT, "", block.indent)
                            } else {
                                fieldValue = withSentinel(TextFieldValue(rest, TextRange(0)))
                                onChange(block.copy(kind = kind, text = rest, checked = NoteBlockKind.isToggle(kind)))
                            }
                            return@BasicTextField
                        }
                        val split = if (!softBreak && canInsert && !isCode)
                            splitNoteOnEnter(previous.text, next.text, next.selection.end,
                                next.composition?.let { it.min until it.max }) else null
                        fieldValue = withSentinel(if (split == null) next else TextFieldValue(split.first, TextRange(split.first.length)))
                        if (split != null) {
                            if (exitsOnEmptyEnter(block.kind) && split.first.isEmpty() && split.second.isEmpty()) {
                                // Empty list item + Enter: step out one level, then back to plain text.
                                onChange(if (block.indent > 0) block.copy(indent = block.indent - 1, text = "")
                                    else block.copy(kind = NoteBlockKind.TEXT, text = "", checked = false))
                            } else {
                                onChange(block.copy(text = split.first))
                                when {
                                    isToggle && block.checked -> onInsertAfter(NoteBlockKind.TEXT, split.second,
                                        (block.indent + 1).coerceAtMost(MAX_NOTE_INDENT))
                                    block.kind == NoteBlockKind.TOGGLE -> onInsertAfter(NoteBlockKind.TOGGLE, split.second, block.indent)
                                    else -> onInsertAfter(nextNoteBlockKind(block.kind), split.second, block.indent)
                                }
                            }
                        } else onChange(block.copy(text = next.text))
                    },
                    visualTransformation = if (isCode || block.kind == NoteBlockKind.EMBED) VisualTransformation.None else inlineFormat,
                    textStyle = TextStyle(
                        color = when {
                            block.checked && block.kind == NoteBlockKind.TODO -> TextSecondary
                            block.kind == NoteBlockKind.EMBED -> TextSecondary
                            else -> textColor
                        },
                        fontSize = if (block.kind == NoteBlockKind.EMBED) 13.sp else fontSize.sp,
                        lineHeight = lineHeight.sp,
                        fontWeight = weight,
                        fontFamily = if (isCode) FontFamily.Monospace else null,
                        textDecoration = if (block.checked && block.kind == NoteBlockKind.TODO) TextDecoration.LineThrough else null),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("note-block-${block.id}").focusRequester(focusRequester)
                        .onFocusChanged { if (focused != it.isFocused) { focused = it.isFocused; onFocused(it.isFocused) } }.onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter) softBreak = event.type == KeyEventType.KeyDown && event.isShiftPressed
                        false
                    }.fillMaxWidth().heightIn(min = if (heading) 34.dp else 26.dp, max = if (isCode) 480.dp else 240.dp)
                        .padding(vertical = 2.dp),
                    decorationBox = { inner ->
                        Box {
                            // Placeholder only in the block being edited (like BlockNote).
                            if (block.text.isEmpty() && focused) Text(when {
                                heading -> "제목 ${NoteBlockKind.headingLevel(block.kind)}"
                                block.kind == NoteBlockKind.TODO -> "할 일"
                                block.kind == NoteBlockKind.BULLET || block.kind == NoteBlockKind.NUMBERED -> "목록"
                                isToggle -> "토글"
                                block.kind == NoteBlockKind.QUOTE -> "인용"
                                block.kind == NoteBlockKind.CODE -> "코드"
                                block.kind == NoteBlockKind.EMBED -> "링크 붙여넣기 (YouTube · 지도 · Figma · Gist …)"
                                else -> "내용 입력, / 로 블록 선택"
                            }, color = TextTertiary, fontSize = if (block.kind == NoteBlockKind.EMBED) 13.sp else fontSize.sp,
                                fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            inner()
                        }
                    }
                )
                    DropdownMenu(expanded = slashQuery != null && slashQuery != dismissedSlash,
                        onDismissRequest = { dismissedSlash = block.text }, properties = PopupProperties(focusable = false)) {
                        if (slashItems.isEmpty()) DropdownMenuItem(text = { Text("일치하는 블록이 없어요") }, enabled = false, onClick = {})
                        slashItems.forEach { item ->
                            DropdownMenuItem(text = { Text(item.label) }, onClick = {
                                onChange(block.copy(kind = item.kind, text = "", checked = NoteBlockKind.isToggle(item.kind)))
                                dismissedSlash = block.text
                                if (NoteBlockKind.isStatic(item.kind) && canInsert) onInsertAfter(NoteBlockKind.TEXT, "", block.indent)
                                else focusRequester.requestFocus()
                            })
                        }
                    }
                    }
                    // Link card under an embed block.
                    if (block.kind == NoteBlockKind.EMBED && isWebUrl(block.text)) NoteLinkCard(block.text.trim())
                    // Links typed with [text](url): open them from here while editing.
                    if (focused) {
                        val links = remember(block.text) { if (isCode) emptyList() else inlineLinks(block.text) }
                        if (links.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            links.take(3).forEach { url ->
                                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                    .clickable { openNoteUrl(context, url) }.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Link, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(noteEmbedService(url) ?: url, fontSize = 11.sp, color = TextSecondary, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

/** Opens a web link; bare hosts get https://. */
private fun openNoteUrl(context: android.content.Context, url: String) {
    val fixed = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(url)) url else "https://$url"
    runCatching {
        val uri = Uri.parse(fixed)
        require(uri.scheme == "https" || uri.scheme == "http" || uri.scheme == "geo" || uri.scheme == "mailto" || uri.scheme == "tel")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(context, "링크를 열 수 없어요", Toast.LENGTH_SHORT).show() }
}

/** Bookmark-style card for an embed block (service name + address), opens the link. */
@Composable
private fun NoteLinkCard(url: String) {
    val context = LocalContext.current
    val service = noteEmbedService(url) ?: url
    val icon = when (service) {
        "YouTube" -> Icons.Outlined.PlayCircle
        "Google Maps" -> Icons.Outlined.Map
        "Figma" -> Icons.Outlined.Brush
        "GitHub Gist", "GitHub" -> Icons.Outlined.Code
        "Google Drive" -> Icons.Outlined.Cloud
        else -> Icons.Outlined.Public
    }
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp).clip(RoundedCornerShape(10.dp))
        .background(DarkSurfaceVariant.copy(alpha = 0.45f)).clickable { openNoteUrl(context, url) }
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(service, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, "열기", tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun NoteMediaBlock(block: TaskMemo) {
    val context = LocalContext.current
    val parts = block.attachment.split('|', limit = 3)
    val uri = parts.getOrNull(1).orEmpty()
    val name = parts.getOrNull(2).orEmpty().ifBlank { "첨부파일" }
    var preview by remember { mutableStateOf(false) }
    var failed by remember(uri) { mutableStateOf(false) }
    if (block.kind == NoteBlockKind.IMAGE) {
        // Notion style: the picture spans the full width, its height follows
        // the image's own proportions (very tall shots are capped).
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (!failed) AsyncImage(model = uri, contentDescription = name, contentScale = ContentScale.FillWidth,
                onError = { failed = true }, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(6.dp)).clickable { preview = true })
            else Text("이 기기에서 이미지를 열 수 없어요", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
        }
    } else {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .clickable {
                runCatching {
                    val parsed = Uri.parse(uri)
                    require(parsed.scheme == "content")
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(parsed, context.contentResolver.getType(parsed) ?: "*/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }.onFailure { Toast.makeText(context, "이 파일을 열 수 있는 앱을 찾지 못했어요", Toast.LENGTH_SHORT).show() }
            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AttachFile, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text(name, color = TextPrimary, fontSize = 13.sp)
        }
    }
    if (preview) Dialog(onDismissRequest = { preview = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface).padding(12.dp)) {
            AsyncImage(model = uri, contentDescription = name, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp))
            TextButton(onClick = { preview = false }, modifier = Modifier.align(Alignment.End)) { Text("닫기") }
        }
    }
}

private fun dateLabel(day: String): String = runCatching {
    java.time.LocalDate.parse(day).let { "${it.monthValue}월 ${it.dayOfMonth}일" }
}.getOrDefault(day)

/** Invisible first character of every block field — see [NoteBlockRow]. */
private const val NOTE_SENTINEL = '\u200B'

private fun TextRange.shift(by: Int, max: Int) =
    TextRange((start + by).coerceIn(0, max), (end + by).coerceIn(0, max))

/** Field value → plain block value (sentinel stripped, ranges moved left). */
private fun withoutSentinel(v: TextFieldValue): TextFieldValue {
    if (!v.text.startsWith(NOTE_SENTINEL)) return v
    val text = v.text.substring(1)
    return TextFieldValue(text, v.selection.shift(-1, text.length), v.composition?.shift(-1, text.length))
}

/** Plain block value → field value (sentinel added, caret never before it). */
private fun withSentinel(v: TextFieldValue): TextFieldValue {
    val text = NOTE_SENTINEL + v.text
    return TextFieldValue(text, v.selection.shift(1, text.length).let {
        TextRange(it.start.coerceAtLeast(1), it.end.coerceAtLeast(1))
    }, v.composition?.shift(1, text.length))
}
