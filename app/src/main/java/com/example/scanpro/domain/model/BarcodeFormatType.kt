package com.example.scanpro.domain.model

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat

/**
 * Domain-level representation of barcode symbology formats.
 *
 * This enum acts as a **format abstraction layer** that decouples the rest of the
 * application from the concrete format constants used by ML Kit (scanning) and
 * ZXing (generation). Two companion-object helpers translate between the three
 * representations:
 *
 * ```
 *  ML Kit int  ──►  BarcodeFormatType  ──►  ZXing BarcodeFormat
 * ```
 *
 * @property displayName  A user-friendly label for UI display.
 * @property is2D         `true` for two-dimensional symbologies (QR, Data Matrix, …),
 *                        `false` for linear / one-dimensional ones (EAN, Code 128, …).
 */
enum class BarcodeFormatType(val displayName: String, val is2D: Boolean) {

    // ── 2-D symbologies ──────────────────────────────────────────────────
    QR_CODE("QR Code", true),
    DATA_MATRIX("Data Matrix", true),
    AZTEC("Aztec", true),
    PDF_417("PDF 417", true),

    // ── 1-D symbologies ──────────────────────────────────────────────────
    CODE_128("Code 128", false),
    CODE_39("Code 39", false),
    CODE_93("Code 93", false),
    EAN_13("EAN-13", false),
    EAN_8("EAN-8", false),
    UPC_A("UPC-A", false),
    UPC_E("UPC-E", false),
    ITF("ITF", false),
    CODABAR("Codabar", false),

    // ── Fallback ─────────────────────────────────────────────────────────
    UNKNOWN("Unknown", false);

    companion object {

        /**
         * Maps an ML Kit `Barcode.FORMAT_*` integer constant to the corresponding
         * [BarcodeFormatType].
         *
         * ML Kit reports the format of every detected barcode as one of the
         * `Barcode.FORMAT_*` integer constants defined in
         * `com.google.mlkit.vision.barcode.common.Barcode`. This function
         * translates that integer into our domain enum so the rest of the app
         * never needs to depend on ML Kit directly.
         *
         * @param format The ML Kit format constant (e.g. [Barcode.FORMAT_QR_CODE]).
         * @return The matching [BarcodeFormatType], or [UNKNOWN] if the constant
         *         is not recognized.
         */
        fun fromMlKitFormat(format: Int): BarcodeFormatType {
            return when (format) {
                Barcode.FORMAT_QR_CODE     -> QR_CODE
                Barcode.FORMAT_DATA_MATRIX -> DATA_MATRIX
                Barcode.FORMAT_AZTEC       -> AZTEC
                Barcode.FORMAT_PDF417      -> PDF_417
                Barcode.FORMAT_CODE_128    -> CODE_128
                Barcode.FORMAT_CODE_39     -> CODE_39
                Barcode.FORMAT_CODE_93     -> CODE_93
                Barcode.FORMAT_EAN_13      -> EAN_13
                Barcode.FORMAT_EAN_8       -> EAN_8
                Barcode.FORMAT_UPC_A       -> UPC_A
                Barcode.FORMAT_UPC_E       -> UPC_E
                Barcode.FORMAT_ITF         -> ITF
                Barcode.FORMAT_CODABAR     -> CODABAR
                else                       -> UNKNOWN
            }
        }

        /**
         * Maps a [BarcodeFormatType] to the equivalent ZXing [BarcodeFormat]
         * used by `MultiFormatWriter` for barcode generation.
         *
         * ZXing is the library we use to *generate* barcode images. This
         * function bridges our domain enum to ZXing's own format enum so the
         * generation use-case can stay agnostic of ZXing internals.
         *
         * @param type The domain format type to convert.
         * @return The corresponding [BarcodeFormat].
         * @throws IllegalArgumentException if [type] is [UNKNOWN], since we
         *         cannot generate a barcode without a concrete symbology.
         */
        fun toZxingFormat(type: BarcodeFormatType): BarcodeFormat {
            return when (type) {
                QR_CODE     -> BarcodeFormat.QR_CODE
                DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
                AZTEC       -> BarcodeFormat.AZTEC
                PDF_417     -> BarcodeFormat.PDF_417
                CODE_128    -> BarcodeFormat.CODE_128
                CODE_39     -> BarcodeFormat.CODE_39
                CODE_93     -> BarcodeFormat.CODE_93
                EAN_13      -> BarcodeFormat.EAN_13
                EAN_8       -> BarcodeFormat.EAN_8
                UPC_A       -> BarcodeFormat.UPC_A
                UPC_E       -> BarcodeFormat.UPC_E
                ITF          -> BarcodeFormat.ITF
                CODABAR     -> BarcodeFormat.CODABAR
                UNKNOWN     -> throw IllegalArgumentException(
                    "Cannot convert UNKNOWN format to a ZXing BarcodeFormat. " +
                    "Choose a concrete symbology before generating."
                )
            }
        }
    }
}
