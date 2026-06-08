package com.example.scanpro.di

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlKitModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer = 
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @Provides
    @Singleton
    fun provideBarcodeScanner(): BarcodeScanner = 
        BarcodeScanning.getClient()
}
