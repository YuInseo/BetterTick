package com.bettertick.ui.screens.tasks.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PushPin
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
import com.bettertick.ui.screens.tasks.SwipeAction
import com.bettertick.ui.screens.tasks.TaskListPrefs
import com.bettertick.ui.theme.DarkBackground
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Task row with TickTick-style swipe buttons on both sides. Which buttons
 * appear is set in 설정 > 스와이프 동작 ([TaskListPrefs.swipeRight] /
 * [TaskListPrefs.swipeLeft]); an action whose callback is null is skipped.
 *
 * Drag is a plain [Animatable] + `detectHorizontalDragGestures`. The row
 * snaps closed, or fully open on the dragged side when released past 40%.
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
    resolvedTags: List<com.bettertick.data.model.Tag> = emptyList(),
    /** Fill behind the sliding row — page background in plain lists
     *  (TickTick style), card color when the row sits inside a card. */
    rowBackground: Color = DarkBackground,
    onLongClick: (() -> Unit)? = null,
    onPin: (() -> Unit)? = null,
    onWontDo: (() -> Unit)? = null
) {
    fun handler(a: SwipeAction): (() -> Unit)? = when (a) {
        SwipeAction.NONE -> null
        SwipeAction.COMPLETE -> onToggleComplete
        SwipeAction.DATE -> onReschedule
        SwipeAction.MOVE -> onMove
        SwipeAction.DELETE -> onDelete
        SwipeAction.PIN -> onPin
        SwipeAction.WONT_DO -> onWontDo
    }
    val startActions = TaskListPrefs.swipeRight.filter { handler(it) != null }
    val endActions = TaskListPrefs.swipeLeft.filter { handler(it) != null }

    val actionWidth = 72.dp
    val density = LocalDensity.current
    val startWidthPx = with(density) { (actionWidth * startActions.size).toPx() }
    val endWidthPx = with(density) { (actionWidth * endActions.size).toPx() }

    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()) {
        // Button strips under the row; matchParentSize() makes them as tall
        // as the TaskItem sibling (fillMaxHeight collapses inside LazyColumn).
        Row(modifier = Modifier.matchParentSize()) {
            startActions.forEach { a ->
                SwipeActionButton(
                    icon = a.icon(task), background = a.color(), width = actionWidth,
                    onClick = {
                        scope.launch { offset.animateTo(0f) }
                        handler(a)?.invoke()
                    }
                )
            }
            Spacer(Modifier.weight(1f))
            endActions.forEach { a ->
                SwipeActionButton(
                    icon = a.icon(task), background = a.color(), width = actionWidth,
                    onClick = {
                        scope.launch { offset.animateTo(0f) }
                        handler(a)?.invoke()
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(rowBackground)
                .pointerInput(startWidthPx, endWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val v = offset.value
                                val snapTo = when {
                                    v > 0 && v > startWidthPx * 0.4f -> startWidthPx
                                    v < 0 && v < -endWidthPx * 0.4f -> -endWidthPx
                                    else -> 0f
                                }
                                offset.animateTo(snapTo)
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-endWidthPx, startWidthPx))
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
                resolvedTags = resolvedTags,
                onLongClick = onLongClick
            )
        }
    }
}

private fun SwipeAction.icon(task: Task): ImageVector = when (this) {
    SwipeAction.COMPLETE -> Icons.Outlined.Check
    SwipeAction.DATE -> Icons.Outlined.CalendarMonth
    SwipeAction.MOVE -> Icons.Outlined.DriveFileMoveRtl
    SwipeAction.PIN -> if (task.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin
    SwipeAction.WONT_DO -> Icons.Outlined.Block
    SwipeAction.DELETE -> Icons.Outlined.Delete
    SwipeAction.NONE -> Icons.Outlined.Check
}

private fun SwipeAction.color(): Color = when (this) {
    SwipeAction.COMPLETE -> Color(0xFF35B36A)
    SwipeAction.DATE -> ActionOrange
    SwipeAction.MOVE -> ActionBlue
    SwipeAction.PIN -> Color(0xFF8E6CEF)
    SwipeAction.WONT_DO -> Color(0xFF8E8E93)
    SwipeAction.DELETE -> ActionRed
    SwipeAction.NONE -> Color.Transparent
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

private val ActionBlue = Color(0xFF4772FA)
private val ActionRed = Color(0xFFE5453A)
private val ActionOrange = Color(0xFFFAA80C)
