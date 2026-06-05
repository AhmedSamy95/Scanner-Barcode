package com.example.scanpro.domain.model

import com.example.scanpro.data.local.db.BarcodeEntity

/**
 * Core domain model representing a single barcode record.
 *
 * [BarcodeItem] is the **single source of truth** for barcode data as it flows
 * through the domain and presentation layers. It is intentionally free of any
 * framework or persistence annotations so it can be unit-tested in isolation.
 *
 * Persistence mapping is handled by the [toEntity] and [toDomainModel]
 * extension functions defined at the bottom of this file, keeping the data
 * layer's [BarcodeEntity] out of the domain model's own API surface.
 *
 * @property id             Auto-generated primary key (0 for not-yet-persisted items).
 * @property rawValue       The unprocessed string extracted from the barcode.
 * @property displayValue   A human-friendly rendering of the barcode content. For most
 *                          formats this equals [rawValue]; for structured formats (vCard,
 *                          Wi-Fi, …) it may be a summarized or formatted version.
 * @property format         The barcode symbology (QR, EAN-13, Code 128, …).
 * @property contentType    The semantic type of the payload (URL, Wi-Fi, Contact, …).
 * @property timestamp      Unix epoch millis when the barcode was scanned or created.
 * @property isFavorite     Whether the user has marked this item as a favorite.
 * @property characterCount The number of characters in [rawValue].
 * @property encodingType   The text encoding used for the raw value (default UTF-8).
 * @property source         Origin of the record — `"SCAN"` for camera scans,
 *                          `"GENERATE"` for user-created barcodes, `"IMPORT"` for
 *                          imported records.
 */
data class BarcodeItem(
    val id: Long = 0,
    val rawValue: String,
    val displayValue: String,
    val format: BarcodeFormatType,
    val contentType: BarcodeContentType,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val characterCount: Int = rawValue.length,
    val encodingType: String = "UTF-8",
    val source: String = "SCAN"
)

// ═══════════════════════════════════════════════════════════════════════════
// Extension functions for mapping between the domain model and the Room entity.
// These live here (domain layer) rather than in the data layer so that:
//   • The domain model file is the authoritative place for all conversions.
//   • Use-cases can call them without importing data-layer internals beyond
//     the entity class itself.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Converts this [BarcodeItem] domain model into a [BarcodeEntity] suitable for
 * Room persistence.
 *
 * Enum values are stored as their [Enum.name] strings so the database remains
 * human-readable and is not coupled to enum ordinal positions.
 *
 * @return A [BarcodeEntity] mirroring the data in this domain model.
 */
fun BarcodeItem.toEntity(): BarcodeEntity {
    return BarcodeEntity(
        id = id,
        rawValue = rawValue,
        displayValue = displayValue,
        format = format.name,
        contentType = contentType.name,
        timestamp = timestamp,
        isFavorite = isFavorite,
        characterCount = characterCount,
        encodingType = encodingType,
        source = source
    )
}

/**
 * Converts this [BarcodeEntity] persistence object into a [BarcodeItem] domain
 * model.
 *
 * Enum fields are resolved via [enumValueOf]; if the stored string does not
 * match any constant (e.g. after a migration), the fallback is [BarcodeFormatType.UNKNOWN]
 * or [BarcodeContentType.UNKNOWN] respectively, so the app degrades gracefully.
 *
 * @return A [BarcodeItem] populated from this entity's fields.
 */
fun BarcodeEntity.toDomainModel(): BarcodeItem {
    return BarcodeItem(
        id = id,
        rawValue = rawValue,
        displayValue = displayValue,
        format = try {
            enumValueOf<BarcodeFormatType>(format)
        } catch (_: IllegalArgumentException) {
            BarcodeFormatType.UNKNOWN
        },
        contentType = try {
            enumValueOf<BarcodeContentType>(contentType)
        } catch (_: IllegalArgumentException) {
            BarcodeContentType.UNKNOWN
        },
        timestamp = timestamp,
        isFavorite = isFavorite,
        characterCount = characterCount,
        encodingType = encodingType,
        source = source
    )
}
