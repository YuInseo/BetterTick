package com.bettertick.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentEntry by viewModel.currentEntry.collectAsState()
    val entriesMap by viewModel.entriesMap.collectAsState()
    val draftContent by viewModel.draftContent.collectAsState()

    var displayMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var textContent by remember { mutableStateOf("") }
    var selectedMood by remember { mutableIntStateOf(0) }
    var hasUnsaved by remember { mutableStateOf(false) }

    LaunchedEffect(currentEntry) {
        textContent = currentEntry?.content ?: ""
        selectedMood = currentEntry?.mood ?: 0
        hasUnsaved = false
    }

    // Auto-save draft after 2s of inactivity
    LaunchedEffect(textContent) {
        if (textContent.isNotBlank() && hasUnsaved) {
            delay(2000)
            viewModel.saveDraft(textContent)
        }
    }

    val showDraftBanner = draftContent != null && draftContent != textContent

    val listState = rememberLazyListState()
    LaunchedEffect(selectedDate, displayMonth) {
        if (selectedDate.year == displayMonth.year && selectedDate.month == displayMonth.month) {
            listState.animateScrollToItem((selectedDate.dayOfMonth - 1).coerceAtLeast(0))
        }
    }

    fun saveIfNeeded() {
        if (hasUnsaved && textContent.isNotBlank()) {
            viewModel.saveEntry(textContent, selectedMood)
            hasUnsaved = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "일기",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (currentEntry != null) {
                IconButton(onClick = { viewModel.deleteCurrentEntry() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "삭제", tint = TextSecondary)
                }
            }
            if (hasUnsaved) {
                TextButton(onClick = {
                    viewModel.saveEntry(textContent, selectedMood)
                    hasUnsaved = false
                }) {
                    Text("저장", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Month navigation
        val isNotToday = selectedDate != LocalDate.now()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { displayMonth = displayMonth.minusMonths(1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Outlined.ChevronLeft, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = isNotToday) {
                        saveIfNeeded()
                        viewModel.selectDate(LocalDate.now())
                        displayMonth = LocalDate.now().withDayOfMonth(1)
                    }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = displayMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                if (isNotToday) {
                    Text(
                        text = "오늘로 돌아가기",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = { displayMonth = displayMonth.plusMonths(1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        // Day strip
        val days = remember(displayMonth) {
            (1..displayMonth.lengthOfMonth()).map { displayMonth.withDayOfMonth(it) }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(days) { day ->
                val isSelected = day == selectedDate
                val hasEntry = entriesMap.containsKey(day.toString())
                val isToday = day == LocalDate.now()
                val dow = when (day.dayOfWeek.value) {
                    1 -> "월"; 2 -> "화"; 3 -> "수"; 4 -> "목"
                    5 -> "금"; 6 -> "토"; else -> "일"
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable {
                            saveIfNeeded()
                            viewModel.selectDate(day)
                            displayMonth = day.withDayOfMonth(1)
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .width(32.dp)
                ) {
                    Text(
                        text = dow,
                        fontSize = 10.sp,
                        color = when {
                            isSelected -> Color.White.copy(alpha = 0.75f)
                            day.dayOfWeek.value == 7 -> Color(0xFFE57373)
                            day.dayOfWeek.value == 6 -> Color(0xFF64B5F6)
                            else -> TextSecondary
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = day.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> Color.White
                            isToday -> MaterialTheme.colorScheme.primary
                            day.dayOfWeek.value == 7 -> Color(0xFFE57373)
                            day.dayOfWeek.value == 6 -> Color(0xFF64B5F6)
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected && hasEntry -> Color.White
                                    hasEntry -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C2E))

        // Date label
        val fmt = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)
        Text(
            text = selectedDate.format(fmt),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)
        )

        // Mood selector
        val moods = listOf("😶" to 0, "😔" to 1, "😐" to 2, "🙂" to 3, "😄" to 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moods.forEach { (emoji, value) ->
                val isSel = selectedMood == value
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSel) 1.5.dp else 0.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            selectedMood = value
                            hasUnsaved = true
                        }
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Draft restore banner
        if (showDraftBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "임시저장된 내용이 있습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        textContent = draftContent!!
                        viewModel.deleteDraft()
                        hasUnsaved = true
                    }
                ) { Text("복구", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = { viewModel.deleteDraft() }) {
                    Text("삭제", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8A8E))
                }
            }
        }

        // Writing area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = textContent,
                onValueChange = {
                    textContent = it
                    hasUnsaved = true
                },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 26.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (textContent.isEmpty()) {
                        Text(
                            text = "오늘은 어떤 하루였나요?\n생각이나 느낌을 자유롭게 적어보세요...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF555558),
                                lineHeight = 26.sp
                            )
                        )
                    }
                    inner()
                }
            )
        }
    }
}
