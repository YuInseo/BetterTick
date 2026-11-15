package com.bettertick.widget.calendar

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Widget date / today / week-arrow taps. Runs inside Glance's own action
 * receiver for exactly the widget that was tapped (its [GlanceId]), so there is
 * no activity relay to launch and no widget id to match — taps went missing
 * through the [WidgetActionDispatcher] route.
 */
class SelectWidgetDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val date = parameters[DateKey] ?: return
        Log.i("SelectWidgetDateAction", "select $date for $glanceId")
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ReminderWidget.SELECTED_DATE_KEY] = date
            prefs[ReminderWidget.MENU_OPEN_KEY] = false
        }
        ReminderWidget().update(context, glanceId)
    }

    companion object {
        val DateKey = ActionParameters.Key<String>("date")
    }
}
