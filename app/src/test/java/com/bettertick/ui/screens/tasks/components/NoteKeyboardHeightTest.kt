package com.bettertick.ui.screens.tasks.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteKeyboardHeightTest {
    @Test fun toolbarStaysFixedEvenWhenImePausesOrReverses() {
        for (ime in listOf(600, 480, 240, 0, 0, 60, 60, 420, 180, 600)) {
            assertEquals(600, ime + remainingPanelHeight(600, ime, 0))
            assertEquals(600, ime + remainingPanelHeight(600, ime, 600))
        }
    }

    @Test fun toolbarTracksAChangedKeyboardHeightWithoutOvershoot() {
        for (target in listOf(420, 780)) {
            for (step in 0..10) {
                val ime = target * step / 10
                val expected = 600 + (target - 600) * step / 10
                assertEquals(expected, ime + remainingPanelHeight(600, ime, target))
            }
            assertEquals(0, remainingPanelHeight(600, target + 30, target))
        }
    }
}
