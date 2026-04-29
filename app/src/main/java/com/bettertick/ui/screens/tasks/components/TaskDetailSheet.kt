package com.bettertick.ui.screens.tasks.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.ui.components.MarkdownInlineTransformation
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.Orange
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
private val DetailAccent = Color(0xFF4A90E2)

/**
 * Bottom-sheet view of a single task. Shows the list the task belongs to,
 * its date/time line (tappable — reuses [TaskDatePickerSheet]), a big
 * title, a Notion link card, and a markdown-editable notes area.
 *
 * Edits are local until the sheet is dismissed; we fire a single
 * [onUpdateTask] when the sheet closes if anything has changed, so we
 * don't write on every keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    listName: String,
    onDismiss: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onToggleComplete: () -> Unit,
    tags: List<Tag> = emptyList(),
    onCreateTag: suspend (String) -> String = { "" }
) {
    // Local edit state — keyed by task.id so re-opening a different task
    // pulls in fresh values instead of leaking from the previous one.
    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    val attachments = remember(task.id) {
        androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(task.attachments) }
    }
    // Inline checklist/bullet editor state. When listMode != None, `items`
    // is the source of truth for the notes body; we serialize back into
    // `notes` on every mutation so commitAndClose sees the latest.
    var listMode by remember(task.id) {
        mutableStateOf(inferListMode(task.notes))
    }
    val items = remember(task.id) {
        androidx.compose.runtime.mutableStateListOf<ChecklistItem>().apply {
            addAll(parseChecklistItems(task.notes))
        }
    }
    fun syncNotesFromItems() {
        notes = serializeChecklist(items, listMode)
    }
    var notionUrl by remember(task.id) { mutableStateOf(task.notionUrl) }
    var dueTimestamp by remember(task.id) { mutableStateOf(task.dueDate) }
    var duration by remember(task.id) { mutableStateOf(task.durationMinutes) }
    var repeatRule by remember(task.id) { mutableStateOf(task.repeatRule) }
    var repeatEnd by remember(task.id) { mutableStateOf(task.repeatEnd) }
    var tagIds by remember(task.id) { mutableStateOf(task.tagIds) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showNotionInput by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var priorityMenuOpen by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    fun commitAndClose() {
        val attachmentsList = attachments.toList()
        val changed =
            title != task.title ||
                notes != task.notes ||
                notionUrl != task.notionUrl ||
                dueTimestamp != task.dueDate ||
                duration != task.durationMinutes ||
                repeatRule != task.repeatRule ||
                repeatEnd != task.repeatEnd ||
                tagIds != task.tagIds ||
                priority != task.priority ||
                attachmentsList != task.attachments
        if (changed) {
            onUpdateTask(
                task.copy(
                    title = title,
                    notes = notes,
                    notionUrl = notionUrl,
                    dueDate = dueTimestamp,
                    durationMinutes = duration,
                    repeatRule = repeatRule,
                    repeatEnd = repeatEnd,
                    tagIds = tagIds,
                    priority = priority,
                    attachments = attachmentsList
                )
            )
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { commitAndClose() },
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 8.dp)
        ) {
            // ── Header: list selector + flag + more ──────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* list picker — future */ }
                ) {
                    Text(
                        text = listName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = "리스트 선택",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                Box {
                    val flagTint = priorityColor(priority)
                    IconButton(onClick = { priorityMenuOpen = true }) {
                        Icon(
                            imageVector = if (priority > 0) Icons.Filled.Flag
                                else Icons.Outlined.Flag,
                            contentDescription = "우선순위",
                            tint = flagTint
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
                IconButton(onClick = { /* more — future */ }) {
                    Icon(
                        Icons.Outlined.MoreHoriz,
                        contentDescription = "더 보기",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Date / time row — checkbox on the left, tappable chip ────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val checkboxShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(checkboxShape)
                        .then(
                            if (task.isCompleted) {
                                Modifier.background(Orange)
                            } else {
                                Modifier.border(2.dp, TextSecondary, checkboxShape)
                            }
                        )
                        .clickable { onToggleComplete() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "완료됨",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showDatePicker = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Text(
                        text = formatDueLabel(dueTimestamp, duration),
                        fontSize = 15.sp,
                        color = DetailAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = "알림",
                        tint = DetailAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Title (editable) ─────────────────────────────────────────
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                ),
                cursorBrush = SolidColor(Orange),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Notion card ──────────────────────────────────────────────
            NotionCard(
                url = notionUrl,
                onEdit = { showNotionInput = true },
                onOpen = {
                    if (notionUrl.isNotBlank()) {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(notionUrl.ensureScheme())
                        )
                        runCatching { context.startActivity(intent) }
                    } else {
                        showNotionInput = true
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

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

            // ── Notes body ───────────────────────────────────────────────
            // Plain markdown when listMode is None, otherwise an interactive
            // checklist / bullet editor. The list-button in the bottom bar
            // cycles between the two modes and keeps `notes` in sync.
            Text(
                text = "설명",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (listMode == ListMode.None) {
                MarkdownNotesEditor(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                )
            } else {
                ChecklistEditor(
                    items = items,
                    mode = listMode,
                    onItemsMutated = { syncNotesFromItems() },
                    onExitListMode = {
                        syncNotesFromItems()
                        listMode = ListMode.None
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom toolbar ───────────────────────────────────────────
            BottomToolbar(
                onTagClick = { showTagPicker = true },
                onListClick = {
                    val next = when (listMode) {
                        ListMode.None -> ListMode.Checklist
                        ListMode.Checklist -> ListMode.Bullet
                        ListMode.Bullet -> ListMode.None
                    }
                    if (listMode == ListMode.None && next != ListMode.None) {
                        // Entering list mode from plain text — parse notes
                        // into items so existing content carries over.
                        items.clear()
                        items.addAll(parseChecklistItems(notes))
                        if (items.isEmpty()) {
                            items.add(ChecklistItem(newItemId(), "", false))
                        }
                    }
                    listMode = next
                    if (next == ListMode.None) {
                        syncNotesFromItems()
                    } else {
                        syncNotesFromItems()
                    }
                },
                onAttachmentPicked = { entry ->
                    attachments.add(entry)
                }
            )

            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AttachmentGrid(
                    entries = attachments,
                    onRemove = { idx -> if (idx in attachments.indices) attachments.removeAt(idx) }
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        // Reuse the existing full date picker, seeding the time + duration
        // so the Duration tab reflects what's saved instead of a hardcoded
        // 14:00 default. The callback returns everything we need to rebuild
        // the timestamp and commit a new durationMinutes.
        val zoned = dueTimestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
        val initialDate = zoned?.toLocalDate() ?: LocalDate.now()
        val initialTime = zoned?.toLocalTime()?.takeIf { it != LocalTime.MIDNIGHT }
        TaskDatePickerSheet(
            initialDate = initialDate,
            initialTime = initialTime,
            initialDurationMinutes = duration,
            initialRepeat = parseRepeatRule(repeatRule),
            initialRepeatEnd = parseRepeatEnd(repeatEnd),
            onDismiss = { showDatePicker = false },
            onDelete = {
                dueTimestamp = null
                repeatRule = null
                repeatEnd = null
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
                color = Color.White,
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
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Notes editor ─────────────────────────────────────────────────────────

@Composable
private fun MarkdownNotesEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transformation = remember { MarkdownInlineTransformation() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = "메모를 입력하세요 (**굵게**, *기울임*, `코드`, ~~취소선~~ 지원)",
                    fontSize = 14.sp,
                    color = TextTertiary
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(Orange),
                visualTransformation = transformation,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Bottom toolbar ───────────────────────────────────────────────────────

@Composable
private fun BottomToolbar(
    onTagClick: () -> Unit,
    onListClick: () -> Unit,
    onAttachmentPicked: (entry: String) -> Unit
) {
    var attachMenuOpen by remember { mutableStateOf(false) }
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
            val savedUri = saveBitmapToCache(context, bitmap)
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
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onTagClick) {
            Icon(
                Icons.AutoMirrored.Outlined.Label,
                contentDescription = "태그",
                tint = TextSecondary
            )
        }
        IconButton(onClick = onListClick) {
            Icon(
                Icons.AutoMirrored.Outlined.ViewList,
                contentDescription = "하위 할 일",
                tint = TextSecondary
            )
        }
        Box {
            IconButton(onClick = { attachMenuOpen = true }) {
                Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = "첨부",
                    tint = TextSecondary
                )
            }
            androidx.compose.material3.DropdownMenu(
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
        text = { Text(label, color = Color.White, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, contentDescription = label, tint = Color.White) },
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
private fun saveBitmapToCache(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap
): Uri? = runCatching {
    val dir = java.io.File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = java.io.File(dir, "photo_${System.currentTimeMillis()}.jpg")
    java.io.FileOutputStream(file).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
    }
    Uri.fromFile(file)
}.getOrNull()

// ── Attachment grid ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachmentGrid(
    entries: List<String>,
    onRemove: (index: Int) -> Unit
) {
    val context = LocalContext.current
    Text(
        text = "첨부",
        fontSize = 12.sp,
        color = TextTertiary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.forEachIndexed { idx, entry ->
            val parts = entry.split('|', limit = 3)
            val kind = parts.getOrNull(0) ?: "file"
            val uriStr = parts.getOrNull(1).orEmpty()
            val name = parts.getOrNull(2).orEmpty()
            val uri = runCatching { Uri.parse(uriStr) }.getOrNull()

            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .clickable {
                        if (uri != null) {
                            runCatching {
                                val view = Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(view)
                            }
                        }
                    }
            ) {
                if (kind == "image" && uri != null) {
                    coil.compose.AsyncImage(
                        model = uri,
                        contentDescription = name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (kind == "audio") Icons.Outlined.Mic else Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = name.ifBlank { "파일" },
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                // Remove button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .clickable { onRemove(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "삭제",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Checklist editor ─────────────────────────────────────────────────────

enum class ListMode { None, Checklist, Bullet }

data class ChecklistItem(val id: Long, val text: String, val done: Boolean)

private var checklistIdCounter = 0L
private fun newItemId(): Long {
    checklistIdCounter += 1
    return checklistIdCounter
}

private fun inferListMode(notes: String): ListMode {
    val lines = notes.split("\n").map { it.trimStart() }
    return when {
        lines.any { it.startsWith("- [ ] ") || it.startsWith("- [x] ") || it.startsWith("- [X] ") } -> ListMode.Checklist
        lines.any { it.startsWith("- ") } -> ListMode.Bullet
        else -> ListMode.None
    }
}

private fun parseChecklistItems(notes: String): List<ChecklistItem> {
    if (notes.isBlank()) return emptyList()
    return notes.split("\n").mapNotNull { raw ->
        val line = raw.trimStart()
        when {
            line.startsWith("- [ ] ") -> ChecklistItem(newItemId(), line.removePrefix("- [ ] "), false)
            line.startsWith("- [x] ") || line.startsWith("- [X] ") ->
                ChecklistItem(newItemId(), line.replaceFirst(Regex("""- \[[xX]\] """), ""), true)
            line.startsWith("- ") -> ChecklistItem(newItemId(), line.removePrefix("- "), false)
            line.isBlank() -> null
            else -> ChecklistItem(newItemId(), line, false)
        }
    }
}

private fun serializeChecklist(items: List<ChecklistItem>, mode: ListMode): String {
    return items.joinToString("\n") { item ->
        when (mode) {
            ListMode.Checklist -> if (item.done) "- [x] ${item.text}" else "- [ ] ${item.text}"
            ListMode.Bullet -> "- ${item.text}"
            ListMode.None -> item.text
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: androidx.compose.runtime.snapshots.SnapshotStateList<ChecklistItem>,
    mode: ListMode,
    onItemsMutated: () -> Unit,
    onExitListMode: () -> Unit
) {
    // Row id that should grab focus on its next composition. Set by the
    // Enter-key handler when a new row is inserted so the cursor lands on
    // the fresh row instead of staying on the previous one.
    var pendingFocusId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { idx, item ->
            androidx.compose.runtime.key(item.id) {
                ChecklistRow(
                    item = item,
                    mode = mode,
                    isLast = idx == items.lastIndex,
                    requestFocus = pendingFocusId == item.id,
                    onFocusConsumed = { pendingFocusId = null },
                    onTextChange = { newText ->
                        items[idx] = item.copy(text = newText)
                        onItemsMutated()
                    },
                    onToggleDone = {
                        items[idx] = item.copy(done = !item.done)
                        onItemsMutated()
                    },
                    onEnter = {
                        val next = ChecklistItem(newItemId(), "", false)
                        items.add(idx + 1, next)
                        pendingFocusId = next.id
                        onItemsMutated()
                    },
                    onDelete = {
                        if (items.size <= 1 && item.text.isBlank()) {
                            onExitListMode()
                        } else {
                            items.removeAt(idx)
                            onItemsMutated()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    mode: ListMode,
    isLast: Boolean,
    requestFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggleDone: () -> Unit,
    onEnter: () -> Unit,
    onDelete: () -> Unit
) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(requestFocus) {
        if (requestFocus) {
            runCatching { focusRequester.requestFocus() }
            onFocusConsumed()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Leading marker — checkbox for Checklist, dot for Bullet.
        if (mode == ListMode.Checklist) {
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(shape)
                    .then(
                        if (item.done) Modifier.background(TextTertiary)
                        else Modifier.border(2.dp, TextSecondary, shape)
                    )
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (item.done) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "완료",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TextSecondary)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (item.text.isEmpty() && isLast) {
                Text(
                    text = "\"엔터\"를 눌러 할일 생성",
                    color = TextTertiary,
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    color = if (item.done) TextSecondary else Color.White,
                    fontSize = 15.sp,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                ),
                cursorBrush = SolidColor(Orange),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onNext = { onEnter() },
                    onDone = { onEnter() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "삭제",
                tint = if (isLast && item.text.isEmpty()) OverdueRed else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    // Hairline divider under each row to match the screenshot's underlined
    // field look.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TextTertiary.copy(alpha = 0.25f))
    )
}

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
            color = Color.White
        )
    }
}

@Composable
private fun TagPickerDialog(
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
                    color = Color.White,
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
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Orange),
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
    3 -> Color(0xFFFF5D5D)
    2 -> Color(0xFFFF8C00)
    1 -> Color(0xFF3DA5F5)
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
        text = { Text(label, color = Color.White) },
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
            color = Color.White,
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

/** "오늘, 4월 20일, 오전 10:00 - 오전 11:45" style label. Falls back to
 *  "날짜 설정" when the task has no due date yet. End time is derived from
 *  [durationMinutes]; a zero/negative duration collapses to just the start. */
private fun formatDueLabel(ts: Timestamp?, durationMinutes: Int): String {
    if (ts == null) return "날짜 설정"
    val instant = ts.toDate().toInstant()
    val zoned = instant.atZone(ZoneId.systemDefault())
    val date = zoned.toLocalDate()
    val time = zoned.toLocalTime()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dayLabel = when (date) {
        today -> "오늘"
        tomorrow -> "내일"
        else -> null
    }
    val datePart = "${date.monthValue}월 ${date.dayOfMonth}일"
    val prefix = if (dayLabel != null) "$dayLabel, $datePart" else datePart

    val hasTime = time != LocalTime.MIDNIGHT
    if (!hasTime) return prefix
    val startLabel = formatKorean12h(time)
    return if (durationMinutes > 0) {
        val endLabel = formatKorean12h(time.plusMinutes(durationMinutes.toLong()))
        "$prefix, $startLabel - $endLabel"
    } else {
        "$prefix, $startLabel"
    }
}

private fun formatKorean12h(time: LocalTime): String {
    val ampm = if (time.hour < 12) "오전" else "오후"
    val h = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%s %d:%02d".format(ampm, h, time.minute)
}

private fun String.ensureScheme(): String =
    if (startsWith("http://") || startsWith("https://")) this else "https://$this"
