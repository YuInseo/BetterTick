package com.bettertick.widget.calendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Routes WeeklyCalendarWidget clicks (week navigation) to Glance state writes.
 * Mirrors the architecture of [WidgetActionDispatcher] for the same reason —
 * Glance 1.1's broadcast pipeline mishandles repeated PendingIntents, so an
 * invisible Activity is the reliable choice.
 *
 * URI schema: bettertick://weeklycalendar/anchor/<ISO_DATE>
 */
class WeeklyCalendarActionDispatcher : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        Log.i(TAG, "dispatch uri=$uri")
        if (uri == null || uri.host != "weeklycalendar") return
        val segments = uri.pathSegments
        if (segments.firstOrNull() != "anchor") return
        val date = segments.getOrNull(1)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return

        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val ids = GlanceAppWidgetManager(appContext)
                    .getGlanceIds(WeeklyCalendarWidget::class.java)
                val widget = WeeklyCalendarWidget()
                for (id in ids) {
                    updateAppWidgetState(appContext, id) { prefs ->
                        prefs[WeeklyCalendarWidget.ANCHOR_DATE_KEY] = date.toString()
                    }
                    widget.update(appContext, id)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "state write failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "WeeklyCalendarDispatch"
    }
}
