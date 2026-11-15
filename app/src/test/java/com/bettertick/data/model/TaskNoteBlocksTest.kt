package com.bettertick.data.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TaskNoteBlocksTest {
    @Test fun migrationPreservesEveryDateAndParagraph() {
        val task = Task(notes = "first\nsecond", memos = listOf(TaskMemo(id = "m", text = "memo", day = "2026-11-15")),
            dayNotes = mapOf("2026-11-16" to "hidden"), attachments = listOf("image|content://one|photo"))
        val blocks = task.noteBlocks()
        assertEquals("first\nsecond", blocks.first().text)
        assertTrue(blocks.any { it.day == "2026-11-16" && it.text == "hidden" })
        assertEquals(4, blocks.size)
        assertEquals(blocks.map { it.id }, task.noteBlocks().map { it.id })
    }

    @Test fun structuredRoundTripDoesNotDuplicatePreviewOrAttachments() {
        val blocks = listOf(TaskMemo(id = "a", kind = "heading", text = "Title"),
            TaskMemo(id = "b", kind = "todo", text = "Do it", checked = true),
            attachmentNoteBlock("image|content://a|photo", "image"))
        val task = Task(notes = blocks.commonNoteText(), memos = blocks, attachments = blocks.noteAttachments())
        assertEquals(blocks, task.noteBlocks())
        assertEquals("# Title\n- [x] Do it", task.notes)
        assertTrue(task.memosOn(LocalDate.of(2026, 11, 15)).isEmpty())
    }

    @Test fun existingMarkdownChecklistKeepsCheckedState() {
        val blocks = Task(notes = "# title\n- [x] done\n- [ ] open").noteBlocks()
        assertEquals(listOf("heading", "todo", "todo"), blocks.map { it.kind })
        assertTrue(blocks[1].checked)
        assertFalse(blocks[2].checked)
    }

    @Test fun movingVisibleBlockRetainsHiddenOccurrence() {
        val blocks = listOf(TaskMemo(id = "a", kind = "text", text = "A"),
            TaskMemo(id = "hidden", kind = "text", text = "H", day = "2026-11-16"),
            TaskMemo(id = "b", kind = "text", text = "B", day = "2026-11-15"))
        val moved = blocks.moveNoteBlock("b", -1, "2026-11-15")
        assertEquals(listOf("b", "a", "hidden"), moved.map { it.id })
        assertEquals(blocks[1], moved.last())
        assertEquals(moved, moved.moveNoteBlock("b", -1, "2026-11-15"))
    }

    @Test fun savingRemovesOnlyEmptyContentAndKeepsMediaAndDivider() {
        val blocks = listOf(TaskMemo(kind = "text"), TaskMemo(kind = "divider"),
            attachmentNoteBlock("image|content://one|photo"), TaskMemo(kind = "text", text = "other", day = "2026-11-17"))
        assertEquals(3, blocks.savedNoteBlocks().size)
        assertEquals(listOf("image|content://one|photo"), blocks.noteAttachments())
    }
    @Test fun previewKeepsLegacyMemosAndOnlyTheSelectedDate() {
        val task = Task(memos = listOf(
            TaskMemo(id = "legacy", text = "legacy shared"),
            TaskMemo(id = "shared", kind = "text", text = "already projected"),
            TaskMemo(id = "today", kind = "text", text = "today", day = "2026-11-15"),
            TaskMemo(id = "other", kind = "text", text = "other", day = "2026-11-16")
        ))
        assertEquals(listOf("legacy", "today"), task.memosOn(LocalDate.of(2026, 11, 15)).map { it.id })
    }
}
