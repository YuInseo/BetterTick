package com.bettertick.ui.screens.tasks.components

import com.bettertick.data.model.NoteBlockKind
import java.util.Locale

internal data class NoteSlashItem(val kind: String, val label: String, val aliases: String)

/** Independent native command model; reference behavior is recorded in docs/BLOCK_EDITOR_REFERENCES.md. */
internal fun noteSlashItems(query: String): List<NoteSlashItem> {
    val needle = query.trim().lowercase(Locale.ROOT)
    return listOf(
        NoteSlashItem(NoteBlockKind.TEXT, "글", "text paragraph 글 문단"),
        NoteSlashItem(NoteBlockKind.HEADING, "제목", "heading title 제목"),
        NoteSlashItem(NoteBlockKind.TODO, "체크리스트", "todo check checklist 체크 할일"),
        NoteSlashItem(NoteBlockKind.DIVIDER, "구분선", "divider line 구분 선")
    ).filter { it.aliases.contains(needle) || it.label.contains(needle) }
}

/** Split only a single newly inserted newline at the caret, never bulk pasted or composing text. */
internal fun splitNoteOnEnter(previous: String, next: String, cursor: Int): Pair<String, String>? {
    val at = cursor - 1
    if (next.length != previous.length + 1 || at !in next.indices || next[at] != '\n') return null
    if (next.removeRange(at, at + 1) != previous) return null
    return next.substring(0, at) to next.substring(at + 1)
}

internal fun nextNoteBlockKind(current: String): String =
    if (current == NoteBlockKind.TODO) NoteBlockKind.TODO else NoteBlockKind.TEXT
