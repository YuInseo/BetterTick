package com.bettertick.ui.screens.location

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.LocationRecord
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LocationHistoryScreen(
    viewModel: LocationHistoryViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val records by viewModel.getRecordsForDate(selectedDate.toString()).collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        DateNavigationBar(
            date = selectedDate,
            onPrev = { selectedDate = selectedDate.minusDays(1) },
            onNext = { if (selectedDate < LocalDate.now()) selectedDate = selectedDate.plusDays(1) }
        )

        if (records.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(records, key = { it.id }) { record ->
                    LocationItem(record = record, isLast = record == records.last())
                }
            }
        }
    }
}

@Composable
private fun DateNavigationBar(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "이전", tint = TextSecondary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(onClick = onNext, enabled = date < today) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "다음",
                tint = if (date < today) TextSecondary else TextTertiary
            )
        }
    }
}

@Composable
private fun LocationItem(record: LocationRecord, isLast: Boolean) {
    val time = remember(record.timestamp) {
        val sdf = java.text.SimpleDateFormat("HH:mm", Locale.KOREAN)
        sdf.format(record.timestamp.toDate())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (!isLast) 0.dp else 16.dp)) {
            Text(time, color = TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = record.address.ifBlank { "%.4f, %.4f".format(record.latitude, record.longitude) },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("이 날의 위치 기록이 없어요", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("설정에서 위치 기록을 켜주세요", color = TextTertiary, fontSize = 12.sp)
        }
    }
}
