package com.example.scanpro.data.repository

import com.example.scanpro.data.local.db.BarcodeDao
import com.example.scanpro.data.local.datastore.SettingsDataStore
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.model.toEntity
import com.example.scanpro.domain.model.toDomainModel
import com.example.scanpro.domain.repository.BarcodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Concrete implementation of [BarcodeRepository] backed by Room ([BarcodeDao])
 * for barcode persistence and Jetpack DataStore ([SettingsDataStore]) for
 * scanner preferences.
 */
class BarcodeRepositoryImpl @Inject constructor(
    private val dao: BarcodeDao,
    private val settingsStore: SettingsDataStore
) : BarcodeRepository {

    override fun getAllBarcodes(): Flow<List<BarcodeItem>> {
        return dao.getAllSorted().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFavorites(): Flow<List<BarcodeItem>> {
        return dao.getFavorites().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchBarcodes(query: String): Flow<List<BarcodeItem>> {
        return dao.search(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getBarcodeById(id: Long): BarcodeItem? {
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun getByRawValue(rawValue: String): BarcodeItem? {
        return dao.getByRawValue(rawValue)?.toDomainModel()
    }

    override suspend fun insertBarcode(item: BarcodeItem): Long {
        return dao.insert(item.toEntity())
    }

    override suspend fun updateBarcode(item: BarcodeItem) {
        dao.update(item.toEntity())
    }

    override suspend fun deleteBarcode(item: BarcodeItem) {
        dao.delete(item.toEntity())
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun getAllForExport(): List<BarcodeItem> {
        return dao.getAllForExport().map { it.toDomainModel() }
    }

    override suspend fun importBarcodes(items: List<BarcodeItem>) {
        dao.insertAll(items.map { it.toEntity() })
    }

    // Settings
    override val continuousScan: Flow<Boolean>
        get() = settingsStore.continuousScan

    override val handleDuplicates: Flow<Boolean>
        get() = settingsStore.handleDuplicates

    override val soundFeedback: Flow<Boolean>
        get() = settingsStore.soundFeedback

    override val vibrationFeedback: Flow<Boolean>
        get() = settingsStore.vibrationFeedback

    override val autoCopyToClipboard: Flow<Boolean>
        get() = settingsStore.autoCopyToClipboard

    override val productLookup: Flow<Boolean>
        get() = settingsStore.productLookup

    override suspend fun setContinuousScan(enabled: Boolean) {
        settingsStore.setContinuousScan(enabled)
    }

    override suspend fun setHandleDuplicates(enabled: Boolean) {
        settingsStore.setHandleDuplicates(enabled)
    }

    override suspend fun setSoundFeedback(enabled: Boolean) {
        settingsStore.setSoundFeedback(enabled)
    }

    override suspend fun setVibrationFeedback(enabled: Boolean) {
        settingsStore.setVibrationFeedback(enabled)
    }

    override suspend fun setAutoCopyToClipboard(enabled: Boolean) {
        settingsStore.setAutoCopyToClipboard(enabled)
    }

    override suspend fun setProductLookup(enabled: Boolean) {
        settingsStore.setProductLookup(enabled)
    }
}
