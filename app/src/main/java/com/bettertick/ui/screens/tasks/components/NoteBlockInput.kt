package com.bettertick.ui.screens.tasks.components

import com.bettertick.data.model.NoteBlockKind
import com.bettertick.data.model.MAX_NOTE_INDENT
import com.bettertick.data.model.TaskMemo
import java.util.Locale

internal data class NoteSlashItem(val kind: String, val label: String, val aliases: String)

/** Independent native command model; reference behavior is recorded in docs/BLOCK_EDITOR_REFERENCES.md. */
internal fun noteSlashItems(query: String): List<NoteSlashItem> {
    val needle = query.trim().lowercase(Locale.ROOT)
    return NoteSlashCatalog.filter { it.aliases.contains(needle) || it.label.lowercase(Locale.ROOT).contains(needle) }
}

/** Every block the slash menu and the + panel can make, in menu order. */
internal val NoteSlashCatalog = listOf(
    NoteSlashItem(NoteBlockKind.TEXT, "텍스트", "text paragraph 글 문단 텍스트"),
    NoteSlashItem(NoteBlockKind.HEADING, "제목1", "heading title h1 제목 제목1"),
    NoteSlashItem(NoteBlockKind.HEADING2, "제목2", "heading2 h2 제목2 subheading"),
    NoteSlashItem(NoteBlockKind.HEADING3, "제목3", "heading3 h3 제목3"),
    NoteSlashItem(NoteBlockKind.TODO, "체크리스트", "todo check checklist 체크 할일"),
    NoteSlashItem(NoteBlockKind.BULLET, "글머리 기호 목록", "bullet list ul 글머리 목록 불릿"),
    NoteSlashItem(NoteBlockKind.NUMBERED, "번호 매기기 목록", "numbered list ol 번호 목록"),
    NoteSlashItem(NoteBlockKind.TOGGLE, "토글 목록", "toggle 토글 접기"),
    NoteSlashItem(NoteBlockKind.PAGE, "페이지", "page subpage 페이지 하위 문서"),
    NoteSlashItem(NoteBlockKind.TOGGLE_H1, "토글 제목1", "toggle heading 토글 제목1"),
    NoteSlashItem(NoteBlockKind.TOGGLE_H2, "토글 제목2", "toggle heading2 토글 제목2"),
    NoteSlashItem(NoteBlockKind.TOGGLE_H3, "토글 제목3", "toggle heading3 토글 제목3"),
    NoteSlashItem(NoteBlockKind.QUOTE, "인용", "quote 인용"),
    NoteSlashItem(NoteBlockKind.CALLOUT, "콜아웃", "callout 콜아웃 강조 메모"),
    NoteSlashItem(NoteBlockKind.DIVIDER, "구분선", "divider line 구분 선"),
    NoteSlashItem(NoteBlockKind.CODE, "코드", "code 코드 snippet"),
    NoteSlashItem(NoteBlockKind.TOC, "목차", "toc table of contents 목차"),
    NoteSlashItem(NoteBlockKind.EMBED, "링크 · 임베드", "embed link bookmark url 임베드 링크 북마크 youtube 유튜브 지도 maps figma gist drive")
)

/** Split only a single newly inserted newline at the caret, never bulk pasted or composing text. */
internal fun splitNoteOnEnter(previous: String, next: String, cursor: Int, composingRange: IntRange? = null): Pair<String, String>? {
    val at = cursor - 1
    if (composingRange?.contains(at) == true) return null
    if (next.length != previous.length + 1 || at !in next.indices || next[at] != '\n') return null
    if (next.removeRange(at, at + 1) != previous) return null
    return next.substring(0, at) to next.substring(at + 1)
}

/** Lists continue as the same list; everything else continues as text. */
internal fun nextNoteBlockKind(current: String): String = when (current) {
    NoteBlockKind.TODO, NoteBlockKind.BULLET, NoteBlockKind.NUMBERED -> current
    else -> NoteBlockKind.TEXT
}

/** Kinds whose empty block turns back into text on Enter, like Notion lists. */
internal fun exitsOnEmptyEnter(kind: String) = when (kind) {
    NoteBlockKind.TODO, NoteBlockKind.BULLET, NoteBlockKind.NUMBERED, NoteBlockKind.QUOTE,
    NoteBlockKind.CALLOUT, NoteBlockKind.TOGGLE -> true
    else -> false
}

/** Markdown shortcuts typed at the start of a text block: "- ", "1. ", "# ", "> ", "[] ", "```". */
internal fun markdownShortcut(previous: String, next: String): Pair<String, String>? {
    if (next.length != previous.length + 1) return null
    val rules = listOf(
        "### " to NoteBlockKind.HEADING3, "## " to NoteBlockKind.HEADING2, "# " to NoteBlockKind.HEADING,
        "- " to NoteBlockKind.BULLET, "* " to NoteBlockKind.BULLET, "1. " to NoteBlockKind.NUMBERED,
        "[] " to NoteBlockKind.TODO, "> " to NoteBlockKind.TOGGLE, "\" " to NoteBlockKind.QUOTE,
        "```" to NoteBlockKind.CODE, "---" to NoteBlockKind.DIVIDER
    )
    val hit = rules.firstOrNull { (prefix, _) -> next.startsWith(prefix) && !previous.startsWith(prefix) } ?: return null
    return hit.second to next.removePrefix(hit.first)
}

/** Remove padding-only edge lines without flattening intentional line breaks or indentation. */
internal fun compactNoteBlockText(text: String): String = text.lines()
    .dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }.joinToString("\n")

/** What Backspace at the very start of a block does (BlockNote-style, native rules). */
internal enum class NoteBackspace {
    /** Heading / checklist → plain text, text kept. */
    TO_TEXT,
    /** Empty block → removed, caret to the end of the block above. */
    DELETE_FOCUS_PREVIOUS,
    /** Text joins the end of the block above. */
    MERGE_INTO_PREVIOUS,
    /** The divider right above is removed. */
    REMOVE_PREVIOUS,
    /** Empty block under media → removed, nothing to focus. */
    DELETE,
    /** Nested block → one level out first. */
    OUTDENT,
    NONE
}

internal fun noteBackspaceAtStart(current: TaskMemo, previous: TaskMemo?): NoteBackspace = when {
    current.kind.isNotBlank() && current.kind != NoteBlockKind.TEXT -> NoteBackspace.TO_TEXT
    current.indent > 0 -> NoteBackspace.OUTDENT
    previous == null -> NoteBackspace.NONE
    previous.kind == NoteBlockKind.DIVIDER || previous.kind == NoteBlockKind.TOC -> NoteBackspace.REMOVE_PREVIOUS
    previous.attachment.isNotBlank() || previous.kind == NoteBlockKind.EMBED || previous.kind == NoteBlockKind.PAGE ->
        if (current.text.isEmpty()) NoteBackspace.DELETE else NoteBackspace.NONE
    current.text.isEmpty() -> NoteBackspace.DELETE_FOCUS_PREVIOUS
    else -> NoteBackspace.MERGE_INTO_PREVIOUS
}

/** Number shown on a numbered-list block: counts the run of numbered siblings above it. */
internal fun noteListNumber(blocks: List<TaskMemo>, index: Int): Int {
    val block = blocks.getOrNull(index) ?: return 1
    var n = 1
    var i = index - 1
    while (i >= 0) {
        val b = blocks[i]
        if (b.indent > block.indent) { i--; continue }
        if (b.indent < block.indent || b.kind != NoteBlockKind.NUMBERED) break
        n++; i--
    }
    return n
}

/** Ids hidden because they sit inside a collapsed toggle (deeper indent right below it). */
internal fun collapsedNoteIds(blocks: List<TaskMemo>): Set<String> {
    val hidden = mutableSetOf<String>()
    var closedAt = -1 // indent of the collapsed toggle we are inside, or -1
    blocks.forEach { b ->
        if (closedAt >= 0 && b.indent > closedAt) { hidden += b.id; return@forEach }
        closedAt = if (NoteBlockKind.isToggle(b.kind) && !b.checked) b.indent else -1
    }
    return hidden
}

/** Known embed services, matched on the link's host. */
internal fun noteEmbedService(url: String): String? {
    val host = runCatching { java.net.URI(url.trim()).host?.lowercase(Locale.ROOT) }.getOrNull() ?: return null
    return when {
        host.endsWith("youtube.com") || host == "youtu.be" -> "YouTube"
        host.startsWith("maps.") || (host.contains("google.") && url.contains("/maps")) || host == "goo.gl" -> "Google Maps"
        host.endsWith("figma.com") -> "Figma"
        host == "gist.github.com" -> "GitHub Gist"
        host.endsWith("github.com") -> "GitHub"
        host == "drive.google.com" || host == "docs.google.com" -> "Google Drive"
        host == "x.com" || host.endsWith("twitter.com") -> "X (트윗)"
        host.endsWith("notion.so") || host.endsWith("notion.site") -> "Notion"
        host.endsWith("instagram.com") -> "Instagram"
        host.contains("naver") -> "Naver"
        else -> host.removePrefix("www.")
    }
}

internal fun isWebUrl(text: String) = Regex("^https?://\\S+$").matches(text.trim())

/**
 * Indents (+1) or outdents (−1) a block together with the blocks nested under it.
 * Returns null when the move is not allowed (first block, already at the edge).
 */
internal fun shiftNoteIndent(blocks: List<TaskMemo>, id: String, delta: Int): List<TaskMemo>? {
    val at = blocks.indexOfFirst { it.id == id }
    if (at < 0 || delta == 0) return null
    val block = blocks[at]
    val target = block.indent + delta
    if (target < 0 || target > MAX_NOTE_INDENT) return null
    if (delta > 0 && (at == 0 || target > blocks[at - 1].indent + 1)) return null
    var end = at + 1
    while (end < blocks.size && blocks[end].indent > block.indent) end++
    return blocks.mapIndexed { i, b ->
        if (i in at until end) b.copy(indent = (b.indent + delta).coerceIn(0, MAX_NOTE_INDENT)) else b
    }
}
