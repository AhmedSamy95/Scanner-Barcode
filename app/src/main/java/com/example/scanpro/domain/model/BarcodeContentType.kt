package com.example.scanpro.domain.model

/**
 * Represents the semantic content type of a scanned or generated barcode.
 *
 * Barcode raw values can encode many different kinds of data — URLs, Wi-Fi credentials,
 * contact cards, plain text, etc. This enum classifies the payload so the UI and
 * action-handling layers can present context-appropriate options (e.g. "Open in browser"
 * for [URL], "Connect to network" for [WIFI]).
 *
 * @property displayName A human-readable label suitable for display in the UI.
 */
enum class BarcodeContentType(val displayName: String) {

    /** A web URL (http, https, or www prefix). */
    URL("URL"),

    /** Generic plain text that does not match any structured pattern. */
    TEXT("Plain Text"),

    /** A Wi-Fi network configuration string (WIFI: protocol). */
    WIFI("Wi-Fi Network"),

    /** A vCard contact record (BEGIN:VCARD). */
    CONTACT("Contact"),

    /** An email address or mailto: link. */
    EMAIL("Email"),

    /** A telephone number (tel: or digit pattern). */
    PHONE("Phone Number"),

    /** An SMS message (sms: or smsto: prefix). */
    SMS("SMS"),

    /** A geographic coordinate (geo: URI). */
    GEO("Geographic Location"),

    /** A product identifier such as EAN / UPC (8–14 digit numeric string). */
    PRODUCT("Product"),

    /** An iCalendar event (BEGIN:VEVENT). */
    CALENDAR_EVENT("Calendar Event"),

    /** Fallback when the content cannot be classified. */
    UNKNOWN("Unknown");

    companion object {

        /**
         * Infers the [BarcodeContentType] from a raw barcode string.
         *
         * The function applies a series of heuristic checks in priority order.
         * More specific prefixes (e.g. `WIFI:`, `BEGIN:VCARD`) are tested before
         * looser patterns (e.g. email via `@`). If no rule matches, [TEXT] is
         * returned rather than [UNKNOWN], because arbitrary text is still valid
         * content — [UNKNOWN] is reserved for truly unclassifiable data from
         * external sources.
         *
         * @param raw The raw string value extracted from the barcode.
         * @return The best-matching [BarcodeContentType].
         */
        fun fromRawValue(raw: String): BarcodeContentType {
            return when {
                // ── Structured prefixes (highest confidence) ──────────────
                raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("www.") -> URL
                raw.startsWith("WIFI:") -> WIFI
                raw.startsWith("BEGIN:VCARD") -> CONTACT
                raw.startsWith("BEGIN:VEVENT") -> CALENDAR_EVENT
                raw.startsWith("geo:") -> GEO
                raw.startsWith("smsto:") || raw.startsWith("sms:") -> SMS
                raw.startsWith("tel:") -> PHONE
                raw.startsWith("mailto:") -> EMAIL

                // ── Pattern-based heuristics (lower confidence) ──────────
                raw.contains("@") && raw.contains(".") -> EMAIL
                raw.matches(Regex("^[+]?[0-9\\s\\-()]{7,15}$")) -> PHONE
                raw.all { it.isDigit() } && raw.length in 8..14 -> PRODUCT

                // ── Default ──────────────────────────────────────────────
                else -> TEXT
            }
        }
    }
}
