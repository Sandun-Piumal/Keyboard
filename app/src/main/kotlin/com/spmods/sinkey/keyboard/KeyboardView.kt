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
import androidx.compose.material3.Text
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

// Brand green
private val DeshGreen = Color(0xFF2D6A4F)

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
            bg             = Color(0xFFDDE1E7),
            keyBg          = if (showKeyBorders) Color.White else Color(0xFFF0F2F5),
            specialKeyBg   = Color(0xFFBCC4CC),
            keyText        = Color(0xFF1A1A1A),
            specialKeyText = Color(0xFF333333),
            subText        = Color(0xFF888888),
            spaceKeyBg     = Color.White,
            spaceKeyText   = Color(0xFF888888),
        )
    }
}

/** Convert a 0..3 slider step to a concrete key-row height in dp. */
private fun stepToKeyHeight(step: Float): Dp = when (Math.round(step)) {
    0    -> 40.dp
    1    -> 46.dp
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
    val keyShape     = if (showKeyBorders) RoundedCornerShape(6.dp) else RoundedCornerShape(4.dp)

    var shift by remember { mutableStateOf(false) }
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKey(label = "?123", weight = 1.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { }
                    EmojiKey(
                        weight = 1.3f,
                        keyHeight = keyHeight,
                        colors = colors,
                        keyShape = keyShape,
                        onTap = { onKey(",") },
                        onLongPress = { showEmojiPicker = true }
                    )
                    Box(modifier = Modifier.weight(1.3f)) {
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
                        weight = 4.5f,
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

    androidx.compose.animation.AnimatedContent(
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
                    .height(40.dp)
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
                    .height(40.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
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
                        Text(text = label, fontSize = 18.sp, color = colors.subText)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.specialKeyBg)
                        .clickable { onKey("TOOL_MIC") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎤", fontSize = 16.sp)
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
        Text(text = number, fontSize = 9.sp, color = colors.subText,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        Text(text = label, fontSize = 18.sp, color = colors.keyText,
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
        Text(text = label, fontSize = 18.sp, color = colors.keyText)
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
        Text(text = if (active) "▲" else "△", fontSize = 18.sp, color = colors.specialKeyText)
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
        Text(text = "⌫", fontSize = 18.sp, color = colors.specialKeyText)
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
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.specialKeyText)
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
            Text(
                text = "අ", fontSize = 13.sp,
                color = if (isSinhala) DeshGreen else colors.specialKeyText,
                fontWeight = if (isSinhala) FontWeight.Bold else FontWeight.Normal
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
            Text(text = "☺", fontSize = 16.sp, color = colors.specialKeyText, lineHeight = 18.sp)
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
            text = "Desh Keyboard", fontSize = 12.sp,
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
        Text(text = "←", fontSize = 22.sp, color = Color.White)
    }
}
