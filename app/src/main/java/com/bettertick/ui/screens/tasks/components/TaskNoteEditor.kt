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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    onFocusBlock: (String) -> Unit
) {
    var addOpen by remember { mutableStateOf(false) }
    var scopeOpen by remember { mutableStateOf(false) }
    var addingDay by remember(day) { mutableStateOf(day.orEmpty()) }
    var removed by remember { mutableStateOf<Pair<Int, TaskMemo>?>(null) }
    val visible = blocks.filter { it.day.isBlank() || day == null || it.day == day }
    val hidden = blocks.size - visible.size
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
                    listOf(NoteBlockKind.TEXT to "글", NoteBlockKind.HEADING to "제목",
                        NoteBlockKind.TODO to "체크리스트", NoteBlockKind.DIVIDER to "구분선").forEach { (kind, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onAdd(kind, addingDay); addOpen = false })
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
                    onChange = { changed -> val at = blocks.indexOfFirst { it.id == block.id }; if (at >= 0) blocks[at] = changed },
                    onMove = { direction -> val moved = blocks.toList().moveNoteBlock(block.id, direction, day); blocks.clear(); blocks.addAll(moved) },
                    canMoveUp = index > 0, canMoveDown = index < visible.lastIndex,
                    onDuplicate = {
                        val at = blocks.indexOfFirst { it.id == block.id }
                        if (at >= 0 && blocks.size < 1000) blocks.add(at + 1, block.copy(id = java.util.UUID.randomUUID().toString()))
                    },
                    onRemove = { val at = blocks.indexOfFirst { it.id == block.id }; if (at >= 0) { removed = at to block; blocks.removeAt(at) } },
                    selectedDay = day,
                    canInsert = blocks.size < 1000,
                    onInsertAfter = { kind, text ->
                        val at = blocks.indexOfFirst { it.id == block.id }
                        if (at >= 0 && blocks.size < 1000) {
                            val next = TaskMemo(kind = kind, text = text, day = block.day)
                            blocks.add(at + 1, next)
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

@Composable
private fun NoteBlockRow(
    block: TaskMemo, focus: Boolean, onChange: (TaskMemo) -> Unit,
    onMove: (Int) -> Unit, canMoveUp: Boolean, canMoveDown: Boolean,
    onDuplicate: () -> Unit, onRemove: () -> Unit, selectedDay: String?,
    canInsert: Boolean, onInsertAfter: (kind: String, text: String) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val media = block.attachment.isNotBlank()
    var fieldValue by remember(block.id) { mutableStateOf(TextFieldValue(block.text, TextRange(if (focus) 0 else block.text.length))) }
    var softBreak by remember { mutableStateOf(false) }
    var dismissedSlash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(block.text) {
        if (!block.text.startsWith("/")) dismissedSlash = null
        if (fieldValue.text != block.text) fieldValue = TextFieldValue(block.text,
            TextRange(fieldValue.selection.end.coerceIn(0, block.text.length)))
    }
    val slashQuery = block.text.takeIf { it.startsWith("/") && '\n' !in it && it.length <= 40 }
    val slashItems = noteSlashItems(slashQuery?.drop(1).orEmpty())
    LaunchedEffect(focus) { if (focus && !media && block.kind != NoteBlockKind.DIVIDER) focusRequester.requestFocus() }
    Row(Modifier.fillMaxWidth().padding(top = if (block.kind == NoteBlockKind.HEADING) 6.dp else 0.dp,
        bottom = if (block.kind == NoteBlockKind.HEADING) 2.dp else 0.dp), verticalAlignment = Alignment.Top) {
        Box {
            Box(Modifier.size(width = 32.dp, height = 26.dp).clickable { menu = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DragIndicator, "블록 메뉴", tint = TextTertiary, modifier = Modifier.size(17.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("아래에 글 추가") }, enabled = canInsert,
                    onClick = { onInsertAfter(NoteBlockKind.TEXT, ""); menu = false })
                if (!media) {
                    listOf(NoteBlockKind.TEXT to "글로 바꾸기", NoteBlockKind.HEADING to "제목으로 바꾸기",
                        NoteBlockKind.TODO to "체크리스트로 바꾸기", NoteBlockKind.DIVIDER to "구분선으로 바꾸기").forEach { (kind, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onChange(block.copy(kind = kind)); menu = false })
                    }
                }
                DropdownMenuItem(text = { Text("위로 이동") }, enabled = canMoveUp, onClick = { onMove(-1); menu = false })
                DropdownMenuItem(text = { Text("아래로 이동") }, enabled = canMoveDown, onClick = { onMove(1); menu = false })
                if (selectedDay != null || block.day.isNotBlank()) DropdownMenuItem(
                    text = { Text(if (block.day.isBlank()) "${dateLabel(selectedDay.orEmpty())}에만 표시" else "공통으로 표시") },
                    onClick = { onChange(block.copy(day = if (block.day.isBlank()) selectedDay.orEmpty() else "")); menu = false })
                DropdownMenuItem(text = { Text("복제") }, onClick = { onDuplicate(); menu = false })
                DropdownMenuItem(text = { Text("삭제", color = OverdueRed) }, onClick = { onRemove(); menu = false })
            }
        }
        if (block.kind == NoteBlockKind.TODO) {
            Box(Modifier.size(width = 30.dp, height = 26.dp).clickable { onChange(block.copy(checked = !block.checked)) }, contentAlignment = Alignment.Center) {
                TickCheckbox(0, block.checked, size = 18.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            when {
                media -> NoteMediaBlock(block)
                block.kind == NoteBlockKind.DIVIDER -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 15.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                else -> Box(Modifier.fillMaxWidth()) {
                    BasicTextField(
                    value = fieldValue,
                    onValueChange = { next ->
                        val split = if (!softBreak && canInsert)
                            splitNoteOnEnter(fieldValue.text, next.text, next.selection.end,
                                next.composition?.let { it.min until it.max }) else null
                        fieldValue = if (split == null) next else TextFieldValue(split.first, TextRange(split.first.length))
                        if (split != null) {
                            if (block.kind == NoteBlockKind.TODO && split.first.isEmpty() && split.second.isEmpty()) {
                                onChange(block.copy(kind = NoteBlockKind.TEXT, text = "", checked = false))
                            } else {
                                onChange(block.copy(text = split.first))
                                onInsertAfter(nextNoteBlockKind(block.kind), split.second)
                            }
                        } else onChange(block.copy(text = next.text))
                    },
                    textStyle = TextStyle(
                        color = if (block.checked && block.kind == NoteBlockKind.TODO) TextSecondary else TextPrimary,
                        fontSize = if (block.kind == NoteBlockKind.HEADING) 21.sp else 15.sp,
                        lineHeight = if (block.kind == NoteBlockKind.HEADING) 30.sp else 22.sp,
                        fontWeight = if (block.kind == NoteBlockKind.HEADING) FontWeight.SemiBold else FontWeight.Normal,
                        textDecoration = if (block.checked && block.kind == NoteBlockKind.TODO) TextDecoration.LineThrough else null),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("note-block-${block.id}").focusRequester(focusRequester).onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter) softBreak = event.type == KeyEventType.KeyDown && event.isShiftPressed
                        false
                    }.fillMaxWidth().heightIn(min = if (block.kind == NoteBlockKind.HEADING) 36.dp else 26.dp, max = 240.dp)
                        .padding(vertical = 2.dp),
                    decorationBox = { inner ->
                        Box {
                            if (block.text.isEmpty()) Text(when (block.kind) {
                                NoteBlockKind.HEADING -> "제목"; NoteBlockKind.TODO -> "할 일"; else -> "내용 입력, / 로 블록 선택"
                            }, color = TextTertiary, fontSize = if (block.kind == NoteBlockKind.HEADING) 21.sp else 15.sp)
                            inner()
                        }
                    }
                )
                    DropdownMenu(expanded = slashQuery != null && slashQuery != dismissedSlash,
                        onDismissRequest = { dismissedSlash = block.text }, properties = PopupProperties(focusable = false)) {
                        if (slashItems.isEmpty()) DropdownMenuItem(text = { Text("일치하는 블록이 없어요") }, enabled = false, onClick = {})
                        slashItems.forEach { item ->
                            DropdownMenuItem(text = { Text(item.label) }, onClick = {
                                onChange(block.copy(kind = item.kind, text = "", checked = false))
                                dismissedSlash = block.text
                                if (item.kind == NoteBlockKind.DIVIDER && canInsert) onInsertAfter(NoteBlockKind.TEXT, "")
                                else focusRequester.requestFocus()
                            })
                        }
                    }
                }
            }
        }
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
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            if (!failed) AsyncImage(model = uri, contentDescription = name, contentScale = ContentScale.Fit,
                onError = { failed = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 240.dp)
                    .clip(RoundedCornerShape(10.dp)).clickable { preview = true })
            else Text("이 기기에서 이미지를 열 수 없어요", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            Text(name, fontSize = 10.sp, color = TextTertiary, modifier = Modifier.padding(top = 4.dp))
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
