package com.example.scanpro.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the `barcodes` table.
 *
 * Provides a type-safe, coroutine-friendly API for all barcode CRUD operations.
 * Reactive read queries return [Flow] so that the UI layer automatically receives
 * updates whenever the underlying data changes (e.g. a new scan is inserted or a
 * record is favourited). One-shot reads and all write operations are `suspend`
 * functions designed to be called from a coroutine scope (typically `viewModelScope`
 * or a repository's `IO` dispatcher).
 *
 * ### Query design notes
 * - **Sorting:** All list queries default to reverse-chronological order
 *   (`ORDER BY timestamp DESC`) so the most recent barcodes appear first.
 * - **Search:** The [search] query performs a case-insensitive `LIKE` match
 *   against both [BarcodeEntity.rawValue] and [BarcodeEntity.displayValue],
 *   covering scenarios where the display value has been prettified.
 * - **Conflict strategy:** [Insert] operations use [OnConflictStrategy.REPLACE]
 *   so that re-scanning an existing barcode (matched by primary key) silently
 *   updates the row rather than throwing a constraint violation.
 */
@Dao
interface BarcodeDao {

    // ──────────────────────────────────────────────────────────────────────────
    // Reactive read queries (return Flow for automatic UI updates)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Observes **all** barcode records, ordered newest-first.
     *
     * The returned [Flow] emits a fresh list every time the `barcodes` table
     * is modified, making it ideal for powering a history list in the UI.
     *
     * @return A [Flow] emitting the full list of [BarcodeEntity] records,
     *         sorted by [BarcodeEntity.timestamp] descending.
     */
    @Query("SELECT * FROM barcodes ORDER BY timestamp DESC")
    fun getAllSorted(): Flow<List<BarcodeEntity>>

    /**
     * Observes only the barcode records that the user has marked as favourites.
     *
     * @return A [Flow] emitting favourite [BarcodeEntity] records,
     *         sorted by [BarcodeEntity.timestamp] descending.
     */
    @Query("SELECT * FROM barcodes WHERE is_favorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<BarcodeEntity>>

    /**
     * Performs a free-text search across [BarcodeEntity.rawValue] and
     * [BarcodeEntity.displayValue].
     *
     * The search is case-insensitive (SQLite's default `LIKE` behaviour) and
     * matches any substring. For example, searching `"http"` will match a
     * barcode whose raw value is `"https://example.com"`.
     *
     * @param query The search term to match against raw and display values.
     * @return A [Flow] emitting matching [BarcodeEntity] records,
     *         sorted by [BarcodeEntity.timestamp] descending.
     */
    @Query(
        "SELECT * FROM barcodes " +
        "WHERE raw_value LIKE '%' || :query || '%' " +
        "   OR display_value LIKE '%' || :query || '%' " +
        "ORDER BY timestamp DESC"
    )
    fun search(query: String): Flow<List<BarcodeEntity>>

    // ──────────────────────────────────────────────────────────────────────────
    // One-shot read queries (suspend — called from coroutines)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Looks up a barcode by its [BarcodeEntity.rawValue].
     *
     * Used during scanning to detect duplicates: if a record with the same raw
     * payload already exists, the app can update its timestamp instead of
     * creating a new row (depending on the user's "handle duplicates" setting).
     *
     * @param rawValue The exact raw payload to search for.
     * @return The matching [BarcodeEntity], or `null` if no match is found.
     */
    @Query("SELECT * FROM barcodes WHERE raw_value = :rawValue LIMIT 1")
    suspend fun getByRawValue(rawValue: String): BarcodeEntity?

    /**
     * Retrieves a single barcode record by its primary key.
     *
     * Typically used when navigating to a detail screen from a list item.
     *
     * @param id The auto-generated primary key of the barcode.
     * @return The matching [BarcodeEntity], or `null` if the ID does not exist.
     */
    @Query("SELECT * FROM barcodes WHERE id = :id")
    suspend fun getById(id: Long): BarcodeEntity?

    /**
     * Returns **all** barcode records as a plain list (non-reactive).
     *
     * Intended for bulk operations such as CSV / JSON export where a one-shot
     * snapshot of the data is sufficient and a continuous [Flow] is unnecessary.
     *
     * @return A list of every [BarcodeEntity] in the database, newest first.
     */
    @Query("SELECT * FROM barcodes ORDER BY timestamp DESC")
    suspend fun getAllForExport(): List<BarcodeEntity>

    // ──────────────────────────────────────────────────────────────────────────
    // Write operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a single barcode record, replacing any existing row with the
     * same primary key.
     *
     * @param barcode The [BarcodeEntity] to insert.
     * @return The SQLite `rowId` of the newly inserted (or replaced) row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(barcode: BarcodeEntity): Long

    /**
     * Bulk-inserts a list of barcode records, replacing any existing rows
     * with matching primary keys.
     *
     * Used by the import feature to restore a previously exported data set.
     *
     * @param barcodes The list of [BarcodeEntity] records to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(barcodes: List<BarcodeEntity>)

    /**
     * Updates an existing barcode record in-place.
     *
     * Common use cases include toggling [BarcodeEntity.isFavorite] or
     * refreshing [BarcodeEntity.timestamp] on a duplicate re-scan.
     *
     * @param barcode The [BarcodeEntity] with updated field values.
     *                The [BarcodeEntity.id] must match an existing row.
     */
    @Update
    suspend fun update(barcode: BarcodeEntity)

    /**
     * Deletes a single barcode record from the database.
     *
     * @param barcode The [BarcodeEntity] to remove. Matched by primary key.
     */
    @Delete
    suspend fun delete(barcode: BarcodeEntity)

    /**
     * Deletes **all** barcode records from the `barcodes` table.
     *
     * This is a destructive operation typically guarded by a user-facing
     * confirmation dialog. It does **not** reset the auto-increment counter.
     */
    @Query("DELETE FROM barcodes")
    suspend fun deleteAll()
}
