package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurfaceVariant

private val InfoAccent = Color(0xFF4A90E2)

/** "반복 유형에 대해" — help modal launched from the ? button next to the
 *  dropdown in [CustomRepeatDialog]. Three labeled paragraphs, single
 *  acknowledge button. */
@Composable
fun RepeatTypeInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "반복 유형에 대해",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            InfoParagraph(
                label = "만료일에 따라",
                body = ": 미리 설정한 규칙에 따라 정기적으로만 반복합니다."
            )
            Spacer(Modifier.height(12.dp))
            InfoParagraph(
                label = "완료일에 따라",
                body = ": 현재 주기의 작업이 완료된 후에만 다음 반복이 생성됩니다."
            )
            Spacer(Modifier.height(12.dp))
            InfoParagraph(
                label = "특정 날짜에 따라",
                body = ": 필요한 만큼 많은 날짜를 선택하여 작업을 수행합니다."
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "알겠습니다",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InfoAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoParagraph(label: String, body: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                append(label)
            }
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.75f))) {
                append(body)
            }
        },
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
}
