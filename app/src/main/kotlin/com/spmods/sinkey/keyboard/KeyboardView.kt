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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

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
    onDismiss: (() -> Unit)? = null,
    inputType: Int = 0
) {
    val colors = keyboardColors(showKeyBorders, isDark)
    val keyHeight = stepToKeyHeight(keyboardHeight)
    val bottomPadding = if (bottomSpaceEnabled) stepToBottomPadding(bottomSpaceSize) else 4.dp
    val keyShape = RoundedCornerShape(6.dp)

    var shift by remember { mutableStateOf(false) }
    var showSymbols by remember { mutableStateOf(false) }
    var showLangTooltip by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val isPhoneInput = remember(inputType) {
        (inputType and android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE) ==
            android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE
    }

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())

    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    // ONE Column for the whole keyboard — toolbar always at top, content below
    // wrapContentHeight() is critical: it constrains the root Box to exactly
    // the height of its content. Without it the IME window gives us the full
    // remaining screen height and Android renders a ghost copy of the keyboard
    // in the empty space above — producing the "double keyboard" appearance.
    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(colors.bg)
        ) {
            // ── Toolbar (always visible, never re-created on pad switch) ──────
            AppsMicBar(
                colors = colors,
                suggestions = if (isPhoneInput || showSymbols) emptyList() else suggestions,
                onSuggestionSelected = onSuggestionSelected,
                onKey = onKey
            )

            // ── Recent emoji row (not on dial pad) ───────────────────────────
            if (!isPhoneInput && recentEmojis.isNotEmpty()) {
                EmojiRow(
                    emojis = recentEmojis,
                    colors = colors,
                    onKey = onKey,
                    onMoreClick = { showEmojiPicker = true }
                )
            }

            // ── Content area — only this part switches ────────────────────────
            when {
                isPhoneInput -> PhoneDialPadKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding, onKey = onKey
                )
                showEmojiPicker -> EmojiPickerView(
                    recentEmojis = recentEmojis,
                    onEmojiSelected = { emoji -> onKey(emoji) },
                    onBackspace = { onKey("BACKSPACE") },
                    onDismiss = { showEmojiPicker = false }
                )
                showSymbols -> SymbolsKeyboardKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding,
                    onKey = onKey, onBack = { showSymbols = false }
                )
                else -> MainKeyboardKeys(
                    currentLanguage = currentLanguage,
                    shift = shift, onShiftChange = { shift = it },
                    keyHeight = keyHeight, keyShape = keyShape,
                    bottomPadding = bottomPadding, colors = colors,
                    onKey = onKey,
                    onSymbols = { showSymbols = true },
                    onEmojiPicker = { showEmojiPicker = true },
                    onLangTooltip = { showLangTooltip = true }
                )
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
// ─────────────────────────────────────────────────────────────────────────────
// Content-only composables (no toolbar — toolbar is in KeyboardView)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainKeyboardKeys(
    currentLanguage: String,
    shift: Boolean,
    onShiftChange: (Boolean) -> Unit,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    colors: KeyboardColors,
    onKey: (String) -> Unit,
    onSymbols: () -> Unit,
    onEmojiPicker: () -> Unit,
    onLangTooltip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
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
                onShiftChange(!shift); onKey("SHIFT")
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
            SymbolsKey(weight = 1.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onSymbols() }
            EmojiKey(weight = 0.9f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onTap = { onKey(",") }, onLongPress = { onEmojiPicker() })
            Box(modifier = Modifier.weight(0.9f)) {
                LangToggleKey(currentLanguage = currentLanguage, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape,
                    onTap = { onKey("LANG_TOGGLE"); onLangTooltip() })
            }
            SpaceKey(weight = 5.5f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onTap = { onKey("SPACE") }, onLongPress = { onKey("SWITCH_KEYBOARD") })
            SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(".") }
            EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
        }
    }
}

@Composable
private fun SymbolsKeyboardKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit
) {
    SymbolsKeyboardView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey, onBack = onBack
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey
    )
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
private fun ConditionalEmojiRow(
    colors: KeyboardColors,
    onKey: (String) -> Unit,
    onMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())
    if (recentEmojis.isNotEmpty()) {
        EmojiRow(emojis = recentEmojis, colors = colors, onKey = onKey, onMoreClick = onMoreClick)
    }
}

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
    useSearchIcon: Boolean = false,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(keyShape).background(DeshGreen)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (useSearchIcon) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_enter_key),
                contentDescription = "Enter",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Symbols Keyboard  (Desh-exact from APK XML analysis)
// ─────────────────────────────────────────────────────────────────────────────

// rowkeys_symbols1.xml  → keyspec_symbols_0..9
private val SymRow1 = listOf("1","2","3","4","5","6","7","8","9","0")
// rowkeys_symbols2.xml  → @ # ₹(mainCurrencyKey) % & * - = ( )
private val SymRow2 = listOf("@","#","₹","%","&","*","-","=","(",")")
// rowkeys_symbols3.xml  → ! " ' : + / ?
private val SymRow3 = listOf("!","\"","'",":","+","/","?")

// rowkeys_symbols_shift1.xml  → ~ ` _ ° ± ´ × ÷ • √
private val SymShiftRow1 = listOf("~","`","_","°","±","´","×","÷","•","√")
// rowkeys_symbols_shift2.xml  → ^ ₩ £ € ¥ $ © ® ™ π
private val SymShiftRow2 = listOf("^","₩","£","€","¥","$","©","®","™","π")
// rowkeys_symbols_shift3.xml  → \ | < > ; ¡ ¿
private val SymShiftRow3 = listOf("\\","|","<",">",";","¡","¿")

@Composable
private fun SymbolsKeyboardContent(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onShowEmoji: () -> Unit = {}
) {
    SymbolsKeyboardView(
        colors = colors,
        keyHeight = keyHeight,
        keyShape = keyShape,
        bottomPadding = bottomPadding,
        onKey = onKey,
        onBack = onBack
    )
}

@Composable
private fun SymbolsKeyboardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit
) {
    var shifted by remember { mutableStateOf(false) }
    var showEmojiFromSymbols by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }

    if (showNumpad) {
        NumberPadView(
            colors = colors,
            keyHeight = keyHeight,
            keyShape = keyShape,
            bottomPadding = bottomPadding,
            onKey = onKey,
            onBack = { showNumpad = false }
        )
        return
    }

    if (showEmojiFromSymbols) {
        val context = LocalContext.current
        val prefsManager = remember { PreferencesManager(context) }
        val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())
        EmojiPickerView(
            recentEmojis = recentEmojis,
            onEmojiSelected = { emoji -> onKey(emoji) },
            onBackspace = { onKey("BACKSPACE") },
            onDismiss = { showEmojiFromSymbols = false }
        )
        return
    }

    val row1 = if (shifted) SymShiftRow1 else SymRow1
    val row2 = if (shifted) SymShiftRow2 else SymRow2
    val row3 = if (shifted) SymShiftRow3 else SymRow3

    Column(
        modifier = Modifier.fillMaxWidth().background(colors.bg)
    ) {
        // ── Key rows ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .padding(bottom = bottomPadding)
        ) {
            // Row 1: numbers / shift-symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row1.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 2: symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 3: shift-toggle + symbols + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // <\> shift key  (ic_back_to_symbols drawable)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.4f)
                        .clip(keyShape)
                        .background(if (shifted) DeshGreen else colors.specialKeyBg)
                        .clickable { shifted = !shifted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_to_symbols),
                        contentDescription = "Shift",
                        modifier = Modifier.size(22.dp),
                        tint = if (shifted) Color.White else colors.specialKeyText
                    )
                }
                row3.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
                BackspaceKey(weight = 1.4f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
            }

            // Bottom row  (row_symbols_bottom.xml)
            // toLatinFromSymbolsKeyStyle | comma | toEmojiKeyStyle | space | toNumpadKeyStyle | . | enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABC  (toLatinFromSymbolsKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.8f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ABC", fontSize = 14.sp,
                        color = colors.specialKeyText, fontWeight = FontWeight.Medium)
                }
                // , (comma_key)
                SpecialKey(label = ",", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(",") }
                // Emoji  (toEmojiKeyStyle → ic_emoji_for_compose)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(0.9f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { showEmojiFromSymbols = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                        contentDescription = "Emoji",
                        modifier = Modifier.size(22.dp),
                        tint = colors.specialKeyText
                    )
                }
                // Space  (spaceKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(5.5f)
                        .clip(keyShape).background(colors.spaceKeyBg)
                        .clickable { onKey("SPACE") },
                    contentAlignment = Alignment.Center
                ) { }
                // 12/34  (toNumpadKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.0f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { showNumpad = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "12\n34", fontSize = 11.sp, color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                // .
                SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(".") }
                // Enter  (enterKeyStyle)
                EnterKey(weight = 2.0f, keyHeight = keyHeight,
                    keyShape = keyShape) { onKey("ENTER") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number Pad  (phone-style 3×3 + bottom row)
// Layout matches screenshot: 1 2 3 / 4 5 6 / 7 8 9 / . 0 _ with ABC, , ⌫ Enter on right
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit
) {
    // ── Numpad grid ─────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Row 1: 1  2  3  │ ABC
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("1", keyHeight, colors, keyShape) { onKey("1") }
                NumpadDigitKey("2", keyHeight, colors, keyShape) { onKey("2") }
                NumpadDigitKey("3", keyHeight, colors, keyShape) { onKey("3") }
                // ABC — back to symbols keyboard
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        fontSize = 14.sp,
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Row 2: 4  5  6  │ ,
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("4", keyHeight, colors, keyShape) { onKey("4") }
                NumpadDigitKey("5", keyHeight, colors, keyShape) { onKey("5") }
                NumpadDigitKey("6", keyHeight, colors, keyShape) { onKey("6") }
                // comma
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onKey(",") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ",",
                        fontSize = 20.sp,
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Row 3: 7  8  9  │ ⌫
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("7", keyHeight, colors, keyShape) { onKey("7") }
                NumpadDigitKey("8", keyHeight, colors, keyShape) { onKey("8") }
                NumpadDigitKey("9", keyHeight, colors, keyShape) { onKey("9") }
                // Backspace
                BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                    onKey("BACKSPACE")
                }
            }

            // Row 4: .  0  _  │ Enter (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // . (decimal / period)
                NumpadDigitKey(".", keyHeight, colors, keyShape) { onKey(".") }
                // 0
                NumpadDigitKey("0", keyHeight, colors, keyShape) { onKey("0") }
                // _ (underscore)
                NumpadDigitKey("_", keyHeight, colors, keyShape) { onKey("_") }
                // Enter — green
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
            }
        }
}

/** A single large numpad digit/symbol key. */
@Composable
private fun RowScope.NumpadDigitKey(
    label: String,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(keyHeight).weight(1f)
            .clip(keyShape).background(colors.keyBg)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            color = colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phone Dial Pad  (auto-shown when system sends TYPE_CLASS_PHONE)
// Layout: 1 / 2 ABC / 3 DEF / 4 GHI / 5 JKL / 6 MNO
//          7 PQRS / 8 TUV / 9 WXYZ / *# / 0+ / _ / Search(green)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadContent(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors,
        keyHeight = keyHeight,
        keyShape = keyShape,
        bottomPadding = bottomPadding,
        onKey = onKey
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    val dialKeys = listOf(
        Triple("1", "", "1"),
        Triple("2", "ABC", "2"),
        Triple("3", "DEF", "3"),
        Triple("4", "GHI", "4"),
        Triple("5", "JKL", "5"),
        Triple("6", "MNO", "6"),
        Triple("7", "PQRS", "7"),
        Triple("8", "TUV", "8"),
        Triple("9", "WXYZ", "9")
    )

    // ── Grid ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Rows 1-3: 1  2ABC  3DEF │ -   /   4GHI  5JKL  6MNO │ .   /   7PQRS  8TUV  9WXYZ │ ⌫
            val sideKeys = listOf("-", ".", null) // right-side special keys per row
            dialKeys.chunked(3).forEachIndexed { rowIdx, trio ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trio.forEach { (digit, sub, key) ->
                        Box(
                            modifier = Modifier
                                .height(keyHeight).weight(1f)
                                .clip(keyShape).background(colors.keyBg)
                                .clickable { onKey(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = digit,
                                    fontSize = 26.sp,
                                    color = colors.keyText,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 28.sp
                                )
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        fontSize = 9.sp,
                                        color = colors.keyText.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    // Right-side key
                    when (rowIdx) {
                        0 -> { // -
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey("-") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = 24.sp, color = colors.specialKeyText)
                            }
                        }
                        1 -> { // .
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey(".") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(".", fontSize = 24.sp, color = colors.specialKeyText)
                            }
                        }
                        2 -> { // ⌫
                            BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                                onKey("BACKSPACE")
                            }
                        }
                    }
                }
            }

            // Row 4: *#  /  0+  /  _  │ Search (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // *# — tap → *, long press → #
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey("*") }, onLongClick = { onKey("#") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("* #", fontSize = 20.sp, color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // 0+ — tap → 0, long press → +
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey("0") }, onLongClick = { onKey("+") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("0 +", fontSize = 20.sp, color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // _ — tap → _, long press → switch keyboard
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey(" ") }, onLongClick = { onKey("SWITCH_KEYBOARD") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("_", fontSize = 24.sp, color = colors.keyText)
                }
                // Search / Enter (green)
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape, useSearchIcon = true) {
                    onKey("ENTER")
                }
            }
        }
    }
