package com.bettertick.ui.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.TabBarConfig
import com.bettertick.data.model.defaultTabBarConfig
import com.bettertick.data.repository.TabBarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabBarViewModel @Inject constructor(
    private val repository: TabBarRepository
) : ViewModel() {

    val config: StateFlow<TabBarConfig> = repository.observeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultTabBarConfig)

    fun saveConfig(config: TabBarConfig) {
        viewModelScope.launch { repository.saveConfig(config) }
    }
}
