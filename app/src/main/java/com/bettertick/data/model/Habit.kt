package com.bettertick.data.model

import com.google.firebase.Timestamp

data class Habit(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val color: String = "#FF8C00",
    val frequency: String = "daily",              // "daily" | "weekly" | "interval"
    val targetDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val weeklyCount: Int = 3,                     // for frequency="weekly"
    val intervalDays: Int = 2,                    // for frequency="interval"
    val reminderTime: String? = null,
    val reminders: List<String> = emptyList(),    // HH:mm list
    val sortOrder: Long = 0,
    val isArchived: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    // New options
    val goalType: String = "complete_all",        // "complete_all" | "reach_amount"
    val startDate: String = "",                   // yyyy-MM-dd (empty = use createdAt)
    val targetDayCount: Int = 0,                  // 0 = forever
    val group: String = "기타",
    val autoShowLog: Boolean = false
)
