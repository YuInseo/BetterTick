package com.bettertick.data.model

import com.google.firebase.Timestamp

data class Tag(
    val id: String = "",
    val name: String = "",
    val color: String = "#FF8C00",
    val parentTagId: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)
