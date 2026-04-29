package com.bettertick.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val settings: Map<String, Any> = emptyMap()
)
