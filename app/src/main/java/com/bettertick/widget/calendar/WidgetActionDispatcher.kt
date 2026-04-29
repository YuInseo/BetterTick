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
 * Invisible dispatcher that routes widget clicks to Glance state mutations.
 * Uses an Activity rather than a BroadcastReceiver because Glance 1.1's
 * `actionSendBroadcast` wraps PendingIntents with FLAG_ONE_SHOT, breaking
 * repeated clicks.
 *
 * Action + payload live in the intent's data URI path — extras proved
 * unreliable through Glance's PendingIntent pipeline. URI schemas:
 *   bettertick://widget/selectdate/<ISO_DATE>
 *   bettertick://widget/action/<ACTION_NAME>
 *
 * All state lives in Glance's per-widget Preferences datastore (via
 * [updateAppWidgetState]). Glance watches these and triggers re-composition
 * automatically — that's the whole reason we moved off SharedPreferences.
 */
class WidgetActionDispatcher : ComponentActivity() {
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
        if (uri == null) return
        if (uri.host != "widget") return

        val segments = uri.pathSegments
        val kind = segments.firstOrNull() ?: return
        val arg = segments.getOrNull(1)

        // Side-effects that aren't Glance state (launch another activity) run
        // synchronously here so they fire even if the coroutine below races
        // with Activity.finish().
        if (kind == "action" && arg == ACTION_OPEN_SETTINGS) {
            Log.i(TAG, "OPEN_SETTINGS")
            startActivity(
                Intent(this, WidgetSettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val ids = GlanceAppWidgetManager(appContext)
                    .getGlanceIds(ReminderWidget::class.java)
                Log.i(TAG, "writing state for ${ids.size} widget(s)")
                val widget = ReminderWidget()
                for (id in ids) {
                    updateAppWidgetState(appContext, id) { prefs ->
                        when (kind) {
                            "selectdate" -> {
                                val date = arg?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                }
                                if (date != null) {
                                    Log.i(TAG, "SELECT_DATE $date")
                                    prefs[ReminderWidget.SELECTED_DATE_KEY] = date.toString()
                                    prefs[ReminderWidget.MENU_OPEN_KEY] = false
                                }
                            }
                            "action" -> when (arg) {
                                ACTION_TOGGLE_MENU -> {
                                    val now = prefs[ReminderWidget.MENU_OPEN_KEY] ?: false
                                    Log.i(TAG, "TOGGLE_MENU $now → ${!now}")
                                    prefs[ReminderWidget.MENU_OPEN_KEY] = !now
                                }
                                ACTION_CLOSE_MENU,
                                ACTION_REFRESH,
                                ACTION_OPEN_SETTINGS -> {
                                    Log.i(TAG, "close menu ($arg)")
                                    prefs[ReminderWidget.MENU_OPEN_KEY] = false
                                }
                            }
                        }
                    }
                    // Explicit update — updateAppWidgetState alone does NOT
                    // trigger provideGlance re-run on Glance 1.1.1; verified
                    // empirically: state writes succeed but the composition
                    // never re-runs. Forcing update() here makes the widget
                    // actually re-render with the new state.
                    Log.i(TAG, "→ forcing update() for $id")
                    widget.update(appContext, id)
                    Log.i(TAG, "← update() returned for $id")
                }
                Log.i(TAG, "state writes complete")
            } catch (t: Throwable) {
                Log.e(TAG, "state write failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "WidgetActionDispatcher"

        const val ACTION_TOGGLE_MENU = "toggle_menu"
        const val ACTION_CLOSE_MENU = "close_menu"
        const val ACTION_REFRESH = "refresh"
        const val ACTION_OPEN_SETTINGS = "open_settings"
    }
}
