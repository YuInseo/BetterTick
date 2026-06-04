package com.bettertick.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Tasks : BottomNavItem("tasks", "Tasks", Icons.Outlined.CheckBox)
    data object Calendar : BottomNavItem("calendar", "Calendar", Icons.Outlined.CalendarMonth)
    data object Matrix : BottomNavItem("matrix", "Matrix", Icons.Outlined.GridView)
    data object Focus : BottomNavItem("focus", "Focus", Icons.Outlined.RadioButtonUnchecked)
    data object Habits : BottomNavItem("habits", "Habits", Icons.Outlined.Schedule)
    data object Diary : BottomNavItem("diary", "일기", Icons.Outlined.Book)
    data object More : BottomNavItem("more", "More", Icons.Outlined.MoreHoriz)

    companion object {
        // `by lazy` defers building the list until after every nested
        // `data object` above has been class-loaded. Eagerly evaluating
        // `listOf(...)` here races with the data-object initializers and
        // leaves null entries in the list, crashing the nav bar with an
        // NPE on `route`.
        val items: List<BottomNavItem> by lazy {
            listOf(Tasks, Calendar, Matrix, Focus, Habits, Diary, More)
        }
    }
}
