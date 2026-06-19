package com.bettertick.ui.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.FavoritePlace
import com.bettertick.data.model.LocationRecord
import com.bettertick.data.repository.LocationRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LocationHistoryViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    fun getRecordsForDate(dateStr: String): Flow<List<LocationRecord>> =
        locationRepository.observeRecordsForDate(dateStr)

    val favorites = locationRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFavorite(name: String, lat: Double, lng: Double) {
        viewModelScope.launch { locationRepository.addFavorite(name, lat, lng) }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch { locationRepository.removeFavorite(id) }
    }

    // 동선 화면을 보는 동안 이동을 즉시 기록 — 백그라운드 서비스의 200m 기준을
    // 기다리지 않고 화면에서 경로가 실시간으로 쌓이도록. ~20m 이동마다 한 점
    // (걸은 길을 촘촘히 남겨 직선이 아니라 실제 경로처럼 보이게).
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastWriteMs = 0L
    fun recordWaypointIfMoved(lat: Double, lng: Double) {
        // 최소 시간 간격(4초) 스로틀 — 빠른 이동/GPS 튐으로 쓰기가 폭주해 무료
        // 한도를 갉아먹는 것을 막는다(걷기 20m는 보통 이 간격 안에 안 넘음).
        val now = System.currentTimeMillis()
        if (lastWriteMs != 0L && now - lastWriteMs < 4_000L) return
        if (lastLat != 0.0 || lastLng != 0.0) {
            val out = FloatArray(1)
            android.location.Location.distanceBetween(lastLat, lastLng, lat, lng, out)
            if (out[0] < 20f) return
        }
        lastLat = lat; lastLng = lng
        lastWriteMs = now
        viewModelScope.launch {
            locationRepository.addRecord(
                LocationRecord(
                    dateStr = LocalDate.now().toString(),
                    timestamp = Timestamp(Date()),
                    latitude = lat,
                    longitude = lng,
                    address = "%.4f, %.4f".format(lat, lng)
                )
            )
        }
    }
}
