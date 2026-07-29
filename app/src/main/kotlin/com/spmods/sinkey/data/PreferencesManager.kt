package com.spmods.sinkey.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sinkey_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Central, DataStore-backed store for every user-facing preference:
 * theme mode, default typing language, key sound, vibration, and recent emojis.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_LANG = stringPreferencesKey("default_lang") // "si" or "en"
        val KEY_SOUND = booleanPreferencesKey("key_sound")
        val KEY_VIBRATE = booleanPreferencesKey("key_vibrate")
        val RECENT_EMOJIS = stringPreferencesKey("recent_emojis") // comma-separated
        // Keyboard Height settings
        val KEYBOARD_HEIGHT = floatPreferencesKey("keyboard_height") // 0f=S, 1f=M, 2f=L, 3f=XL
        val BOTTOM_SPACE_ENABLED = booleanPreferencesKey("bottom_space_enabled")
        val BOTTOM_SPACE_SIZE = floatPreferencesKey("bottom_space_size") // 0f=S, 1f=M, 2f=L, 3f=XL
        val SHOW_KEY_BORDERS = booleanPreferencesKey("show_key_borders")
        // Composing/preview text font — one of KeyboardFont.entries' .key values.
        val KEYBOARD_FONT = stringPreferencesKey("keyboard_font")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val defaultLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_LANG] ?: "si"
    }

    val keySoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEY_SOUND] ?: true
    }

    val keyVibrateEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEY_VIBRATE] ?: false
    }

    // 0f=S, 1f=M, 2f=L, 3f=XL — default L (2f)
    val keyboardHeight: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEYBOARD_HEIGHT] ?: 2f
    }

    val bottomSpaceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BOTTOM_SPACE_ENABLED] ?: true
    }

    // 0f=S, 1f=M, 2f=L, 3f=XL — default S (0f)
    val bottomSpaceSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.BOTTOM_SPACE_SIZE] ?: 0f
    }

    val showKeyBorders: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_KEY_BORDERS] ?: true
    }

    /** Currently selected composing/preview font, defaulting to the system default. */
    val keyboardFont: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEYBOARD_FONT] ?: KeyboardFont.DEFAULT_REGULAR.key
    }

    /** Emits the most-recently-used emojis list (up to [MAX_RECENT] entries). */
    val recentEmojis: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_EMOJIS] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultLanguage(lang: String) {
        context.dataStore.edit { it[Keys.DEFAULT_LANG] = lang }
    }

    suspend fun setKeySoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEY_SOUND] = enabled }
    }

    suspend fun setKeyVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEY_VIBRATE] = enabled }
    }

    suspend fun setKeyboardHeight(value: Float) {
        context.dataStore.edit { it[Keys.KEYBOARD_HEIGHT] = value }
    }

    suspend fun setBottomSpaceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BOTTOM_SPACE_ENABLED] = enabled }
    }

    suspend fun setBottomSpaceSize(value: Float) {
        context.dataStore.edit { it[Keys.BOTTOM_SPACE_SIZE] = value }
    }

    suspend fun setShowKeyBorders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_KEY_BORDERS] = enabled }
    }

    suspend fun setKeyboardFont(fontKey: String) {
        context.dataStore.edit { it[Keys.KEYBOARD_FONT] = fontKey }
    }

    /**
     * Pushes [emoji] to the front of the recent-emojis list and persists it.
     * Duplicates are removed and the list is capped at [MAX_RECENT].
     */
    suspend fun addRecentEmoji(emoji: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_EMOJIS] ?: "")
                .split(",")
                .filter { it.isNotBlank() && it != emoji } // remove duplicate
            val updated = (listOf(emoji) + current).take(MAX_RECENT)
            prefs[Keys.RECENT_EMOJIS] = updated.joinToString(",")
        }
    }

    companion object {
        const val MAX_RECENT = 20 // LazyRow allows unlimited scroll — keep up to 20 recent emojis
    }
}

/**
 * The 10 built-in font options for the composing/preview text (Phase 1 —
 * system generic font families × weight, no bundled/downloaded font files).
 * [key] is the stable identifier persisted to DataStore; [label] is shown
 * in the Font board UI.
 *
 * A later phase can add user-downloaded fonts (e.g. fetched from a GitHub
 * repo of .ttf files) as additional entries that resolve to a custom
 * FontFamily loaded from a cached file instead of [genericFamily].
 */
enum class KeyboardFont(
    val key: String,
    val label: String,
    val genericFamily: androidx.compose.ui.text.font.FontFamily,
    val weight: androidx.compose.ui.text.font.FontWeight
) {
    DEFAULT_LIGHT("default_light", "Default Light",
        androidx.compose.ui.text.font.FontFamily.Default, androidx.compose.ui.text.font.FontWeight.Light),
    DEFAULT_REGULAR("default_regular", "Default",
        androidx.compose.ui.text.font.FontFamily.Default, androidx.compose.ui.text.font.FontWeight.Normal),
    DEFAULT_BOLD("default_bold", "Default Bold",
        androidx.compose.ui.text.font.FontFamily.Default, androidx.compose.ui.text.font.FontWeight.Bold),
    SANS_SERIF_LIGHT("sans_serif_light", "Sans-serif Light",
        androidx.compose.ui.text.font.FontFamily.SansSerif, androidx.compose.ui.text.font.FontWeight.Light),
    SANS_SERIF_BOLD("sans_serif_bold", "Sans-serif Bold",
        androidx.compose.ui.text.font.FontFamily.SansSerif, androidx.compose.ui.text.font.FontWeight.Bold),
    SERIF_REGULAR("serif_regular", "Serif",
        androidx.compose.ui.text.font.FontFamily.Serif, androidx.compose.ui.text.font.FontWeight.Normal),
    SERIF_BOLD("serif_bold", "Serif Bold",
        androidx.compose.ui.text.font.FontFamily.Serif, androidx.compose.ui.text.font.FontWeight.Bold),
    MONOSPACE_REGULAR("monospace_regular", "Monospace",
        androidx.compose.ui.text.font.FontFamily.Monospace, androidx.compose.ui.text.font.FontWeight.Normal),
    MONOSPACE_BOLD("monospace_bold", "Monospace Bold",
        androidx.compose.ui.text.font.FontFamily.Monospace, androidx.compose.ui.text.font.FontWeight.Bold),
    CURSIVE_REGULAR("cursive_regular", "Cursive",
        androidx.compose.ui.text.font.FontFamily.Cursive, androidx.compose.ui.text.font.FontWeight.Normal);

    companion object {
        fun fromKey(key: String): KeyboardFont = entries.find { it.key == key } ?: DEFAULT_REGULAR
    }
}
