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
 * Thin ambient lighting strip along the top edge of the keyboard, animated
 * per [pattern] — the "LED / Neon Lighting" feature. Distinct from the
 * per-key KeyEffect: this is one continuous strip reacting as a whole,
 * not a decoration on individual keys, so the two layer independently
 * (e.g. RIPPLE key-effect + WAVE strip both active at once).
 *
 * [isIdle] dims the strip's overall alpha when true (see
 * rememberKeyboardIdleState) — the "idle dimming" behavior from the LED
 * pattern preferences, only meaningful when the user has that sub-toggle on.
 */
@Composable
fun KeyboardLedStrip(pattern: LedPattern, accent: Color, isIdle: Boolean, modifier: Modifier = Modifier) {
    if (pattern == LedPattern.NONE) return

    val idleAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isIdle) 0.35f else 1f,
        animationSpec = tween(900),
        label = "ledIdleAlpha"
    )

    Box(modifier = modifier.fillMaxWidth().height(3.dp)) {
        when (pattern) {
            LedPattern.NONE -> Unit
            LedPattern.BREATHING -> BreathingStrip(accent, idleAlpha)
            LedPattern.WAVE -> WaveStrip(accent, idleAlpha)
            LedPattern.CYCLE -> CycleStrip(idleAlpha)
            LedPattern.STARS -> StarsStrip(accent, idleAlpha)
        }
    }
}

@Composable
private fun BreathingStrip(accent: Color, idleAlpha: Float) {
    val infinite = rememberInfiniteTransition(label = "ledBreathing")
    val brightness by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "ledBreathingAlpha"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        drawRect(color = accent.copy(alpha = brightness * idleAlpha))
    }
}

@Composable
private fun WaveStrip(accent: Color, idleAlpha: Float) {
    val infinite = rememberInfiniteTransition(label = "ledWave")
    val pos by infinite.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ledWavePos"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        val bandCenter = pos * size.width
        val bandWidth = size.width * 0.28f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = idleAlpha), Color.Transparent),
                start = Offset(bandCenter - bandWidth, 0f),
                end = Offset(bandCenter + bandWidth, 0f)
            )
        )
    }
}

@Composable
private fun CycleStrip(idleAlpha: Float) {
    val infinite = rememberInfiniteTransition(label = "ledCycle")
    val hue by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "ledCycleHue"
    )
    val colors = remember(hue) {
        listOf(
            Color.hsv(hue % 360f, 0.9f, 1f),
            Color.hsv((hue + 90f) % 360f, 0.9f, 1f),
            Color.hsv((hue + 180f) % 360f, 0.9f, 1f),
            Color.hsv((hue + 270f) % 360f, 0.9f, 1f),
        )
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        drawRect(
            brush = Brush.linearGradient(
                colors = colors.map { it.copy(alpha = idleAlpha) },
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f)
            )
        )
    }
}

@Composable
private fun StarsStrip(accent: Color, idleAlpha: Float) {
    val infinite = rememberInfiniteTransition(label = "ledStars")
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "ledStarsTwinkle"
    )
    val positions = remember { List(24) { Random(it).nextFloat() } }
    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        positions.forEachIndexed { i, x ->
            val phase = ((twinkle + i * 0.13f) % 1f)
            val a = (kotlin.math.sin(phase * Math.PI).toFloat()).coerceIn(0f, 1f)
            drawCircle(
                color = accent.copy(alpha = a * idleAlpha),
                radius = 2.dp.toPx(),
                center = Offset(x * size.width, size.height / 2f)
            )
        }
    }
}

/**
 * A single floating emoji/image pop-up that rises and fades out above a
 * pressed key — the "Typing Animation" feature. Call [key] with a changing
 * value (e.g. a press counter) to retrigger the animation on each press;
 * this composable owns its own rise/fade Animatable and resets it via
 * LaunchedEffect(key).
 */
@Composable
fun TypingAnimationPopup(
    trigger: Int,
    animation: TypingAnimation,
    customEmoji: String,
    customImageBitmap: android.graphics.Bitmap?,
    anchorOffset: IntOffset,
    modifier: Modifier = Modifier
) {
    if (animation == TypingAnimation.NONE || trigger == 0) return

    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    // Pick one glyph from the preset's cycle each trigger, so repeated
    // presses don't always show the exact same emoji.
    val glyph = remember(trigger) {
        animation.emojiSet.takeIf { it.isNotEmpty() }?.let { it[trigger % it.size] } ?: ""
    }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(550, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        visible = false
    }

    if (!visible) return

    val riseOffsetY = (-36 * progress.value).dp
    val alpha = (1f - progress.value).coerceIn(0f, 1f)
    val scale = 0.7f + 0.5f * (1f - progress.value)

    // Popup renders into its own window, positioned by `alignment` +
    // `offset` (both in raw pixels for offset) rather than by any Modifier
    // from the caller's own layout tree — Popup has no `modifier`
    // parameter, since its content isn't a normal child of the composable
    // that calls it. BottomCenter matches where the caller wants this to
    // appear (just above the keyboard's key rows).
    Popup(alignment = Alignment.BottomCenter, offset = anchorOffset) {
        Box(
            modifier = modifier
                .graphicsLayer {
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
