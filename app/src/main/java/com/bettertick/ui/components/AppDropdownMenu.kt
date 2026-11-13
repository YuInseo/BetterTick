package com.bettertick.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * App-wide dropdown menu: Material's menu with rounded corners and a neutral
 * (not accent-tinted) raised surface. Material 1.2 takes the menu's shape and
 * tint from the theme only, so the theme is swapped just around the popup
 * and restored for the menu's content.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val typography = MaterialTheme.typography
    MaterialTheme(
        colorScheme = colors.copy(surfaceTint = colors.onSurface),
        shapes = shapes.copy(extraSmall = RoundedCornerShape(16.dp)),
        typography = typography
    ) {
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            scrollState = scrollState,
            properties = properties
        ) {
            MaterialTheme(colorScheme = colors, shapes = shapes, typography = typography) {
                content()
            }
        }
    }
}
