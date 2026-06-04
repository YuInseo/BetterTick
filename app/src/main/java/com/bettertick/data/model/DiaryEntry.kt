package com.bettertick.data.model

import com.google.firebase.Timestamp

data class DiaryEntry(
    val id: String = "",
    val dateStr: String = "",
    val content: String = "",
    val mood: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
