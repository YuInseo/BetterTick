package com.bettertick.data.model

import com.google.firebase.Timestamp

data class FocusSession(
    val id: String = "",
    val activityName: String = "",
    val activityIcon: String = "",
    val activityColor: String = "#FF8C00",
    val durationSeconds: Long = 0,
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null,
    val isCompleted: Boolean = false
)
