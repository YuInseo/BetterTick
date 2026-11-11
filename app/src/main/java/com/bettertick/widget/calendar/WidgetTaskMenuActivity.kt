package com.bettertick.widget.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.MainActivity
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskStatusAction
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.widget.WidgetServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Widget row tap → small sheet with 완료 / 취소됨 / 내일로 미루기 / 앱에서 열기.
 * Widgets can't detect a long press, so the row tap stands in for the app's
 * checkbox long-press menu (the checkbox itself still just completes).
 *
 * URI: bettertick://widget/taskmenu/<taskId>?d=<ISO date shown in the widget>
 */
class WidgetTaskMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        val taskId = uri?.pathSegments?.getOrNull(1)
        val day = uri?.getQueryParameter("d")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val task = taskId?.let { id ->
            if (WidgetServiceLocator.isInitialized) WidgetServiceLocator.tasksCache.value.firstOrNull { it.id == id } else null
        }
        if (task == null) {
            openApp()
            return
        }
        setContent {
            BetterTickTheme {
                StatusSheet(
                    task = task,
                    onAction = { action ->
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            runCatching { WidgetServiceLocator.taskRepository.applyStatus(task, action, day) }
                        }
                        finish()
                    },
                    onOpenApp = { openApp() },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        fun intent(context: Context, taskId: String, day: LocalDate): Intent =
            Intent(context, WidgetTaskMenuActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                data = Uri.parse("bettertick://widget/taskmenu/${Uri.encode(taskId)}?d=$day")
            }
    }
}

@Composable
private fun StatusSheet(
    task: Task,
    onAction: (TaskStatusAction) -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = noRipple, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCard)
                // Swallow taps on the card so they don't dismiss.
                .clickable(interactionSource = noRipple, indication = null) {}
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = task.title,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            TaskStatusAction.choicesFor(task).forEach { action ->
                SheetRow(action.label, MaterialTheme.colorScheme.primary.takeIf { action == TaskStatusAction.COMPLETE }) {
                    onAction(action)
                }
            }
            SheetRow("앱에서 열기", null, onOpenApp)
        }
    }
}

@Composable
private fun SheetRow(label: String, color: Color?, onClick: () -> Unit) {
    Text(
        text = label,
        color = color ?: TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}
