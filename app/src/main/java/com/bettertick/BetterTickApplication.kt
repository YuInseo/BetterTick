package com.bettertick

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.bettertick.data.repository.AuthRepository
import com.bettertick.data.repository.FocusRepository
import com.bettertick.data.repository.HabitRepository
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.update.AppUpdateWorker
import com.bettertick.widget.WidgetServiceLocator
import com.bettertick.widget.WidgetUpdateWorker
import com.bettertick.widget.calendar.ReminderWidget
import com.bettertick.widget.calendar.WeeklyCalendarWidget
import com.bettertick.widget.focus.FocusDistributionWidget
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class BetterTickApplication : Application() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var focusRepository: FocusRepository
    @Inject lateinit var listRepository: ListRepository
    @Inject lateinit var authRepository: AuthRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, "36af0c02f9ab81fb67df8c9bbeefc7f1")
        // Initialize widget service locator for AppWidget access
        WidgetServiceLocator.init(taskRepository, habitRepository, focusRepository, listRepository)
        // Schedule periodic widget updates
        WidgetUpdateWorker.enqueue(this)
        // Schedule background app update checks (every 6 hours) + immediate first check
        AppUpdateWorker.createNotificationChannels(this)
        AppUpdateWorker.enqueue(this)
        AppUpdateWorker.runImmediately(this)

        // Server-backed sync for tasks. The widget (and the rest of the app)
        // reads from ListenSource.CACHE, so nothing pulls from the server by
        // itself — without this listener, a fresh install / cleared cache
        // would never see today's tasks in the widget until the user opens
        // the app and triggers a write. This listener populates the local
        // Firestore cache so the cache-only listeners below emit real data.
        authRepository.observeAuthState()
            .flatMapLatest { user ->
                if (user == null) emptyFlow() else taskRepository.startBackgroundSync()
            }
            .launchIn(appScope)

        // Continuously mirror task/list data into the widget's hot cache so
        // `provideGlance` can read the latest snapshot synchronously — no
        // cold-listener race. Each emission also repaints the widget.
        //
        // Gate on auth: `tasksCollection()` throws when currentUser is null,
        // which would kill the flow forever on a cold start before sign-in.
        // flatMapLatest re-subscribes on every auth state change, so we get
        // tasks immediately for returning users and on sign-in for new ones.
        authRepository.observeAuthState()
            .flatMapLatest { user ->
                if (user == null) emptyFlow() else taskRepository.observeAllTasks()
            }
            .onEach { tasks ->
                WidgetServiceLocator.publishTasks(tasks)
                runCatching { ReminderWidget().updateAll(this@BetterTickApplication) }
                runCatching { WeeklyCalendarWidget().updateAll(this@BetterTickApplication) }
            }
            .launchIn(appScope)
        authRepository.observeAuthState()
            .flatMapLatest { user ->
                if (user == null) emptyFlow() else listRepository.observeLists()
            }
            .onEach { lists ->
                WidgetServiceLocator.publishLists(lists)
                // Repaint so the task row's list-name label updates when a
                // list is renamed. Tasks alone triggering updateAll left the
                // list-name stale until the next task mutation.
                runCatching { ReminderWidget().updateAll(this@BetterTickApplication) }
                // List color changes drive WeeklyCalendar's chip colors, so
                // it needs the same repaint.
                runCatching { WeeklyCalendarWidget().updateAll(this@BetterTickApplication) }
            }
            .launchIn(appScope)

        // Focus session stream → focus widget.
        authRepository.observeAuthState()
            .flatMapLatest { user ->
                if (user == null) emptyFlow() else focusRepository.observeThisWeekSessions()
            }
            .onEach { sessions ->
                WidgetServiceLocator.publishFocusWeek(sessions)
                runCatching { FocusDistributionWidget().updateAll(this@BetterTickApplication) }
            }
            .launchIn(appScope)
    }
}
