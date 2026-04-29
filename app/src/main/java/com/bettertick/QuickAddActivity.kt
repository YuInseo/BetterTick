package com.bettertick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.ui.screens.tasks.QuickAddViewModel
import com.bettertick.ui.screens.tasks.components.TaskInputSheet
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.DarkSurface
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lightweight translucent activity used by the home-screen widget's "+" button.
 * Renders nothing but the quick-add dialog — the launcher / whatever app was
 * visible stays behind the scrim, matching Samsung Reminder / Google Tasks
 * widget behavior. Dismissing (scrim tap, back, or submit) finishes the
 * activity so we don't leave an empty task on the back stack.
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterTickTheme {
                val vm: QuickAddViewModel = hiltViewModel()
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
                            TaskInputSheet(
                                onAddTask = { title, date ->
                                    vm.addTask(title, date)
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
