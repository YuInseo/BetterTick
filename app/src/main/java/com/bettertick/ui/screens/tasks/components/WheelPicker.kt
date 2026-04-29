package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Vertically scrolling wheel picker — think iOS's UIPickerView column. The
 * centre slot is the "selected" value; items above and below fade out with
 * distance so the focus is unambiguous even while scrolling.
 *
 * Implementation notes:
 *  - Empty spacer "items" are added at the top and bottom equal to
 *    [halfVisibleCount]. That lets the first and last real items reach the
 *    centre of the viewport without exotic contentPadding math.
 *  - The selected index is derived from [androidx.compose.foundation.lazy.LazyListLayoutInfo]
 *    by finding which real item's centre is closest to the viewport centre.
 *    This stays correct during fling (the `firstVisibleItemIndex` alone
 *    would lag or flip between spacer/real items in some positions).
 *  - [rememberSnapFlingBehavior] snaps to item boundaries on release so
 *    the wheel always rests with a single value in the centre slot.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 5,
    itemHeight: Dp = 40.dp,
    centerFontSize: TextUnit = 22.sp,
    sideFontSize: TextUnit = 18.sp
) {
    require(visibleItemsCount % 2 == 1) { "visibleItemsCount must be odd so one row is the centre." }
    require(items.isNotEmpty()) { "WheelPicker needs at least one item." }

    val halfVisible = visibleItemsCount / 2
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // "Real item at viewport centre" — skip the invisible spacers at both
    // ends and find the real item whose midpoint is closest to the centre
    // of the visible area.
    val centerRealIndex by remember(items.size, halfVisible) {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf safeIndex
            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val realRange = halfVisible until (halfVisible + items.size)
            info.visibleItemsInfo
                .filter { it.index in realRange }
                .minByOrNull { abs((it.offset + it.size / 2f) - viewportCentre) }
                ?.let { it.index - halfVisible }
                ?: safeIndex
        }
    }

    // Propagate selection changes upward. Filtered against the latest
    // incoming `selectedIndex` so we don't fight the caller when they
    // reset the wheel externally.
    LaunchedEffect(centerRealIndex) {
        if (centerRealIndex != selectedIndex) onSelectedIndexChange(centerRealIndex)
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleItemsCount)
    ) {
        // Top spacers — invisible, just reserve scroll space so item 0 can
        // reach the centre.
        items(count = halfVisible, key = { "top-spacer-$it" }) {
            Box(modifier = Modifier.height(itemHeight))
        }

        itemsIndexed(items, key = { i, _ -> "real-$i" }) { index, label ->
            val distance = abs(index - centerRealIndex)
            // Fade curve: 1 / 0.55 / 0.3 / 0.15 matches the reference's
            // visible gradient (centre fully lit, rows just outside clearly
            // readable, farther rows barely there).
            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.55f
                2 -> 0.3f
                else -> 0.15f
            }
            val isCentre = distance == 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = if (isCentre) centerFontSize else sideFontSize,
                    fontWeight = if (isCentre) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = alpha)
                )
            }
        }

        items(count = halfVisible, key = { "bottom-spacer-$it" }) {
            Box(modifier = Modifier.height(itemHeight))
        }
    }
}
