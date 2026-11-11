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
 * Same wave/cycle/stars glow drawing as above, but selected via the
 * Themes-screen "Effects" grid (KeyEffect.WAVE/CYCLE/STARS) rather than the
 * separate LedPattern picker — see KeyboardLedRipple's doc comment for why
 * these live in KeyboardLighting.kt rather than as LedPattern cases: the
 * actual border-glow math (drawWaveBorders/drawCycleBorders/
 * drawStarsBorders) is identical, only which preference selects it differs.
 */
@Composable
internal fun KeyboardKeyEffectRipple(
    effect: com.spmods.sinkey.data.KeyEffect,
    origin: Offset?,
    triggerId: Int,
    keyPositions: List<KeyPoint>,
    keySizes: Map<Char, androidx.compose.ui.geometry.Size>,
    accent: Color,
    isIdle: Boolean,
    modifier: Modifier = Modifier
) {
    val isRippleEffect = effect == com.spmods.sinkey.data.KeyEffect.WAVE ||
        effect == com.spmods.sinkey.data.KeyEffect.CYCLE ||
        effect == com.spmods.sinkey.data.KeyEffect.STARS
    if (!isRippleEffect || origin == null || keyPositions.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(triggerId, effect) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
    }

    if (progress.value >= 1f) return

    val idleAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isIdle) 0.4f else 1f,
        animationSpec = tween(900),
        label = "keyEffectLedIdleAlpha"
    )

    Canvas(modifier = modifier) {
        val fallbackSize = if (keySizes.isNotEmpty()) {
            androidx.compose.ui.geometry.Size(
                keySizes.values.map { it.width }.average().toFloat(),
                keySizes.values.map { it.height }.average().toFloat()
            )
        } else {
            androidx.compose.ui.geometry.Size(size.width / 10f, size.height / 4f)
        }
        when (effect) {
            com.spmods.sinkey.data.KeyEffect.WAVE -> drawWaveBorders(origin, keyPositions, keySizes, fallbackSize, accent, progress.value, idleAlpha)
            com.spmods.sinkey.data.KeyEffect.CYCLE -> drawCycleBorders(origin, keyPositions, keySizes, fallbackSize, progress.value, idleAlpha)
            com.spmods.sinkey.data.KeyEffect.STARS -> drawStarsBorders(origin, keyPositions, keySizes, fallbackSize, accent, progress.value, idleAlpha)
            else -> Unit
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
            Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
