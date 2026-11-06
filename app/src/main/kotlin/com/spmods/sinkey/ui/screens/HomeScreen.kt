package com.spmods.sinkey.ui.screens

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spmods.sinkey.R
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle

/**
 * Which action the header's trailing icon performs, driven by which tab is
 * currently showing (see SinKeyApp in MainActivity): a plain Premium badge
 * on Home (no menu — nothing on that screen to reset), or a 3-dot overflow
 * menu with a single "Reset …" action on Themes/Settings.
 */
enum class HeaderMenuMode { PREMIUM, THEME_RESET, SETTINGS_RESET }

/**
 * Matches ProfileScreen's medal thresholds (500/1500/2500 total points,
 * 1 point per 1000 characters typed — callers pass points, not raw
 * character counts) so the Home header badge always reflects
 * the same tier shown on the Medals card in Profile — NONE falls back to
 * the header's original plain indigo look.
 */
enum class MedalTier { NONE, BRONZE, SILVER, GOLD }

fun medalTierForPoints(totalPoints: Long): MedalTier = when {
    totalPoints >= 2500L -> MedalTier.GOLD
    totalPoints >= 1500L -> MedalTier.SILVER
    totalPoints >= 500L -> MedalTier.BRONZE
    else -> MedalTier.NONE
}


/**
 * Shared top bar for all three tabs (Home/Themes/Settings). Transparent so the
 * page behind it shows through (Home paints a lavender gradient there —
 * MainActivity). Layout: menu · "SinKey <Board|Themes|Settings>" with tagline ·
 * trailing control.
 *
 * Trailing control by [menuMode]:
 *  - PREMIUM (Home): round glossy profile button → [onProfileClick]. Its fill
 *    follows [medalTier] (Gold/Silver/Bronze) once a medal is earned.
 *  - THEME_RESET / SETTINGS_RESET: ⋮ menu with one "Reset …" item.
 */
@Composable
fun AppHeader(
    menuMode: HeaderMenuMode = HeaderMenuMode.PREMIUM,
    medalTier: MedalTier = MedalTier.NONE,
    onResetClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { homePalette(dark) }

    val titleSecondWord = when (menuMode) {
        HeaderMenuMode.PREMIUM -> "Board"
        HeaderMenuMode.THEME_RESET -> "Themes"
        HeaderMenuMode.SETTINGS_RESET -> "Settings"
    }
    val accent = Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0xFF9B4DFF)))

    val medalBg: Color? = when (medalTier) {
        MedalTier.GOLD -> Color(0xFFF4B400)
        MedalTier.SILVER -> Color(0xFFB0B7C3)
        MedalTier.BRONZE -> Color(0xFFB4692B)
        MedalTier.NONE -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 14.dp, 16.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Menu,
            contentDescription = "Menu",
            tint = p.title,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { onMenuClick() }
                .padding(2.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Text("SinKey ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                    Text(
                        titleSecondWord,
                        style = TextStyle(brush = accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    )
                }
                Text(
                    "Type Smart. Type Easy. Type SinKey.",
                    fontSize = 11.sp,
                    color = p.sub,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        Box {
            if (menuMode == HeaderMenuMode.PREMIUM) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(
                            if (medalBg != null) SolidColor(medalBg)
                            else if (dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                            else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                        )
                        .border(1.5.dp, if (dark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = if (medalBg != null) Color.White else p.iconIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = p.title,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                val label = if (menuMode == HeaderMenuMode.THEME_RESET) "Reset theme" else "Reset settings"
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onResetClick()
                    }
                )
            }
        }
    }
}

// ── Home look: palette ───────────────────────────────────────────────────────
// Everything on the Home tab (header, cards, bottom bar) is styled from this
// one palette so light/dark stay consistent. Light = lavender glass look.
private val IndigoDeep   = Color(0xFF6C4CE0)
private val IndigoMid    = Color(0xFF7C5CF0)
private val PinkAccent   = Color(0xFFE0498A)
private val BodyGrey     = Color(0xFF6B7280)

@Immutable
internal class HomePalette(
    val dark: Boolean,
    val title: Color,        // headings
    val body: Color,         // descriptions
    val sub: Color,          // small captions
    val card: Color,         // translucent "glass" card fill
    val cardBorder: Color,
    val heroGradient: List<Color>,
    val testGradient: List<Color>,
    val chipBg: Color,
    val chipText: Color,
    val blue: Color,         // "Sinhala" word
    val purple: Color,       // "English" word
    val iconIndigo: Color,
    val iconRed: Color,
    val glow: Color          // shadow tint
)

internal fun homePalette(dark: Boolean) = if (dark) HomePalette(
    dark = true,
    title = Color(0xFFF2EEFF), body = Color(0xFFCFC8F2), sub = Color(0xFFA9A4D0),
    card = Color(0xE61E1840), cardBorder = Color(0xFF3A2F66),
    heroGradient = listOf(Color(0xFF2A1F5C), Color(0xFF2B1B52), Color(0xFF1F1648)),
    testGradient = listOf(Color(0xFF2A2060), Color(0xFF2E1D56), Color(0xFF211850)),
    chipBg = Color(0xFF3A2F66), chipText = Color(0xFFD7C8FF),
    blue = Color(0xFF8FA0FF), purple = Color(0xFFC09BFF),
    iconIndigo = Color(0xFFB093FF), iconRed = Color(0xFFFF7B7E),
    glow = Color(0xFF000000)
) else HomePalette(
    dark = false,
    title = Color(0xFF1B1464), body = Color(0xFF3A3566), sub = Color(0xFF5B5780),
    card = Color.White.copy(alpha = 0.78f), cardBorder = Color.White,
    heroGradient = listOf(Color(0xFFE7DBFF), Color(0xFFF3E4FB), Color(0xFFDCCBFF)),
    testGradient = listOf(Color(0xFFE2D4FF), Color(0xFFF1DEFC), Color(0xFFD8C6FF)),
    chipBg = Color(0xFFDCCEFB), chipText = Color(0xFF5A32D0),
    blue = Color(0xFF2F45E6), purple = Color(0xFF6B2FD9),
    iconIndigo = Color(0xFF5B3FE0), iconRed = Color(0xFFE5383B),
    glow = Color(0xFF7B4DF5)
)

/** Page background used behind the whole Home tab (also behind header + bottom bar). */
internal fun homeBackgroundBrush(dark: Boolean): Brush = Brush.verticalGradient(homeBackgroundColors(dark))

private fun homeBackgroundColors(dark: Boolean): List<Color> =
    if (dark) listOf(Color(0xFF0F0C1D), Color(0xFF1B1438), Color(0xFF140F2B))
    else listOf(Color(0xFFE6D9FD), Color(0xFFF1E9FF), Color(0xFFE0D0FC))

/** Status-bar colour that continues the gradient's top edge, so the bar blends into the page. */
internal fun homeStatusBarColor(dark: Boolean): Color = homeBackgroundColors(dark).first()

private val ButtonGradient = listOf(Color(0xFF4A1FB8), Color(0xFF6A34F0))
private val GlossyWhite = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.25f)))

@Composable
fun HomeScreen(
    isDark: Boolean = isSystemInDarkTheme(),
    onStartTest: () -> Unit = {},
    onViewProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val ime = rememberImeStatus()
    val enabled = ime.enabled
    val isDefault = ime.isDefault
    val p = remember(isDark) { homePalette(isDark) }

    // No solid background here: MainActivity paints homeBackgroundBrush behind
    // the header, this scroll area and the bottom bar so it reads as one page.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 18.dp)
    ) {
        HeroCard(p, onViewProfile)

        // ── "Let's get you started" ──────────────────────────────────────────
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🚀", fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Let's get you started",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = p.title
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(52.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0x005B2BEF))))
                )
            }
        }

        SetupStepRow(
            p = p,
            icon = Icons.Filled.Shield,
            iconTint = p.iconIndigo,
            title = "1. Enable SinKey",
            subtitle = "Turn on SinKey in system keyboard settings.",
            actionLabel = "Enable Now",
            done = enabled
        ) { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }

        Spacer(Modifier.height(8.dp))

        SetupStepRow(
            p = p,
            icon = Icons.Filled.Star,
            iconTint = PinkAccent,
            title = "2. Set as Default Keyboard",
            subtitle = "Choose SinKey as your default keyboard.",
            actionLabel = "Set as Default",
            done = isDefault
        ) {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm.showInputMethodPicker()
        }

        TestTypingCard(p, onStartTest)
        FeatureGrid(p)
    }
}

/** Frosted "glass" container shared by the step rows and the feature grid. */
private fun Modifier.glassCard(p: HomePalette, radius: Int = 22, elevation: Int = 6): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .shadow(elevation.dp, shape, ambientColor = p.glow.copy(alpha = 0.35f), spotColor = p.glow.copy(alpha = 0.35f))
        .clip(shape)
        .background(p.card)
        .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape)
}

/** Dark-indigo gradient pill ("View Profile", "Start Test"). */
@Composable
private fun PillButton(label: String, textSize: Int, hPad: Int, vPad: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .shadow(8.dp, shape, ambientColor = Color(0xFF4A1FB8), spotColor = Color(0xFF4A1FB8))
            .clip(shape)
            .background(Brush.horizontalGradient(ButtonGradient))
            .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = hPad.dp, vertical = vPad.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = textSize.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size((textSize + 2).dp))
    }
}

@Composable
private fun HeroCard(p: HomePalette, onViewProfile: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = p.heroGradient,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.5.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // WELCOME 👋 chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.chipBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WELCOME", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = p.chipText)
                    Spacer(Modifier.width(5.dp))
                    Text("👋", fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text("Type your world", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                Row {
                    Text("in ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                    Text("Sinhala ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.blue)
                    Text("or ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                    Text("English", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.purple)
                }
                Spacer(Modifier.height(8.dp))
                Text("Switch anytime. Type naturally.", fontSize = 13.sp, color = p.body)
                Spacer(Modifier.height(16.dp))
                PillButton("View Profile", textSize = 14, hPad = 22, vPad = 12, onClick = onViewProfile)
            }

            Image(
                painter = painterResource(id = R.drawable.keyboard_hero),
                contentDescription = "Keyboard illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(170.dp).height(150.dp)
            )
        }
    }
}

@Composable
private fun TestTypingCard(p: HomePalette, onStartTest: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val dot = if (p.dark) Color(0xFF7B5FD0) else Color(0xFF5B2BEF)
    Box(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = p.glow.copy(alpha = 0.35f), spotColor = p.glow.copy(alpha = 0.35f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = p.testGradient,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.5.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape)
    ) {
        // decorative dots, top-right
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-70).dp, y = 10.dp).size(8.dp).clip(CircleShape).background(dot))
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-36).dp, y = 20.dp).size(8.dp).clip(CircleShape).background(dot))
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = 32.dp).size(4.dp).clip(CircleShape).background(dot.copy(alpha = 0.7f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 16.dp, bottom = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.test_typing_stopwatch),
                contentDescription = "Test typing illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(ButtonGradient))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("TEST YOUR TYPING", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text("Test Typing", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Check your typing speed and improve your skills with a quick test.",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = p.body
                )
            }
            Spacer(Modifier.width(8.dp))
            PillButton("Start Test", textSize = 12, hPad = 14, vPad = 11, onClick = onStartTest)
        }
    }
}

@Composable
private fun FeatureGrid(p: HomePalette) {
    val divider = if (p.dark) Color(0xFF3A2F66) else Color(0xFFD9CCF7)
    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .glassCard(p, radius = 28, elevation = 8)
            .padding(vertical = 8.dp)
    ) {
        FeatureRow(
            p, divider,
            FeatureSpec("සිං", null, p.iconIndigo, "Sinhala Typing", "Easy & Natural"),
            FeatureSpec("A", null, p.iconRed, "English Typing", "Fast & Smart"),
            FeatureSpec(null, Icons.Filled.Palette, p.iconIndigo, "Themes", "Style your keyboard"),
            FeatureSpec(null, Icons.Filled.VerifiedUser, p.iconRed, "Privacy First", "100% Secure")
        )
        HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 18.dp))
        FeatureRow(
            p, divider,
            FeatureSpec("සිA", null, p.iconIndigo, "Mix & Match", "Type both freely"),
            FeatureSpec(null, Icons.Filled.Translate, p.iconRed, "Instant Translate", "Translate as you type"),
            FeatureSpec(null, Icons.Filled.EmojiEmotions, p.iconIndigo, "Sticker Studio", "Create & Send"),
            FeatureSpec("Tт", null, p.iconRed, "Font Collection", "10+ unique styles")
        )
    }
}

private class FeatureSpec(
    val glyph: String?,
    val icon: ImageVector?,
    val tint: Color,
    val title: String,
    val subtitle: String
)

@Composable
private fun FeatureRow(p: HomePalette, divider: Color, vararg items: FeatureSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 10.dp)
    ) {
        items.forEachIndexed { i, f ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .background(divider)
                )
            }
            FeatureItem(p, f, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureItem(p: HomePalette, f: FeatureSpec, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 2.dp)) {
        // glossy white tile with a soft purple glow
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(6.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                    else Brush.verticalGradient(listOf(Color.White, Color(0xFFEFE7FF)))
                )
                .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (f.icon != null) {
                Icon(f.icon, null, tint = f.tint, modifier = Modifier.size(24.dp))
            } else if (f.glyph != null) {
                Text(f.glyph, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = f.tint)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            f.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = p.title,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            f.subtitle,
            fontSize = 9.5.sp,
            color = p.sub,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

/**
 * One "Let's get you started" row — its own glass card. Done = glossy green
 * circle + green "Done" pill; otherwise a tinted icon circle + outlined action.
 */
@Composable
private fun SetupStepRow(
    p: HomePalette,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    done: Boolean,
    onAction: () -> Unit
) {
    val doneFg = if (p.dark) Color(0xFF4ADE80) else Color(0xFF1B7A44)
    val doneBg = if (p.dark) Color(0xFF1E8A4C).copy(alpha = 0.25f) else Color(0xFFCDF2D9)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(p)
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(if (done) 4.dp else 0.dp, CircleShape, ambientColor = Color(0xFF3CC26B), spotColor = Color(0xFF3CC26B))
                .clip(CircleShape)
                .background(
                    if (done) Brush.verticalGradient(listOf(Color(0xFF8DEBA8), Color(0xFF45C773)))
                    else SolidColor(iconTint.copy(alpha = if (p.dark) 0.22f else 0.12f))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (done) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = if (done) Color(0xFF0F5C2E) else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
            Text(subtitle, fontSize = 11.5.sp, color = p.body, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Spacer(Modifier.width(8.dp))

        if (done) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(doneBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, null, tint = doneFg, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Done", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = doneFg)
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, iconTint.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .clickable { onAction() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconTint, maxLines = 1)
                Icon(Icons.Filled.ChevronRight, null, tint = iconTint, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Floating pill bottom bar (Home / Themes / Settings). The selected tab is a
 * deep-indigo rounded block; the others sit on the frosted pill with thin
 * dividers between them. Replaces the stock Material NavigationBar.
 */
@Composable
fun SinKeyBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { homePalette(dark) }
    val shape = RoundedCornerShape(34.dp)
    val items = listOf(
        Triple("Home", Icons.Filled.Home, 0),
        Triple("Themes", Icons.Filled.Palette, 1),
        Triple("Settings", Icons.Filled.Settings, 2)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .glassCard(p, radius = 34, elevation = 10)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon, index) ->
                if (i > 0) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(if (dark) Color(0xFF3A2F66) else Color(0xFFD3C5F5))
                    )
                }
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (selected) Modifier
                                .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFF4A1FB8), spotColor = Color(0xFF4A1FB8))
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF5A2BD8), Color(0xFF34128F))))
                            else Modifier.clip(RoundedCornerShape(28.dp))
                        )
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else p.iconIndigo,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (selected) Color.White else p.title
                    )
                }
            }
        }
    }
}
