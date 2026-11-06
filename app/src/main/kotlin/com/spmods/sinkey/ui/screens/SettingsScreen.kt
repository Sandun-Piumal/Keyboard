package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.ThemeMode

@Composable
fun SettingsScreen(
    defaultLanguage: String,
    themeMode: ThemeMode,
    mixAutoSinhala: Boolean,
    swipeTypingEnabled: Boolean,
    smoothImeTransition: Boolean = true,
    sinhalaKeyHintsEnabled: Boolean = true,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMixAutoSinhalaChange: (Boolean) -> Unit,
    onSwipeTypingChange: (Boolean) -> Unit,
    onSmoothImeTransitionChange: (Boolean) -> Unit = {},
    onSinhalaKeyHintsChange: (Boolean) -> Unit = {},
    onOpenKeyboardHeight: () -> Unit = {},
    // Navigates to the new Sound & vibration sub-screen — see
    // SoundVibrationScreen.kt. Key sound / Vibrate on tap / Vibration level
    // used to be inline switches right in this screen; they're a dedicated
    // page now (matching the reference screenshots), same pattern as
    // "Keyboard height" below.
    onOpenSoundVibration: () -> Unit = {},
    // Navigates to the Quick text sub-screen — see QuickTextScreen.kt.
    onOpenQuickText: () -> Unit = {},
    // Navigates to the Personal dictionary sub-screen — see
    // PersonalDictionaryScreen.kt.
    onOpenPersonalDictionary: () -> Unit = {},
    // The "ABOUT" section below is a flat set of rows directly in this
    // screen (not a separate "About" sub-page) — each opens its own
    // destination or performs its own action straight from here.
    appVersion: String = "1.0.0",
    onOpenUserGuide: () -> Unit = {},
    onShowTutorial: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAboutDeveloper: () -> Unit = {},
    onRateUs: () -> Unit = {}
) {
    // Same look as Home: frosted glass cards over the lavender page gradient
    // that MainActivity paints behind this tab (this Column has no background
    // of its own). Palette comes from HomeScreen's homePalette().
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { homePalette(dark) }
    val chevron: @Composable () -> Unit = {
        Icon(Icons.Filled.ChevronRight, null, tint = p.iconIndigo, modifier = Modifier.size(26.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp)
    ) {
        SettingsGroup(p) {
            SettingRow(
                p, glyph = "🌐",
                title = "Default language",
                subtitle = when (defaultLanguage) {
                    "si" -> "Sinhala first"
                    "en" -> "English first"
                    else -> "Mix – Sinhala + English"
                },
                modifier = Modifier.clickable {
                    onLanguageChange(
                        when (defaultLanguage) {
                            "mix" -> "en"
                            "en" -> "si"
                            else -> "mix"
                        }
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.chipBg)
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (defaultLanguage) {
                            "si" -> "සිංහල"
                            "en" -> "English"
                            else -> "Mix"
                        },
                        fontSize = 15.sp,
                        color = p.chipText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = p.chipText, modifier = Modifier.size(20.dp))
                }
            }
            SettingRow(
                p, glyph = "🔁",
                title = "Mix mode: auto-convert to Sinhala",
                subtitle = "On space/enter, turn the typed word into Sinhala"
            ) { PurpleSwitch(p, mixAutoSinhala, onMixAutoSinhalaChange) }
            SettingRow(
                p, glyph = "අ",
                title = "Sinhala letter hints",
                subtitle = "Show the matching සිංහල letter on each key"
            ) { PurpleSwitch(p, sinhalaKeyHintsEnabled, onSinhalaKeyHintsChange) }
            SettingRow(
                p, glyph = "👆",
                title = "Swipe to type",
                subtitle = "Drag across letters instead of tapping each one – works for Sinhala & English"
            ) { PurpleSwitch(p, swipeTypingEnabled, onSwipeTypingChange) }
            SettingRow(
                p, glyph = "🔊",
                title = "Sound & vibration",
                subtitle = "Key sound, vibrate, vibration level",
                modifier = Modifier.clickable { onOpenSoundVibration() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "⚡",
                title = "Quick text",
                subtitle = "Type a shortcut like 'gm' and expand it to a full phrase",
                modifier = Modifier.clickable { onOpenQuickText() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "📖",
                title = "Personal dictionary",
                subtitle = "Words you've typed – view, add, or remove them",
                modifier = Modifier.clickable { onOpenPersonalDictionary() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "✨",
                title = "Smooth keyboard transition",
                subtitle = "Gentle fade + slide when the keyboard appears"
            ) { PurpleSwitch(p, smoothImeTransition, onSmoothImeTransitionChange) }
            SettingRow(
                p, glyph = "⌨",
                title = "Keyboard height",
                subtitle = "Adjust height, bottom space & borders",
                modifier = Modifier.clickable { onOpenKeyboardHeight() },
                last = true,
                trailing = chevron
            )
        }

        SectionCard(p, label = "THEME MODE", icon = Icons.Filled.Palette) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeTile(p, "🌓", "Follow system", ThemeMode.SYSTEM, themeMode, onThemeModeChange, Modifier.weight(1f))
                ThemeTile(p, "☀️", "Always light", ThemeMode.LIGHT, themeMode, onThemeModeChange, Modifier.weight(1f))
                ThemeTile(p, "🌙", "Always dark", ThemeMode.DARK, themeMode, onThemeModeChange, Modifier.weight(1f))
            }
        }

        SectionCard(p, label = "ABOUT", icon = Icons.Filled.Info) {
            SettingRow(
                p, glyph = "📘", compact = true,
                title = "User guide",
                subtitle = "How to use SinKey",
                modifier = Modifier.clickable { onOpenUserGuide() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "▶️", compact = true,
                title = "Show tutorial again",
                subtitle = "Replay the getting-started guide",
                modifier = Modifier.clickable { onShowTutorial() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "🛡️", compact = true,
                title = "Privacy policy",
                subtitle = "How your data is handled",
                modifier = Modifier.clickable { onOpenPrivacyPolicy() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "📄", compact = true,
                title = "Terms & conditions",
                subtitle = "Terms of using this app",
                modifier = Modifier.clickable { onOpenTerms() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "👨‍💻", compact = true,
                title = "About developer",
                subtitle = "Who made SinKey, and how to reach out",
                modifier = Modifier.clickable { onOpenAboutDeveloper() },
                trailing = chevron
            )
            SettingRow(
                p, glyph = "⭐", compact = true,
                title = "Rate us",
                subtitle = "Enjoying SinKey? Leave a review",
                modifier = Modifier.clickable { onRateUs() },
                last = true,
                trailing = chevron
            )
        }

        // version pill
        Row(
            modifier = Modifier
                .padding(top = 18.dp)
                .align(Alignment.CenterHorizontally)
                .glassCard(p, radius = 50, elevation = 4)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👑", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text("SinKey v$appVersion", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.title)
        }
    }
}

/** Switch tinted like the screenshot: deep-purple track when on, pale lavender when off. */
@Composable
private fun PurpleSwitch(p: HomePalette, checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF5B2BEF),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = if (p.dark) Color(0xFF2E2560) else Color(0xFFE6DCFB),
            uncheckedBorderColor = if (p.dark) Color(0xFF4A3F85) else Color(0xFFC9B8F3)
        )
    )
}

/** The big frosted card holding the main switch/navigation rows. */
@Composable
private fun SettingsGroup(p: HomePalette, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(p, radius = 28, elevation = 8)
            .padding(vertical = 6.dp)
    ) { content() }
}

/**
 * Card with a dark-gradient label chip (icon + caps title) overlapping its top-left edge —
 * used for THEME MODE and ABOUT.
 */
@Composable
private fun SectionCard(
    p: HomePalette,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Column(
            modifier = Modifier
                .padding(top = 19.dp)
                .fillMaxWidth()
                .glassCard(p, radius = 26, elevation = 6)
        ) {
            Spacer(Modifier.height(26.dp))
            content()
        }
        Row(
            modifier = Modifier
                .offset(x = 12.dp)
                .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFF4A1FB8), spotColor = Color(0xFF4A1FB8))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Color(0xFF4A1FB8), Color(0xFF6A34F0))))
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        }
    }
}

/**
 * One settings row: glossy icon tile · title + subtitle · trailing control
 * (switch / chevron / language pill), with a hairline divider underneath
 * unless [last]. [compact] is the slimmer variant used in the ABOUT card.
 */
@Composable
private fun SettingRow(
    p: HomePalette,
    glyph: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    last: Boolean = false,
    trailing: @Composable () -> Unit
) {
    val tile = if (compact) 38.dp else 46.dp
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = if (compact) 8.dp else 10.dp, bottom = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(tile)
                    .shadow(5.dp, RoundedCornerShape(14.dp), ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                        else Brush.verticalGradient(listOf(Color(0xFFF6F1FF), Color(0xFFE4D8FD)))
                    )
                    .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, fontSize = if (compact) 18.sp else 22.sp, color = p.iconIndigo, fontWeight = FontWeight.ExtraBold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                Text(subtitle, fontSize = 12.sp, color = p.body, lineHeight = 15.sp, modifier = Modifier.padding(top = 1.dp))
            }
            trailing()
        }
        if (!last) {
            HorizontalDivider(
                color = if (p.dark) Color(0xFF3A2F66) else Color(0xFFDCD0F6),
                modifier = Modifier.padding(start = 74.dp, end = 16.dp)
            )
        }
    }
}

/** One of the three THEME MODE tiles; the selected one is deep indigo with a ✔ badge. */
@Composable
private fun ThemeTile(
    p: HomePalette,
    emoji: String,
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = mode == current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(84.dp)
            .shadow(
                if (selected) 8.dp else 4.dp, shape,
                ambientColor = if (selected) Color(0xFF4A1FB8) else p.glow.copy(alpha = 0.35f),
                spotColor = if (selected) Color(0xFF4A1FB8) else p.glow.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFF5A2BD8), Color(0xFF34128F)))
                else SolidColor(if (p.dark) Color(0xFF241C4A) else Color.White)
            )
            .clickable { onSelect(mode) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else p.title,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, null, tint = Color(0xFF4A1FB8), modifier = Modifier.size(14.dp))
            }
        }
    }
}
