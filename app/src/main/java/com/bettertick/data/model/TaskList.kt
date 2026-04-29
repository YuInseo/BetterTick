package com.bettertick.data.model

import com.google.firebase.Timestamp

data class TaskList(
    val id: String = "",
    val name: String = "",
    val color: String = "#FF8C00",
    val icon: String = "folder",
    val isDefault: Boolean = false,
    val sortOrder: Long = 0,
    val createdAt: Timestamp = Timestamp.now(),
    // Controls how TasksScreen renders this list — "list" (default), "kanban",
    // or "timetable". Stored as a string so Firestore serialization stays
    // forward-compatible with any future view modes.
    val viewType: String = "list",
    // Pinned lists render as compact tiles at the top of the drawer, above
    // every other section.
    val isPinned: Boolean = false,
    // Kanban view stores its user-defined column names here. Tasks reference
    // a column via [Task.kanbanColumn]; anything unset renders under the
    // synthetic "미분류" bucket and doesn't need a stored name.
    val kanbanColumns: List<String> = emptyList()
)
