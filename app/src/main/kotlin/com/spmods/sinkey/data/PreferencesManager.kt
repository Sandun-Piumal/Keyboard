package com.spmods.sinkey.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sinkey_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Central, DataStore-backed store for every user-facing preference:
 * theme mode, default typing language, key sound, and vibration.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_LANG = stringPreferencesKey("default_lang") // "si" or "en"
        val KEY_SOUND = booleanPreferencesKey("key_sound")
        val KEY_VIBRATE = booleanPreferencesKey("key_vibrate")
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
}
