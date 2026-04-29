package com.bettertick.ui.screens.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.QuadrantConfig
import com.bettertick.data.model.defaultMatrix
import com.bettertick.data.model.priorityLabel
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

/**
 * "행렬 편집" — list of the four quadrant rows. Tapping a row opens
 * [QuadrantEditScreen] for that quadrant. 예시 at the bottom resets to the
 * default layout (Urgent/Important split by priority).
 */
@Composable
fun MatrixEditScreen(
    onBack: () -> Unit,
    onEditQuadrant: (String) -> Unit,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "행렬 편집",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            config.quadrants.forEach { q ->
                QuadrantSummaryRow(
                    quadrant = q,
                    onClick = { onEditQuadrant(q.id) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { viewModel.saveConfig(defaultMatrix) }) {
                Text(
                    text = "예시",
                    color = Color(0xFF4A90E2),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuadrantSummaryRow(
    quadrant: QuadrantConfig,
    onClick: () -> Unit
) {
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(quadrant.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quadrant.id,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quadrant.nameEn.ifEmpty { quadrant.nameKo },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            val summary = summaryFor(quadrant)
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Reorder,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** One-liner summary of the active filters for the row preview. Only
 *  non-default facets show up so the label doesn't turn into a kitchen sink. */
private fun summaryFor(q: QuadrantConfig): String {
    val parts = mutableListOf<String>()
    if (q.listIds.isNotEmpty()) parts += "목록 ${q.listIds.size}개"
    when (QuadrantConfig.TagMode.from(q.tagMode)) {
        QuadrantConfig.TagMode.Has -> parts += "태그 포함"
        QuadrantConfig.TagMode.Lacks -> parts += "태그 제외"
        QuadrantConfig.TagMode.Any -> Unit
    }
    when (QuadrantConfig.DateMode.from(q.dateMode)) {
        QuadrantConfig.DateMode.All -> Unit
        else -> parts += QuadrantConfig.DateMode.from(q.dateMode).label
    }
    if (q.priority in 0..3) parts += q.priorityLabel()
    when (QuadrantConfig.TypeMode.from(q.typeMode)) {
        QuadrantConfig.TypeMode.All -> Unit
        else -> parts += QuadrantConfig.TypeMode.from(q.typeMode).label
    }
    return parts.joinToString(" & ")
}
