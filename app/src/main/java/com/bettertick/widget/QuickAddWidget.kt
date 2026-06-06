package com.bettertick.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.bettertick.QuickAddActivity
import com.bettertick.QuickMemoActivity
import com.bettertick.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class QuickAddWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    companion object {
        fun update(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quickadd)

            val dateLabel = LocalDate.now().format(
                DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
            )
            views.setTextViewText(R.id.widget_quickadd_date, dateLabel)

            val taskIntent = PendingIntent.getActivity(
                context, widgetId * 10,
                Intent(context, QuickAddActivity::class.java).apply {
                    action = "com.bettertick.QUICK_ADD"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val memoIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 1,
                Intent(context, QuickMemoActivity::class.java).apply {
                    action = "com.bettertick.QUICK_MEMO"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.btn_add_task, taskIntent)
            views.setOnClickPendingIntent(R.id.btn_add_memo, memoIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
