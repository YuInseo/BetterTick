package com.bettertick.ui.screens.tasks.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * Inline styles are kept as small markdown markers inside the block text, so the
 * stored document stays plain text (search, export and old app versions keep working):
 * `**bold**`, `*italic*`, `__underline__`, `~~strike~~`, `` `code` ``, `==highlight==`
 * and `[text](url)`. While editing, the markers are hidden and the text is styled.
 */
internal enum class InlineMark(val open: String, val close: String) {
    BOLD("**", "**"), ITALIC("*", "*"), UNDERLINE("__", "__"), STRIKE("~~", "~~"),
    CODE("`", "`"), HIGHLIGHT("==", "=="), LINK("[", "]()")
}

internal data class InlineSpan(val mark: InlineMark, val start: Int, val end: Int, val url: String = "")

internal class InlineParse(val spans: List<InlineSpan>, val hidden: BooleanArray)

/** Finds styled runs and the marker characters to hide. Unclosed markers stay visible. */
internal fun parseInline(text: String): InlineParse {
    val hidden = BooleanArray(text.length)
    val spans = mutableListOf<InlineSpan>()
    // Code first: nothing inside a code run is parsed further.
    val codeRanges = mutableListOf<IntRange>()
    Regex("`([^`\n]+)`").findAll(text).forEach { m ->
        hidden[m.range.first] = true; hidden[m.range.last] = true
        spans += InlineSpan(InlineMark.CODE, m.range.first + 1, m.range.last)
        codeRanges += m.range
    }
    fun free(range: IntRange) = codeRanges.none { it.first <= range.last && range.first <= it.last }
    Regex("\\[([^\\]\n]+)]\\(([^)\\s]*)\\)").findAll(text).filter { free(it.range) }.forEach { m ->
        val labelEnd = m.range.first + 1 + m.groupValues[1].length
        hidden[m.range.first] = true
        for (i in labelEnd..m.range.last) hidden[i] = true
        spans += InlineSpan(InlineMark.LINK, m.range.first + 1, labelEnd, m.groupValues[2])
    }
    listOf(
        InlineMark.BOLD to Regex("\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*"),
        InlineMark.UNDERLINE to Regex("__(?=\\S)(.+?)(?<=\\S)__"),
        InlineMark.STRIKE to Regex("~~(?=\\S)(.+?)(?<=\\S)~~"),
        InlineMark.HIGHLIGHT to Regex("==(?=\\S)(.+?)(?<=\\S)=="),
        InlineMark.ITALIC to Regex("(?<![*\\\\])\\*(?![*\\s])(.+?)(?<![*\\s])\\*(?!\\*)")
    ).forEach { (mark, regex) ->
        regex.findAll(text).filter { m -> free(m.range) && '\n' !in m.value &&
            // A marker that another style already hid is not reused (italic inside bold).
            !hidden[m.range.first] && !hidden[m.range.last]
        }.forEach { m ->
            val w = mark.open.length
            for (i in 0 until w) { hidden[m.range.first + i] = true; hidden[m.range.last - i] = true }
            spans += InlineSpan(mark, m.range.first + w, m.range.last - w + 1)
        }
    }
    return InlineParse(spans, hidden)
}

/** Links found in a block, for the "open link" chips. */
internal fun inlineLinks(text: String): List<String> =
    parseInline(text).spans.filter { it.mark == InlineMark.LINK && it.url.isNotBlank() }.map { it.url }

/**
 * Wraps the selection in [mark] (or unwraps it when it is already wrapped). With no
 * selection, an empty pair is inserted and the caret goes between the markers. A link
 * puts the caret inside the "()" so the address can be typed right away.
 */
internal fun toggleInlineMark(value: TextFieldValue, mark: InlineMark): TextFieldValue {
    val text = value.text
    val start = value.selection.min
    val end = value.selection.max
    val open = mark.open
    val close = if (mark == InlineMark.LINK) "](" else mark.close
    val tail = if (mark == InlineMark.LINK) ")" else ""
    // Already wrapped right around the selection → remove the markers.
    if (mark != InlineMark.LINK && start >= open.length && end + close.length <= text.length &&
        text.substring(start - open.length, start) == open && text.substring(end, end + close.length) == close
    ) {
        val unwrapped = text.removeRange(end, end + close.length).removeRange(start - open.length, start)
        return TextFieldValue(unwrapped, TextRange(start - open.length, end - open.length))
    }
    val inner = text.substring(start, end)
    val wrapped = text.substring(0, start) + open + inner + close + tail + text.substring(end)
    val caret = when {
        mark == InlineMark.LINK -> start + open.length + inner.length + close.length
        start == end -> start + open.length
        else -> -1
    }
    return if (caret >= 0) TextFieldValue(wrapped, TextRange(caret))
    else TextFieldValue(wrapped, TextRange(start + open.length, end + open.length))
}

/** Hides markers and styles the runs; offsets are mapped so the caret never lands oddly. */
internal class InlineFormatTransformation(private val linkColor: Color, private val codeBackground: Color,
                                          private val highlight: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val parse = parseInline(raw)
        if (parse.spans.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val toShown = IntArray(raw.length + 1)
        val toRaw = ArrayList<Int>(raw.length + 1)
        val shown = StringBuilder()
        for (i in raw.indices) {
            toShown[i] = shown.length
            if (!parse.hidden[i]) { toRaw += i; shown.append(raw[i]) }
        }
        toShown[raw.length] = shown.length
        toRaw += raw.length
        val builder = AnnotatedString.Builder(shown.toString())
        parse.spans.forEach { span ->
            val s = toShown[span.start.coerceIn(0, raw.length)]
            val e = toShown[span.end.coerceIn(0, raw.length)]
            if (e <= s) return@forEach
            builder.addStyle(when (span.mark) {
                InlineMark.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                InlineMark.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                InlineMark.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                InlineMark.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                InlineMark.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground, color = Color(0xFFEB5757))
                InlineMark.HIGHLIGHT -> SpanStyle(background = highlight)
                InlineMark.LINK -> SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            }, s, e)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toShown[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int) = toRaw[offset.coerceIn(0, toRaw.lastIndex)]
        }
        return TransformedText(builder.toAnnotatedString(), mapping)
    }

    override fun equals(other: Any?) = other is InlineFormatTransformation && other.linkColor == linkColor &&
        other.codeBackground == codeBackground && other.highlight == highlight
    override fun hashCode() = linkColor.hashCode() * 31 + codeBackground.hashCode()
}

/** Block colours, Notion palette (text colour, then background tint). */
internal val NoteColors = listOf(
    "" to "기본", "gray" to "회색", "brown" to "갈색", "orange" to "주황", "yellow" to "노랑",
    "green" to "초록", "blue" to "파랑", "purple" to "보라", "pink" to "분홍", "red" to "빨강"
)

internal fun noteColor(name: String): Color? = when (name.removePrefix("bg_")) {
    "gray" -> Color(0xFF9B9A97); "brown" -> Color(0xFFBA856F); "orange" -> Color(0xFFC77D48)
    "yellow" -> Color(0xFFCA9849); "green" -> Color(0xFF529E72); "blue" -> Color(0xFF5E87C9)
    "purple" -> Color(0xFF9D68D3); "pink" -> Color(0xFFD15796); "red" -> Color(0xFFDF5452)
    else -> null
}
