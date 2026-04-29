package com.bettertick.ui.screens.tags

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Tag
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/**
 * Full-screen editor reached from the drawer's 추가 → 태그 menu (create mode)
 * or from the tag management screen / long-press 편집 action (edit mode).
 * The 가족 태그 row opens a picker sheet listing every other tag so the user
 * can group this tag under a parent. Save blocks on blank name and on
 * duplicate names (case-insensitive, trimmed).
 */
@Composable
fun AddTagScreen(
    onBack: () -> Unit,
    initialTag: Tag? = null,
    viewModel: AddTagViewModel = hiltViewModel()
) {
    val isEdit = initialTag != null
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialTag?.color) }
    var parentTagId by remember { mutableStateOf(initialTag?.parentTagId) }
    var parentPickerOpen by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val existingTags by viewModel.tags.collectAsState()
    val trimmedName = name.trim()
    val duplicateName = trimmedName.isNotBlank() && existingTags.any {
        it.id != initialTag?.id &&
            it.name.trim().equals(trimmedName, ignoreCase = true)
    }
    val canSave = trimmedName.isNotBlank() && !duplicateName

    val parentTag = existingTags.firstOrNull { it.id == parentTagId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 40.dp)
    ) {
        // Top bar — X / title / ✓
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel",
                    tint = Color.White
                )
            }
            Text(
                text = if (isEdit) "태그 편집" else "태그 추가",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                enabled = canSave,
                onClick = {
                    scope.launch {
                        if (initialTag != null) {
                            viewModel.updateTag(
                                initialTag.copy(
                                    name = trimmedName,
                                    color = selectedColor ?: initialTag.color,
                                    parentTagId = parentTagId
                                )
                            )
                        } else {
                            viewModel.createTag(
                                Tag(
                                    name = trimmedName,
                                    color = selectedColor ?: "#FF8C00",
                                    parentTagId = parentTagId
                                )
                            )
                        }
                        onBack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (canSave) Color.White else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Name field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (name.isEmpty()) {
                        Text(
                            text = "이름",
                            color = TextTertiary,
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (duplicateName) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "같은 이름의 태그가 이미 있어요.",
                color = Color(0xFFFF5D5D),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color + parent card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                text = "색상",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ColorRow(
                selected = selectedColor,
                onSelect = { selectedColor = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 가족 태그 row — acts as a chevron button that opens the picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .clickable(
                    enabled = !isEdit || initialTag?.id != null
                ) {
                    // When editing, exclude self from the picker so a tag
                    // can't become its own parent. Open unconditionally
                    // otherwise.
                    parentPickerOpen = true
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "가족 태그",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (parentTag != null) {
                Text(
                    text = parentTag.name,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = ">",
                color = TextSecondary,
                fontWeight = FontWeight.Light
            )
        }
    }

    if (parentPickerOpen) {
        ParentTagPickerDialog(
            tags = existingTags.filter { it.id != initialTag?.id },
            selectedId = parentTagId,
            onSelect = {
                parentTagId = it
                parentPickerOpen = false
            },
            onDismiss = { parentPickerOpen = false }
        )
    }
}

private val Palette = listOf(
    null,                // 'no color' / default
    "#FF5D5D",
    "#FF8C00",
    "#FFC828",
    "#C6DB3B",
    "#4CD267",
    "#3DA5F5",
    "#9C51E0"            // stand-in for the reference's rainbow slot
)

@Composable
private fun ColorRow(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Palette.forEach { hex ->
            val isSelected = hex == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (hex == null) Modifier.border(
                            1.5.dp,
                            TextTertiary,
                            CircleShape
                        ) else Modifier.background(Color(android.graphics.Color.parseColor(hex)))
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            Color(0xFF4A90E2),
                            CircleShape
                        ) else Modifier
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex == null) {
                    Text(
                        text = "⊘",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentTagPickerDialog(
    tags: List<Tag>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
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
            Column(
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    text = "가족 태그",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ParentTagOption(
                        label = "없음",
                        selected = selectedId == null,
                        onClick = { onSelect(null) }
                    )
                    tags.forEach { tag ->
                        ParentTagOption(
                            label = tag.name,
                            selected = selectedId == tag.id,
                            onClick = { onSelect(tag.id) }
                        )
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
                            color = Color(0xFF4A90E2),
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
private fun ParentTagOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF4A90E2),
                unselectedColor = TextTertiary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
