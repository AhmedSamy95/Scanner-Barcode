package com.example.scanpro.data.export

import com.example.scanpro.data.local.db.BarcodeEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles serialisation and deserialisation of [BarcodeEntity] records to and from
 * portable file formats (CSV and JSON).
 *
 * ### Design decisions
 * - **CSV** is generated with RFC 4180-compliant quoting: every field is double-quoted
 *   and any embedded double-quote characters are escaped by doubling them (`""`).
 *   This guarantees correct round-tripping even when barcode payloads contain commas,
 *   newlines, or quote characters.
 * - **JSON** serialisation uses [Gson] for simplicity and compatibility with the
 *   Gson dependency already present in many Android projects. The output is a
 *   JSON array of objects whose keys match [BarcodeEntity] property names.
 * - On **import**, every record's [BarcodeEntity.id] is forced to `0` so that Room
 *   treats them as new inserts and auto-generates fresh primary keys. This prevents
 *   accidental overwrites of existing records when restoring a backup.
 *
 * ### Thread safety
 * All public functions are pure transformations (no I/O, no shared mutable state)
 * and are therefore safe to call from any thread or coroutine dispatcher.
 *
 * @property gson The [Gson] instance used for JSON serialisation / deserialisation.
 */
@Singleton
class ExportManager @Inject constructor() {

    /** Lazily initialised [Gson] instance shared across all JSON operations. */
    private val gson: Gson = Gson()

    // ─────────────────────────────────────────────────────────────────────────
    // CSV Export
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serialises a list of [BarcodeEntity] records into a CSV-formatted string.
     *
     * The first line is a header row whose column names match the database column
     * names for clarity. All field values are double-quoted to handle embedded
     * commas and newlines safely.
     *
     * Column order:
     * `id, raw_value, display_value, format, content_type, timestamp,
     *  is_favorite, character_count, encoding_type, source`
     *
     * @param records The barcode records to export. An empty list produces a
     *                string containing only the header row.
     * @return A complete CSV document as a [String], including a trailing newline.
     */
    fun exportToCsv(records: List<BarcodeEntity>): String {
        val builder = StringBuilder()

        // ── Header row ──────────────────────────────────────────────────────
        builder.appendLine(CSV_HEADER)

        // ── Data rows ───────────────────────────────────────────────────────
        for (record in records) {
            builder.appendLine(
                listOf(
                    record.id.toString(),
                    record.rawValue,
                    record.displayValue,
                    record.format,
                    record.contentType,
                    record.timestamp.toString(),
                    record.isFavorite.toString(),
                    record.characterCount.toString(),
                    record.encodingType,
                    record.source
                ).joinToString(separator = ",") { field -> escapeCsvField(field) }
            )
        }

        return builder.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON Export
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serialises a list of [BarcodeEntity] records into a JSON array string.
     *
     * Each element in the array is a JSON object whose keys correspond to
     * [BarcodeEntity] property names (camelCase). The output is **not**
     * pretty-printed to minimise file size; callers can pipe it through a
     * formatter if human readability is required.
     *
     * @param records The barcode records to export. An empty list produces `"[]"`.
     * @return A JSON array string representation of the records.
     */
    fun exportToJson(records: List<BarcodeEntity>): String {
        return gson.toJson(records)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV Import
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a CSV-formatted string back into a list of [BarcodeEntity] records.
     *
     * The first line is assumed to be a header row and is skipped. Subsequent
     * lines are split respecting quoted fields (RFC 4180), so embedded commas
     * and newlines within quoted values are handled correctly.
     *
     * Every imported record has its [BarcodeEntity.id] set to `0` so that Room
     * assigns a fresh auto-generated primary key on insertion.
     *
     * @param csvContent The raw CSV text, typically read from a file or clipboard.
     * @return A list of [BarcodeEntity] records ready for database insertion.
     *         Malformed lines are silently skipped to avoid crashing the import
     *         on minor data issues.
     */
    fun importFromCsv(csvContent: String): List<BarcodeEntity> {
        val results = mutableListOf<BarcodeEntity>()

        // Split into logical CSV records (respecting quoted newlines).
        val records = parseCsvRecords(csvContent)

        // Drop the header row.
        val dataRecords = if (records.isNotEmpty()) records.drop(1) else emptyList()

        for (record in dataRecords) {
            val fields = parseCsvFields(record)

            // We expect exactly 10 columns; skip malformed rows gracefully.
            if (fields.size < 10) continue

            try {
                results.add(
                    BarcodeEntity(
                        id = 0, // Force new insert — never reuse exported IDs.
                        rawValue = fields[1],
                        displayValue = fields[2],
                        format = fields[3],
                        contentType = fields[4],
                        timestamp = fields[5].toLongOrNull() ?: System.currentTimeMillis(),
                        isFavorite = fields[6].toBooleanStrictOrNull() ?: false,
                        characterCount = fields[7].toIntOrNull() ?: 0,
                        encodingType = fields[8],
                        source = fields[9]
                    )
                )
            } catch (_: Exception) {
                // Skip rows that cannot be parsed rather than aborting the whole import.
            }
        }

        return results
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON Import
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a JSON array string back into a list of [BarcodeEntity] records.
     *
     * Uses [Gson] with a [TypeToken] to deserialise the JSON array. Every
     * imported record has its [BarcodeEntity.id] reset to `0` so Room assigns
     * fresh primary keys.
     *
     * @param jsonContent The raw JSON text, expected to be a JSON array of
     *                    objects whose keys match [BarcodeEntity] property names.
     * @return A list of [BarcodeEntity] records ready for database insertion.
     * @throws com.google.gson.JsonSyntaxException if the input is not valid JSON.
     */
    fun importFromJson(jsonContent: String): List<BarcodeEntity> {
        val type = object : TypeToken<List<BarcodeEntity>>() {}.type
        val imported: List<BarcodeEntity> = gson.fromJson(jsonContent, type)

        // Zero out IDs so Room auto-generates new primary keys on insert.
        return imported.map { entity -> entity.copy(id = 0) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV Helpers (private)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps a single field value in double quotes and escapes any embedded
     * double-quote characters by doubling them, per RFC 4180.
     *
     * Example: `He said "hello"` → `"He said ""hello"""`
     *
     * @param field The raw field value.
     * @return The escaped and quoted field, safe for inclusion in a CSV row.
     */
    private fun escapeCsvField(field: String): String {
        val escaped = field.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Splits raw CSV text into logical records (lines), correctly handling
     * newlines that appear inside quoted fields.
     *
     * Standard [String.lines] would incorrectly break a quoted value containing
     * `\n` into multiple records. This function uses a simple state machine to
     * track whether the parser is currently inside a quoted field.
     *
     * @param csvContent The full CSV document text.
     * @return A list of logical record strings (one per CSV row).
     */
    private fun parseCsvRecords(csvContent: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (char in csvContent) {
            when {
                char == '"' -> {
                    insideQuotes = !insideQuotes
                    current.append(char)
                }
                (char == '\n' || char == '\r') && !insideQuotes -> {
                    // End of a logical record (ignore bare \r from \r\n).
                    if (current.isNotBlank()) {
                        records.add(current.toString().trim())
                    }
                    current.clear()
                }
                else -> current.append(char)
            }
        }

        // Capture any trailing record that doesn't end with a newline.
        if (current.isNotBlank()) {
            records.add(current.toString().trim())
        }

        return records
    }

    /**
     * Splits a single CSV record string into its constituent field values,
     * correctly handling quoted fields that may contain commas.
     *
     * Escaped double quotes (`""`) inside a quoted field are unescaped back
     * to a single `"`.
     *
     * @param record A single logical CSV row (no leading/trailing newlines).
     * @return An ordered list of unescaped field values.
     */
    private fun parseCsvFields(record: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < record.length) {
            val char = record[i]

            when {
                // Toggle quote state or handle escaped quotes ("").
                char == '"' -> {
                    if (insideQuotes && i + 1 < record.length && record[i + 1] == '"') {
                        // Escaped double-quote → emit a literal quote character.
                        current.append('"')
                        i++ // Skip the second quote.
                    } else {
                        // Toggle quoted-field state (opening or closing quote).
                        insideQuotes = !insideQuotes
                    }
                }
                // Field delimiter — but only when outside quotes.
                char == ',' && !insideQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                // Regular character — always append.
                else -> current.append(char)
            }

            i++
        }

        // Capture the last field (after the final comma).
        fields.add(current.toString())

        return fields
    }

    companion object {

        /**
         * CSV header row matching the column order used in [exportToCsv].
         *
         * Keeping this as a constant ensures export and import always agree on
         * column positions, even if the [BarcodeEntity] property order changes.
         */
        private const val CSV_HEADER =
            "\"id\",\"raw_value\",\"display_value\",\"format\"," +
            "\"content_type\",\"timestamp\",\"is_favorite\"," +
            "\"character_count\",\"encoding_type\",\"source\""
    }
}
