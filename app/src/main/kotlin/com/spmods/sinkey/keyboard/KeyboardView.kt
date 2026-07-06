package com.spmods.sinkey.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.spmods.sinkey.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spmods.sinkey.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.detectTapGestures

// Number labels for top row keys
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Desh Keyboard exact accent color (accentContainer from ManglishLight theme)
private val DeshGreen = Color(0xFF6E9A65)

// ── Theme-aware color helpers ─────────────────────────────────────────────────

private data class KeyboardColors(
    val bg: Color,
    val keyBg: Color,
    val specialKeyBg: Color,
    val keyText: Color,
    val specialKeyText: Color,
    val subText: Color,
    val spaceKeyBg: Color,
    val spaceKeyText: Color,
)

@Composable
private fun keyboardColors(showKeyBorders: Boolean, isDark: Boolean): KeyboardColors {
    return if (isDark) {
        KeyboardColors(
            bg             = Color(0xFF1E1E1E),
            keyBg          = if (showKeyBorders) Color(0xFF2E2E2E) else Color(0xFF262626),
            specialKeyBg   = Color(0xFF3A3A3A),
            keyText        = Color(0xFFE8E8E8),
            specialKeyText = Color(0xFFCCCCCC),
            subText        = Color(0xFF888888),
            spaceKeyBg     = Color(0xFF2E2E2E),
            spaceKeyText   = Color(0xFF777777),
        )
    } else {
        KeyboardColors(
            // keyboard_background_light_bordered = #E6EAED
            bg             = Color(0xFFE6EAED),
            // Bordered: primaryContainer = White
            keyBg          = Color(0xFFFFFFFF),
            // secondaryContainer (functional keys) = #335f9154 overlay on bg
            specialKeyBg   = Color(0xFFC5CDD5),
            // onPrimaryContainer = black
            keyText        = Color(0xFF000000),
            // specialKeyText - dark grey
            specialKeyText = Color(0xFF444444),
            // subText for number hints
            subText        = Color(0xFF666666),
            // space bar bg same as key bg (white/light)
            spaceKeyBg     = Color(0xFFFFFFFF),
            // space bar text - medium grey
            spaceKeyText   = Color(0xFF888888),
        )
    }
}

/** Convert a 0..3 slider step to a concrete key-row height in dp. */
private fun stepToKeyHeight(step: Float): Dp = when (Math.round(step)) {
    // Desh exact: config_key_height_qwerty = 48dp (default = step 1)
    0    -> 42.dp
    1    -> 48.dp
    2    -> 54.dp
    else -> 62.dp
}

/** Convert a 0..3 slider step to bottom padding in dp. */
private fun stepToBottomPadding(step: Float): Dp = when (Math.round(step)) {
    0    -> 4.dp
    1    -> 10.dp
    2    -> 18.dp
    else -> 28.dp
}

@Composable
fun KeyboardView(
    currentLanguage: String,
    keyboardHeight: Float = 2f,
    bottomSpaceEnabled: Boolean = true,
    bottomSpaceSize: Float = 0f,
    showKeyBorders: Boolean = true,
    isDark: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {},
    onKey: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val colors = keyboardColors(showKeyBorders, isDark)

    val keyHeight    = stepToKeyHeight(keyboardHeight)
    val bottomPadding = if (bottomSpaceEnabled) stepToBottomPadding(bottomSpaceSize) else 4.dp
    // Desh exact: config_key_radius = 6.0dip
    val keyShape     = RoundedCornerShape(6.dp)

    var shift by remember { mutableStateOf(false) }
    var showSymbols by remember { mutableStateOf(false) }
    var showLangTooltip by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())

    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    if (showSymbols) {
        SymbolsKeyboardView(
            colors = colors,
            keyHeight = keyHeight,
            keyShape = keyShape,
            bottomPadding = bottomPadding,
            onKey = onKey,
            onBack = { showSymbols = false }
        )
        return
    }

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
                .background(colors.bg)
        ) {
            AppsMicBar(
                colors = colors,
                suggestions = suggestions,
                onSuggestionSelected = onSuggestionSelected,
                onKey = onKey
            )

            EmojiRow(
                emojis = recentEmojis,
                colors = colors,
                onKey = onKey,
                onMoreClick = { showEmojiPicker = true }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .padding(bottom = bottomPadding)
            ) {
                NumberedKeyRow(EnglishRows[0], topRowNumbers, shift, keyHeight, colors, keyShape) { onKey(it) }
                KeyRow(EnglishRows[1], shift, keyHeight, colors, keyShape) { onKey(it) }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShiftKey(weight = 1.4f, active = shift, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                        shift = !shift
                        onKey("SHIFT")
                    }
                    EnglishRows[2].forEach { k ->
                        val display = if (shift) k.uppercase() else k
                        LetterKey(label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(display) }
                    }
                    BackspaceKey(weight = 1.4f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SymbolsKey(weight = 1.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { showSymbols = true }
                    EmojiKey(
                        weight = 0.9f,
                        keyHeight = keyHeight,
                        colors = colors,
                        keyShape = keyShape,
                        onTap = { onKey(",") },
                        onLongPress = { showEmojiPicker = true }
                    )
                    Box(modifier = Modifier.weight(0.9f)) {
                        LangToggleKey(
                            currentLanguage = currentLanguage,
                            keyHeight = keyHeight,
                            colors = colors,
                            keyShape = keyShape,
                            onTap = {
                                onKey("LANG_TOGGLE")
                                showLangTooltip = true
                            }
                        )
                    }
                    SpaceKey(
                        weight = 5.5f,
                        keyHeight = keyHeight,
                        colors = colors,
                        keyShape = keyShape,
                        onTap = { onKey("SPACE") },
                        onLongPress = { onKey("SWITCH_KEYBOARD") }
                    )
                    SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(".") }
                    EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
                }
            }
        }

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
// Toolbar — tools row OR suggestion strip depending on typing state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppsMicBar(
    colors: KeyboardColors,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    onKey: (String) -> Unit
) {
    val isTyping = suggestions.isNotEmpty()

    @OptIn(ExperimentalAnimationApi::class)
    AnimatedContent(
        targetState = isTyping,
        transitionSpec = {
            (fadeIn() + slideInVertically { -it }) togetherWith
            (fadeOut() + slideOutVertically { -it })
        },
        label = "toolbar_anim"
    ) { typing ->
        if (typing) {
            // ── Suggestion strip ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.bg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    items(suggestions.size) { idx ->
                        val word = suggestions[idx]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSuggestionSelected(word) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = word, fontSize = 14.sp, color = colors.keyText, maxLines = 1)
                        }
                        if (idx < suggestions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(1.dp)
                                    .background(colors.subText.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        } else {
            // ── Full tools row ───────────────────────────────────────────
            val tools = listOf(
                R.drawable.ic_unified_menu  to "TOOL_APPS",
                R.drawable.ic_sticker       to "TOOL_STICKER",
                R.drawable.ic_emoji_for_compose to "TOOL_CLIPBOARD",
                R.drawable.ic_custom_font   to "TOOL_FONT",
                R.drawable.ic_translation   to "TOOL_TRANSLATE",
                R.drawable.ic_settings      to "TOOL_SETTINGS"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (iconRes, action) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onKey(action) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = colors.subText
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFBBBBBB))
                        .clickable { onKey("TOOL_MIC") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎤", fontSize = 19.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emoji row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmojiRow(emojis: List<String>, colors: KeyboardColors, onKey: (String) -> Unit, onMoreClick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(colors.bg).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        state = rememberLazyListState()
    ) {
        item { Spacer(modifier = Modifier.width(4.dp)) }
        items(emojis.size) { index ->
            val emoji = emojis[index]
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onKey(emoji) },
                contentAlignment = Alignment.Center
            ) { Text(text = emoji, fontSize = 22.sp) }
        }
        item {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) { Text(text = "•••", fontSize = 14.sp, color = colors.subText) }
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
                text = if (currentLanguage == "en")
                    buildAnnotatedStringBold("English", " enabled")
                else
                    buildAnnotatedStringBold("සිංහල", " enabled"),
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
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEachIndexed { index, k ->
            val display = if (shift) k.uppercase() else k
            val num = numbers.getOrNull(index) ?: ""
            NumberedLetterKey(
                label = display, number = num, weight = 1f,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
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
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(display) }
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
    label: String, number: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.keyBg)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
    ) {
        // Desh exact: hint_letter_ratio_lxx = 25% of 48dp = 12dp ≈ 11sp
        Text(text = number, fontSize = 11.sp, color = colors.subText,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        // Desh exact: config_key_font_size = 24dp, letter_ratio_lxx = 55% of key height
        Text(text = label, fontSize = 22.sp, color = colors.keyText, fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun RowScope.LetterKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.keyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 22.sp, color = colors.keyText, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun RowScope.ShiftKey(
    weight: Float, active: Boolean,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    val bg = if (active)
        colors.specialKeyBg.copy(alpha = 0.7f)
    else
        colors.specialKeyBg
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(bg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (active) R.drawable.ic_shift_key_shifted else R.drawable.ic_shift_key),
            contentDescription = "Shift",
            modifier = Modifier.size(26.dp),
            tint = if (active) DeshGreen else colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.specialKeyBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        val longPressDelay = 400L
                        val repeatInterval = 50L
                        val released = withTimeoutOrNull(longPressDelay) { tryAwaitRelease() }
                        if (released != null) {
                            onTap()
                        } else {
                            onTap()
                            try {
                                while (true) {
                                    delay(repeatInterval)
                                    onTap()
                                    val done = withTimeoutOrNull(1L) { tryAwaitRelease() }
                                    if (done != null) break
                                }
                            } catch (_: Exception) { }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_backspace),
            contentDescription = "Backspace",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.SpecialKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.specialKeyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.specialKeyText)
    }
}

@Composable
private fun RowScope.SymbolsKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.specialKeyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back_to_symbols),
            contentDescription = "Symbols",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun LangToggleKey(
    currentLanguage: String,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    val isSinhala = currentLanguage == "si"
    Box(
        modifier = Modifier
            .height(keyHeight).fillMaxWidth()
            .clip(keyShape).background(colors.specialKeyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_native_letter),
                contentDescription = "Language",
                modifier = Modifier.size(18.dp),
                tint = if (isSinhala) DeshGreen else colors.specialKeyText
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp).height(2.dp).width(18.dp)
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
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.specialKeyBg)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() }),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                contentDescription = "Emoji",
                modifier = Modifier.size(20.dp),
                tint = colors.specialKeyText
            )
            Text(text = ",", fontSize = 10.sp, color = colors.specialKeyText, lineHeight = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SpaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(colors.spaceKeyBg)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sinkey board", fontSize = 12.sp,
            color = colors.spaceKeyText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.EnterKey(
    weight: Float,
    keyHeight: Dp, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(DeshGreen)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_enter_key),
            contentDescription = "Enter",
            modifier = Modifier.size(26.dp),
            tint = Color.White
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Symbols Keyboard
// ─────────────────────────────────────────────────────────────────────────────

private val SymbolsRow1 = listOf("1","2","3","4","5","6","7","8","9","0")
private val SymbolsRow2 = listOf("@","#","₹","%","&","*","-","=","(",")")
private val SymbolsRow3 = listOf("!","\"","'",":","+","/","?")

@Composable
fun SymbolsKeyboardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
    ) {
        // Toolbar + emoji row same as main keyboard
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .padding(bottom = bottomPadding)
        ) {
            // Row 1: numbers
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SymbolsRow1.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 2: symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SymbolsRow2.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 3: <\> + symbols + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // <\> key
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.4f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onKey("SYMBOLS_SHIFT") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "<\\>", fontSize = 13.sp, color = colors.specialKeyText, fontWeight = FontWeight.Normal)
                }
                SymbolsRow3.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(ch) }
                }
                // Backspace
                BackspaceKey(weight = 1.4f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
            }

            // Bottom row: ABC + , + emoji + space + 12/34 + . + enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABC key
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.8f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ABC", fontSize = 14.sp, color = colors.specialKeyText, fontWeight = FontWeight.Medium)
                }
                // Comma
                SpecialKey(label = ",", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(",") }
                // Emoji
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(0.9f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "☺", fontSize = 18.sp, color = colors.specialKeyText)
                }
                // Space
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(5.5f)
                        .clip(keyShape).background(colors.spaceKeyBg)
                        .clickable { onKey("SPACE") },
                    contentAlignment = Alignment.Center
                ) { }
                // 12/34 key
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.0f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onKey("NUMPAD") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "12\n34", fontSize = 11.sp, color = colors.specialKeyText, fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                // Dot
                SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(".") }
                // Enter
                EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
            }
        }
    }
}
