package com.example.scanpro.domain.usecase

import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the persistence of a newly-scanned barcode.
 *
 * This use-case encapsulates the **scan-and-save** business rule:
 *
 * 1. Check the user's *Handle Duplicates* preference.
 * 2. If duplicate handling is enabled **and** a record with the same raw value
 *    already exists, update the existing record's timestamp (effectively
 *    "bumping" it to the top of the history list) instead of creating a
 *    duplicate row.
 * 3. Otherwise, insert a brand-new record.
 *
 * By isolating this logic in a use-case, the ViewModel remains thin and the
 * rule can be unit-tested independently of the Android framework.
 *
 * ## Usage
 * ```kotlin
 * val id = scanBarcodeUseCase(newItem)
 * ```
 *
 * @property repository The [BarcodeRepository] providing data access and settings.
 */
class ScanBarcodeUseCase @Inject constructor(
    private val repository: BarcodeRepository
) {

    /**
     * Persists the scanned [item], respecting the duplicate-handling preference.
     *
     * @param item The [BarcodeItem] built from the ML Kit scan result.
     * @return The database row ID — either the existing record's ID (if
     *         duplicates were merged) or the newly inserted row's ID.
     */
    suspend operator fun invoke(item: BarcodeItem): Long {
        // ── 1. Read the current duplicate-handling preference ────────────
        // We use `first()` to collect a single snapshot from the Flow rather
        // than observing it continuously — this is a one-shot operation.
        val shouldHandleDuplicates = repository.handleDuplicates.first()

        // ── 2. Check for an existing record with the same raw value ──────
        if (shouldHandleDuplicates) {
            val existing = repository.getByRawValue(item.rawValue)
            if (existing != null) {
                // Merge: keep the original ID and favorite status but refresh
                // the timestamp so the item appears at the top of the history.
                val updated = existing.copy(
                    timestamp = System.currentTimeMillis(),
                    displayValue = item.displayValue,
                    format = item.format,
                    contentType = item.contentType
                )
                repository.updateBarcode(updated)
                return existing.id
            }
        }

        // ── 3. No duplicate (or handling disabled) — insert a new row ────
        return repository.insertBarcode(item)
    }
}
