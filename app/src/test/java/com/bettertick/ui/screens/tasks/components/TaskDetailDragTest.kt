package com.bettertick.ui.screens.tasks.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDetailDragTest {
    @Test fun sheetTopTracksFingerDistanceInsteadOfWaitingForRelease() {
        val moved = dragTaskDetail(DetailDragPosition(), -40f, 200f)
        assertEquals(0.2f, moved.expansion, 0.0001f)
        assertEquals(40f, moved.expansion * 200f, 0.0001f)
    }

    @Test fun reversingTheDragReturnsToTheOriginalPosition() {
        val up = dragTaskDetail(DetailDragPosition(), -90f, 200f)
        val back = dragTaskDetail(up, 90f, 200f)
        assertEquals(0f, back.expansion, 0.0001f)
        assertEquals(0f, back.dismissOffset, 0.0001f)
    }

    @Test fun downwardOverscrollAndReversalStayContinuous() {
        val down = dragTaskDetail(DetailDragPosition(), 60f, 200f)
        assertEquals(60f, down.dismissOffset, 0.0001f)
        val upAgain = dragTaskDetail(down, -100f, 200f)
        assertEquals(0.2f, upAgain.expansion, 0.0001f)
        assertEquals(0f, upAgain.dismissOffset, 0.0001f)
    }

    @Test fun fullPageClampsWithoutOvershootingAndCanBePulledDown() {
        val full = dragTaskDetail(DetailDragPosition(), -300f, 200f)
        assertEquals(1f, full.expansion, 0.0001f)
        val back = dragTaskDetail(full, 60f, 200f)
        assertEquals(0.7f, back.expansion, 0.0001f)
    }

    @Test fun releaseUsesPositionAndDensityAwareVelocityAndNeverExpandsOnATap() {
        assertEquals(DetailSnapTarget.DRAWER, taskDetailSnap(DetailDragPosition(), 0f, 2f))
        assertEquals(DetailSnapTarget.DRAWER, taskDetailSnap(DetailDragPosition(0.49f), 0f, 2f))
        assertEquals(DetailSnapTarget.PAGE, taskDetailSnap(DetailDragPosition(0.51f), 0f, 2f))
        assertEquals(DetailSnapTarget.PAGE, taskDetailSnap(DetailDragPosition(0.1f), -2000f, 2f))
        assertEquals(DetailSnapTarget.DRAWER, taskDetailSnap(DetailDragPosition(0.9f), 2000f, 2f))
    }

    @Test fun dismissalRequiresActualDownwardTravel() {
        assertEquals(DetailSnapTarget.DRAWER, taskDetailSnap(DetailDragPosition(), 5000f, 1f))
        assertEquals(DetailSnapTarget.CLOSED, taskDetailSnap(DetailDragPosition(dismissOffset = 100f), 0f, 1f))
        assertEquals(DetailSnapTarget.CLOSED, taskDetailSnap(DetailDragPosition(dismissOffset = 30f), 1500f, 1f))
    }
}
