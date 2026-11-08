package com.spmods.sinkey.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.spmods.sinkey.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One tappable row destination the drawer can open. Each maps directly to
 * an existing state setter already used elsewhere in SinKeyApp
 * (settingsSubScreen / showProfile) — the drawer doesn't introduce any
 * new navigation state of its own, only shortcuts into the states that
 * already exist. See SinKeyApp's onDrawerItemClick wiring in
 * MainActivity.kt for what each of these actually does.
 */
enum class DrawerDestination { PROFILE, PERSONAL_DICTIONARY, QUICK_TEXT, ABOUT }

private val DrawerIndigoDeep = Color(0xFF2A0E7A)
private val DrawerIndigo = Color(0xFF4A1FB8)
private val DrawerViolet = Color(0xFF6A34F0)

private const val DRAWER_YOUTUBE_URL = "https://youtube.com/@datahackerz?si=3ORYJfLTK2LeInBj"
private const val DRAWER_TIKTOK_URL = "https://tiktok.com/@sinkeyboard"

/**
 * Side drawer opened from AppHeader's hamburger icon. Home/Themes/Settings
 * stay on the bottom bar; this is for DEEPER links that don't have their own
 * bottom-tab slot: Profile, Personal Dictionary, Quick Text (shown as
 * "Quick Test" in the design but it still opens the Quick text page, as
 * before) and About.
 *
 * Frosted-glass look shared with the rest of the app: deep-indigo wave header
 * with a monogram avatar, glass rows, a tagline over a lavender wave and an
 * indigo footer card with the version + social links.
 */
@Composable
fun SinKeyDrawer(
    userDisplayName: String,
    gender: String = "",
    onDestinationClick: (DrawerDestination) -> Unit,
    onDismiss: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { homePalette(dark) }
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
    }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    ModalDrawerSheet(
        drawerContainerColor = if (dark) Color(0xFF17122F) else Color(0xFFF8F5FF)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // lavender wave behind the lower half (tagline area)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val wave = Path().apply {
                    moveTo(0f, h * 0.70f)
                    cubicTo(w * 0.25f, h * 0.62f, w * 0.55f, h * 0.80f, w, h * 0.66f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    wave,
                    Brush.verticalGradient(
                        if (dark) listOf(Color(0x335B2BEF), Color(0x665B2BEF))
                        else listOf(Color(0x33B79CFF), Color(0x887C4DFF)),
                        startY = h * 0.62f, endY = h
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val body = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w, 0f)
                            lineTo(w, h * 0.80f)
                            cubicTo(w * 0.70f, h * 1.02f, w * 0.35f, h * 0.70f, 0f, h * 0.90f)
                            close()
                        }
                        drawPath(body, Brush.linearGradient(listOf(DrawerIndigoDeep, DrawerIndigo, DrawerViolet), Offset(0f, 0f), Offset(w, h)))
                        // soft light wave inside the band
                        val glow = Path().apply {
                            moveTo(0f, h * 0.12f)
                            cubicTo(w * 0.30f, h * 0.45f, w * 0.60f, -h * 0.05f, w, h * 0.22f)
                            lineTo(w, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        drawPath(glow, Color.White.copy(alpha = 0.07f))
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(96.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .align(Alignment.Center)
                                    .shadow(10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(3.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                when (gender) {
                                    "male" -> Image(
                                        painter = painterResource(id = R.drawable.avatar_male),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                    "female" -> Image(
                                        painter = painterResource(id = R.drawable.avatar_female),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                    else -> Text(
                                        text = userDisplayName.trim().firstOrNull()?.uppercase() ?: "S",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DrawerIndigo
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DrawerIndigo)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (userDisplayName.isNotBlank()) userDisplayName else "Welcome",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 2
                            )
                            Text(
                                text = "SPMods Team",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // ── Destination rows ────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DrawerRow(p, Icons.Filled.Person, "Profile", "View and edit your profile") {
                        onDestinationClick(DrawerDestination.PROFILE); onDismiss()
                    }
                    DrawerRow(p, Icons.Filled.MenuBook, "Personal Dictionary", "Your saved words") {
                        onDestinationClick(DrawerDestination.PERSONAL_DICTIONARY); onDismiss()
                    }
                    DrawerRow(p, Icons.Filled.Bolt, "Quick Text", "Shortcuts that expand as you type") {
                        onDestinationClick(DrawerDestination.QUICK_TEXT); onDismiss()
                    }
                    DrawerRow(p, Icons.Filled.Info, "About", "App information") {
                        onDestinationClick(DrawerDestination.ABOUT); onDismiss()
                    }
                }

                Spacer(Modifier.weight(1f))

                // ── Tagline over the wave ───────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(modifier = Modifier.size(width = 46.dp, height = 30.dp)) {
                        val w = size.width
                        val h = size.height
                        val crown = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, h * 0.30f)
                            lineTo(w * 0.25f, h * 0.60f)
                            lineTo(w * 0.5f, 0f)
                            lineTo(w * 0.75f, h * 0.60f)
                            lineTo(w, h * 0.30f)
                            lineTo(w, h)
                            close()
                        }
                        drawPath(crown, Brush.verticalGradient(listOf(Color(0xFF9B7BFF), DrawerViolet)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Type Smart. Type Easy. Type SinKey.",
                        fontSize = 17.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) Color(0xFFD9CCFF) else Color(0xFF4A1FB8)
                    )
                    Canvas(modifier = Modifier.padding(top = 4.dp).size(width = 120.dp, height = 8.dp)) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.8f)
                            quadraticBezierTo(size.width * 0.5f, 0f, size.width, size.height * 0.5f)
                        }
                        drawPath(path, DrawerIndigo, style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                }

                // ── Footer card: version + follow us ────────────────────
                val footerShape = RoundedCornerShape(26.dp)
                Row(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                        .fillMaxWidth()
                        .shadow(10.dp, footerShape, ambientColor = DrawerIndigo, spotColor = DrawerIndigo)
                        .clip(footerShape)
                        .background(Brush.horizontalGradient(listOf(DrawerIndigoDeep, DrawerViolet)))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), footerShape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFE6DCFF), modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SinKey v$version", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                        Text("SPMods Development", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                    }
                    Box(modifier = Modifier.width(1.dp).height(46.dp).background(Color.White.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Follow us", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SocialTile("♪", Color(0xFF111111)) { open(DRAWER_TIKTOK_URL) }
                            SocialTile("▶", Color(0xFFE62117)) { open(DRAWER_YOUTUBE_URL) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialTile(glyph: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DrawerRow(
    p: HomePalette,
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(p, radius = 24, elevation = 6)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tile = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(5.dp, tile, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                .clip(tile)
                .background(
                    if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                    else Brush.verticalGradient(listOf(Color(0xFFF6F1FF), Color(0xFFE0D3FD)))
                )
                .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, tile),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = p.iconIndigo, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = p.title, maxLines = 1)
            Text(subtitle, fontSize = 13.sp, color = p.body, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = p.title, modifier = Modifier.size(24.dp))
    }
}
