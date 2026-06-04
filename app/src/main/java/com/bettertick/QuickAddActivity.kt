package com.bettertick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.repository.DiaryRepository
import com.bettertick.ui.screens.tasks.QuickAddViewModel
import com.bettertick.ui.screens.tasks.components.TaskInputSheet
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.DarkSurface
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class QuickAddDiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    private val _todayEntry = MutableStateFlow<DiaryEntry?>(null)
    val todayEntry: StateFlow<DiaryEntry?> = _todayEntry.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            diaryRepository.observeEntryForDate(LocalDate.now().toString())
                .collect { entry ->
                    _todayEntry.value = entry
                    _isLoaded.value = true
                }
        }
    }

    fun save(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val existing = _todayEntry.value
            val entry = existing?.copy(content = content)
                ?: DiaryEntry(dateStr = LocalDate.now().toString(), content = content)
            diaryRepository.saveEntry(entry)
        }
    }
}

@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterTickTheme {
                val taskVm: QuickAddViewModel = hiltViewModel()
                val diaryVm: QuickAddDiaryViewModel = hiltViewModel()
                var selectedTab by remember { mutableStateOf(0) }

                Dialog(
                    onDismissRequest = { finish() },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurface,
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Column {
                                QuickAddTabRow(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                when (selectedTab) {
                                    0 -> TaskInputSheet(
                                        onAddTask = { title, date ->
                                            taskVm.addTask(title, date)
                                            finish()
                                        },
                                        onDismiss = { finish() }
                                    )
                                    else -> QuickDiarySheet(
                                        vm = diaryVm,
                                        onSave = { content ->
                                            diaryVm.save(content)
                                            finish()
                                        },
                                        onDismiss = { finish() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        listOf("할일", "일기").forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF8A8A8E)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun QuickDiarySheet(
    vm: QuickAddDiaryViewModel,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val todayEntry by vm.todayEntry.collectAsState()
    val isEditing = todayEntry != null

    var text by remember { mutableStateOf(todayEntry?.content ?: "") }
    val focusRequester = remember { FocusRequester() }
    val dateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    )

    // Pre-fill existing entry content when loaded (don't overwrite if user already typed)
    LaunchedEffect(todayEntry) {
        if (todayEntry != null && text.isBlank()) {
            text = todayEntry!!.content
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "오늘의 일기 · 수정 중" else "오늘의 일기",
                style = MaterialTheme.typography.bodySmall,
                color = if (isEditing) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else Color(0xFF8A8A8E)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A8A8E)
            )
        }

        Spacer(Modifier.height(14.dp))

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 220.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 26.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        "오늘 하루는 어땠나요?",
                        style = TextStyle(fontSize = 16.sp, color = Color(0xFF6B6B6B))
                    )
                }
                inner()
            }
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF8A8A8E))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (text.isNotBlank()) onSave(text) else onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEditing) "수정" else "저장")
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
