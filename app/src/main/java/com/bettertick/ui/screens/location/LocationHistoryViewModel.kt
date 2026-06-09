package com.bettertick.ui.screens.location

import androidx.lifecycle.ViewModel
import com.bettertick.data.model.LocationRecord
import com.bettertick.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LocationHistoryViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    fun getRecordsForDate(dateStr: String): Flow<List<LocationRecord>> =
        locationRepository.observeRecordsForDate(dateStr)
}
