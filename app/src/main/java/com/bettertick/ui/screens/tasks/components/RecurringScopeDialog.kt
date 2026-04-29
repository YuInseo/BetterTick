package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary

/** The scope the user selected for a recurring-task operation. */
enum class RecurringScope { ThisOccurrence, AllIncomplete }

private val ScopeAccent = Color(0xFF4A90E2)

/**
 * "반복 할일 편집/삭제" confirmation — matches the reference app's two-option
 * layout (지금 반복 / 모든 미완료 주기) with a 취소 at the bottom-right. No
 * confirm button: picking a radio option fires [onChoice] immediately so
 * the drag or delete can proceed.
 */
@Composable
fun RecurringScopeDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onChoice: (RecurringScope) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))

            ScopeRow("지금 반복") { onChoice(RecurringScope.ThisOccurrence) }
            Spacer(Modifier.height(4.dp))
            ScopeRow("모든 미완료 주기") { onChoice(RecurringScope.AllIncomplete) }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScopeAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ScopeRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, TextSecondary, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}
