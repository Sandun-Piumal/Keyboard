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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp

// Number labels for top row keys (QWERTYUIOP → 1–9, 0)
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Dark green color matching Desh Keyboard style
private val DeshGreen = Color(0xFF2D6A4F)
private val KeyboardBgLight = Color(0xFFDDE1E7)
private val KeyboardBgDark = Color(0xFF1A1A1F)
private val KeyBgLight = Color.White
private val KeyBgDark = Color(0xFF2C2C34)
private val KeySpecialBgLight = Color(0xFFBCC4CC)
private val KeySpecialBgDark = Color(0xFF3A3A45)
private val KeyTextLight = Color(0xFF1A1A1A)
private val KeyTextDark = Color(0xFFE8E8EE)
private val KeySubTextLight = Color(0xFF555555)
private val KeySubTextDark = Color(0xFFAAAAAA)

/** Convert a 0..3 slider step to a concrete key-row height in dp. */
private fun stepToKeyHeight(step: Float): Dp = when (Math.round(step)) {
    0 -> 40.dp   // S
    1 -> 46.dp   // M (default)
    2 -> 54.dp   // L
    else -> 62.dp // XL
}

/** Convert a 0..3 slider step to bottom padding in dp. */
private fun stepToBottomPadding(step: Float): Dp = when (Math.round(step)) {
    0 -> 4.dp    // S
    1 -> 10.dp   // M
    2 -> 18.dp   // L
    else -> 28.dp // XL
}

@Composable
fun KeyboardView(
    currentLanguage: String, // "en" or "si"
    keyboardHeight: Float = 1f,          // 0=S 1=M 2=L 3=XL
    bottomSpaceEnabled: Boolean = true,
    bottomSpaceSize: Float = 0f,         // 0=S 1=M 2=L 3=XL
    showKeyBorders: Boolean = true,
    onKey: (String) -> Unit,
    onDismiss: (() -> Unit)? = null      // called when keyboard should close (tap outside in preview)
) {
    val isDark = isSystemInDarkTheme()
    val keyboardBg = if (isDark) KeyboardBgDark else KeyboardBgLight
    val keyBgBase = if (isDark) KeyBgDark else KeyBgLight
    val keySpecialBg = if (isDark) KeySpecialBgDark else KeySpecialBgLight
    val keyTextColor = if (isDark) KeyTextDark else KeyTextLight
    val keySubTextColor = if (isDark) KeySubTextDark else KeySubTextLight
    val spaceKeyBg = if (isDark) Color(0xFF38383F) else Color.White

    val keyHeight = stepToKeyHeight(keyboardHeight)
    val bottomPadding = if (bottomSpaceEnabled) stepToBottomPadding(bottomSpaceSize) else 4.dp
    val keyBg = if (showKeyBorders) keyBgBase else if (isDark) Color(0xFF252530) else Color(0xFFF0F2F5)
    val keyShape = if (showKeyBorders) RoundedCornerShape(6.dp) else RoundedCornerShape(4.dp)
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
                .background(keyboardBg)
        ) {
            // ── Toolbar row (icons at top) ──────────────────────────────
            ToolbarRow(onKey = onKey, keyboardBg = keyboardBg, iconColor = keySubTextColor)

            // ── Recent emoji row ────────────────────────────────────────
            EmojiRow(
                emojis = recentEmojis,
                onKey = onKey,
                onMoreClick = { showEmojiPicker = true },
                keyboardBg = keyboardBg
            )

            // ── Letter rows ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .padding(bottom = bottomPadding)
            ) {
                // Row 1: Q-P with number superscripts
                NumberedKeyRow(EnglishRows[0], topRowNumbers, shift, keyHeight, keyBg, keyShape, keyTextColor) { onKey(it) }

                // Row 2: A-L
                KeyRow(EnglishRows[1], shift, keyHeight, keyBg, keyShape, keyTextColor) { onKey(it) }

                // Row 3: Shift + Z-M + Backspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShiftKey(weight = 1.4f, active = shift, keyHeight = keyHeight, keyShape = keyShape, keySpecialBg = keySpecialBg, keyTextColor = keyTextColor) {
                        shift = !shift
                        onKey("SHIFT")
                    }
                    EnglishRows[2].forEach { k ->
                        val display = if (shift) k.uppercase() else k
                        LetterKey(label = display, weight = 1f, keyHeight = keyHeight, keyBg = keyBg, keyShape = keyShape, keyTextColor = keyTextColor) { onKey(display) }
                    }
                    BackspaceKey(weight = 1.4f, keyHeight = keyHeight, keyShape = keyShape, keySpecialBg = keySpecialBg, keyTextColor = keyTextColor) { onKey("BACKSPACE") }
                }

                // Row 4: ?123 | emoji | lang toggle | space | period | enter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKey(label = "?123", weight = 1.8f, keyHeight = keyHeight, keyShape = keyShape, keySpecialBg = keySpecialBg, keyTextColor = keyTextColor) { /* TODO: number layout */ }
                    EmojiKey(
                        weight = 1.3f,
                        keyHeight = keyHeight,
                        keyShape = keyShape,
                        keySpecialBg = keySpecialBg,
                        keyTextColor = keyTextColor,
                        onTap = { onKey(",") },
                        onLongPress = { showEmojiPicker = true }
                    )

                    // Language toggle — with tooltip anchor
                    Box(modifier = Modifier.weight(1.3f)) {
                        LangToggleKey(
                            currentLanguage = currentLanguage,
                            keyHeight = keyHeight,
                            keyShape = keyShape,
                            keySpecialBg = keySpecialBg,
                            keyTextColor = keyTextColor,
                            onTap = {
                                onKey("LANG_TOGGLE")
                                showLangTooltip = true
                            }
                        )
                    }

                    // Space — center, largest weight
                    SpaceKey(
                        weight = 4.5f,
                        keyHeight = keyHeight,
                        keyShape = keyShape,
                        spaceKeyBg = spaceKeyBg,
                        keyTextColor = keyTextColor,
                        onTap = { onKey("SPACE") },
                        onLongPress = { onKey("SWITCH_KEYBOARD") }
                    )

                    SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, keyShape = keyShape, keySpecialBg = keySpecialBg, keyTextColor = keyTextColor) { onKey(".") }
                    EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
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
private fun ToolbarRow(onKey: (String) -> Unit, keyboardBg: Color, iconColor: Color) {
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
            .background(keyboardBg)
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
                Text(text = label, fontSize = 18.sp, color = iconColor)
            }
        }
        // Mic button on the far right (filled circle)
        val micBg = if (iconColor == KeySubTextDark) Color(0xFF38383F) else Color.White
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .background(micBg)
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
private fun EmojiRow(emojis: List<String>, onKey: (String) -> Unit, onMoreClick: () -> Unit, keyboardBg: Color) {
    // LazyRow is the CORRECT way to get horizontal scrolling in Compose.
    // Regular Row + horizontalScroll + fillMaxWidth conflict with each other.
    // LazyRow handles clipping and scrolling natively without those issues.
    val items = emojis + listOf("MORE_BTN")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(keyboardBg)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        state = rememberLazyListState()
    ) {
        item { Spacer(modifier = Modifier.width(4.dp)) }
        items(emojis.size) { index ->
            val emoji = emojis[index]
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
        // "•••" more button → opens full emoji picker
        item {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "•••", fontSize = 14.sp, color = Color(0xFF888888))
            }
        }
        item { Spacer(modifier = Modifier.width(4.dp)) }
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
    keyHeight: Dp,
    keyBg: Color,
    keyShape: RoundedCornerShape,
    keyTextColor: Color,
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
                keyHeight = keyHeight,
                keyBg = keyBg,
                keyShape = keyShape,
                keyTextColor = keyTextColor,
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
    keyHeight: Dp,
    keyBg: Color,
    keyShape: RoundedCornerShape,
    keyTextColor: Color,
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
            LetterKey(label = display, weight = 1f, keyHeight = keyHeight, keyBg = keyBg, keyShape = keyShape, keyTextColor = keyTextColor) { onKey(display) }
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
    keyHeight: Dp = 46.dp,
    keyBg: Color = Color.White,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keyTextColor: Color = Color(0xFF1A1A1A),
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(keyBg)
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { onLongPress() }
            )
    ) {
        Text(
            text = number,
            fontSize = 9.sp,
            color = keyTextColor.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp)
        )
        Text(
            text = label,
            fontSize = 18.sp,
            color = keyTextColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun RowScope.LetterKey(
    label: String,
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyBg: Color = Color.White,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keyTextColor: Color = Color(0xFF1A1A1A),
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(keyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 18.sp, color = keyTextColor)
    }
}

@Composable
private fun RowScope.ShiftKey(
    weight: Float,
    active: Boolean,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keySpecialBg: Color = Color(0xFFBCC4CC),
    keyTextColor: Color = Color(0xFF333333),
    onTap: () -> Unit
) {
    val activeBg = keySpecialBg.copy(alpha = 0.7f)
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(if (active) activeBg else keySpecialBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active) "▲" else "△",
            fontSize = 18.sp,
            color = keyTextColor
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keySpecialBg: Color = Color(0xFFBCC4CC),
    keyTextColor: Color = Color(0xFF333333),
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(keySpecialBg)
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
        Text(text = "⌫", fontSize = 18.sp, color = keyTextColor)
    }
}

@Composable
private fun RowScope.SpecialKey(
    label: String,
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keySpecialBg: Color = Color(0xFFBCC4CC),
    keyTextColor: Color = Color(0xFF333333),
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(keySpecialBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = keyTextColor
        )
    }
}

@Composable
private fun LangToggleKey(
    currentLanguage: String,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keySpecialBg: Color = Color(0xFFBCC4CC),
    keyTextColor: Color = Color(0xFF333333),
    onTap: () -> Unit
) {
    val isSinhala = currentLanguage == "si"
    Box(
        modifier = Modifier
            .height(keyHeight)
            .fillMaxWidth()
            .clip(keyShape)
            .background(keySpecialBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "අ",
                fontSize = 13.sp,
                color = if (isSinhala) DeshGreen else keyTextColor,
                fontWeight = if (isSinhala) FontWeight.Bold else FontWeight.Normal
            )
            // Green underline indicator when Sinhala active
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(2.dp)
                    .width(14.dp)
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
private fun RowScope.EmojiKey(
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    keySpecialBg: Color = Color(0xFFBCC4CC),
    keyTextColor: Color = Color(0xFF444444),
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // Screenshot shows: emoji face icon on top, comma below — single key
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(keySpecialBg)
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
                color = keyTextColor,
                lineHeight = 18.sp
            )
            Text(
                text = ",",
                fontSize = 10.sp,
                color = keyTextColor,
                lineHeight = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SpaceKey(
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    spaceKeyBg: Color = Color.White,
    keyTextColor: Color = Color(0xFF888888),
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(spaceKeyBg)
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { onLongPress() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Desh Keyboard",
            fontSize = 12.sp,
            color = keyTextColor.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.EnterKey(
    weight: Float,
    keyHeight: Dp = 46.dp,
    keyShape: RoundedCornerShape = RoundedCornerShape(6.dp),
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight)
            .weight(weight)
            .clip(keyShape)
            .background(DeshGreen)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        // Screenshot: left-pointing return arrow ←
        Text(text = "←", fontSize = 22.sp, color = Color.White)
    }
}
