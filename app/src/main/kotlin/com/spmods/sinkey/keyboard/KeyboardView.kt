package com.spmods.sinkey.keyboard

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
// matchParentSize() is a member of BoxScope (not a top-level extension
// function like fillMaxSize), so it needs no import — it resolves
// automatically via the implicit BoxScope receiver at each call site
// inside a Box{} content lambda. Importing it as if it were top-level
// doesn't compile.
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.OpenInFull

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
    // Settings screen "Key opacity" (0f..1f, default 1f) — multiplies the
    // alpha of keyBg/specialKeyBg/spaceKeyBg only. Deliberately NOT applied
    // to keyText/specialKeyText/spaceKeyText/subText or to cardBg (sticker/
    // clipboard tile surfaces stay fully readable regardless), and NOT
    // applied to `bg` (already handled separately by transparentBg above).
    // At the default 1f every .copy(alpha = keyOpacity) call below is a
    // no-op, so existing themes render unchanged until the user lowers it.
    val keyOpacity: Float = 1f,
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
    // Settings screen "Key opacity" slider value, 0f..1f, default 1f (no
    // change from original solid keys). See KeyboardColors.keyOpacity doc
    // comment for exactly which fields this multiplies.
    keyOpacity: Float = 1f,
): KeyboardColors {
    val base = keyboardColorsBase(showKeyBorders, isDark)
    return base.copy(
        bg = if (transparentBg) Color.Transparent else base.bg,
        accent = palette.accent,
        keyEffect = keyEffect,
        keyOpacity = keyOpacity,
        // Baked into the color itself (rather than left for each of the
        // ~24 .background(colors.keyBg)-style call sites to apply
        // individually) so every one of them respects the slider for free,
        // with no risk of a call site being missed. The existing pressed-
        // state `.copy(alpha = 0.6f)` calls scattered through this file
        // still work correctly on top of this — Color.copy(alpha=) sets an
        // absolute alpha, so 0.6f there means "60% of keyOpacity", i.e. it
        // still darkens/lightens relative to the already-reduced resting
        // alpha instead of jumping back up to 60% of fully opaque.
        keyBg = base.keyBg.copy(alpha = base.keyBg.alpha * keyOpacity),
        specialKeyBg = base.specialKeyBg.copy(alpha = base.specialKeyBg.alpha * keyOpacity),
        spaceKeyBg = base.spaceKeyBg.copy(alpha = base.spaceKeyBg.alpha * keyOpacity),
        // "Colors" selection was previously a no-op on the real keyboard —
        // keyboardColorsBase's keyText is a fixed neutral color regardless
        // of palette, so accent only ever reached the one effect-overlay
        // usage (colors.accent at KeyboardView's line ~1338) and never
        // anything the user could actually see just by picking a card.
        // Tinting the letter/number key *text* (not the key background —
        // that stays neutral so the keyboard doesn't turn into a wall of
        // one color) is what makes the choice visible on real keys.
        keyText = if (palette == com.spmods.sinkey.data.KeyColorPalette.DEFAULT) {
            base.keyText
        } else {
            palette.accent
        },
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
        // GLOW and RIPPLE are board-wide overlays (see MechGlowOverlay /
        // ColorfulRippleOverlay), not per-key decorations — no-op here.
        // WAVE/CYCLE/STARS are likewise board-wide, drawn by
        // KeyboardLedRipple (see the ledActive branch below), so they're
        // a no-op at this per-key decoration site too.
        com.spmods.sinkey.data.KeyEffect.GLOW,
        com.spmods.sinkey.data.KeyEffect.RIPPLE,
        com.spmods.sinkey.data.KeyEffect.WAVE,
        com.spmods.sinkey.data.KeyEffect.CYCLE,
        com.spmods.sinkey.data.KeyEffect.STARS -> this
    }
}

/**
 * KeyEffect.GLOW, real Desh "Mech Glow" behavior — re-derived by inspecting
 * mech_glow_theme_dark.webp frame-by-frame (the original implementation
 * here was a guess based on the word "glow" and turned out wrong): it is
 * NOT a soft blurred blob. It's a colored *border outline* that traces
 * around the touched key, then visibly spreads outward to neighboring
 * keys' borders (both the row it's in and the row above/below) like a
 * wipe/wave, fading out — and the wave's own color hue-shifts over time
 * (red → yellow → green in the reference, i.e. a slow hue rotation), not a
 * per-key independent color.
 */
@Composable
private fun MechGlowOverlay(
    origin: Offset?,
    triggerId: Int,
    keyPositions: List<KeyPoint>,
    keySizes: Map<Char, androidx.compose.ui.geometry.Size>,
    modifier: Modifier = Modifier
) {
    if (origin == null) return

    var hue by remember { mutableStateOf(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(triggerId) {
        // Slow rotation per tap (not a full 360 sweep within one wave's
        // lifetime — the reference shows maybe a 60-90° drift red→yellow
        // across ~1.5s, so successive taps gradually cycle through hues).
        hue = (hue + 45f) % 360f
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1500, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
    }

    if (progress.value >= 1f || keySizes.isEmpty()) return

    Canvas(modifier = modifier) {
        val maxWaveRadiusPx = size.width * 0.35f
        val waveFrontPx = progress.value * maxWaveRadiusPx
        // Band width the outline stays visible within, in px — keys well
        // outside this band (already passed, or not yet reached) stay
        // undrawn, matching the reference's localized wipe rather than a
        // board-wide effect.
        val bandWidthPx = 0.5f * maxWaveRadiusPx
        val waveColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.9f, 1f)))

        keyPositions.forEach { kp ->
            val sz = keySizes[kp.char] ?: return@forEach
            val dx = kp.x - origin.x
            val dy = kp.y - origin.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val distFromFront = kotlin.math.abs(dist - waveFrontPx)
            if (distFromFront < bandWidthPx) {
                val bandStrength = 1f - (distFromFront / bandWidthPx)
                val overallFade = 1f - progress.value
                val alpha = (bandStrength * overallFade).coerceIn(0f, 1f)
                if (alpha > 0.02f) {
                    drawRoundRect(
                        color = waveColor.copy(alpha = alpha),
                        topLeft = Offset(kp.x - sz.width / 2f, kp.y - sz.height / 2f),
                        size = androidx.compose.ui.geometry.Size(sz.width, sz.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * KeyEffect.RIPPLE, real Desh "Colorful Ripple" behavior — re-derived by
 * inspecting ripple_theme_preview_dark.webp frame-by-frame (the original
 * implementation here guessed a full 5-hue rainbow palette and a huge
 * board-wide radius; neither matches the real asset). The actual glow is:
 * a single warm color (amber/orange, staying within a narrow hue band —
 * not cycling through the full spectrum), fairly small and localized
 * (roughly a couple of keys wide, not the whole board), that drifts/
 * sweeps diagonally from its origin rather than expanding as a large
 * uniform circle.
 */
@Composable
private fun ColorfulRippleOverlay(
    origin: Offset?,
    triggerId: Int,
    keyPositions: List<KeyPoint>,
    keySizes: Map<Char, androidx.compose.ui.geometry.Size>,
    modifier: Modifier = Modifier
) {
    if (origin == null) return

    // Narrow warm hue band (amber/orange), not the full rainbow — matches
    // the reference asset's consistent warm-glow look across its whole
    // cycle.
    val paletteHues = remember { floatArrayOf(28f, 34f, 40f, 46f) }
    var rippleColor by remember { mutableStateOf(Color.White) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(triggerId) {
        rippleColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(paletteHues.random(), 0.85f, 1f)))
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(2000, easing = androidx.compose.animation.core.Easing { fraction -> overshootEase(fraction, 0.5f) })
        )
    }

    if (progress.value >= 1f) return

    Canvas(modifier = modifier) {
        // Localized — roughly 2 keys wide, not board-wide (was 1.2x full
        // canvas width before, which read as a huge rainbow burst instead
        // of the small warm glow the reference shows).
        val maxRadiusPx = size.width * 0.28f
        // Overshoot easing can push progress slightly above 1f mid-animation
        // before settling — coerce only the *radius* so the circle doesn't
        // draw outside a sane bound, while alpha still reads the raw value.
        val radius = (progress.value.coerceIn(0f, 1f)) * maxRadiusPx
        val alpha = (1f - progress.value).coerceIn(0f, 1f)
        if (alpha > 0.01f) {
            // Desh clips the key rectangles OUT of the ripple draw, so the
            // color only shows in the gaps between keys and the rows above
            // the keyboard (suggestions bar, toolbar) — never washing over
            // the key faces themselves. clipPath(Difference) reproduces
            // that: start from the full canvas, subtract every known key's
            // rounded rect, draw the gradient into what's left.
            val keysPath = Path().apply {
                keyPositions.forEach { kp ->
                    val sz = keySizes[kp.char] ?: return@forEach
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = kp.x - sz.width / 2f,
                            top = kp.y - sz.height / 2f,
                            right = kp.x + sz.width / 2f,
                            bottom = kp.y + sz.height / 2f,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                    )
                }
            }
            clipPath(keysPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(rippleColor.copy(alpha = alpha), rippleColor.copy(alpha = 0f)),
                        center = origin,
                        radius = radius.coerceAtLeast(1f)
                    ),
                    radius = radius.coerceAtLeast(1f),
                    center = origin
                )
            }
        }
    }
}

/**
 * Matches Android's OvershootInterpolator(tension) formula: overshoots past
 * 1f then settles back, rather than easing straight to 1f — this is what
 * gives Desh's ripple its slight "bounce" at the end of the expansion.
 */
private fun overshootEase(t: Float, tension: Float): Float {
    val x = t - 1f
    return (x * x * ((tension + 1f) * x + tension) + 1f)
}

@Composable
private fun keyboardColorsBase(showKeyBorders: Boolean, isDark: Boolean): KeyboardColors {
    return if (isDark) {
        // FIX #12: Dark theme previously had almost no visible difference between
        // bordered (0xFF2E2E2E) and borderless (0xFF262626) key backgrounds.
        // Now borderless uses the same bg colour as the keyboard background so
        // keys appear "flat/floating", while bordered uses a clearly lighter slab.
        KeyboardColors(
            bg             = Color(0xFF000000),
            keyBg          = if (showKeyBorders) Color(0xFF3A3A3A) else Color(0xFF000000),
            specialKeyBg   = if (showKeyBorders) Color(0xFF2C2C2C) else Color(0xFF000000),
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
enum class Board { MAIN, SYMBOLS, NUMPAD, EMOJI, CLIPBOARD, FONT, DECORATION, DECORATION_STYLES, STICKER, STICKER_CREATE, STICKER_EDIT }

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
    // Small Sinhala-glyph corner hints on each QWERTY key in "si"/"mix"
    // modes — see sinhalaKeyHints in KeyboardLayouts.kt. Default true.
    sinhalaKeyHintsEnabled: Boolean = true,
    isDark: Boolean = false,
    // "Hidden message" decode reveal — non-null shows a dismissible banner
    // above the keyboard with the just-decoded text (see
    // SinKeyInputMethodService's hiddenMessageDecodedText/
    // registerClipboardListener). Passing null hides it; onDismiss should
    // set it back to null upstream.
    hiddenMessageDecodedText: String? = null,
    onDismissHiddenMessageBanner: () -> Unit = {},
    // "Just copied" preview strip — non-null shows a dismissible strip in
    // the suggestion-strip/tools-row slot with a one-line preview of the
    // text just copied (system-wide, via registerClipboardListener).
    // Auto-hides after ~1 minute (owned/timed by the IME service, see
    // SinKeyInputMethodService.showCopyPreview); onDismissCopyPreview fires
    // on explicit X tap, onCopyPreviewPaste on tapping the strip itself
    // (pastes justCopiedText at the cursor, same as a clipboard-history
    // entry), onCopyPreviewExpand on the expand icon (opens the full
    // clipboard history board).
    justCopiedText: String? = null,
    onDismissCopyPreview: () -> Unit = {},
    onCopyPreviewPaste: (String) -> Unit = {},
    onCopyPreviewExpand: () -> Unit = {},
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String, Int) -> Unit = { _, _ -> },
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
    // Opens SinKey's Settings screen (the app's own MainActivity) from the
    // keyboard — see AppsMicBar's matching param doc comment. Only the IME
    // service can actually start an Activity here (KeyboardView itself has
    // no Activity/Service context to do it from), so this is threaded in
    // rather than resolved locally the way e.g. decorationEnabled is.
    onOpenAppSettings: () -> Unit = {},
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
    onSaveImageSticker: (ImageStickerDraft) -> Unit = {},
    // ── Translate row state (TOOL_TRANSLATE) ──────────────────────────
    // Unlike Clipboard/Font/Sticker, Translate is NOT a separate Board —
    // it's a permanent-until-closed row that replaces the toolbar's
    // suggestion-strip/tools-row slot in place, the same way typing swaps
    // the tools row for the suggestion strip. This keeps the key rows
    // below (and the language-switch/globe key on them) working normally
    // while translating, matching the reference screenshot: a translate
    // bar sitting above an otherwise-untouched QWERTY keyboard, not a
    // full-screen board that hides everything else. All state here is
    // owned by the service (SinKeyInputMethodService.translateState),
    // since the service is what pushes each debounced translation result
    // into the real InputConnection as composing text — see that class's
    // TranslateState doc comment.
    isTranslateMode: Boolean = false,
    onTranslateModeChange: ((Boolean) -> Unit)? = null,
    translateSourceLang: String = "en",
    translateTargetLang: String = "si",
    onTranslateLanguagesSwapped: () -> Unit = {},
    translateSourceText: String = "",
    onTranslateSourceTextChanged: (String) -> Unit = {},
    // BUG FIX: the actual cursor offset within translateSourceText, owned
    // by the service (SinKeyInputMethodService.translateCursorPos) and
    // updated both by typing and by onTranslateSourceTextTapped below.
    // Previously this was tracked in the service but never threaded down
    // to TranslateRow at all, so the row had no way to draw the cursor
    // anywhere but the end of the text regardless of where it actually was.
    translateSourceCursor: Int = translateSourceText.length,
    // Tap-to-position-cursor in the translate row's source field — called
    // with the character offset (relative to translateSourceText, i.e. the
    // same coordinate space translateSourceText itself is in) closest to
    // where the user tapped. Wired to SinKeyInputMethodService.moveTranslateCursorTo,
    // which is what actually moves the real field's cursor there. See
    // TranslateRow's own doc comment for why this exists — the row isn't a
    // real BasicTextField, so tap-to-position has to be built by hand.
    onTranslateSourceTextTapped: (Int) -> Unit = {},
    translateResultText: String = "",
    isTranslating: Boolean = false,
    // BUG FIX: previously a failed translation (offline vs. reached the
    // server but failed) was invisible in the UI — translateResultText
    // just stayed blank/stale with no explanation. Null means no error;
    // non-null is the exact user-facing message to show (already resolved
    // to text by the caller — see SinKeyInputMethodService.TranslateErrorState
    // — so this file doesn't need to know about that enum, just render
    // whatever string it's given).
    translateErrorMessage: String? = null,
    // ── "Edit theme" live preview override ─────────────────────────────
    // PhotoEditThemeScreen shows this same KeyboardView as its "real
    // keyboard" preview while the user is still adjusting Show key
    // borders / Blur / Brightness / Key opacity for a photo they just
    // cropped — none of which are saved to PreferencesManager yet (the
    // user might still cancel via onBack). Everything above this point
    // reads its background/blur/brightness/keyOpacity from prefsManager
    // (see customBackgroundUri/customBackgroundBlur/etc. below), which
    // would only show the *previously saved* theme, not the in-progress
    // edit. When non-null, previewBackgroundBitmap short-circuits the
    // normal customBackgroundUri-driven background path to draw this
    // in-memory Bitmap instead, and the preview*Override values (when
    // non-null) replace the corresponding prefs-driven value the same
    // way — every other caller (the real IME service, the plain
    // keyboard-preview overlay in MainActivity) omits all of these and
    // gets the normal all-prefs behaviour unchanged.
    previewBackgroundBitmap: Bitmap? = null,
    previewBlur: Float? = null,
    previewBrightness: Float? = null,
    previewKeyOpacity: Float? = null,
    previewShowKeyBorders: Boolean? = null
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
    val customBackgroundBlur by prefsManager.customBackgroundBlur.collectAsState(initial = 0f)
    val customBackgroundBrightness by prefsManager.customBackgroundBrightness.collectAsState(initial = 0.5f)
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
    val keyOpacityPref by prefsManager.keyOpacity.collectAsState(initial = 1f)

    // Effective values — previewXxx overrides win when non-null (Edit
    // theme's live preview), otherwise fall back to the normal
    // prefs-driven value every other caller already relies on. See the
    // previewBackgroundBitmap doc comment on this function's params for
    // why this exists.
    val effectiveShowKeyBorders = previewShowKeyBorders ?: showKeyBorders
    val effectiveBlur = previewBlur ?: customBackgroundBlur
    val effectiveBrightness = previewBrightness ?: customBackgroundBrightness
    val keyOpacity = previewKeyOpacity ?: keyOpacityPref

    val colors = keyboardColors(
        showKeyBorders = effectiveShowKeyBorders,
        isDark = isDark,
        palette = keyColorPalette,
        keyEffect = keyEffect,
        transparentBg = previewBackgroundBitmap != null || customBackgroundUri != null || backgroundStyle != com.spmods.sinkey.data.BackgroundStyle.NONE,
        keyOpacity = keyOpacity,
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
    // (see ledPattern/typingAnimation branch there) —
    // hoisted up to this top level, rather than kept local to that nested
    // block, specifically so this composable's own TypingAnimationPopup
    // and KeyboardLedRipple calls at the bottom of the function can read
    // them. pressOrigin/pressTriggerId use the same restart-even-on-
    // identical-value reasoning as the rest of this state —
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
    val decorationEnabled by prefsManager.decorationEnabled.collectAsState(initial = false)
    val decorationVaryStyles by prefsManager.decorationVaryStyles.collectAsState(initial = true)
    val hiddenMessageEnabled by prefsManager.hiddenMessageEnabled.collectAsState(initial = false)
    val selectedDecorationKey by prefsManager.decorationStyle.collectAsState(initial = DecorationStyle.NONE.key)
    // "Incognito" — see DecorationPickerView's incognitoEnabled param doc
    // comment and PreferencesManager.Keys.INCOGNITO_ENABLED. Default off,
    // same self-sourced-from-DataStore pattern as every other toggle read
    // directly in this composable above.
    val incognitoEnabled by prefsManager.incognitoEnabled.collectAsState(initial = false)
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
        // — bottom-most layer. Drawn only when no "My themes" photo is set
        // (nor a live preview bitmap — see previewBackgroundBitmap doc
        // comment), since a user-picked photo always takes precedence (see
        // BackgroundStyle doc comment) — both are still allowed to make
        // colors.bg transparent above so either one shows through cleanly.
        if (previewBackgroundBitmap == null && customBackgroundUri == null && backgroundStyle != com.spmods.sinkey.data.BackgroundStyle.NONE) {
            KeyboardBuiltInBackground(
                style = backgroundStyle,
                isDark = isDark,
                modifier = Modifier.matchParentSize()
            )
        }
        // "Edit theme" live preview bitmap — takes precedence over the
        // saved customBackgroundUri below (see previewBackgroundBitmap doc
        // comment on this function's params). Same blur/brightness
        // treatment as KeyboardCustomBackground's static-image path, just
        // drawn directly from the in-memory Bitmap instead of decoding a
        // Uri, since PhotoEditThemeScreen's cropped photo isn't saved to
        // one until the user taps Done.
        if (previewBackgroundBitmap != null) {
            val previewBrightnessDelta = ((effectiveBrightness) - 0.5f) * 2f * 255f
            val previewBrightnessFilter = remember(previewBrightnessDelta) {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, previewBrightnessDelta,
                            0f, 1f, 0f, 0f, previewBrightnessDelta,
                            0f, 0f, 1f, 0f, previewBrightnessDelta,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            Image(
                bitmap = previewBackgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = previewBrightnessFilter,
                modifier = Modifier
                    .matchParentSize()
                    .let { if (effectiveBlur > 0.01f) it.blur(20.dp * effectiveBlur) else it }
            )
        } else customBackgroundUri?.let { uriString ->
            // "My themes" custom photo background — drawn as the
            // bottom-most layer of the outer Box, behind everything else.
            // Only rendered when a background is actually set; colors.bg
            // is made fully transparent by keyboardColors(transparentBg =
            // ...) in that case so every row's own `.background(colors.bg)`
            // (toolbar, suggestion strip, key rows, etc.) lets this show
            // through instead of painting over it in solid color
            // band-by-band.
            KeyboardCustomBackground(
                uriString = uriString,
                blur = effectiveBlur,
                brightness = effectiveBrightness,
                modifier = Modifier.matchParentSize()
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(colors.bg)
        ) {
            // ── Hidden-message decode banner — sits above the toolbar,
            // pushing the whole keyboard down while visible, same as the
            // translate row's own placement. Only rendered when there's
            // something decoded to show; dismissing (tap or the X) hides it
            // and hands control back to onDismissHiddenMessageBanner, which
            // clears the upstream state so it doesn't reappear until the
            // next matching copy.
            if (hiddenMessageDecodedText != null) {
                HiddenMessageBanner(
                    colors = colors,
                    decodedText = hiddenMessageDecodedText,
                    onDismiss = onDismissHiddenMessageBanner
                )
            }
            // ── Toolbar (always visible, never re-created on pad switch,
            // except for the Emoji board — which moves its own category
            // tabs to the very top instead, in place of this toolbar) ──────
            if (currentBoard != Board.EMOJI && currentBoard != Board.CLIPBOARD && currentBoard != Board.FONT &&
                currentBoard != Board.DECORATION && currentBoard != Board.DECORATION_STYLES &&
                currentBoard != Board.STICKER && currentBoard != Board.STICKER_CREATE) {
                AppsMicBar(
                    colors = colors,
                    isDark = isDark,
                    justCopiedText = justCopiedText,
                    onDismissCopyPreview = onDismissCopyPreview,
                    onCopyPreviewPaste = onCopyPreviewPaste,
                    onCopyPreviewExpand = onCopyPreviewExpand,
                    // BUG FIX: suggestions previously came only from the
                    // real typing pipeline (wordBuffer etc.), which is
                    // fully bypassed while translate mode is open (see
                    // SinKeyInputMethodService's translate-state doc
                    // comment) — so `suggestions` now also gets populated
                    // for the translate buffer (via updateTranslateSuggestions)
                    // while isTranslateMode is true, and this no longer
                    // needs to force it empty for that case; only the
                    // pre-existing phone/symbols/numpad cases still do.
                    suggestions = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD) emptyList() else suggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    autocorrectUndoWord = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD || isTranslateMode) null else autocorrectUndoWord,
                    onUndoAutocorrect = onUndoAutocorrect,
                    onKey = onKey,
                    onClipboardOpen = { pushBoard(Board.CLIPBOARD) },
                    onFontOpen = { pushBoard(Board.FONT) },
                    onDecorationOpen = { pushBoard(Board.DECORATION) },
                    hiddenMessageEnabled = hiddenMessageEnabled,
                    onHiddenMessageToggle = {
                        coroutineScope.launch { prefsManager.setHiddenMessageEnabled(!hiddenMessageEnabled) }
                    },
                    onOpenAppSettings = onOpenAppSettings,
                    onStickerOpen = { pushBoard(Board.STICKER) },
                    isTranslateMode = isTranslateMode,
                    onTranslateOpen = onTranslateModeChange?.let { { it(true) } } ?: {},
                    onTranslateClose = onTranslateModeChange?.let { { it(false) } } ?: {},
                    translateSourceLang = translateSourceLang,
                    translateTargetLang = translateTargetLang,
                    onTranslateLanguagesSwapped = onTranslateLanguagesSwapped,
                    translateSourceText = translateSourceText,
                    translateSourceCursor = translateSourceCursor,
                    onTranslateSourceTextChanged = onTranslateSourceTextChanged,
                    translateResultText = translateResultText,
                    isTranslating = isTranslating,
                    translateErrorMessage = translateErrorMessage,
                    onTranslateSourceTextTapped = onTranslateSourceTextTapped,
                    selectedFontStyle = FancyTextStyle.fromKey(selectedFontKey),
                    decorationEnabled = decorationEnabled,
                    decorationVaryStyles = decorationVaryStyles,
                    selectedDecorationStyle = DecorationStyle.fromKey(selectedDecorationKey)
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
                currentBoard != Board.DECORATION && currentBoard != Board.DECORATION_STYLES &&
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
                currentBoard == Board.DECORATION -> DecorationPickerView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    incognitoEnabled = incognitoEnabled,
                    onIncognitoChange = { incognito ->
                        coroutineScope.launch { prefsManager.setIncognitoEnabled(incognito) }
                    },
                    enabled = decorationEnabled,
                    onEnabledChange = { enabled ->
                        coroutineScope.launch { prefsManager.setDecorationEnabled(enabled) }
                    },
                    varyStyles = decorationVaryStyles,
                    onStylesOpen = { pushBoard(Board.DECORATION_STYLES) },
                    onBack = { popBoard() }
                )
                currentBoard == Board.DECORATION_STYLES -> DecorationStylesView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    varyStyles = decorationVaryStyles,
                    onVaryStylesChange = { vary ->
                        coroutineScope.launch { prefsManager.setDecorationVaryStyles(vary) }
                    },
                    selectedStyleKey = selectedDecorationKey,
                    onStyleSelected = { styleKey ->
                        coroutineScope.launch { prefsManager.setDecorationStyle(styleKey) }
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
                    // every other key's position for the LED
                    // ripple / the Typing Animation pop-up, all of which
                    // need to know where the actually-touched key is. This
                    // map itself is hoisted to the top of KeyboardView (see
                    // "Shared press state" above) since TypingAnimationPopup
                    // and KeyboardLedRipple are drawn at that outer level,
                    // not inside this nested Box — only the *touch watcher*
                    // that fills it in lives here, since only the MAIN
                    // board's keys currently report their positions.
                    // WAVE/CYCLE/STARS now live in the Themes "Effects"
                    // grid (KeyEffect) rather than the old separate LED
                    // pattern picker, drawn by KeyboardKeyEffectRipple
                    // (same border-glow math, just keyed off KeyEffect
                    // instead of LedPattern). ledPattern itself still
                    // supplies NONE/BREATHING as its own separate picker,
                    // so both can be independently active.
                    val keyEffectRippleActive = colors.keyEffect == com.spmods.sinkey.data.KeyEffect.WAVE ||
                        colors.keyEffect == com.spmods.sinkey.data.KeyEffect.CYCLE ||
                        colors.keyEffect == com.spmods.sinkey.data.KeyEffect.STARS
                    val ledActive = ledPattern != com.spmods.sinkey.data.LedPattern.NONE || keyEffectRippleActive
                    val typingAnimActive = typingAnimation != com.spmods.sinkey.data.TypingAnimation.NONE
                    // Desh-style RIPPLE is a board-wide overlay (not a
                    // per-key decoration), so it needs the same shared
                    // touch-position plumbing as the LED ripple/Typing
                    // Animation below.
                    val colorfulRippleActive = colors.keyEffect == com.spmods.sinkey.data.KeyEffect.RIPPLE
                    val glowActive = colors.keyEffect == com.spmods.sinkey.data.KeyEffect.GLOW
                    val needsKeyPositions = ledActive || typingAnimActive || swipeTypingEnabled || colorfulRippleActive || glowActive
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
                    // origin for every row). KeyboardLedRipple/
                    // TypingAnimationPopup are drawn
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
                                    // for the LED ripple / the
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
                                            // mean LED/Typing
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
                                            // A finger that has been held down at
                                            // roughly the same spot for longer than
                                            // the system long-press threshold is a
                                            // long-press, never a swipe — even if it
                                            // has drifted a little past touchSlop in
                                            // the meantime (natural hand tremor while
                                            // holding a key does this constantly).
                                            // Without this guard, a long-press on
                                            // e/u/i/o/a/s (or any letter) would very
                                            // often get reinterpreted as the start of
                                            // a swipe right as the long-press timer in
                                            // LetterKey was about to fire, this outer
                                            // handler would consume() that event, and
                                            // LetterKey's own pointerInput — a sibling
                                            // further down this same pointer stream —
                                            // would never see the up event it needs to
                                            // show the popup or commit an alternate.
                                            // Capturing downTimeMs up front and
                                            // checking elapsed time on every event
                                            // means: once the long-press threshold
                                            // has passed, this handler simply stops
                                            // trying to claim the gesture as a swipe
                                            // at all, letting the tap/long-press
                                            // machinery in MainKeyboardKeys' own keys
                                            // handle it exactly as if swipe typing
                                            // were off.
                                            val downTimeMs = System.currentTimeMillis()
                                            val longPressThresholdMs =
                                                android.view.ViewConfiguration.getLongPressTimeout().toLong()

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
                                                    val elapsed = System.currentTimeMillis() - downTimeMs
                                                    val travelled = (change.position - down.position).getDistance()
                                                    if (elapsed < longPressThresholdMs && travelled > touchSlop) {
                                                        // Past the slop threshold
                                                        // while still well inside the
                                                        // long-press window — this is
                                                        // unambiguously a fast swipe,
                                                        // not a hold. Start claiming
                                                        // the gesture from here on.
                                                        // Because this pointerInput
                                                        // lives on the Box ABOVE
                                                        // MainKeyboardKeys (not a
                                                        // sibling beside it), consuming
                                                        // here also stops
                                                        // MainKeyboardKeys' own
                                                        // clickable handlers from ever
                                                        // starting a press for this
                                                        // pointer — exactly the "swipe
                                                        // wins once it's clearly a
                                                        // swipe" behavior this needs,
                                                        // without ever blocking plain
                                                        // taps or long-presses.
                                                        dragging = true
                                                        gestureIsDragging = true
                                                        gesturePathPoints = listOf(down.position, change.position)
                                                        change.consume()
                                                    }
                                                    // Still under slop, or the
                                                    // long-press threshold has already
                                                    // passed: leave the change
                                                    // unconsumed so a tap OR a
                                                    // long-press (and its popup) still
                                                    // reaches MainKeyboardKeys.
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
                            showKeyBorders = effectiveShowKeyBorders,
                            sinhalaKeyHintsEnabled = sinhalaKeyHintsEnabled,
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

                        if (colorfulRippleActive) {
                            // Desh-style ripple: a random-colored radial
                            // glow expanding from the touched key out across
                            // the *whole* board (not just that one key), with
                            // a slight overshoot-then-settle bounce. Pure
                            // drawing layer — no pointerInput of its own —
                            // so it never blocks taps.
                            ColorfulRippleOverlay(
                                origin = pressOrigin,
                                triggerId = pressTriggerId,
                                keyPositions = keyPositions.values.toList(),
                                keySizes = keySizes,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        if (glowActive) {
                            // Desh-style mech glow: a traveling colored
                            // outline wave around key borders (re-derived
                            // from the real animated preview asset — see
                            // MechGlowOverlay's doc comment), hue rotating
                            // per tap. Same shared pressOrigin/pressTriggerId
                            // as every other press-reactive overlay here.
                            MechGlowOverlay(
                                origin = pressOrigin,
                                triggerId = pressTriggerId,
                                keyPositions = keyPositions.values.toList(),
                                keySizes = keySizes,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        if (ledActive) {
                            // pressOrigin/pressTriggerId are the same shared
                            // touch state Typing Animation also reads from —
                            // each consumer draws its own distinct visual off
                            // of it.
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
                            // WAVE/CYCLE/STARS selected from the Themes
                            // "Effects" grid — same underlying animation as
                            // KeyboardLedRipple above, drawn independently
                            // so it can be combined with ledPattern's own
                            // BREATHING at the same time if both are set.
                            KeyboardKeyEffectRipple(
                                effect = colors.keyEffect,
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
// symbol character. Exists so the LED ripple / the Typing
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
    sinhalaKeyHintsEnabled: Boolean = true,
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
        // Show Sinhala hint glyphs only when Sinhala can actually be typed
        // AND the user hasn't turned the hint off in Settings.
        val showSinhalaHints = sinhalaKeyHintsEnabled && (currentLanguage == "si" || currentLanguage == "mix")
        // FIX #5: Pass onShiftChange so letter rows can reset one-shot shift.
        NumberedKeyRow(EnglishRows[0], topRowNumbers, shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            showSinhalaHints = showSinhalaHints,
            onKey = { onKey(it); if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF) })
        KeyRow(EnglishRows[1], shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            showSinhalaHints = showSinhalaHints,
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
                // against anything; only used so the LED ripple/
                // the Typing Animation pop-up can find these keys' real
                // positions too, the same way they already could for every
                // letter key.
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(KEY_SENTINEL_SHIFT, coords) } },
                onTap = { onKey("SHIFT") },
                onDoubleTap = { onKey("SHIFT_LOCK") }
            )
            EnglishRows[2].forEach { k ->
                val display = if (shift) k.uppercase() else k
                val hint = if (showSinhalaHints) sinhalaKeyHints[k.lowercase().firstOrNull() ?: ' '] else null
                // FIX #5: Reset shift after each letter (one-shot shift behaviour).
                LetterKey(
                    label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                    onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                    hint = hint,
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
// Translate row (TOOL_TRANSLATE) — replaces the toolbar's tools-row/
// suggestion-strip slot in place while active. See AppsMicBar's isTranslateMode
// branch and KeyboardView's doc comment on the translate-row params.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TranslateRow(
    colors: KeyboardColors,
    sourceLang: String,
    targetLang: String,
    onLanguagesSwapped: () -> Unit,
    sourceText: String,
    onSourceTextChanged: (String) -> Unit,
    // BUG FIX: the blinking cursor used to always render after the full
    // sourceText regardless of where the user last tapped — tap-to-position
    // (onSourceTextTapped below) updated the real cursor position upstream
    // in the service, but this row never received or drew from it, so the
    // cursor visually stayed pinned to the end no matter where you tapped
    // or typed. sourceCursor is the character offset (same coordinate
    // space as sourceText) to actually draw the cursor at.
    sourceCursor: Int = sourceText.length,
    resultText: String,
    isTranslating: Boolean,
    // BUG FIX: non-null when the last translate attempt failed — shown in
    // place of resultText (mutually exclusive: a failed attempt has no
    // result to show) so the user can tell "no internet" apart from
    // "reached Google but it failed" instead of the row just staying
    // blank/stale with no explanation.
    errorMessage: String? = null,
    // Tap-to-position-cursor: called with the character offset (within
    // sourceText, i.e. relative to the translate anchor) the user tapped
    // closest to, so the real field's cursor can be moved there — same as
    // tapping anywhere in a normal text field. Left as a no-op default so
    // every other caller (there are none today, but future previews of
    // this row) doesn't need to wire it up to get a working row.
    onSourceTextTapped: (Int) -> Unit = {},
    onClose: () -> Unit
) {
    val headerHeight = 40.dp
    val languageBarHeight = 40.dp
    val fieldHeight = 44.dp

    Column(modifier = Modifier.fillMaxWidth().background(colors.bg)) {
        // ── Header: "Translate" label + Close ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Translate",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close translate",
                    modifier = Modifier.size(18.dp),
                    tint = colors.subText
                )
            }
        }

        // ── Language pair + swap ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(languageBarHeight).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.keyBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(text = translateLanguageLabel(sourceLang), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.keyText)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onLanguagesSwapped() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap languages",
                    modifier = Modifier.size(18.dp),
                    tint = DeshGreen
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.keyBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(text = translateLanguageLabel(targetLang), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.keyText)
            }
        }

        // ── Input field ─────────────────────────────────────────────────
        // NOT a real BasicTextField/TextField — this Composable runs inside
        // the IME's own window (unlike e.g. StickerImageEditorView's field,
        // which runs in a normal Activity). A real focusable text field
        // here would try to request its own soft keyboard, since as far as
        // Android is concerned it's just another editable view needing an
        // IME — see StickerCreateView's top-level doc comment for the same
        // reasoning applied to the sticker text composer. Typing instead
        // happens through the real QWERTY keys already on screen below,
        // same as StickerTextComposeView's draft buffer. Tapping within the
        // text DOES move the cursor (see the pointerInput block on the Row
        // below) — this field behaves like a normal text field for
        // positioning purposes even though it isn't a real
        // BasicTextField/EditText and doesn't request its own IME.
        val infinite = rememberInfiniteTransition(label = "translateCursorBlink")
        val cursorAlpha by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "translateCursorBlinkAlpha"
        )

        // Tracks the most recent text layout of the FULL sourceText (laid
        // out via the invisible measuring Text below), so a tap's raw x/y
        // can be converted into a character offset via
        // TextLayoutResult.getOffsetForPosition — the standard way to map a
        // tap into an offset for a Text that (unlike BasicTextField) has no
        // built-in tap-to-cursor handling of its own.
        //
        // BUG FIX: this used to come from the visible Text that rendered
        // sourceText — but that Text now only renders the *beforeCursor*
        // half (see the cursor-splitting block below), so its layout only
        // covers characters up to the cursor and any tap past the cursor
        // would incorrectly resolve against a truncated width. A dedicated
        // zero-size measuring Text keeps a layout of the *entire* string
        // for hit-testing, independent of how the cursor splits the
        // visible rendering.
        var sourceTextLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        Text(
            text = sourceText,
            fontSize = 15.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.size(0.dp),
            onTextLayout = { sourceTextLayout = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.keyBg)
                .padding(horizontal = 12.dp)
                // Tap-to-position-cursor (matches how every other Android
                // text field behaves — see this row's own doc comment on
                // why this isn't a real BasicTextField). Placed on the Row
                // rather than just the Text so tapping the empty space past
                // the end of a short source text still moves the cursor to
                // the end, same as tapping past the last character in a
                // normal field.
                .pointerInput(sourceText) {
                    detectTapGestures { offset ->
                        val layout = sourceTextLayout
                        val charOffset = if (sourceText.isEmpty() || layout == null) {
                            0
                        } else {
                            // Clamp: a tap past the last character's right
                            // edge would otherwise resolve to a mid-string
                            // offset (getOffsetForPosition finds the
                            // *nearest* character), not "end of text" like
                            // users expect from tapping empty trailing
                            // space in a real field.
                            if (offset.x >= layout.size.width.toFloat()) {
                                sourceText.length
                            } else {
                                layout.getOffsetForPosition(offset).coerceIn(0, sourceText.length)
                            }
                        }
                        onSourceTextTapped(charOffset)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sourceText.isEmpty()) {
                Text(
                    text = "Type to translate…",
                    fontSize = 15.sp,
                    color = colors.subText,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // BUG FIX: split the text at the real cursor offset instead
                // of always drawing the whole string followed by a cursor
                // bar. Splitting into a before/after pair and placing the
                // bar between them is what makes the cursor actually track
                // sourceCursor — previously the bar's screen position had
                // no relationship to sourceCursor at all, it just always
                // ended up after every character because the full string
                // was rendered as one Text before it.
                val clampedCursor = sourceCursor.coerceIn(0, sourceText.length)
                val beforeCursor = sourceText.substring(0, clampedCursor)
                val afterCursor = sourceText.substring(clampedCursor)
                Text(
                    text = beforeCursor,
                    fontSize = 15.sp,
                    color = colors.keyText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                )
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(18.dp)
                        .background(DeshGreen.copy(alpha = cursorAlpha))
                )
                Text(
                    text = afterCursor,
                    fontSize = 15.sp,
                    color = colors.keyText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            if (isTranslating) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = DeshGreen
                )
            }
        }

        // ── Live translated result, or error if the last attempt failed ──
        // BUG FIX: errorMessage and resultText are mutually exclusive (a
        // failed attempt has no result), so this replaces rather than
        // supplements the old "just show resultText" block — previously a
        // failure left this whole area blank with no explanation at all.
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE05252),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            )
        } else if (resultText.isNotEmpty()) {
            Text(
                text = resultText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DeshGreen,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

private fun translateLanguageLabel(code: String): String = when (code) {
    "en" -> "English"
    "si" -> "සිංහල"
    else -> code
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar — tools row OR suggestion strip depending on typing state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppsMicBar(
    colors: KeyboardColors,
    isDark: Boolean = false,
    // See KeyboardView's own doc comment on these — same passthrough
    // pattern as the translate-row params below.
    justCopiedText: String? = null,
    onDismissCopyPreview: () -> Unit = {},
    onCopyPreviewPaste: (String) -> Unit = {},
    onCopyPreviewExpand: () -> Unit = {},
    suggestions: List<String>,
    onSuggestionSelected: (String, Int) -> Unit,
    // Non-null right after an autocorrect swap — renders as a distinct
    // "Undo" chip pinned before the regular suggestions. See KeyboardView's
    // doc comment on this same param for ownership/lifecycle.
    autocorrectUndoWord: String? = null,
    onUndoAutocorrect: () -> Unit = {},
    onKey: (String) -> Unit,
    onClipboardOpen: () -> Unit,
    onFontOpen: () -> Unit,
    onDecorationOpen: () -> Unit = {},
    // "Hidden message" tools-row toggle — see ZeroWidthEncoder.kt. Purely a
    // state flip here (no board to open); the actual encoding happens at
    // commit time in the IME service, reading the same cached preference.
    hiddenMessageEnabled: Boolean = false,
    onHiddenMessageToggle: () -> Unit = {},
    // Opens SinKey's own Settings screen (MainActivity) — replaces the
    // previous mic pill button, which only ever called
    // sendDefaultEditorAction(true) (effectively a second Enter key, not
    // real voice typing) rather than anything mic-related.
    onOpenAppSettings: () -> Unit = {},
    onStickerOpen: () -> Unit,
    // See KeyboardView's own doc comment on these — the translate row is a
    // third state of this same toolbar area (tools row / suggestion strip
    // / translate row), not a separate board, so the key rows underneath
    // and the language-switch key on them keep working while it's shown.
    isTranslateMode: Boolean = false,
    onTranslateOpen: () -> Unit = {},
    onTranslateClose: () -> Unit = {},
    translateSourceLang: String = "en",
    translateTargetLang: String = "si",
    onTranslateLanguagesSwapped: () -> Unit = {},
    translateSourceText: String = "",
    // BUG FIX: forwarded through to TranslateRow — see that composable's
    // doc comment on sourceCursor for why this exists.
    translateSourceCursor: Int = translateSourceText.length,
    onTranslateSourceTextChanged: (String) -> Unit = {},
    translateResultText: String = "",
    isTranslating: Boolean = false,
    translateErrorMessage: String? = null,
    // See TranslateRow's own doc comment on the matching param — forwarded
    // straight through, same as every other translate-row callback here.
    onTranslateSourceTextTapped: (Int) -> Unit = {},
    selectedFontStyle: FancyTextStyle = FancyTextStyle.NONE,
    decorationEnabled: Boolean = false,
    decorationVaryStyles: Boolean = true,
    selectedDecorationStyle: DecorationStyle = DecorationStyle.NONE
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
    //
    // The translate row is deliberately exempted from that same-height
    // constraint (it's taller — header + language pair + text field) since
    // entering/leaving it is always an explicit tap on TOOL_TRANSLATE/Close,
    // never something that happens mid-typing-flow the way the tools-row ↔
    // suggestion-strip swap does, so a resize here doesn't carry the same
    // risk of racing WhatsApp's own panel animation.
    //
    // IMPORTANT: TranslateRow is prepended ABOVE the normal toolbar
    // content below (in a Column), not a replacement for it — the
    // tools row (emoji/clipboard/font/etc. icons) still needs to render
    // normally while translating. The suggestion strip itself is
    // suppressed during translate mode (see the isTranslateMode check on
    // AppsMicBar's suggestions/autocorrectUndoWord params above) since
    // typing during translate mode no longer goes through the ordinary
    // typing pipeline that produces those suggestions.
    Column(modifier = Modifier.fillMaxWidth()) {
    if (isTranslateMode) {
        TranslateRow(
            colors = colors,
            sourceLang = translateSourceLang,
            targetLang = translateTargetLang,
            onLanguagesSwapped = onTranslateLanguagesSwapped,
            sourceText = translateSourceText,
            sourceCursor = translateSourceCursor,
            onSourceTextChanged = onTranslateSourceTextChanged,
            resultText = translateResultText,
            isTranslating = isTranslating,
            errorMessage = translateErrorMessage,
            onSourceTextTapped = onTranslateSourceTextTapped,
            onClose = onTranslateClose
        )
    }
    // justCopiedText takes over only the middle of the tools row (between
    // the always-present grid/TOOL_APPS icon and the settings pill) — see
    // the tools-row branch below, not a separate branch here. Suggestions
    // still win over the plain tools row the same as before; the copy
    // preview only replaces the *icons* inside the tools-row branch, so it
    // never appears while actively typing (isTyping already covers that).
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
                        .clickable { onDecorationOpen() },
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
                        val decorationForThisChip = when {
                            !decorationEnabled -> DecorationStyle.NONE
                            // "Vary styles": each suggestion slot gets a different
                            // style (deterministic by position — see
                            // TextDecorator.cycleStyleFor's doc comment) instead of
                            // every chip using the one style picked in the list.
                            decorationVaryStyles -> TextDecorator.cycleStyleFor(idx)
                            else -> selectedDecorationStyle
                        }
                        val displayWord = TextDecorator.apply(
                            FancyTextMapper.apply(word, selectedFontStyle),
                            decorationForThisChip
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSuggestionSelected(word, idx) }
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
            // TOOL_SETTINGS previously did nothing (logged "not yet
            // implemented"). Repurposed to the "Hidden message" toggle —
            // real Settings access moved to the pill button at the end of
            // the row (onOpenAppSettings), replacing the old mic button
            // there, which likewise never did real voice typing.
            val tools = listOf(
                R.drawable.ic_unified_menu  to "TOOL_APPS",
                R.drawable.ic_sticker       to "TOOL_STICKER",
                R.drawable.ic_clipboard     to "TOOL_CLIPBOARD",
                R.drawable.ic_custom_font   to "TOOL_FONT",
                R.drawable.ic_translation   to "TOOL_TRANSLATE",
                null                        to "TOOL_HIDDEN_MESSAGE"
            )
            // Pure white on dark theme, pure black on light theme — see the
            // doc comment at this Row's icon Icon() calls for why this
            // isn't colors.subText.
            val toolIconTint = if (isDark) Color.White else Color.Black
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TOOL_APPS (grid icon) always stays pinned on the left,
                // copy-preview or not — same reasoning as its pin in the
                // suggestion strip above (see that Row's own comment).
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDecorationOpen() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_unified_menu),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = toolIconTint
                    )
                }
                if (justCopiedText != null) {
                    // ── "Just copied" preview ────────────────────────────
                    // Replaces only the middle sticker/clipboard/font/
                    // translate/hidden-message icons — grid (TOOL_APPS,
                    // above) and settings (below) stay put either way, so
                    // this never hides the whole toolbar, only the icons
                    // that a fresh copy has something more useful to show
                    // than.
                    CopyPreviewInline(
                        colors = colors,
                        isDark = isDark,
                        text = justCopiedText,
                        onPaste = { onCopyPreviewPaste(justCopiedText) },
                        onExpand = onCopyPreviewExpand,
                        onDismiss = onDismissCopyPreview
                    )
                } else {
                tools.drop(1).forEach { (iconRes, action) ->
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
                                    "TOOL_TRANSLATE" -> onTranslateOpen()
                                    "TOOL_HIDDEN_MESSAGE" -> onHiddenMessageToggle()
                                    else -> onKey(action)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Tools-row icons are pure white on dark theme / pure
                        // black on light theme — deliberately NOT colors.subText
                        // (a mid-grey in both themes), which read as washed-out
                        // against the toolbar. toolIconTint below is local to
                        // this Row, not part of KeyboardColors, since nothing
                        // else in the keyboard wants pure white/black rather
                        // than the softer subText grey.
                        if (action == "TOOL_HIDDEN_MESSAGE") {
                            // Filled while on, outline while off — same
                            // filled/outlined pairing already used for the
                            // pinned-sticker star elsewhere in this file.
                            Icon(
                                imageVector = if (hiddenMessageEnabled) Icons.Filled.VisibilityOff else Icons.Outlined.VisibilityOff,
                                contentDescription = "Hidden message",
                                modifier = Modifier.size(22.dp),
                                tint = if (hiddenMessageEnabled) colors.accent else toolIconTint
                            )
                        } else if (iconRes != null) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = toolIconTint
                            )
                        }
                    }
                }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        // No background pill — matches the rest of the
                        // tools row (a plain icon button, not a filled
                        // circle) rather than standing out as the one
                        // highlighted-looking icon in the row.
                        .clickable { onOpenAppSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Open SinKey settings",
                        modifier = Modifier.size(22.dp),
                        tint = toolIconTint
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "Just copied" preview — see AppsMicBar's justCopiedText param doc comment.
// An inline RowScope piece (not its own Row) since it sits between the
// always-present grid and settings icons inside the tools row's Row, taking
// up the middle space those 5 tool icons would otherwise occupy — replacing
// only them, not the whole toolbar.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RowScope.CopyPreviewInline(
    colors: KeyboardColors,
    isDark: Boolean,
    text: String,
    onPaste: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val toolIconTint = if (isDark) Color.White else Color.Black
    // Single pill/card holding everything — icon, truncated preview text,
    // expand, and close — instead of the expand/close icons sitting
    // outside it as separate plain buttons. Only the icon+text portion is
    // its own nested clickable (paste); expand/close keep their own
    // clickable so tapping them doesn't also trigger a paste.
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.subText.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .clickable { onPaste() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_clipboard),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.keyText
            )
            Text(
                text = text,
                fontSize = 15.sp,
                color = colors.keyText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInFull,
                contentDescription = "Open clipboard history",
                modifier = Modifier.size(15.dp),
                tint = toolIconTint
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(16.dp),
                tint = toolIconTint
            )
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
    // Show each key's Sinhala-hint glyph (top-left corner) — true only in
    // "si"/"mix" language modes; see MainKeyboardKeys' currentLanguage.
    showSinhalaHints: Boolean = false,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEachIndexed { index, k ->
            val display = if (shift) k.uppercase() else k
            val num = numbers.getOrNull(index) ?: ""
            val hint = if (showSinhalaHints) sinhalaKeyHints[k.lowercase().firstOrNull() ?: ' '] else null
            NumberedLetterKey(
                label = display, number = num, weight = 1f,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                hint = hint,
                onTap = { onKey(display) },
                onLongPress = { onKey(num) },
                onAlternateSelected = { alt -> onKey(alt) }
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
    // Show each key's Sinhala-hint glyph (top-right corner) — true only in
    // "si"/"mix" language modes; see MainKeyboardKeys' currentLanguage.
    showSinhalaHints: Boolean = false,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            val hint = if (showSinhalaHints) sinhalaKeyHints[k.lowercase().firstOrNull() ?: ' '] else null
            LetterKey(
                label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                hint = hint,
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
 *
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

/**
 * The small single-character bubble shown the instant a key is pressed —
 * ported 1:1 (visually) from FlorisBoard's PopupUiController.show() /
 * PopupBaseBox (ime/popup/PopupUi.kt + PopupUiController.kt): a plain
 * elevated box, no spring/scale-in choreography, roughly 10% taller than
 * the key itself, holding just the key's own label centered — real
 * FlorisBoard doesn't animate this in with a bounce, it's simply present
 * or not, so this doesn't either (the earlier scale+fade version here was
 * a deviation from FlorisBoard's actual look, not a match for it).
 */
@Composable
private fun KeyPreviewPopup(label: String, keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape) {
    val width = keyHeight
    val height = (keyHeight.value * 1.1f).dp
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = width, minHeight = height)
            .shadow(elevation = 2.dp, shape = keyShape)
            .clip(keyShape)
            .background(colors.specialKeyBg)
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
 * The extended popup grid shown once a long-press on a key with alternates
 * (e.g. "a" → æ ã å ā à á â ä) crosses the system long-press threshold —
 * ported behavior-wise from FlorisBoard's PopupUiController.extend() +
 * PopupExtBox (ime/popup/PopupUiController.kt lines ~139-307 and
 * PopupUi.kt's PopupExtBox). FlorisBoard's own layout rule for the number
 * of alternates n:
 *   - n <= 5:  single row, all n keys, row0 only.
 *   - n > 5 and odd:  two rows, row0 (bottom, closer to the key) has one
 *     MORE key than row1 (top) — row0count = (n+1)/2, row1count = (n-1)/2.
 *   - n > 5 and even: two equal rows of n/2 each.
 * Row0 is always the one closer to the key (bottom row visually, since
 * this whole popup sits above the key and is built "bottom row first" —
 * hence `elements` is iterated `.asReversed()` when drawing, matching
 * PopupExtBox in PopupUi.kt). The row that's one longer as an anchor
 * offset toward whichever half of the keyboard the key sits in (left keys
 * anchor left, right keys anchor right) is a screen-edge-avoidance detail
 * from the original that matters far less on a single key row this size,
 * so this port keeps the row-count split exactly but always centers the
 * whole grid above the key — simpler, and visually indistinguishable at
 * these small popup counts (max 8 for any letter here).
 *
 * @param alternates the characters to show, in FlorisBoard's own left-to-
 *   right display order (see LongPressPopupData.kt — already ported
 *   directly from FlorisBoard's en.json).
 * @param selectedIndex index into [alternates] of the character currently
 *   under the finger, supplied by the caller's own drag tracking.
 */
@Composable
private fun LongPressPopupRow(
    alternates: List<String>,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    selectedIndex: Int,
) {
    val n = alternates.size
    val row0count: Int
    val row1count: Int
    when {
        n <= 5 -> { row0count = n; row1count = 0 }
        n % 2 == 1 -> { row0count = (n + 1) / 2; row1count = (n - 1) / 2 }
        else -> { row0count = n / 2; row1count = n / 2 }
    }
    // row0 = bottom (closer to key, indices [0, row0count)),
    // row1 = top (indices [row0count, n)) — matches FlorisBoard's own
    // uiIndex convention where row1 (if present) comes first in reading
    // order but renders above row0.
    val row0 = alternates.subList(0, row0count)
    val row1 = if (row1count > 0) alternates.subList(row0count, n) else emptyList()

    val cellWidth = (keyHeight.value * 0.85f).dp
    val cellHeight = (keyHeight.value * 0.85f).dp

    Column(
        modifier = Modifier
            .shadow(elevation = 2.dp, shape = keyShape)
            .clip(keyShape)
            .background(colors.specialKeyBg),
    ) {
        if (row1.isNotEmpty()) {
            Row {
                row1.forEachIndexed { i, alt ->
                    PopupCell(alt, row0count + i, selectedIndex, cellWidth, cellHeight, colors, keyShape)
                }
            }
        }
        Row {
            row0.forEachIndexed { i, alt ->
                PopupCell(alt, i, selectedIndex, cellWidth, cellHeight, colors, keyShape)
            }
        }
    }
}

/** One character cell inside [LongPressPopupRow] — plain highlight on focus, no per-cell rounding beyond the parent's own shape, matching PopupUi.kt's PopupExtBox/SnyggBox-per-element approach. */
@Composable
private fun PopupCell(
    alt: String,
    index: Int,
    selectedIndex: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
) {
    val isSelected = index == selectedIndex
    // Fixed blue highlight (Gboard/iOS-style) for the popup's selected cell —
    // deliberately not colors.accent, since that's the user's chosen theme
    // accent and reusing it here would tie the popup highlight to whatever
    // theme color is picked instead of staying a consistent, predictable blue.
    val selectedCellColor = Color(0xFF4285F4)
    Box(
        modifier = Modifier
            .size(cellWidth, cellHeight)
            .let { if (isSelected) it.clip(keyShape).background(selectedCellColor) else it },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = alt,
            fontSize = (cellHeight.value * 0.5f).sp,
            color = if (isSelected) Color.White else colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun RowScope.NumberedLetterKey(
    label: String, number: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    // Small Sinhala-glyph hint shown top-LEFT (the number hint already
    // occupies top-right on this row) — empty/null shows nothing.
    hint: String? = null,
    onTap: () -> Unit, onLongPress: () -> Unit,
    // Called instead of onLongPress when the user drags to one of the
    // popup alternates and releases over it — same contract as LetterKey's
    // own onAlternateSelected. Row 0 (q w e r t y u i o p) sits above the
    // number layer, so a key here that HAS accent alternates (e, u, i, o)
    // shows the popup on long-press exactly like row 1/2 letters do;
    // long-pressing a key with no alternates still falls back to typing
    // its number, unchanged from before this parameter existed.
    onAlternateSelected: ((String) -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    var pressTick by remember { mutableStateOf(0) } // see LetterKey for why the preview triggers off this, not `pressed`
    val bumpScale = rememberKeyBumpScale(pressed)
    val bumpOffsetY = rememberKeyBumpOffsetY(pressed)

    // Long-press popup (accented alternates PLUS this key's own row
    // number, inserted as an extra cell in the middle of the bottom row —
    // see rowNumberForKey in LongPressPopupData.kt). Base accent list is
    // ported the same as LetterKey's own (see that composable's doc
    // comments); duplicated here rather than sharing a helper because
    // RowScope.LetterKey and RowScope.NumberedLetterKey differ in what
    // they render around the popup (the small number hint in the corner)
    // and in what a plain long-press-with-no-alternates-AND-no-number
    // falls back to.
    val alternates = remember(label, number) {
        val baseAlts = longPressPopupAlternates[label.lowercase().firstOrNull() ?: ' ']
            ?.let { alts -> if (label.firstOrNull()?.isUpperCase() == true) alts.map { it.uppercase() } else alts }
            ?: emptyList()
        if (number.isEmpty()) {
            baseAlts
        } else if (baseAlts.isEmpty()) {
            // No accents at all (q, w, r, t, y, p) — the popup still
            // shows, just with the number as its only cell, so every
            // row-0 key's number stays reachable via long-press the same
            // way.
            listOf(number)
        } else {
            // Insert the number into the middle of the LAST baseAlts.size/2
            // items (which the row-splitting math below always assigns to
            // the bottom row) so it lands in the middle of the bottom row
            // specifically — matches the reference layout (e.g. "e" → top
            // row ē ê ë, bottom row è 3 é), not just "somewhere in the
            // combined list".
            val withNumber = baseAlts.toMutableList()
            val n = withNumber.size + 1
            val newRow0count = when {
                n <= 5 -> n
                n % 2 == 1 -> (n + 1) / 2
                else -> n / 2
            }
            val bottomRowStart = withNumber.size - (newRow0count - 1)
            val insertAt = bottomRowStart + (newRow0count - 1) / 2
            withNumber.add(insertAt.coerceIn(0, withNumber.size), number)
            withNumber
        }
    }
    var popupVisible by remember { mutableStateOf(false) }
    var selectedAltIndex by remember { mutableStateOf(0) }
    var keyWidthPx by remember { mutableStateOf(0) }
    val row0count = remember(alternates) {
        val n = alternates.size
        when {
            n <= 5 -> n
            n % 2 == 1 -> (n + 1) / 2
            else -> n / 2
        }
    }
    val row1count = alternates.size - row0count
    val density = LocalDensity.current
    val cellStridePx = with(density) { (keyHeight.value * 0.85f).dp.toPx() }
    val cellHeightPx = cellStridePx
    val popupYOffsetPx = with(density) { (keyHeight.value * 0.15f).dp.toPx() }

    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .keyEffectDecoration(colors, keyShape)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .onSizeChanged { keyWidthPx = it.width }
            .pointerInput(alternates) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    pressTick++
                    popupVisible = false
                    selectedAltIndex = 0

                    val longPressThresholdMs = android.view.ViewConfiguration.getLongPressTimeout().toLong()
                    val releasedBeforeLongPress: Boolean? = withTimeoutOrNull(longPressThresholdMs) {
                        waitForUpOrCancellation() != null
                    }

                    if (releasedBeforeLongPress != null) {
                        // Ordinary tap.
                        pressed = false
                        if (releasedBeforeLongPress) {
                            onTap()
                        }
                        return@awaitEachGesture
                    }

                    // Long-press threshold reached while still down.
                    if (alternates.isNotEmpty()) {
                        popupVisible = true
                    } else {
                        // No alternates for this key — same as before:
                        // a long-press just types the row's number.
                        onLongPress()
                        pressed = false
                        // Still wait for the actual finger-up so a
                        // subsequent tap isn't misread as starting mid-gesture.
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (popupVisible && alternates.isNotEmpty()) {
                                val chosen = alternates.getOrNull(selectedAltIndex)
                                if (chosen != null && onAlternateSelected != null) {
                                    onAlternateSelected(chosen)
                                } else if (chosen != null) {
                                    onTap()
                                }
                            }
                            pressed = false
                            popupVisible = false
                            change.consume()
                            break
                        }
                        if (popupVisible && alternates.isNotEmpty() && keyWidthPx > 0) {
                            val distAboveKeyTopPx = -change.position.y
                            val distAbovePopupBottomPx = distAboveKeyTopPx - popupYOffsetPx
                            // Matches LongPressPopupRow's actual layout: row0
                            // (indices [0, row0count)) renders as the BOTTOM row
                            // (closer to the key), row1 (indices [row0count, n))
                            // renders as the TOP row. So a finger higher up
                            // (inRow1) must map to indices starting at row0count,
                            // not 0 — the reverse of what row0/bottom uses.
                            val inRow1 = row1count > 0 && distAbovePopupBottomPx > cellHeightPx
                            val rowAlts = if (inRow1) row1count else row0count
                            val rowStartIndex = if (inRow1) row0count else 0

                            val rowWidthPx = cellStridePx * rowAlts
                            val keyCenterPx = keyWidthPx / 2f
                            val rowLeftEdgePx = keyCenterPx - rowWidthPx / 2f
                            val posInRowPx = change.position.x - rowLeftEdgePx
                            val rawCol = (posInRowPx / cellStridePx).toInt().coerceIn(0, rowAlts - 1)
                            selectedAltIndex = (rowStartIndex + rawCol).coerceIn(0, alternates.lastIndex)
                        }
                        change.consume()
                    }
                }
            }
    ) {
        Text(text = number, fontSize = keyNumberFontSize(keyHeight), color = colors.subText,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        if (!hint.isNullOrEmpty()) {
            Text(text = hint, fontSize = keyNumberFontSize(keyHeight), color = colors.subText,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 3.dp, start = 4.dp))
        }
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center))
        if (popupVisible && alternates.isNotEmpty()) {
            val rowCount = if (row1count > 0) 2 else 1
            val offsetYPx = -(cellHeightPx * rowCount + popupYOffsetPx)
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetYPx.toInt())) {
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
    // Small Sinhala-glyph hint shown top-right (mirrors the top row's
    // number hints) — empty/null shows nothing, unchanged from before
    // this feature existed. Passed in already-resolved (per currentLanguage)
    // by the caller so this composable stays language-agnostic.
    hint: String? = null,
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
    // Row split, mirroring LongPressPopupRow's own row0count/row1count math
    // exactly (see that composable's doc comment for the FlorisBoard rule
    // this ports) — needed here too so the drag-to-select hit-testing below
    // agrees with what's actually being drawn: row0 (bottom, closer to the
    // key) holds indices [0, row0count), row1 (top) holds the rest.
    val row0count = remember(alternates) {
        val n = alternates.size
        when {
            n <= 5 -> n
            n % 2 == 1 -> (n + 1) / 2
            else -> n / 2
        }
    }
    val row1count = alternates.size - row0count
    // Cell size used both here and in LongPressPopupRow — kept as one
    // shared calculation so the two never drift out of sync with each
    // other (matches PopupCell's 0.85x keyHeight sizing, no inter-cell
    // spacing — FlorisBoard's own PopupExtBox cells sit flush against
    // each other).
    val density = LocalDensity.current
    val cellStridePx = with(density) { (keyHeight.value * 0.85f).dp.toPx() }
    val cellHeightPx = cellStridePx
    // How far above the key's top edge the popup's own bottom edge sits —
    // shared between the Popup(...) call's own offset and the drag-to-row
    // hit-testing above, so the two can never disagree about where the
    // popup actually is on screen.
    val popupYOffsetPx = with(density) { (keyHeight.value * 0.15f).dp.toPx() }

    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .offset(y = bumpOffsetY)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .keyEffectDecoration(colors, keyShape)
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
                            // Which of the popup's up to two rows is under
                            // the finger. change.position.y is relative to
                            // THIS key's own top edge and, since Compose
                            // keeps reporting positions even once the
                            // finger has moved outside the key's own
                            // bounds, goes negative as the finger moves up
                            // above the key — exactly where the popup is.
                            // The popup's bottom edge sits popupYOffsetPx
                            // above the key's top edge (see the matching
                            // offsetYPx in the Popup(...) call below), and
                            // row0 (bottom/closer row) occupies the
                            // cellHeightPx nearest that bottom edge, with
                            // row1 (if present) directly above it.
                            val distAboveKeyTopPx = -change.position.y
                            val distAbovePopupBottomPx = distAboveKeyTopPx - popupYOffsetPx
                            // row0 (bottom/closer row) is indices [0, row0count)
                            // in LongPressPopupRow's actual rendering, with row1
                            // (top row, if any) starting at row0count — so a
                            // finger higher up (inRow1) must map to indices
                            // starting at row0count, not 0.
                            val inRow1 = row1count > 0 && distAbovePopupBottomPx > cellHeightPx
                            val rowAlts = if (inRow1) row1count else row0count
                            val rowStartIndex = if (inRow1) row0count else 0

                            // Column within that row: same centered-row
                            // horizontal math as before, just using that
                            // row's own cell count instead of the full
                            // alternates.size.
                            val rowWidthPx = cellStridePx * rowAlts
                            val keyCenterPx = keyWidthPx / 2f
                            val rowLeftEdgePx = keyCenterPx - rowWidthPx / 2f
                            val posInRowPx = change.position.x - rowLeftEdgePx
                            val rawCol = (posInRowPx / cellStridePx).toInt().coerceIn(0, rowAlts - 1)
                            selectedAltIndex = (rowStartIndex + rawCol).coerceIn(0, alternates.lastIndex)
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!hint.isNullOrEmpty()) {
            Text(text = hint, fontSize = keyNumberFontSize(keyHeight), color = colors.subText,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        }
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal)
        if (popupVisible && alternates.isNotEmpty()) {
            // Popup height depends on row count (1 or 2 rows of
            // cellHeightPx each — see LongPressPopupRow) plus the fixed
            // gap above the key (popupYOffsetPx), so the offset here must
            // match the row-count-aware math used for drag hit-testing
            // above, or the visible popup and the "which cell is the
            // finger over" calculation would silently disagree.
            val rowCount = if (row1count > 0) 2 else 1
            val offsetYPx = -(cellHeightPx * rowCount + popupYOffsetPx)
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetYPx.toInt())) {
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
                        colors = colors, keyShape = keyShape, onTap = { onKey(ch) })
                }
            }

            // Row 2: symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape, onTap = { onKey(ch) })
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
                        colors = colors, keyShape = keyShape, onTap = { onKey(ch) })
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
// Decorative text picker — Board.DECORATION. Same layout skeleton as
// FontPickerView above (own full-page board, toolbar/emoji-row hidden while
// it's open), plus an enable Switch at the top: the style list only matters
// once the feature itself is on, same relationship as e.g. Vibrate on tap +
// Vibration level in SoundVibrationScreen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DecorationPickerView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    // "Incognito" — deliberately placed above the "Decorate suggestions"
    // row below (per the request this was added for: a new button sitting
    // above the existing decorate-suggestions toggle). Independent of
    // enabled/onEnabledChange — a user can be in Incognito with or without
    // Decorate suggestions on, same relationship as Decorate suggestions
    // has with Vary styles. Default OFF (see
    // PreferencesManager.Keys.INCOGNITO_ENABLED's doc comment) — the
    // service only reads this to gate learning/history writes; it doesn't
    // change anything about typing itself, so leaving it off changes
    // nothing for existing users.
    incognitoEnabled: Boolean,
    onIncognitoChange: (Boolean) -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    varyStyles: Boolean,
    // Opens the separate Board.DECORATION_STYLES page (Vary styles switch +
    // the fixed-style list) — see DecorationStylesView below.
    onStylesOpen: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp

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
                text = "Decorative text",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        // ── Incognito row — sits above "Decorate suggestions" below. Same
        // switch look/spacing as every other row on this page. No leading
        // icon (removed) — the row reads as its own button via its label
        // rather than part of the decoration group below it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    "Incognito",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.keyText
                )
                Text(
                    "Pauses learning: nothing typed is added to your dictionary, clipboard history, or recent emoji while on",
                    fontSize = 11.sp,
                    color = colors.subText
                )
            }
            androidx.compose.material3.Switch(
                checked = incognitoEnabled,
                onCheckedChange = onIncognitoChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = DeshGreen)
            )
        }

        // ── Enable row — same switch look as SoundVibrationScreen's rows ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Decorate suggestions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.keyText
                )
                Text(
                    "Wraps suggestion-bar words in a decorative style; tap one to apply it",
                    fontSize = 11.sp,
                    color = colors.subText
                )
            }
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = DeshGreen)
            )
        }

        // "Styles" row — only shown once the feature is actually on, same
        // hide-not-dim treatment the list previously had. Tapping it opens
        // the separate Board.DECORATION_STYLES page (Vary styles switch +
        // the fixed-style list), rather than showing that content inline
        // here — see DecorationStylesView below.
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStylesOpen() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Styles",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.keyText
                    )
                    Text(
                        if (varyStyles) "Varying — cycles a different style per suggestion" else "Choose a fixed style",
                        fontSize = 11.sp,
                        color = colors.subText
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 180f),
                    tint = colors.subText
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Decoration styles picker — Board.DECORATION_STYLES. Split out of
// DecorationPickerView so the "Vary styles" switch + fixed-style list get
// their own page instead of sharing DECORATION's page with Incognito /
// Decorate suggestions. Reached via the "Styles" row on DecorationPickerView
// (pushBoard(Board.DECORATION_STYLES)); "Back" pops back to DECORATION, not
// MAIN, same stack behaviour as every other sub-board. Uses the exact same
// header pattern and targetContentHeight sizing as DecorationPickerView (and
// FontPickerView, ClipboardHistoryView, etc.) so this page renders at the
// same size as the keyboard itself, not a different height.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DecorationStylesView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    varyStyles: Boolean,
    onVaryStylesChange: (Boolean) -> Unit,
    selectedStyleKey: String,
    onStyleSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp

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
                text = "Styles",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Vary styles",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.keyText
                )
                Text(
                    "Cycle a different style per suggestion instead of picking one below",
                    fontSize = 11.sp,
                    color = colors.subText
                )
            }
            androidx.compose.material3.Switch(
                checked = varyStyles,
                onCheckedChange = onVaryStylesChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = DeshGreen)
            )
        }

        // The fixed-style list only matters once "Vary styles" is off —
        // picking a row here is what "Vary styles" would otherwise
        // override. Shown regardless (dimmed, not hidden) since it's the
        // only content on this page.
        //
        // BUG FIX (page wouldn't scroll): this LazyColumn previously had
        // no explicit LazyListState and relied purely on weight(1f) for
        // its height. That's normally enough, but this Column's own
        // height is an *explicit* fixed Dp (targetContentHeight) sitting
        // inside an ancestor chain that is wrapContentHeight() all the
        // way up (see the outer Box/Column in KeyboardView) — under
        // those conditions Compose can resolve this weight(1f) pass
        // against a min-height-0/max-height-unbounded constraint on the
        // very first composition, which makes the list lay out at its
        // full intrinsic (unscrollable) height instead of the intended
        // fixed viewport. Constraining with a matching heightIn(max=)
        // derived from the same explicit targetContentHeight forces a
        // bounded, scrollable viewport regardless of how the ancestor
        // chain resolves its own pass, and an explicit
        // rememberLazyListState() plus userScrollEnabled = true rule out
        // the list silently losing its scroll state across recomposition.
        val decorationListState = rememberLazyListState()
        LazyColumn(
            state = decorationListState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .heightIn(max = targetContentHeight),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(DecorationStyle.pickable, key = { it.key }) { style ->
                DecorationRow(
                    style = style,
                    selected = style.key == selectedStyleKey,
                    dimmed = varyStyles,
                    colors = colors,
                    onSelect = { onStyleSelected(style.key) }
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hidden-message decode banner — see KeyboardView's hiddenMessageDecodedText
// param doc comment. A read-only reveal, not an editable row like
// TranslateRow: the user didn't open this, a matching copy triggered it, so
// it only ever needs to show text and offer dismissal.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HiddenMessageBanner(
    colors: KeyboardColors,
    decodedText: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeshGreen.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = DeshGreen
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = "Hidden message found",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.subText
            )
            Text(
                text = decodedText,
                fontSize = 15.sp,
                color = colors.keyText,
                maxLines = 3
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(16.dp),
                tint = colors.subText
            )
        }
    }
}

@Composable
private fun DecorationRow(
    style: DecorationStyle,
    selected: Boolean,
    dimmed: Boolean,
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .alpha(if (dimmed) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.label,
                fontSize = 13.sp,
                color = colors.subText,
            )
            Text(
                // Same principle as FontRow above — the exact string that
                // will wrap the committed/suggested word, not an approximation.
                text = TextDecorator.apply("word", style),
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
