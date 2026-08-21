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
        // The old in-page "APPEARANCE / Choose a theme / ..." header text
        // block used to live here — it's now redundant since AppHeader
        // (added in MainActivity, fixed above this scrolling content) shows
        // the page title/branding instead. Left with just a small gap so
        // the first section isn't flush against the header.
        Spacer(modifier = Modifier.height(10.dp))

        // Manual 2-column chunking instead of LazyVerticalGrid — this Column
        // is now inside the page's own verticalScroll, and nesting a lazy
        // scrollable grid inside a scrolling Column caused the janky
        // "scrolls in pieces" feel (competing nested-scroll containers).
        // A plain Column of Rows has no scroll behavior of its own, so the
        // whole page scrolls as a single smooth list.

        SectionHeader("My themes")
        MyThemesRow(
            preview = customBackgroundPreview,
            onPick = onPickCustomBackground,
            onClear = onClearCustomBackground
        )

        // "Default": Cream Light / Night / Follow System — same three
        // ThemeMode options as before, restyled to match Desh's compact
        // "Default" row (a checkmark on the selected card's split light/
        // dark preview, rather than the old big single-color hero + emoji
        // + two-line label layout). Moved to right after "My themes" (Desh
        // has no separate hero section above it — Default comes first).
        SectionHeader("Default")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
        ) {
            themeOptions.forEach { option ->
                val selected = option.mode == currentMode
                DefaultThemeCard(
                    option = option,
                    selected = selected,
                    onClick = { option.mode?.let(onSelect) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionHeader("Colors")
        ColorsGrid(selected = keyColorPalette, onSelect = onKeyColorPaletteChange)

        SectionHeader("Effects")
        val effectsIsDark = when (currentMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        EffectsGrid(selected = keyEffect, palette = keyColorPalette, onSelect = onKeyEffectChange, isDark = effectsIsDark)

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
 * "Default" section card matching Desh's exact style: a light/dark split
 * preview swatch (using [option]'s own bg color as one half and a fixed
 * complementary shade as the other, standing in for "this mode next to its
 * opposite"), a bold "Aa", a checkmark overlay when selected, a bottom bar,
 * and a small accent dot — same visual language as ColorSwatchCard below,
 * since Desh's Default and Colors cards share one style.
 */
@Composable
private fun DefaultThemeCard(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .background(option.bg)
    ) {
        // System mode has no single "photo" of its own — Desh represents
        // it as half light / half dark, so this card gets that same split
        // treatment; Light/Dark modes stay a single flat fill (their .bg).
        if (option.mode == ThemeMode.SYSTEM) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .background(Color(0xFF15130F))
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        )
        Text(
            "Aa",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = option.fg,
            modifier = Modifier.align(Alignment.Center)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(option.bg.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = option.fg,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
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
                    ColorSwatchCard(
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
 * "Aa" preview card matching Desh's Colors section: a card-colored surface,
 * bold "Aa" centered, a thin rounded bar standing in for the space bar
 * along the bottom, and a small accent-colored dot in the top-right corner
 * — that dot is the only cue for which accent this card represents (Desh
 * doesn't tint the "Aa" text itself), plus a colored ring when selected.
 */
@Composable
private fun ColorSwatchCard(
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAccentDot: Boolean = true
) {
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .fillMaxWidth(0.55f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

/**
 * Effects section: shows Desh's own static preview images (extracted from
 * its APK — res/drawable/{mech_glow,ripple_theme_preview}_theme_{dark,
 * light}.webp) rather than a live Compose re-render. Desh itself uses
 * fixed screenshot-like images for these cards, not a live animation
 * preview, so matching that means matching the actual images, picked by
 * whichever of the two variants suits the currently-selected app theme
 * (light/dark/system).
 */
@Composable
private fun EffectsGrid(
    selected: KeyEffect,
    palette: KeyColorPalette,
    onSelect: (KeyEffect) -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
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
                // Desh has no dedicated "None" preview asset (there's
                // nothing to animate for "no effect") — none_theme_preview_
                // {dark,light} are a static frame captured from Ripple's
                // own asset at a moment its glow has fully faded, giving a
                // clean plain-keyboard shot in the exact same visual style
                // (crop, key look, lighting) as Desh's real Ripple/Glow
                // images, rather than a separately hand-built Compose mock.
                KeyEffect.NONE -> if (isDark) R.drawable.none_theme_preview_dark else R.drawable.none_theme_preview_light
                KeyEffect.GLOW -> if (isDark) R.drawable.mech_glow_theme_dark else R.drawable.mech_glow_theme_light
                KeyEffect.RIPPLE -> if (isDark) R.drawable.ripple_theme_preview_dark else R.drawable.ripple_theme_preview_light
                // WAVE/CYCLE/STARS moved here from the old "LED / Neon
                // Lighting" section — same underlying animation
                // (KeyboardKeyEffectRipple), just now selected from this
                // grid instead of a separate LedPattern preference. Their
                // preview assets are custom-built (not extracted from
                // Desh's APK, since Desh has no such effects) as genuinely
                // looping animated webp — generated by rendering this same
                // key-border-glow math frame-by-frame over the None card's
                // own base screenshot, matching how mech_glow/ripple's own
                // assets are real multi-frame animations rather than a
                // single still.
                KeyEffect.WAVE -> if (isDark) R.drawable.wave_theme_preview_dark else R.drawable.wave_theme_preview_light
                KeyEffect.CYCLE -> if (isDark) R.drawable.cycle_theme_preview_dark else R.drawable.cycle_theme_preview_light
                KeyEffect.STARS -> if (isDark) R.drawable.stars_theme_preview_dark else R.drawable.stars_theme_preview_light
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.35f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    // Matches whichever base look the Ripple/Glow images
                    // themselves show at rest (near-black in dark mode,
                    // light gray in light mode) — previously a fixed dark
                    // color regardless of isDark, so the None card looked
                    // inconsistent with the theme the other two cards were
                    // actually showing.
                    .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE3E3E6))
                    .clickable { onSelect(effect) },
                contentAlignment = Alignment.Center
            ) {
                if (previewRes != null) {
                    // GLOW/RIPPLE and the new WAVE/CYCLE/STARS previews are
                    // all genuinely animated (looping) webp assets now — see
                    // previewRes selection above — so they all use the
                    // AnimatedDrawableResource path. Only NONE's asset is a
                    // plain static frame, so it always uses the
                    // non-animated Image() path, same as the <API 28
                    // fallback below.
                    if (effect != KeyEffect.NONE && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        com.spmods.sinkey.keyboard.AnimatedDrawableResource(
                            resId = previewRes,
                            contentDescription = effect.label,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(previewRes),
                            contentDescription = effect.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }
        }
        }
      }
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Static illustrative preview only — the real
                            // effect (KeyboardLedRipple) needs live touch
                            // positions on the actual keyboard to animate,
                            // which doesn't exist in this small settings
                            // swatch. A few small bordered squares stand in
                            // for "the light traces each key's border",
                            // colored/spaced to hint at what each pattern
                            // looks like without faking the animation.
                            when (pattern) {
                                LedPattern.NONE -> Unit
                                LedPattern.BREATHING -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { i ->
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
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
