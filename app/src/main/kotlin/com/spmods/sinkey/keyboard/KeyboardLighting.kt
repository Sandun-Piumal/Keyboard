package com.spmods.sinkey.keyboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.State
import com.spmods.sinkey.data.LedPattern
import com.spmods.sinkey.data.TypingAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * True once at least [idleAfterMs] has passed since [lastActivityAtMs] with
 * no further key presses — drives LedPattern's "idle dimming" behavior.
 * Returns false immediately (never idle) when [enabled] is false, so the
 * LED strip's own idleAlpha animation stays at full brightness when the
 * user has turned idle-dimming off.
 */
@Composable
fun rememberKeyboardIdleState(lastActivityAtMs: Long, enabled: Boolean, idleAfterMs: Long = 3000L): State<Boolean> {
    val state = remember { mutableStateOf(false) }
    LaunchedEffect(lastActivityAtMs, enabled) {
        if (!enabled) {
            state.value = false
            return@LaunchedEffect
        }
        state.value = false
        delay(idleAfterMs)
        state.value = true
    }
    return state
}

/**
 * Decodes the user's DIY typing-animation image (see
 * PreferencesManager.typingAnimationImageUri) off the main thread,
 * re-decoding whenever the Uri changes. Returns null while decoding or if
 * none is set — callers already handle a null bitmap by simply not
 * drawing anything (see TypingAnimationPopup's CUSTOM_IMAGE branch).
 */
@Composable
fun rememberCustomAnimationBitmap(uriString: String?): Bitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = if (uriString != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }
    return bitmap
}

/**
 * Material You (Android 12+) dynamic accent color derived from the system
 * wallpaper, for use as the keyboard's KeyColorPalette-equivalent accent
 * when the user turns on "Material You" in Themes. Falls back to the
 * existing DeshGreen accent on API < 31 or if the dynamic color resources
 * are unavailable for any reason — never crashes, never returns a jarring
 * default like pure black/white.
 */
fun materialYouAccentColor(context: Context, isDark: Boolean): Color {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
        return DeshGreen
    }
    return runCatching {
        // android.R.color.system_accent1_400 (light) / _200 (dark) are the
        // framework's own dynamic-color swatches, generated from the
        // current wallpaper by the system at boot / wallpaper-change time —
        // no separate "Material You" library dependency needed for just
        // reading these resolved colors.
        val resId = if (isDark) {
            android.R.color.system_accent1_200
        } else {
            android.R.color.system_accent1_400
        }
        Color(context.resources.getColor(resId, context.theme))
    }.getOrDefault(DeshGreen)
}

/**
 * "LED / Neon Lighting" — draws a glowing border outline around each key,
 * lit up as the light spreads outward from the pressed key, restarting on
 * every press. This traces each key's own border rather than filling the
 * key or drawing a free-floating dot at its center — "the light runs along
 * the border of the keys around the one I pressed" is the actual visual,
 * not a colored blob sitting on top of the key. Keys further from the
 * pressed one light up later (WAVE/CYCLE) or dimmer (BREATHING/STARS), so
 * the light visibly originates at the pressed key and travels outward
 * across the board through neighboring keys' borders.
 *
 * [keySizes] gives each key's real measured (width, height) in px, keyed
 * by the same Char used in [keyPositions] — captured from that key's own
 * onGloballyPositioned callback in KeyboardView, not estimated, so the
 * glow outline matches each key's actual size and shape, including rows
 * (like the bottom quick-toggle row with Space/./Enter) whose keys are a
 * genuinely different size than the main letter rows. A key missing from
 * this map (e.g. its own position hasn't been measured yet on the very
 * first frame after a board switch) falls back to an average of whatever
 * sizes are already known, computed once per draw call rather than passed
 * in, so this stays a pure function of its two map arguments.
 *
 * [origin] is the pressed key's center (this Box's local coordinates,
 * matching [keyPositions]'s coordinate space) and [triggerId] is bumped by
 * the caller on every press — including two consecutive presses of the
 * exact same key — so the glow restarts from scratch each time even when
 * origin's value didn't change (a plain Offset key wouldn't retrigger for
 * that case). Nothing is drawn once the current glow has fully faded, and
 * nothing is drawn at all when [pattern] is NONE or no press has happened
 * yet ([origin] null) — this only reacts to real typing, it doesn't run
 * on its own the way an ambient strip would.
 *
 * [isIdle] dims the glow's peak alpha when true (see
 * rememberKeyboardIdleState), for the "idle dimming" preference — mostly
 * relevant right as an idle glow is fading, since a genuinely idle board
 * has no presses left to trigger new glows anyway.
 */
// KeyPoint (see GestureWordMatcher.kt) is `internal` to this module, so any
// public function taking it as a parameter type would leak that internal
// type through a public API surface — Kotlin's explicit-API-safety check
// catches this at compile time. KeyboardLedRipple is only ever called from
// within this same module (KeyboardView.kt), so `internal` here costs
// nothing and satisfies that check.
@Composable
internal fun KeyboardLedRipple(
    pattern: LedPattern,
    origin: Offset?,
    triggerId: Int,
    keyPositions: List<KeyPoint>,
    keySizes: Map<Char, androidx.compose.ui.geometry.Size>,
    accent: Color,
    isIdle: Boolean,
    modifier: Modifier = Modifier
) {
    if (pattern == LedPattern.NONE || origin == null || keyPositions.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(triggerId, pattern) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = if (pattern == LedPattern.BREATHING) 900 else 650,
                easing = androidx.compose.animation.core.LinearOutSlowInEasing
            )
        )
    }

    if (progress.value >= 1f) return

    val idleAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isIdle) 0.4f else 1f,
        animationSpec = tween(900),
        label = "ledIdleAlpha"
    )

    Canvas(modifier = modifier) {
        // Fallback for the rare key whose real size hasn't been measured
        // yet (e.g. the very first frame right after a board switch) — an
        // average of whatever sizes ARE known so far, so that one key
        // isn't drawn with a wildly wrong placeholder box while the rest
        // use their real size. Falls back further to keyPositions' own
        // count-based default only if literally nothing has been measured.
        val fallbackSize = if (keySizes.isNotEmpty()) {
            androidx.compose.ui.geometry.Size(
                keySizes.values.map { it.width }.average().toFloat(),
                keySizes.values.map { it.height }.average().toFloat()
            )
        } else {
            androidx.compose.ui.geometry.Size(size.width / 10f, size.height / 4f)
        }
        when (pattern) {
            LedPattern.NONE -> Unit
            LedPattern.BREATHING -> drawBreathingBorders(origin, keyPositions, keySizes, fallbackSize, accent, progress.value, idleAlpha)
            LedPattern.WAVE -> drawWaveBorders(origin, keyPositions, keySizes, fallbackSize, accent, progress.value, idleAlpha)
            LedPattern.CYCLE -> drawCycleBorders(origin, keyPositions, keySizes, fallbackSize, progress.value, idleAlpha)
            LedPattern.STARS -> drawStarsBorders(origin, keyPositions, keySizes, fallbackSize, accent, progress.value, idleAlpha)
        }
    }
}

/** Max travel distance (dp) any LedPattern's glow spreads outward from the pressed key. */
private const val LED_RIPPLE_MAX_RADIUS_DP = 300

/** Stroke width (dp) of every key's glowing border outline. */
private const val LED_BORDER_STROKE_DP = 2.5f

/** Draws one key's glowing border outline — shared by every LedPattern below so the outline shape/inset is identical across patterns. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKeyBorderGlow(
    key: KeyPoint, keySize: androidx.compose.ui.geometry.Size, color: Color, alpha: Float
) {
    // Inset slightly from the full key size so the glow traces just inside
    // each key's edge (matching where a real key border/outline sits)
    // rather than overlapping into the gap between adjacent keys.
    val insetPx = 3.dp.toPx()
    val topLeft = Offset(key.x - keySize.width / 2f + insetPx, key.y - keySize.height / 2f + insetPx)
    val glowSize = androidx.compose.ui.geometry.Size(
        (keySize.width - insetPx * 2).coerceAtLeast(1f),
        (keySize.height - insetPx * 2).coerceAtLeast(1f)
    )
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = topLeft,
        size = glowSize,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = LED_BORDER_STROKE_DP.dp.toPx())
    )
}

/** BREATHING: every key's border softly pulses in unison, brightest near the origin, fading together as progress ages. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBreathingBorders(
    origin: Offset, keyPositions: List<KeyPoint>, keySizes: Map<Char, androidx.compose.ui.geometry.Size>, fallbackSize: androidx.compose.ui.geometry.Size, accent: Color, progress: Float, idleAlpha: Float
) {
    val maxDist = LED_RIPPLE_MAX_RADIUS_DP.dp.toPx()
    // A single breath cycle (brighten then dim) over the whole animation,
    // rather than a wave front — sin(progress * PI) rises then falls back
    // to 0 exactly at progress = 1.
    val breath = kotlin.math.sin(progress * Math.PI).toFloat().coerceIn(0f, 1f)
    keyPositions.forEach { key ->
        val dx = key.x - origin.x
        val dy = key.y - origin.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val falloff = (1f - (dist / maxDist)).coerceIn(0f, 1f)
        val alpha = (breath * falloff * idleAlpha).coerceIn(0f, 1f)
        if (alpha > 0.02f) {
            drawKeyBorderGlow(key, keySizes[key.char] ?: fallbackSize, accent, alpha)
        }
    }
}

/** WAVE: a ring of light expands outward from the pressed key, lighting each key's border as the front passes over it. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveBorders(
    origin: Offset, keyPositions: List<KeyPoint>, keySizes: Map<Char, androidx.compose.ui.geometry.Size>, fallbackSize: androidx.compose.ui.geometry.Size, accent: Color, progress: Float, idleAlpha: Float
) {
    val maxRadiusPx = LED_RIPPLE_MAX_RADIUS_DP.dp.toPx()
    val waveFrontRadius = progress * maxRadiusPx
    val bandWidthPx = 0.4f * maxRadiusPx
    keyPositions.forEach { key ->
        val dx = key.x - origin.x
        val dy = key.y - origin.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val distanceFromFront = kotlin.math.abs(dist - waveFrontRadius)
        if (distanceFromFront < bandWidthPx) {
            val bandStrength = 1f - (distanceFromFront / bandWidthPx)
            val overallFade = 1f - progress
            val alpha = (bandStrength * overallFade * idleAlpha).coerceIn(0f, 1f)
            if (alpha > 0.02f) {
                drawKeyBorderGlow(key, keySizes[key.char] ?: fallbackSize, accent, alpha)
            }
        }
    }
}

/** CYCLE: same expanding-ring wave as WAVE, but each border's hue rotates through the rainbow by distance instead of using a single accent color. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCycleBorders(
    origin: Offset, keyPositions: List<KeyPoint>, keySizes: Map<Char, androidx.compose.ui.geometry.Size>, fallbackSize: androidx.compose.ui.geometry.Size, progress: Float, idleAlpha: Float
) {
    val maxRadiusPx = LED_RIPPLE_MAX_RADIUS_DP.dp.toPx()
    val waveFrontRadius = progress * maxRadiusPx
    val bandWidthPx = 0.4f * maxRadiusPx
    keyPositions.forEach { key ->
        val dx = key.x - origin.x
        val dy = key.y - origin.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val distanceFromFront = kotlin.math.abs(dist - waveFrontRadius)
        if (distanceFromFront < bandWidthPx) {
            val bandStrength = 1f - (distanceFromFront / bandWidthPx)
            val overallFade = 1f - progress
            val alpha = (bandStrength * overallFade * idleAlpha).coerceIn(0f, 1f)
            if (alpha > 0.02f) {
                val hue = (dist / maxRadiusPx * 360f) % 360f
                drawKeyBorderGlow(key, keySizes[key.char] ?: fallbackSize, Color.hsv(hue, 0.85f, 1f), alpha)
            }
        }
    }
}

/** STARS: a handful of keys near the pressed one have their borders twinkle briefly and independently, rather than one continuous front. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarsBorders(
    origin: Offset, keyPositions: List<KeyPoint>, keySizes: Map<Char, androidx.compose.ui.geometry.Size>, fallbackSize: androidx.compose.ui.geometry.Size, accent: Color, progress: Float, idleAlpha: Float
) {
    val maxDist = LED_RIPPLE_MAX_RADIUS_DP.dp.toPx()
    keyPositions.forEach { key ->
        val dx = key.x - origin.x
        val dy = key.y - origin.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist > maxDist) return@forEach
        // Each key gets its own twinkle phase (derived from its position so
        // it's stable across recompositions) so they don't all flash in
        // perfect unison — reads as scattered stars rather than one pulse.
        val phaseOffset = Random((key.x * 1000 + key.y).toInt()).nextFloat()
        val twinklePhase = ((progress + phaseOffset) % 1f)
        val twinkle = kotlin.math.sin(twinklePhase * Math.PI).toFloat().coerceIn(0f, 1f)
        val falloff = (1f - (dist / maxDist)).coerceIn(0f, 1f)
        val alpha = (twinkle * falloff * idleAlpha).coerceIn(0f, 1f)
        if (alpha > 0.05f) {
            drawKeyBorderGlow(key, keySizes[key.char] ?: fallbackSize, accent, alpha)
        }
    }
}

/**
 * A single floating emoji/image pop-up that rises and fades out above
 * whichever key was actually pressed — the "Typing Animation" feature.
 * [keyPositionPx] is that key's real center, in the same local-coordinate
 * space as KeyboardView's [KeyPoint] tracking (i.e. relative to the Box
 * this is called from), NOT a fixed spot — every press should pop the
 * animation over the specific key that was hit, not always in one place.
 * Call with a changing [trigger] (e.g. a press counter) to retrigger the
 * animation on each press; this composable owns its own rise/fade
 * Animatable and resets it via LaunchedEffect(trigger).
 */
@Composable
fun TypingAnimationPopup(
    trigger: Int,
    animation: TypingAnimation,
    customEmoji: String,
    customImageBitmap: android.graphics.Bitmap?,
    keyPositionPx: Offset?,
    modifier: Modifier = Modifier
) {
    if (animation == TypingAnimation.NONE || trigger == 0 || keyPositionPx == null) return

    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    // Pick one glyph from the preset's cycle each trigger, so repeated
    // presses don't always show the exact same emoji.
    val glyph = remember(trigger) {
        animation.emojiSet.takeIf { it.isNotEmpty() }?.let { it[trigger % it.size] } ?: ""
    }
    // Freeze the position for the lifetime of this one animation run —
    // keyPositionPx keeps changing on every subsequent press, but a glow
    // already mid-flight over a previous key shouldn't jump to the new
    // key's position, it should keep rising from where it started.
    var frozenPosition by remember { mutableStateOf(keyPositionPx) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        frozenPosition = keyPositionPx
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(550, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        visible = false
    }

    if (!visible) return
    val anchoredAt = frozenPosition ?: return

    val riseOffsetY = (-36 * progress.value).dp
    val alpha = (1f - progress.value).coerceIn(0f, 1f)
    val scale = 0.7f + 0.5f * (1f - progress.value)

    // Popup renders into its own window and is positioned by `alignment` +
    // `offset` relative to the calling composable's bounds — not by any
    // Modifier (Popup has no `modifier` parameter, since its content isn't
    // a normal child of the caller's own layout tree). Alignment.TopStart
    // anchors the offset's origin at (0, 0) of the caller's bounds, so
    // adding the pressed key's own pixel position places this exactly
    // above that key rather than at one fixed spot on the board — a
    // further -40dp Y nudge lifts it clear of the key itself, roughly
    // level with the existing per-key press-preview bubble.
    val density = LocalDensity.current
    val liftPx = with(density) { 40.dp.roundToPx() }
    val popupOffset = IntOffset(
        anchoredAt.x.toInt(),
        anchoredAt.y.toInt() - liftPx
    )

    Popup(alignment = Alignment.TopStart, offset = popupOffset) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    // graphicsLayer's own translation/scale/alpha are all
                    // relative to this Box's own bounds, which Popup has
                    // already placed at popupOffset — so this only needs
                    // to add the rise/fade/scale motion on top, not the
                    // base position.
                    translationX = -placeholderHalfWidthPx
                    translationY = riseOffsetY.toPx()
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            when (animation) {
                TypingAnimation.CUSTOM_IMAGE -> {
                    if (customImageBitmap != null) {
                        Image(
                            bitmap = customImageBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                TypingAnimation.CUSTOM_EMOJI -> {
                    Text(customEmoji, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Rough half-width (px, at typical density) of the popup's own content,
 * used to re-center it horizontally on the key it's anchored to — Popup's
 * offset places its *top-left* corner at the given pixel, so without this
 * the emoji/image would appear shifted right of the key's actual center.
 * A fixed estimate rather than a real measured width: exact centering
 * doesn't matter for a small pop-up that also rises and fades quickly, and
 * a real measurement would need a two-pass layout (measure then
 * reposition) that isn't worth the complexity here.
 */
private const val placeholderHalfWidthPx = 24f
