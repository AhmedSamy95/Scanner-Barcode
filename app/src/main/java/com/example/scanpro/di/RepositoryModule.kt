package com.example.scanpro.di

import com.example.scanpro.data.repository.BarcodeRepositoryImpl
import com.example.scanpro.domain.repository.BarcodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBarcodeRepository(
        impl: BarcodeRepositoryImpl
    ): BarcodeRepository
}
