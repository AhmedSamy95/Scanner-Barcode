package com.example.scanpro.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.scanpro.domain.model.BarcodeFormatType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import javax.inject.Inject

/**
 * Generates a barcode image ([Bitmap]) from textual content and a target format.
 *
 * Under the hood this use-case delegates to ZXing's [MultiFormatWriter], which
 * supports both 1-D and 2-D symbologies. The resulting [BitMatrix] is manually
 * converted to an Android [Bitmap] pixel by pixel.
 *
 * ## Design decisions
 * - **1-D height override**: Linear barcodes look awkward when rendered as
 *   squares, so the height is clamped to 200 px regardless of the caller's
 *   requested dimensions.
 * - **Graceful failure**: If ZXing cannot encode the content (e.g. non-numeric
 *   input for EAN-13), the function returns `null` instead of throwing, so the
 *   ViewModel can show an error message without needing a try/catch.
 *
 * ## Usage
 * ```kotlin
 * val bitmap = generateBarcodeUseCase("https://example.com", BarcodeFormatType.QR_CODE)
 * ```
 *
 * @constructor Injected by Hilt — no runtime dependencies are needed.
 */
class GenerateBarcodeUseCase @Inject constructor() {

    /** Default width for generated barcode images, in pixels. */
    companion object {
        private const val DEFAULT_WIDTH = 800
        private const val DEFAULT_HEIGHT = 800
        private const val LINEAR_BARCODE_HEIGHT = 200
    }

    /**
     * Encodes [content] into a barcode [Bitmap] of the specified [format].
     *
     * @param content The textual payload to encode (URL, number, free text, …).
     * @param format  The target symbology. Must not be [BarcodeFormatType.UNKNOWN].
     * @param width   Desired image width in pixels (default [DEFAULT_WIDTH]).
     * @param height  Desired image height in pixels (default [DEFAULT_HEIGHT]).
     *                Ignored for 1-D formats, which always use [LINEAR_BARCODE_HEIGHT].
     * @return A [Bitmap] containing the barcode, or `null` if encoding failed
     *         (invalid content for the chosen format, unsupported characters, etc.).
     */
    operator fun invoke(
        content: String,
        format: BarcodeFormatType,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap? {
        return try {
            // ── 1. Resolve the ZXing format enum ─────────────────────────
            val zxingFormat = BarcodeFormatType.toZxingFormat(format)

            // ── 2. Adjust height for 1-D symbologies ─────────────────────
            // Linear barcodes encode data only along the horizontal axis;
            // a tall image just wastes space and looks wrong.
            val effectiveHeight = if (format.is2D) height else LINEAR_BARCODE_HEIGHT

            // ── 3. Encode to a BitMatrix via ZXing ───────────────────────
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                zxingFormat,
                width,
                effectiveHeight
            )

            // ── 4. Convert BitMatrix → Bitmap ───────────────────────────
            bitMatrixToBitmap(bitMatrix)
        } catch (e: WriterException) {
            // ZXing could not encode the content (wrong charset, invalid
            // length for format, etc.). Return null so callers can react.
            null
        } catch (e: IllegalArgumentException) {
            // Thrown if format is UNKNOWN (caught by toZxingFormat).
            null
        }
    }

    /**
     * Converts a ZXing [BitMatrix] into an Android [Bitmap].
     *
     * Each cell in the matrix is mapped to a single pixel: `true` → black,
     * `false` → white. The resulting bitmap uses [Bitmap.Config.RGB_565] for
     * a smaller memory footprint (barcode images are strictly two-tone).
     *
     * @param matrix The encoded bit matrix from [MultiFormatWriter].
     * @return A black-and-white [Bitmap] of the same dimensions.
     */
    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height

        // Pre-allocate a pixel array to avoid per-pixel Bitmap.setPixel calls,
        // which would be significantly slower due to JNI overhead.
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
