package com.bettertick.ui.screens.tasks.components

import android.content.Context
import com.bettertick.data.model.Task
import com.bettertick.data.model.followingSchedule
import com.bettertick.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * 페이지 블록 (노션의 하위 페이지): each page block is a 하위 할일 of the task it
 * sits in, so it shows up under its parent everywhere and opens as its own page.
 * Reached through an entry point (looked up only when a page is used), so the
 * detail sheet still renders where Hilt isn't set up, e.g. UI tests.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SubPageEntryPoint {
    fun taskRepository(): TaskRepository
}

internal class SubPages(context: Context) {
    private val repo = EntryPointAccessors.fromApplication(context.applicationContext, SubPageEntryPoint::class.java)
        .taskRepository()

    /** Creates the child page under [parent]; returns it with its new id. */
    suspend fun create(parent: Task, title: String = ""): Task {
        val child = Task(
            title = title,
            parentId = parent.id,
            listId = parent.listId,
            sortOrder = System.currentTimeMillis()
        ).followingSchedule(parent, fresh = true)
        return child.copy(id = repo.addTask(child))
    }

    /** The page as it is now (null when it was deleted). */
    suspend fun load(id: String): Task? = repo.observeAllTasks().first().firstOrNull { it.id == id }

    suspend fun update(task: Task) = repo.updateTask(task)

    suspend fun toggle(task: Task) = repo.toggleComplete(task.id, !task.isCompleted)
}
