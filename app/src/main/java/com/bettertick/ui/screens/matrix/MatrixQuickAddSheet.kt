package com.bettertick.ui.screens.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import com.bettertick.data.model.QuadrantConfig
import com.bettertick.ui.components.MarkdownInlineTransformation
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

/**
 * Quick-add sheet shown on the Matrix tab — title + description + a quadrant
 * selector chip. Send stamps the new task with the selected quadrant's
 * facets (priority / listId / tagIds / dueDate) so it lands where the user
 * picked on the very next render of the matrix.
 *
 * The chip's "..." menu opens a popup listing all four quadrants so the user
 * can change the target without closing the sheet.
 */
@Composable
fun MatrixQuickAddSheet(
    onDismiss: () -> Unit,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val quadrants = config.quadrants

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedId by remember(quadrants) {
        mutableStateOf(quadrants.firstOrNull()?.id ?: "I")
    }
    var menuOpen by remember { mutableStateOf(false) }

    val selected = quadrants.firstOrNull { it.id == selectedId }
        ?: quadrants.firstOrNull()
        ?: return

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Title — BasicTextField so the MarkdownInlineTransformation can style
        // #tag / **bold** etc. the same way the task input sheet does.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (title.isEmpty()) {
                Text(
                    text = "#개발",
                    color = TextTertiary,
                    fontSize = 16.sp
                )
            }
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Orange),
                visualTransformation = remember { MarkdownInlineTransformation() },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Description — single-line text field; unbounded here, taller inside
        // the full detail sheet.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (description.isEmpty()) {
                Text(
                    text = "설명",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = description,
                onValueChange = { description = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Orange),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            QuadrantChip(
                quadrant = selected,
                onMenuClick = { menuOpen = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            // Send — disabled until a title is present. Matches the blue
            // paper-plane in the mock.
            val canSend = title.isNotBlank()
            IconButton(
                onClick = {
                    viewModel.createTaskInQuadrant(title, description, selected)
                    onDismiss()
                },
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Color(0xFF4A90E2)
                        else Color(0xFF4A90E2).copy(alpha = 0.4f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "추가",
                    tint = Color.White
                )
            }
        }

        // Quadrant picker popup — anchored to the chip via the surrounding
        // Box. Selecting a row switches the target without dismissing.
        Box(modifier = Modifier.fillMaxWidth()) {
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(DarkSurfaceVariant)
            ) {
                quadrants.forEach { q ->
                    val accent = runCatching {
                        Color(android.graphics.Color.parseColor(q.colorHex))
                    }.getOrDefault(Orange)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = q.nameEn.ifEmpty { q.nameKo },
                                color = Color.White,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = q.id,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        onClick = {
                            selectedId = q.id
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuadrantChip(
    quadrant: QuadrantConfig,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(quadrant.colorHex))
    }.getOrDefault(Orange)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onMenuClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quadrant.id,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = quadrant.nameEn.ifEmpty { quadrant.nameKo },
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "쿼드런트 선택",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}
