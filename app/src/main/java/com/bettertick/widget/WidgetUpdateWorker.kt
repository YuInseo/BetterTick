package com.bettertick.widget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bettertick.widget.calendar.ReminderWidget
import com.bettertick.widget.calendar.WeeklyCalendarWidget
import com.bettertick.widget.focus.FocusDistributionWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // App closed: pull edits made elsewhere (other device / web) into
            // the local cache first, so the repaint shows them.
            if (WidgetServiceLocator.isInitialized) {
                runCatching { WidgetServiceLocator.taskRepository.refreshFromServer() }
                runCatching {
                    WidgetServiceLocator.publishTasks(WidgetServiceLocator.taskRepository.observeAllTasks().first())
                }
            }
            ReminderWidget().updateAll(context)
            WeeklyCalendarWidget().updateAll(context)
            FocusDistributionWidget().updateAll(context)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "bettertick_widget_update"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES // WorkManager minimum
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
