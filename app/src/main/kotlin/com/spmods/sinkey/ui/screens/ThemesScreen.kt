package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.KeyEffect
import com.spmods.sinkey.data.ThemeMode

private data class ThemeOption(val label: String, val siLabel: String, val bg: Color, val fg: Color, val emoji: String, val mode: ThemeMode?)

private val themeOptions = listOf(
    ThemeOption("Cream Light", "ආලෝකය", Color(0xFFF4F2ED), Color(0xFF241C14), "☀️", ThemeMode.LIGHT),
    ThemeOption("Night", "අඳුර", Color(0xFF15130F), Color(0xFFF2EDE4), "🌙", ThemeMode.DARK),
    ThemeOption("Follow system", "පද්ධතිය", Color(0xFFEFE6D8), Color(0xFF7A2038), "⚙️", ThemeMode.SYSTEM)
)

/**
 * Themes tab in the app's own settings UI (not the keyboard itself). Three
 * sections beyond the original light/dark/system picker:
 *  - Colors: an accent palette (KeyColorPalette) tinting the space bar /
 *    special keys and driving the Effects colors below.
 *  - Effects: a border/glow/underline style (KeyEffect) drawn on every key.
 *  - My themes: a user-picked photo shown as the keyboard's background.
 *
 * All three are purely additive on top of the existing light/dark/system
 * selection — none of them replace it, matching how KeyboardColors layers
 * palette/effect/background onto the base light/dark colors rather than
 * substituting for them (see KeyboardView.keyboardColors()).
 */
@Composable
fun ThemesScreen(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    keyColorPalette: KeyColorPalette = KeyColorPalette.DEFAULT,
    onKeyColorPaletteChange: (KeyColorPalette) -> Unit = {},
    keyEffect: KeyEffect = KeyEffect.NONE,
    onKeyEffectChange: (KeyEffect) -> Unit = {},
    // Content-resolver-backed preview bitmap of the currently set "My
    // themes" photo, or null if none is set. Decoding happens in
    // MainActivity (which owns the PickVisualMedia launcher and has a
    // CoroutineScope to decode off the main thread) rather than here, so
    // this screen stays a plain stateless composable like the rest of the
    // app's screens.
    customBackgroundPreview: android.graphics.Bitmap? = null,
    onPickCustomBackground: () -> Unit = {},
    onClearCustomBackground: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(modifier = Modifier.padding(22.dp, 18.dp, 22.dp, 4.dp)) {
            Text(
                "APPEARANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text("Choose a theme", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "The keyboard follows your pick everywhere you type.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themeOptions) { option ->
                val selected = option.mode == currentMode
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { option.mode?.let(onSelect) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(option.bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option.emoji, fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.padding(12.dp, 9.dp)) {
                        Text(option.label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(option.siLabel, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionHeader("My themes")
        MyThemesRow(
            preview = customBackgroundPreview,
            onPick = onPickCustomBackground,
            onClear = onClearCustomBackground
        )

        SectionHeader("Colors")
        ColorsGrid(selected = keyColorPalette, onSelect = onKeyColorPaletteChange)

        SectionHeader("Effects")
        EffectsGrid(selected = keyEffect, palette = keyColorPalette, onSelect = onKeyEffectChange)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp)
    )
}

/**
 * "My themes" row: an "Add Photo" tile (always first, always present so the
 * user can always change/re-pick) followed by the currently-set photo as a
 * selected preview tile with a small remove (X) button, when one is set.
 */
@Composable
private fun MyThemesRow(
    preview: android.graphics.Bitmap?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Photo tile — dashed border, matches the reference screenshot's
        // "My themes" empty-state tile. Tapping it always opens the picker,
        // regardless of whether a photo is already set, so re-picking a
        // different photo doesn't require clearing first.
        Column(
            modifier = Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .dashedBorder(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = "Add photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Add Photo",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (preview != null) {
            Box(modifier = Modifier.size(108.dp)) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Custom background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** Simple manual dashed-border drawer — Compose has no built-in dashed Border. */
private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape): Modifier = this.drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
        width = 1.5.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    )
    val cornerPx = 14.dp.toPx()
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx)
    )
}

@Composable
private fun ColorsGrid(selected: KeyColorPalette, onSelect: (KeyColorPalette) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(((KeyColorPalette.entries.size + 2) / 3) * 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(KeyColorPalette.entries) { palette ->
            val isSelected = palette == selected
            Column(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(palette) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.accent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    palette.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * Mini "q w e" key-row previews, matching the reference screenshots' Effects
 * cards, rendered with real Compose drawing (no bitmap assets) so each
 * option shows exactly what it'll look like on the real keyboard.
 */
@Composable
private fun EffectsGrid(selected: KeyEffect, palette: KeyColorPalette, onSelect: (KeyEffect) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(((KeyEffect.entries.size + 1) / 2) * 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(KeyEffect.entries) { effect ->
            val isSelected = effect == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(Color(0xFF1E1E1E))
                    .clickable { onSelect(effect) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("q", "w", "e").forEach { letter ->
                        EffectPreviewKey(letter, effect, palette.accent, Modifier.weight(1f))
                    }
                }
                Text(
                    effect.label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 10.dp)
                )
            }
        }
    }
}

@Composable
private fun EffectPreviewKey(letter: String, effect: KeyEffect, accent: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val decorated = when (effect) {
        KeyEffect.NONE -> modifier
        KeyEffect.OUTLINE -> modifier.border(1.5.dp, accent.copy(alpha = 0.85f), shape)
        KeyEffect.GLOW -> modifier.shadow(elevation = 6.dp, shape = shape, ambientColor = accent, spotColor = accent)
        KeyEffect.UNDERLINE -> modifier.drawBehind {
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = accent,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height - strokeWidth),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height - strokeWidth),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
    Box(
        modifier = decorated
            .fillMaxSize()
            .clip(shape)
            .background(Color(0xFF3A3A3A)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = Color.White, fontSize = 15.sp)
    }
}
