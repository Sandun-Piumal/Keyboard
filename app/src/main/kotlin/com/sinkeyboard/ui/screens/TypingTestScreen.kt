package com.sinkeyboard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.sinkeyboard.R
import com.sinkeyboard.data.TypingStatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which passage language the person is being tested on. */
enum class TypingTestMode { ENGLISH, SINHALA, MIX }

private val EnglishPassage =
    "The quick brown fox jumps over the lazy dog. Practice makes progress. Keep going and improve your typing skills."
private val SinhalaPassage =
    "අද දවස ලස්සනයි. පුහුණුව මගින් දක්ෂතාවය වර්ධනය වේ. දිගටම උත්සාහ කර ඔබේ ටයිප් කිරීමේ හැකියාව දියුණු කරගන්න."
private val MixPassage =
    "අද මම office එකට යනවා. Practice makes progress කියන එක හරි talk එකක්. ඔයාගේ typing skill එක දියුණු කරගන්න දිගටම try කරන්න."

private const val TEST_DURATION_SECONDS = 60

/**
 * Full typing-speed test screen: shows a passage, tracks what the user has
 * typed against it in real time, runs a 60s countdown once typing starts,
 * and reports WPM + accuracy at the end. Mirrors the reference design:
 * header with stopwatch illustration, a stats strip (Time / Goal Speed /
 * Mode), the passage card with live highlighting, a progress bar, and
 * Settings / Start-Test / Reset controls along the bottom.
 */
@Composable
fun TypingTestScreen(
    onBack: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme(),
    onSettingsClick: () -> Unit = {}
) {
    var mode by remember { mutableStateOf(TypingTestMode.MIX) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val passage = when (mode) {
        TypingTestMode.ENGLISH -> EnglishPassage
        TypingTestMode.SINHALA -> SinhalaPassage
        TypingTestMode.MIX -> MixPassage
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val statsRepo = remember(context) { TypingStatsRepository(context) }

    // Typing state
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(TEST_DURATION_SECONDS) }

    // Results, computed once the test ends (time runs out or passage completed)
    var finalWpm by remember { mutableStateOf(0) }
    var finalAccuracy by remember { mutableStateOf(100) }

    fun resetTest() {
        input = TextFieldValue("")
        isRunning = false
        isFinished = false
        secondsLeft = TEST_DURATION_SECONDS
        finalWpm = 0
        finalAccuracy = 100
    }

    // Reset whenever the mode (English/Sinhala) changes
    LaunchedEffect(mode) { resetTest() }

    // Countdown timer — only ticks while running and not finished
    LaunchedEffect(isRunning, isFinished) {
        while (isRunning && !isFinished && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                isFinished = true
                isRunning = false
            }
        }
    }

    fun computeResults() {
        val typed = input.text
        val correctChars = typed.zip(passage).count { (a, b) -> a == b }
        val elapsedSeconds = TEST_DURATION_SECONDS - secondsLeft
        val minutes = (elapsedSeconds.coerceAtLeast(1)) / 60.0
        val words = typed.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        finalWpm = if (minutes > 0) Math.round(words / minutes).toInt() else 0
        finalAccuracy = if (typed.isNotEmpty())
            Math.round((correctChars.toDouble() / typed.length) * 100).toInt()
        else 100
    }

    // Detect completion of the passage
    LaunchedEffect(input.text) {
        if (isRunning && input.text.length >= passage.length) {
            isFinished = true
            isRunning = false
        }
    }

    // Compute results the moment the test finishes, and hide the keyboard
    LaunchedEffect(isFinished) {
        if (isFinished) {
            computeResults()
            focusManager.clearFocus()
            keyboardController?.hide()
            // Save this real result (not a placeholder) so the Profile
            // screen's Best WPM / Last result / streak reflect actual
            // completed tests. Fired once per finish, same as the
            // computation above.
            coroutineScope.launch { statsRepo.recordTestResult(finalWpm, finalAccuracy) }
        }
    }

    val p = remember(isDark) { homePalette(isDark) }
    val titleColor = p.title
    val subColor = p.body
    val indigo = Color(0xFF5B2BEF)
    val indigoDeep = Color(0xFF34128F)
    val innerBg = if (isDark) Color(0xFF2A2352) else Color(0xFFE6DCFB)

    // Transparent page — MainActivity paints the About-Developer background.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp, 16.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, ambientColor = indigo, spotColor = indigo)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), indigoDeep)))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text("SinKey ", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
                    Text(
                        "Board",
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0xFF9B4DFF))),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Text(
                    "Type Smart. Type Easy. Type SinKey.",
                    fontSize = 11.sp,
                    color = p.sub,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        if (isDark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                        else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                    )
                    .border(1.5.dp, if (isDark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = p.iconIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Title + hero illustration ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 14.dp, 12.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val chipShape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .shadow(8.dp, chipShape, ambientColor = indigo, spotColor = indigo)
                        .clip(chipShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF4A1FB8), Color(0xFF6A34F0))))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = Color(0xFFFFC928), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Typing Test", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (isFinished) buildAnnotatedString { append("Nice work!") }
                    else buildAnnotatedString {
                        append("Take a deep breath\nand start ")
                        withStyle(SpanStyle(color = if (isDark) Color(0xFFB39DEF) else indigo)) { append("typing") }
                        append("!")
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    lineHeight = 32.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (isFinished)
                        "WPM: $finalWpm  •  Accuracy: $finalAccuracy%"
                    else
                        "Improve your speed and accuracy with a quick test.",
                    fontSize = 13.sp,
                    color = subColor
                )
                // small purple swoosh under the subtitle
                Canvas(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 96.dp, height = 12.dp)
                ) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.9f)
                        quadraticBezierTo(size.width * 0.5f, size.height * 0.1f, size.width, size.height * 0.2f)
                    }
                    drawPath(path, indigo, style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }

            Spacer(Modifier.width(6.dp))

            Image(
                painter = painterResource(id = R.drawable.test_typing_stopwatch),
                contentDescription = "Typing test illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(130.dp)
                    .height(130.dp)
            )
        }

        // ── Stats strip: Time / Goal Speed / Mode ────────────────────────
        Row(
            modifier = Modifier
                .padding(16.dp, 18.dp, 16.dp, 0.dp)
                .fillMaxWidth()
                .glassCard(p, radius = 28, elevation = 8)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                p = p,
                icon = Icons.Filled.Schedule,
                label = "Time",
                value = formatTime(secondsLeft),
                tint = p.iconIndigo,
                modifier = Modifier.weight(1f)
            )
            StatDivider(p)
            StatItem(
                p = p,
                icon = Icons.Filled.Speed,
                label = "Goal Speed",
                value = "40 WPM",
                tint = p.iconIndigo,
                modifier = Modifier.weight(1f)
            )
            StatDivider(p)
            Box(modifier = Modifier.weight(1f)) {
                StatItem(
                    p = p,
                    icon = Icons.Filled.Settings,
                    label = "Mode",
                    value = when (mode) {
                        TypingTestMode.ENGLISH -> "English"
                        TypingTestMode.SINHALA -> "Sinhala"
                        TypingTestMode.MIX -> "Mix"
                    },
                    tint = p.iconIndigo,
                    trailingIcon = Icons.Filled.ExpandMore,
                    onClick = { modeMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mix") },
                        onClick = {
                            mode = TypingTestMode.MIX
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("English") },
                        onClick = {
                            mode = TypingTestMode.ENGLISH
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sinhala") },
                        onClick = {
                            mode = TypingTestMode.SINHALA
                            modeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // ── Passage card ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(16.dp, 16.dp, 16.dp, 0.dp)
                .fillMaxWidth()
                .glassCard(p, radius = 28, elevation = 8)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), indigoDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Type the following text",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor
                )
            }

            Spacer(Modifier.height(12.dp))

            // Passage with live highlighting: typed-correct in indigo bg,
            // typed-incorrect in red-ish, remaining plain.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(innerBg)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF4A3F85) else Color(0xFFCDBDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u201C", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = indigo, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buildHighlightedPassage(passage, input.text, titleColor, isDark),
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Hidden-ish text field — visually the passage above is what
            // the person reads, but typing happens here so we can track
            // exact keystrokes against the target passage.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = p.iconIndigo, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.width(2.dp).height(30.dp).background(indigo))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { new ->
                        if (isFinished) return@BasicTextField
                        // No length-based truncation/rejection here anymore.
                        // Sinhala/mix mode composes text via the IME's
                        // composing span, which can briefly grow longer than
                        // passage.length mid-word before settling (or exceed
                        // it right as SPACE finalizes a word) even though the
                        // person hasn't actually reached the end yet. Cutting
                        // the string down to passage.length in that moment
                        // sliced off characters the person had just typed,
                        // which is exactly the "letters go missing on space"
                        // bug. The passage-complete LaunchedEffect below
                        // already ends the test once input.text is at least
                        // as long as the passage, so there's nothing left for
                        // a hard cap here to protect.
                        input = new
                        if (!isRunning && new.text.isNotEmpty()) isRunning = true
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = titleColor
                    ),
                    cursorBrush = SolidColor(indigo),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (input.text.isEmpty()) {
                                Text(
                                    "Start typing here…",
                                    fontSize = 16.sp,
                                    color = subColor
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }

        // ── Progress ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${input.text.length} / ${passage.length}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.iconIndigo
            )
            Spacer(Modifier.width(12.dp))
            val frac = if (passage.isNotEmpty())
                (input.text.length.toFloat() / passage.length.toFloat()).coerceIn(0f, 1f)
            else 0f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF3A2F66) else Color(0xFFD9CCF8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = frac)
                        .height(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF4A1FB8), Color(0xFF7C4DFF))))
                )
            }
        }

        // ── Bottom controls: Settings / Start Test / Reset ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 22.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.Top
        ) {
            RoundIconLabel(
                p = p,
                icon = Icons.Filled.Settings,
                label = "Settings",
                onClick = onSettingsClick
            )

            Spacer(Modifier.width(12.dp))

            val startShape = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
                    .shadow(14.dp, startShape, ambientColor = indigo, spotColor = indigo)
                    .clip(startShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), Color(0xFF3A1AA8))))
                    .border(2.dp, Color.White.copy(alpha = 0.45f), startShape)
                    .clickable {
                        if (isFinished) {
                            resetTest()
                        } else {
                            isRunning = true
                        }
                    }
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isFinished) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isFinished) "Try Again" else if (isRunning) "Typing…" else "Start Test",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(12.dp))

            RoundIconLabel(
                p = p,
                icon = Icons.Filled.Refresh,
                label = "Reset",
                onClick = { resetTest() }
            )
        }

        if (isFinished) {
            Row(
                modifier = Modifier
                    .padding(16.dp, 20.dp, 16.dp, 0.dp)
                    .fillMaxWidth()
                    .glassCard(p, radius = 22, elevation = 6)
                    .background(if (isDark) Color(0x331E8A4C) else Color(0x3322B14C))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22B14C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Test complete",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF7FE3A0) else Color(0xFF1E8A4C)
                    )
                    Text(
                        "$finalWpm WPM at $finalAccuracy% accuracy. Tap Try Again for another round.",
                        fontSize = 12.sp,
                        color = p.body
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    p: HomePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(4.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.3f), spotColor = p.glow.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(if (p.dark) Color(0xFF2E2560) else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 12.sp, color = p.body, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title, maxLines = 1, overflow = TextOverflow.Clip)
                if (trailingIcon != null) {
                    Icon(
                        trailingIcon,
                        contentDescription = null,
                        tint = p.iconIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatDivider(p: HomePalette) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(if (p.dark) Color(0xFF3A2F66) else Color(0xFFD3C5F5))
    )
}

@Composable
private fun RoundIconLabel(
    p: HomePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                    else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                )
                .border(1.5.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = p.iconIndigo, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = p.iconIndigo)
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

/**
 * Builds the passage as an AnnotatedString: characters already typed
 * correctly get a light indigo highlight, the current character gets a
 * cursor-like underline/bold marker, mistyped characters are shown in red,
 * and untyped characters stay plain — mirroring the "The|" highlight seen
 * in the reference screenshot.
 */
@Composable
private fun buildHighlightedPassage(
    passage: String,
    typed: String,
    plainColor: Color,
    isDark: Boolean = false
) = buildAnnotatedString {
    val correctBg = if (isDark) Color(0xFF5B49B0) else Color(0xFFCBB9F7)
    val wrongColor = Color(0xFFE0498A)
    val mutedColor = plainColor.copy(alpha = 0.45f)

    passage.forEachIndexed { index, char ->
        when {
            index < typed.length && typed[index] == char -> {
                withStyle(SpanStyle(background = correctBg, color = plainColor)) {
                    append(char)
                }
            }
            index < typed.length -> {
                withStyle(SpanStyle(background = wrongColor.copy(alpha = 0.25f), color = wrongColor)) {
                    append(char)
                }
            }
            index == typed.length -> {
                withStyle(SpanStyle(background = Color(0xFFB39DEF), color = Color.White)) {
                    append(char)
                }
            }
            else -> {
                withStyle(SpanStyle(color = mutedColor)) {
                    append(char)
                }
            }
        }
    }
}
