package com.spmods.sinkey.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.spmods.sinkey.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.detectTapGestures

// Number labels for top row keys (QWERTYUIOP → 1–9, 0)
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Dark green color matching Desh Keyboard style
private val DeshGreen = Color(0xFF2D6A4F)
private val KeyboardBg = Color(0xFFDDE1E7)

@Composable
fun KeyboardView(
    currentLanguage: String, // "en" or "si"
    onKey: (String) -> Unit
) {
    var shift by remember { mutableStateOf(false) }
    var showLangTooltip by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Collect real recent emojis from DataStore
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())

    // Auto-hide tooltip after 1.5 seconds
    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    // ── Emoji picker replaces keyboard when open ────────────────────────────
    if (showEmojiPicker) {
        EmojiPickerView(
            recentEmojis = recentEmojis,
            onEmojiSelected = { emoji -> onKey(emoji) },
            onBackspace = { onKey("BACKSPACE") },
            onDismiss = { showEmojiPicker = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KeyboardBg)
        ) {
            // ── Toolbar row (icons at top) ──────────────────────────────
            ToolbarRow(onKey = onKey)

            // ── Recent emoji row ────────────────────────────────────────
            EmojiRow(
                emojis = recentEmojis,
                onKey = onKey,
                onMoreClick = { showEmojiPicker = true }
            )

            // ── Letter rows ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Row 1: Q-P with number superscripts
                NumberedKeyRow(EnglishRows[0], topRowNumbers, shift) { onKey(it) }

                // Row 2: A-L
                KeyRow(EnglishRows[1], shift) { onKey(it) }

                // Row 3: Shift + Z-M + Backspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShiftKey(weight = 1.4f, active = shift) {
                        shift = !shift
                        onKey("SHIFT")
                    }
                    EnglishRows[2].forEach { k ->
                        val display = if (shift) k.uppercase() else k
                        LetterKey(label = display, weight = 1f) { onKey(display) }
                    }
                    BackspaceKey(weight = 1.4f) { onKey("BACKSPACE") }
                }

                // Row 4: ?123 | emoji | lang toggle | space | period | enter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKey(label = "?123", weight = 1.8f) { /* TODO: number layout */ }
                    EmojiKey(
                        weight = 1.3f,
                        onTap = { onKey(",") },
                        onLongPress = { showEmojiPicker = true }
                    )

                    // Language toggle — with tooltip anchor
                    Box(modifier = Modifier.weight(1.3f)) {
                        LangToggleKey(
                            currentLanguage = currentLanguage,
                            onTap = {
                                onKey("LANG_TOGGLE")
                                showLangTooltip = true
                            }
                        )
                    }

                    // Space — center, largest weight
                    SpaceKey(
                        weight = 4.5f,
                        onTap = { onKey("SPACE") },
                        onLongPress = { onKey("SWITCH_KEYBOARD") }
                    )

                    SpecialKey(label = ".", weight = 0.8f) { onKey(".") }
                    EnterKey(weight = 2.0f) { onKey("ENTER") }
                }
            }
        }

        // Tooltip overlay — floats above the keyboard
        AnimatedVisibility(
            visible = showLangTooltip,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 96.dp, bottom = 62.dp)
                .zIndex(10f)
        ) {
            LangTooltip(currentLanguage = currentLanguage)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar row  (apps, sticker, clipboard, font-A, translate, settings, mic)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ToolbarRow(onKey: (String) -> Unit) {
    // Icon labels (unicode symbols that look similar to the screenshot icons)
    val tools = listOf(
        "⊞" to "TOOL_APPS",
        "☺" to "TOOL_STICKER",
        "📋" to "TOOL_CLIPBOARD",
        "A" to "TOOL_FONT",
        "🇦" to "TOOL_TRANSLATE",
        "⚙" to "TOOL_SETTINGS"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyboardBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onKey(action) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, fontSize = 18.sp, color = Color(0xFF555555))
            }
        }
        // Mic button on the far right (filled circle)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable { onKey("TOOL_MIC") },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎤", fontSize = 16.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent emoji row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmojiRow(emojis: List<String>, onKey: (String) -> Unit, onMoreClick: () -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyboardBg)
            .padding(vertical = 2.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        emojis.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onKey(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
        // "..." more button → opens full emoji picker
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onMoreClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "•••", fontSize = 14.sp, color = Color(0xFF888888))
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LangTooltip(currentLanguage: String) {
    val label = if (currentLanguage == "en") "English enabled" else "සිංහල enabled"
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(DeshGreen)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓ ", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = if (currentLanguage == "en") {
                    buildAnnotatedStringBold("English", " enabled")
                } else {
                    buildAnnotatedStringBold("සිංහල", " enabled")
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
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        keys.forEachIndexed { index, k ->
            val display = if (shift) k.uppercase() else k
            val num = numbers.getOrNull(index) ?: ""
            NumberedLetterKey(
                label = display,
                number = num,
                weight = 1f,
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
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(label = display, weight = 1f) { onKey(display) }
        }
        Box(modifier = Modifier.weight(0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual keys
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.NumberedLetterKey(
    label: String,
    number: String,
    weight: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { onLongPress() }
            )
    ) {
        Text(
            text = number,
            fontSize = 9.sp,
            color = Color(0xFF888888),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp)
        )
        Text(
            text = label,
            fontSize = 18.sp,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun RowScope.LetterKey(label: String, weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 18.sp, color = Color(0xFF1A1A1A))
    }
}

@Composable
private fun RowScope.ShiftKey(weight: Float, active: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color(0xFFB0BEC5) else Color(0xFFBCC4CC))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active) "▲" else "△",
            fontSize = 18.sp,
            color = Color(0xFF333333)
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFBCC4CC))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        val longPressDelay = 400L
                        val repeatInterval = 50L

                        // withTimeoutOrNull returns null on timeout (= long press),
                        // or true if tryAwaitRelease returned before timeout (= tap).
                        val released = withTimeoutOrNull(longPressDelay) { tryAwaitRelease() }

                        if (released != null) {
                            // Finger lifted within threshold → simple tap
                            onTap()
                        } else {
                            // Finger still held → start repeating until released
                            onTap()
                            try {
                                while (true) {
                                    delay(repeatInterval)
                                    onTap()
                                    // Check if finger was released (1ms window)
                                    val done = withTimeoutOrNull(1L) { tryAwaitRelease() }
                                    if (done != null) break
                                }
                            } catch (_: Exception) {
                                // Coroutine cancelled (finger released / gesture ended)
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "⌫", fontSize = 18.sp, color = Color(0xFF333333))
    }
}

@Composable
private fun RowScope.SpecialKey(label: String, weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFBCC4CC))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
    }
}

@Composable
private fun LangToggleKey(currentLanguage: String, onTap: () -> Unit) {
    val isSinhala = currentLanguage == "si"
    Box(
        modifier = Modifier
            .height(46.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFBCC4CC))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "අ",
                fontSize = 13.sp,   // smaller — matches screenshot
                color = if (isSinhala) DeshGreen else Color(0xFF333333),
                fontWeight = if (isSinhala) FontWeight.Bold else FontWeight.Normal
            )
            // Green underline indicator when Sinhala active
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(2.dp)
                    .width(18.dp)
                    .background(
                        color = if (isSinhala) DeshGreen else Color.Transparent,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.EmojiKey(weight: Float, onTap: () -> Unit, onLongPress: () -> Unit) {
    // Screenshot shows: emoji face icon on top, comma below — single key
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFBCC4CC))
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { onLongPress() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "☺",
                fontSize = 16.sp,
                color = Color(0xFF444444),
                lineHeight = 18.sp
            )
            Text(
                text = ",",
                fontSize = 10.sp,
                color = Color(0xFF444444),
                lineHeight = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SpaceKey(weight: Float, onTap: () -> Unit, onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { onLongPress() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Desh Keyboard",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.EnterKey(weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(DeshGreen)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        // Screenshot: left-pointing return arrow ←
        Text(text = "←", fontSize = 22.sp, color = Color.White)
    }
}
