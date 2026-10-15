package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Habit
import com.bettertick.ui.screens.habits.HabitIconView
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState

/** A habit inside 오늘 / 다음 7일 (TickTick shows due habits there). */
@Composable
fun HabitTaskRow(habit: Habit, done: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitIconView(
            iconKey = habit.icon,
            colorHex = habit.color,
            circleSize = 26.dp,
            iconSize = 15.dp,
            fallbackText = habit.name
        )
        Spacer(Modifier.width(12.dp))
        Text(
            habit.name,
            fontSize = 15.sp,
            color = if (done) TextTertiary else TextPrimary,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val accent = ThemeState.accentColor
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(accent)
                    else Modifier.border(1.5.dp, TextTertiary, CircleShape)
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Default.Check, "체크인 취소", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
