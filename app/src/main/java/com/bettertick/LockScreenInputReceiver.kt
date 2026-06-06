package com.bettertick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.bettertick.data.model.Task
import com.bettertick.data.repository.TaskRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class LockScreenInputReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val text = bundle.getCharSequence(KEY_TASK_TEXT)?.toString()?.trim()
        if (text.isNullOrBlank()) return

        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                taskRepository.addTask(
                    Task(
                        title = text,
                        dueDate = Timestamp(Date()),
                        sortOrder = System.currentTimeMillis()
                    )
                )
            }
            LockScreenBar.show(context)
            result.finish()
        }
    }

    companion object {
        const val KEY_TASK_TEXT = "task_text"
    }
}
