package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.spmods.sinkey.data.BackgroundStyle
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.KeyEffect
import com.spmods.sinkey.data.LedPattern
import com.spmods.sinkey.data.ThemeMode
import com.spmods.sinkey.data.TypingAnimation

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
    onClearCustomBackground: () -> Unit = {},
    // Backgrounds: built-in procedural presets (Gradient/Rainbow/Galaxy/...).
    backgroundStyle: BackgroundStyle = BackgroundStyle.NONE,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit = {},
    // Material You: dynamic accent from the system wallpaper (Android 12+).
    materialYouEnabled: Boolean = false,
    onMaterialYouEnabledChange: (Boolean) -> Unit = {},
    materialYouAvailable: Boolean = true,
    // Typing Animation: emoji/icon pop-up on keypress, plus DIY custom
    // emoji/image variants.
    typingAnimation: TypingAnimation = TypingAnimation.NONE,
    onTypingAnimationChange: (TypingAnimation) -> Unit = {},
    typingAnimationEmoji: String = "✨",
    onTypingAnimationEmojiChange: (String) -> Unit = {},
    typingAnimationImagePreview: android.graphics.Bitmap? = null,
    onPickTypingAnimationImage: () -> Unit = {},
    onClearTypingAnimationImage: () -> Unit = {},
    // LED / Neon lighting pattern (ambient strip) + idle dimming.
    ledPattern: LedPattern = LedPattern.NONE,
    onLedPatternChange: (LedPattern) -> Unit = {},
    ledIdleDimming: Boolean = true,
    onLedIdleDimmingChange: (Boolean) -> Unit = {}
) {
    // Scrollable: with the Colors/Effects grids now holding more entries
    // than fit on one screen (12 colors, 9 effects), the page needs its own
    // scroll — previously this Column had no verticalScroll at all, so
    // content below the fold (esp. the Effects grid) was simply clipped and
    // unreachable rather than scrollable.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
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

        // Manual 2-column chunking instead of LazyVerticalGrid — this Column
        // is now inside the page's own verticalScroll, and nesting a lazy
        // scrollable grid inside a scrolling Column caused the janky
        // "scrolls in pieces" feel (competing nested-scroll containers).
        // A plain Column of Rows has no scroll behavior of its own, so the
        // whole page scrolls as a single smooth list.
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            themeOptions.chunked(2).forEach { rowOptions ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowOptions.forEach { option ->
                        val selected = option.mode == currentMode
                        Column(
                            modifier = Modifier
                                .weight(1f)
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
                    // Pad out an odd last row so the single remaining tile
                    // keeps half-width instead of stretching full-width.
                    if (rowOptions.size == 1) {
                        Box(modifier = Modifier.weight(1f))
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

        SectionHeader("Backgrounds")
        Text(
            "Gradient, rainbow, galaxy, smoke and more — used when no custom photo is set above.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp)
        )
        BackgroundsGrid(selected = backgroundStyle, onSelect = onBackgroundStyleChange)

        SectionHeader("Material You")
        MaterialYouRow(
            enabled = materialYouEnabled,
            available = materialYouAvailable,
            onToggle = onMaterialYouEnabledChange
        )

        SectionHeader("Colors")
        ColorsGrid(selected = keyColorPalette, onSelect = onKeyColorPaletteChange)

        SectionHeader("Effects")
        EffectsGrid(selected = keyEffect, palette = keyColorPalette, onSelect = onKeyEffectChange)

        SectionHeader("Typing Animation")
        Text(
            "An emoji or icon pops up near a key as you type.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp)
        )
        TypingAnimationGrid(selected = typingAnimation, onSelect = onTypingAnimationChange)
        if (typingAnimation == TypingAnimation.CUSTOM_EMOJI) {
            CustomEmojiPicker(
                emoji = typingAnimationEmoji,
                onEmojiChange = onTypingAnimationEmojiChange
            )
        }
        if (typingAnimation == TypingAnimation.CUSTOM_IMAGE) {
            CustomImagePicker(
                preview = typingAnimationImagePreview,
                onPick = onPickTypingAnimationImage,
                onClear = onClearTypingAnimationImage
            )
        }

        SectionHeader("LED / Neon Lighting")
        Text(
            "An animated light strip along the top of the keyboard.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp)
        )
        LedPatternGrid(selected = ledPattern, palette = keyColorPalette, onSelect = onLedPatternChange)
        if (ledPattern != LedPattern.NONE) {
            LedIdleDimmingRow(enabled = ledIdleDimming, onToggle = onLedIdleDimmingChange)
        }
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
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KeyColorPalette.entries.chunked(3).forEach { rowPalettes ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPalettes.forEach { palette ->
                    val isSelected = palette == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
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
                // Pad out a short last row so tiles keep their 1/3-width
                // size instead of stretching to fill the row.
                repeat(3 - rowPalettes.size) {
                    Box(modifier = Modifier.weight(1f))
                }
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
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KeyEffect.entries.chunked(2).forEach { rowEffects ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowEffects.forEach { effect ->
                    val isSelected = effect == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
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
                if (rowEffects.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
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
        // Static previews for the new effects — the real animated versions
        // (pulse/RGB cycle/ripple) run on the actual keyboard; these cards
        // just need to communicate the *idea* of each style at a glance.
        KeyEffect.RIPPLE -> modifier.drawBehind {
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = size.minDimension * 0.55f,
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            )
        }
        KeyEffect.POP_SCALE -> modifier.border(1.5.dp, accent.copy(alpha = 0.6f), shape)
        KeyEffect.SHADOW_3D -> modifier
            .shadow(elevation = 4.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
            .border(0.75.dp, accent.copy(alpha = 0.4f), shape)
        KeyEffect.NEON_PULSE -> modifier.shadow(elevation = 8.dp, shape = shape, ambientColor = accent, spotColor = accent)
        KeyEffect.RGB_CYCLE -> modifier.border(1.75.dp, accent, shape)
        // Static preview only — the real wave-outward-by-distance animation
        // only makes sense across a full keyboard's worth of keys, not a
        // 3-key card, so this just hints at the neon-ring look with the
        // middle key highlighted as if a wave just passed through it.
        KeyEffect.RGB_RIPPLE -> modifier.border(1.75.dp, accent.copy(alpha = 0.9f), shape)
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

/** Small preview swatch for each [BackgroundStyle], drawn with the same real renderer used on the keyboard. */
@Composable
private fun BackgroundsGrid(selected: BackgroundStyle, onSelect: (BackgroundStyle) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BackgroundStyle.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { style ->
                    val isSelected = style == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(style) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))
                        ) {
                            if (style == BackgroundStyle.NONE) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            } else {
                                com.spmods.sinkey.keyboard.KeyboardBuiltInBackground(
                                    style = style,
                                    isDark = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Text(
                            style.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(6.dp)
                        )
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MaterialYouRow(enabled: Boolean, available: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(16.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Use wallpaper colors", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (available) "Keys pick up your phone's Material You accent."
                else "Needs Android 12 or newer.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = enabled && available, onCheckedChange = onToggle, enabled = available)
    }
}

@Composable
private fun TypingAnimationGrid(selected: TypingAnimation, onSelect: (TypingAnimation) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypingAnimation.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { anim ->
                    val isSelected = anim == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onSelect(anim) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val glyph = when (anim) {
                            TypingAnimation.NONE -> "—"
                            TypingAnimation.CUSTOM_EMOJI -> "😀"
                            TypingAnimation.CUSTOM_IMAGE -> "🖼️"
                            else -> anim.emojiSet.firstOrNull() ?: "—"
                        }
                        Text(glyph, fontSize = 20.sp)
                        Text(
                            anim.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** DIY Animation: lets the user type/paste a single emoji to use as their custom pop-up. */
@Composable
private fun CustomEmojiPicker(emoji: String, onEmojiChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your emoji:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = emoji,
                onValueChange = { new ->
                    // Keep only the last typed character/grapheme-ish chunk so
                    // pasting a whole sentence doesn't leave junk text behind —
                    // this field is meant to hold exactly one emoji.
                    val trimmed = if (new.length > 4) new.takeLast(4) else new
                    onEmojiChange(trimmed)
                },
                textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
                singleLine = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            "Tap and paste or type any single emoji.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** DIY Animation: lets the user pick a cropped image to use as their custom pop-up. */
@Composable
private fun CustomImagePicker(
    preview: android.graphics.Bitmap?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .dashedBorder(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = "Pick image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text("Pick image", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        if (preview != null) {
            Box(modifier = Modifier.size(88.dp)) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Custom typing animation image",
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
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LedPatternGrid(selected: LedPattern, palette: KeyColorPalette, onSelect: (LedPattern) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LedPattern.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { pattern ->
                    val isSelected = pattern == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(Color(0xFF1E1E1E))
                            .clickable { onSelect(pattern) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(vertical = 14.dp)
                        ) {
                            if (pattern != LedPattern.NONE) {
                                com.spmods.sinkey.keyboard.KeyboardLedStrip(
                                    pattern = pattern,
                                    accent = palette.accent,
                                    isIdle = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Text(
                            pattern.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LedIdleDimmingRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(16.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Idle dimming", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Dim the light strip after a few seconds of no typing.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
