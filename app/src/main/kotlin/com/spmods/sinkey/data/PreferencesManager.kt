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

        // Themes screen "Backgrounds" section: a built-in procedurally-drawn
        // background (solid/gradient/rainbow/galaxy/smoke/etc.) — one of
        // BackgroundStyle.entries' .key values. NONE keeps the existing flat
        // colors.bg look. Layered UNDER customBackgroundUri: if the user has
        // also picked a photo/GIF, the photo wins (see KeyboardView).
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        // Material You (Android 12+ dynamic color from system wallpaper)
        // applied to the keyboard itself, independent of the app UI's own
        // Material You usage (if any). Default off — purely opt-in since it
        // overrides whatever KeyColorPalette/BackgroundStyle is selected.
        val MATERIAL_YOU_ENABLED = booleanPreferencesKey("material_you_enabled")

        // Typing Animation: emoji/icon/custom-image pop-up shown near a key
        // as it's pressed. Off by default (NONE).
        val TYPING_ANIMATION = stringPreferencesKey("typing_animation")
        // DIY Animation: user-chosen emoji character used as the pop-up
        // when TYPING_ANIMATION == CUSTOM_EMOJI.
        val TYPING_ANIMATION_EMOJI = stringPreferencesKey("typing_animation_emoji")
        // DIY Animation: user-chosen cropped image (content:// Uri, persisted
        // permission) used as the pop-up when TYPING_ANIMATION == CUSTOM_IMAGE.
        val TYPING_ANIMATION_IMAGE_URI = stringPreferencesKey("typing_animation_image_uri")

        // LED / Neon lighting pattern driving the whole board's ambient glow
        // (distinct from the per-key KeyEffect above) — one of
        // LedPattern.entries' .key values. NONE = no ambient lighting layer.
        val LED_PATTERN = stringPreferencesKey("led_pattern")
        // Whether the LED pattern should dim automatically after a few
        // seconds of no keypresses ("idle dimming"), then brighten again on
        // the next touch. Only meaningful when LED_PATTERN != NONE.
        val LED_IDLE_DIMMING = booleanPreferencesKey("led_idle_dimming")

        // Smooth IME open/close transition (WindowInsetsAnimationCompat-
        // driven slide/fade instead of an abrupt show/hide). Default on —
        // this is a pure visual-polish improvement with no behavior change,
        // safe to ship enabled.
        val SMOOTH_IME_TRANSITION = booleanPreferencesKey("smooth_ime_transition")
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

    /** Currently selected built-in background style, defaulting to NONE (flat colors.bg). */
    val backgroundStyle: Flow<BackgroundStyle> = context.dataStore.data.map { prefs ->
        BackgroundStyle.fromKey(prefs[Keys.BACKGROUND_STYLE] ?: BackgroundStyle.NONE.key)
    }

    /** Whether Material You dynamic color is applied to the keyboard. Default off. */
    val materialYouEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MATERIAL_YOU_ENABLED] ?: false
    }

    /** Currently selected typing (keypress pop-up) animation, defaulting to NONE. */
    val typingAnimation: Flow<TypingAnimation> = context.dataStore.data.map { prefs ->
        TypingAnimation.fromKey(prefs[Keys.TYPING_ANIMATION] ?: TypingAnimation.NONE.key)
    }

    /** User-chosen emoji for the DIY "Custom emoji" typing animation. Defaults to a heart. */
    val typingAnimationEmoji: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TYPING_ANIMATION_EMOJI]?.takeIf { it.isNotBlank() } ?: "✨"
    }

    /** content:// Uri of the user's chosen DIY typing-animation image, or null if none set. */
    val typingAnimationImageUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TYPING_ANIMATION_IMAGE_URI]?.takeIf { it.isNotBlank() }
    }

    /** Currently selected ambient LED/neon lighting pattern, defaulting to NONE. */
    val ledPattern: Flow<LedPattern> = context.dataStore.data.map { prefs ->
        LedPattern.fromKey(prefs[Keys.LED_PATTERN] ?: LedPattern.NONE.key)
    }

    /** Whether the LED pattern dims after a period of inactivity. Default on. */
    val ledIdleDimming: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LED_IDLE_DIMMING] ?: true
    }

    /** Smooth IME show/hide transition toggle. Default on. */
    val smoothImeTransition: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SMOOTH_IME_TRANSITION] ?: true
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

    suspend fun setBackgroundStyle(style: BackgroundStyle) {
        context.dataStore.edit { it[Keys.BACKGROUND_STYLE] = style.key }
    }

    suspend fun setMaterialYouEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MATERIAL_YOU_ENABLED] = enabled }
    }

    suspend fun setTypingAnimation(animation: TypingAnimation) {
        context.dataStore.edit { it[Keys.TYPING_ANIMATION] = animation.key }
    }

    suspend fun setTypingAnimationEmoji(emoji: String) {
        context.dataStore.edit { it[Keys.TYPING_ANIMATION_EMOJI] = emoji }
    }

    suspend fun setTypingAnimationImageUri(uri: String?) {
        context.dataStore.edit { it[Keys.TYPING_ANIMATION_IMAGE_URI] = uri ?: "" }
    }

    suspend fun setLedPattern(pattern: LedPattern) {
        context.dataStore.edit { it[Keys.LED_PATTERN] = pattern.key }
    }

    suspend fun setLedIdleDimming(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LED_IDLE_DIMMING] = enabled }
    }

    suspend fun setSmoothImeTransition(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMOOTH_IME_TRANSITION] = enabled }
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
    /** Expanding ripple circle from the touch point, on key press. */
    RIPPLE("ripple", "Ripple"),
    /** Soft colored glow (blurred shadow) behind every key in the accent color. */
    GLOW("glow", "Mech Glow");

    companion object {
        fun fromKey(key: String): KeyEffect = entries.find { it.key == key } ?: NONE
    }
}

/**
 * "Typing Animation" — a small emoji/icon/image that pops up near (above)
 * a key the instant it's pressed, purely cosmetic feedback layered on top
 * of whatever KeyEffect is already drawn on the key itself. NONE reproduces
 * the original behavior (no pop-up at all).
 *
 * The preset entries (HEARTS, STARS, SPARKLES, FIRE, CONFETTI) each cycle
 * through a small fixed emoji set per press so repeated presses don't look
 * identical. CUSTOM_EMOJI and CUSTOM_IMAGE are the "DIY Animation" feature:
 * CUSTOM_EMOJI always pops the single emoji character stored in
 * PreferencesManager.typingAnimationEmoji; CUSTOM_IMAGE pops the small
 * cropped image at PreferencesManager.typingAnimationImageUri instead of an
 * emoji glyph.
 */
enum class TypingAnimation(val key: String, val label: String, val emojiSet: List<String>) {
    NONE("none", "Off", emptyList()),
    HEARTS("hearts", "Hearts", listOf("💖", "💕", "💗", "❤️")),
    STARS("stars", "Stars", listOf("⭐", "✨", "🌟", "💫")),
    SPARKLES("sparkles", "Sparkles", listOf("✨", "🎇", "💥")),
    FIRE("fire", "Fire", listOf("🔥", "🔥", "💥")),
    CONFETTI("confetti", "Confetti", listOf("🎉", "🎊", "🎈")),
    /** DIY: pops the single custom emoji the user chose. */
    CUSTOM_EMOJI("custom_emoji", "Custom emoji", emptyList()),
    /** DIY: pops the custom cropped image the user chose. */
    CUSTOM_IMAGE("custom_image", "Custom image", emptyList());

    companion object {
        fun fromKey(key: String): TypingAnimation = entries.find { it.key == key } ?: NONE
    }
}

/**
 * "LED / Neon Lighting" — an ambient, whole-board lighting layer distinct
 * from the per-key KeyEffect above. Where KeyEffect decorates individual
 * key borders/glows, LedPattern drives a thin animated light strip along
 * the top edge of the keyboard (like an RGB keyboard's under-glow),
 * independent of which KeyEffect is also selected — the two can be
 * combined (e.g. RIPPLE key-effect + WAVE led-pattern).
 */
enum class LedPattern(val key: String, val label: String) {
    /** No ambient lighting strip — original look. */
    NONE("none", "Off"),
    /** Strip brightness smoothly rises and falls, like slow breathing. */
    BREATHING("breathing", "Breathing"),
    /** A band of light travels left-to-right, then loops. */
    WAVE("wave", "Wave"),
    /** Strip hue continuously cycles through the rainbow. */
    CYCLE("cycle", "Cycle"),
    /** Small twinkling points of light switch on/off at random positions. */
    STARS("stars", "Stars");

    companion object {
        fun fromKey(key: String): LedPattern = entries.find { it.key == key } ?: NONE
    }
}

/**
 * Themes screen "Backgrounds" section — a built-in, procedurally-drawn
 * (no bundled image assets, so no copyright/licensing concerns) keyboard
 * background: solid, gradient, rainbow, "galaxy", "smoke", or a couple of
 * soft pastel/cute presets. Purely decorative, drawn behind the normal key
 * rows exactly like the existing "My themes" photo background.
 *
 * Precedence (see KeyboardView): a "My themes" photo/GIF, if set, is drawn
 * on top of and takes priority over this — this is the fallback/base layer
 * a user picks when they don't want to supply their own image.
 */
enum class BackgroundStyle(val key: String, val label: String) {
    /** No built-in background — original flat colors.bg. */
    NONE("none", "None"),
    GRADIENT("gradient", "Gradient"),
    RAINBOW("rainbow", "Rainbow"),
    GALAXY("galaxy", "Galaxy"),
    SMOKE("smoke", "Smoke"),
    SUNSET_SKY("sunset_sky", "Sunset"),
    PASTEL_CUTE("pastel_cute", "Pastel Cute"),
    MINT_CUTE("mint_cute", "Mint Cute");

    companion object {
        fun fromKey(key: String): BackgroundStyle = entries.find { it.key == key } ?: NONE
    }
}
