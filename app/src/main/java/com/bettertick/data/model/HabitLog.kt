package com.bettertick.data.model

import com.google.firebase.Timestamp

data class HabitLog(
    val id: String = "",
    val habitId: String = "",
    val date: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    // Rich check-in fields: mood = -1 for unset, 0..4 for 😭 ☹️ 😐 🙂 😄
    val mood: Int = -1,
    val note: String = ""
)
