package com.bettertick.ui.screens.focus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bettertick.data.model.FocusCategory
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary

@Composable
fun ActivityCategoryCard(
    category: FocusCategory,
    todayMinutes: Long,
    isTimerRunning: Boolean,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 행 전체를 누르면 포모(풀스크린 타이머)가 뜨도록. 우측 ▶ 버튼만 누를
    // 수 있게 하면 발견성도 떨어지고 터치 타겟도 작아 누르기 어려움.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(enabled = !isTimerRunning) { onStartClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${todayMinutes}m",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        IconButton(
            onClick = onStartClick,
            enabled = !isTimerRunning
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start ${category.name}",
                tint = if (isTimerRunning) TextSecondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
