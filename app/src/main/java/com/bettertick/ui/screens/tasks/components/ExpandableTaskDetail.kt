package com.bettertick.ui.screens.tasks.components

import android.view.WindowManager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One window and one editor composition for the entire drawer ↔ page transition. */
@Composable
internal fun ExpandableTaskDetail(onClose: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable () -> Unit) {
    var position by remember { mutableStateOf(DetailDragPosition()) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val latestClose by rememberUpdatedState(onClose)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Dialog(onDismissRequest = { latestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        // Draw our own scrim so it fades continuously with the sheet instead of swapping windows.
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val oldDim = window?.attributes?.dimAmount ?: 0f
            val oldInputMode = window?.attributes?.softInputMode ?: 0
            window?.setDimAmount(0f)
            // A full-height edge-to-edge window must not be panned/resized by the WM as well as Compose.
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            onDispose { window?.setDimAmount(oldDim); window?.setSoftInputMode(oldInputMode) }
        }
        val expansion = position.expansion.coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.42f * (1f - expansion)))
                .clickable(enabled = expansion < 0.99f) { latestClose() })
            BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                val heightPx = with(density) { maxHeight.toPx() }
                val collapsedHeight = maxHeight * 0.78f
                val expandRange = with(density) { (maxHeight - collapsedHeight).toPx() }.coerceAtLeast(1f)
                val sheetHeight = collapsedHeight + (maxHeight - collapsedHeight) * expansion

                fun settle(target: DetailSnapTarget, velocity: Float = 0f) {
                    settleJob?.cancel()
                    settleJob = scope.launch {
                        if (target == DetailSnapTarget.CLOSED) {
                            animate(initialValue = position.dismissOffset, targetValue = heightPx,
                                initialVelocity = velocity.coerceAtLeast(0f),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)) { value, _ ->
                                position = DetailDragPosition(dismissOffset = value.coerceAtLeast(0f))
                            }
                            latestClose()
                        } else {
                            val top = (1f - position.expansion) * expandRange + position.dismissOffset
                            val targetTop = if (target == DetailSnapTarget.PAGE) 0f else expandRange
                            animate(initialValue = top, targetValue = targetTop, initialVelocity = velocity,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)) { value, _ ->
                                position = dragTaskDetail(DetailDragPosition(expansion = 1f), value, expandRange)
                            }
                        }
                    }
                }

                Surface(color = DarkSurface, tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp * (1f - expansion), topEnd = 24.dp * (1f - expansion)),
                    modifier = Modifier.testTag("task-detail-surface").align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
                        .offset { IntOffset(0, position.dismissOffset.roundToInt()) }) {
                    Column(Modifier.fillMaxSize().imePadding()) {
                        Row(Modifier.testTag("task-detail-header").fillMaxWidth().height(44.dp + 4.dp * expansion)
                            .pointerInput(heightPx, expandRange) {
                                val tracker = VelocityTracker()
                                var distance = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { settleJob?.cancel(); tracker.resetTracking(); distance = 0f },
                                    onDragCancel = { settle(if (position.expansion >= 0.5f) DetailSnapTarget.PAGE else DetailSnapTarget.DRAWER) },
                                    onDragEnd = { val velocity = tracker.calculateVelocity().y
                                        settle(taskDetailSnap(position, velocity, density.density), velocity) },
                                    onVerticalDrag = { change, delta ->
                                        change.consume()
                                        distance += delta
                                        tracker.addPosition(change.uptimeMillis, Offset(0f, distance))
                                        position = dragTaskDetail(position, delta, expandRange)
                                    }
                                )
                            }, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { latestClose() }, enabled = expansion > 0.5f,
                                modifier = Modifier.alpha(expansion)) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "저장하고 뒤로", tint = TextPrimary)
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(width = 32.dp, height = 4.dp).alpha(1f - expansion)
                                    .clip(RoundedCornerShape(2.dp)).background(TextTertiary.copy(alpha = 0.5f)))
                                Text("페이지", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.alpha(expansion))
                            }
                            actions()
                            // No expand button: drag the handle up for the full page.
                            // Keeps the handle centred against the back button.
                            Spacer(Modifier.size(48.dp))
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    }
                }
            }
        }
    }
}
