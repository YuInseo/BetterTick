package com.bettertick.ui.screens.tasks.components

internal data class DetailDragPosition(val expansion: Float = 0f, val dismissOffset: Float = 0f)
internal enum class DetailSnapTarget { DRAWER, PAGE, CLOSED }

/** The sheet top follows raw finger distance across both expand and dismiss ranges. */
internal fun dragTaskDetail(position: DetailDragPosition, delta: Float, expandRange: Float): DetailDragPosition {
    val range = expandRange.coerceAtLeast(1f)
    val top = ((1f - position.expansion) * range + position.dismissOffset + delta).coerceAtLeast(0f)
    return if (top <= range) DetailDragPosition(expansion = 1f - top / range)
        else DetailDragPosition(dismissOffset = top - range)
}

/** Velocity is pixels/second, just like the pointer tracker. A tap never changes the anchor. */
internal fun taskDetailSnap(position: DetailDragPosition, velocity: Float, density: Float): DetailSnapTarget = when {
    position.dismissOffset >= 96f * density ||
        (position.dismissOffset > 24f * density && velocity > 1200f * density) -> DetailSnapTarget.CLOSED
    velocity < -900f * density -> DetailSnapTarget.PAGE
    velocity > 900f * density -> DetailSnapTarget.DRAWER
    position.expansion >= 0.5f -> DetailSnapTarget.PAGE
    else -> DetailSnapTarget.DRAWER
}
