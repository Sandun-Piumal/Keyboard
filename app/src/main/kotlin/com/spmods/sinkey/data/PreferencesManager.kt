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
        const val MAX_RECENT = 20 // LazyRow allows unlimited scroll — keep up to 30 recent emojis
    }
}
