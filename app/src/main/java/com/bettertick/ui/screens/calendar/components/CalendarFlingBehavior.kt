package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Wraps the platform fling behavior and dampens the initial velocity before
 * delegating. The default spline-based decay is tuned for short lists and
 * feels runaway on a multi-year calendar — a single flick would blow past
 * several months. Halving the entry velocity keeps the curve shape but
 * shortens the travel distance by roughly 70% (fling distance scales
 * super-linearly with velocity).
 */
private class DampedFlingBehavior(
    private val delegate: FlingBehavior,
    private val velocityMultiplier: Float
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return with(delegate) {
            performFling(initialVelocity * velocityMultiplier)
        }
    }
}

/**
 * Shared fling behavior for the month/year scrollables. [velocityMultiplier]
 * of 0.5 means flicks travel ~30% of the default distance, which lands close
 * to "one month per relaxed flick" for a typical touch velocity.
 */
@Composable
fun rememberCalendarFlingBehavior(velocityMultiplier: Float = 0.5f): FlingBehavior {
    val base = ScrollableDefaults.flingBehavior()
    return remember(base, velocityMultiplier) {
        DampedFlingBehavior(base, velocityMultiplier)
    }
}
