package com.example.scanpro

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for ScanPro, initialized with Hilt for Dependency Injection.
 */
@HiltAndroidApp
class ScanProApp : Application() {

    @Inject
    lateinit var textRecognizer: TextRecognizer

    @Inject
    lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate() {
        super.onCreate()
        warmUpMlKit()
    }

    /**
     * Runs a dummy process on a background thread using the shared singleton clients
     * to ensure models are loaded and ready before the first real scan.
     */
    private fun warmUpMlKit() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Create a tiny dummy image
                val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                val image = InputImage.fromBitmap(bitmap, 0)
                
                // Trigger first runs to load libraries and models into memory
                textRecognizer.process(image)
                barcodeScanner.process(image)
                
            } catch (e: Exception) {
                // Silently fail, it's just a warm-up
            }
        }
    }
}
