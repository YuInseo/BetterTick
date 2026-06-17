package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Habit
import com.bettertick.ui.components.AppActionButton
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary

private val HabitBlue = Color(0xFF4A90E2)

@Composable
fun HabitsListScreen(
    activeHabits: List<Habit>,
    archivedHabits: List<Habit>,
    weekLogs: Map<String, Set<String>>,
    onBack: () -> Unit,
    onHabitClick: (Habit) -> Unit,
    onArchive: (Habit) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("진행 중", "보관됨")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "뒤로",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            tabs.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground
                    else TextSecondary
                )
            }
        }

        // Tab underline
        Row(modifier = Modifier.fillMaxWidth().padding(start = 56.dp)) {
            tabs.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .background(if (selectedTab == index) HabitBlue else Color.Transparent)
                )
                Spacer(Modifier.width(24.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        val displayHabits = if (selectedTab == 0) activeHabits else archivedHabits

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(displayHabits, key = { it.id }) { habit ->
                    val count = weekLogs[habit.id]?.size ?: 0
                    HabitListCard(
                        habit = habit,
                        weekCount = count,
                        isArchived = selectedTab == 1,
                        onClick = { onHabitClick(habit) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (selectedTab == 0) {
                AppActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = "습관 추가",
                    onClick = onBack, // go back to main screen to add
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HabitListCard(
    habit: Habit,
    weekCount: Int,
    isArchived: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitIconView(
            iconKey = habit.icon,
            colorHex = habit.color,
            circleSize = 44.dp,
            iconSize = 24.dp,
            fallbackText = habit.name
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = habit.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = weekCount.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "총 일수",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
