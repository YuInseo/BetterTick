package com.bettertick.ui.screens.tasks.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMoveRtl
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bettertick.data.model.Task
import com.bettertick.ui.theme.DarkCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Task row with swipe-left-to-reveal action buttons (move / delete /
 * reschedule). The row itself is the existing [TaskItem]; this wrapper
 * adds a 3-button action strip underneath that is uncovered as the row
 * translates horizontally.
 *
 * Drag is implemented via a plain [Animatable] + `detectHorizontalDragGestures`
 * rather than `anchoredDraggable` so we can stay on stable Foundation APIs.
 * The row snaps to one of two positions on release: closed (0) or fully
 * revealed (-actionsWidth). A release past ~40% of the reveal distance
 * counts as "open".
 */
@Composable
fun SwipeableTaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier,
    overrideDate: LocalDate? = null,
    listName: String = "기본함",
    resolvedTags: List<com.bettertick.data.model.Tag> = emptyList()
) {
    val actionWidth = 72.dp
    val actionsWidthPx = with(LocalDensity.current) { (actionWidth * 3).toPx() }

    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // No background on the outer container — it inherits whatever the parent
    // (DarkCard panel) is painting, so the row blends in when closed. Only
    // the foreground layer needs a solid background, and it matches DarkCard
    // so nothing visibly darkens against the panel.
    Box(modifier = modifier.fillMaxWidth()) {
        // Action strip sits underneath the row, filling the Box so it
        // matches the TaskItem's height. matchParentSize() is required —
        // fillMaxHeight() on the first child of an unbounded parent (LazyColumn
        // items have Infinity maxHeight) collapses to the Row's intrinsic
        // content height (≈ icon size), making the reveal look thin. With
        // matchParentSize(), the Row measures against the parent Box's final
        // size (which is driven by the TaskItem sibling).
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            SwipeActionButton(
                icon = Icons.Outlined.DriveFileMoveRtl,
                background = ActionBlue,
                width = actionWidth,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onMove()
                }
            )
            SwipeActionButton(
                icon = Icons.Outlined.Delete,
                background = ActionRed,
                width = actionWidth,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onDelete()
                }
            )
            SwipeActionButton(
                icon = Icons.Outlined.CalendarMonth,
                background = ActionOrange,
                width = actionWidth,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onReschedule()
                }
            )
        }

        // Foreground row — translates via offset. Background matches the
        // panel card so the row looks continuous with the surrounding list
        // (previously painted DarkBackground, which showed as a black block
        // over the dark-gray panel).
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(DarkCard)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val snapTo = if (offset.value < -actionsWidthPx * 0.4f) {
                                    -actionsWidthPx
                                } else {
                                    0f
                                }
                                offset.animateTo(snapTo)
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo(
                                    (offset.value + dragAmount)
                                        .coerceIn(-actionsWidthPx, 0f)
                                )
                            }
                        }
                    )
                }
        ) {
            TaskItem(
                task = task,
                onToggleComplete = onToggleComplete,
                onClick = onClick,
                overrideDate = overrideDate,
                listName = listName,
                resolvedTags = resolvedTags
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    background: Color,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    // All three buttons are flush rectangles. The trailing-edge rounding
    // that used to live here made the orange pill look detached on a dark
    // card; the reference shows a flat action strip.
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

private val ActionBlue = Color(0xFF2F7CF6)
private val ActionRed = Color(0xFFE5453A)
private val ActionOrange = Color(0xFFFF9500)
