package com.bettertick.data.model

/** Checkbox long-press menu (앱) / row-tap menu (위젯). */
enum class TaskStatusAction(val label: String) {
    COMPLETE("완료"),
    REOPEN("미완료로 되돌리기"),
    CANCEL("취소됨"),
    POSTPONE("내일로 미루기");

    companion object {
        /** Menu entries that make sense for [task]'s current state. */
        fun choicesFor(task: Task): List<TaskStatusAction> = when {
            task.isCompleted || task.isAbandoned -> listOf(REOPEN) +
                (if (task.isCompleted) listOf(CANCEL) else listOf(COMPLETE))
            else -> listOf(COMPLETE, CANCEL, POSTPONE)
        }
    }
}
