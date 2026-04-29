package com.bettertick.ui.screens.focus

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Storm
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

data class WhiteNoiseOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val WHITE_NOISE_OPTIONS = listOf(
    WhiteNoiseOption("none", "없음", Icons.Default.Block),
    WhiteNoiseOption("clock", "시계", Icons.Outlined.Schedule),
    WhiteNoiseOption("campfire", "야영불", Icons.Outlined.LocalFireDepartment),
    WhiteNoiseOption("boil", "끓기", Icons.Outlined.SoupKitchen),
    WhiteNoiseOption("temple", "사원 블럭", Icons.Outlined.NightsStay),
    WhiteNoiseOption("storm", "스톰", Icons.Outlined.Thunderstorm),
    WhiteNoiseOption("rain", "비", Icons.Outlined.WaterDrop),
    WhiteNoiseOption("cafe", "카페", Icons.Outlined.Coffee),
    WhiteNoiseOption("music_box", "음악 상자", Icons.Outlined.LibraryMusic),
    WhiteNoiseOption("morning", "아침", Icons.Outlined.Spa),
    WhiteNoiseOption("summer", "여름", Icons.Outlined.Cloud),
    WhiteNoiseOption("cricket", "짹짹", Icons.Outlined.Storm),
    WhiteNoiseOption("forest", "숲", Icons.Outlined.Forest),
    WhiteNoiseOption("stream", "스트림", Icons.Outlined.Water),
    WhiteNoiseOption("ocean", "바다", Icons.Outlined.Waves)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoisePickerScreen(
    selectedId: String,
    onPick: (WhiteNoiseOption) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            title = {
                Text(
                    text = "백색 소음",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(WHITE_NOISE_OPTIONS, key = { it.id }) { opt ->
                val selected = opt.id == selectedId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(opt) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else DarkSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            opt.icon,
                            contentDescription = opt.label,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
