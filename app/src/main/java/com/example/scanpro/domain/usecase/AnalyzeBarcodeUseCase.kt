package com.example.scanpro.domain.usecase

import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.domain.model.BarcodeItem
import javax.inject.Inject

/**
 * Detailed analysis result produced by [AnalyzeBarcodeUseCase].
 *
 * This data class bundles several computed properties about a barcode's raw
 * content and metadata into a single, presentation-ready object. The ViewModel
 * can display these fields directly in a "Barcode Details" screen without
 * performing any additional logic.
 *
 * @property characterCount        Total number of characters in the raw value.
 * @property encodingType          Text encoding used for the raw payload (e.g. "UTF-8").
 * @property formatName            Human-readable name of the barcode symbology.
 * @property contentTypeName       Human-readable name of the content category.
 * @property isNumericOnly         `true` if the raw value contains only digits.
 * @property isAlphanumeric        `true` if the raw value contains only letters and digits.
 * @property hasSpecialCharacters  `true` if the raw value contains characters outside
 *                                 the alphanumeric range.
 * @property estimatedDataCapacity A qualitative label ("Low", "Medium", "High")
 *                                 indicating how much data the format can typically hold.
 * @property checksumValid         Result of a simple validation check. For product
 *                                 barcodes (EAN/UPC) this verifies the check digit;
 *                                 for other formats it defaults to `true`.
 */
data class BarcodeAnalysis(
    val characterCount: Int,
    val encodingType: String,
    val formatName: String,
    val contentTypeName: String,
    val isNumericOnly: Boolean,
    val isAlphanumeric: Boolean,
    val hasSpecialCharacters: Boolean,
    val estimatedDataCapacity: String,
    val checksumValid: Boolean
)

/**
 * Analyzes a [BarcodeItem] and produces a [BarcodeAnalysis] summary.
 *
 * This is a **pure, synchronous** use-case — it performs no I/O and has no
 * side effects. All computations are derived from the in-memory [BarcodeItem]
 * properties and string content.
 *
 * ## Analysis details
 *
 * | Property               | How it is computed                                          |
 * |------------------------|-------------------------------------------------------------|
 * | `isNumericOnly`        | `rawValue.all { it.isDigit() }`                             |
 * | `isAlphanumeric`       | `rawValue.all { it.isLetterOrDigit() }`                     |
 * | `hasSpecialCharacters` | Negation of `isAlphanumeric`                                |
 * | `estimatedDataCapacity`| Heuristic based on the symbology family                     |
 * | `checksumValid`        | Check-digit verification for EAN-13 / EAN-8 / UPC-A        |
 *
 * ## Usage
 * ```kotlin
 * val analysis = analyzeBarcodeUseCase(selectedItem)
 * ```
 *
 * @constructor Injected by Hilt — no runtime dependencies required.
 */
class AnalyzeBarcodeUseCase @Inject constructor() {

    /**
     * Performs a full analysis of the supplied [item].
     *
     * @param item The [BarcodeItem] to analyze.
     * @return A [BarcodeAnalysis] containing all computed properties.
     */
    operator fun invoke(item: BarcodeItem): BarcodeAnalysis {
        val raw = item.rawValue

        // ── Character classification ─────────────────────────────────────
        val isNumericOnly = raw.all { it.isDigit() }
        val isAlphanumeric = raw.all { it.isLetterOrDigit() }
        val hasSpecialCharacters = !isAlphanumeric

        // ── Data capacity heuristic ──────────────────────────────────────
        val estimatedDataCapacity = estimateCapacity(item.format)

        // ── Checksum validation ──────────────────────────────────────────
        val checksumValid = validateChecksum(raw, item.format)

        return BarcodeAnalysis(
            characterCount = item.characterCount,
            encodingType = item.encodingType,
            formatName = item.format.displayName,
            contentTypeName = item.contentType.displayName,
            isNumericOnly = isNumericOnly,
            isAlphanumeric = isAlphanumeric,
            hasSpecialCharacters = hasSpecialCharacters,
            estimatedDataCapacity = estimatedDataCapacity,
            checksumValid = checksumValid
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Returns a qualitative data-capacity label based on the barcode format.
     *
     * 2-D symbologies (QR, Data Matrix, …) can store significantly more data
     * than 1-D symbologies, so the capacity estimate is derived from the
     * [BarcodeFormatType.is2D] flag plus format-specific knowledge.
     *
     * @param format The barcode symbology to evaluate.
     * @return `"High"`, `"Medium"`, or `"Low"`.
     */
    private fun estimateCapacity(format: BarcodeFormatType): String {
        return when (format) {
            // 2-D formats with large payload capacity
            BarcodeFormatType.QR_CODE,
            BarcodeFormatType.DATA_MATRIX,
            BarcodeFormatType.PDF_417     -> "High"

            // 2-D but typically lower capacity than QR
            BarcodeFormatType.AZTEC       -> "Medium"

            // 1-D variable-length formats
            BarcodeFormatType.CODE_128,
            BarcodeFormatType.CODE_39,
            BarcodeFormatType.CODE_93     -> "Medium"

            // 1-D fixed-length product codes
            BarcodeFormatType.EAN_13,
            BarcodeFormatType.EAN_8,
            BarcodeFormatType.UPC_A,
            BarcodeFormatType.UPC_E,
            BarcodeFormatType.ITF,
            BarcodeFormatType.CODABAR     -> "Low"

            BarcodeFormatType.UNKNOWN     -> "Low"
        }
    }

    /**
     * Validates the check digit for product barcodes (EAN-13, EAN-8, UPC-A).
     *
     * The standard algorithm alternates multipliers of 1 and 3 across the
     * digits (excluding the last), sums the weighted values, and verifies
     * that `(10 - (sum % 10)) % 10` equals the final digit.
     *
     * For non-product formats, or if the raw value is not purely numeric /
     * of the expected length, the function returns `true` (assumed valid)
     * because no meaningful check can be performed.
     *
     * @param raw    The raw barcode string.
     * @param format The barcode symbology.
     * @return `true` if the checksum is valid or not applicable.
     */
    private fun validateChecksum(raw: String, format: BarcodeFormatType): Boolean {
        // Only validate formats that use the EAN/UPC check-digit algorithm.
        val expectedLengths = when (format) {
            BarcodeFormatType.EAN_13 -> listOf(13)
            BarcodeFormatType.EAN_8  -> listOf(8)
            BarcodeFormatType.UPC_A  -> listOf(12)
            BarcodeFormatType.UPC_E  -> listOf(8)
            else -> return true // No check-digit algorithm for this format.
        }

        // Guard: raw value must be all digits and of the right length.
        if (!raw.all { it.isDigit() } || raw.length !in expectedLengths) {
            return true // Cannot validate — assume valid.
        }

        // Standard EAN / UPC check-digit algorithm:
        // Sum = Σ digit[i] * weight[i]  where weight alternates 1, 3, 1, 3, …
        // Check digit = (10 - (Sum % 10)) % 10
        val digits = raw.map { it.digitToInt() }
        val payload = digits.dropLast(1)
        val checkDigit = digits.last()

        val sum = payload.mapIndexed { index, digit ->
            if (index % 2 == 0) digit * 1 else digit * 3
        }.sum()

        val expected = (10 - (sum % 10)) % 10
        return expected == checkDigit
    }
}
