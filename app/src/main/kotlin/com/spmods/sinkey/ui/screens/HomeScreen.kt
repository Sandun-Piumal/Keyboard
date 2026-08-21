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

/**
 * Which action the header's trailing icon performs, driven by which tab is
 * currently showing (see SinKeyApp in MainActivity): a plain Premium badge
 * on Home (no menu — nothing on that screen to reset), or a 3-dot overflow
 * menu with a single "Reset …" action on Themes/Settings.
 */
enum class HeaderMenuMode { PREMIUM, THEME_RESET, SETTINGS_RESET }

/**
 * Shared top bar for all three tabs (Home/Themes/Settings) — kept in this
 * file since Home originally owned this exact look (SinKey/Board title,
 * tagline, trailing action box). MainActivity now hosts a single instance
 * of this above the Scaffold's tab content instead of each screen drawing
 * its own, so it stays visually fixed (non-scrolling) while the tab content
 * scrolls underneath it — see SinKeyApp's Column(header, then Box(content)).
 *
 * The trailing box's icon/behavior swaps with [menuMode]:
 *  - PREMIUM: static WorkspacePremium glyph, no click action (Home tab).
 *  - THEME_RESET / SETTINGS_RESET: a MoreVert (⋮) icon that opens a small
 *    dropdown with one destructive-ish "Reset theme"/"Reset settings" item;
 *    tapping it calls [onResetClick] and closes the menu.
 */
@Composable
fun AppHeader(
    menuMode: HeaderMenuMode = HeaderMenuMode.PREMIUM,
    onResetClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Second word of the title swaps with the tab so the header reads
    // "SinKey Board" on Home, "SinKey Theme" on Themes, "SinKey Settings"
    // on Settings — same first word/color, only the accent word changes.
    val titleSecondWord = when (menuMode) {
        HeaderMenuMode.PREMIUM -> "Board"
        HeaderMenuMode.THEME_RESET -> "Themes"
        HeaderMenuMode.SETTINGS_RESET -> "Settings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp, 18.dp, 20.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Menu,
            contentDescription = "Menu",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(26.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    "SinKey ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TitleIndigo
                )
                Text(
                    titleSecondWord,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PinkAccent
                )
            }
            Text(
                "Type Smart. Type Easy. Type SinKey.",
                fontSize = 12.sp,
                color = BodyGrey,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    // The background/border/rounded-corner "chip" that used
                    // to wrap this icon is removed — now just a plain icon
                    // with an invisible touch target the same size, so tap
                    // area is unchanged but nothing draws around it.
                    .then(
                        if (menuMode != HeaderMenuMode.PREMIUM)
                            Modifier.clickable { menuExpanded = true }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (menuMode == HeaderMenuMode.PREMIUM) Icons.Filled.WorkspacePremium
                    else Icons.Filled.MoreVert,
                    contentDescription = if (menuMode == HeaderMenuMode.PREMIUM) "Premium" else "More options",
                    // Premium keeps its brand indigo. The 3-dot menu (Themes/
                    // Settings) instead uses onBackground so it auto-flips:
                    // black in Light theme, white in Dark theme — matching
                    // the Menu icon on the left rather than a fixed color
                    // that could vanish against either theme's background.
                    tint = if (menuMode == HeaderMenuMode.PREMIUM) IndigoDeep
                        else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
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

// ── Palette ──────────────────────────────────────────────────────────────────
private val IndigoDeep   = Color(0xFF6C4CE0)
private val IndigoMid    = Color(0xFF7C5CF0)
private val PinkAccent   = Color(0xFFE0498A)
private val TitleIndigo  = Color(0xFF3B2F8C)
private val BodyGrey     = Color(0xFF6B7280)
// StepCardBg and FeatureStripBg now use MaterialTheme — see usages below
private val OutlineIndigo = Color(0xFF6C4CE0)
private val OutlinePink  = Color(0xFFE0498A)

private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java)
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeDefault(context: Context): Boolean {
    val defaultIme = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return defaultIme?.startsWith(context.packageName) == true
}

@Composable
fun HomeScreen(isDark: Boolean = isSystemInDarkTheme()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var enabled   by remember { mutableStateOf(isImeEnabled(context)) }
    var isDefault by remember { mutableStateOf(isImeDefault(context)) }

    DisposableEffect(lifecycleOwner) {
        fun refresh() {
            enabled   = isImeEnabled(context)
            isDefault = isImeDefault(context)
        }

        val handler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            false, contentObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
            false, contentObserver
        )

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 24.dp)
    ) {
        // ── Hero card ────────────────────────────────────────────────────────
        // isDark now comes from the caller (MainActivity's real ThemeMode-
        // aware value) instead of isSystemInDarkTheme() here directly — the
        // old local val ignored the user's in-app Light/Dark/System choice
        // and only ever reflected the OS-level setting, so picking "Always
        // dark"/"Always light" in Settings never changed this gradient.
        val heroGradient = Brush.linearGradient(
            colors = if (isDark)
                listOf(Color(0xFF1E1840), Color(0xFF1F1520), Color(0xFF20101A))
            else
                listOf(Color(0xFFE9E4FB), Color(0xFFF3E7F2), Color(0xFFFBE7EE)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )

        Box(
            modifier = Modifier
                .padding(20.dp, 14.dp, 20.dp, 0.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(heroGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ── Left: text + CTA ─────────────────────────────────
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "WELCOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = IndigoDeep
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Type your world",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row {
                            Text("in ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Sinhala ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = IndigoDeep)
                            Text("or ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("English", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PinkAccent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Switch anytime. Type naturally.",
                            fontSize = 13.sp,
                            color = BodyGrey
                        )
                        Spacer(Modifier.height(18.dp))

                        // CTA button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(IndigoDeep, IndigoMid))
                                )
                                .clickable {
                                    context.startActivity(
                                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    )
                                }
                                .padding(horizontal = 22.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Start Typing",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // ── Right: keyboard image + sparkles + dots ───────────
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(180.dp)
                            .offset(y = (-5).dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.keyboard_hero),
                            contentDescription = "Keyboard illustration",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )

                        Text(
                            "✦",
                            fontSize = 10.sp,
                            color = Color(0xFFB8A0EC),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 28.dp, y = 2.dp)
                        )

                        Text(
                            "✦",
                            fontSize = 18.sp,
                            color = Color(0xFFD4C5F9),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-2).dp, y = 28.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB8A0EC))
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 8.dp, y = 42.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB8A0EC))
                        )
                    }
                }
            }
        }

        // ── "Let's get you started" ──────────────────────────────────────────
        Text(
            "Let's get you started",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 10.dp)
        )

        Card(
            modifier = Modifier
                .padding(20.dp, 0.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SetupStepRow(
                    icon = Icons.Filled.Shield,
                    iconBg = Color(0xFFEDE8FC),
                    iconTint = IndigoDeep,
                    title = "1. Enable SinKey",
                    subtitle = "Turn on SinKey in system keyboard settings.",
                    actionLabel = "Enable Now",
                    actionOutline = OutlineIndigo,
                    done = enabled
                ) {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                SetupStepRow(
                    icon = Icons.Filled.Star,
                    iconBg = Color(0xFFFCE8F0),
                    iconTint = PinkAccent,
                    title = "2. Set as Default Keyboard",
                    subtitle = "Choose SinKey as your default keyboard.",
                    actionLabel = "Set as Default",
                    actionOutline = OutlinePink,
                    done = isDefault
                ) {
                    val imm = context.getSystemService(InputMethodManager::class.java)
                    imm.showInputMethodPicker()
                }
            }
        }

        // ── Feature strip ────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Row 1 — Original features ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureItem(
                        glyph = "සිං",
                        glyphColor = IndigoDeep,
                        bgColor = Color(0xFFEDE8FC),
                        title = "Sinhala Typing",
                        subtitle = "Easy & Natural"
                    )
                    FeatureItem(
                        glyph = "A",
                        glyphColor = PinkAccent,
                        bgColor = Color(0xFFFCE8F0),
                        title = "English Typing",
                        subtitle = "Fast & Smart"
                    )
                    FeatureItem(
                        icon = Icons.Filled.Palette,
                        glyphColor = IndigoDeep,
                        bgColor = Color(0xFFEDE8FC),
                        title = "Themes",
                        subtitle = "Style your keyboard"
                    )
                    FeatureItem(
                        icon = Icons.Filled.VerifiedUser,
                        glyphColor = PinkAccent,
                        bgColor = Color(0xFFFCE8F0),
                        title = "Privacy First",
                        subtitle = "100% Secure"
                    )
                }

                // ── Row 2 — New features ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureItem(
                        glyph = "සිA",
                        glyphColor = IndigoDeep,
                        bgColor = Color(0xFFEDE8FC),
                        title = "Mix & Match",
                        subtitle = "Type both freely"
                    )
                    FeatureItem(
                        icon = Icons.Filled.Translate,
                        glyphColor = PinkAccent,
                        bgColor = Color(0xFFFCE8F0),
                        title = "Instant Translate",
                        subtitle = "Translate as you type"
                    )
                    FeatureItem(
                        icon = Icons.Filled.EmojiEmotions,
                        glyphColor = IndigoDeep,
                        bgColor = Color(0xFFEDE8FC),
                        title = "Sticker Studio",
                        subtitle = "Create & send stickers"
                    )
                    FeatureItem(
                        icon = Icons.Filled.TextFields,
                        glyphColor = PinkAccent,
                        bgColor = Color(0xFFFCE8F0),
                        title = "Font Collection",
                        subtitle = "10+ unique styles"
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionOutline: Color,
    done: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 14.dp, 18.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (done) Color(0xFFE3F5EA) else iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF1E8A4C),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = BodyGrey,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        if (done) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE3F5EA))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF1E8A4C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Done",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E8A4C)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, actionOutline.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable { onAction() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = actionOutline
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = actionOutline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    glyph: String? = null,
    icon: ImageVector? = null,
    glyphColor: Color,
    bgColor: Color,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (glyph != null) {
                Text(
                    glyph,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = glyphColor
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            subtitle,
            fontSize = 9.sp,
            color = BodyGrey,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
