package com.bettertick.data.model

import com.google.firebase.Timestamp

data class LocationRecord(
    val id: String = "",
    val dateStr: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    // 한곳에 일정 시간 머문 "건물 방문" 지점이면 true → 지도에 깃발로 표시.
    // 단순 이동 경로상의 waypoint는 false.
    val isPlace: Boolean = false,
    // 방문한 건물/장소 이름(역지오코딩). 깃발 라벨/바텀시트에 사용.
    val placeName: String = ""
)
