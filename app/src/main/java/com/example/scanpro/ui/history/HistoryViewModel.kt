package com.example.scanpro.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import com.example.scanpro.domain.usecase.ExportHistoryUseCase
import com.example.scanpro.domain.usecase.ImportHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val barcodes: List<BarcodeItem> = emptyList(),
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * ViewModel managing local history database collection, querying, deletion, and exports.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    private val exportHistoryUseCase: ExportHistoryUseCase,
    private val importHistoryUseCase: ImportHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Combine database records stream with search text and favorites toggling
    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllBarcodes(),
        _searchQuery,
        _showFavoritesOnly,
        _isLoading
    ) { allBarcodes, search, favOnly, loading ->
        val filteredList = allBarcodes.filter { item ->
            val matchSearch = item.rawValue.contains(search, ignoreCase = true) ||
                    item.displayValue.contains(search, ignoreCase = true) ||
                    item.format.displayName.contains(search, ignoreCase = true)

            val matchFav = !favOnly || item.isFavorite

            matchSearch && matchFav
        }.sortedWith(
            compareByDescending<BarcodeItem> { it.isFavorite }
                .thenByDescending { it.timestamp }
        )

        HistoryUiState(
            barcodes = filteredList,
            searchQuery = search,
            showFavoritesOnly = favOnly,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(item: BarcodeItem) {
        viewModelScope.launch {
            repository.updateBarcode(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteBarcode(item: BarcodeItem) {
        viewModelScope.launch {
            repository.deleteBarcode(item)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _feedbackMessage.value = "History cleared successfully"
        }
    }

    fun exportToCsv(): String? {
        var csvContent: String? = null
        _isLoading.value = true
        viewModelScope.launch {
            try {
                csvContent = exportHistoryUseCase.exportCsv()
                _feedbackMessage.value = "CSV exported successfully!"
            } catch (e: Exception) {
                _feedbackMessage.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
        return csvContent
    }

    fun exportToJson(): String? {
        var jsonContent: String? = null
        _isLoading.value = true
        viewModelScope.launch {
            try {
                jsonContent = exportHistoryUseCase.exportJson()
                _feedbackMessage.value = "JSON exported successfully!"
            } catch (e: Exception) {
                _feedbackMessage.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
        return jsonContent
    }

    fun importCsv(content: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val count = importHistoryUseCase.importCsv(content)
                _feedbackMessage.value = "Successfully imported $count records!"
            } catch (e: Exception) {
                _feedbackMessage.value = "Import failed. Verify CSV format."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importJson(content: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val count = importHistoryUseCase.importJson(content)
                _feedbackMessage.value = "Successfully imported $count records!"
            } catch (e: Exception) {
                _feedbackMessage.value = "Import failed. Verify JSON format."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
