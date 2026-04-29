package com.bettertick.ui.screens.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.QuadrantConfig
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.priorityLabel
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

private val MatrixAccent = Color(0xFF4A90E2)

/**
 * Per-quadrant filter editor. Five rows — 목록 / 태그 / 날짜 / 우선 순위 /
 * 작업 유형 — each opens a picker sheet that overwrites that facet in the
 * draft config. ✓ in the top-right commits the draft through the VM; the
 * back arrow discards the draft.
 */
@Composable
fun QuadrantEditScreen(
    quadrantId: String,
    onBack: () -> Unit,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val original = config.quadrants.firstOrNull { it.id == quadrantId }
        ?: return // Quadrant id isn't in the config (shouldn't happen) — bail.

    var draft by remember(quadrantId) { mutableStateOf(original) }
    var picker by remember { mutableStateOf<Picker?>(null) }

    val accent = runCatching {
        Color(android.graphics.Color.parseColor(draft.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "행렬 편집",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                viewModel.saveQuadrant(draft)
                onBack()
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.White
                )
            }
        }

        // Quadrant name header — Roman badge + English name, non-editable
        // label matching the reference mock.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = draft.id,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = draft.nameEn.ifEmpty { draft.nameKo },
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
        ) {
            SettingRow(
                label = "목록",
                value = listsValueLabel(draft.listIds, lists),
                onClick = { picker = Picker.Lists }
            )
            Divider()
            SettingRow(
                label = "태그",
                value = QuadrantConfig.TagMode.from(draft.tagMode).label,
                onClick = { picker = Picker.Tag }
            )
            Divider()
            SettingRow(
                label = "날짜",
                value = QuadrantConfig.DateMode.from(draft.dateMode).label,
                onClick = { picker = Picker.Date }
            )
            Divider()
            SettingRow(
                label = "우선 순위",
                value = if (draft.priority in 0..3) draft.priorityLabel() else "전체",
                onClick = { picker = Picker.Priority }
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
        ) {
            SettingRow(
                label = "작업 유형",
                value = QuadrantConfig.TypeMode.from(draft.typeMode).label,
                onClick = { picker = Picker.Type }
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = {
                draft = original.copy(
                    listIds = emptyList(),
                    tagMode = QuadrantConfig.TagMode.Any.key,
                    tagIds = emptyList(),
                    dateMode = QuadrantConfig.DateMode.All.key,
                    priority = -1,
                    typeMode = QuadrantConfig.TypeMode.All.key
                )
            }) {
                Text(
                    text = "기본값으로 재설정",
                    color = MatrixAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    when (picker) {
        Picker.Lists -> ListsPickerDialog(
            lists = lists,
            selected = draft.listIds,
            onConfirm = {
                draft = draft.copy(listIds = it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        Picker.Tag -> SingleChoiceDialog(
            title = "태그",
            options = QuadrantConfig.TagMode.entries.map { it.key to it.label },
            selectedKey = draft.tagMode,
            onSelect = {
                draft = draft.copy(tagMode = it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        Picker.Date -> SingleChoiceDialog(
            title = "날짜",
            options = QuadrantConfig.DateMode.entries.map { it.key to it.label },
            selectedKey = draft.dateMode,
            onSelect = {
                draft = draft.copy(dateMode = it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        Picker.Priority -> SingleChoiceDialog(
            title = "우선 순위",
            options = listOf(
                "-1" to "전체",
                "3" to "높은 우선도",
                "2" to "중간 우선도",
                "1" to "낮은 우선도",
                "0" to "우선도 없음"
            ),
            selectedKey = draft.priority.toString(),
            onSelect = {
                draft = draft.copy(priority = it.toIntOrNull() ?: -1)
                picker = null
            },
            onDismiss = { picker = null }
        )
        Picker.Type -> SingleChoiceDialog(
            title = "작업 유형",
            options = QuadrantConfig.TypeMode.entries.map { it.key to it.label },
            selectedKey = draft.typeMode,
            onSelect = {
                draft = draft.copy(typeMode = it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        null -> Unit
    }
}

private enum class Picker { Lists, Tag, Date, Priority, Type }

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = ">",
            color = TextSecondary,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DarkSurface)
    )
}

private fun listsValueLabel(ids: List<String>, lists: List<TaskList>): String {
    if (ids.isEmpty()) return "전체"
    val names = ids.mapNotNull { id -> lists.firstOrNull { it.id == id }?.name }
    return if (names.size <= 2) names.joinToString(", ")
    else "${names.take(2).joinToString(", ")} 외 ${names.size - 2}"
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(key) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = key == selectedKey,
                                onClick = { onSelect(key) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MatrixAccent,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "취소",
                            color = MatrixAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListsPickerDialog(
    lists: List<TaskList>,
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // "전체" = empty list. Ticking it clears specific selections. Ticking any
    // specific list implicitly clears 전체.
    var draft by remember { mutableStateOf(selected) }
    val allSelected = draft.isEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "필터 보기 범위",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { draft = emptyList() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "전체",
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = allSelected,
                            onClick = { draft = emptyList() },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MatrixAccent,
                                unselectedColor = TextTertiary
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 20.dp)
                    )
                    lists.forEach { list ->
                        val checked = list.id in draft
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    draft = if (checked) draft - list.id
                                    else draft + list.id
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = list.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            CheckboxTile(checked = checked)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "취소",
                            color = MatrixAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { onConfirm(draft) }) {
                        Text(
                            text = "확인",
                            color = MatrixAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxTile(checked: Boolean) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(MatrixAccent)
                else Modifier.border(1.5.dp, TextTertiary, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
