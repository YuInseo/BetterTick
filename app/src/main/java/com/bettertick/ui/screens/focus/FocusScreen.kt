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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.ui.screens.focus.components.ActivityCategoryCard
import com.bettertick.ui.screens.focus.components.RunningTimerBar
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onOpenStats: () -> Unit = {},
    viewModel: FocusViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    // 카테고리 클릭 → 풀스크린 타이머 노출. 사용자가 down chevron으로 축소하면
    // false로 떨어뜨려 categories + RunningTimerBar로 복귀.
    var timerExpanded by remember { mutableStateOf(false) }

    if (timerState.isRunning && timerExpanded) {
        FocusTimerScreen(
            timerState = timerState,
            timeText = viewModel.formatTime(timerState.elapsedSeconds),
            onCollapse = { timerExpanded = false },
            onPause = { viewModel.pauseSession() },
            onResume = { viewModel.resumeSession() },
            onStop = {
                viewModel.stopSession()
                timerExpanded = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "포커스",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                IconButton(onClick = onOpenStats) {
                    Icon(Icons.Outlined.PieChart, contentDescription = "포커스 통계", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add category", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        // Category list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                val todayMinutes = viewModel.getTodayTotalSeconds(category.name) / 60
                ActivityCategoryCard(
                    category = category,
                    todayMinutes = todayMinutes,
                    isTimerRunning = timerState.isRunning,
                    onStartClick = {
                        viewModel.startSession(category)
                        timerExpanded = true
                    }
                )
            }

            if (categories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "집중할 활동을 추가하세요",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "우측 상단의 + 버튼을 눌러 시작하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Running timer bar — 축소 상태에서만 노출. 클릭하면 풀스크린 복귀.
        if (timerState.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { timerExpanded = true }
            ) {
                RunningTimerBar(
                    timerState = timerState,
                    timeText = viewModel.formatTime(timerState.elapsedSeconds),
                    onPause = { viewModel.pauseSession() },
                    onResume = { viewModel.resumeSession() },
                    onStop = { viewModel.stopSession() }
                )
            }
        }
    }

    // Add category dialog
    if (showAddDialog) {
        var categoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("활동 추가", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    placeholder = { Text("활동 이름") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            viewModel.addCategory(categoryName.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("추가", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}
