package com.example.scanpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.repository.BarcodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val continuousScan: Boolean = false,
    val handleDuplicates: Boolean = true,
    val soundFeedback: Boolean = true,
    val vibrationFeedback: Boolean = true,
    val autoCopyToClipboard: Boolean = false,
    val productLookup: Boolean = false
)

/**
 * ViewModel managing setting states by listening/writing preferences via Repository and DataStore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BarcodeRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.continuousScan,
        repository.handleDuplicates,
        repository.soundFeedback,
        repository.vibrationFeedback,
        repository.autoCopyToClipboard,
        repository.productLookup
    ) { array ->
        SettingsUiState(
            continuousScan = array[0],
            handleDuplicates = array[1],
            soundFeedback = array[2],
            vibrationFeedback = array[3],
            autoCopyToClipboard = array[4],
            productLookup = array[5]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setContinuousScan(enabled: Boolean) {
        viewModelScope.launch { repository.setContinuousScan(enabled) }
    }

    fun setHandleDuplicates(enabled: Boolean) {
        viewModelScope.launch { repository.setHandleDuplicates(enabled) }
    }

    fun setSoundFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundFeedback(enabled) }
    }

    fun setVibrationFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationFeedback(enabled) }
    }

    fun setAutoCopyToClipboard(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoCopyToClipboard(enabled) }
    }

    fun setProductLookup(enabled: Boolean) {
        viewModelScope.launch { repository.setProductLookup(enabled) }
    }
}
