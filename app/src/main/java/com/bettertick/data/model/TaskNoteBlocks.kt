package com.bettertick.data.model

/** Native note blocks share the existing memo list, so deployed owner rules remain applicable. */
object NoteBlockKind {
    const val TEXT = "text"
    const val HEADING = "heading"
    const val TODO = "todo"
    const val DIVIDER = "divider"
    const val IMAGE = "image"
    const val FILE = "file"
}

/** Lossless lazy migration; hidden dates are kept in the document, not just the selected page. */
fun Task.noteBlocks(): List<TaskMemo> {
    val structured = memos.any { it.kind.isNotBlank() }
    val blocks = mutableListOf<TaskMemo>()
    if (!structured && notes.isNotBlank()) {
        // Preserve paragraph line breaks, but recognize existing checklist/heading lines.
        val paragraph = mutableListOf<String>()
        var part = 0
        fun flush() {
            if (paragraph.isNotEmpty()) {
                blocks += TaskMemo(id = "legacy-notes-${part++}", text = paragraph.joinToString("\n"), kind = NoteBlockKind.TEXT)
                paragraph.clear()
            }
        }
        notes.lines().forEach { line ->
            val todo = Regex("^- \\[([ xX])\\] (.*)$").matchEntire(line)
            val heading = Regex("^#{1,3} (.+)$").matchEntire(line)
            when {
                todo != null -> { flush(); blocks += TaskMemo(id = "legacy-notes-${part++}",
                    text = todo.groupValues[2], kind = NoteBlockKind.TODO, checked = todo.groupValues[1] != " ") }
                heading != null -> { flush(); blocks += TaskMemo(id = "legacy-notes-${part++}",
                    text = heading.groupValues[1], kind = NoteBlockKind.HEADING) }
                else -> paragraph += line
            }
        }
        flush()
    }
    blocks += memos.map { if (it.kind.isBlank()) it.copy(kind = NoteBlockKind.TEXT) else it }
    dayNotes.forEach { (day, text) ->
        val id = "legacy-day-$day"
        if (blocks.none { it.id == id }) blocks += TaskMemo(id = id, text = text, day = day, kind = NoteBlockKind.TEXT)
    }
    attachments.forEachIndexed { index, attachment ->
        if (blocks.none { it.attachment == attachment }) blocks += attachmentNoteBlock(attachment, "legacy-attachment-$index")
    }
    return blocks
}

fun attachmentNoteBlock(entry: String, id: String = java.util.UUID.randomUUID().toString()): TaskMemo {
    val parts = entry.split('|', limit = 3)
    return TaskMemo(id = id, text = parts.getOrNull(2).orEmpty(), attachment = entry,
        kind = if (parts.firstOrNull() == "image") NoteBlockKind.IMAGE else NoteBlockKind.FILE)
}

fun List<TaskMemo>.savedNoteBlocks(): List<TaskMemo> = filter {
    it.kind == NoteBlockKind.DIVIDER || it.text.isNotBlank() || it.attachment.isNotBlank()
}

/** A plain markdown projection keeps search, exports and calendar previews compatible. */
fun List<TaskMemo>.commonNoteText(): String = filter { it.day.isBlank() }.mapNotNull {
    when (it.kind) {
        NoteBlockKind.IMAGE, NoteBlockKind.FILE -> null
        NoteBlockKind.DIVIDER -> "---"
        NoteBlockKind.HEADING -> "# ${it.text}"
        NoteBlockKind.TODO -> "- [${if (it.checked) "x" else " "}] ${it.text}"
        else -> it.text
    }
}.joinToString("\n").take(200000)

fun List<TaskMemo>.noteAttachments(): List<String> = map { it.attachment }.filter { it.isNotBlank() }.distinct()

/** Move within the currently visible page; unseen occurrence blocks retain their order and data. */
fun List<TaskMemo>.moveNoteBlock(id: String, direction: Int, day: String?): List<TaskMemo> {
    val from = indexOfFirst { it.id == id }
    if (from < 0 || direction == 0) return this
    val visible = indices.filter { this[it].day.isBlank() || day == null || this[it].day == day }
    val position = visible.indexOf(from)
    if (position < 0) return this
    val to = visible.getOrNull(position + if (direction < 0) -1 else 1) ?: return this
    return toMutableList().apply { val block = removeAt(from); add(to, block) }
}
