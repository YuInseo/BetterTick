package com.bettertick.ui.screens.tasks.components

import org.junit.Assert.*
import org.junit.Test

class NoteBlockInputTest {
    @Test fun slashCommandsAcceptKoreanAndEnglishAliases() {
        assertEquals(listOf("heading"), noteSlashItems("제목").map { it.kind })
        assertEquals(listOf("todo"), noteSlashItems("CHECKLIST").map { it.kind })
        assertEquals(4, noteSlashItems("").size)
        assertTrue(noteSlashItems("unknown-command").isEmpty())
    }

    @Test fun enterSplitsAtTheCaretWithoutDiscardingFollowingText() {
        val split = splitNoteOnEnter("beforeafter", "before\nafter", 7)
        assertEquals("before" to "after", split)
        assertEquals("before\nafter", split!!.first + "\n" + split.second)
    }

    @Test fun existingLineBreaksUseTheActualCaretLocation() {
        assertEquals("a" to "\nb", splitNoteOnEnter("a\nb", "a\n\nb", 2))
    }

    @Test fun multilinePasteAndRegularEditsRemainInTheCurrentBlock() {
        assertNull(splitNoteOnEnter("a", "a\nb\nc", 5))
        assertNull(splitNoteOnEnter("a\nb", "ab", 1))
        assertNull(splitNoteOnEnter("abc", "abcd", 4))
    }

    @Test fun headingContinuesAsParagraphAndCheckboxContinuesAsCheckbox() {
        assertEquals("text", nextNoteBlockKind("heading"))
        assertEquals("todo", nextNoteBlockKind("todo"))
        assertEquals("text", nextNoteBlockKind("text"))
    }
}
