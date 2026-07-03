package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.ui.theme.AccentGradient

/**
 * The actual on-screen keyboard, rendered by [com.spmods.sinkey.ime.SinKeyInputMethodService].
 *
 * Key taps are reported through [onKey] with either a literal character,
 * or one of the control tokens: "BACKSPACE", "SPACE", "ENTER", "SHIFT",
 * "LANG_TOGGLE".
 */
@Composable
fun KeyboardView(
    currentLanguage: String, // "en" or "si"
    onKey: (String) -> Unit
) {
    var shift by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        KeyRow(EnglishRows[0], shift) { onKey(it) }
        KeyRow(EnglishRows[1], shift) { onKey(it) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialKey(label = "⇧", weight = 1.2f, active = shift) {
                shift = !shift
                onKey("SHIFT")
            }
            EnglishRows[2].forEach { k ->
                val display = if (shift) k.uppercase() else k
                LetterKey(label = display, weight = 1f) { onKey(display) }
            }
            SpecialKey(label = "⌫", weight = 1.2f) { onKey("BACKSPACE") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecialKey(label = if (currentLanguage == "en") "🌐 EN" else "🌐 සිං", weight = 1.6f) {
                onKey("LANG_TOGGLE")
            }
            SpecialKey(label = ",", weight = 1f) { onKey(",") }
            SpaceKey(weight = 4f) { onKey("SPACE") }
            SpecialKey(label = ".", weight = 1f) { onKey(".") }
            EnterKey(weight = 1.6f) { onKey("ENTER") }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(label = display, weight = 1f) { onKey(display) }
        }
    }
}

@Composable
private fun RowScope.LetterKey(label: String, weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.SpecialKey(label: String, weight: Float, active: Boolean = false, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.background
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.SpaceKey(weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "SPACE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.EnterKey(weight: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .weight(weight)
            .clip(RoundedCornerShape(10.dp))
            .background(AccentGradient)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "↵", fontSize = 18.sp, color = Color.White)
    }
}
