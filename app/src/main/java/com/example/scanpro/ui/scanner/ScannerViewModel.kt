package com.example.scanpro.ui.scanner

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.Camera
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.model.BarcodeContentType
import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import com.example.scanpro.domain.usecase.ScanBarcodeUseCase
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isFlashOn: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 5f,
    val lastScannedBarcode: BarcodeItem? = null,
    val showBottomSheet: Boolean = false,
    val isScanningPaused: Boolean = false,
    val continuousScan: Boolean = false,
    val soundFeedback: Boolean = true,
    val vibrationFeedback: Boolean = true,
    val autoCopyToClipboard: Boolean = false,
    val scanMode: ScanMode = ScanMode.BARCODE,
    val detectedText: String = "",
    val isGalleryProcessing: Boolean = false,
    val ocrFrameSize: Float = 0.5f,
    val multiScanResults: List<BarcodeItem> = emptyList(),
    val showMultiScanPicker: Boolean = false
)

enum class ScanMode {
    BARCODE, OCR
}

/**
 * ViewModel managing scanning state, camera configurations, and feedback triggers.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    val textRecognizer: TextRecognizer,
    val barcodeScanner: BarcodeScanner,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScanTime = 0L
    private var lastScanValue = ""
    private var lastOcrUpdateTime = 0L

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    init {
        viewModelScope.launch {
            combine(
                repository.continuousScan,
                repository.soundFeedback,
                repository.vibrationFeedback,
                repository.autoCopyToClipboard
            ) { continuous, sound, vibrate, autocopy ->
                _uiState.update {
                    it.copy(
                        continuousScan = continuous,
                        soundFeedback = sound,
                        vibrationFeedback = vibrate,
                        autoCopyToClipboard = autocopy
                    )
                }
            }.collect()
        }
    }

    fun onCameraReady(camera: Camera) {
        camera.cameraInfo.zoomState.value?.let { zoomState ->
            _uiState.update {
                it.copy(
                    minZoom = zoomState.minZoomRatio,
                    maxZoom = zoomState.maxZoomRatio,
                    zoomRatio = zoomState.zoomRatio
                )
            }
        }
    }

    fun setZoomRatio(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    fun onBarcodesDetected(barcodes: List<Barcode>) {
        if (uiState.value.isScanningPaused || uiState.value.scanMode != ScanMode.BARCODE) return

        val barcode = barcodes.firstOrNull() ?: return
        val rawValue = barcode.rawValue ?: return
        val displayValue = barcode.displayValue ?: rawValue

        val currentTime = System.currentTimeMillis()

        if (rawValue == lastScanValue && (currentTime - lastScanTime) < 2000) {
            return
        }

        lastScanTime = currentTime
        lastScanValue = rawValue

        val format = BarcodeFormatType.fromMlKitFormat(barcode.format)
        val contentType = BarcodeContentType.fromRawValue(rawValue)

        val barcodeItem = BarcodeItem(
            rawValue = rawValue,
            displayValue = displayValue,
            format = format,
            contentType = contentType,
            timestamp = currentTime,
            source = "SCAN"
        )

        viewModelScope.launch {
            val savedId = scanBarcodeUseCase(barcodeItem)
            val finalItem = barcodeItem.copy(id = savedId)

            triggerFeedback()

            if (uiState.value.autoCopyToClipboard) {
                copyToClipboard(rawValue)
            }

            if (!uiState.value.continuousScan) {
                _uiState.update {
                    it.copy(
                        lastScannedBarcode = finalItem,
                        showBottomSheet = true,
                        isScanningPaused = true,
                        continuousScan = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        lastScannedBarcode = finalItem
                    )
                }
            }
        }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(showBottomSheet = false) }
    }

    fun resumeScanning() {
        _uiState.update {
            it.copy(
                showBottomSheet = false,
                isScanningPaused = false,
                lastScannedBarcode = null,
                detectedText = ""
            )
        }
        lastScanValue = ""
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { 
            it.copy(
                scanMode = mode, 
                detectedText = "", 
                isScanningPaused = false,
                showBottomSheet = false,
                lastScannedBarcode = null
            ) 
        }
        lastScanValue = ""
    }

    fun setOcrFrameSize(size: Float) {
        _uiState.update { it.copy(ocrFrameSize = size.coerceIn(0.2f, 0.9f)) }
    }

    fun onTextDetected(text: String) {
        if (uiState.value.isScanningPaused || uiState.value.scanMode != ScanMode.OCR) return
        if (text.isBlank() || text == uiState.value.detectedText) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOcrUpdateTime < 400) return

        lastOcrUpdateTime = currentTime
        _uiState.update { it.copy(detectedText = text) }
    }

    fun copyDetectedText() {
        val text = uiState.value.detectedText
        if (text.isNotEmpty()) {
            copyToClipboard(text)
            triggerFeedback()
        }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(isGalleryProcessing = true) }
        val image: InputImage
        try {
            image = InputImage.fromFilePath(application, uri)
        } catch (e: Exception) {
            _uiState.update { it.copy(isGalleryProcessing = false) }
            return
        }

        if (uiState.value.scanMode == ScanMode.BARCODE) {
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    _uiState.update { it.copy(isGalleryProcessing = false) }
                    if (barcodes.isEmpty()) return@addOnSuccessListener
                    
                    val items = barcodes.map { barcode ->
                        val rawValue = barcode.rawValue ?: ""
                        val format = BarcodeFormatType.fromMlKitFormat(barcode.format)
                        val contentType = BarcodeContentType.fromRawValue(rawValue)
                        BarcodeItem(
                            rawValue = rawValue,
                            displayValue = barcode.displayValue ?: rawValue,
                            format = format,
                            contentType = contentType,
                            source = "GALLERY"
                        )
                    }.filter { it.rawValue.isNotEmpty() }

                    if (items.size == 1) {
                        handleSingleBarcode(items.first())
                    } else if (items.size > 1) {
                        _uiState.update { it.copy(multiScanResults = items, showMultiScanPicker = true) }
                    }
                }
                .addOnFailureListener {
                    _uiState.update { it.copy(isGalleryProcessing = false) }
                }
        } else {
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    _uiState.update { 
                        it.copy(
                            isGalleryProcessing = false,
                            detectedText = visionText.text
                        ) 
                    }
                    lastOcrUpdateTime = System.currentTimeMillis()
                }
                .addOnFailureListener {
                    _uiState.update { it.copy(isGalleryProcessing = false) }
                }
        }
    }

    private fun handleSingleBarcode(item: BarcodeItem) {
        viewModelScope.launch {
            val savedId = scanBarcodeUseCase(item)
            val finalItem = item.copy(id = savedId)
            triggerFeedback()
            _uiState.update {
                it.copy(
                    lastScannedBarcode = finalItem,
                    showBottomSheet = true,
                    isScanningPaused = true
                )
            }
        }
    }

    fun onBarcodeSelectedFromList(item: BarcodeItem) {
        _uiState.update { it.copy(showMultiScanPicker = false, multiScanResults = emptyList()) }
        handleSingleBarcode(item)
    }

    fun dismissMultiScanPicker() {
        _uiState.update { it.copy(showMultiScanPicker = false, multiScanResults = emptyList()) }
    }

    private fun triggerFeedback() {
        val state = uiState.value
        
        if (state.soundFeedback) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                // Ignore audio crash
            }
        }

        if (state.vibrationFeedback) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(80)
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Scanned Barcode", text)
        clipboard?.setPrimaryClip(clip)
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator.release()
    }
}
