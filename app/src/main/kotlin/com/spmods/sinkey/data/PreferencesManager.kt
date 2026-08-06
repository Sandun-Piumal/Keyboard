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
        val DEFAULT_LANG = stringPreferencesKey("default_lang") // "si", "en", or "mix"
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
        // Mix mode only: when true, finishing a word (space/enter) converts the
        // typed Latin buffer to its Sinhala transliteration, same as pure "si"
        // mode. Default OFF — mix mode commits the word as plain typed text.
        val MIX_AUTO_SINHALA = booleanPreferencesKey("mix_auto_sinhala")
        // Gesture typing (swipe across letters instead of tapping each one)
        // — off by default so existing users' typing behavior doesn't
        // silently change; see GestureWordMatcher for the recognition side.
        val SWIPE_TYPING_ENABLED = booleanPreferencesKey("swipe_typing_enabled")
        // Internal, not user-facing: whether DictionarySeeder has already
        // loaded the bundled base word lists into the words table. Not a
        // simple "run once at install" flag — it's re-checked (cheaply) on
        // every app start via WordRepository.seedBaseDictionaryIfNeeded()
        // so a future app update that ships a larger word list can bump
        // DictionarySeeder.SEED_VERSION to re-seed without wiping learned
        // words (seedWord's OR IGNORE never touches an existing row).
        val DICTIONARY_SEED_VERSION = androidx.datastore.preferences.core.intPreferencesKey("dictionary_seed_version")

        // Themes screen: which KeyColorPalette accent is applied to keys —
        // one of KeyColorPalette.entries' .key values. DEFAULT keeps the
        // existing look (no accent tint) so this is purely additive for
        // existing users.
        val KEY_COLOR_PALETTE = stringPreferencesKey("key_color_palette")
        // Themes screen: which KeyEffect border/glow style is drawn on keys —
        // one of KeyEffect.entries' .key values. NONE keeps the existing
        // flat look.
        val KEY_EFFECT = stringPreferencesKey("key_effect")
        // "My themes" — a user-picked photo (persisted via
        // takePersistableUriPermission so it survives reboots) shown as the
        // keyboard's background. Null/blank = no custom background, fall
        // back to the normal solid keyboardColors().bg.
        val CUSTOM_BACKGROUND_URI = stringPreferencesKey("custom_background_uri")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val defaultLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_LANG] ?: "mix"
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

    /** Mix mode: auto-convert the typed word to Sinhala on space/enter. Default off. */
    val mixAutoSinhala: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MIX_AUTO_SINHALA] ?: false
    }

    /** Gesture (swipe-to-type) typing toggle — default off. */
    val swipeTypingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SWIPE_TYPING_ENABLED] ?: false
    }

    /** Current dictionary-seed version already applied on this device; 0 if never seeded. */
    val dictionarySeedVersion: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DICTIONARY_SEED_VERSION] ?: 0
    }

    /** Emits the most-recently-used emojis list (up to [MAX_RECENT] entries). */
    val recentEmojis: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_EMOJIS] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").filter { it.isNotBlank() }
    }

    /** Currently selected key-color palette, defaulting to DEFAULT (no tint change). */
    val keyColorPalette: Flow<KeyColorPalette> = context.dataStore.data.map { prefs ->
        KeyColorPalette.fromKey(prefs[Keys.KEY_COLOR_PALETTE] ?: KeyColorPalette.DEFAULT.key)
    }

    /** Currently selected key border/glow effect, defaulting to NONE. */
    val keyEffect: Flow<KeyEffect> = context.dataStore.data.map { prefs ->
        KeyEffect.fromKey(prefs[Keys.KEY_EFFECT] ?: KeyEffect.NONE.key)
    }

    /**
     * content:// Uri (as a string) of the user's chosen "My themes" photo
     * background, or null if none is set / the built-in solid background
     * should be used instead.
     */
    val customBackgroundUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_BACKGROUND_URI]?.takeIf { it.isNotBlank() }
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

    suspend fun setMixAutoSinhala(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MIX_AUTO_SINHALA] = enabled }
    }

    suspend fun setSwipeTypingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_TYPING_ENABLED] = enabled }
    }

    suspend fun setDictionarySeedVersion(version: Int) {
        context.dataStore.edit { it[Keys.DICTIONARY_SEED_VERSION] = version }
    }

    suspend fun setKeyColorPalette(palette: KeyColorPalette) {
        context.dataStore.edit { it[Keys.KEY_COLOR_PALETTE] = palette.key }
    }

    suspend fun setKeyEffect(effect: KeyEffect) {
        context.dataStore.edit { it[Keys.KEY_EFFECT] = effect.key }
    }

    /**
     * Sets (or clears, when [uri] is null) the "My themes" custom photo
     * background. Persisting the read permission is the caller's
     * responsibility (see ThemesScreen's photo picker launcher) — this just
     * stores the string, so a Uri without a persisted permission would
     * fail to load on the next app/keyboard restart.
     */
    suspend fun setCustomBackgroundUri(uri: String?) {
        context.dataStore.edit { it[Keys.CUSTOM_BACKGROUND_URI] = uri ?: "" }
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

/**
 * Themes screen "Colors" section — an accent color applied to the keyboard
 * (space bar / special-key tint and the Themes card's own accent dot),
 * layered on top of the existing light/dark KeyboardColors rather than
 * replacing it. DEFAULT reproduces the app's original look exactly (no
 * accent override), so switching to this system doesn't change anything
 * for users who never open the new Colors picker.
 *
 * [accent] is a single representative color used both for the small dot in
 * the Themes screen's preview card and as the tint mixed into the
 * keyboard's special/space key backgrounds — see
 * KeyboardView.applyPaletteAccent().
 */
enum class KeyColorPalette(val key: String, val label: String, val accent: androidx.compose.ui.graphics.Color) {
    DEFAULT("default", "Default", androidx.compose.ui.graphics.Color(0xFF6E9A65)),
    BLUE("blue", "Blue", androidx.compose.ui.graphics.Color(0xFF3B6FE0)),
    RED("red", "Red", androidx.compose.ui.graphics.Color(0xFFE05252)),
    GREEN("green", "Green", androidx.compose.ui.graphics.Color(0xFF3FAE6B)),
    CYAN("cyan", "Cyan", androidx.compose.ui.graphics.Color(0xFF33C2C2)),
    PURPLE("purple", "Purple", androidx.compose.ui.graphics.Color(0xFF8A5FE0)),
    AMBER("amber", "Amber", androidx.compose.ui.graphics.Color(0xFFE0A23B)),
    // New palettes (see ThemesScreen "Colors" section) — same pattern as
    // above: a single representative accent color, purely additive.
    CYBERPUNK("cyberpunk", "Cyberpunk", androidx.compose.ui.graphics.Color(0xFFFF2E9A)),
    SUNSET("sunset", "Sunset", androidx.compose.ui.graphics.Color(0xFFFF7A45)),
    OCEAN("ocean", "Ocean", androidx.compose.ui.graphics.Color(0xFF1E9FD6)),
    FOREST("forest", "Forest", androidx.compose.ui.graphics.Color(0xFF2E7D4F)),
    ROYAL_PURPLE("royal_purple", "Royal Purple", androidx.compose.ui.graphics.Color(0xFF5E35B1));

    companion object {
        fun fromKey(key: String): KeyColorPalette = entries.find { it.key == key } ?: DEFAULT
    }
}

/**
 * Themes screen "Effects" section — how each key's border/edge is drawn.
 * Purely a rendering choice layered onto whatever KeyboardColors.keyBg
 * already is; doesn't change key colors, sizing, or layout. NONE reproduces
 * the app's original flat-background look.
 */
enum class KeyEffect(val key: String, val label: String) {
    /** Flat key background only, no extra border/glow — original look. */
    NONE("none", "None"),
    /** Thin solid border in the palette accent color around every key. */
    OUTLINE("outline", "Outline"),
    /** Soft colored glow (blurred shadow) behind every key in the accent color. */
    GLOW("glow", "Glow"),
    /** Bottom-edge-only accent underline, like a subtle key "shadow" cue. */
    UNDERLINE("underline", "Underline"),
    /** Expanding ripple circle from the touch point, on key press. */
    RIPPLE("ripple", "Ripple"),
    /** Key briefly scales up on press, like a soft "pop". */
    POP_SCALE("pop_scale", "Pop"),
    /** Bottom-edge drop shadow giving keys a raised/3D button look. */
    SHADOW_3D("shadow_3d", "3D Shadow"),
    /** Soft glow that continuously pulses in/out, not just on press. */
    NEON_PULSE("neon_pulse", "Neon Pulse"),
    /** Key border cycles through a loop of accent hues — RGB/cyberpunk look. */
    RGB_CYCLE("rgb_cycle", "RGB Cycle"),
    /**
     * Touching any key sends an expanding neon/RGB color wave outward to
     * every other key on the board, based on distance from the touched
     * key — fades out as it travels and over time. Purely touch-reactive;
     * keys show no color at rest, unlike RGB_CYCLE's always-on animation.
     */
    RGB_RIPPLE("rgb_ripple", "RGB Ripple");

    companion object {
        fun fromKey(key: String): KeyEffect = entries.find { it.key == key } ?: NONE
    }
}
