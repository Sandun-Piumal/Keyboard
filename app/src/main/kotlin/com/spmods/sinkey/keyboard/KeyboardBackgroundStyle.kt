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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
        BackgroundStyle.CYBER_BLOOM -> CyberBloomBackground(modifier)
        BackgroundStyle.SINKEY_LAVENDER -> SinKeyLavenderBackground(modifier)
        BackgroundStyle.SINKEY_NEON -> SinKeyNeonBackground(modifier)
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


/**
 * SinKey's own keyboard backdrop: a near-black violet gradient with soft
 * glowing "silk" swooshes — drawn the same way for light and dark themes
 * (the neon key chrome in KeyboardView assumes a dark surface).
 */
@Composable
private fun SinKeyNeonBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF07021A), Color(0xFF12053A), Color(0xFF0A0224), Color(0xFF05010F))
            )
        )
        // corner glows
        drawCircle(
            Brush.radialGradient(listOf(Color(0x555B2BEF), Color.Transparent), center = Offset(0f, h * 0.15f), radius = w * 0.45f),
            radius = w * 0.45f, center = Offset(0f, h * 0.15f)
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x447C3AED), Color.Transparent), center = Offset(w, h * 0.9f), radius = w * 0.5f),
            radius = w * 0.5f, center = Offset(w, h * 0.9f)
        )
        // silk swooshes (wide faint stroke + thin bright core)
        fun swoosh(y0: Float, y1: Float, y2: Float, y3: Float, alpha: Float) {
            val path = Path().apply {
                moveTo(-w * 0.05f, h * y0)
                cubicTo(w * 0.30f, h * y1, w * 0.60f, h * y2, w * 1.05f, h * y3)
            }
            drawPath(path, Color(0xFF7C3AED).copy(alpha = alpha * 0.35f), style = Stroke(width = h * 0.045f, cap = StrokeCap.Round))
            drawPath(path, Color(0xFFB79CFF).copy(alpha = alpha), style = Stroke(width = h * 0.006f, cap = StrokeCap.Round))
        }
        swoosh(0.10f, 0.35f, -0.05f, 0.30f, 0.55f)
        swoosh(0.55f, 0.30f, 0.85f, 0.55f, 0.45f)
        swoosh(0.95f, 0.70f, 1.00f, 0.75f, 0.40f)
    }
}


/**
 * Light lavender backdrop with diagonal violet stripes in the top-right and
 * bottom-left corners and a few sparkles — SinKey Lavender keyboard theme.
 */
@Composable
private fun SinKeyLavenderBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFEFE9FF), Color(0xFFDCD0FA), Color(0xFFE6DDFC))))
        fun stripe(x0: Float, x1: Float, x2: Float, x3: Float, y0: Float, y1: Float, a: Float) {
            val p = Path().apply {
                moveTo(w * x0, h * y0); lineTo(w * x1, h * y0); lineTo(w * x3, h * y1); lineTo(w * x2, h * y1); close()
            }
            drawPath(p, Brush.linearGradient(listOf(Color(0xFF7C3AED).copy(alpha = a), Color(0xFF5B2BEF).copy(alpha = a * 0.3f))))
        }
        // top-right corner
        stripe(0.80f, 0.86f, 0.90f, 0.96f, 0f, 0.25f, 0.55f)
        stripe(0.88f, 0.96f, 0.98f, 1.06f, 0f, 0.30f, 0.45f)
        stripe(0.68f, 0.72f, 0.76f, 0.80f, 0f, 0.14f, 0.30f)
        // bottom-left corner
        stripe(-0.06f, 0.02f, -0.18f, -0.10f, 0.72f, 1.0f, 0.0f)
        stripe(0.04f, 0.10f, -0.08f, -0.02f, 0.78f, 1.0f, 0.45f)
        stripe(0.14f, 0.19f, 0.02f, 0.07f, 0.88f, 1.0f, 0.35f)
        // sparkles
        fun sparkle(cx: Float, cy: Float, r: Float) {
            val p = Path().apply {
                moveTo(cx, cy - r); quadraticBezierTo(cx, cy, cx + r, cy)
                quadraticBezierTo(cx, cy, cx, cy + r); quadraticBezierTo(cx, cy, cx - r, cy)
                quadraticBezierTo(cx, cy, cx, cy - r); close()
            }
            drawPath(p, Color(0xFF7C3AED).copy(alpha = 0.7f))
        }
        sparkle(w * 0.95f, h * 0.55f, h * 0.03f)
        sparkle(w * 0.05f, h * 0.45f, h * 0.02f)
        sparkle(w * 0.30f, h * 0.96f, h * 0.025f)
    }
}


/**
 * Cyber Bloom: blurred neon flowers + circuit-board lines (bundled photo,
 * res/drawable-nodpi/bg_cyber_bloom.jpg) cropped to fill the keyboard.
 */
@Composable
private fun CyberBloomBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.spmods.sinkey.R.drawable.bg_cyber_bloom),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}
