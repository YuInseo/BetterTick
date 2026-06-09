package com.bettertick.data.model

import com.google.firebase.Timestamp

data class LocationRecord(
    val id: String = "",
    val dateStr: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)
