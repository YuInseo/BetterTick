package com.bettertick.widget.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.bettertick.ui.theme.BetterTickTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.widget.WidgetServiceLocator
import kotlinx.coroutines.launch

/**
 * Full-screen settings activity matching the reference widget settings UI.
 * Sections:
 *  - Theme (black / white)
 *  - Font size (small / normal / large)
 *  - Opacity slider (0..100)
 *  - Filter view scope (all / <list>)
 *  - Show completed tasks toggle
 *  - Detail view toggle
 * The ✓ in the top bar persists the changes and refreshes the widget; the ✕
 * discards and closes.
 */
class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterTickTheme {
                val ctx = this
                val scope = rememberCoroutineScope()
                var settings by remember { mutableStateOf(WidgetServiceLocator.reminderSettings(ctx)) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .systemBarsPadding()
                ) {
                    TopBar(
                        onClose = { finish() },
                        onConfirm = {
                            WidgetServiceLocator.saveReminderSettings(ctx, settings)
                            // Settings live in SharedPreferences, but Glance
                            // re-renders only when its own state mutates.
                            // Bump a monotonic version in each widget's
                            // Glance state so provideGlance re-reads the
                            // fresh prefs. Use a DETACHED scope + app
                            // context — `rememberCoroutineScope()` is tied
                            // to the composition and dies on finish(),
                            // which stranded the second update mid-flight.
                            val appContext = ctx.applicationContext
                            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                                val mgr = GlanceAppWidgetManager(appContext)
                                val ids = mgr.getGlanceIds(ReminderWidget::class.java)
                                val widget = ReminderWidget()
                                for (id in ids) {
                                    updateAppWidgetState(appContext, id) { prefs ->
                                        prefs[SETTINGS_VERSION_KEY] = System.currentTimeMillis()
                                    }
                                    widget.update(appContext, id)
                                }
                            }
                            finish()
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))

                        // Appearance group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                        ) {
                            OptionRow(
                                title = "테마",
                                value = labelForTheme(settings.theme),
                                onClick = {
                                    settings = settings.copy(
                                        theme = if (settings.theme == "black") "white" else "black"
                                    )
                                }
                            )
                            OptionRow(
                                title = "글자 크기",
                                value = labelForFontSize(settings.fontSize),
                                onClick = {
                                    val next = when (settings.fontSize) {
                                        "small" -> "normal"
                                        "normal" -> "large"
                                        else -> "small"
                                    }
                                    settings = settings.copy(fontSize = next)
                                }
                            )
                            OpacityRow(
                                value = settings.opacity,
                                onChange = { settings = settings.copy(opacity = it) }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Data-filter group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                        ) {
                            OptionRow(
                                title = "필터 보기 범위",
                                value = if (settings.filter == "all") "전체" else settings.filter,
                                onClick = { /* list picker — out of scope for now */ }
                            )
                            ToggleRow(
                                title = "완료된 할일 보기",
                                checked = settings.showCompleted,
                                onChange = { settings = settings.copy(showCompleted = it) }
                            )
                            ToggleRow(
                                title = "자세히 보기",
                                checked = settings.showDetail,
                                onChange = { settings = settings.copy(showDetail = it) }
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
        }
        Text(
            text = "위젯 설정",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onConfirm() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "저장", tint = Color.White)
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(title, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun OpacityRow(value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("투명도", fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4A90E2),
                activeTrackColor = Color(0xFF4A90E2),
                inactiveTrackColor = DarkSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text("${value}%", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4A90E2),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}

private fun labelForTheme(key: String) = when (key) {
    "white" -> "화이트"
    else -> "블랙"
}

private fun labelForFontSize(key: String) = when (key) {
    "small" -> "작게"
    "large" -> "크게"
    else -> "보통"
}

// Monotonic key inside Glance's per-widget state store. Settings live in
// SharedPreferences (shared across widgets), so changing them doesn't touch
// Glance state on its own — bumping this forces provideGlance to re-run.
private val SETTINGS_VERSION_KEY = longPreferencesKey("settings_version")
