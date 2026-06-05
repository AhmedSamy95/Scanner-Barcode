package com.example.scanpro.domain.repository

import com.example.scanpro.domain.model.BarcodeItem
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for all barcode data and settings operations.
 *
 * This interface is the **single gateway** through which use-cases interact with
 * persistent storage and user preferences. Implementations live in the data layer
 * and are provided via dependency injection, keeping the domain layer free of
 * framework dependencies.
 *
 * ## Reactive streams
 * Methods that return [Flow] provide **real-time observation** of the underlying
 * data source. The UI can collect these flows to receive automatic updates
 * whenever the Room database or DataStore changes.
 *
 * ## Suspend functions
 * One-shot operations (insert, update, delete) are `suspend` functions so they
 * run on a coroutine dispatcher chosen by the caller (typically `Dispatchers.IO`
 * via the ViewModel or use-case).
 *
 * ## Settings
 * Scanner preference flows and setters are co-located here instead of in a
 * separate `SettingsRepository` because they are tightly coupled with scanning
 * behavior (e.g. duplicate handling, continuous scan mode). This keeps the
 * use-case layer from juggling multiple repository dependencies for a single
 * scan operation.
 */
interface BarcodeRepository {

    // ═════════════════════════════════════════════════════════════════════
    // Barcode CRUD & queries
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Observes **all** barcode records, ordered by most recent first.
     *
     * @return A [Flow] emitting the full list whenever the data changes.
     */
    fun getAllBarcodes(): Flow<List<BarcodeItem>>

    /**
     * Observes only the barcodes the user has marked as favorites.
     *
     * @return A [Flow] emitting the filtered list whenever favorites change.
     */
    fun getFavorites(): Flow<List<BarcodeItem>>

    /**
     * Observes barcodes whose [BarcodeItem.rawValue] or [BarcodeItem.displayValue]
     * contains the given [query] (case-insensitive).
     *
     * @param query The search term to filter by.
     * @return A [Flow] emitting matching items reactively.
     */
    fun searchBarcodes(query: String): Flow<List<BarcodeItem>>

    /**
     * Retrieves a single barcode by its database [id].
     *
     * @param id The primary key.
     * @return The matching [BarcodeItem], or `null` if not found.
     */
    suspend fun getBarcodeById(id: Long): BarcodeItem?

    /**
     * Retrieves a barcode by its [rawValue].
     *
     * Used primarily for **duplicate detection** during scanning.
     *
     * @param rawValue The exact raw string to search for.
     * @return The first matching [BarcodeItem], or `null` if none exists.
     */
    suspend fun getByRawValue(rawValue: String): BarcodeItem?

    /**
     * Inserts a new barcode record.
     *
     * @param item The domain model to persist.
     * @return The auto-generated row ID assigned by Room.
     */
    suspend fun insertBarcode(item: BarcodeItem): Long

    /**
     * Updates an existing barcode record in-place.
     *
     * @param item The domain model with updated fields. The [BarcodeItem.id]
     *             must match an existing row.
     */
    suspend fun updateBarcode(item: BarcodeItem)

    /**
     * Deletes a single barcode record.
     *
     * @param item The domain model to remove. Matched by [BarcodeItem.id].
     */
    suspend fun deleteBarcode(item: BarcodeItem)

    /**
     * Deletes **all** barcode records (history wipe).
     */
    suspend fun deleteAll()

    /**
     * Returns every barcode record as a plain list (non-reactive) for
     * one-shot export operations.
     *
     * @return All persisted [BarcodeItem]s.
     */
    suspend fun getAllForExport(): List<BarcodeItem>

    /**
     * Bulk-inserts a list of [BarcodeItem]s, typically from an import operation.
     *
     * @param items The domain models to persist.
     */
    suspend fun importBarcodes(items: List<BarcodeItem>)

    // ═════════════════════════════════════════════════════════════════════
    // Scanner settings — reactive flows
    // ═════════════════════════════════════════════════════════════════════

    /** Whether the scanner should keep scanning after detecting a barcode. */
    val continuousScan: Flow<Boolean>

    /** Whether duplicate raw values should be merged instead of creating new rows. */
    val handleDuplicates: Flow<Boolean>

    /** Whether to play an audible beep on successful scan. */
    val soundFeedback: Flow<Boolean>

    /** Whether to trigger haptic vibration on successful scan. */
    val vibrationFeedback: Flow<Boolean>

    /** Whether to automatically copy the scanned value to the system clipboard. */
    val autoCopyToClipboard: Flow<Boolean>

    /** Whether to perform an online product lookup for EAN / UPC barcodes. */
    val productLookup: Flow<Boolean>

    // ═════════════════════════════════════════════════════════════════════
    // Scanner settings — setters
    // ═════════════════════════════════════════════════════════════════════

    /** Persists the [enabled] state for continuous scan mode. */
    suspend fun setContinuousScan(enabled: Boolean)

    /** Persists the [enabled] state for duplicate handling. */
    suspend fun setHandleDuplicates(enabled: Boolean)

    /** Persists the [enabled] state for sound feedback. */
    suspend fun setSoundFeedback(enabled: Boolean)

    /** Persists the [enabled] state for vibration feedback. */
    suspend fun setVibrationFeedback(enabled: Boolean)

    /** Persists the [enabled] state for auto-copy to clipboard. */
    suspend fun setAutoCopyToClipboard(enabled: Boolean)

    /** Persists the [enabled] state for product lookup. */
    suspend fun setProductLookup(enabled: Boolean)
}
