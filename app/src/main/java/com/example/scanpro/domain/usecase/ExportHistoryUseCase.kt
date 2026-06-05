package com.example.scanpro.domain.usecase

import com.example.scanpro.data.export.ExportManager
import com.example.scanpro.domain.model.toEntity
import com.example.scanpro.domain.repository.BarcodeRepository
import javax.inject.Inject

/**
 * Exports the user's barcode scan history to a portable file format (CSV or JSON).
 *
 * The use-case follows a two-step flow:
 * 1. Retrieve all persisted [BarcodeItem]s from the repository.
 * 2. Convert them to entity form and delegate serialization to [ExportManager],
 *    which returns the file path of the written export file.
 *
 * ## Why convert to entity before exporting?
 * The [ExportManager] operates on the **persistence schema** ([BarcodeEntity])
 * so that exported files mirror the database columns exactly. This means
 * re-importing the same file is lossless — column names, enum string values,
 * and timestamps all round-trip without transformation.
 *
 * ## Usage
 * ```kotlin
 * val csvPath = exportHistoryUseCase.exportCsv()
 * val jsonPath = exportHistoryUseCase.exportJson()
 * ```
 *
 * @property repository    Provides access to the barcode history.
 * @property exportManager Handles the actual file I/O and serialization.
 */
class ExportHistoryUseCase @Inject constructor(
    private val repository: BarcodeRepository,
    private val exportManager: ExportManager
) {

    /**
     * Exports the entire barcode history as a **CSV** file.
     *
     * The CSV uses a header row matching the [BarcodeEntity] field names, with
     * each subsequent row representing one barcode record.
     *
     * @return The absolute file path of the generated CSV file.
     */
    suspend fun exportCsv(): String {
        // Fetch all domain models, convert to entities for schema-aligned export.
        val entities = repository.getAllForExport().map { it.toEntity() }
        return exportManager.exportToCsv(entities)
    }

    /**
     * Exports the entire barcode history as a **JSON** file.
     *
     * The JSON structure is a top-level array of objects, each object mirroring
     * the [BarcodeEntity] fields.
     *
     * @return The absolute file path of the generated JSON file.
     */
    suspend fun exportJson(): String {
        // Fetch all domain models, convert to entities for schema-aligned export.
        val entities = repository.getAllForExport().map { it.toEntity() }
        return exportManager.exportToJson(entities)
    }
}
