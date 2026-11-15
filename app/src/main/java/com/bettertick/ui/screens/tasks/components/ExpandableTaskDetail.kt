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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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

/** 0 = drawer, 1 = full page; lets the content move its date row into the pinned bar. */
internal val LocalDetailExpansion = androidx.compose.runtime.compositionLocalOf { 0f }

/** One window and one editor composition for the entire drawer ↔ page transition. */
@Composable
internal fun ExpandableTaskDetail(
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    /** Page mode: shown in the pinned top bar once the big title has scrolled away (Notion style). */
    pageTitle: String = "",
    pageSubtitle: String = "",
    titleScrolledAway: Boolean = false,
    /** Page mode only: shown beside the back button (the sheet's date row moves up here). */
    pageBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var position by remember { mutableStateOf(DetailDragPosition()) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val latestClose by rememberUpdatedState(onClose)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Every close (back, ←, tap outside) slides the sheet away and fades the dim
    // first; the window itself then goes without an animation of its own — its
    // exit fade over a still-dark dim was the black flash after closing.
    var closeRequested by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { closeRequested = true },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        // Draw our own scrim so it fades continuously with the sheet instead of swapping windows.
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val oldDim = window?.attributes?.dimAmount ?: 0f
            val oldInputMode = window?.attributes?.softInputMode ?: 0
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
            // A full-height edge-to-edge window must not be panned/resized by the WM as well as Compose.
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            // The dialog window has its own (black) status / navigation bar colour,
            // which showed as a black strip at the top over the home screen or the
            // calendar. Clear them so our dim and sheet show right to the edges.
            window?.let { w ->
                w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                @Suppress("DEPRECATION")
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
                androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
            onDispose { window?.setDimAmount(oldDim); window?.setSoftInputMode(oldInputMode) }
        }
        val expansion = position.expansion.coerceIn(0f, 1f)
        // How far the drawer travels to leave the screen (set from the layout below).
        var sheetTravel by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
        // Pulling the sheet down (to close it) puts the keyboard away right away.
        val keyboardCtl = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val pullingDown = position.dismissOffset > with(density) { 24.dp.toPx() }
        LaunchedEffect(pullingDown) {
            if (pullingDown) { keyboardCtl?.hide(); focusManager.clearFocus() }
        }
        Box(Modifier.fillMaxSize()) {
            // The dim fades out as the sheet is pulled away, so closing doesn't end on
            // a dark screen that then vanishes at once.
            val closeFade = (1f - position.dismissOffset / sheetTravel.coerceAtLeast(1f)).coerceIn(0f, 1f)
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.42f * (1f - expansion) * closeFade))
                .clickable(enabled = expansion < 0.99f) { closeRequested = true })
            BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                val heightPx = with(density) { maxHeight.toPx() }
                sheetTravel = heightPx * 0.78f
                val collapsedHeight = maxHeight * 0.78f
                val expandRange = with(density) { (maxHeight - collapsedHeight).toPx() }.coerceAtLeast(1f)
                val sheetHeight = collapsedHeight + (maxHeight - collapsedHeight) * expansion

                fun settle(target: DetailSnapTarget, velocity: Float = 0f) {
                    settleJob?.cancel()
                    settleJob = scope.launch {
                        if (target == DetailSnapTarget.CLOSED) {
                            // A short eased slide (not a spring, whose slow tail made the
                            // sheet hang near the bottom before closing).
                            val remaining = (heightPx - position.dismissOffset).coerceAtLeast(0f)
                            val ms = (180 + 140 * remaining / heightPx.coerceAtLeast(1f)).toInt()
                            animate(initialValue = position.dismissOffset, targetValue = heightPx,
                                animationSpec = androidx.compose.animation.core.tween(ms,
                                    easing = androidx.compose.animation.core.FastOutLinearInEasing)) { value, _ ->
                                // Keep the page / drawer size while it slides away.
                                position = position.copy(dismissOffset = value.coerceAtLeast(0f))
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
                LaunchedEffect(closeRequested) {
                    // Reset at once so a later back press works again if a drag interrupted it.
                    if (closeRequested) { closeRequested = false; settle(DetailSnapTarget.CLOSED) }
                }

                // Drag anywhere on the sheet, not only the top handle: once the
                // content is scrolled to its top, pulling down moves the sheet
                // (page → drawer → close), and pushing up first expands it.
                val sheetDrag = remember(heightPx, expandRange) {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        var moved = false
                        // Only a finger on the sheet moves it — not the scroll that
                        // brings a focused field into view when the keyboard opens.
                        var fingerDown = false
                        override fun onPreScroll(available: Offset,
                                                 source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                            if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag || !fingerDown) return Offset.Zero
                            // Sheet not fully up yet (or pulled down): the finger moves the sheet first.
                            if (available.y < 0f && (position.dismissOffset > 0f || (moved && position.expansion < 1f))) {
                                settleJob?.cancel()
                                position = dragTaskDetail(position, available.y, expandRange)
                                moved = true
                                return Offset(0f, available.y)
                            }
                            return Offset.Zero
                        }
                        override fun onPostScroll(consumed: Offset, available: Offset,
                                                  source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                            if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag || available.y <= 0f || !fingerDown) return Offset.Zero
                            settleJob?.cancel()
                            position = dragTaskDetail(position, available.y, expandRange)
                            moved = true
                            return Offset(0f, available.y)
                        }
                        override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                            if (!moved) return androidx.compose.ui.unit.Velocity.Zero
                            moved = false
                            settle(taskDetailSnap(position, available.y, density.density), available.y)
                            return available
                        }
                    }
                }

                Surface(color = DarkSurface, tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp * (1f - expansion), topEnd = 24.dp * (1f - expansion)),
                    modifier = Modifier.testTag("task-detail-surface").align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
                        .offset { IntOffset(0, position.dismissOffset.roundToInt()) }) {
                    Column(Modifier.fillMaxSize().imePadding()) {
                        Row(Modifier.testTag("task-detail-header").fillMaxWidth().height(44.dp + (if (pageBar != null) 16.dp else 4.dp) * expansion)
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
                            IconButton(onClick = { closeRequested = true }, enabled = expansion > 0.5f,
                                modifier = Modifier.alpha(expansion)) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "저장하고 뒤로", tint = TextPrimary)
                            }
                            val titleAlpha by androidx.compose.animation.core.animateFloatAsState(
                                if (titleScrolledAway) 1f else 0f, label = "pageTitle")
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(width = 32.dp, height = 4.dp).alpha(1f - expansion)
                                    .clip(RoundedCornerShape(2.dp)).background(TextTertiary.copy(alpha = 0.5f)))
                                // Page mode: the date row sits here, level with the back button…
                                val pageAlpha = ((expansion - 0.5f) * 2f).coerceIn(0f, 1f)
                                if (pageBar != null && expansion >= 0.5f && titleAlpha < 1f) {
                                    Box(Modifier.fillMaxWidth().alpha(pageAlpha * (1f - titleAlpha))) { pageBar() }
                                }
                                // …and the title takes its place once the big title scrolls away.
                                if (pageTitle.isNotBlank() && titleAlpha > 0f) Column(Modifier.fillMaxWidth().alpha(
                                    (if (pageBar != null) pageAlpha else expansion) * titleAlpha)) {
                                    Text(pageTitle, fontSize = 16.sp, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    if (pageSubtitle.isNotBlank()) Text(pageSubtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            actions()
                            // No expand button: drag the handle up for the full page.
                            // Keeps the handle centred against the back button.
                            Spacer(Modifier.width(if (pageBar != null) 48.dp * (1f - expansion) + 8.dp * expansion else 48.dp))
                        }
                        // Hairline under the pinned bar while the page is scrolled.
                        Box(Modifier.fillMaxWidth().height(1.dp)
                            .alpha(if (titleScrolledAway) expansion else 0f).background(TextTertiary.copy(alpha = 0.18f)))
                        Box(Modifier.weight(1f).fillMaxWidth()
                            .pointerInput(sheetDrag) {
                                // Watch (never consume) touches so the sheet knows a finger is down.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    sheetDrag.fingerDown = true
                                    do {
                                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    } while (event.changes.any { it.pressed })
                                    sheetDrag.fingerDown = false
                                }
                            }
                            .nestedScroll(sheetDrag)) {
                            CompositionLocalProvider(LocalDetailExpansion provides expansion) { content() }
                        }
                    }
                }
            }
        }
    }
}
