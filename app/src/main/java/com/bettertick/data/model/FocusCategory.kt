package com.bettertick.data.model

import com.google.firebase.Timestamp

data class FocusCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val color: String = "#FF8C00",
    val sortOrder: Long = 0,
    val createdAt: Timestamp = Timestamp.now()
)
