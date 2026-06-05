package com.example.scanpro.ui.scanner

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.model.BarcodeContentType
import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import com.example.scanpro.domain.usecase.ScanBarcodeUseCase
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.camera.core.Camera
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
    val autoCopyToClipboard: Boolean = false
)

/**
 * ViewModel managing scanning state, camera configurations, and feedback triggers.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScanTime = 0L
    private var lastScanValue = ""

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
        // Ignore incoming frames while scanning is paused (detail being shown)
        if (uiState.value.isScanningPaused) return

        val barcode = barcodes.firstOrNull() ?: return
        val rawValue = barcode.rawValue ?: return
        val displayValue = barcode.displayValue ?: rawValue

        val currentTime = System.currentTimeMillis()

        // Throttling to prevent spamming identical results
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
        // Hide the sheet but keep scanning paused — user must tap "Scan Again" to resume
        _uiState.update { it.copy(showBottomSheet = false) }
    }

    fun resumeScanning() {
        _uiState.update {
            it.copy(
                showBottomSheet = false,
                isScanningPaused = false,
                lastScannedBarcode = null
            )
        }
        lastScanValue = "" // Reset to allow scanning the same item again
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
