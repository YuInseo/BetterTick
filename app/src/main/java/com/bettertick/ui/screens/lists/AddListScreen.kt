package com.bettertick.ui.screens.lists

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.TaskList
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/**
 * Full-screen editor reached from the drawer's 추가 → 목록 menu (create mode)
 * or from a list's long-press 편집 action (edit mode). When [initialList] is
 * non-null the form is pre-filled and a 삭제 row appears. Save is disabled
 * until the name is non-blank — no point letting Firestore write a nameless
 * list.
 */
@Composable
fun AddListScreen(
    onBack: () -> Unit,
    initialList: TaskList? = null,
    onDelete: () -> Unit = {},
    viewModel: AddListViewModel = hiltViewModel()
) {
    val isEdit = initialList != null
    var name by remember { mutableStateOf(initialList?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialList?.color) }
    var viewType by remember { mutableStateOf(initialList?.viewType ?: "list") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Watch the full list collection so we can block a save that would
    // create a second list with the same (trimmed, case-insensitive) name.
    // In edit mode the current list is excluded so keeping the existing
    // name stays valid.
    val existingLists by viewModel.lists.collectAsState()
    val trimmedName = name.trim()
    val duplicateName = trimmedName.isNotBlank() && existingLists.any {
        it.id != initialList?.id &&
            it.name.trim().equals(trimmedName, ignoreCase = true)
    }
    val canSave = trimmedName.isNotBlank() && !duplicateName

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
                text = if (isEdit) "목록 편집" else "목록 추가",
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
                        if (isEdit && initialList != null) {
                            viewModel.updateList(
                                initialList.copy(
                                    name = trimmedName,
                                    color = selectedColor ?: initialList.color,
                                    viewType = viewType
                                )
                            )
                        } else {
                            viewModel.createList(
                                TaskList(
                                    name = trimmedName,
                                    color = selectedColor ?: "#FF8C00",
                                    viewType = viewType,
                                    sortOrder = System.currentTimeMillis()
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
                    imageVector = Icons.Outlined.Reorder,
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
                text = "같은 이름의 목록이 이미 있어요.",
                color = Color(0xFFFF5D5D),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color + view-type card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                text = "목록 색상",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ColorRow(
                selected = selectedColor,
                onSelect = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "보기 유형",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ViewTypeRow(
                selected = viewType,
                onSelect = { viewType = it }
            )
        }
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
                    // Empty-color slot: diagonal line to signal "no color".
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
private fun ViewTypeRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewTypeCard(
            key = "list",
            label = "목록",
            icon = Icons.Default.Reorder,
            selected = selected == "list",
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ViewTypeCard(
            key = "kanban",
            label = "칸반",
            icon = Icons.Default.ViewKanban,
            selected = selected == "kanban",
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ViewTypeCard(
            key = "timetable",
            label = "시간표",
            icon = Icons.Default.CalendarViewMonth,
            selected = selected == "timetable",
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViewTypeCard(
    key: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF4A90E2)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else TextTertiary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(DarkSurface)
                .clickable { onSelect(key) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (selected) accent else TextSecondary,
            fontSize = 13.sp
        )
    }
}
