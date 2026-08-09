package com.spmods.sinkey.keyboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.spmods.sinkey.data.BackgroundStyle
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws one of the built-in [BackgroundStyle] presets full-bleed behind the
 * keyboard. Every style here is drawn procedurally with Compose's own
 * gradient/canvas APIs — no bundled bitmap assets — so there's nothing to
 * license and no app-size cost.
 *
 * Layering (see KeyboardView): this sits *below* a "My themes" photo/GIF
 * background when one is set, and below the key rows themselves, exactly
 * like the existing KeyboardCustomBackground.
 */
@Composable
fun KeyboardBuiltInBackground(style: BackgroundStyle, isDark: Boolean, modifier: Modifier = Modifier) {
    if (style == BackgroundStyle.NONE) return
    when (style) {
        BackgroundStyle.GRADIENT -> GradientBackground(
            colors = if (isDark) listOf(Color(0xFF1B1033), Color(0xFF2C1250), Color(0xFF120826))
                     else listOf(Color(0xFFDCEBFF), Color(0xFFEAF2FF), Color(0xFFFDEFF9)),
            modifier = modifier
        )
        BackgroundStyle.RAINBOW -> RainbowBackground(modifier)
        BackgroundStyle.GALAXY -> GalaxyBackground(modifier)
        BackgroundStyle.SMOKE -> SmokeBackground(isDark, modifier)
        BackgroundStyle.SUNSET_SKY -> GradientBackground(
            colors = listOf(Color(0xFFFF7A45), Color(0xFFFF4E82), Color(0xFF6A3EA1)),
            modifier = modifier
        )
        BackgroundStyle.PASTEL_CUTE -> GradientBackground(
            colors = listOf(Color(0xFFFFE1F0), Color(0xFFE3D6FF), Color(0xFFD6ECFF)),
            modifier = modifier
        )
        BackgroundStyle.MINT_CUTE -> GradientBackground(
            colors = listOf(Color(0xFFDFFFF0), Color(0xFFE7FFE0), Color(0xFFFFFDE0)),
            modifier = modifier
        )
        BackgroundStyle.NONE -> Unit
    }
}

@Composable
private fun GradientBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
    }
}

/** Slowly-shifting rainbow diagonal gradient — a continuous hue sweep. */
@Composable
private fun RainbowBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "rainbowBg")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "rainbowShift"
    )
    val hues = remember(shift) {
        (0..5).map { i -> Color.hsv((shift + i * 60f) % 360f, 0.55f, 0.95f) }
    }
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = hues,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height * 0.4f)
            )
        )
    }
}

/** Deep space gradient with scattered static "stars" (twinkle handled by LedPattern.STARS separately). */
@Composable
private fun GalaxyBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "galaxyBg")
    val twinkle by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "galaxyTwinkle"
    )
    // Fixed star field so it doesn't re-randomize (and jump around) on every recomposition.
    val stars = remember {
        Random(42).let { rnd -> List(60) { Offset(rnd.nextFloat(), rnd.nextFloat()) to rnd.nextFloat() * 1.6f + 0.6f } }
    }
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF090318), Color(0xFF1C0B3E), Color(0xFF32104F)),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        stars.forEach { (rel, radius) ->
            drawCircle(
                color = Color.White.copy(alpha = twinkle * Random(rel.hashCode()).nextFloat().coerceIn(0.4f, 1f)),
                radius = radius.dp2px(this),
                center = Offset(rel.x * size.width, rel.y * size.height)
            )
        }
    }
}

private fun Float.dp2px(scope: DrawScope): Float = this * scope.density

/** Soft drifting translucent blobs, giving a "smoke" / fog look. */
@Composable
private fun SmokeBackground(isDark: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "smokeBg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "smokeDrift"
    )
    val base = if (isDark) Color(0xFF141414) else Color(0xFFE9E9EC)
    val smoke = if (isDark) Color(0xFF3A3A42) else Color(0xFFC9C9D6)
    Canvas(modifier = modifier) {
        drawRect(color = base)
        val cx1 = size.width * (0.2f + 0.15f * sin(drift * Math.PI).toFloat())
        val cx2 = size.width * (0.75f - 0.15f * sin(drift * Math.PI).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(smoke.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx1, size.height * 0.3f),
                radius = size.width * 0.55f
            ),
            radius = size.width * 0.55f,
            center = Offset(cx1, size.height * 0.3f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(smoke.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(cx2, size.height * 0.8f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(cx2, size.height * 0.8f)
        )
    }
}
