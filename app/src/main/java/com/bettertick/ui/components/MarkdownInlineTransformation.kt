package com.bettertick.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.bettertick.ui.theme.DarkSurfaceVariant

/**
 * Inline markdown renderer for editable fields. Handles four syntaxes —
 * `**bold**`, `*italic*`, `~~strike~~`, `` `code` `` — and collapses the
 * marker characters once the user commits the pair by typing a space or
 * newline after the closing marker. Deleting the committing whitespace
 * restores the markers automatically because collapse is re-computed from
 * the raw text each render.
 *
 * Why this design:
 *  - The user sees plain styled text ("테스트" italic) instead of noisy
 *    `*테스트*` once they've moved past the pair.
 *  - While still typing inside the pair (no trailing space yet), the
 *    markers remain visible so the author can see what's being edited.
 *  - Backspace through the trailing space uncollapses naturally — the
 *    pair is no longer "committed", so markers are re-shown on the next
 *    render.
 *
 * OffsetMapping contract: `originalToTransformed(i)` counts non-hidden
 * chars before position i in the raw text; `transformedToOriginal(t)`
 * finds the first raw index whose prefix has exactly t visible chars.
 * When that index lands inside a hidden marker run, it's skipped forward
 * past the run so the caret never visually parks on a hidden char.
 */
class MarkdownInlineTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(raw.toAnnotatedString(), OffsetMapping.Identity)

        val spans = parseSpans(raw)

        val hidden = BooleanArray(raw.length)
        for (s in spans) {
            if (s.collapsed) {
                for (i in s.openStart until s.openEnd) hidden[i] = true
                for (i in s.closeStart until s.closeEnd) hidden[i] = true
            }
        }

        val map = IntArray(raw.length + 1)
        val sb = StringBuilder()
        var cur = 0
        for (i in 0..raw.length) {
            map[i] = cur
            if (i < raw.length && !hidden[i]) {
                sb.append(raw[i])
                cur++
            }
        }

        val annotated = buildAnnotatedString {
            append(sb.toString())
            for (s in spans) {
                val st = map[s.contentStart]
                val en = map[s.contentEnd]
                if (st < en) addStyle(s.style, st, en)
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                map[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int {
                // Binary-search the raw index whose visible prefix is `offset`.
                var lo = 0
                var hi = raw.length
                while (lo < hi) {
                    val mid = (lo + hi) ushr 1
                    if (map[mid] < offset) lo = mid + 1 else hi = mid
                }
                // Skip forward through any hidden run — stops the caret from
                // reporting a position that's invisible on screen.
                while (lo < raw.length && hidden[lo]) lo++
                return lo
            }
        }

        return TransformedText(annotated, mapping)
    }

    private data class ParsedSpan(
        val openStart: Int,
        val openEnd: Int,
        val contentStart: Int,
        val contentEnd: Int,
        val closeStart: Int,
        val closeEnd: Int,
        val style: SpanStyle,
        val collapsed: Boolean
    )

    private fun parseSpans(raw: String): List<ParsedSpan> {
        val out = mutableListOf<ParsedSpan>()
        var i = 0
        while (i < raw.length) {
            val matched = when {
                raw.startsWith("**", i) -> tryPair(raw, i, "**", boldStyle)
                raw.startsWith("~~", i) -> tryPair(raw, i, "~~", strikeStyle)
                raw[i] == '*' -> tryPair(raw, i, "*", italicStyle)
                raw[i] == '`' -> tryPair(raw, i, "`", codeStyle)
                else -> null
            }
            if (matched != null) {
                out += matched
                i = matched.closeEnd
            } else {
                i++
            }
        }
        return out
    }

    private fun tryPair(raw: String, start: Int, marker: String, style: SpanStyle): ParsedSpan? {
        val contentStart = start + marker.length
        val closeStart = raw.indexOf(marker, contentStart)
        if (closeStart == -1 || closeStart <= contentStart) return null
        // For asterisks, reject an immediately-repeated marker so that `**`
        // isn't misread as two `*` pairs around an empty string.
        if (marker == "*" && raw.getOrNull(contentStart) == '*') return null
        val closeEnd = closeStart + marker.length
        val collapsed = shouldCollapse(raw, closeEnd)
        return ParsedSpan(
            openStart = start,
            openEnd = contentStart,
            contentStart = contentStart,
            contentEnd = closeStart,
            closeStart = closeStart,
            closeEnd = closeEnd,
            style = style,
            collapsed = collapsed
        )
    }

    private fun shouldCollapse(raw: String, closeEnd: Int): Boolean {
        if (closeEnd >= raw.length) return false
        val c = raw[closeEnd]
        return c == ' ' || c == '\n' || c == '\t'
    }

    private fun String.toAnnotatedString(): AnnotatedString = buildAnnotatedString { append(this@toAnnotatedString) }

    companion object {
        private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
        private val strikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
        private val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = DarkSurfaceVariant)
    }
}
