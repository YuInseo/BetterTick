package com.bettertick.ui.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.FavoritePlace
import com.bettertick.data.model.LocationRecord
import com.bettertick.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
}
