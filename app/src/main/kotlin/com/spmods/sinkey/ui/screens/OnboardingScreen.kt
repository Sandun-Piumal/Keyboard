package com.spmods.sinkey.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.database.ContentObserver
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spmods.sinkey.R
import kotlinx.coroutines.launch

/**
 * Same checks HomeScreen's setup card uses (see isImeEnabled/isImeDefault
 * there) — duplicated locally rather than shared, since neither is more
 * than a couple of lines and pulling them into a shared file for two
 * call sites isn't worth the indirection.
 */
private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java)
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeDefault(context: Context): Boolean {
    val defaultIme = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return defaultIme?.startsWith(context.packageName) == true
}

// ── Onboarding palette ──────────────────────────────────────────────
// Fixed light-purple palette so all 5 pages look identical regardless of
// the app's light/dark theme (the artwork is designed for a light background).
private val ObBgTop = Color(0xFFFBF8FF)
private val ObBgBottom = Color(0xFFEFE6FF)
private val ObTitle = Color(0xFF1E0B6E)
private val ObBody = Color(0xFF2B1A7A)
private val ObAccent = Color(0xFF7B2FFF)
private val ObButtonStart = Color(0xFF5B2BEF)
private val ObButtonEnd = Color(0xFF8A1FFF)
private val ObDotIdle = Color(0xFFD3C2F7)
private val ObButtonBrush = Brush.horizontalGradient(listOf(ObButtonStart, ObButtonEnd))

/**
 * One tutorial slide. The title is split in three so the middle part can
 * be drawn in the accent colour — every page uses the same pattern.
 */
private data class OnboardingSlide(
    val illustration: Int,
    val titleStart: String,
    val titleHighlight: String,
    val titleEnd: String,
    val description: String
)

private val slides = listOf(
    OnboardingSlide(
        illustration = R.drawable.onb_sinhala,
        titleStart = "Type ",
        titleHighlight = "Sinhala",
        titleEnd = " naturally",
        description = "Type in English letters and SinKey converts it to Sinhala as you go – e.g. “kohomada” becomes “කොහොමද”. No separate Sinhala keyboard layout to learn."
    ),
    OnboardingSlide(
        illustration = R.drawable.onb_modes,
        titleStart = "Sinhala, ",
        titleHighlight = "English",
        titleEnd = ", or both",
        description = "Switch between pure Sinhala, pure English, and Mix mode (Sinhala + English together in the same sentence) any time from the language key."
    ),
    OnboardingSlide(
        illustration = R.drawable.onb_shortcuts,
        titleStart = "Quick text ",
        titleHighlight = "shortcuts",
        titleEnd = "",
        description = "Set up a shortcut like “gm” to expand into a full phrase like “Good morning” as you type. Find it under Settings → Quick text."
    ),
    OnboardingSlide(
        illustration = R.drawable.onb_dictionary,
        titleStart = "Your own ",
        titleHighlight = "dictionary",
        titleEnd = "",
        description = "SinKey learns the words you type so they're suggested again next time. Manage them any time under Settings → Personal dictionary."
    ),
    OnboardingSlide(
        illustration = R.drawable.onb_keyboard,
        titleStart = "One ",
        titleHighlight = "last",
        titleEnd = " step",
        description = "To start typing with SinKey, enable it and set it as your default keyboard."
    )
)

/**
 * First-launch onboarding tutorial (see MainActivity's hasSeenOnboarding
 * gate). All five pages share ONE template so nothing jumps around when
 * swiping: same background, Skip button, illustration box, title + swoosh,
 * 3-line description, dots, and a fixed-height bottom action area.
 *
 * Pages 1–4 show a "Next" button in the bottom area; page 5 shows the two
 * setup steps + "Finish" in that same area. The setup logic (isImeEnabled /
 * isImeDefault, ContentObserver + ON_RESUME refresh) is unchanged.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(isImeEnabled(context)) }
    var isDefault by remember { mutableStateOf(isImeDefault(context)) }

    DisposableEffect(lifecycleOwner) {
        fun refresh() {
            enabled = isImeEnabled(context)
            isDefault = isImeDefault(context)
        }

        val handler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            false, contentObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
            false, contentObserver
        )

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage == slides.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Skip — same place on every page, including the last.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFinish) {
                    Text("Skip", color = ObButtonStart, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ObButtonStart)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingSlideContent(slides[page])
            }

            PagerDots(
                pageCount = slides.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            )

            // Fixed-height action area → dots and content never shift between pages.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .padding(horizontal = 24.dp)
            ) {
                if (isLastSlide) {
                    SetupStepRow(
                        label = "Enable SinKey",
                        done = enabled,
                        onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SetupStepRow(
                        label = "Set as default keyboard",
                        done = isDefault,
                        onClick = {
                            val imm = context.getSystemService(InputMethodManager::class.java)
                            imm.showInputMethodPicker()
                        }
                    )
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (enabled && isDefault) "Finish" else "Finish setup later",
                            color = ObButtonStart,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    GradientPillButton(
                        text = "Next",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                }
            }

            // Leaves room for the decorative wave at the very bottom.
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/** Shared page template: illustration → title + swoosh → description. */
@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val illustrationHeight: Dp = (maxHeight * 0.50f).coerceIn(170.dp, 300.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(slide.illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(illustrationHeight)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = buildAnnotatedString {
                    append(slide.titleStart)
                    withStyle(SpanStyle(color = ObAccent)) { append(slide.titleHighlight) }
                    append(slide.titleEnd)
                },
                color = ObTitle,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            TitleSwoosh()
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = slide.description,
                color = ObBody,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                minLines = 3
            )
        }
    }
}

/** Curved underline drawn under every title. */
@Composable
private fun TitleSwoosh() {
    Canvas(modifier = Modifier.width(170.dp).height(12.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.85f)
            quadraticBezierTo(size.width * 0.5f, -size.height * 0.35f, size.width, size.height * 0.45f)
        }
        drawPath(
            path = path,
            color = ObAccent,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun GradientPillButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(12.dp, CircleShape, ambientColor = ObButtonStart, spotColor = ObButtonStart)
            .clip(CircleShape)
            .background(ObButtonBrush)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
    }
}

/**
 * One setup step on the last page. [done] → filled purple circle with a
 * check and no click; otherwise an empty outlined circle and tappable row.
 * [onClick] only launches the system flow — the ContentObserver refresh in
 * OnboardingScreen is what flips [done] after the user returns.
 */
@Composable
private fun SetupStepRow(label: String, done: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color.White, shape)
            .then(if (done) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(ObButtonBrush)
                    else Modifier.border(2.dp, ObAccent.copy(alpha = 0.45f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = ObTitle, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

/** Dot indicator — selected dot is a wider pill. Same position on every page. */
@Composable
private fun PagerDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateFloatAsState(if (isSelected) 28f else 10f, label = "dotWidth")
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .width(width.dp)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ObButtonStart else ObDotIdle)
            )
        }
    }
}

// ── Static decorative background (identical on every page) ─────────

@Composable
private fun OnboardingBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(ObBgTop, ObBgBottom)))

        // Soft blobs
        fun blob(cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB794FF).copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = maxOf(rx, ry)
                ),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2, ry * 2)
            )
        }
        blob(0f, h * 0.05f, w * 0.45f, h * 0.10f, 0.55f)
        blob(w, h * 0.17f, w * 0.40f, h * 0.09f, 0.45f)
        blob(0f, h * 0.72f, w * 0.40f, h * 0.10f, 0.45f)
        blob(w, h * 0.68f, w * 0.40f, h * 0.10f, 0.40f)

        // Sparkles
        val sparkleColor = Color(0xFF7B4DF5)
        sparkle(Offset(w * 0.24f, h * 0.12f), 14.dp.toPx(), sparkleColor)
        sparkle(Offset(w * 0.15f, h * 0.16f), 11.dp.toPx(), sparkleColor)
        sparkle(Offset(w * 0.80f, h * 0.15f), 11.dp.toPx(), sparkleColor)
        sparkle(Offset(w * 0.86f, h * 0.76f), 12.dp.toPx(), sparkleColor)

        // Bottom waves
        val waveH = 120.dp.toPx()
        val top = h - waveH
        val back = Path().apply {
            moveTo(0f, top + waveH * 0.25f)
            cubicTo(w * 0.25f, top - waveH * 0.05f, w * 0.55f, top + waveH * 0.55f, w, top + waveH * 0.10f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(back, Color(0xFFB79CFF).copy(alpha = 0.28f))
        val front = Path().apply {
            moveTo(0f, top + waveH * 0.55f)
            cubicTo(w * 0.30f, top + waveH * 0.25f, w * 0.65f, top + waveH * 0.85f, w, top + waveH * 0.40f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(
            front,
            Brush.horizontalGradient(listOf(Color(0xFF7B3FF2), Color(0xFFA57CFF)))
        )
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r)
        quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
