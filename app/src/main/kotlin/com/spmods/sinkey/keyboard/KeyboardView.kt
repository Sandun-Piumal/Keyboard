package com.spmods.sinkey.keyboard

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.drawBehind

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
// matchParentSize() is a member of BoxScope (not a top-level extension
// function like fillMaxSize), so it needs no import — it resolves
// automatically via the implicit BoxScope receiver at each call site
// inside a Box{} content lambda. Importing it as if it were top-level
// doesn't compile.
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.spmods.sinkey.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.data.FancyTextStyle
import com.spmods.sinkey.data.RemoteUpdateInfo
import com.spmods.sinkey.data.UpdateChecker
import com.spmods.sinkey.data.clipboard.ClipEntity
import com.spmods.sinkey.data.clipboard.ClipRepository
import com.spmods.sinkey.ime.SinKeyInputMethodService
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo

// Number labels for top row keys
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Desh Keyboard exact accent color (accentContainer from ManglishLight theme)
internal val DeshGreen = Color(0xFF6E9A65)

// ── Theme-aware color helpers ─────────────────────────────────────────────────

internal data class KeyboardColors(
    val bg: Color,
    val keyBg: Color,
    val specialKeyBg: Color,
    val keyText: Color,
    val specialKeyText: Color,
    val subText: Color,
    val spaceKeyBg: Color,
    val spaceKeyText: Color,
    // Surface colour for card/tile UI that lives *inside* a board — sticker
    // grid tiles, clipboard item cards, font-style preview cards. Unlike
    // keyBg (which intentionally collapses to the same colour as `bg` when
    // showKeyBorders is off, so letter keys read as flat/floating), these
    // cards need a background that's always visibly distinct from the
    // board's own bg, borders-off or not — otherwise the tiles disappear
    // and only their icons/text float on the page. Kept independent of
    // showKeyBorders on purpose.
    val cardBg: Color,
    // Themes screen "Colors" accent — see KeyColorPalette. Used to tint the
    // space bar / special keys and to drive the "Effects" outline/glow/
    // underline colors below. Defaults to DeshGreen (KeyColorPalette.DEFAULT's
    // accent), matching the app's original accent exactly when no palette
    // has been explicitly chosen.
    val accent: Color = DeshGreen,
    // Themes screen "Effects" selection, carried alongside the colors so
    // every key composable can read both from one place instead of two
    // separate params threaded through ~15 call sites. NONE = original
    // flat-background rendering, unchanged from before this feature existed.
    val keyEffect: com.spmods.sinkey.data.KeyEffect = com.spmods.sinkey.data.KeyEffect.NONE,
)

@Composable
internal fun keyboardColors(
    showKeyBorders: Boolean,
    isDark: Boolean,
    palette: com.spmods.sinkey.data.KeyColorPalette = com.spmods.sinkey.data.KeyColorPalette.DEFAULT,
    keyEffect: com.spmods.sinkey.data.KeyEffect = com.spmods.sinkey.data.KeyEffect.NONE,
    // True when a "My themes" custom photo background is active — makes
    // `bg` fully transparent so the outer Box's image can show through
    // every band of the keyboard (toolbar, suggestion strip, key rows)
    // instead of being painted over by each row's own .background(colors.bg).
    // See KeyboardView's root Box/Column for where the image itself is drawn.
    transparentBg: Boolean = false,
): KeyboardColors {
    val base = keyboardColorsBase(showKeyBorders, isDark)
    return base.copy(
        bg = if (transparentBg) Color.Transparent else base.bg,
        accent = palette.accent,
        keyEffect = keyEffect,
    )
}

/**
 * Applies the current Themes-screen "Effects" style (see KeyEffect) as a
 * decoration around a key, layered on top of whatever background/clip the
 * caller already applied. Meant to be chained right after
 * `.clip(keyShape).background(colors.keyBg)` at each of the ~9 key-drawing
 * sites in this file — added once here instead of duplicating the same
 * `when (colors.keyEffect)` branch at every call site.
 *
 * NONE is a true no-op (returns `this` unchanged) so keys look byte-for-byte
 * identical to before this feature existed when no effect is selected.
 */
private fun Modifier.keyEffectDecoration(colors: KeyboardColors, keyShape: RoundedCornerShape): Modifier {
    return when (colors.keyEffect) {
        com.spmods.sinkey.data.KeyEffect.NONE -> this
        com.spmods.sinkey.data.KeyEffect.OUTLINE -> this.border(
            width = 1.5.dp,
            color = colors.accent.copy(alpha = 0.85f),
            shape = keyShape
        )
        com.spmods.sinkey.data.KeyEffect.GLOW -> this.shadow(
            elevation = 6.dp,
            shape = keyShape,
            ambientColor = colors.accent,
            spotColor = colors.accent
        )
        com.spmods.sinkey.data.KeyEffect.UNDERLINE -> this.drawBehind {
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = colors.accent,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height - strokeWidth),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height - strokeWidth),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        // Bottom-edge drop shadow only (offset shadow), giving a raised
        // "3D button" look rather than GLOW's even all-around blur.
        com.spmods.sinkey.data.KeyEffect.SHADOW_3D -> this
            .shadow(
                elevation = 4.dp,
                shape = keyShape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .border(width = 0.75.dp, color = colors.accent.copy(alpha = 0.35f), shape = keyShape)
        // Continuously-pulsing glow — animated version of GLOW. Uses
        // Modifier.composed so it can host its own infinite animation
        // without every call site needing to be touched.
        com.spmods.sinkey.data.KeyEffect.NEON_PULSE -> this.composed {
            val infinite = rememberInfiniteTransition(label = "neonPulse")
            val pulse by infinite.animateFloat(
                initialValue = 2.dp.value,
                targetValue = 10.dp.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neonPulseElevation"
            )
            this.shadow(
                elevation = pulse.dp,
                shape = keyShape,
                ambientColor = colors.accent,
                spotColor = colors.accent
            )
        }
        // Border hue cycles through a small loop of colors derived from the
        // chosen accent — the "gaming RGB / cyberpunk" look.
        com.spmods.sinkey.data.KeyEffect.RGB_CYCLE -> this.composed {
            val infinite = rememberInfiniteTransition(label = "rgbCycle")
            val hueShift by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = androidx.compose.animation.core.LinearEasing)
                ),
                label = "rgbHueShift"
            )
            val cycledColor = remember(hueShift) { rotateHue(colors.accent, hueShift) }
            this.border(width = 1.75.dp, color = cycledColor, shape = keyShape)
        }
        // Ripple handled separately via .keyRippleEffect() at the call site
        // (needs the actual touch position from pointerInput), so it's a
        // no-op here — see LetterKey/NumberedLetterKey/NumpadDigitKey.
        com.spmods.sinkey.data.KeyEffect.RIPPLE -> this
        // Pop/scale handled via rememberKeyBumpScale's existing pressed-scale
        // animation at the call site (bumpScale already responds to
        // `pressed`) — see keyPopScaleMultiplier() used alongside .scale().
        com.spmods.sinkey.data.KeyEffect.POP_SCALE -> this
        // Drawn entirely by RgbRippleOverlay (a Box layered above all keys —
        // see KeyboardView's rgbRippleActive branch), since the wave needs
        // to travel across *every* key's position relative to whichever key
        // was touched, not just decorate that one key itself. No-op here.
        com.spmods.sinkey.data.KeyEffect.RGB_RIPPLE -> this
    }
}

/**
 * Rotates [color]'s hue by [degrees] (0–360) in HSV space, keeping
 * saturation/value the same — used by RGB_CYCLE to animate through a loop
 * of hues derived from whichever accent color the user picked, rather than
 * a hardcoded rainbow unrelated to their chosen palette.
 */
private fun rotateHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Extra scale multiplier for KeyEffect.POP_SCALE, layered on top of the
 * existing press-bump scale so "Pop" reads as a clearly bigger bounce than
 * the default subtle bump. Returns 1f (no-op) for every other effect so
 * this is safe to multiply into .scale() unconditionally at call sites.
 */
@Composable
private fun keyPopScaleMultiplier(pressed: Boolean, keyEffect: com.spmods.sinkey.data.KeyEffect): Float {
    if (keyEffect != com.spmods.sinkey.data.KeyEffect.POP_SCALE) return 1f
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "popScale"
    )
    return scale
}

/**
 * Draws an expanding, fading ripple circle from the last touch-down point
 * for KeyEffect.RIPPLE. No-op Modifier for every other effect. Meant to be
 * chained right after .keyEffectDecoration() at key call sites that track
 * their own `pressed`/touch state (LetterKey, NumberedLetterKey,
 * NumpadDigitKey).
 */
private fun Modifier.keyRippleEffect(
    colors: KeyboardColors,
    pressed: Boolean
): Modifier = composed {
    if (colors.keyEffect != com.spmods.sinkey.data.KeyEffect.RIPPLE) return@composed this

    val rippleProgress = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, animationSpec = tween(420, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
        }
    }
    this.drawWithContent {
        drawContent()
        if (rippleProgress.value > 0f && rippleProgress.value < 1f) {
            val maxRadius = kotlin.math.max(size.width, size.height) * 0.75f
            drawCircle(
                color = colors.accent.copy(alpha = (1f - rippleProgress.value) * 0.45f),
                radius = maxRadius * rippleProgress.value,
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}

@Composable
private fun keyboardColorsBase(showKeyBorders: Boolean, isDark: Boolean): KeyboardColors {
    return if (isDark) {
        // FIX #12: Dark theme previously had almost no visible difference between
        // bordered (0xFF2E2E2E) and borderless (0xFF262626) key backgrounds.
        // Now borderless uses the same bg colour as the keyboard background so
        // keys appear "flat/floating", while bordered uses a clearly lighter slab.
        KeyboardColors(
            bg             = Color(0xFF1E1E1E),
            keyBg          = if (showKeyBorders) Color(0xFF3A3A3A) else Color(0xFF1E1E1E),
            specialKeyBg   = if (showKeyBorders) Color(0xFF2C2C2C) else Color(0xFF1E1E1E),
            keyText        = Color(0xFFE8E8E8),
            specialKeyText = Color(0xFFCCCCCC),
            subText        = Color(0xFF888888),
            // Borderless: subtle tint (slightly lighter than bg) so the
            // space-bar pill stays visible without a full bordered look.
            spaceKeyBg     = if (showKeyBorders) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
            spaceKeyText   = Color(0xFF777777),
            // Always the same lighter slab used by bordered keys, regardless
            // of showKeyBorders — cards need to stay visible against bg.
            cardBg         = Color(0xFF3A3A3A),
        )
    } else {
        KeyboardColors(
            // keyboard_background_light_bordered = #E6EAED
            bg             = Color(0xFFE6EAED),
            // Bordered: primaryContainer = White. Borderless: same as keyboard
            // bg so keys appear "flat/floating" (mirrors the dark theme fix).
            keyBg          = if (showKeyBorders) Color(0xFFFFFFFF) else Color(0xFFE6EAED),
            // secondaryContainer (functional keys) = #335f9154 overlay on bg.
            // Borderless: flat with keyboard bg, same as keyBg above.
            specialKeyBg   = if (showKeyBorders) Color(0xFFC5CDD5) else Color(0xFFE6EAED),
            // onPrimaryContainer = black
            keyText        = Color(0xFF000000),
            // specialKeyText - dark grey
            specialKeyText = Color(0xFF444444),
            // subText for number hints
            subText        = Color(0xFF666666),
            // space bar bg: bordered = white, borderless = subtle tint
            // (slightly darker than keyboard bg) so the pill stays visible
            spaceKeyBg     = if (showKeyBorders) Color(0xFFFFFFFF) else Color(0xFFDBE0E4),
            // space bar text - medium grey
            spaceKeyText   = Color(0xFF888888),
            // Always white, regardless of showKeyBorders — cards need to
            // stay visible against bg.
            cardBg         = Color(0xFFFFFFFF),
        )
    }
}

/** Convert a 0..3 slider step to a concrete key-row height in dp. */
private fun stepToKeyHeight(step: Float): Dp = when (Math.round(step)) {
    // Desh exact: config_key_height_qwerty = 48dp (default = step 1)
    0    -> 42.dp
    1    -> 48.dp
    2    -> 54.dp
    else -> 62.dp
}

/** Convert a 0..3 slider step to bottom padding in dp. */
private fun stepToBottomPadding(step: Float): Dp = when (Math.round(step)) {
    0    -> 4.dp
    1    -> 10.dp
    2    -> 18.dp
    else -> 28.dp
}

// Board state enum — tracks which keyboard panel is currently visible.
// Replaces the previous ad-hoc boolean flags (showSymbols, showEmojiPicker)
// which had no memory of which board opened them, so back always went to MAIN.
// Must be internal (not private) so SinKeyInputMethodService can reference it.
enum class Board { MAIN, SYMBOLS, NUMPAD, EMOJI, CLIPBOARD, FONT, STICKER, STICKER_CREATE, STICKER_EDIT }

/** Result of Board.STICKER_EDIT's async preview-image decode — see that branch in KeyboardView's content `when`. */
private sealed class StickerEditDecodeResult {
    data class Ok(val bitmap: android.graphics.Bitmap) : StickerEditDecodeResult()
    object Failed : StickerEditDecodeResult()
}

@Composable
internal fun KeyboardView(
    currentLanguage: String,
    keyboardHeight: Float = 2f,
    bottomSpaceEnabled: Boolean = true,
    bottomSpaceSize: Float = 0f,
    showKeyBorders: Boolean = true,
    isDark: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {},
    // Non-null right after a silent autocorrect swapped what the user
    // typed for a spell-checker correction — holds the original typed
    // word so AppsMicBar can show a one-tap "Undo" chip. Owned by the IME
    // service (see SinKeyInputMethodService.autocorrectUndo) so it's
    // cleared consistently from every edit path, the same reasoning as
    // boardStack/shiftState above. Preview callers omit this and get no chip.
    autocorrectUndoWord: String? = null,
    onUndoAutocorrect: () -> Unit = {},
    // Gesture (swipe-to-type) typing — see GestureTypingOverlay/
    // GestureWordMatcher. Off unless swipeTypingEnabled is true (mirrors
    // PreferencesManager.swipeTypingEnabled, collected by the IME service).
    // onSwipeGesture reports the raw swiped letter sequence (already
    // deduped/nearest-key-resolved by GestureTypingOverlay) plus the
    // current typing language, and gets back candidate words ranked best-
    // first — actual dictionary matching happens in the service (see
    // SinKeyInputMethodService.resolveGestureCandidates) since that's
    // where wordRepo/serviceScope already live; the overlay itself has no
    // coroutine scope of its own for DB access. The best candidate is
    // committed automatically; the rest populate the suggestion strip so
    // the user can tap an alternative, the same as typed-word suggestions.
    swipeTypingEnabled: Boolean = false,
    onSwipeGesture: suspend (letters: String, language: String) -> List<String> = { _, _ -> emptyList() },
    onGestureWordCommitted: (String) -> Unit = {},
    onKey: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    inputType: Int = 0,
    // Board stack owned by the IME service so it survives keyboard hide/show cycles.
    // Preview callers (MainActivity) omit these and get default MAIN behaviour.
    boardStack: List<Board> = listOf(Board.MAIN),
    onBoardStackChange: (List<Board>) -> Unit = {},
    // ShiftState owned by the IME service (survives hide/show). Preview callers omit.
    shiftState: SinKeyInputMethodService.ShiftState = SinKeyInputMethodService.ShiftState.OFF,
    onShiftStateChange: (SinKeyInputMethodService.ShiftState) -> Unit = {},
    // Update-banner dismiss state, owned by the IME service so it's reset
    // on every keyboard show (see the field comment in
    // SinKeyInputMethodService.kt) — unlike boardStack/shiftState above,
    // this one is deliberately NOT meant to survive hide/show. Preview
    // callers (MainActivity) omit these and default to "not dismissed".
    dismissedUpdateVersionCode: Int = 0,
    onDismissedUpdateVersionCodeChange: (Int) -> Unit = {},
    // Sticker board (Board.STICKER / STICKER_CREATE). onStickerSend is
    // called with (filePath, mimeType) — see
    // SinKeyInputMethodService.onStickerSelected for what each means.
    // Preview callers (MainActivity) omit this and get a no-op.
    onStickerSend: (String, String) -> Unit = { _, _ -> },
    // Triggers the system gallery picker for Board.STICKER_CREATE's Image
    // Sticker option — wired to SinKeyInputMethodService.pickImageForSticker,
    // since only the service can start the trampoline Activity needed here.
    onPickStickerImage: () -> Unit = {},
    // Board.STICKER_EDIT state: the temp file path of the image most
    // recently picked via onPickStickerImage, hoisted in the service (same
    // reasoning as boardStack — must survive hide/show while the editor is
    // open). Null when no image sticker is currently being edited.
    pendingStickerImagePath: String? = null,
    // Called when the user taps "Add to stickers" on Board.STICKER_EDIT,
    // wired to SinKeyInputMethodService.saveEditedImageSticker.
    onSaveImageSticker: (ImageStickerDraft) -> Unit = {}
) {
    // Read ahead of `colors` below (moved up from where these used to live,
    // further down this function) because keyboardColors() now needs the
    // palette/effect/custom-background prefs to build the right
    // KeyboardColors for this composition.
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val keyColorPalette by prefsManager.keyColorPalette.collectAsState(
        initial = com.spmods.sinkey.data.KeyColorPalette.DEFAULT
    )
    val keyEffect by prefsManager.keyEffect.collectAsState(
        initial = com.spmods.sinkey.data.KeyEffect.NONE
    )
    val customBackgroundUri by prefsManager.customBackgroundUri.collectAsState(initial = null)
    val backgroundStyle by prefsManager.backgroundStyle.collectAsState(
        initial = com.spmods.sinkey.data.BackgroundStyle.NONE
    )
    val materialYouEnabled by prefsManager.materialYouEnabled.collectAsState(initial = false)
    val typingAnimation by prefsManager.typingAnimation.collectAsState(
        initial = com.spmods.sinkey.data.TypingAnimation.NONE
    )
    val typingAnimationEmoji by prefsManager.typingAnimationEmoji.collectAsState(initial = "✨")
    val typingAnimationImageUri by prefsManager.typingAnimationImageUri.collectAsState(initial = null)
    val ledPattern by prefsManager.ledPattern.collectAsState(
        initial = com.spmods.sinkey.data.LedPattern.NONE
    )
    val ledIdleDimming by prefsManager.ledIdleDimming.collectAsState(initial = true)

    val colors = keyboardColors(
        showKeyBorders = showKeyBorders,
        isDark = isDark,
        palette = keyColorPalette,
        keyEffect = keyEffect,
        transparentBg = customBackgroundUri != null || backgroundStyle != com.spmods.sinkey.data.BackgroundStyle.NONE,
    ).let { base ->
        // Material You: when enabled, override the accent with the
        // dynamic system-wallpaper color instead of the picked
        // KeyColorPalette — purely a color swap, effects/backgrounds
        // still layer on top exactly as before.
        if (materialYouEnabled) base.copy(accent = materialYouAccentColor(context, isDark)) else base
    }
    val keyHeight = stepToKeyHeight(keyboardHeight)
    val bottomPadding = if (bottomSpaceEnabled) stepToBottomPadding(bottomSpaceSize) else 4.dp
    val keyShape = RoundedCornerShape(6.dp)

    // ── Shared "a key was just pressed here" state for the three features
    // that all need to react to real touch positions rather than to
    // onKey's post-commit callback: Typing Animation (pop-up over the
    // actual key), LED/Neon ripple (glow spreading from the actual key),
    // and LED idle-dimming (any press counts as activity). Populated by
    // the touch-down pointerInput inside the MAIN board's key Box below
    // (see rgbRippleActive/ledPattern/typingAnimation branch there) —
    // hoisted up to this top level, rather than kept local to that nested
    // block, specifically so this composable's own TypingAnimationPopup
    // and KeyboardLedRipple calls at the bottom of the function can read
    // them. pressOrigin/pressTriggerId intentionally mirror the existing
    // rgbRippleOrigin/rgbRippleTriggerId pattern (see RgbRippleOverlay) —
    // same restart-even-on-identical-value reasoning: bumping triggerId on
    // every touch-down, even two touches of the same key in a row, is what
    // makes the LaunchedEffect(triggerId) in each consumer actually restart
    // every time rather than silently no-op when origin didn't change.
    val keyPositions = remember { mutableStateMapOf<Char, KeyPoint>() }
    // Real measured (width, height) in px of each key, captured alongside
    // keyPositions — see the two places that write into it below (the
    // recompute LaunchedEffect and the immediate best-effort conversion in
    // onKeyPositioned) for why this needs the same "record raw, convert
    // when ready" two-step as positions do. This replaced an earlier
    // version that used one hardcoded (screenWidth/10, keyHeight) estimate
    // for every key on the board — that estimate didn't account for rows
    // whose keys are genuinely a different height/width than the main
    // letter rows (the quick-toggle row of ?123/emoji/lang-toggle/Space/./
    // Enter being the most visibly wrong case, since Space in particular is
    // both much wider AND not necessarily the same height as a letter key),
    // so the border-glow outline this drives ended up too big/small and
    // misaligned specifically on that row — using each key's own real
    // measured size fixes that at the source rather than special-casing it.
    val keySizes = remember { mutableStateMapOf<Char, androidx.compose.ui.geometry.Size>() }
    var pressOrigin by remember { mutableStateOf<Offset?>(null) }
    var pressTriggerId by remember { mutableStateOf(0) }
    var lastActivityAtMs by remember { mutableStateOf(0L) }
    val isLedIdle by rememberKeyboardIdleState(lastActivityAtMs, enabled = ledIdleDimming)
    val typingAnimationImageBitmap = rememberCustomAnimationBitmap(typingAnimationImageUri)

    // shift = true whenever shiftState is ONE_SHOT or LOCKED
    val shift = shiftState != SinKeyInputMethodService.ShiftState.OFF
    val shiftLocked = shiftState == SinKeyInputMethodService.ShiftState.LOCKED

    var showLangTooltip by remember { mutableStateOf(false) }

    // Formula-derived height of MainKeyboardKeys' content, in dp. Boards that
    // hide the toolbar/emoji-row and want to fill that reclaimed space
    // exactly (CLIPBOARD, FONT) read this so they always render at exactly
    // the same height as MAIN.
    //
    // This used to be a *measured* value that started at a guessed fallback
    // (keyHeight * 4 + 40.dp) and only became accurate after MAIN had been
    // composed once and reported its real size via onGloballyPositioned. If
    // the user opened Font/Clipboard first in a session — e.g. right when
    // the keyboard popped up, before ever touching MAIN — they'd get the
    // wrong guessed height, visibly mismatching the main board. Deriving it
    // directly from the same row-height + padding formula MainKeyboardKeys
    // itself uses removes that race entirely: it's correct on frame one,
    // every time.
    //
    // MainKeyboardKeys layout: Column(vertical padding 2.dp top + bottom,
    // plus bottomPadding) containing 4 rows of height keyHeight, each row
    // padded 3.dp top+bottom.
    val measuredMainContentHeight = (keyHeight + 6.dp) * 4 + 4.dp + bottomPadding

    val currentBoard = boardStack.last()
    fun pushBoard(b: Board) { onBoardStackChange(boardStack + b) }
    fun popBoard()          { if (boardStack.size > 1) onBoardStackChange(boardStack.dropLast(1)) }

    val isPhoneInput = remember(inputType) {
        (inputType and android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE) ==
            android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE
    }

    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())
    val clipRepository = remember { ClipRepository(context) }
    val clipHistory by clipRepository.history.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val selectedFontKey by prefsManager.keyboardFont.collectAsState(initial = FancyTextStyle.NONE.key)
    val stickerRepository = remember { com.spmods.sinkey.data.sticker.StickerRepository(context) }
    val ownStickers by stickerRepository.all.collectAsState(initial = emptyList())
    val favouriteStickers by stickerRepository.favourites.collectAsState(initial = emptyList())

    // ── Update check ────────────────────────────────────────────────────────
    // Fetched once per keyboard-composition (not on every recomposition —
    // `Unit` as the LaunchedEffect key means this block runs exactly once
    // for the lifetime of this composable instance, same as the keyboard
    // being shown once per IME session). Any failure inside UpdateChecker
    // is already swallowed there and returns null, so this can't crash or
    // show an error — it just silently stays "no update" on failure.
    var remoteUpdate by remember { mutableStateOf<RemoteUpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        remoteUpdate = UpdateChecker.checkForUpdate(context)
    }
    // dismissedUpdateVersionCode comes from the IME service (see its param
    // doc above) — NOT DataStore/PreferencesManager, because dismissal here
    // must be undone every time the keyboard is reopened, not persisted
    // forever like a normal user preference.
    val showUpdateBanner = remoteUpdate != null && remoteUpdate!!.versionCode > dismissedUpdateVersionCode

    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    // ONE Column for the whole keyboard — toolbar always at top, content below
    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        // Built-in procedural background (Gradient/Rainbow/Galaxy/Smoke/...)
        // — bottom-most layer. Drawn only when no "My themes" photo is set,
        // since a user-picked photo always takes precedence (see
        // BackgroundStyle doc comment) — both are still allowed to make
        // colors.bg transparent above so either one shows through cleanly.
        if (customBackgroundUri == null && backgroundStyle != com.spmods.sinkey.data.BackgroundStyle.NONE) {
            KeyboardBuiltInBackground(
                style = backgroundStyle,
                isDark = isDark,
                modifier = Modifier.matchParentSize()
            )
        }
        // "My themes" custom photo background — drawn as the bottom-most
        // layer of the outer Box, behind everything else. Only rendered
        // when a background is actually set; colors.bg is made fully
        // transparent by keyboardColors(transparentBg = ...) in that case
        // so every row's own `.background(colors.bg)` (toolbar, suggestion
        // strip, key rows, etc.) lets this show through instead of
        // painting over it in solid color band-by-band.
        customBackgroundUri?.let { uriString ->
            KeyboardCustomBackground(
                uriString = uriString,
                modifier = Modifier.matchParentSize()
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(colors.bg)
        ) {
            // ── Toolbar (always visible, never re-created on pad switch,
            // except for the Emoji board — which moves its own category
            // tabs to the very top instead, in place of this toolbar) ──────
            if (currentBoard != Board.EMOJI && currentBoard != Board.CLIPBOARD && currentBoard != Board.FONT &&
                currentBoard != Board.STICKER && currentBoard != Board.STICKER_CREATE) {
                AppsMicBar(
                    colors = colors,
                    isDark = isDark,
                    suggestions = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD) emptyList() else suggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    autocorrectUndoWord = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD) null else autocorrectUndoWord,
                    onUndoAutocorrect = onUndoAutocorrect,
                    onKey = onKey,
                    onClipboardOpen = { pushBoard(Board.CLIPBOARD) },
                    onFontOpen = { pushBoard(Board.FONT) },
                    onStickerOpen = { pushBoard(Board.STICKER) },
                    selectedFontStyle = FancyTextStyle.fromKey(selectedFontKey)
                )
            }

            // ── Recent emoji row — shown above whichever board is active,
            // except the Emoji board itself (which has its own Recent tab).
            // When an update is available and hasn't been dismissed, this
            // strip is TEMPORARILY replaced by the update banner instead
            // (same slot, same 44dp height, so nothing else on the board
            // shifts or resizes) — the recent-emoji strip itself is not
            // shown at all while the banner is up.
            if (!isPhoneInput && currentBoard != Board.EMOJI && currentBoard != Board.CLIPBOARD && currentBoard != Board.FONT &&
                currentBoard != Board.STICKER && currentBoard != Board.STICKER_CREATE) {
                if (showUpdateBanner) {
                    UpdateBanner(
                        colors = colors,
                        onOpenClick = {
                            openUrl(context, remoteUpdate!!.url)
                        },
                        onDismissClick = {
                            onDismissedUpdateVersionCodeChange(remoteUpdate!!.versionCode)
                        }
                    )
                } else if (recentEmojis.isNotEmpty()) {
                    EmojiRow(
                        emojis = recentEmojis,
                        colors = colors,
                        onKey = onKey,
                        onMoreClick = { pushBoard(Board.EMOJI) }
                    )
                }
            }

            // ── Content area — only this part switches ────────────────────────
            when {
                isPhoneInput -> PhoneDialPadKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding, onKey = onKey
                )
                currentBoard == Board.EMOJI -> EmojiPickerView(
                    recentEmojis = recentEmojis,
                    colors = colors,
                    keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    onEmojiSelected = { emoji -> onKey(emoji) },
                    onBackspace = { onKey("BACKSPACE") },
                    onDismiss = { popBoard() }          // back to whichever board opened emoji
                )
                currentBoard == Board.SYMBOLS -> SymbolsKeyboardKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding,
                    onKey = onKey,
                    onBack = { popBoard() },            // back to MAIN
                    onNumpad = { pushBoard(Board.NUMPAD) },
                    onEmoji  = { pushBoard(Board.EMOJI) }
                )
                currentBoard == Board.NUMPAD -> NumberPadView(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding,
                    onKey = onKey,
                    onBack = { popBoard() },
                    onBoardStackChange = onBoardStackChange
                )
                currentBoard == Board.CLIPBOARD -> ClipboardHistoryView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    history = clipHistory,
                    onPaste = { text -> onKey("PASTE_TEXT:$text") },
                    onTogglePin = { text, pinned ->
                        coroutineScope.launch { clipRepository.setPinned(text, pinned) }
                    },
                    onDelete = { text ->
                        coroutineScope.launch { clipRepository.delete(text) }
                    },
                    onClearAll = {
                        coroutineScope.launch { clipRepository.clearUnpinned() }
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.FONT -> FontPickerView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    selectedFontKey = selectedFontKey,
                    onFontSelected = { fontKey ->
                        coroutineScope.launch { prefsManager.setKeyboardFont(fontKey) }
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER -> StickerBoardView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    ownStickers = ownStickers,
                    favouriteStickers = favouriteStickers,
                    onSendOwnSticker = { filePath -> onStickerSend(filePath, "image/png") },
                    onToggleFavourite = { filePath, fav ->
                        coroutineScope.launch { stickerRepository.setFavourite(filePath, fav) }
                    },
                    onDeleteSticker = { filePath ->
                        coroutineScope.launch { stickerRepository.delete(filePath) }
                    },
                    onCreateClick = { pushBoard(Board.STICKER_CREATE) },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER_CREATE -> StickerCreateView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    onPickImageRequested = onPickStickerImage,
                    onTextSubmitted = { text, textColor ->
                        coroutineScope.launch { stickerRepository.createFromText(text, textColor) }
                        popBoard()
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER_EDIT -> {
                    // Decoding a picked photo (even downscaled) is real I/O +
                    // CPU work — doing it synchronously inside remember{}
                    // blocks Compose's composition phase on the UI thread,
                    // which can freeze the keyboard badly enough to look
                    // exactly like "selecting an image does nothing" (no
                    // crash, no toast, just a stuck/unresponsive picker).
                    // produceState runs the decode in a coroutine instead,
                    // so composition itself stays fast and a loading state
                    // can be shown while it works.
                    //
                    // Three distinct states are tracked (not just "bitmap or
                    // null") so a genuine decode failure can show feedback
                    // and a way back, instead of either an infinite spinner
                    // or a silent pop that looks like nothing happened:
                    //   null    == still decoding (or not started)
                    //   Ok      == decoded successfully
                    //   Failed  == decode finished and returned null
                    val previewState = androidx.compose.runtime.produceState<StickerEditDecodeResult?>(
                        initialValue = null,
                        key1 = pendingStickerImagePath
                    ) {
                        val path = pendingStickerImagePath
                        value = if (path == null) {
                            null
                        } else {
                            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.spmods.sinkey.data.sticker.StickerFileStore.decodePreviewBitmap(java.io.File(path))
                            }
                            if (bitmap != null) StickerEditDecodeResult.Ok(bitmap) else StickerEditDecodeResult.Failed
                        }
                    }
                    val editHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp)

                    when (val result = previewState.value) {
                        is StickerEditDecodeResult.Ok -> StickerImageEditorView(
                            colors = colors,
                            bottomPadding = bottomPadding,
                            targetContentHeight = editHeight,
                            imageBitmap = result.bitmap,
                            onSave = { draft -> onSaveImageSticker(draft) },
                            onBack = { popBoard() }
                        )
                        StickerEditDecodeResult.Failed -> Box(
                            modifier = Modifier.fillMaxWidth().height(editHeight).background(colors.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Couldn't read that image",
                                    fontSize = 13.sp,
                                    color = colors.subText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBg)
                                        .clickable { popBoard() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(text = "Go back", fontSize = 13.sp, color = colors.keyText)
                                }
                            }
                        }
                        null -> if (pendingStickerImagePath == null) {
                            // Nothing to edit at all (shouldn't normally
                            // happen — STICKER_EDIT is only ever pushed
                            // together with setting this) — back out safely
                            // instead of showing a permanently blank board.
                            LaunchedEffect(Unit) { popBoard() }
                        } else {
                            // Genuinely still decoding.
                            Box(
                                modifier = Modifier.fillMaxWidth().height(editHeight).background(colors.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = DeshGreen)
                            }
                        }
                    }
                }
                else -> {
                    // Key-center map built up as MainKeyboardKeys lays out
                    // each letter key (see LetterKey/NumberedLetterKey's
                    // onPositioned) — used once a swipe finishes to translate
                    // the touch path into a letter sequence, AND to know
                    // every other key's position for RGB_RIPPLE / the LED
                    // ripple / the Typing Animation pop-up, all of which
                    // need to know where the actually-touched key is. This
                    // map itself is hoisted to the top of KeyboardView (see
                    // "Shared press state" above) since TypingAnimationPopup
                    // and KeyboardLedRipple are drawn at that outer level,
                    // not inside this nested Box — only the *touch watcher*
                    // that fills it in lives here, since only the MAIN
                    // board's keys currently report their positions.
                    val rgbRippleActive = colors.keyEffect == com.spmods.sinkey.data.KeyEffect.RGB_RIPPLE
                    val ledActive = ledPattern != com.spmods.sinkey.data.LedPattern.NONE
                    val typingAnimActive = typingAnimation != com.spmods.sinkey.data.TypingAnimation.NONE
                    val needsKeyPositions = rgbRippleActive || ledActive || typingAnimActive || swipeTypingEnabled
                    // Gesture-in-progress state, hoisted up to this shared
                    // Box (see the pointerInput placement below for why it
                    // can't live inside GestureTypingOverlay itself anymore).
                    var gesturePathPoints by remember { mutableStateOf(listOf<Offset>()) }
                    var gestureIsDragging by remember { mutableStateOf(false) }
                    val gestureScope = rememberCoroutineScope()
                    // This outer Box's own coordinates, captured so each
                    // key's position can be converted into ITS coordinate
                    // space (see onKeyPositioned below) rather than staying
                    // in whatever coordinate space `coords.positionInParent()`
                    // would have given (that key's own direct parent Row —
                    // several layout levels below this Box, and a different
                    // origin for every row). RgbRippleOverlay/
                    // KeyboardLedRipple/TypingAnimationPopup are all drawn
                    // as direct children of THIS Box (via matchParentSize()/
                    // Popup below), so their (0,0) origin is this Box's
                    // top-left corner — key positions need to be measured
                    // against that same origin or every effect anchored on
                    // them lands in the wrong place, which is exactly what
                    // was happening before this fix (a popup meant to sit
                    // over the pressed key instead floating up near the
                    // whole keyboard's top edge, offset by however far that
                    // key's own row happened to be from this Box's origin).
                    var boxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                    // Raw per-key coordinates, captured as each key is laid
                    // out — kept separately from `keyPositions` (the
                    // outer-Box-relative Float x/y map effects actually
                    // read) because a key can be placed and report its own
                    // onGloballyPositioned BEFORE this Box's own coordinates
                    // are available yet (composition/layout ordering between
                    // a Box and its deeply-nested children isn't guaranteed
                    // to run parent-first). Converting eagerly at each key's
                    // own callback and just skipping when boxCoordinates
                    // was still null at that moment silently dropped that
                    // key from keyPositions forever, since nothing else
                    // would ever re-trigger THAT specific key's own
                    // onGloballyPositioned again afterward — in practice
                    // this meant only whichever row happened to lay out
                    // after the Box did (typically the row nearest the
                    // press point) ever made it into the map, which is
                    // exactly the "only spreads along one row" bug. Storing
                    // the raw coordinates here and recomputing the whole
                    // keyPositions map below (in the LaunchedEffect keyed on
                    // boxCoordinates) fixes that: the moment this Box's own
                    // coordinates do arrive, every key captured so far gets
                    // converted in one pass, regardless of which order
                    // things were laid out in.
                    val rawKeyCoords = remember { mutableStateMapOf<Char, androidx.compose.ui.layout.LayoutCoordinates>() }
                    // Keyed on both boxCoordinates AND rawKeyCoords.size —
                    // the box's own coordinates typically only change once
                    // (right after the very first layout pass), but keys
                    // keep reporting their own positions over several
                    // subsequent frames as the rest of the board finishes
                    // laying out; re-running this effect as that count
                    // grows keeps catching up any key that arrived after
                    // the last time this ran, on top of the immediate
                    // best-effort conversion already done inline in
                    // onKeyPositioned below.
                    LaunchedEffect(boxCoordinates, rawKeyCoords.size) {
                        val box = boxCoordinates ?: return@LaunchedEffect
                        if (!box.isAttached) return@LaunchedEffect
                        rawKeyCoords.forEach { (ch, coords) ->
                            if (coords.isAttached) {
                                val centerInKey = Offset(coords.size.width / 2f, coords.size.height / 2f)
                                val centerInBox = box.localPositionOf(coords, centerInKey)
                                keyPositions[ch] = KeyPoint(ch, centerInBox.x, centerInBox.y)
                                keySizes[ch] = androidx.compose.ui.geometry.Size(
                                    coords.size.width.toFloat(),
                                    coords.size.height.toFloat()
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { boxCoordinates = it }
                            .let { m ->
                                // Gesture detection lives on THIS Box —
                                // the same node MainKeyboardKeys is a child
                                // of — rather than on a separate Box drawn
                                // on top of it. That distinction is the
                                // actual fix here (see the long comment
                                // this replaces, previously attached to
                                // GestureTypingOverlay's own inner Box):
                                // Compose hit-tests overlapping *siblings*
                                // by z-order and delivers every pointer
                                // event to only the topmost one — it is NOT
                                // like the Android View system where an
                                // unconsumed touch can fall through to
                                // whatever's underneath. So a separate
                                // GestureTypingOverlay Box stacked via
                                // matchParentSize() on top of
                                // MainKeyboardKeys intercepted 100% of
                                // touches in that region regardless of
                                // whether it ever called change.consume() —
                                // MainKeyboardKeys' own clickable/pointerInput
                                // modifiers were never even hit-tested, so
                                // every key looked completely dead the
                                // instant swipe typing was turned on. The
                                // only two supported ways around Compose's
                                // "one hit per pointer" rule are (a) a
                                // custom PointerInputModifierNode with
                                // sharePointerInputWithSiblings = true, or
                                // (b) not creating a competing sibling node
                                // in the first place — attaching the
                                // gesture pointerInput directly to this Box
                                // (an ancestor of MainKeyboardKeys, not a
                                // sibling of it) is (b), and needs no
                                // experimental APIs.
                                if (needsKeyPositions && !swipeTypingEnabled) {
                                    // Lightweight, read-only tap watcher:
                                    // never consumes anything
                                    // (awaitFirstDown(requireUnconsumed =
                                    // false) + no change.consume() calls) so
                                    // every tap still reaches the real key
                                    // underneath exactly as before — this
                                    // purely observes "a finger just went
                                    // down here" to record which key was hit
                                    // for RGB_RIPPLE / the LED ripple / the
                                    // Typing Animation pop-up, all three of
                                    // which key off the same pressOrigin +
                                    // pressTriggerId pair.
                                    m.pointerInput(keyPositions) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val nearest = keyPositions.values.minByOrNull { kp ->
                                                val dx = kp.x - down.position.x
                                                val dy = kp.y - down.position.y
                                                dx * dx + dy * dy
                                            }
                                            val origin = nearest?.let { Offset(it.x, it.y) } ?: down.position
                                            pressOrigin = origin
                                            pressTriggerId++
                                            lastActivityAtMs = System.currentTimeMillis()
                                        }
                                    }
                                } else if (swipeTypingEnabled) {
                                    m.pointerInput(keyPositions, currentLanguage) {
                                        // Only a currentLanguage of "si" or
                                        // "en" makes sense for gesture

                                        // matching (mix mode doesn't have a
                                        // single fixed dictionary to score
                                        // against) — mix mode swipes fall
                                        // through as an ordinary drag with
                                        // no word committed, which the logic
                                        // below still handles safely (an
                                        // empty onSwipeGesture result just
                                        // produces no candidates).
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            // Same press-origin bookkeeping
                                            // as the plain tap-watcher branch
                                            // above (see needsKeyPositions) —
                                            // swipe typing being on doesn't
                                            // mean RGB_RIPPLE/LED/Typing
                                            // Animation should stop reacting
                                            // to ordinary taps, so this needs
                                            // to happen here too rather than
                                            // only in the mutually-exclusive
                                            // branch above.
                                            if (needsKeyPositions) {
                                                val nearest = keyPositions.values.minByOrNull { kp ->
                                                    val dx = kp.x - down.position.x
                                                    val dy = kp.y - down.position.y
                                                    dx * dx + dy * dy
                                                }
                                                pressOrigin = nearest?.let { Offset(it.x, it.y) } ?: down.position
                                                pressTriggerId++
                                                lastActivityAtMs = System.currentTimeMillis()
                                            }
                                            var dragging = false
                                            val touchSlop = viewConfiguration.touchSlop

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (change.changedToUpIgnoreConsumed()) {
                                                    if (dragging) {
                                                        change.consume()
                                                        gestureIsDragging = false
                                                        val finishedPath = gesturePathPoints
                                                        gesturePathPoints = emptyList()
                                                        if (finishedPath.size >= MIN_GESTURE_POINTS && keyPositions.isNotEmpty()) {
                                                            val swipeKeyPoints = finishedPath.map { KeyPoint(' ', it.x, it.y) }
                                                            val letters = nearestLettersAlongPath(swipeKeyPoints, keyPositions)
                                                            if (letters.length >= MIN_GESTURE_LETTERS) {
                                                                gestureScope.launch {
                                                                    val candidates = onSwipeGesture(letters, currentLanguage)
                                                                    candidates.firstOrNull()?.let { onGestureWordCommitted(it) }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    break
                                                }

                                                if (!dragging) {
                                                    val travelled = (change.position - down.position).getDistance()
                                                    if (travelled > touchSlop) {
                                                        // Past the slop threshold —
                                                        // this is now unambiguously a
                                                        // swipe, not a tap. Start
                                                        // claiming the gesture from
                                                        // here on. Because this
                                                        // pointerInput lives on the
                                                        // Box ABOVE MainKeyboardKeys
                                                        // (not a sibling beside it),
                                                        // consuming here also stops
                                                        // MainKeyboardKeys' own
                                                        // clickable handlers from
                                                        // ever starting a press for
                                                        // this pointer — exactly the
                                                        // "swipe wins once it's
                                                        // clearly a swipe" behavior
                                                        // this needs, without ever
                                                        // blocking plain taps.
                                                        dragging = true
                                                        gestureIsDragging = true
                                                        gesturePathPoints = listOf(down.position, change.position)
                                                        change.consume()
                                                    }
                                                    // Still under slop: leave the
                                                    // change unconsumed so a tap
                                                    // still reaches MainKeyboardKeys.
                                                } else {
                                                    gesturePathPoints = gesturePathPoints + change.position
                                                    change.consume()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    m
                                }
                            }
                    ) {
                        MainKeyboardKeys(
                            currentLanguage = currentLanguage,
                            shift = shift, shiftLocked = shiftLocked,
                            onShiftStateChange = onShiftStateChange,
                            keyHeight = keyHeight, keyShape = keyShape,
                            bottomPadding = bottomPadding, colors = colors,
                            showKeyBorders = showKeyBorders,
                            onKey = onKey,
                            onSymbols = { pushBoard(Board.SYMBOLS) },
                            onEmojiPicker = { pushBoard(Board.EMOJI) },
                            onLangTooltip = { showLangTooltip = true },
                            imeAction = inputType,
                            onKeyPositioned = if (needsKeyPositions) { { ch, coords ->
                                // Always record the raw coordinates (see
                                // rawKeyCoords above) — this alone
                                // guarantees the key is never permanently
                                // lost even if boxCoordinates isn't ready
                                // yet, since the LaunchedEffect(boxCoordinates)
                                // above will pick it up as soon as it is.
                                rawKeyCoords[ch] = coords
                                // Also convert immediately when possible, so
                                // the common case (Box already laid out
                                // before most keys are) doesn't wait for an
                                // extra recomposition it doesn't need.
                                val box = boxCoordinates
                                if (box != null && coords.isAttached && box.isAttached) {
                                    val centerInKey = Offset(coords.size.width / 2f, coords.size.height / 2f)
                                    val centerInBox = box.localPositionOf(coords, centerInKey)
                                    keyPositions[ch] = KeyPoint(ch, centerInBox.x, centerInBox.y)
                                    keySizes[ch] = androidx.compose.ui.geometry.Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat()
                                    )
                                }
                            } } else null
                        )

                        if (rgbRippleActive) {
                            // Pure drawing layer, same non-hit-testing
                            // reasoning as GestureTypingOverlay below — this
                            // Box has no pointerInput of its own so it can
                            // sit on top of every key without blocking taps.
                            RgbRippleOverlay(
                                origin = pressOrigin,
                                triggerId = pressTriggerId,
                                keyPositions = keyPositions.values.toList(),
                                accent = colors.accent,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        if (ledActive) {
                            // Same pressOrigin/pressTriggerId as RGB_RIPPLE
                            // above — both can be active together (a
                            // KeyEffect ripple on the keys themselves, plus
                            // a separate LED glow), since they're keyed off
                            // the same shared touch state but draw distinct
                            // visuals.
                            KeyboardLedRipple(
                                pattern = ledPattern,
                                origin = pressOrigin,
                                triggerId = pressTriggerId,
                                keyPositions = keyPositions.values.toList(),
                                keySizes = keySizes,
                                accent = colors.accent,
                                isIdle = isLedIdle,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        if (typingAnimActive) {
                            // Anchored at the real touched key's position —
                            // see TypingAnimationPopup's own doc comment for
                            // why this needs keyPositionPx rather than a
                            // fixed spot on the board.
                            TypingAnimationPopup(
                                trigger = pressTriggerId,
                                animation = typingAnimation,
                                customEmoji = typingAnimationEmoji,
                                customImageBitmap = typingAnimationImageBitmap,
                                keyPositionPx = pressOrigin
                            )
                        }

                        if (swipeTypingEnabled) {
                            // Pure drawing layer now — no pointerInput of
                            // its own (see the outer Box's own modifier
                            // above for where gesture detection actually
                            // lives and why). matchParentSize() here only
                            // affects layout/drawing bounds, which is fine —
                            // it's Compose's *pointer hit-testing* of
                            // overlapping siblings that was the problem,
                            // and a Box with no pointerInput modifier is
                            // never hit-tested at all, so it can safely
                            // sit visually on top without intercepting
                            // anything.
                            GestureTypingOverlay(
                                isDragging = gestureIsDragging,
                                pathPoints = gesturePathPoints,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
            }
        }

        // FIX #11: Replaced hardcoded padding(start=96.dp, bottom=62.dp) which was
        // wrong on different keyboard heights / bottom-space settings. Now the tooltip
        // is centered horizontally and floats just above the bottom row by using
        // Alignment.BottomCenter + a single fixed vertical nudge that works at every
        // keyboard height step (the bottom row is always keyHeight + 6dp padding).
        AnimatedVisibility(
            visible = showLangTooltip,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = keyHeight + 10.dp)
                .zIndex(10f)
        ) {
            LangTooltip(currentLanguage = currentLanguage)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// Content-only composables (no toolbar — toolbar is in KeyboardView)
// ─────────────────────────────────────────────────────────────────────────────

// Private-use-area Unicode sentinels used ONLY as unique `keyPositions` map
// keys for the MAIN board's non-letter keys (Shift/Backspace/Symbols/Emoji/
// Lang-toggle/Space) — these keys have no natural single Char of their own
// the way a letter key does. Never shown, typed, committed, or matched
// against real text; U+E000-U+E005 sit in the Unicode Private Use Area,
// which is guaranteed never to collide with any real Sinhala, English, or
// symbol character. Exists so RGB_RIPPLE / the LED ripple / the Typing
// Animation pop-up can find these keys' real board positions too, same as
// they already could for every letter key.
private const val KEY_SENTINEL_SHIFT = '\uE000'
private const val KEY_SENTINEL_BACKSPACE = '\uE001'
private const val KEY_SENTINEL_SYMBOLS = '\uE002'
private const val KEY_SENTINEL_EMOJI = '\uE003'
private const val KEY_SENTINEL_LANG_TOGGLE = '\uE004'
private const val KEY_SENTINEL_SPACE = '\uE005'

@Composable
internal fun MainKeyboardKeys(
    currentLanguage: String,
    shift: Boolean,
    shiftLocked: Boolean,
    onShiftStateChange: (SinKeyInputMethodService.ShiftState) -> Unit,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    colors: KeyboardColors,
    showKeyBorders: Boolean = true,
    onKey: (String) -> Unit,
    onSymbols: () -> Unit,
    onEmojiPicker: () -> Unit,
    onLangTooltip: () -> Unit,
    imeAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_NONE,
    // Reports every letter key's on-screen center (in this Column's local
    // coordinate space) as they're laid out — GestureTypingOverlay collects
    // these into the key-position map GestureWordMatcher needs. null
    // (the default) skips all the onGloballyPositioned work entirely, so
    // this has no cost when gesture typing is off (see KeyboardView's own
    // swipeTypingEnabled gate, which is what actually decides whether a
    // non-null callback is passed in).
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // FIX #5: Pass onShiftChange so letter rows can reset one-shot shift.
        NumberedKeyRow(EnglishRows[0], topRowNumbers, shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            onKey = { onKey(it); if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF) })
        KeyRow(EnglishRows[1], shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            onKey = { onKey(it); if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF) })
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Double-tap = LOCKED, single tap = toggle ONE_SHOT
            ShiftKey(weight = 1.4f, active = shift, locked = shiftLocked,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                // Special keys (Shift/Backspace/Space/Symbols/Emoji/Lang
                // toggle) have no natural Char of their own the way a
                // letter key does, so each gets its own private-use-area
                // Unicode sentinel (U+E000..U+E005) purely as a unique map
                // key for `keyPositions` — never shown, typed, or matched
                // against anything; only used so RGB_RIPPLE/the LED ripple/
                // the Typing Animation pop-up can find these keys' real
                // positions too, the same way they already could for every
                // letter key.
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_SHIFT, coords) } },
                onTap = { onKey("SHIFT") },
                onDoubleTap = { onKey("SHIFT_LOCK") }
            )
            EnglishRows[2].forEach { k ->
                val display = if (shift) k.uppercase() else k
                // FIX #5: Reset shift after each letter (one-shot shift behaviour).
                LetterKey(
                    label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                    onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                    onTap = {
                        onKey(display)
                        if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF)
                    },
                    onAlternateSelected = { alt ->
                        onKey(alt)
                        if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF)
                    }
                )
            }
            BackspaceKey(weight = 1.4f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_BACKSPACE, coords) } }
            ) { onKey("BACKSPACE") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SymbolsKey(weight = 1.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_SYMBOLS, coords) } }
            ) { onSymbols() }
            EmojiKey(weight = 0.9f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_EMOJI, coords) } },
                onTap = { onKey(",") }, onLongPress = { onEmojiPicker() })
            Box(modifier = Modifier.weight(0.9f)) {
                LangToggleKey(currentLanguage = currentLanguage, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape,
                    onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_LANG_TOGGLE, coords) } },
                    onTap = { onKey("LANG_TOGGLE"); onLangTooltip() })
            }
            SpaceKey(weight = 5.5f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                showKeyBorders = showKeyBorders,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_SPACE, coords) } },
                onTap = { onKey("SPACE") }, onLongPress = { onKey("SWITCH_KEYBOARD") })
            SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(".") }
            // FIX #8: Pass imeAction so the Enter key label reflects the current field action.
            EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape,
                colors = colors, showKeyBorders = showKeyBorders,
                imeAction = imeAction) { onKey("ENTER") }
        }
    }
}

/**
 * Purely the visual trail drawn while a swipe is in progress — the actual
 * gesture *detection* (drag tracking, slop threshold, letter-path
 * resolution, word commit) now lives on the outer Box that wraps both this
 * composable and MainKeyboardKeys as siblings (see that Box's own
 * pointerInput for the full explanation). This composable is intentionally
 * left with no pointerInput of its own.
 *
 * That split matters for more than just code organization: Compose only
 * ever hit-tests and dispatches pointer events to the single topmost
 * composable among overlapping siblings (see "Understand gestures" in the
 * Compose docs) — regardless of whether that composable ever calls
 * change.consume(). A plain, unconsumed touch does NOT "fall through" to
 * whatever's underneath the way it would in the old Android View system.
 * This composable used to own its own pointerInput as a sibling Box
 * layered on top of MainKeyboardKeys via matchParentSize() — even after
 * that pointerInput was fixed to never consume a plain tap, every key
 * still looked completely dead the instant swipe typing was turned on,
 * because MainKeyboardKeys' own clickable/pointerInput modifiers were
 * simply never hit-tested at all while this overlay sat on top of them.
 * Removing this composable's pointerInput entirely (a Box with no
 * pointerInput modifier isn't hit-tested, so it can't intercept anything)
 * and moving gesture detection up onto the shared ancestor Box is what
 * actually fixes that; drawing the trail here on top is still fine and
 * layout-safe, since drawing and hit-testing are independent.
 */
/**
 * KeyEffect.RGB_RIPPLE: draws an expanding neon/RGB color wave, centered on
 * [origin] (the touched key's center, in this Box's local coordinates),
 * that colors every other known key position by distance from that origin
 * as the wave front passes over it, then fades. Pure drawing layer — same
 * non-hit-testing reasoning as GestureTypingOverlay (see its doc comment),
 * so it can sit on top of every key without blocking taps.
 *
 * [triggerId] is bumped by the caller on every touch-down, including two
 * consecutive touches on the exact same key — keying the restart animation
 * off triggerId rather than off `origin` itself means the wave still
 * restarts from scratch even when origin didn't change value.
 */
@Composable
private fun RgbRippleOverlay(
    origin: Offset?,
    triggerId: Int,
    keyPositions: List<KeyPoint>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (origin == null || keyPositions.isEmpty()) return

    // 0f..1f once per triggerId — represents how far the wave front has
    // traveled (as a fraction of RIPPLE_MAX_RADIUS_DP) and how much it's
    // faded, both driven off the same progress value for simplicity.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(triggerId) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(650, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
    }

    if (progress.value >= 1f) return

    Canvas(modifier = modifier) {
        val maxRadiusPx = RIPPLE_MAX_RADIUS_DP.dp.toPx()
        val waveFrontRadius = progress.value * maxRadiusPx
        // How wide the visible "band" of the wave is, in px — keys well
        // behind the front (already passed) or well ahead of it (not yet
        // reached) stay undrawn; only keys near the current front glow.
        val bandWidthPx = 0.42f * maxRadiusPx

        keyPositions.forEach { key ->
            val dx = key.x - origin.x
            val dy = key.y - origin.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val distanceFromFront = kotlin.math.abs(dist - waveFrontRadius)
            if (distanceFromFront < bandWidthPx) {
                // Closer to the exact front = brighter; also fades globally
                // as the whole wave ages (1f - progress).
                val bandStrength = 1f - (distanceFromFront / bandWidthPx)
                val overallFade = 1f - progress.value
                val alpha = (bandStrength * overallFade).coerceIn(0f, 1f)
                if (alpha > 0.02f) {
                    // Hue cycles with distance so the wave reads as a
                    // genuine RGB/rainbow ring rather than a single flat
                    // accent-colored blob.
                    val hueShift = (dist / maxRadiusPx) * 300f
                    val waveColor = rotateHue(accent, hueShift)
                    drawCircle(
                        color = waveColor.copy(alpha = alpha * 0.8f),
                        radius = 26.dp.toPx(),
                        center = Offset(key.x, key.y)
                    )
                }
            }
        }
    }
}

/** Max travel distance (dp) of the RGB_RIPPLE wave front from its origin key. */
private const val RIPPLE_MAX_RADIUS_DP = 260

@Composable
private fun GestureTypingOverlay(
    isDragging: Boolean,
    pathPoints: List<Offset>,
    modifier: Modifier = Modifier
) {
    if (isDragging && pathPoints.size >= 2) {
        Canvas(modifier = modifier) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(pathPoints.first().x, pathPoints.first().y)
                pathPoints.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = DeshGreen.copy(alpha = 0.55f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 12f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Reduces a raw swipe path down to the sequence of letters it actually
 * passed near, in order, with consecutive duplicates collapsed — e.g. a
 * path that lingers over "e" for several sampled points only contributes
 * one 'e', matching how GestureWordMatcher.idealPath also collapses
 * consecutive-duplicate letters in candidate words. This is a coarse first
 * pass (nearest key at each sample point); the real ranking happens in
 * GestureWordMatcher.match against the full path shape, not just this
 * letter sequence — this function only needs to be good enough to build a
 * plausible query string.
 */
private fun nearestLettersAlongPath(path: List<KeyPoint>, keyPositions: Map<Char, KeyPoint>): String {
    val sb = StringBuilder()
    var lastChar: Char? = null
    for (point in path) {
        var closestChar: Char? = null
        var closestDist = Float.MAX_VALUE
        for ((ch, key) in keyPositions) {
            val dx = point.x - key.x
            val dy = point.y - key.y
            val dist = dx * dx + dy * dy
            if (dist < closestDist) {
                closestDist = dist
                closestChar = ch
            }
        }
        if (closestChar != null && closestChar != lastChar) {
            sb.append(closestChar)
            lastChar = closestChar
        }
    }
    return sb.toString()
}

private const val MIN_GESTURE_POINTS = 6
private const val MIN_GESTURE_LETTERS = 2

@Composable
private fun SymbolsKeyboardKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onNumpad: () -> Unit,
    onEmoji: () -> Unit
) {
    SymbolsKeyboardView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey, onBack = onBack,
        onNumpad = onNumpad, onEmoji = onEmoji
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar — tools row OR suggestion strip depending on typing state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppsMicBar(
    colors: KeyboardColors,
    isDark: Boolean = false,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    // Non-null right after an autocorrect swap — renders as a distinct
    // "Undo" chip pinned before the regular suggestions. See KeyboardView's
    // doc comment on this same param for ownership/lifecycle.
    autocorrectUndoWord: String? = null,
    onUndoAutocorrect: () -> Unit = {},
    onKey: (String) -> Unit,
    onClipboardOpen: () -> Unit,
    onFontOpen: () -> Unit,
    onStickerOpen: () -> Unit,
    selectedFontStyle: FancyTextStyle = FancyTextStyle.NONE
) {
    // The undo chip must be able to show the strip even when there are no
    // regular suggestions to go with it — right after autocorrect commits a
    // word at SPACE, the word buffer (and therefore `suggestions`) is
    // usually empty, but the chip describing what was just corrected still
    // needs somewhere to render.
    val isTyping = suggestions.isNotEmpty() || autocorrectUndoWord != null

    // AnimatedContent was removed here — the slide/fade transition caused the
    // toolbar to render twice during WhatsApp emoji panel open/close because
    // the IME window resizes mid-animation, leaving ghost frames visible.
    //
    // FIX Ghost-Toolbar: Both branches MUST use the same fixed height (48.dp).
    // The previous code used 52.dp for suggestion strip and 44.dp for tool row.
    // That 8dp height difference caused the IME window to resize every time
    // typing started/stopped, which triggered WhatsApp's emoji panel to overlap
    // the keyboard producing the visible ghost/duplicate toolbar layer.
    if (isTyping) {
            // ── Suggestion strip ─────────────────────────────────────────
            // FIX: The grid/apps icon (TOOL_APPS) used to disappear entirely
            // while typing because this whole branch replaced the tools row.
            // Now it stays pinned on the left of the suggestion strip so the
            // user can always reach it without needing to stop typing first.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onKey("TOOL_APPS") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_unified_menu),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colors.subText
                    )
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    if (autocorrectUndoWord != null) {
                        item {
                            // Distinct pill (not a plain text chip like the
                            // suggestions below) so it visually reads as an
                            // action rather than a word to insert — tapping
                            // it reverts the correction rather than typing
                            // autocorrectUndoWord into the field.
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.subText.copy(alpha = 0.12f))
                                    .clickable { onUndoAutocorrect() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = colors.keyText
                                )
                                Text(
                                    text = "\"$autocorrectUndoWord\"",
                                    fontSize = 15.sp,
                                    color = colors.keyText,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        if (suggestions.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(colors.subText.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                    items(suggestions.size) { idx ->
                        val word = suggestions[idx]
                        // Fancy-font styling is applied to the suggestion chip's
                        // display text too, not just to committed text — otherwise
                        // a user with e.g. Bold font selected never sees that a
                        // chip like the raw-English "mix mode" word ("ba") will
                        // actually commit as its styled form ("𝐛𝐚") until after
                        // they've already tapped it. FancyTextMapper only maps
                        // a-z/A-Z/0-9, so Sinhala suggestion chips pass through
                        // completely unchanged — safe to apply unconditionally.
                        val displayWord = FancyTextMapper.apply(word, selectedFontStyle)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSuggestionSelected(word) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = displayWord, fontSize = 19.sp, color = colors.keyText, maxLines = 1)
                        }
                        if (idx < suggestions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(1.dp)
                                    .background(colors.subText.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        } else {
            // ── Full tools row ───────────────────────────────────────────
            val tools = listOf(
                R.drawable.ic_unified_menu  to "TOOL_APPS",
                R.drawable.ic_sticker       to "TOOL_STICKER",
                R.drawable.ic_clipboard     to "TOOL_CLIPBOARD",
                R.drawable.ic_custom_font   to "TOOL_FONT",
                R.drawable.ic_translation   to "TOOL_TRANSLATE",
                R.drawable.ic_settings      to "TOOL_SETTINGS"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (iconRes, action) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                // TOOL_CLIPBOARD / TOOL_FONT open their own boards (handled by
                                // the parent KeyboardView) instead of going through onKey.
                                // TOOL_CLIPBOARD previously instantly pasted only whatever was
                                // on the system clipboard *right now*, with no way to reach
                                // anything copied earlier. TOOL_FONT previously did nothing at
                                // all (logged "not yet implemented"). Other tool actions still
                                // go through onKey as before.
                                when (action) {
                                    "TOOL_CLIPBOARD" -> onClipboardOpen()
                                    "TOOL_FONT" -> onFontOpen()
                                    "TOOL_STICKER" -> onStickerOpen()
                                    else -> onKey(action)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = colors.subText
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        // Mic button keeps its pill background regardless of
                        // showKeyBorders — it's a distinct action button, not
                        // a regular key, so it shouldn't go flat/invisible.
                        .background(
                            if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
                        )
                        .clickable { onKey("TOOL_MIC") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_microphone_normal),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colors.subText
                    )
                }
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emoji row — recent-emoji strip shown above the main typing keyboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmojiRow(emojis: List<String>, colors: KeyboardColors, onKey: (String) -> Unit, onMoreClick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(colors.bg).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        state = rememberLazyListState()
    ) {
        item { Spacer(modifier = Modifier.width(4.dp)) }
        items(emojis.size) { index ->
            val emoji = emojis[index]
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onKey(emoji) },
                contentAlignment = Alignment.Center
            ) { Text(text = emoji, fontSize = 22.sp) }
        }
        item {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) { Text(text = "•••", fontSize = 14.sp, color = colors.subText) }
        }
        item { Spacer(modifier = Modifier.width(4.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Update banner — temporarily replaces the recent-emoji strip above the main
// keyboard when UpdateChecker finds a newer versionCode in the remote JSON.
// Sized identically to EmojiRow (44dp total: 40dp content + 2dp top/bottom
// padding) so swapping between the two never shifts or resizes anything
// else on the board.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UpdateBanner(
    colors: KeyboardColors,
    onOpenClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 2.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialKeyBg)
            .clickable { onOpenClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎉 New update available — tap to download",
            fontSize = 13.sp,
            color = colors.keyText,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDismissClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.subText
            )
        }
    }
}

/**
 * Opens [url] in the device's default browser. Launched from a Service
 * context (the IME, not an Activity), so FLAG_ACTIVITY_NEW_TASK is required
 * — without it, starting an Activity from a non-Activity context throws
 * android.util.AndroidRuntimeException at runtime.
 */
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("KeyboardView", "Could not open update URL: $url", e)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LangTooltip(currentLanguage: String) {
    val label = when (currentLanguage) {
        "en" -> "English enabled"
        "mix" -> "Mix (සිංහල+English) enabled"
        else -> "සිංහල enabled"
    }
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(if (currentLanguage == "mix") Color.Black else DeshGreen)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓ ", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = when (currentLanguage) {
                    "en" -> buildAnnotatedStringBold("English", " enabled")
                    "mix" -> buildAnnotatedStringBold("Mix", " (සිංහල+English) enabled")
                    else -> buildAnnotatedStringBold("සිංහල", " enabled")
                },
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun buildAnnotatedStringBold(bold: String, normal: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
        append(bold)
        pop()
        append(normal)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Key rows
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberedKeyRow(
    keys: List<String>,
    numbers: List<String>,
    shift: Boolean,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    // (keyChar, coordinates) -> Unit — see LetterKey's onPositioned doc.
    // keyChar is the *unshifted* lowercase char, since GestureWordMatcher
    // always matches against lowercase candidate words.
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEachIndexed { index, k ->
            val display = if (shift) k.uppercase() else k
            val num = numbers.getOrNull(index) ?: ""
            NumberedLetterKey(
                label = display, number = num, weight = 1f,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                onTap = { onKey(display) },
                onLongPress = { onKey(num) }
            )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    shift: Boolean,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(
                label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                onTap = { onKey(display) },
                onAlternateSelected = { alt -> onKey(alt) }
            )
        }
        Box(modifier = Modifier.weight(0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual keys
// ─────────────────────────────────────────────────────────────────────────────

// Bug 3 Fix: Key label font sizes were hardcoded to 22sp regardless of the
// keyboard height setting. Small keys (42dp) look cramped at 22sp and large
// keys (62dp) have too much empty space. Scale font proportionally to keyHeight.
private fun keyLabelFontSize(keyHeight: Dp): androidx.compose.ui.unit.TextUnit =
    (keyHeight.value * 0.50f).sp   // ~21sp @ 42dp, ~24sp @ 48dp, ~27sp @ 54dp, ~31sp @ 62dp

private fun keyNumberFontSize(keyHeight: Dp): androidx.compose.ui.unit.TextUnit =
    (keyHeight.value * 0.25f).sp   // ~10sp @ 42dp, ~12sp @ 48dp, ~15sp @ 62dp

/**
 * Animates a quick "bump" scale for key-press feedback: snaps down fast on press,
 * springs back up with a slight overshoot on release.
 */
@Composable
/**
 * The key's press-down scale bump. Matches FlorisBoard's actual feel for
 * fast typing: FlorisBoard's own key rendering (ime/text/keyboard/
 * TextKeyboardLayout.kt) doesn't animate the pressed state at all — it's
 * a Canvas-drawn View that flips `key.isPressed = true/false` and
 * repaints on the very next frame, so there's no transition curve that
 * can ever fall behind. A spring-based animation, however fast, still has
 * a nonzero settle time; during fast typing (taps arriving well under
 * 100ms apart) a new press can start before the previous key's spring
 * finished settling, which reads as the key's press-down feeling "late"
 * or the visual bump "not keeping up" with the finger — this is what
 * showed up as a complaint about vibration/animation not matching fast
 * typing speed.
 *
 * Fix: press-DOWN now uses a very short (40ms) linear tween instead of a
 * spring — short enough that even at typing speeds well beyond normal
 * human tapping (multiple presses within 40ms of each other), the
 * previous key's down-transition has already finished or is imperceptibly
 * close to finished before a new one can start, so it never visibly
 * "lags". Release still uses the original bouncier spring, since the
 * release animation isn't gating anything time-sensitive — it plays out
 * after the character's already been typed, so a little extra visual
 * flourish there doesn't cost anything.
 */
@Composable
private fun rememberKeyBumpScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = if (pressed) {
            tween(durationMillis = 40, easing = LinearOutSlowInEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "keyBumpScale"
    )
    return scale
}

/**
 * Companion to [rememberKeyBumpScale]: how far (in dp, always >= 0) the key
 * visually sinks downward while pressed, springing back up on release —
 * the physical "key gets pushed down and pops back" feel real mechanical/
 * on-screen keyboards have, on top of (not instead of) the existing scale
 * shrink. Each call site already keys this off its own local `pressed`
 * boolean (see e.g. LetterKey/ShiftKey/BackspaceKey), so — same as the
 * scale animation — every key's bump is fully independent: two keys
 * pressed in quick succession, or the same key pressed again before its
 * previous release animation finished, each get their own correct
 * down-then-up motion rather than sharing one animation that would glitch
 * or restart oddly under fast typing.
 *
 * Press-down uses the same short linear tween as rememberKeyBumpScale, for
 * the same reason (see that function's doc comment) — kept in sync so the
 * scale-shrink and the downward offset always finish their press-down
 * transition at exactly the same moment rather than drifting apart under
 * fast typing.
 */
@Composable
private fun rememberKeyBumpOffsetY(pressed: Boolean): Dp {
    val offsetY by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = if (pressed) {
            tween(durationMillis = 40, easing = LinearOutSlowInEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "keyBumpOffsetY"
    )
    return offsetY
}

/**
 * Shows the character preview bubble for [minVisibleMs] every time
 * [pressTick] changes, regardless of how long the underlying physical
 * press actually lasted.
 *
 * Originally this was keyed off a live `pressed: Boolean` and stayed
 * visible for "at least minVisibleMs, or until release, whichever is
 * longer" — the intent being that a real key-preview bubble's fade-out
 * runs on its own timer, not hard-tied to finger-up. In practice that
 * still required `pressed` to reach a composed frame as `true` at all,
 * and during fast typing the down+up pair can complete in under one
 * frame — clickable's own gesture detector and this composable's
 * pointerInput block are both racing the same events, and on a fast tap
 * the up can land before Compose ever renders the intermediate
 * pressed=true state. The bubble would then never trigger at all, not
 * even briefly — reproducing exactly as "works on a slow deliberate tap,
 * never shows during normal-speed typing".
 *
 * Fixed by decoupling entirely from `pressed` surviving into a frame:
 * the call site now bumps [pressTick] the instant `awaitFirstDown` fires,
 * before `pressed` is even set — a plain state write Compose is
 * guaranteed to observe as a rising edge on the very next recomposition,
 * independent of how quickly the matching up event follows. The bubble's
 * visible window is now unconditionally minVisibleMs long, timed purely
 * from the down event, which better matches how Gboard/SwiftKey behave
 * anyway (their preview bubble length doesn't visibly depend on press
 * duration either).
 */
@Composable
private fun rememberPreviewVisible(pressTick: Int, minVisibleMs: Long = 150L): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(pressTick) {
        if (pressTick == 0) return@LaunchedEffect
        visible = true
        delay(minVisibleMs)
        visible = false
    }
    return visible
}

@Composable
private fun KeyPreviewPopup(label: String, keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape) {
    val size = (keyHeight.value * 1.1f).dp
    // Real keyboards (Gboard/SwiftKey) pop this bubble in with a quick
    // scale+fade rather than having it snap fully-formed into place, which
    // reads as more "alive"/responsive even though the key itself already
    // has its own bump animation. Animate from freshly-composed (this
    // popup is created and destroyed each press, so `remember`ing false
    // and flipping true right after first composition via LaunchedEffect
    // is what actually triggers the transition — starting `true`
    // immediately would skip it, since animateFloatAsState only animates
    // on subsequent target changes).
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val animScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "keyPreviewScale"
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyPreviewAlpha"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animScale
                scaleY = animScale
                alpha = animAlpha
                // Scale from the bottom edge (closest to the key/finger)
                // rather than dead-center, so the bubble reads as growing
                // up out of the key instead of expanding from its own
                // middle — matches how real keyboard preview bubbles move.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(keyShape)
            .background(colors.keyBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (keyHeight.value * 0.60f).sp,
            color = colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * The row of alternate characters shown when a letter key is long-pressed
 * (e.g. long-pressing "a" shows "æ ã å ā à á â ä"). Ported behavior-wise
 * from FlorisBoard's popup interaction (ime/popup/PopupUiController.kt):
 * the row appears directly above the key, the currently-highlighted
 * character follows the finger as it drags left/right across the row
 * (clamped to the row's bounds — dragging past either end just keeps the
 * end item selected rather than dismissing), and lifting the finger
 * commits whichever character is highlighted at that moment. The row is
 * always shown, but with a single character (the key's own base label)
 * when there are no popup alternates for a key so callers don't need to
 * branch — see `alternates` handling in LetterKey below for when this is
 * actually invoked.
 *
 * @param alternates the characters to show, in display order, left to right.
 * @param keyHeight used to size each cell to roughly match the real key
 *   below it, so the row doesn't look mismatched in scale.
 * @param onSelectionChange called with the index currently under the
 *   finger every time it changes, purely so LetterKey can highlight it.
 * @param onCommit called once, when the finger lifts, with the character
 *   that was highlighted at that moment.
 */
@Composable
private fun LongPressPopupRow(
    alternates: List<String>,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    selectedIndex: Int,
) {
    val cellSize = (keyHeight.value * 0.95f).dp
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val animScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "popupRowScale"
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "popupRowAlpha"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animScale
                scaleY = animScale
                alpha = animAlpha
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(keyShape)
            .background(colors.keyBg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        alternates.forEachIndexed { index, alt ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .clip(RoundedCornerShape((cellSize.value * 0.3f).dp))
                    .background(if (isSelected) colors.accent.copy(alpha = 0.35f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = alt,
                    fontSize = (cellSize.value * 0.5f).sp,
                    color = colors.keyText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.NumberedLetterKey(
    label: String, number: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var pressTick by remember { mutableStateOf(0) } // see LetterKey for why the preview triggers off this, not `pressed`
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    val popScale = keyPopScaleMultiplier(pressed, colors.keyEffect)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale * popScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .keyEffectDecoration(colors, keyShape)
            .keyRippleEffect(colors, pressed)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    pressTick++
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
    ) {
        Text(text = number, fontSize = keyNumberFontSize(keyHeight), color = colors.subText,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center))
        if (rememberPreviewVisible(pressTick)) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@Composable
private fun RowScope.LetterKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    // Reports this key's center in the root keyboard's coordinate space
    // whenever it's laid out/moved — used only by GestureTypingOverlay
    // (see KeyboardKeys' gestureKeyPositions state) to build the key-center
    // map GestureWordMatcher needs. Left null everywhere gesture typing
    // isn't relevant (symbol/numpad keys, previews) so this stays a no-op
    // there instead of needing every LetterKey call site updated.
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit,
    // Called instead of onTap when the user long-presses this key, drags
    // to one of the popup alternates, and releases over it. Left null at
    // call sites that don't want the feature (kept optional rather than
    // wiring it through every LetterKey call site at once).
    onAlternateSelected: ((String) -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    // BUG FIX (preview bubble doesn't show during fast typing): `pressed`
    // used to be driven only from the awaitEachGesture block below, which
    // runs on the SAME pointerInput pass as `.clickable`'s own internal
    // gesture detector — both are racing to react to the same down/up
    // events. On a slow, deliberate tap there's enough time between down
    // and up for `pressed = true` to actually reach a composition and for
    // rememberPreviewVisible's LaunchedEffect to start before the up event
    // arrives. During fast typing, the up event can follow the down event
    // by less than one frame — clickable's own detector can finish the
    // whole gesture and this block's `pressed = true` / `pressed = false`
    // can both apply before Compose ever composes an intermediate frame
    // with pressed=true, so rememberPreviewVisible's rising-edge detection
    // never observes a true value at all and the bubble silently never
    // appears — exactly the "works on a slow tap, never shows when typing
    // normally" symptom. Fix: bump a dedicated counter the INSTANT a down
    // event is detected, in the same place, so the preview's trigger no
    // longer depends on `pressed` surviving into a rendered frame — only
    // the raw fact that a down event happened has to reach state, which
    // awaitFirstDown always sees regardless of how quickly up follows.
    var pressTick by remember { mutableStateOf(0) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    val popScale = keyPopScaleMultiplier(pressed, colors.keyEffect)

    // Long-press popup (accented alternates — see LongPressPopupData.kt).
    // Looked up from the key's own base (lowercase) character; empty when
    // this key has no alternates, in which case the popup path below is
    // never entered at all and behavior is identical to before this
    // feature existed.
    val alternates = remember(label) {
        longPressPopupAlternates[label.lowercase().firstOrNull() ?: ' ']
            ?.let { alts -> if (label.firstOrNull()?.isUpperCase() == true) alts.map { it.uppercase() } else alts }
            ?: emptyList()
    }
    var popupVisible by remember { mutableStateOf(false) }
    var selectedAltIndex by remember { mutableStateOf(0) }
    // The key's own on-screen width, needed to translate a drag offset
    // (in px, relative to where the finger went down) into "how many
    // popup-row cells has the finger moved across". Updated by
    // onSizeChanged below; read from the gesture loop.
    var keyWidthPx by remember { mutableStateOf(0) }
    // Cell width used both here and in LongPressPopupRow — kept as one
    // shared calculation so the two never drift out of sync with each
    // other (2.dp horizontal spacing between cells, matching the Row spacedBy
    // in LongPressPopupRow above).
    val density = LocalDensity.current
    val cellStridePx = with(density) { ((keyHeight.value * 0.95f).dp + 2.dp).toPx() }

    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale * popScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .keyEffectDecoration(colors, keyShape)
            .keyRippleEffect(colors, pressed)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .onSizeChanged { keyWidthPx = it.width }
            .pointerInput(alternates) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    pressTick++
                    popupVisible = false
                    selectedAltIndex = 0

                    // Race a long-press timer against the finger lifting.
                    // withTimeoutOrNull returns null (timed out — meaning
                    // the long-press threshold was reached while still
                    // down) or the UP event (finger lifted before the
                    // threshold). This mirrors what .clickable/detectTapGestures
                    // do internally, but written out explicitly because we
                    // need to keep tracking drag position *after* the
                    // long-press fires, which clickable's own long-press
                    // callback doesn't support.
                    val longPressThresholdMs = android.view.ViewConfiguration.getLongPressTimeout().toLong()
                    // withTimeoutOrNull's own return value is null only
                    // when it timed out (long-press threshold reached
                    // while still down) — proceed to long-press/popup
                    // handling below in that case. A non-null Boolean
                    // means the finger was released (true) or the gesture
                    // was cancelled, e.g. scrolled away (false) before
                    // that threshold — an ordinary tap either way, just
                    // only actually type the character if it wasn't
                    // cancelled.
                    val releasedBeforeLongPress: Boolean? = withTimeoutOrNull(longPressThresholdMs) {
                        waitForUpOrCancellation() != null
                    }

                    if (releasedBeforeLongPress != null) {
                        // Ordinary tap — released (or cancelled) before
                        // the long-press threshold. Behaves exactly as it
                        // always has.
                        pressed = false
                        if (releasedBeforeLongPress) {
                            onTap()
                        }
                        return@awaitEachGesture
                    }

                    // Long-press threshold reached while still down. Only
                    // actually show the popup if this key has alternates
                    // to offer — otherwise fall through to normal
                    // press-and-hold-then-release-still-taps behavior,
                    // unchanged from before this feature existed.
                    if (alternates.isNotEmpty()) {
                        popupVisible = true
                    }

                    // Keep tracking the finger (drag across the popup row
                    // to change selection) until it's lifted or cancelled.
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Finger lifted. Commit whichever alternate is
                            // currently selected, if the popup was showing
                            // at all — otherwise this was a long-press on
                            // a key with no alternates, which still just
                            // types the base character (matches existing
                            // long-press-does-nothing-special behavior for
                            // those keys).
                            if (popupVisible && alternates.isNotEmpty()) {
                                val chosen = alternates.getOrNull(selectedAltIndex)
                                if (chosen != null && onAlternateSelected != null) {
                                    onAlternateSelected(chosen)
                                } else {
                                    onTap()
                                }
                            } else {
                                onTap()
                            }
                            pressed = false
                            popupVisible = false
                            change.consume()
                            break
                        }
                        if (popupVisible && alternates.isNotEmpty() && keyWidthPx > 0) {
                            // The popup row is centered above the key. To
                            // find which cell is under the finger: take
                            // the finger's x position (relative to this
                            // key's own left edge, which is what
                            // change.position.x already is, since we're
                            // inside this key's own pointerInput), shift
                            // it into the row's coordinate space by
                            // correcting for the row being wider/narrower
                            // than the key and centered rather than
                            // left-aligned, then divide by one cell's
                            // stride to get an index.
                            val rowWidthPx = cellStridePx * alternates.size
                            val keyCenterPx = keyWidthPx / 2f
                            val rowLeftEdgePx = keyCenterPx - rowWidthPx / 2f
                            val posInRowPx = change.position.x - rowLeftEdgePx
                            val rawIndex = (posInRowPx / cellStridePx).toInt()
                            selectedAltIndex = rawIndex.coerceIn(0, alternates.lastIndex)
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal)
        if (popupVisible && alternates.isNotEmpty()) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                LongPressPopupRow(
                    alternates = alternates,
                    keyHeight = keyHeight,
                    colors = colors,
                    keyShape = keyShape,
                    selectedIndex = selectedAltIndex
                )
            }
        } else if (rememberPreviewVisible(pressTick)) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.ShiftKey(
    weight: Float, active: Boolean, locked: Boolean,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    val bg = when {
        locked -> DeshGreen.copy(alpha = 0.85f)
        active -> colors.specialKeyBg.copy(alpha = 0.7f)
        else   -> colors.specialKeyBg
    }
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape).background(bg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .combinedClickable(
                onClick = onTap,
                onDoubleClick = onDoubleTap
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (active) R.drawable.ic_shift_key_shifted else R.drawable.ic_shift_key),
            contentDescription = if (locked) "Caps Lock" else "Shift",
            modifier = Modifier.size(26.dp),
            tint = if (active || locked) Color.White else colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape).background(colors.specialKeyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        pressed = true
                        val longPressDelay = 400L
                        val repeatInterval = 50L
                        val released = withTimeoutOrNull(longPressDelay) { tryAwaitRelease() }
                        if (released != null) {
                            onTap()
                        } else {
                            onTap()
                            try {
                                while (true) {
                                    delay(repeatInterval)
                                    onTap()
                                    val done = withTimeoutOrNull(1L) { tryAwaitRelease() }
                                    if (done != null) break
                                }
                            } catch (_: Exception) { }
                        }
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_backspace),
            contentDescription = "Backspace",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.SpecialKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var pressTick by remember { mutableStateOf(0) } // see LetterKey for why the preview triggers off this, not `pressed`
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    pressTick++
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), fontWeight = FontWeight.Medium, color = colors.specialKeyText)
        if (rememberPreviewVisible(pressTick)) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@Composable
private fun RowScope.SymbolsKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back_to_symbols),
            contentDescription = "Symbols",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun LangToggleKey(
    currentLanguage: String,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit
) {
    val isSinhala = currentLanguage == "si"
    val isMix = currentLanguage == "mix"
    val indicatorColor = when (currentLanguage) {
        "si" -> DeshGreen
        "mix" -> colors.specialKeyText
        else -> Color.Transparent
    }
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).fillMaxWidth()
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_native_letter),
                contentDescription = "Language",
                modifier = Modifier.size(18.dp),
                tint = if (isSinhala || isMix) indicatorColor else colors.specialKeyText
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp).height(2.dp).width(18.dp)
                    .background(
                        color = indicatorColor,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.EmojiKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                contentDescription = "Emoji",
                modifier = Modifier.size(20.dp),
                tint = colors.specialKeyText
            )
            Text(text = ",", fontSize = 10.sp, color = colors.specialKeyText, lineHeight = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SpaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    showKeyBorders: Boolean = true,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    // Desh-style pill shape only when borders are hidden; bordered mode keeps
    // the standard key corner radius like every other key.
    val spaceShape = if (showKeyBorders) keyShape else RoundedCornerShape(keyHeight / 2)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(spaceShape)
            .background(if (pressed) colors.spaceKeyBg.copy(alpha = 0.6f) else colors.spaceKeyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sinkey board", fontSize = 12.sp,
            color = colors.spaceKeyText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * FIX #8: EnterKey previously had a [useSearchIcon] flag that was only set
 * for the dial-pad, while the main keyboard always showed the generic enter
 * icon regardless of the current IME action (Search, Send, Go, Next, Done…).
 *
 * Now accepts [imeAction] derived from [EditorInfo.imeOptions] so the icon/
 * label correctly reflects what the action will do in the focused field.
 * Callers that don't pass imeAction fall back to the generic enter icon.
 */
@Composable
private fun RowScope.EnterKey(
    weight: Float,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    colors: KeyboardColors? = null,
    showKeyBorders: Boolean = true,
    imeAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_NONE,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)

    if (!showKeyBorders) {
        // Desh-style enter key (borders hidden): filled circle with a
        // checkmark icon for the default action.
        val circleSize = keyHeight * 0.72f
        Box(
            modifier = Modifier
                .height(keyHeight).weight(weight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .scale(bumpScale)
                    .offset(y = bumpOffsetY)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (pressed) DeshGreen.copy(alpha = 0.8f) else DeshGreen)
                    .clickable { onTap() }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            waitForUpOrCancellation()
                            pressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (imeAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> Text(
                        "Send", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_GO -> Text(
                        "Go", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> Text(
                        "Next", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Enter",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }
        }
        return
    }

    // Original bordered-mode enter key: rounded-square key shape, filled
    // green, generic enter/action icon or label.
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) DeshGreen.copy(alpha = 0.8f) else DeshGreen)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (imeAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
            android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> Text(
                "Send", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_GO -> Text(
                "Go", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> Text(
                "Next", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> Text(
                "Done", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            else -> Icon(
                painter = painterResource(id = R.drawable.ic_enter_key),
                contentDescription = "Enter",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Symbols Keyboard  (Desh-exact from APK XML analysis)
// ─────────────────────────────────────────────────────────────────────────────

// rowkeys_symbols1.xml  → keyspec_symbols_0..9
private val SymRow1 = listOf("1","2","3","4","5","6","7","8","9","0")
// rowkeys_symbols2.xml  → @ # ₹(mainCurrencyKey) % & * - = ( )
private val SymRow2 = listOf("@","#","₹","%","&","*","-","=","(",")")
// rowkeys_symbols3.xml  → ! " ' : + / ?
private val SymRow3 = listOf("!","\"","'",":","+","/","?")

// rowkeys_symbols_shift1.xml  → ~ ` _ ° ± ´ × ÷ • √
private val SymShiftRow1 = listOf("~","`","_","°","±","´","×","÷","•","√")
// rowkeys_symbols_shift2.xml  → ^ ₩ £ € ¥ $ © ® ™ π
private val SymShiftRow2 = listOf("^","₩","£","€","¥","$","©","®","™","π")
// rowkeys_symbols_shift3.xml  → \ | < > ; ¡ ¿
private val SymShiftRow3 = listOf("\\","|","<",">",";","¡","¿")

// FIX #7: Removed dead SymbolsKeyboardContent wrapper. It accepted an
// onShowEmoji parameter but never forwarded it to SymbolsKeyboardView,
// silently discarding the callback. SymbolsKeyboardView is called directly
// everywhere, so this wrapper had no callers and no purpose.

@Composable
private fun SymbolsKeyboardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onNumpad: () -> Unit,  // parent pushes NUMPAD onto back-stack
    onEmoji: () -> Unit    // parent pushes EMOJI onto back-stack
) {
    var shifted by remember { mutableStateOf(false) }
    // Bug fix: removed local showNumpad / showEmojiFromSymbols booleans.
    // Navigation is now delegated to the parent back-stack so that back()
    // from Numpad or Emoji returns to SYMBOLS, not MAIN.

    val row1 = if (shifted) SymShiftRow1 else SymRow1
    val row2 = if (shifted) SymShiftRow2 else SymRow2
    val row3 = if (shifted) SymShiftRow3 else SymRow3

    Column(
        modifier = Modifier.fillMaxWidth().background(colors.bg)
    ) {
        // ── Key rows ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .padding(bottom = bottomPadding)
        ) {
            // Row 1: numbers / shift-symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row1.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 2: symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 3: shift-toggle + symbols + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // <\> shift key  (ic_back_to_symbols drawable)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.4f)
                        .clip(keyShape)
                        .background(if (shifted) DeshGreen else colors.specialKeyBg)
                        .clickable { shifted = !shifted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_to_symbols),
                        contentDescription = "Shift",
                        modifier = Modifier.size(22.dp),
                        tint = if (shifted) Color.White else colors.specialKeyText
                    )
                }
                row3.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
                BackspaceKey(weight = 1.4f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
            }

            // Bottom row  (row_symbols_bottom.xml)
            // toLatinFromSymbolsKeyStyle | comma | toEmojiKeyStyle | space | toNumpadKeyStyle | . | enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABC  (toLatinFromSymbolsKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.8f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ABC", fontSize = 14.sp,
                        color = colors.specialKeyText, fontWeight = FontWeight.Medium)
                }
                // , (comma_key)
                SpecialKey(label = ",", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(",") }
                // Emoji  (toEmojiKeyStyle → ic_emoji_for_compose)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(0.9f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onEmoji() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                        contentDescription = "Emoji",
                        modifier = Modifier.size(22.dp),
                        tint = colors.specialKeyText
                    )
                }
                // Space  (spaceKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(5.5f)
                        .clip(keyShape).background(colors.spaceKeyBg)
                        .clickable { onKey("SPACE") },
                    contentAlignment = Alignment.Center
                ) { }
                // 12/34  (toNumpadKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.0f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onNumpad() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "12\n34", fontSize = 11.sp, color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                // .
                SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(".") }
                // Enter  (enterKeyStyle)
                EnterKey(weight = 2.0f, keyHeight = keyHeight,
                    keyShape = keyShape) { onKey("ENTER") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number Pad  (phone-style 3×3 + bottom row)
// Layout matches screenshot: 1 2 3 / 4 5 6 / 7 8 9 / . 0 _ with ABC, , ⌫ Enter on right
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onBoardStackChange: (List<Board>) -> Unit = {}
) {
    // ── Numpad grid ─────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Row 1: 1  2  3  │ ABC
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("1", keyHeight, colors, keyShape) { onKey("1") }
                NumpadDigitKey("2", keyHeight, colors, keyShape) { onKey("2") }
                NumpadDigitKey("3", keyHeight, colors, keyShape) { onKey("3") }
                // ABC — go directly to MAIN board (not just pop to SYMBOLS)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBoardStackChange(listOf(Board.MAIN)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        fontSize = 14.sp,
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Row 2: 4  5  6  │ ,
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("4", keyHeight, colors, keyShape) { onKey("4") }
                NumpadDigitKey("5", keyHeight, colors, keyShape) { onKey("5") }
                NumpadDigitKey("6", keyHeight, colors, keyShape) { onKey("6") }
                // comma
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onKey(",") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ",",
                        fontSize = keyLabelFontSize(keyHeight),
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Row 3: 7  8  9  │ ⌫
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("7", keyHeight, colors, keyShape) { onKey("7") }
                NumpadDigitKey("8", keyHeight, colors, keyShape) { onKey("8") }
                NumpadDigitKey("9", keyHeight, colors, keyShape) { onKey("9") }
                // Backspace
                BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                    onKey("BACKSPACE")
                }
            }

            // Row 4: .  0  _  │ Enter (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // . (decimal / period)
                NumpadDigitKey(".", keyHeight, colors, keyShape) { onKey(".") }
                // 0
                NumpadDigitKey("0", keyHeight, colors, keyShape) { onKey("0") }
                // _ (underscore)
                NumpadDigitKey("_", keyHeight, colors, keyShape) { onKey("_") }
                // Enter — green
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
            }
        }
}

/** A single large numpad digit/symbol key. */
@Composable
private fun RowScope.NumpadDigitKey(
    label: String,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(1f)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .keyEffectDecoration(colors, keyShape)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = keyLabelFontSize(keyHeight),
            color = colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clipboard history — persistent list of everything the user has copied
// (system-wide, captured by SinKeyInputMethodService's clipboard listener).
// Tap a row to paste it, tap the pin to keep it pinned to the top (pinned
// entries survive "Clear all"), tap the trash icon on a row to remove just
// that entry.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ClipboardHistoryView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    // The REAL measured height of MainKeyboardKeys' content (see
    // measuredMainContentHeight in KeyboardView) plus however much of the
    // toolbar/emoji-row above it this board reclaims by hiding them —
    // passed in rather than recomputed from row-padding math, which drifted
    // by a dp or two from the actual rendered height due to rounding.
    targetContentHeight: Dp,
    history: List<ClipEntity>,
    onPaste: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
        // Header: back arrow, title, clear-all
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
            Text(
                text = "Clipboard",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            if (history.any { !it.pinned }) {
                Text(
                    text = "Clear all",
                    fontSize = 13.sp,
                    color = colors.subText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClearAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing copied yet",
                    fontSize = 13.sp,
                    color = colors.subText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(history, key = { it.text }) { clip ->
                    ClipRow(clip = clip, colors = colors, onPaste = onPaste, onTogglePin = onTogglePin, onDelete = onDelete)
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun ClipRow(
    clip: ClipEntity,
    colors: KeyboardColors,
    onPaste: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBg)
            .clickable { onPaste(clip.text) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = clip.text,
            fontSize = 14.sp,
            color = colors.keyText,
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onTogglePin(clip.text, !clip.pinned) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (clip.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (clip.pinned) "Unpin" else "Pin",
                modifier = Modifier.size(16.dp),
                tint = if (clip.pinned) DeshGreen else colors.subText
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDelete(clip.text) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp),
                tint = colors.subText
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Font picker — Board.FONT. Same layout pattern as ClipboardHistoryView:
// hides the toolbar/emoji-row above it and reclaims that space so switching
// boards never resizes the keyboard.
//
// Each row previews a FancyTextStyle (see FancyTextMapper) using the exact
// Unicode substitution that will be committed to the target app — not a
// Compose FontFamily, which (as the old implementation showed) only affects
// how the keyboard draws its own labels and has no effect on typed text.
// English text only; Sinhala has no equivalent styled-Unicode block.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FontPickerView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    selectedFontKey: String,
    onFontSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp
    val captionHeight = 20.dp

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
            Text(
                text = "Font",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Text(
            text = "Applies to English text only",
            fontSize = 11.sp,
            color = colors.subText,
            modifier = Modifier.height(captionHeight).padding(horizontal = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(FancyTextStyle.entries.toList(), key = { it.key }) { font ->
                FontRow(
                    font = font,
                    selected = font.key == selectedFontKey,
                    colors = colors,
                    onSelect = { onFontSelected(font.key) }
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun FontRow(
    font: FancyTextStyle,
    selected: Boolean,
    colors: KeyboardColors,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DeshGreen.copy(alpha = 0.15f) else colors.cardBg)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = font.label,
                fontSize = 13.sp,
                color = colors.subText,
            )
            Text(
                // Real Unicode substitution — this is exactly what gets typed
                // into the target app when this style is selected, not just a
                // keyboard-side preview approximation.
                text = FancyTextMapper.apply("Hello World", font),
                fontSize = 17.sp,
                color = colors.keyText,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DeshGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phone Dial Pad  (auto-shown when system sends TYPE_CLASS_PHONE)
// Layout: 1 / 2 ABC / 3 DEF / 4 GHI / 5 JKL / 6 MNO
//          7 PQRS / 8 TUV / 9 WXYZ / *# / 0+ / _ / Search(green)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadContent(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors,
        keyHeight = keyHeight,
        keyShape = keyShape,
        bottomPadding = bottomPadding,
        onKey = onKey
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    val dialKeys = listOf(
        Triple("1", "", "1"),
        Triple("2", "ABC", "2"),
        Triple("3", "DEF", "3"),
        Triple("4", "GHI", "4"),
        Triple("5", "JKL", "5"),
        Triple("6", "MNO", "6"),
        Triple("7", "PQRS", "7"),
        Triple("8", "TUV", "8"),
        Triple("9", "WXYZ", "9")
    )

    // ── Grid ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Rows 1-3: 1  2ABC  3DEF │ -   /   4GHI  5JKL  6MNO │ .   /   7PQRS  8TUV  9WXYZ │ ⌫
            val sideKeys = listOf("-", ".", null) // right-side special keys per row
            dialKeys.chunked(3).forEachIndexed { rowIdx, trio ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trio.forEach { (digit, sub, key) ->
                        Box(
                            modifier = Modifier
                                .height(keyHeight).weight(1f)
                                .clip(keyShape).background(colors.keyBg)
                                .keyEffectDecoration(colors, keyShape)
                                .clickable { onKey(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = digit,
                                    fontSize = keyLabelFontSize(keyHeight),
                                    color = colors.keyText,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 28.sp
                                )
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        fontSize = 9.sp,
                                        color = colors.keyText.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    // Right-side key
                    when (rowIdx) {
                        0 -> { // -
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey("-") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = keyLabelFontSize(keyHeight), color = colors.specialKeyText)
                            }
                        }
                        1 -> { // .
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey(".") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(".", fontSize = keyLabelFontSize(keyHeight), color = colors.specialKeyText)
                            }
                        }
                        2 -> { // ⌫
                            BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                                onKey("BACKSPACE")
                            }
                        }
                    }
                }
            }

            // Row 4: *#  /  0+  /  _  │ Search (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // *# — tap → *, long press → #
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .keyEffectDecoration(colors, keyShape)
                        .combinedClickable(onClick = { onKey("*") }, onLongClick = { onKey("#") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("* #", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // 0+ — tap → 0, long press → +
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .keyEffectDecoration(colors, keyShape)
                        .combinedClickable(onClick = { onKey("0") }, onLongClick = { onKey("+") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("0 +", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // FIX #6: Was sending " " (space) on tap despite showing "_" label.
                // Now correctly sends "_" on tap; long press still switches keyboard.
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .keyEffectDecoration(colors, keyShape)
                        .combinedClickable(onClick = { onKey("_") }, onLongClick = { onKey("SWITCH_KEYBOARD") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("_", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText)
                }
                // Search / Enter (green) — dial pad always shows Search icon.
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape,
                    imeAction = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    onKey("ENTER")
                }
            }
        }
    }
