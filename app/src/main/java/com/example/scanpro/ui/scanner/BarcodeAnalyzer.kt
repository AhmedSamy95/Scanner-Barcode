package com.example.scanpro.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import java.util.concurrent.Executors

/**
 * Optimized CameraX Image Analyzer.
 * Uses shared singleton clients and background processing for maximum performance.
 */
class BarcodeAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val textRecognizer: TextRecognizer,
    private val scanModeProvider: () -> ScanMode,
    private val ocrFrameScaleProvider: () -> Float,
    private val onBarcodeDetected: (List<Barcode>) -> Unit,
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val processingExecutor = Executors.newSingleThreadExecutor()
    private var lastAnalysisTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        val currentMode = scanModeProvider()

        // OCR is heavier, throttle more strictly. Barcode is light, allow more frames.
        // Reduced OCR throttle from 600ms to 300ms for faster initial detection.
        val throttleMs = if (currentMode == ScanMode.OCR) 300 else 200
        
        if (currentTime - lastAnalysisTime < throttleMs) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            when (currentMode) {
                ScanMode.BARCODE -> {
                    barcodeScanner.process(image)
                        .addOnSuccessListener(processingExecutor) { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                onBarcodeDetected(barcodes)
                            }
                        }
                        .addOnFailureListener { /* Suppress */ }
                        .addOnCompleteListener { imageProxy.close() }
                }
                ScanMode.OCR -> {
                    val frameScale = ocrFrameScaleProvider()
                    textRecognizer.process(image)
                        .addOnSuccessListener(processingExecutor) { visionText ->
                            val filteredText = filterTextByRoi(visionText, imageProxy, frameScale)
                            if (filteredText.isNotBlank()) {
                                onTextDetected(filteredText)
                            }
                        }
                        .addOnFailureListener { /* Suppress */ }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }
        } else {
            imageProxy.close()
        }
    }

    private fun filterTextByRoi(
        visionText: com.google.mlkit.vision.text.Text,
        imageProxy: ImageProxy,
        scale: Float
    ): String {
        val imageWidth = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()
        
        val rotation = imageProxy.imageInfo.rotationDegrees
        val (effectiveWidth, effectiveHeight) = if (rotation == 90 || rotation == 270) {
            imageHeight to imageWidth
        } else {
            imageWidth to imageHeight
        }

        val roiWidth = effectiveWidth * 0.85f
        val roiHeight = effectiveHeight * scale
        
        val left = (effectiveWidth - roiWidth) / 2
        val top = (effectiveHeight - roiHeight) / 2
        val right = left + roiWidth
        val bottom = top + roiHeight

        val sb = StringBuilder()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null) {
                    val centerX = box.centerX().toFloat()
                    val centerY = box.centerY().toFloat()
                    
                    if (centerX in left..right && centerY in top..bottom) {
                        val text = line.text
                        if (!isJunkBarcodeText(text)) {
                            sb.append(text).append("\n")
                        }
                    }
                }
            }
        }
        return sb.toString().trim()
    }

    private fun isJunkBarcodeText(text: String): Boolean {
        if (text.length < 3) return false

        val verticalChars = text.count { it == '|' || it == 'I' || it == 'l' || it == '1' || it == 'i' }
        val ratio = verticalChars.toFloat() / text.length
        if (text.length > 5 && ratio > 0.8f) return true

        if (text.none { it.isLetterOrDigit() }) return true

        val stripped = text.replace(" ", "")
        val digitCount = stripped.count { it.isDigit() }
        val digitRatio = digitCount.toFloat() / stripped.length
        if (stripped.length >= 8 && digitRatio >= 0.75f) return true

        return false
    }
}
