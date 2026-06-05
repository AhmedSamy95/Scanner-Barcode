package com.example.scanpro.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single barcode record stored in the local database.
 *
 * Each record captures the full lifecycle of a barcode interaction — whether it was
 * scanned from a physical code or generated within the app. The entity stores both
 * the machine-readable [rawValue] and a human-friendly [displayValue], along with
 * rich metadata such as format, content semantics, and encoding information.
 *
 * Table name: `barcodes`
 *
 * @property id             Auto-generated primary key. Pass `0` to let Room assign the next ID.
 * @property rawValue       The unprocessed payload extracted from the barcode (e.g. a full URL,
 *                          a WiFi config string, or plain text). This is the canonical value
 *                          used for duplicate detection.
 * @property displayValue   A user-facing representation of the barcode content. May be identical
 *                          to [rawValue] or a prettified / truncated version for UI display.
 * @property format         The symbology of the barcode, matching ML Kit's format constants
 *                          (e.g. `"QR_CODE"`, `"CODE_128"`, `"EAN_13"`, `"PDF_417"`).
 * @property contentType    Semantic classification of the payload content. Possible values
 *                          include `"URL"`, `"TEXT"`, `"WIFI"`, `"EMAIL"`, `"PHONE"`,
 *                          `"GEO"`, `"CONTACT"`, `"PRODUCT"`, etc.
 * @property timestamp      Unix epoch milliseconds when the barcode was first scanned or created.
 *                          Defaults to the current system time. Also updated on re-scan when
 *                          the "handle duplicates" setting is enabled.
 * @property isFavorite     Whether the user has starred / bookmarked this barcode for quick access.
 * @property characterCount The number of characters in [rawValue]. Pre-computed at insertion time
 *                          so list UIs can display length info without recalculation.
 * @property encodingType   The character encoding used to decode the barcode payload
 *                          (e.g. `"UTF-8"`, `"ISO-8859-1"`). Defaults to `"UTF-8"`.
 * @property source         Origin of the barcode record: `"SCAN"` if captured via the camera
 *                          scanner, or `"GENERATED"` if created through the app's barcode
 *                          generator feature.
 */
@Entity(tableName = "barcodes")
data class BarcodeEntity(

    /** Auto-generated primary key; pass `0` to let Room assign the next available ID. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Raw, unprocessed payload extracted directly from the barcode image. */
    @ColumnInfo(name = "raw_value")
    val rawValue: String,

    /** Human-friendly representation of the barcode content, suitable for UI display. */
    @ColumnInfo(name = "display_value")
    val displayValue: String,

    /**
     * Barcode symbology / format identifier.
     *
     * Matches the constant names used by Google ML Kit's `Barcode.FORMAT_*` family,
     * serialised as upper-case strings (e.g. `"QR_CODE"`, `"CODE_128"`, `"DATA_MATRIX"`).
     */
    @ColumnInfo(name = "format")
    val format: String,

    /**
     * Semantic type of the barcode payload.
     *
     * Indicates what kind of data the barcode encodes so the app can offer contextual
     * actions (e.g. open browser for `"URL"`, join network for `"WIFI"`).
     */
    @ColumnInfo(name = "content_type")
    val contentType: String,

    /**
     * Unix epoch timestamp (milliseconds) when the record was created or last re-scanned.
     *
     * Defaults to [System.currentTimeMillis] at construction time.
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /** `true` if the user has marked this barcode as a favourite. */
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    /** Pre-computed character count of [rawValue] for efficient UI rendering. */
    @ColumnInfo(name = "character_count")
    val characterCount: Int = 0,

    /** Character encoding used to interpret the barcode bytes (defaults to `"UTF-8"`). */
    @ColumnInfo(name = "encoding_type")
    val encodingType: String = "UTF-8",

    /**
     * Origin of the record.
     *
     * - `"SCAN"` — captured via the camera-based barcode scanner.
     * - `"GENERATED"` — created through the in-app barcode generator.
     */
    @ColumnInfo(name = "source")
    val source: String = "SCAN"
)
