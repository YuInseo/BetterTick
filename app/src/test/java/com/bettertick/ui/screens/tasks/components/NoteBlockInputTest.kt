package com.bettertick.ui.screens.tasks.components

import org.junit.Assert.*
import org.junit.Test

class NoteBlockInputTest {
    @Test fun slashCommandsAcceptKoreanAndEnglishAliases() {
        assertEquals("heading", noteSlashItems("제목").first().kind)
        assertEquals(listOf("todo"), noteSlashItems("CHECKLIST").map { it.kind })
        assertEquals(NoteSlashCatalog.size, noteSlashItems("").size)
        assertEquals(listOf("code"), noteSlashItems("코드").map { it.kind })
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
        assertEquals("bullet", nextNoteBlockKind("bullet"))
        assertEquals("numbered", nextNoteBlockKind("numbered"))
        assertEquals("text", nextNoteBlockKind("quote"))
    }
    @Test fun samsungStyleCompositionOutsideNewlineCanStillContinueToNextBlock() {
        assertEquals("흠" to "", splitNoteOnEnter("흠", "흠\n", 2, 0 until 1))
        assertNull(splitNoteOnEnter("흠", "흠\n", 2, 0 until 2))
    }

    @Test fun compactingLegacyBlockEdgesKeepsInternalFormatting() {
        assertEquals("  first\n\nsecond", compactNoteBlockText("\n  first\n\nsecond\n  \n"))
        assertEquals("", compactNoteBlockText("\n \n"))
    }

    @Test fun backspaceAtBlockStartFollowsBlockNoteRules() {
        val text = com.bettertick.data.model.TaskMemo(id = "a", text = "앞")
        val empty = com.bettertick.data.model.TaskMemo(id = "b", text = "")
        val todo = com.bettertick.data.model.TaskMemo(id = "c", kind = com.bettertick.data.model.NoteBlockKind.TODO, text = "")
        val divider = com.bettertick.data.model.TaskMemo(id = "d", kind = com.bettertick.data.model.NoteBlockKind.DIVIDER)
        assertEquals(NoteBackspace.TO_TEXT, noteBackspaceAtStart(todo, text))
        assertEquals(NoteBackspace.DELETE_FOCUS_PREVIOUS, noteBackspaceAtStart(empty, text))
        assertEquals(NoteBackspace.MERGE_INTO_PREVIOUS, noteBackspaceAtStart(text.copy(id = "e"), text))
        assertEquals(NoteBackspace.REMOVE_PREVIOUS, noteBackspaceAtStart(empty, divider))
        assertEquals(NoteBackspace.NONE, noteBackspaceAtStart(empty, null))
    }

    private fun memo(id: String, kind: String = "text", indent: Int = 0, checked: Boolean = false, text: String = "") =
        com.bettertick.data.model.TaskMemo(id = id, kind = kind, indent = indent, checked = checked, text = text)

    @Test fun nestedBlocksOutdentBeforeMerging() {
        assertEquals(NoteBackspace.OUTDENT, noteBackspaceAtStart(memo("b", indent = 1, text = "x"), memo("a", text = "a")))
        assertEquals(NoteBackspace.TO_TEXT, noteBackspaceAtStart(memo("b", kind = "bullet"), memo("a")))
        assertEquals(NoteBackspace.REMOVE_PREVIOUS, noteBackspaceAtStart(memo("b"), memo("a", kind = "toc")))
    }

    @Test fun markdownShortcutsTurnTextIntoBlocks() {
        assertEquals("bullet" to "", markdownShortcut("-", "- "))
        assertEquals("numbered" to "", markdownShortcut("1.", "1. "))
        assertEquals("heading2" to "", markdownShortcut("##", "## "))
        assertEquals("todo" to "", markdownShortcut("[]", "[] "))
        assertEquals("code" to "", markdownShortcut("``", "```"))
        assertNull(markdownShortcut("- a", "- ab"))
        assertNull(markdownShortcut("a", "a-"))
    }

    @Test fun numberedListsCountTheirOwnRun() {
        val blocks = listOf(memo("1", "numbered"), memo("2", "numbered"), memo("x", "bullet", indent = 1),
            memo("3", "numbered"), memo("t"), memo("4", "numbered"))
        assertEquals(listOf(1, 2, 3, 1), listOf(0, 1, 3, 5).map { noteListNumber(blocks, it) })
    }

    @Test fun collapsedTogglesHideTheirChildren() {
        val blocks = listOf(memo("t", "toggle", checked = false), memo("c1", indent = 1), memo("c2", indent = 2),
            memo("after"), memo("open", "toggle", checked = true), memo("c3", indent = 1))
        assertEquals(setOf("c1", "c2"), collapsedNoteIds(blocks))
    }

    @Test fun indentCarriesChildrenAndRespectsLimits() {
        val blocks = listOf(memo("a"), memo("b"), memo("c", indent = 1), memo("d"))
        val shifted = shiftNoteIndent(blocks, "b", 1)!!
        assertEquals(listOf(0, 1, 2, 0), shifted.map { it.indent })
        assertNull(shiftNoteIndent(blocks, "a", 1))
        assertNull(shiftNoteIndent(blocks, "a", -1))
        assertNull(shiftNoteIndent(shifted, "b", 1))
    }

    @Test fun embedServicesAreRecognised() {
        assertEquals("YouTube", noteEmbedService("https://youtu.be/abc"))
        assertEquals("Google Maps", noteEmbedService("https://maps.app.goo.gl/xyz"))
        assertEquals("Figma", noteEmbedService("https://www.figma.com/file/1"))
        assertEquals("GitHub Gist", noteEmbedService("https://gist.github.com/u/1"))
        assertEquals("example.com", noteEmbedService("https://www.example.com/a"))
        assertTrue(isWebUrl("https://a.b/c"))
        assertFalse(isWebUrl("not a url"))
    }
}
