package com.bettertick.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

private val FocusBlue = Color(0xFF4A6BD8)
private val RingTrack = Color(0xFF1F2230)
private val ControlBg = Color(0xFF1A1C24)

/**
 * Fullscreen running-timer view shown while a focus session is active.
 * Mirrors the TickTick reference: down chevron to collapse, large ring with
 * elapsed time, music / pause·resume / stop controls.
 *
 * The ring is decorative for now — a future iteration can animate progress
 * against a target duration. Currently it always renders the full track plus
 * a single dot at the 12 o'clock position so the visual matches the reference.
 */
@Composable
fun FocusTimerScreen(
    timerState: TimerState,
    timeText: String,
    onCollapse: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenSounds: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar — down chevron + decorative right icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "축소",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* brightness — placeholder */ }) {
                Icon(
                    Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            IconButton(onClick = { /* more — placeholder */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Category subtitle ("포커스 >") — picker entry, future: change category mid-session
        Text(
            text = if (timerState.categoryName.isBlank()) "포커스" else timerState.categoryName,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        // Big ring + elapsed time
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    drawCircle(
                        color = RingTrack,
                        radius = (size.minDimension - stroke) / 2,
                        style = Stroke(width = stroke)
                    )
                    // Top-of-ring marker dot
                    drawCircle(
                        color = FocusBlue,
                        radius = 6.dp.toPx(),
                        center = Offset(size.width / 2, stroke + 2.dp.toPx())
                    )
                }
                Text(
                    text = timeText,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleControl(
                size = 52.dp,
                background = ControlBg,
                onClick = onOpenSounds
            ) {
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = "백색 소음",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            CircleControl(
                size = 80.dp,
                background = FocusBlue,
                onClick = if (timerState.isPaused) onResume else onPause
            ) {
                Icon(
                    if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (timerState.isPaused) "재개" else "일시정지",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            CircleControl(
                size = 52.dp,
                background = ControlBg,
                onClick = onStop
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "정지",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Footer — note placeholder
        Text(
            text = "집중 노트 추가하기",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun CircleControl(
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
