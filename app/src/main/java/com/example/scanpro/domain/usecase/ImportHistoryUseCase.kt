package com.example.scanpro.domain.usecase

import com.example.scanpro.data.export.ExportManager
import com.example.scanpro.domain.model.toDomainModel
import com.example.scanpro.domain.repository.BarcodeRepository
import javax.inject.Inject

/**
 * Imports barcode history from a portable file format (CSV or JSON) into the
 * local database.
 *
 * The use-case mirrors [ExportHistoryUseCase] in reverse:
 * 1. Pass the raw file content (CSV or JSON string) to [ExportManager] for
 *    deserialization into [BarcodeEntity] objects.
 * 2. Convert each entity into a [BarcodeItem] domain model.
 * 3. Bulk-insert the domain models via the [BarcodeRepository].
 *
 * ## Import semantics
 * - Imported records are inserted **as-is**; no duplicate detection is performed
 *   during import. This matches user expectations — if you export and re-import,
 *   you get all your records back, even if some already exist.
 * - The [BarcodeItem.id] field on imported items is set to `0` by the entity →
 *   domain mapping so that Room auto-generates fresh primary keys, preventing
 *   conflicts with existing rows.
 *
 * ## Usage
 * ```kotlin
 * val count = importHistoryUseCase.importCsv(csvString)
 * val count = importHistoryUseCase.importJson(jsonString)
 * ```
 *
 * @property repository    Provides bulk-insert capability.
 * @property exportManager Handles deserialization of CSV / JSON content.
 */
class ImportHistoryUseCase @Inject constructor(
    private val repository: BarcodeRepository,
    private val exportManager: ExportManager
) {

    /**
     * Parses a **CSV** string and imports the resulting barcode records.
     *
     * @param content The full CSV text, including the header row.
     * @return The number of records successfully imported.
     */
    suspend fun importCsv(content: String): Int {
        // Deserialize CSV rows into persistence entities.
        val entities = exportManager.importFromCsv(content)

        // Convert entities to domain models (id is reset to 0 for fresh insertion).
        val domainItems = entities.map { it.toDomainModel().copy(id = 0) }

        // Bulk-insert into the database via the repository.
        repository.importBarcodes(domainItems)

        return domainItems.size
    }

    /**
     * Parses a **JSON** string and imports the resulting barcode records.
     *
     * @param content The full JSON text (expected to be a top-level array).
     * @return The number of records successfully imported.
     */
    suspend fun importJson(content: String): Int {
        // Deserialize JSON array into persistence entities.
        val entities = exportManager.importFromJson(content)

        // Convert entities to domain models (id is reset to 0 for fresh insertion).
        val domainItems = entities.map { it.toDomainModel().copy(id = 0) }

        // Bulk-insert into the database via the repository.
        repository.importBarcodes(domainItems)

        return domainItems.size
    }
}
