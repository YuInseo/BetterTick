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
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
class FloatingOverlayService : Service() {

    @Inject lateinit var taskRepository: TaskRepository

    private lateinit var windowManager: WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var buttonView: ComposeView? = null
    private var sheetView: ComposeView? = null

    // Button window params — draggable
    private val buttonParams = WindowManager.LayoutParams(
        BUTTON_SIZE_PX, BUTTON_SIZE_PX,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.END
        x = EDGE_MARGIN_PX
        y = EDGE_MARGIN_PX * 3
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        runCatching {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            lifecycleOwner.start()
            if (Settings.canDrawOverlays(this)) showFloatingButton()
        }.onFailure { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    // ── Floating "+" button ────────────────────────────────────────────────

    private fun showFloatingButton() {
        val view = makeComposeView().also { buttonView = it }
        var lastX = 0f; var lastY = 0f
        var isDragging = false

        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.rawX; lastY = ev.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - lastX; val dy = ev.rawY - lastY
                    if (!isDragging && (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        buttonParams.x -= dx.toInt()
                        buttonParams.y -= dy.toInt()
                        lastX = ev.rawX; lastY = ev.rawY
                        runCatching { windowManager.updateViewLayout(view, buttonParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        openSheet()
                    }
                    false
                }
                else -> false
            }
        }

        view.setContent {
            BetterTickTheme {
                Box(
                    modifier = Modifier
                        .size(BUTTON_SIZE_DP.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "빠른 추가",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        runCatching { windowManager.addView(view, buttonParams) }
    }

    // ── Quick-add bottom sheet ─────────────────────────────────────────────

    private fun openSheet() {
        if (sheetView != null) return
        buttonView?.let { runCatching { windowManager.removeView(it) }; buttonView = null }

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

        val view = makeComposeView().also { sheetView = it }
        view.setContent {
            BetterTickTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var inputText by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .pointerInput(Unit) { detectTapGestures { closeSheet() } }
                        .imePadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp)
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
                                    .padding(start = 8.dp, end = 8.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    listOf("할일", "일기").forEachIndexed { i, label ->
                                        val sel = selectedTab == i
                                        Column(
                                            modifier = Modifier
                                                .pointerInput(i) {
                                                    detectTapGestures { selectedTab = i }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sel) MaterialTheme.colorScheme.primary
                                                        else Color(0xFF8A8A8E)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                Modifier
                                                    .width(28.dp)
                                                    .height(2.dp)
                                                    .background(
                                                        if (sel) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                        RoundedCornerShape(1.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .pointerInput(Unit) { detectTapGestures { closeSheet() } }
                                ) {
                                    Icon(
                                        Icons.Default.Close, null,
                                        tint = Color(0xFF8A8A8E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 12.dp, bottom = 12.dp)
                            ) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
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
                                        if (inputText.isEmpty()) {
                                            Text(
                                                if (selectedTab == 0) "무엇을 할 예정인가요?"
                                                else "오늘 하루는 어땠나요?",
                                                style = TextStyle(
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF6B6B6B)
                                                )
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
                                    TextButton(onClick = { closeSheet() }) {
                                        Text("취소", color = Color(0xFF8A8A8E))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val text = inputText.trim()
                                            if (text.isNotBlank() && selectedTab == 0) {
                                                scope.launch {
                                                    taskRepository.addTask(
                                                        Task(
                                                            title = text,
                                                            dueDate = Timestamp(Date()),
                                                            sortOrder = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                            }
                                            closeSheet()
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

                SideEffect { runCatching { focusRequester.requestFocus() } }
            }
        }

        runCatching { windowManager.addView(view, params) }
    }

    private fun closeSheet() {
        sheetView?.let { runCatching { windowManager.removeView(it) }; sheetView = null }
        showFloatingButton()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun makeComposeView(): ComposeView {
        val ctx = android.view.ContextThemeWrapper(this, com.bettertick.R.style.Theme_BetterTick)
        val view = ComposeView(ctx)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        ViewCompat.setImportantForAccessibility(view, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        return view
    }

    private fun startForegroundCompat() {
        val channelId = "bettertick_floating"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "잠금화면 빠른 추가", NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("BetterTick")
                .setContentText("잠금화면 빠른 추가 활성화됨")
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("BetterTick")
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(9903, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(9903, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        buttonView?.let { runCatching { windowManager.removeView(it) } }
        sheetView?.let { runCatching { windowManager.removeView(it) } }
        lifecycleOwner.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val BUTTON_SIZE_DP = 54
        private const val BUTTON_SIZE_PX = (BUTTON_SIZE_DP * 2.75).toInt() // rough dp→px at ~2.75 density
        private const val EDGE_MARGIN_PX = 40

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
