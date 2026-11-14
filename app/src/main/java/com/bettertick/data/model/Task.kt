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
    val attachments: List<String> = emptyList(),
    // Reminder offsets in minutes before the task's start (0 = on time).
    // All-day tasks are reminded relative to 09:00 that day. Scheduled by
    // ReminderScheduler; empty = no reminder.
    val reminders: List<Int> = emptyList(),
    // 고정 — pinned tasks sort to the top of their list (TickTick 'Pin').
    val isPinned: Boolean = false,
    // 근무(주/야/비/휴) 할일의 휴가 사용 기록: "yyyy-MM-dd" → "병가" | "지휴".
    // Keyed by date so a repeating shift keeps one record per occurrence.
    val leaves: Map<String, String> = emptyMap(),
    // 근무 할일의 날짜별 근무 바꿈: "yyyy-MM-dd" → "주" | "야" | "비" | "휴" | "일근".
    // That day counts as this shift instead of the one in the title.
    val dayShifts: Map<String, String> = emptyMap(),
    // 하위 할일: id of the task this one is nested under (one level only).
    // A subtask follows its parent's start day, repeat rule, end and skipped
    // days (see TaskHierarchy.kt) but keeps its own time and length, so its
    // timetable block repeats together with the parent. Empty = standalone.
    val parentId: String = "",
    // 반복 할일의 날짜별 완료: "yyyy-MM-dd" of each occurrence checked off.
    val completedDates: List<String> = emptyList(),
    // true = 체크하면 반복 전체가 완료 (예전 방식); false = 날짜별로 체크.
    val checkWholeSeries: Boolean = false,
    // 개별 메모: 반복 할일의 날짜별 메모 ("yyyy-MM-dd" → 메모). [notes]는 공통 메모.
    val dayNotes: Map<String, String> = emptyMap(),
    // 메모장: + 버튼으로 쌓는 메모들. 반복 할일은 메모마다 공통 / 그 날만.
    val memos: List<TaskMemo> = emptyList()
)

/** 할일 메모 하나. [day] 비어 있으면 공통(모든 반복), "yyyy-MM-dd"면 그 날만. */
data class TaskMemo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val day: String = ""
)

/** Memos to show on [day]: the shared ones plus that day's (all when [day] is null). */
fun Task.memosOn(day: java.time.LocalDate?): List<TaskMemo> {
    val key = day?.toString()
    val legacy = dayNotes.map { (d, t) -> TaskMemo(id = "day-$d", text = t, day = d) }
    return (memos + legacy).filter { it.text.isNotBlank() && (it.day.isBlank() || key == null || it.day == key) }
}
