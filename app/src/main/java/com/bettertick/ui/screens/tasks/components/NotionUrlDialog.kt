package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextTertiary

/**
 * Centered dialog for attaching / editing the Notion URL on a task.
 * Tiny on purpose — one input + two footer buttons, matching the
 * proportions of [TaskDatePickerSheet] without the full picker chrome.
 */
private val DialogAccent = Color(0xFF4A90E2)

@Composable
fun NotionUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "노션 링크",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "https://www.notion.so/...",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialUrl.isNotBlank()) {
                    Text(
                        text = "삭제",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DialogAccent,
                        modifier = Modifier
                            .clickable {
                                text = ""
                                onConfirm("")
                            }
                            .padding(vertical = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DialogAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "확인",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DialogAccent,
                    modifier = Modifier
                        .clickable { onConfirm(text) }
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }
        }
    }
}
