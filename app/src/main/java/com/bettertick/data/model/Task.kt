package com.bettertick.data.model

import com.google.firebase.Timestamp

data class Task(
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val listId: String = "",
    val tagIds: List<String> = emptyList(),
    val dueDate: Timestamp? = null,
    val durationMinutes: Int = 60,
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val isAbandoned: Boolean = false,
    val abandonedAt: Timestamp? = null,
    val priority: Int = 0,
    val repeatRule: String? = null,
    val repeatEnd: String? = null,
    // ISO-8601 local dates ("2026-04-20") where this recurring task's
    // occurrence is skipped — used when the user moves or deletes a
    // single instance of a repeating task without touching the series.
    val exceptions: List<String> = emptyList(),
    val notionUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val sortOrder: Long = 0,
    // Which kanban column (by name) this task belongs to inside its list's
    // kanban view. Empty = 미분류 (uncategorized). Irrelevant for list/timetable
    // view types — the field is only read when the parent list renders as
    // kanban.
    val kanbanColumn: String = "",
    // Local attachments. Each entry is `kind|uri|name` where:
    //   kind = "image" | "file" | "audio"
    //   uri  = content:// or file:// URI (local device only, not uploaded)
    //   name = display filename shown as caption
    // The URIs point to on-device content — syncing across devices is not
    // attempted here; a future PC companion app will handle that path.
    val attachments: List<String> = emptyList()
)
