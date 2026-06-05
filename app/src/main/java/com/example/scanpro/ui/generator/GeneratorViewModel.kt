package com.example.scanpro.ui.generator

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.model.BarcodeContentType
import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import com.example.scanpro.domain.usecase.GenerateBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

data class GeneratorUiState(
    val inputText: String = "",
    val selectedFormat: BarcodeFormatType = BarcodeFormatType.QR_CODE,
    val generatedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null,
    val isAddedToFavorites: Boolean = false
)

/**
 * ViewModel handling barcode generation actions, saving bitmaps to scoped storage, and sharing images.
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generateBarcodeUseCase: GenerateBarcodeUseCase,
    private val repository: BarcodeRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, saveSuccessMessage = null, isAddedToFavorites = false) }
    }

    fun onFormatSelected(format: BarcodeFormatType) {
        _uiState.update { it.copy(selectedFormat = format, saveSuccessMessage = null, isAddedToFavorites = false) }
    }

    fun generateBarcode() {
        val state = uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter some data to encode") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, errorMessage = null, generatedBitmap = null) }

        viewModelScope.launch {
            try {
                // ZXing generation can block main thread, shift to Dispatchers.Default
                val bitmap = withContext(Dispatchers.Default) {
                    val width = if (state.selectedFormat.is2D) 600 else 800
                    val height = if (state.selectedFormat.is2D) 600 else 250
                    generateBarcodeUseCase(
                        content = state.inputText,
                        format = state.selectedFormat,
                        width = width,
                        height = height
                    )
                }

                if (bitmap != null) {
                    _uiState.update {
                        it.copy(
                            generatedBitmap = bitmap,
                            isGenerating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to generate barcode. Check characters fit the format rules.",
                            isGenerating = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Error: ${e.localizedMessage}",
                        isGenerating = false
                    )
                }
            }
        }
    }

    fun saveToGallery() {
        val bitmap = uiState.value.generatedBitmap ?: return
        val filename = "ScanPro_${System.currentTimeMillis()}.png"

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val resolver = application.contentResolver
                    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                    val imageDetails = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScanPro")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val imageUri = resolver.insert(imageCollection, imageDetails)
                    if (imageUri != null) {
                        resolver.openOutputStream(imageUri).use { out ->
                            if (out != null) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            imageDetails.clear()
                            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(imageUri, imageDetails, null, null)
                        }
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (success) {
                _uiState.update { it.copy(saveSuccessMessage = "Saved to Gallery successfully!") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to save image to Gallery") }
            }
        }
    }

    fun shareBarcode() {
        val bitmap = uiState.value.generatedBitmap ?: return

        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val cachePath = File(application.cacheDir, "shared_images")
                    cachePath.mkdirs()
                    val file = File(cachePath, "generated_barcode.png")
                    val stream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()

                    FileProvider.getUriForFile(
                        application,
                        "${application.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Generated Barcode").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(chooser)
            }
        }
    }

    fun addToFavorites() {
        val state = uiState.value
        if (state.inputText.isBlank()) return

        val barcodeItem = BarcodeItem(
            rawValue = state.inputText,
            displayValue = state.inputText,
            format = state.selectedFormat,
            contentType = BarcodeContentType.fromRawValue(state.inputText),
            isFavorite = true,
            source = "GENERATED"
        )

        viewModelScope.launch {
            repository.insertBarcode(barcodeItem)
            _uiState.update { it.copy(isAddedToFavorites = true, saveSuccessMessage = "Added to History & Favorites!") }
        }
    }

    fun clearNotifications() {
        _uiState.update { it.copy(errorMessage = null, saveSuccessMessage = null) }
    }
}
