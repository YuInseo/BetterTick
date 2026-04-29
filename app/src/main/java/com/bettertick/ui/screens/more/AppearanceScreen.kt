package com.bettertick.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.ThemeState

data class ThemeColor(
    val name: String,
    val colors: List<Color>,
    val accentColor: Color = Color.Unspecified,
    // City / scene themes override the preview card with a full-bleed gradient.
    // Default themes leave these null and fall back to DarkCard.
    val backgroundColors: List<Color>? = null,
    val cardColor: Color? = null,
    val previewTextColor: Color? = null,
    // Vector silhouette drawn onto the preview card (replaces emoji).
    val cityLandmark: CityLandmark? = null,
    val isPremium: Boolean = false,
    // Shows a row of accent-color swatches below the preview — user can pick
    // any palette color and it becomes the applied accent. Used by 어둠.
    val hasCustomAccent: Boolean = false,
    // Description text shown below the preview card. Used by 재료 너.
    val description: String? = null
)

@Composable
fun AppearanceScreen(
    onBack: () -> Unit
) {
    val tabs = listOf("테마", "앱 아이콘", "표시")
    var selectedTab by remember { mutableIntStateOf(0) }
    val themeColors = remember {
        listOf(
            ThemeColor("기본값", listOf(Color(0xFF4A90D9), Color(0xFF357ABD)), accentColor = Color(0xFF4A90D9)),
            ThemeColor("티얼 블루", listOf(Color(0xFF7ED6DF), Color(0xFF22A6B3)), accentColor = Color(0xFF22A6B3)),
            ThemeColor("터쿼이즈", listOf(Color(0xFF7CEBC6), Color(0xFF55E6A0)), accentColor = Color(0xFF55E6A0)),
            ThemeColor("갈대 녹색", listOf(Color(0xFFD4E09B), Color(0xFFC5D86D)), accentColor = Color(0xFFC5D86D)),
            ThemeColor("살구 노랑", listOf(Color(0xFFFAD7A0), Color(0xFFF5CBA7)), accentColor = Color(0xFFF0B27A)),
            ThemeColor("복숭아", listOf(Color(0xFFF5B7B1), Color(0xFFF1948A)), accentColor = Color(0xFFF1948A)),
            ThemeColor("라일락", listOf(Color(0xFFD7BDE2), Color(0xFFBB8FCE)), accentColor = Color(0xFFBB8FCE)),
            ThemeColor("진주색", listOf(Color(0xFFF2F3F4), Color(0xFFE5E8E8)), accentColor = Color(0xFFD5DBDB)),
            ThemeColor("자갈색", listOf(Color(0xFFBDC3C7), Color(0xFF95A5A6)), accentColor = Color(0xFF95A5A6)),
            ThemeColor(
                name = "어둠",
                colors = listOf(Color(0xFF2C2C2C), Color(0xFF1A1A1A)),
                accentColor = Orange,
                hasCustomAccent = true
            ),
            ThemeColor(
                name = "재료 너",
                colors = listOf(Color(0xFF81FBB8), Color(0xFF28C76F), Color(0xFF7367F0)),
                accentColor = Color(0xFF28C76F),
                isPremium = true,
                description = "매번 핸드폰 배경화면을 변경할 때마다, 당신의 BetterTick 화면도 새롭게 갱신됩니다."
            ),
            // City / scene themes — premium. Preview card shows a full gradient
            // background with a Canvas-drawn landmark silhouette at the bottom.
            ThemeColor(
                name = "베이징",
                colors = listOf(Color(0xFFC9C5F7), Color(0xFFA5A0E8)),
                accentColor = Color(0xFF5BA7E0),
                backgroundColors = listOf(Color(0xFFCFCBF4), Color(0xFFB8B4F0), Color(0xFFE89AB8)),
                cardColor = Color(0xFFF3F1FE),
                previewTextColor = Color(0xFF1A1830),
                cityLandmark = CityLandmark.BeijingPearlTower,
                isPremium = true
            ),
            ThemeColor(
                name = "런던",
                colors = listOf(Color(0xFFD8C8E0), Color(0xFFB9A8CC)),
                accentColor = Color(0xFF3B2F7E),
                backgroundColors = listOf(Color(0xFFDCCEE6), Color(0xFFC6B8D7)),
                cardColor = Color(0xFFEEE6F2),
                previewTextColor = Color(0xFF1F1A35),
                cityLandmark = CityLandmark.LondonBigBen,
                isPremium = true
            ),
            ThemeColor(
                name = "모스크바",
                colors = listOf(Color(0xFFC8E0E8), Color(0xFFB0CEDB)),
                accentColor = Color(0xFF0E4F68),
                backgroundColors = listOf(Color(0xFFD4E9F0), Color(0xFFBFDAE4)),
                cardColor = Color(0xFFEAF4F8),
                previewTextColor = Color(0xFF0F2A3A),
                cityLandmark = CityLandmark.MoscowStBasils,
                isPremium = true
            ),
            ThemeColor(
                name = "샌프란시스코",
                colors = listOf(Color(0xFFFFD0C1), Color(0xFFFFAA8A)),
                accentColor = Color(0xFFFF6D2C),
                backgroundColors = listOf(Color(0xFFFFD5C4), Color(0xFFFFB8A0)),
                cardColor = Color(0xFFFFE7DC),
                previewTextColor = Color(0xFF3A1D0F),
                cityLandmark = CityLandmark.SanFranciscoGoldenGate,
                isPremium = true
            ),
            ThemeColor(
                name = "서울",
                colors = listOf(Color(0xFFCDEDE6), Color(0xFFA5D8CC)),
                accentColor = Color(0xFF3FB7A0),
                backgroundColors = listOf(Color(0xFFD5F0E8), Color(0xFFB6E0D2)),
                cardColor = Color(0xFFEAF7F2),
                previewTextColor = Color(0xFF0F3A31),
                cityLandmark = CityLandmark.SeoulHanok,
                isPremium = true
            ),
            ThemeColor(
                name = "도쿄",
                colors = listOf(Color(0xFFFFD1DC), Color(0xFFFFA8BD)),
                accentColor = Color(0xFFE8577E),
                backgroundColors = listOf(Color(0xFFFFDCE5), Color(0xFFFFB9CC)),
                cardColor = Color(0xFFFFEEF2),
                previewTextColor = Color(0xFF3A0F1E),
                cityLandmark = CityLandmark.TokyoTower,
                isPremium = true
            )
        )
    }
    var selectedTheme by remember { mutableStateOf(
        themeColors.find { it.accentColor == ThemeState.accentColor }?.name ?: "어둠"
    ) }
    var detailTheme by remember { mutableStateOf<ThemeColor?>(null) }

    // Show detail screen when a color is tapped — preview + apply.
    // Pass the full list so the user can swipe left/right between themes.
    detailTheme?.let { theme ->
        ColorThemeDetailScreen(
            themes = themeColors,
            initialIndex = themeColors.indexOf(theme).coerceAtLeast(0),
            onBack = { detailTheme = null },
            onApply = { applied ->
                selectedTheme = applied.name
                ThemeState.setTheme(applied.accentColor)
                detailTheme = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Back button + Tabs in same row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        // Content
        when (selectedTab) {
            0 -> ThemeTabContent(
                themeColors = themeColors,
                selectedTheme = selectedTheme,
                onThemeSelected = { theme ->
                    detailTheme = theme
                }
            )
            1 -> PlaceholderContent("앱 아이콘 설정")
            2 -> PlaceholderContent("표시 설정")
        }
    }
}

@Composable
private fun ThemeTabContent(
    themeColors: List<ThemeColor>,
    selectedTheme: String,
    onThemeSelected: (ThemeColor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dark mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .clickable {}
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "시스템 어두운 모드로 이동",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "켜기",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Color themes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(16.dp)
        ) {
            Text(
                text = "색상 시리즈",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grid of theme colors - using rows instead of LazyVerticalGrid to avoid nested scroll
            val rows = themeColors.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { theme ->
                        ThemeColorCard(
                            theme = theme,
                            isSelected = theme.name == selectedTheme,
                            onClick = { onThemeSelected(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if row is not full
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ThemeColorCard(
    theme: ThemeColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cache brush so it's not re-created on each recomposition.
    val brush = remember(theme) { Brush.linearGradient(theme.colors) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) Modifier.border(2.dp, Orange, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .background(brush = brush)
                .clickable { onClick() }
        ) {
            // Vector landmark silhouette — bottom half of the card
            if (theme.cityLandmark != null) {
                CityLandmarkIllustration(
                    landmark = theme.cityLandmark,
                    color = theme.accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                )
            }
            // Premium crown badge top-left
            if (theme.isPremium) {
                Text(
                    text = "👑",
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 4.dp, y = 4.dp)
                )
            }
            // Selected check top-right
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Orange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.name,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun PlaceholderContent(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}
