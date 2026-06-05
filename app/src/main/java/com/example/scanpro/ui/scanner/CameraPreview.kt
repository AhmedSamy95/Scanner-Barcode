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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors

/**
 * CameraX view wrapper hosting camera surface and sending frames to BarcodeAnalyzer.
 */
@Composable
fun CameraPreview(
    isFlashOn: Boolean,
    zoomRatio: Float,
    onZoomChanged: (Float) -> Unit,
    onBarcodeDetected: (List<Barcode>) -> Unit,
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

    // Update Flash Torch State — يعمل أيضاً فور استعداد الكاميرا
    LaunchedEffect(isFlashOn, cameraInstance) {
        cameraInstance?.cameraControl?.enableTorch(isFlashOn)
    }

    // Update Zoom Ratio State — يعمل أيضاً فور استعداد الكاميرا
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
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analyzerExecutor, BarcodeAnalyzer(onBarcodeDetected))
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
                    
                    // Set default zoom states
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
