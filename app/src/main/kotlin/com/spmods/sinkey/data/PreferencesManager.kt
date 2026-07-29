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
        // Fancy-text style applied to committed English text — one of
        // FancyTextStyle.entries' .key values. Reusing the old preference
        // key name (keyboard_font) so existing users' stored default isn't
        // silently reset to NONE by this migration.
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

    /** Currently selected fancy-text style for English typing, defaulting to off (plain text). */
    val keyboardFont: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEYBOARD_FONT] ?: FancyTextStyle.NONE.key
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
 * "Fancy text" styles for English typing.
 *
 * IMPORTANT — what this actually does and why the old version didn't work:
 * A soft keyboard cannot force a custom *font* onto text typed into another
 * app. It can only send Unicode characters via commitText(); how those
 * characters are drawn is entirely up to the receiving app (WhatsApp,
 * Messages, etc.), which uses its own font/renderer. The previous
 * KeyboardFont implementation only fed a Compose FontFamily into
 * ProvideTextStyle around the keyboard's own key labels — so it changed how
 * the letters on the KEYS looked, but had no effect whatsoever on what was
 * actually committed to the input field. Nothing was wired into
 * commitText()/setComposingText() at all, so typed text always came out in
 * the receiving app's normal font.
 *
 * This replaces that with genuine Unicode character substitution: each
 * style maps plain a-z / A-Z / 0-9 to visually distinct Unicode code points
 * (𝓼𝓬𝓻𝓲𝓹𝓽, 𝗯𝗼𝗹𝗱, 𝕕𝕠𝕦𝕓𝕝𝕖-𝕤𝕥𝕣𝕦𝕔𝕜, etc.). Those ARE real characters, so
 * they render with the chosen look in *any* app, exactly as typed — because
 * the visual difference now lives in the character itself, not in a font
 * request the receiving app has no reason to honor.
 *
 * English only: Sinhala Unicode has no equivalent styled code-point block,
 * so applying this to Sinhala text isn't possible the same way. The style
 * is simply not applied while typing in Sinhala.
 */
enum class FancyTextStyle(val key: String, val label: String, val preview: String) {
    NONE("none", "Normal (off)", "Normal"),
    BOLD("bold", "𝗕𝗼𝗹𝗱", "𝗕𝗼𝗹𝗱"),
    ITALIC("italic", "𝘐𝘵𝘢𝘭𝘪𝘤", "𝘐𝘵𝘢𝘭𝘪𝘤"),
    BOLD_ITALIC("bold_italic", "𝙱𝚘𝚕𝚍 𝙸𝚝𝚊𝚕𝚒𝚌", "𝙱𝚘𝚕𝚍 𝙸𝚝𝚊𝚕𝚒𝚌"),
    SCRIPT("script", "𝓢𝓬𝓻𝓲𝓹𝓽", "𝓢𝓬𝓻𝓲𝓹𝓽"),
    DOUBLE_STRUCK("double_struck", "𝔻𝕠𝕦𝕓𝕝𝕖-𝕤𝕥𝕣𝕦𝕔𝕜", "𝔻𝕠𝕦𝕓𝕝𝕖-𝕤𝕥𝕣𝕦𝕔𝕜"),
    FRAKTUR("fraktur", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯"),
    MONOSPACE("monospace", "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎", "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎"),
    CIRCLED("circled", "Ⓒⓘⓡⓒ⓵ⓔⓓ", "Ⓒⓘⓡⓒ⓵ⓔⓓ"),
    SMALL_CAPS("small_caps", "sᴍᴀʟʟ ᴄᴀᴘs", "sᴍᴀʟʟ ᴄᴀᴘs"),
    UPSIDE_DOWN("upside_down", "ıɟlıpsn-uʍop", "ıɟlıpsn-uʍop");

    companion object {
        fun fromKey(key: String): FancyTextStyle = entries.find { it.key == key } ?: NONE
    }
}
