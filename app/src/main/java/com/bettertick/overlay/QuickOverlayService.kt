package com.bettertick.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bettertick.data.model.Task
import com.bettertick.data.repository.TaskRepository
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.DarkSurface
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class QuickOverlayService : Service() {

    @Inject lateinit var taskRepository: TaskRepository

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        lifecycleOwner.start()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showOverlay()
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }

        val view = ComposeView(this).also { overlayView = it }
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        ViewCompat.setImportantForAccessibility(view, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)

        view.setContent {
            BetterTickTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var taskText by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                // Full-screen dimmed background; tap outside bottom sheet → dismiss
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .pointerInput(Unit) { detectTapGestures { dismiss() } }
                        .imePadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            // consume touches so they don't reach the dim layer
                            .pointerInput(Unit) { detectTapGestures { } },
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurface,
                        shadowElevation = 12.dp
                    ) {
                        Column {
                            // Tab row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                            ) {
                                listOf("할일", "일기").forEachIndexed { index, label ->
                                    val isSelected = selectedTab == index
                                    Column(
                                        modifier = Modifier
                                            .pointerInput(index) {
                                                detectTapGestures { selectedTab = index }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color(0xFF8A8A8E)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent,
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                            // Input area
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 12.dp, bottom = 12.dp)
                            ) {
                                BasicTextField(
                                    value = taskText,
                                    onValueChange = { taskText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp, max = 180.dp)
                                        .focusRequester(focusRequester),
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        lineHeight = 26.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { inner ->
                                        if (taskText.isEmpty()) {
                                            Text(
                                                if (selectedTab == 0) "무엇을 할 예정인가요?"
                                                else "오늘 하루는 어땠나요?",
                                                style = TextStyle(fontSize = 16.sp, color = Color(0xFF6B6B6B))
                                            )
                                        }
                                        inner()
                                    }
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { dismiss() }) {
                                        Text("취소", color = Color(0xFF8A8A8E))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (taskText.isNotBlank()) {
                                                saveAndDismiss(taskText, selectedTab)
                                            } else {
                                                dismiss()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("저장")
                                    }
                                }
                            }
                        }
                    }
                }

                SideEffect { focusRequester.requestFocus() }
            }
        }

        windowManager.addView(view, params)
    }

    private fun saveAndDismiss(text: String, tab: Int) {
        scope.launch {
            if (tab == 0) {
                taskRepository.addTask(
                    Task(
                        title = text.trim(),
                        dueDate = Timestamp(Date()),
                        sortOrder = System.currentTimeMillis()
                    )
                )
            }
            // tab == 1 (diary): would need DiaryRepository — handled via QuickMemoActivity for now
        }
        dismiss()
    }

    private fun dismiss() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        lifecycleOwner.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Foreground service boilerplate (required on Android 8+) ---

    private fun startForegroundCompat() {
        val channelId = "bettertick_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "빠른 추가 오버레이", NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("BetterTick 빠른 추가")
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("BetterTick 빠른 추가")
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(9902, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(9902, notification)
        }
    }
}
