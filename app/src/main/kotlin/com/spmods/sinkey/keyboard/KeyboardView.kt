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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Number labels for top row keys (QWERTYUIOP → 1–9, 0)
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Dark green color matching Desh Keyboard style
private val DeshGreen = Color(0xFF2D6A4F)

@Composable
fun KeyboardView(
    currentLanguage: String, // "en" or "si"
    onKey: (String) -> Unit
) {
    var shift by remember { mutableStateOf(false) }
    var showLangTooltip by remember { mutableStateOf(false) }

    // Auto-hide tooltip after 1.5 seconds
    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDDE1E7))
                .padding(horizontal = 4.dp, vertical = 6.dp)
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
                SpecialKey(label = "?123", weight = 2.0f) { /* TODO: number layout */ }
                EmojiKey(weight = 1.5f, onTap = { onKey(",") }, onLongPress = { onKey("EMOJI") })

                // Language toggle — with tooltip anchor
                Box(modifier = Modifier.weight(1.5f)) {
                    LangToggleKey(
                        currentLanguage = currentLanguage,
                        onTap = {
                            onKey("LANG_TOGGLE")
                            showLangTooltip = true
                        }
                    )
                }

                SpaceKey(weight = 4.5f) { onKey("SPACE") }
                SpecialKey(label = ".", weight = 0.8f) { onKey(".") }
                EnterKey(weight = 2.0f) { onKey("ENTER") }
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

// Helper to bold the first part
@Composable
private fun buildAnnotatedStringBold(bold: String, normal: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
        append(bold)
        pop()
        append(normal)
    }
}

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
        // Center row 2 (9 keys) with small padding
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(label = display, weight = 1f) { onKey(display) }
        }
        Box(modifier = Modifier.weight(0.5f))
    }
}

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
        // Number superscript — top right
        Text(
            text = number,
            fontSize = 9.sp,
            color = Color(0xFF888888),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp)
        )
        // Main letter — center
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
                    onTap = { onTap() },
                    onPress = { _ ->
                        // Wait for long-press threshold, then start repeating
                        val longPressDelay = 400L
                        val repeatInterval = 50L
                        var didLongPress = false
                        try {
                            delay(longPressDelay)
                            didLongPress = true
                            // Keep firing while finger held down
                            while (isActive) {
                                onTap()
                                delay(repeatInterval)
                            }
                        } finally {
                            // If finger lifted before long press, onTap() already
                            // called by onTap handler above — nothing extra needed
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
                fontSize = 17.sp,
                // Green text when Sinhala, grey when English
                color = if (isSinhala) DeshGreen else Color(0xFF333333),
                fontWeight = if (isSinhala) FontWeight.Bold else FontWeight.Medium
            )
            // Underline: filled green when Sinhala, transparent when English
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(
                        color = if (isSinhala) DeshGreen else Color.Transparent,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 1.5.dp)
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.EmojiKey(weight: Float, onTap: () -> Unit, onLongPress: () -> Unit) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "☺", fontSize = 13.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
            Text(text = ",", fontSize = 10.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RowScope.SpaceKey(weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SinKey",
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
        Text(text = "↵", fontSize = 20.sp, color = Color.White)
    }
}
