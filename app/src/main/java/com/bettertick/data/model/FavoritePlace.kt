package com.bettertick.data.model

/**
 * 사용자가 이름 붙인 즐겨찾기 위치. 동선 화면에서 이 위치 반경 안의 기록은
 * 지번 주소 대신 이 이름으로 표시한다.
 */
data class FavoritePlace(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
