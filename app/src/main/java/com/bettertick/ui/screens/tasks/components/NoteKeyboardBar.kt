package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.NoteBlockKind
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

/** What the area under the keyboard bar shows instead of the keyboard. */
internal enum class NotePanel { NONE, ADD, TURN, COLOR }

/**
 * Bar right above the keyboard while a block is edited (Notion style). It scrolls
 * sideways; the keyboard button stays pinned on the right.
 */
@Composable
internal fun BlockKeyboardBar(
    panel: NotePanel,
    canUndo: Boolean,
    canRedo: Boolean,
    onPanel: (NotePanel) -> Unit,
    onFormat: (InlineMark) -> Unit,
    onIndent: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onQuick: (kind: String) -> Unit,
    onImage: () -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().testTag("block-keyboard-bar")) {
        HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f))
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                BarButton("블록 추가", selected = panel == NotePanel.ADD, onClick = { onPanel(NotePanel.ADD) }) {
                    Icon(Icons.Outlined.Add, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
                BarButton("블록 종류 바꾸기", selected = panel == NotePanel.TURN, onClick = { onPanel(NotePanel.TURN) }) {
                    Text("Aa", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                BarButton("굵게", onClick = { onFormat(InlineMark.BOLD) }) {
                    Text("B", color = TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                BarButton("기울임", onClick = { onFormat(InlineMark.ITALIC) }) {
                    Text("I", color = TextSecondary, fontSize = 17.sp, fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif)
                }
                BarButton("밑줄", onClick = { onFormat(InlineMark.UNDERLINE) }) {
                    Text("U", color = TextSecondary, fontSize = 16.sp, textDecoration = TextDecoration.Underline)
                }
                BarButton("취소선", onClick = { onFormat(InlineMark.STRIKE) }) {
                    Text("S", color = TextSecondary, fontSize = 16.sp, textDecoration = TextDecoration.LineThrough)
                }
                BarButton("링크", onClick = { onFormat(InlineMark.LINK) }) {
                    Icon(Icons.Outlined.Link, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("인라인 코드", onClick = { onFormat(InlineMark.CODE) }) {
                    Icon(Icons.Outlined.Code, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("형광펜", onClick = { onFormat(InlineMark.HIGHLIGHT) }) {
                    Icon(Icons.Outlined.BorderColor, null, tint = TextSecondary, modifier = Modifier.size(19.dp))
                }
                BarButton("색", selected = panel == NotePanel.COLOR, onClick = { onPanel(NotePanel.COLOR) }) {
                    Text("A", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.border(1.dp, TextTertiary, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp))
                }
                BarButton("체크리스트", onClick = { onQuick(NoteBlockKind.TODO) }) {
                    Icon(Icons.Outlined.CheckBox, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("글머리 기호", onClick = { onQuick(NoteBlockKind.BULLET) }) {
                    Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("들여쓰기", onClick = { onIndent(1) }) {
                    Icon(Icons.AutoMirrored.Outlined.FormatIndentIncrease, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("내어쓰기", onClick = { onIndent(-1) }) {
                    Icon(Icons.AutoMirrored.Outlined.FormatIndentDecrease, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("이미지 · 파일", onClick = onImage) {
                    Icon(Icons.Outlined.Image, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                BarButton("실행 취소", enabled = canUndo, onClick = onUndo) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = if (canUndo) TextSecondary else TextTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp))
                }
                BarButton("다시 실행", enabled = canRedo, onClick = onRedo) {
                    Icon(Icons.AutoMirrored.Outlined.Redo, null, tint = if (canRedo) TextSecondary else TextTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp))
                }
            }
            Box(Modifier.width(1.dp).height(28.dp).background(TextTertiary.copy(alpha = 0.2f)))
            BarButton("키보드 닫기", onClick = onDone) {
                Icon(Icons.Outlined.KeyboardHide, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BarButton(desc: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit,
                      content: @Composable () -> Unit) {
    Box(
        Modifier.padding(horizontal = 1.dp).size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) TextTertiary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(enabled = enabled, onClickLabel = desc) { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

private data class PanelItem(val label: String, val kind: String, val icon: ImageVector? = null, val glyph: String = "")

private val BasicItems = listOf(
    PanelItem("텍스트", NoteBlockKind.TEXT, Icons.Outlined.TextFields),
    PanelItem("제목1", NoteBlockKind.HEADING, glyph = "H1"),
    PanelItem("제목2", NoteBlockKind.HEADING2, glyph = "H2"),
    PanelItem("제목3", NoteBlockKind.HEADING3, glyph = "H3"),
    PanelItem("체크리스트", NoteBlockKind.TODO, Icons.Outlined.CheckBox),
    PanelItem("글머리 기호 목록", NoteBlockKind.BULLET, Icons.AutoMirrored.Outlined.FormatListBulleted),
    PanelItem("번호 매기기 목록", NoteBlockKind.NUMBERED, Icons.Outlined.FormatListNumbered),
    PanelItem("토글 목록", NoteBlockKind.TOGGLE, Icons.Outlined.ArrowRight),
    PanelItem("인용", NoteBlockKind.QUOTE, Icons.Outlined.FormatQuote),
    PanelItem("콜아웃", NoteBlockKind.CALLOUT, Icons.Outlined.Lightbulb),
    PanelItem("코드", NoteBlockKind.CODE, Icons.Outlined.Code),
    PanelItem("토글 제목1", NoteBlockKind.TOGGLE_H1, glyph = "▸H1"),
    PanelItem("토글 제목2", NoteBlockKind.TOGGLE_H2, glyph = "▸H2"),
    PanelItem("토글 제목3", NoteBlockKind.TOGGLE_H3, glyph = "▸H3")
)

private const val ATTACH = "__attach"

private val MediaItems = listOf(
    PanelItem("이미지 · 파일", ATTACH, Icons.Outlined.Image)
)

private val AdvancedItems = listOf(
    PanelItem("구분선", NoteBlockKind.DIVIDER, Icons.Outlined.HorizontalRule),
    PanelItem("목차", NoteBlockKind.TOC, Icons.AutoMirrored.Outlined.ViewList)
)

private val EmbedItems = listOf(
    PanelItem("링크 · 임베드", NoteBlockKind.EMBED, Icons.Outlined.Link),
    PanelItem("YouTube", NoteBlockKind.EMBED, Icons.Outlined.PlayCircle),
    PanelItem("Google Maps", NoteBlockKind.EMBED, Icons.Outlined.Map),
    PanelItem("Google Drive", NoteBlockKind.EMBED, Icons.Outlined.Cloud),
    PanelItem("Figma", NoteBlockKind.EMBED, Icons.Outlined.Brush),
    PanelItem("GitHub Gist", NoteBlockKind.EMBED, Icons.Outlined.Code),
    PanelItem("X (트윗)", NoteBlockKind.EMBED, Icons.Outlined.Tag),
    PanelItem("웹 북마크", NoteBlockKind.EMBED, Icons.Outlined.Public)
)

/**
 * Shown in place of the keyboard. [turnInto] lists only blocks a text block can
 * become; otherwise every block is offered, grouped like Notion's + menu.
 */
@Composable
internal fun BlockPickerPanel(turnInto: Boolean, onPick: (kind: String) -> Unit, onAttach: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp).testTag("block-picker-panel")) {
        if (turnInto) {
            PanelSection("블록 종류 바꾸기", BasicItems, onPick, onAttach)
        } else {
            PanelSection("기본 블록", BasicItems, onPick, onAttach)
            PanelSection("미디어", MediaItems, onPick, onAttach)
            PanelSection("고급 블록", AdvancedItems, onPick, onAttach)
            PanelSection("임베드", EmbedItems, onPick, onAttach)
        }
    }
}

@Composable
private fun PanelSection(title: String, items: List<PanelItem>, onPick: (String) -> Unit, onAttach: () -> Unit) {
    Text(title, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp))
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { item ->
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .clickable { if (item.kind == ATTACH) onAttach() else onPick(item.kind) }
                        .padding(horizontal = 8.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        if (item.icon != null) Icon(item.icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                        else Text(item.glyph, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(item.label, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Text colour and background colour for the edited block. */
@Composable
internal fun BlockColorPanel(current: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp).testTag("block-color-panel")) {
        listOf("글자 색" to "", "배경 색" to "bg_").forEach { (title, prefix) ->
            Text(title, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp))
            NoteColors.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (name, label) ->
                        val value = if (name.isEmpty()) "" else prefix + name
                        val swatch = noteColor(name)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onPick(value) }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape)
                                    .background(if (prefix.isNotEmpty() && swatch != null) swatch.copy(alpha = 0.3f) else Color.Transparent)
                                    .border(if (value == current) 2.dp else 1.dp,
                                        if (value == current) TextPrimary else TextTertiary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = if (prefix.isEmpty()) swatch ?: TextPrimary else TextPrimary,
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
