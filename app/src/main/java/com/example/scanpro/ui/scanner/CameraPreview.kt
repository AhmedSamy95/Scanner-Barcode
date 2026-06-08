package com.example.scanpro.ui.scanner

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.TextRecognizer
import java.util.concurrent.Executors

/**
 * CameraX view wrapper hosting camera surface and sending frames to BarcodeAnalyzer.
 */
@Composable
fun CameraPreview(
    isFlashOn: Boolean,
    zoomRatio: Float,
    scanMode: ScanMode,
    ocrFrameScale: Float,
    barcodeScanner: BarcodeScanner,
    textRecognizer: TextRecognizer,
    onZoomChanged: (Float) -> Unit,
    onBarcodeDetected: (List<Barcode>) -> Unit,
    onTextDetected: (String) -> Unit,
    onCameraReady: (Camera) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    val currentScanMode by rememberUpdatedState(scanMode)
    val currentOcrFrameScale by rememberUpdatedState(ocrFrameScale)
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnTextDetected by rememberUpdatedState(onTextDetected)

    LaunchedEffect(isFlashOn, cameraInstance) {
        cameraInstance?.cameraControl?.enableTorch(isFlashOn)
    }

    LaunchedEffect(zoomRatio, cameraInstance) {
        cameraInstance?.cameraInfo?.zoomState?.value?.let { zoomState ->
            val minZoom = zoomState.minZoomRatio
            val maxZoom = zoomState.maxZoomRatio
            val targetZoom = zoomRatio.coerceIn(minZoom, maxZoom)
            cameraInstance?.cameraControl?.setZoomRatio(targetZoom)
        }
    }

    AndroidView(
        factory = { ctx ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(960, 540))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analyzerExecutor, BarcodeAnalyzer(
                            barcodeScanner = barcodeScanner,
                            textRecognizer = textRecognizer,
                            scanModeProvider = { currentScanMode },
                            ocrFrameScaleProvider = { currentOcrFrameScale },
                            onBarcodeDetected = { barcodes -> currentOnBarcodeDetected(barcodes) },
                            onTextDetected = { text -> currentOnTextDetected(text) }
                        ))
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    cameraInstance = boundCamera
                    onCameraReady(boundCamera)
                    boundCamera.cameraControl.enableTorch(isFlashOn)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    cameraInstance?.cameraInfo?.zoomState?.value?.let { zoomState ->
                        val currentZoom = zoomState.zoomRatio
                        val newZoom = (currentZoom * zoom).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                        onZoomChanged(newZoom)
                    }
                }
            }
    )
}
