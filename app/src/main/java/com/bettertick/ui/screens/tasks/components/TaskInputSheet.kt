package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bettertick.ui.components.MarkdownInlineTransformation
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate

@Composable
fun TaskInputSheet(
    onAddTask: (String, LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate = LocalDate.now()
) {
    var text by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val markdownTransformation = remember { MarkdownInlineTransformation() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("무엇을 하고 싶으신가요?", color = TextSecondary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = Orange
            ),
            visualTransformation = markdownTransformation,
            // Single-line title — Enter (both hardware and the 완료 IME key)
            // reliably submits. Without singleLine the Enter key inserts a
            // newline on some keyboards and the 완료 button silently becomes
            // a no-op.
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onAddTask(text.trim(), selectedDate)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("설명", color = TextTertiary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = Orange
            ),
            visualTransformation = markdownTransformation,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onAddTask(text.trim(), selectedDate)
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date chip — opens the full date picker. Label reads "오늘" for
            // today, otherwise "M/d" so the user can tell at a glance whether
            // the due date has been moved.
            val today = LocalDate.now()
            val chipLabel = when (selectedDate) {
                today -> "오늘"
                today.plusDays(1) -> "내일"
                else -> "${selectedDate.monthValue}/${selectedDate.dayOfMonth}"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Orange.copy(alpha = 0.15f))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(chipLabel, color = Orange, style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Flag, contentDescription = "Priority", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = "Tags", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.MoveToInbox, contentDescription = "Move", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Mic when the title is empty, big orange Send once there's
            // something to submit — same affordance you'd expect from a
            // chat composer.
            val canSubmit = text.isNotBlank()
            IconButton(
                onClick = {
                    if (canSubmit) onAddTask(text.trim(), selectedDate)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (canSubmit) Icons.AutoMirrored.Filled.Send else Icons.Outlined.Mic,
                    contentDescription = if (canSubmit) "추가" else "Voice",
                    tint = if (canSubmit) Orange else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        TaskDatePickerSheet(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDelete = {
                selectedDate = LocalDate.now()
                showDatePicker = false
            },
            onConfirm = { date, _, _, _, _ ->
                // Quick-add sheet captures only the date; time/duration and
                // repeat live in the full TaskDetailSheet flow so we don't
                // overload the quick-add path.
                selectedDate = date
                showDatePicker = false
            }
        )
    }
}

