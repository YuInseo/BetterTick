package com.bettertick.ui.screens.habits

data class HabitDraft(
    val name: String,
    val description: String = "",
    val icon: String = "",
    val color: String = "#FF8C00",
    val frequency: String = "daily",
    val targetDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val weeklyCount: Int = 3,
    val intervalDays: Int = 2,
    val goalType: String = "complete_all",
    val startDate: String = "",
    val targetDayCount: Int = 0,
    val group: String = "기타",
    val reminders: List<String> = emptyList(),
    val autoShowLog: Boolean = false
)
