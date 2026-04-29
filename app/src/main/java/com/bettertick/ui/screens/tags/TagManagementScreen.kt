package com.bettertick.ui.screens.tags

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

/**
 * Tag management screen reached from the drawer's long-press on 태그 → 편집.
 * Matches the "스마트 목록 / 태그" reference: top bar with two tabs (only 태그
 * is interactive here), a "비어있지 않으면 표시" section header, a 태그 추가
 * row that opens AddTagScreen, then a flat list of every tag. Each row
 * navigates into the edit form for that tag.
 */
@Composable
fun TagManagementScreen(
    onBack: () -> Unit,
    onAddTag: () -> Unit,
    onEditTag: (String) -> Unit,
    viewModel: AddTagViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 40.dp)
    ) {
        // Top bar — back / "스마트 목록" (inactive) / "태그" (active)
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "스마트 목록",
                color = TextTertiary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "태그",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(Color(0xFF4A90E2))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "비어있지 않으면 표시" header card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Label,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "태그",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "비어있지 않으면 표시",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ">",
                color = TextSecondary,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tag list card — 태그 추가 row at top, then every tag
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddTag() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "태그 추가",
                    color = Color(0xFF4A90E2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(tags) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTag(tag.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = runCatching {
                                Color(android.graphics.Color.parseColor(tag.color))
                            }.getOrDefault(TextSecondary),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tag.name,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
