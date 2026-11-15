package com.bettertick.ui.screens.tasks.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class NoteInlineFormatTest {
    private fun shown(text: String): String {
        val p = parseInline(text)
        return text.filterIndexed { i, _ -> !p.hidden[i] }
    }

    @Test fun markersAreHiddenAndStyled() {
        assertEquals("굵게 and 기울임", shown("**굵게** and *기울임*"))
        assertEquals("a b c d", shown("__a__ ~~b~~ `c` ==d=="))
        assertEquals("링크", shown("[링크](https://x.y)"))
        assertEquals(listOf("https://x.y"), inlineLinks("[링크](https://x.y) 끝"))
        val marks = parseInline("**a** *b*").spans.map { it.mark }.toSet()
        assertEquals(setOf(InlineMark.BOLD, InlineMark.ITALIC), marks)
    }

    @Test fun unclosedMarkersStayVisible() {
        assertEquals("**미완", shown("**미완"))
        assertEquals("2 * 3", shown("2 * 3"))
    }

    @Test fun codeContentIsNotParsed() {
        assertEquals("**x**", shown("`**x**`"))
    }

    @Test fun toggleWrapsAndUnwrapsTheSelection() {
        val wrapped = toggleInlineMark(TextFieldValue("가나다", TextRange(0, 2)), InlineMark.BOLD)
        assertEquals("**가나**다", wrapped.text)
        assertEquals(TextRange(2, 4), wrapped.selection)
        val back = toggleInlineMark(wrapped, InlineMark.BOLD)
        assertEquals("가나다", back.text)
        assertEquals(TextRange(0, 2), back.selection)
    }

    @Test fun emptySelectionPutsTheCaretBetweenMarkers() {
        val v = toggleInlineMark(TextFieldValue("ab", TextRange(1)), InlineMark.STRIKE)
        assertEquals("a~~~~b", v.text)
        assertEquals(TextRange(3), v.selection)
        val link = toggleInlineMark(TextFieldValue("링크", TextRange(0, 2)), InlineMark.LINK)
        assertEquals("[링크]()", link.text)
        assertEquals(TextRange(5), link.selection)
    }
}
