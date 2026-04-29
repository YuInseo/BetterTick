package com.bettertick.ui.screens.focus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.focus.TimerState
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary

@Composable
fun RunningTimerBar(
    timerState: TimerState,
    timeText: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Category name
        Text(
            text = timerState.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        // Timer
        Text(
            text = timeText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Play/Pause
            IconButton(
                onClick = { if (timerState.isPaused) onResume() else onPause() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Orange)
            ) {
                Icon(
                    imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.3f))
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
