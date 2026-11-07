package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import com.spmods.sinkey.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    ThemeOption("Light", "ආලෝකය", Color(0xFFF8F6FC), Color(0xFF0A0D2C), "☀️", ThemeMode.LIGHT),
    ThemeOption("Night", "අඳුර", Color(0xFF0F0C1D), Color(0xFFF2EEFF), "🌙", ThemeMode.DARK),
    ThemeOption("Follow system", "පද්ධතිය", Color(0xFFE4DAFB), Color(0xFF4A1FC6), "⚙️", ThemeMode.SYSTEM)
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
    // Frosted-glass look shared with Home/Settings (HomePalette, GlassKit.kt).
    // No page background here — MainActivity paints the About-Developer
    // background behind the header, this scroll area and the bottom bar.
    val p = rememberGlassPalette()

    // Scrollable: with the Colors/Effects grids now holding more entries
    // than fit on one screen (12 colors, 9 effects), the page needs its own
    // scroll. Manual 2/3-column chunking instead of LazyVerticalGrid — this
    // Column is inside the page's own verticalScroll, and nesting a lazy
    // scrollable grid inside it caused the janky "scrolls in pieces" feel.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SectionHeader(p, "My themes", first = true)
        MyThemesRow(
            p = p,
            preview = customBackgroundPreview,
            onPick = onPickCustomBackground,
            onClear = onClearCustomBackground
        )

        // "Default": Light / Night / Follow System — the same three ThemeMode options.
        SectionHeader(p, "Default")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth()
        ) {
            themeOptions.forEach { option ->
                val selected = option.mode == currentMode
                DefaultThemeCard(
                    p = p,
                    option = option,
                    selected = selected,
                    onClick = { option.mode?.let(onSelect) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionHeader(p, "Colors")
        ColorsGrid(p, selected = keyColorPalette, onSelect = onKeyColorPaletteChange)

        SectionHeader(p, "Effects")
        val effectsIsDark = when (currentMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        EffectsGrid(p, selected = keyEffect, onSelect = onKeyEffectChange, isDark = effectsIsDark)

        SectionHeader(p, "Backgrounds")
        SectionHint(p, "Gradient, rainbow, galaxy, smoke and more — used when no custom photo is set above.")
        BackgroundsGrid(p, selected = backgroundStyle, onSelect = onBackgroundStyleChange)

        SectionHeader(p, "Material You")
        MaterialYouRow(
            p = p,
            enabled = materialYouEnabled,
            available = materialYouAvailable,
            onToggle = onMaterialYouEnabledChange
        )

        SectionHeader(p, "Typing Animation")
        SectionHint(p, "An emoji or icon pops up near a key as you type.")
        TypingAnimationGrid(p, selected = typingAnimation, onSelect = onTypingAnimationChange)
        if (typingAnimation == TypingAnimation.CUSTOM_EMOJI) {
            CustomEmojiPicker(
                p = p,
                emoji = typingAnimationEmoji,
                onEmojiChange = onTypingAnimationEmojiChange
            )
        }
        if (typingAnimation == TypingAnimation.CUSTOM_IMAGE) {
            CustomImagePicker(
                p = p,
                preview = typingAnimationImagePreview,
                onPick = onPickTypingAnimationImage,
                onClear = onClearTypingAnimationImage
            )
        }

        SectionHeader(p, "LED / Neon Lighting")
        SectionHint(p, "An animated light strip along the top of the keyboard.")
        LedPatternGrid(p, selected = ledPattern, palette = keyColorPalette, onSelect = onLedPatternChange)
        if (ledPattern != LedPattern.NONE) {
            LedIdleDimmingRow(p, enabled = ledIdleDimming, onToggle = onLedIdleDimmingChange)
        }
    }
}

// ── Glass helpers ───────────────────────────────────────────────────────────

private val ThemeIndigo = Color(0xFF5B2BEF)

/** Selection-ring colour: deep indigo in light mode, brighter violet in dark mode. */
private fun ringColor(p: HomePalette): Color = if (p.dark) Color(0xFF9B7BFF) else ThemeIndigo

private fun tileFill(p: HomePalette): Brush =
    if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
    else Brush.verticalGradient(listOf(Color.White, Color(0xFFEFE7FF)))

/**
 * Frosted tile: soft shadow (glowing in [accent] when selected) → rounded clip →
 * [fill] → border (solid accent ring when selected, glossy white otherwise).
 */
private fun Modifier.glassTile(
    p: HomePalette,
    selected: Boolean,
    accent: Color,
    fill: Brush,
    radius: Int = 16
): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val glow = if (selected) accent else p.glow
    return this
        .shadow(
            if (selected) 9.dp else 5.dp, shape,
            ambientColor = glow.copy(alpha = 0.45f), spotColor = glow.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(fill)
        .border(
            if (selected) 2.dp else 1.dp,
            if (selected) SolidColor(accent) else if (p.dark) SolidColor(p.cardBorder) else GlossyWhite,
            shape
        )
}

/** Small indigo ✔ badge shown in the top-right corner of the selected tile. */
@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), Color(0xFF4A1FB8))))
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SectionHeader(p: HomePalette, title: String, first: Boolean = false) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = p.title,
        modifier = Modifier.padding(start = 20.dp, top = if (first) 10.dp else 22.dp, bottom = 10.dp)
    )
}

@Composable
private fun SectionHint(p: HomePalette, text: String) {
    Text(
        text,
        fontSize = 12.5.sp,
        color = p.body,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
    )
}

/**
 * "Default" card: a light / night / split-system preview swatch with a bold
 * "Aa", a bottom bar and a small accent dot; the selected one gets the indigo
 * ring + ✔ badge.
 */
@Composable
private fun DefaultThemeCard(
    p: HomePalette,
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .glassTile(p, selected, ringColor(p), SolidColor(option.bg))
            .clickable(onClick = onClick)
    ) {
        // System mode has no single look of its own — half light / half dark.
        if (option.mode == ThemeMode.SYSTEM) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .background(Color(0xFF0F0C1D))
            )
        }
        Text(
            "Aa",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = option.fg,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .fillMaxWidth(0.55f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(option.fg.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(ringColor(p))
        )
        if (selected) SelectedBadge(Modifier.align(Alignment.TopEnd))
    }
}

/**
 * "My themes" row: an "Add Photo" tile (always first, always present so the
 * user can always change/re-pick) followed by the currently-set photo as a
 * selected preview tile with a small remove (X) button, when one is set.
 */
@Composable
private fun MyThemesRow(
    p: HomePalette,
    preview: android.graphics.Bitmap?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Photo tile — dashed indigo border. Tapping it always opens the
        // picker, regardless of whether a photo is already set, so re-picking
        // a different photo doesn't require clearing first.
        Column(
            modifier = Modifier
                .size(108.dp)
                .clip(shape)
                .background(p.card)
                .dashedBorder(ringColor(p).copy(alpha = 0.6f), shape)
                .clickable { onPick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = "Add photo",
                tint = p.iconIndigo,
                modifier = Modifier.size(32.dp)
            )
            Text(
                "Add Photo",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.title,
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
                        .shadow(9.dp, shape, ambientColor = ringColor(p).copy(alpha = 0.45f), spotColor = ringColor(p).copy(alpha = 0.45f))
                        .clip(shape)
                        .border(2.dp, ringColor(p), shape)
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
    val cornerPx = 16.dp.toPx()
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx)
    )
}

@Composable
private fun ColorsGrid(p: HomePalette, selected: KeyColorPalette, onSelect: (KeyColorPalette) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KeyColorPalette.entries.chunked(3).forEach { rowPalettes ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPalettes.forEach { palette ->
                    val isSelected = palette == selected
                    ColorSwatchCard(
                        p = p,
                        accent = palette.accent,
                        selected = isSelected,
                        // "Default" (the first palette) means "no accent
                        // chosen" — showing a green dot on it contradicted
                        // that, since it looked like a real color pick
                        // rather than the neutral/default option. Every
                        // other palette keeps its dot as the swatch cue.
                        showAccentDot = palette != KeyColorPalette.DEFAULT,
                        onClick = { onSelect(palette) },
                        modifier = Modifier.weight(1f)
                    )
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
 * "Aa" preview card: glass tile, bold "Aa" centered, a thin rounded bar
 * standing in for the space bar, and a small accent-colored dot in the
 * top-right corner (the only cue for which accent the card represents).
 * Selected = ring in the accent colour (indigo for the neutral Default).
 */
@Composable
private fun ColorSwatchCard(
    p: HomePalette,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAccentDot: Boolean = true
) {
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .glassTile(p, selected, if (showAccentDot) accent else ringColor(p), tileFill(p))
            .clickable(onClick = onClick)
    ) {
        if (showAccentDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Text(
            "Aa",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = p.title,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .fillMaxWidth(0.55f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(p.sub.copy(alpha = 0.25f))
        )
    }
}

/**
 * Effects section: shows Desh's own static preview images (extracted from
 * its APK — res/drawable/{mech_glow,ripple_theme_preview}_theme_{dark,
 * light}.webp) rather than a live Compose re-render, picked by whichever of
 * the two variants suits the currently-selected app theme (light/dark/system).
 */
@Composable
private fun EffectsGrid(
    p: HomePalette,
    selected: KeyEffect,
    onSelect: (KeyEffect) -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeyEffect.entries.chunked(3).forEach { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
        row.forEach { effect ->
            val isSelected = effect == selected
            val previewRes = when (effect) {
                // Desh has no dedicated "None" preview asset — none_theme_preview_
                // {dark,light} are a static frame captured from Ripple's own
                // asset at a moment its glow has fully faded.
                KeyEffect.NONE -> if (isDark) R.drawable.none_theme_preview_dark else R.drawable.none_theme_preview_light
                KeyEffect.GLOW -> if (isDark) R.drawable.mech_glow_theme_dark else R.drawable.mech_glow_theme_light
                KeyEffect.RIPPLE -> if (isDark) R.drawable.ripple_theme_preview_dark else R.drawable.ripple_theme_preview_light
                // WAVE/CYCLE/STARS: custom-built looping animated webp previews.
                KeyEffect.WAVE -> if (isDark) R.drawable.wave_theme_preview_dark else R.drawable.wave_theme_preview_light
                KeyEffect.CYCLE -> if (isDark) R.drawable.cycle_theme_preview_dark else R.drawable.cycle_theme_preview_light
                KeyEffect.STARS -> if (isDark) R.drawable.stars_theme_preview_dark else R.drawable.stars_theme_preview_light
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.35f)
                    // Matches whichever base look the preview images themselves
                    // show at rest (near-black in dark mode, light gray in light).
                    .glassTile(
                        p, isSelected, ringColor(p),
                        SolidColor(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE3E3E6))
                    )
                    .clickable { onSelect(effect) },
                contentAlignment = Alignment.Center
            ) {
                // GLOW/RIPPLE/WAVE/CYCLE/STARS previews are genuinely animated
                // (looping) webp, so they use AnimatedDrawableResource on
                // API 28+; NONE's asset is a plain static frame.
                if (effect != KeyEffect.NONE && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    com.spmods.sinkey.keyboard.AnimatedDrawableResource(
                        resId = previewRes,
                        contentDescription = effect.label,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(previewRes),
                        contentDescription = effect.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isSelected) SelectedBadge(Modifier.align(Alignment.TopEnd))
            }
        }
        repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
        }
      }
    }
}

/** Small preview swatch for each [BackgroundStyle], drawn with the same real renderer used on the keyboard. */
@Composable
private fun BackgroundsGrid(p: HomePalette, selected: BackgroundStyle, onSelect: (BackgroundStyle) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BackgroundStyle.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { style ->
                    val isSelected = style == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glassTile(p, isSelected, ringColor(p), tileFill(p))
                            .clickable { onSelect(style) }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (style == BackgroundStyle.NONE) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(p.chipBg.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("—", color = p.sub, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = p.title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(6.dp)
                            )
                        }
                        if (isSelected) SelectedBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MaterialYouRow(p: HomePalette, enabled: Boolean, available: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .glassCard(p, radius = 20, elevation = 6)
            .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(p, "🎨", size = 42, fontSize = 20)
        Column(modifier = Modifier.weight(1f)) {
            Text("Use wallpaper colors", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
            Text(
                if (available) "Keys pick up your phone's Material You accent."
                else "Needs Android 12 or newer.",
                fontSize = 12.sp,
                color = p.body,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        GlassSwitch(p, enabled && available, onToggle, enabled = available)
    }
}

@Composable
private fun TypingAnimationGrid(p: HomePalette, selected: TypingAnimation, onSelect: (TypingAnimation) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypingAnimation.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { anim ->
                    val isSelected = anim == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.15f)
                            .glassTile(p, isSelected, ringColor(p), tileFill(p))
                            .clickable { onSelect(anim) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val glyph = when (anim) {
                                TypingAnimation.NONE -> "—"
                                TypingAnimation.CUSTOM_EMOJI -> "😀"
                                TypingAnimation.CUSTOM_IMAGE -> "🖼️"
                                else -> anim.emojiSet.firstOrNull() ?: "—"
                            }
                            Text(glyph, fontSize = 26.sp, color = p.title, fontWeight = FontWeight.Bold)
                            Text(
                                anim.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = p.title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                        if (isSelected) SelectedBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** DIY Animation: lets the user type/paste a single emoji to use as their custom pop-up. */
@Composable
private fun CustomEmojiPicker(p: HomePalette, emoji: String, onEmojiChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .glassCard(p, radius = 20, elevation = 6)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your emoji:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.title)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(p.chipBg),
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
                textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center, color = p.title),
                singleLine = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            "Tap and paste or type any single emoji.",
            fontSize = 11.sp,
            color = p.body
        )
    }
}

/** DIY Animation: lets the user pick a cropped image to use as their custom pop-up. */
@Composable
private fun CustomImagePicker(
    p: HomePalette,
    preview: android.graphics.Bitmap?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .size(88.dp)
                .clip(shape)
                .background(p.card)
                .dashedBorder(ringColor(p).copy(alpha = 0.6f), shape)
                .clickable { onPick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = "Pick image",
                tint = p.iconIndigo,
                modifier = Modifier.size(26.dp)
            )
            Text("Pick image", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = p.title, modifier = Modifier.padding(top = 4.dp))
        }
        if (preview != null) {
            Box(modifier = Modifier.size(88.dp)) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Custom typing animation image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(9.dp, shape, ambientColor = ringColor(p).copy(alpha = 0.45f), spotColor = ringColor(p).copy(alpha = 0.45f))
                        .clip(shape)
                        .border(2.dp, ringColor(p), shape)
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
private fun LedPatternGrid(p: HomePalette, selected: LedPattern, palette: KeyColorPalette, onSelect: (LedPattern) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LedPattern.entries.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { pattern ->
                    val isSelected = pattern == selected
                    // Dark tiles (the strip is a light on a dark keyboard) with the
                    // same ring + ✔ badge as the other grids.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glassTile(p, isSelected, ringColor(p), Brush.verticalGradient(listOf(Color(0xFF15121F), Color(0xFF05040A))))
                            .clickable { onSelect(pattern) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Static illustrative preview only — the real
                                // effect (KeyboardLedRipple) needs live touch
                                // positions on the actual keyboard to animate.
                                // A few small bordered squares stand in for
                                // "the light traces each key's border".
                                when (pattern) {
                                    LedPattern.NONE -> Unit
                                    LedPattern.BREATHING -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        repeat(3) { i ->
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .border(
                                                        1.5.dp,
                                                        palette.accent.copy(alpha = 1f - i * 0.28f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                pattern.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        if (isSelected) SelectedBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
                repeat(2 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LedIdleDimmingRow(p: HomePalette, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .glassCard(p, radius = 20, elevation = 6)
            .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(p, "🌙", size = 42, fontSize = 20)
        Column(modifier = Modifier.weight(1f)) {
            Text("Idle dimming", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
            Text(
                "Dim the light strip after a few seconds of no typing.",
                fontSize = 12.sp,
                color = p.body,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        GlassSwitch(p, enabled, onToggle)
    }
}
