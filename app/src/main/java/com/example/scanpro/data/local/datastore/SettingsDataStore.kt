package com.example.scanpro.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Type-safe wrapper around Jetpack [DataStore]<[Preferences]> for all user-facing settings.
 *
 * Each setting is exposed as a [Flow]<[Boolean]> so the UI layer can collect changes
 * reactively, and a corresponding `suspend` setter that performs an atomic read-modify-write
 * transaction via [DataStore.edit].
 *
 * ### Threading
 * All reads are cold [Flow]s that execute on the DataStore's internal IO dispatcher.
 * Writes (`set*` functions) are `suspend` and should be called from a coroutine scope
 * (e.g. `viewModelScope`). They are safe to call from any dispatcher.
 *
 * ### Dependency injection
 * The [DataStore]<[Preferences]> instance is provided by a Hilt `@Module` — typically
 * created with `preferencesDataStore(name = "settings")` at the top of the Application
 * file or inside a provider function. This class simply consumes the injected instance.
 *
 * @param dataStore The Jetpack [DataStore] instance used for persisting preferences.
 *
 * @see [Companion] for the preference key definitions and their default values.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Continuous Scan
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether the scanner should remain active and keep detecting barcodes
     * after a successful scan, rather than pausing after each result.
     *
     * - `true`  → the camera feed stays live and subsequent barcodes are
     *              appended to the history automatically.
     * - `false` → (default) scanning pauses after each detection so the user
     *              can review the result before continuing.
     */
    val continuousScan: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CONTINUOUS_SCAN] ?: DEFAULT_CONTINUOUS_SCAN
    }

    /**
     * Persists the [continuousScan] preference.
     *
     * @param enabled `true` to enable continuous scanning mode.
     */
    suspend fun setContinuousScan(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CONTINUOUS_SCAN] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handle Duplicates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Controls how the app handles scanning a barcode whose [BarcodeEntity.rawValue]
     * already exists in the database.
     *
     * - `true`  → (default) the existing record's timestamp is updated to "now",
     *              effectively bumping it to the top of the history list.
     * - `false` → a brand-new row is inserted, allowing true duplicate entries.
     */
    val handleDuplicates: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HANDLE_DUPLICATES] ?: DEFAULT_HANDLE_DUPLICATES
    }

    /**
     * Persists the [handleDuplicates] preference.
     *
     * @param enabled `true` to update the existing record's timestamp on re-scan;
     *                `false` to always insert a new row.
     */
    suspend fun setHandleDuplicates(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HANDLE_DUPLICATES] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sound Feedback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether an audible "beep" is played upon a successful barcode detection.
     *
     * Defaults to `true`.
     */
    val soundFeedback: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SOUND_FEEDBACK] ?: DEFAULT_SOUND_FEEDBACK
    }

    /**
     * Persists the [soundFeedback] preference.
     *
     * @param enabled `true` to play a confirmation sound after each scan.
     */
    suspend fun setSoundFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_FEEDBACK] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vibration Feedback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether the device vibrates upon a successful barcode detection.
     *
     * Defaults to `true`.
     */
    val vibrationFeedback: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_VIBRATION_FEEDBACK] ?: DEFAULT_VIBRATION_FEEDBACK
    }

    /**
     * Persists the [vibrationFeedback] preference.
     *
     * @param enabled `true` to trigger a haptic pulse after each scan.
     */
    suspend fun setVibrationFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_VIBRATION_FEEDBACK] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auto-Copy to Clipboard
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether the scanned barcode's raw value is automatically copied to the
     * system clipboard immediately after detection.
     *
     * Defaults to `false` to avoid overwriting the user's clipboard unexpectedly.
     */
    val autoCopyToClipboard: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTO_COPY] ?: DEFAULT_AUTO_COPY
    }

    /**
     * Persists the [autoCopyToClipboard] preference.
     *
     * @param enabled `true` to auto-copy scanned values to the clipboard.
     */
    suspend fun setAutoCopyToClipboard(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_COPY] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Product Lookup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether the app should attempt an online product lookup when a product
     * barcode (EAN-13, UPC-A, etc.) is detected.
     *
     * Defaults to `false` because it requires an active internet connection and
     * may incur data usage.
     */
    val productLookup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PRODUCT_LOOKUP] ?: DEFAULT_PRODUCT_LOOKUP
    }

    /**
     * Persists the [productLookup] preference.
     *
     * @param enabled `true` to enable automatic online product lookups.
     */
    suspend fun setProductLookup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PRODUCT_LOOKUP] = enabled
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preference keys & defaults
    // ─────────────────────────────────────────────────────────────────────────

    companion object {

        // ── Keys ────────────────────────────────────────────────────────────

        /** DataStore key for the continuous-scan toggle. */
        private val KEY_CONTINUOUS_SCAN = booleanPreferencesKey("continuous_scan")

        /** DataStore key for the duplicate-handling toggle. */
        private val KEY_HANDLE_DUPLICATES = booleanPreferencesKey("handle_duplicates")

        /** DataStore key for the sound-feedback toggle. */
        private val KEY_SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")

        /** DataStore key for the vibration-feedback toggle. */
        private val KEY_VIBRATION_FEEDBACK = booleanPreferencesKey("vibration_feedback")

        /** DataStore key for the auto-copy-to-clipboard toggle. */
        private val KEY_AUTO_COPY = booleanPreferencesKey("auto_copy_to_clipboard")

        /** DataStore key for the product-lookup toggle. */
        private val KEY_PRODUCT_LOOKUP = booleanPreferencesKey("product_lookup")

        // ── Defaults ────────────────────────────────────────────────────────

        /** Default value for [continuousScan]: scanning pauses after each detection. */
        const val DEFAULT_CONTINUOUS_SCAN = false

        /** Default value for [handleDuplicates]: existing record's timestamp is updated. */
        const val DEFAULT_HANDLE_DUPLICATES = true

        /** Default value for [soundFeedback]: audible confirmation is enabled. */
        const val DEFAULT_SOUND_FEEDBACK = true

        /** Default value for [vibrationFeedback]: haptic confirmation is enabled. */
        const val DEFAULT_VIBRATION_FEEDBACK = true

        /** Default value for [autoCopyToClipboard]: clipboard copy is disabled. */
        const val DEFAULT_AUTO_COPY = false

        /** Default value for [productLookup]: online lookup is disabled. */
        const val DEFAULT_PRODUCT_LOOKUP = false
    }
}
