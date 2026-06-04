package com.bettertick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.repository.DiaryRepository
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.DarkSurface
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class QuickMemoViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    fun saveMemo(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            diaryRepository.saveEntry(DiaryEntry(dateStr = today, content = content))
        }
    }
}

@AndroidEntryPoint
class QuickMemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterTickTheme {
                val vm: QuickMemoViewModel = hiltViewModel()
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
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = DarkSurface,
                            shadowElevation = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MemoInputSheet(
                                onSave = { content ->
                                    vm.saveMemo(content)
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

@Composable
private fun MemoInputSheet(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val dateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "오늘의 일기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
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
                .heightIn(min = 80.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 26.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (text.isNotBlank()) onSave(text) else onDismiss()
            }),
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
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF8A8A8E))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (text.isNotBlank()) onSave(text) else onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("저장")
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
