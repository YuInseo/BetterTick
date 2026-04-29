package com.bettertick.ui.screens.more

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState

/**
 * Swipeable theme preview. Each page is a calendar mockup tinted with that
 * theme's accent. Adjacent themes peek from the sides so the user can swipe
 * to browse and tap "사용" to apply the centered one.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColorThemeDetailScreen(
    themes: List<ThemeColor>,
    initialIndex: Int,
    onBack: () -> Unit,
    onApply: (ThemeColor) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (themes.size - 1).coerceAtLeast(0))
    ) { themes.size }

    val currentTheme by remember {
        derivedStateOf { themes.getOrNull(pagerState.currentPage) ?: themes.first() }
    }

    // Selected accent for themes that expose a custom palette (e.g. 어둠).
    // Reset back to the theme's default whenever the user swipes to a
    // different theme, so picks don't leak across pages.
    var customAccent by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(pagerState.currentPage) {
        customAccent = null
    }
    val effectiveAccent =
        if (currentTheme.hasCustomAccent) customAccent ?: currentTheme.accentColor
        else currentTheme.accentColor
    val isCurrentlyApplied = effectiveAccent == ThemeState.accentColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTheme.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (currentTheme.isPremium) {
                        Spacer(Modifier.size(6.dp))
                        Text(text = "👑", fontSize = 16.sp)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 8.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val theme = themes[page]
            // Only the currently-centered theme reflects the live custom
            // accent; other pages keep their own accent so peeking stays
            // visually accurate.
            val accent =
                if (page == pagerState.currentPage && theme.hasCustomAccent) effectiveAccent
                else theme.accentColor
            ThemePreviewCard(theme = theme, accentOverride = accent)
        }

        // Optional description text (e.g. 재료 너 wallpaper-sync explainer)
        currentTheme.description?.let { desc ->
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        // Optional custom accent palette (어둠)
        if (currentTheme.hasCustomAccent) {
            CustomAccentPalette(
                selected = effectiveAccent,
                onSelect = { customAccent = it },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }

        // Apply button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isCurrentlyApplied) effectiveAccent.copy(alpha = 0.4f)
                    else effectiveAccent
                )
                .clickable(enabled = !isCurrentlyApplied) {
                    onApply(currentTheme.copy(accentColor = effectiveAccent))
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCurrentlyApplied) "사용 중" else "사용",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// Palette shown below the 어둠 preview. User picks any swatch and it becomes
// the applied accent color while keeping the dark background.
private val CustomAccentSwatches = listOf(
    Color(0xFFFF8C00), // Orange (default)
    Color(0xFF4A8FE3), // Blue
    Color(0xFF7BB7E8), // Sky blue
    Color(0xFF22A6B3), // Teal
    Color(0xFFF0B27A), // Sand
    Color(0xFFE8A5B5), // Pink
    Color(0xFFBB8FCE)  // Purple
)

@Composable
private fun CustomAccentPalette(
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomAccentSwatches.forEach { color ->
            val isPicked = color == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isPicked) {
                    // White ring inside the swatch to indicate selection
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawCircle(
                                color = Color.White,
                                radius = size.minDimension * 0.5f - 2f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                            )
                        }
                    }
                }
            }
        }
        // Rainbow / conic-gradient wheel placeholder — cycles to the next
        // non-selected swatch when tapped, mirroring the visual from the
        // reference screenshot.
        val rainbow = remember {
            Brush.sweepGradient(
                listOf(
                    Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFFEE6C),
                    Color(0xFF6BE585), Color(0xFF4A90E2), Color(0xFFB55CFF),
                    Color(0xFFFF5F6D)
                )
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rainbow)
                .clickable {
                    val idx = CustomAccentSwatches.indexOf(selected)
                    val next = CustomAccentSwatches[(idx + 1).mod(CustomAccentSwatches.size)]
                    onSelect(next)
                }
        )
    }
}

// Pre-computed static calendar grid — defined at file scope so it isn't
// re-allocated on every recomposition. Each entry is (date, isAprilMonth).
private val PreviewWeeks: List<List<Pair<Int, Boolean>>> = listOf(
    listOf(29 to false, 30 to false, 31 to false, 1 to true, 2 to true, 3 to true, 4 to true),
    listOf(5 to true, 6 to true, 7 to true, 8 to true, 9 to true, 10 to true, 11 to true),
    listOf(12 to true, 13 to true, 14 to true, 15 to true, 16 to true, 17 to true, 18 to true),
    listOf(19 to true, 20 to true, 21 to true, 22 to true, 23 to true, 24 to true, 25 to true),
    listOf(26 to true, 27 to true, 28 to true, 29 to true, 30 to true, 1 to false, 2 to false),
    listOf(3 to false, 4 to false, 5 to false, 6 to false, 7 to false, 8 to false, 9 to false)
)
private val DayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * One swipeable preview page. Default themes get a solid dark card; scene
 * themes (isPremium + backgroundColors) get a full-bleed vertical gradient
 * with a Canvas-drawn landmark silhouette at the bottom.
 *
 * [accentOverride] lets the caller swap in a user-picked accent (e.g. the
 * 어둠 custom-color palette) without mutating the underlying [theme].
 */
@Composable
private fun ThemePreviewCard(theme: ThemeColor, accentOverride: Color = theme.accentColor) {
    val isScene = theme.backgroundColors != null
    val backgroundBrush = remember(theme) {
        theme.backgroundColors?.let { Brush.verticalGradient(it) }
    }
    val cardColor = theme.cardColor ?: DarkSurface
    val textColor = theme.previewTextColor ?: Color.White
    val secondaryTextColor = if (isScene) textColor.copy(alpha = 0.6f) else TextSecondary
    val tertiaryTextColor = if (isScene) textColor.copy(alpha = 0.4f) else TextTertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush)
                else Modifier.background(DarkCard)
            )
            .padding(14.dp)
    ) {
        CalendarPreviewMock(
            accent = accentOverride,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            tertiaryTextColor = tertiaryTextColor,
            cardColor = cardColor,
            cardTextColor = if (isScene) Color(0xFF1A1A1A) else Color.White,
            cityLandmark = theme.cityLandmark,
            landmarkColor = accentOverride
        )
    }
}

/** Static mini-calendar mockup using [accent] as the selected-day color. */
@Composable
private fun CalendarPreviewMock(
    accent: Color,
    textColor: Color,
    secondaryTextColor: Color,
    tertiaryTextColor: Color,
    cardColor: Color,
    cardTextColor: Color,
    cityLandmark: CityLandmark?,
    landmarkColor: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // City landmark silhouette fills the bottom third (scene themes)
        if (cityLandmark != null) {
            CityLandmarkIllustration(
                landmark = cityLandmark,
                color = landmarkColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.40f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4월",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(12.dp))
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))

            // Day-of-week
            Row(modifier = Modifier.fillMaxWidth()) {
                DayLabels.forEach { d ->
                    Text(
                        text = d,
                        fontSize = 11.sp,
                        color = tertiaryTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            PreviewWeeks.forEachIndexed { weekIdx, week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    week.forEachIndexed { dayIdx, (date, isApril) ->
                        val isSelected = weekIdx == 2 && dayIdx == 3 // 15
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.toString(),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = date.toString(),
                                    color = if (isApril) textColor else tertiaryTextColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Today section card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "오늘",
                        fontSize = 11.sp,
                        color = cardTextColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    MockTaskRow(text = "오늘의 요약을 작성하십시오.", textColor = cardTextColor)
                    Spacer(Modifier.height(6.dp))
                    MockTaskRow(text = "컴퓨터 데스크탑 정리하기", textColor = cardTextColor)
                    Spacer(Modifier.height(6.dp))
                    MockTaskRow(text = "꽃에 물을 주는 것을 잊지 마세요.", textColor = cardTextColor)
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom row mock — FAB on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MockTaskRow(text: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
