package com.bettertick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TickTick floating add button — a solid accent-colored circle with a white
 * "+" and a soft drop shadow, docked bottom-right above the tab bar.
 */
@Composable
fun TickFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = "추가",
    size: Dp = 56.dp
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = accent,
                spotColor = accent
            )
            .clip(CircleShape)
            .background(accent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
