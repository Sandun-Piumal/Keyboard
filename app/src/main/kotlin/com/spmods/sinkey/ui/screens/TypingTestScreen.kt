package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spmods.sinkey.R
import kotlinx.coroutines.delay

/** Which passage language the person is being tested on. */
enum class TypingTestMode { ENGLISH, SINHALA }

private val EnglishPassage =
    "The quick brown fox jumps over the lazy dog. Practice makes progress. Keep going and improve your typing skills."
private val SinhalaPassage =
    "අද දවස ලස්සනයි. පුහුණුව මගින් දක්ෂතාවය වර්ධනය වේ. දිගටම උත්සාහ කර ඔබේ ටයිප් කිරීමේ හැකියාව දියුණු කරගන්න."

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
    var mode by remember { mutableStateOf(TypingTestMode.ENGLISH) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val passage = if (mode == TypingTestMode.ENGLISH) EnglishPassage else SinhalaPassage

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
        }
    }

    val cardBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val statBarBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val passageBg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val titleColor = if (isDark) Color(0xFFF2EEFB) else Color(0xFF1A1A2E)
    val subColor = if (isDark) Color(0xFFB6AEC9) else Color(0xFF6B7280)
    val indigo = Color(0xFF6C4CE0)
    val indigoMid = Color(0xFF7C5CF0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp, 20.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2145) else Color(0xFFEDE7FB))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = indigo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text("SinKey ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3B2F8C))
                    Text("Board", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE0498A))
                }
                Text(
                    "Type Smart. Type Easy. Type SinKey.",
                    fontSize = 11.sp,
                    color = subColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2145) else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = indigo,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Title + hero illustration ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 12.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF3A2D5C) else Color(0xFFEDE7FB))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Typing Test",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFB39DEF) else indigo
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (isFinished) "Nice work!" else "Take a deep breath\nand start typing!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isFinished)
                        "WPM: $finalWpm  •  Accuracy: $finalAccuracy%"
                    else
                        "Improve your speed and accuracy with a quick test.",
                    fontSize = 13.sp,
                    color = subColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.test_typing_stopwatch),
                    contentDescription = "Typing test illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(110.dp)
                        .height(110.dp)
                )
            }
        }

        // ── Stats strip: Time / Goal Speed / Mode ────────────────────────
        Card(
            modifier = Modifier
                .padding(20.dp, 16.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = statBarBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Filled.Schedule,
                    label = "Time",
                    value = formatTime(secondsLeft),
                    tint = indigo,
                    valueColor = titleColor,
                    labelColor = subColor,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    icon = Icons.Filled.Speed,
                    label = "Goal Speed",
                    value = "40 WPM",
                    tint = indigo,
                    valueColor = titleColor,
                    labelColor = subColor,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                Box(modifier = Modifier.weight(1f)) {
                    StatItem(
                        icon = Icons.Filled.GpsFixed,
                        label = "Mode",
                        value = if (mode == TypingTestMode.ENGLISH) "English" else "Sinhala",
                        tint = indigo,
                        valueColor = titleColor,
                        labelColor = subColor,
                        trailingIcon = Icons.Filled.ExpandMore,
                        onClick = { modeMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = modeMenuExpanded,
                        onDismissRequest = { modeMenuExpanded = false }
                    ) {
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
        }

        // ── Passage card ──────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .padding(20.dp, 16.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isDark) Color(0xFF3A2D5C) else Color(0xFFEDE7FB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = indigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Type the following text",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Passage with live highlighting: typed-correct in indigo bg,
                // typed-incorrect in red-ish, remaining plain.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(passageBg)
                        .padding(16.dp)
                ) {
                    Text(
                        text = buildHighlightedPassage(passage, input.text, titleColor),
                        fontSize = 17.sp,
                        lineHeight = 26.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Hidden-ish text field — visually the passage above is what
                // the person reads, but typing happens here so we can track
                // exact keystrokes against the target passage.
                BasicTextField(
                    value = input,
                    onValueChange = { new ->
                        if (isFinished) return@BasicTextField
                        if (!isRunning && new.text.isNotEmpty()) isRunning = true
                        if (new.text.length <= passage.length) input = new
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        color = titleColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF3A2D5C) else Color.White)
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (input.text.isEmpty()) {
                            Text(
                                "Start typing here…",
                                fontSize = 15.sp,
                                color = subColor
                            )
                        }
                        inner()
                    }
                )
            }
        }

        // ── Progress ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${input.text.length} / ${passage.length}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = indigo
                )
                Spacer(Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = {
                        if (passage.isNotEmpty())
                            input.text.length.toFloat() / passage.length.toFloat()
                        else 0f
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = indigo,
                    trackColor = if (isDark) Color(0xFF3A2D5C) else Color(0xFFE0D8F5)
                )
            }
        }

        // ── Bottom controls: Settings / Start Test / Reset ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 24.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconLabel(
                icon = Icons.Filled.Settings,
                label = "Settings",
                tint = subColor,
                bg = if (isDark) Color(0xFF2C2145) else Color(0xFFEDE7FB),
                onClick = onSettingsClick
            )

            Spacer(Modifier.width(14.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(indigo, indigoMid)
                        )
                    )
                    .clickable {
                        if (isFinished) {
                            resetTest()
                        } else {
                            isRunning = true
                        }
                    }
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isFinished) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isFinished) "Try Again" else if (isRunning) "Typing…" else "Start Test",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            RoundIconLabel(
                icon = Icons.Filled.Refresh,
                label = "Reset",
                tint = subColor,
                bg = if (isDark) Color(0xFF2C2145) else Color(0xFFEDE7FB),
                onClick = { resetTest() }
            )
        }

        if (isFinished) {
            Row(
                modifier = Modifier
                    .padding(20.dp, 18.dp, 20.dp, 0.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1E3A2A) else Color(0xFFE3F5EA))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF1E8A4C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Test complete",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E8A4C)
                    )
                    Text(
                        "$finalWpm WPM at $finalAccuracy% accuracy. Tap Try Again for another round.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFB6AEC9) else Color(0xFF3F6B4F)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = labelColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
                if (trailingIcon != null) {
                    Icon(
                        trailingIcon,
                        contentDescription = null,
                        tint = labelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun RoundIconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = tint)
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
    plainColor: Color
) = buildAnnotatedString {
    val correctBg = Color(0xFFD6C9F7)
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
