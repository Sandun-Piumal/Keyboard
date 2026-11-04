package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R

/*
 * About Developer — built to match the supplied design (852 px wide reference).
 *
 * Every size below is written in "design pixels" of that 852 px wide mock-up and
 * converted with [DevUnit] (screen width / 852), so the page keeps exactly the
 * same proportions on every phone.
 */

// ── Palette: light = sampled from the design, dark = same hues on a deep purple-black ──
private class DevColors(
    val dark: Boolean,
    val bg: Color, val ink: Color, val title: Color,
    val purple: Color, val pillText: Color, val sub: Color, val label: Color,
    val tile: Color, val tileTop: Color, val pill: Color,
    val cardFill: Color, val cardLine: Color, val blobL: Color, val blobR: Color,
    val miniTop: Color, val miniBottom: Color, val miniLine: Color,
    val miniBlob1: Color, val miniBlob2: Color,
    val codeTile: Color, val codeGlyph: Color, val avatarGap: Color,
    val sparkleBig: Color, val sparkleSmall: Color,
    val groupIcon: Color, val megaIcon: Color, val globeIcon: Color, val contactIcon: Color,
    val quote: Color, val divider: Color, val footer: Color, val logoHalo: Color,
    val waveLightTop: Color, val waveLightBottom: Color,
    val waveDeepTop: Color, val waveDeepBottom: Color, val waveSwell: Color,
    val shadow: Color
)

private val LightDev = DevColors(
    dark = false,
    bg = Color(0xFFF8F6FC), ink = Color(0xFF0A0D2C), title = Color(0xFF08071F),
    purple = Color(0xFF6D28F2), pillText = Color(0xFF4A1FC6), sub = Color(0xFF7173AF), label = Color(0xFF5B5C9A),
    tile = Color(0xFFEFE8FD), tileTop = Color(0xFFF2ECFE), pill = Color(0xFFE4DAFB),
    cardFill = Color(0xFFFCFBFE), cardLine = Color(0xFFE8DEFB), blobL = Color(0xFFE6DBFE), blobR = Color(0xFFE4D9FD),
    miniTop = Color(0xFFF4EEFE), miniBottom = Color(0xFFFAF8FE), miniLine = Color(0xFFDCCEFB),
    miniBlob1 = Color(0xFFEBE2FC), miniBlob2 = Color(0xFFF0E8FD),
    codeTile = Color(0xFFEAE1FC), codeGlyph = Color(0xFF6A33E0), avatarGap = Color.White,
    sparkleBig = Color(0xFF6D3CF0), sparkleSmall = Color(0xFF8B62F5),
    groupIcon = Color(0xFF6C2BD9), megaIcon = Color(0xFF742AF8), globeIcon = Color(0xFF7A2FF0), contactIcon = Color(0xFF6D28F2),
    quote = Color(0xFF1B1740), divider = Color(0xFFD9CCFA), footer = Color(0xFF55528D), logoHalo = Color.White,
    waveLightTop = Color(0xFFE2D4FE), waveLightBottom = Color(0xFFDACAFD),
    waveDeepTop = Color(0xFFB684FC), waveDeepBottom = Color(0xFF9450FB), waveSwell = Color(0xFF672DDE),
    shadow = Color(0x226D28F2)
)

private val DarkDev = DevColors(
    dark = true,
    bg = Color(0xFF0F0C1D), ink = Color(0xFFF2EEFF), title = Color(0xFFFFFFFF),
    purple = Color(0xFFA78BFA), pillText = Color(0xFFD6C8FF), sub = Color(0xFFA9A6D6), label = Color(0xFFB9B6E0),
    tile = Color(0xFF251D47), tileTop = Color(0xFF2B2252), pill = Color(0xFF34285F),
    cardFill = Color(0xFF181331), cardLine = Color(0xFF2D2452), blobL = Color(0xFF231A49), blobR = Color(0xFF20174A),
    miniTop = Color(0xFF211A43), miniBottom = Color(0xFF1A1534), miniLine = Color(0xFF3A2F6C),
    miniBlob1 = Color(0xFF2A2056), miniBlob2 = Color(0xFF251C4E),
    codeTile = Color(0xFF34285F), codeGlyph = Color(0xFFC4B0FF), avatarGap = Color(0xFF1A1534),
    sparkleBig = Color(0xFF9B7BFF), sparkleSmall = Color(0xFF7F5FE0),
    groupIcon = Color(0xFFA98BFF), megaIcon = Color(0xFFAE8CFF), globeIcon = Color(0xFFB093FF), contactIcon = Color(0xFFB093FF),
    quote = Color(0xFFE6E1FF), divider = Color(0xFF3A2F6C), footer = Color(0xFF9C99D0), logoHalo = Color(0xFF2A2150),
    waveLightTop = Color(0xFF30246A), waveLightBottom = Color(0xFF281E5C),
    waveDeepTop = Color(0xFF5E38CC), waveDeepBottom = Color(0xFF3F20A3), waveSwell = Color(0xFF2B1580),
    shadow = Color(0x66000000)
)

private val LocalDevColors = compositionLocalOf { LightDev }

/** The design uses a rounded geometric sans. Drop a TTF into res/font and point this at it to match exactly. */
private val DevFont: FontFamily = FontFamily.Default

private const val WEBSITE_URL = "https://www.spmods.download"
private const val YOUTUBE_URL = "https://youtube.com/@datahackerz?si=3ORYJfLTK2LeInBj"
/** TODO: put the real TikTok profile link here. While empty the TikTok tile does nothing. */
private const val TIKTOK_URL = "https://tiktok.com/@sinkeyboard"
private const val TELEGRAM_SANDUN_URL = "https://t.me/SPModsSandun"
private const val TELEGRAM_DHWA_URL = "https://t.me/dhwhatsappultra"
private const val EMAIL_ADDRESS = "sinkeyboardofficial@gmail.com"

// ── Scale ───────────────────────────────────────────────────────────────────
private class DevUnit(val k: Float) {
    fun d(v: Float): Dp = (v * k).dp
    fun sp(v: Float): TextUnit = (v * k).sp
}

private val LocalDevUnit = compositionLocalOf { DevUnit(0.48f) }

private const val REF_WIDTH = 852f

// ── Brand glyph paths (24 x 24 viewBox) ─────────────────────────────────────
private const val TIKTOK_D =
    "M12.525.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93-.01 2.92.01 5.84-.02 8.75-.08 1.4-.54 2.79-1.35 3.94-1.31 1.92-3.58 3.17-5.91 3.21-1.43.08-2.86-.31-4.08-1.03-2.02-1.19-3.44-3.37-3.65-5.71-.02-.5-.03-1-.01-1.49.18-1.9 1.12-3.72 2.58-4.96 1.66-1.44 3.98-2.13 6.15-1.72.02 1.48-.04 2.96-.04 4.44-.99-.32-2.15-.23-3.02.37-.63.41-1.11 1.04-1.36 1.75-.21.51-.15 1.07-.14 1.61.24 1.64 1.82 3.02 3.5 2.87 1.12-.01 2.19-.66 2.77-1.61.19-.33.4-.67.41-1.06.1-1.79.06-3.57.07-5.36.01-4.03-.01-8.05.02-12.07z"

private const val TELEGRAM_D =
    "M16.906 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"

private val tikTokPath: Path by lazy { PathParser().parsePathString(TIKTOK_D).toPath() }
private val telegramPath: Path by lazy { PathParser().parsePathString(TELEGRAM_D).toPath() }

/**
 * "About developer" screen. [onOpenLink] receives the raw URL / mailto — the
 * caller (MainActivity) turns that into an ACTION_VIEW intent. Tapping
 * "Telegram" opens a bottom sheet listing both Telegram channels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDeveloperScreen(
    onOpenLink: (url: String) -> Unit,
    onBack: () -> Unit
) {
    val screenW = LocalConfiguration.current.screenWidthDp
    val unit = DevUnit(screenW / REF_WIDTH)
    // follows the app's own theme (System / Light / Dark setting), not just the OS
    val pal = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkDev else LightDev
    var showTelegramSheet by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDevUnit provides unit, LocalDevColors provides pal) {
        Scaffold(containerColor = pal.bg) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                // Purple wave pinned to the bottom of the screen
                BottomWave(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(unit.d(196f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    HeroSection(onBack = onBack)

                    Spacer(Modifier.height(unit.d(25f)))
                    DevelopersCard()

                    Spacer(Modifier.height(unit.d(36f)))
                    FollowCard(
                        onWebsite = { onOpenLink(WEBSITE_URL) },
                        onTelegram = { showTelegramSheet = true },
                        onYouTube = { onOpenLink(YOUTUBE_URL) },
                        onTikTok = { if (TIKTOK_URL.isNotBlank()) onOpenLink(TIKTOK_URL) }
                    )

                    Spacer(Modifier.height(unit.d(34f)))
                    ContactCard(onEmail = { onOpenLink("mailto:$EMAIL_ADDRESS") })

                    Spacer(Modifier.height(unit.d(33f)))
                    FooterThanks()
                    Spacer(Modifier.height(unit.d(98f)))
                }
            }
        }

        if (showTelegramSheet) {
            TelegramChannelsSheet(
                onDismiss = { showTelegramSheet = false },
                onOpenLink = onOpenLink
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Hero: back bar, logo, "Sinkey", tagline — with the lavender blobs + sparkles
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HeroSection(onBack: () -> Unit) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawHeroDecor(u.k * density, pal) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar (design y 70..146)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(u.d(76f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(u.d(28f)))
            Box(
                modifier = Modifier
                    .size(u.d(46f))
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.ink,
                    modifier = Modifier.size(u.d(46f))
                )
            }
            Spacer(Modifier.width(u.d(31f)))
            DevText("About Developer", 28f, color = pal.ink, weight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(u.d(16f)))
        SinkeyLogo()
        Spacer(Modifier.height(u.d(1f)))
        DevText("Sinkey", 66f, color = pal.title, weight = FontWeight.Bold, lineH = 84f)
        Spacer(Modifier.height(u.d(2f)))

        // Tagline with the two fading rules
        Row(
            modifier = Modifier.height(u.d(40f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaglineRule(fadeLeft = true)
            Spacer(Modifier.width(u.d(20f)))
            Text(
                text = buildAnnotatedString {
                    append("Code  ")
                    withStyle(SpanStyle(color = pal.purple)) { append("\u2022") }
                    append("  Create  ")
                    withStyle(SpanStyle(color = pal.purple)) { append("\u2022") }
                    append("  Improve")
                },
                fontFamily = DevFont,
                fontSize = u.sp(31f),
                lineHeight = u.sp(40f),
                color = pal.sub,
                maxLines = 1
            )
            Spacer(Modifier.width(u.d(21f)))
            TaglineRule(fadeLeft = false)
        }
    }
}

@Composable
private fun TaglineRule(fadeLeft: Boolean) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    val strong = Color(0xFF8F63F6)
    val faint = Color(0xFFB899F9).copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .width(u.d(65f))
            .height(u.d(2f))
            .background(
                Brush.horizontalGradient(
                    if (fadeLeft) listOf(faint, strong) else listOf(strong, faint)
                )
            )
    )
}

/** Circular Sinkey logo: soft glow, white halo, purple ring, dark disc with the logo artwork. */
@Composable
private fun SinkeyLogo() {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Box(
        modifier = Modifier.size(u.d(270f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSizeCompat()) {
            val p = u.k * density
            val c = center
            // glow
            drawCircle(
                brush = Brush.radialGradient(
                    0.74f to Color(0xFF7B3CFF).copy(alpha = 0.34f),
                    1f to Color.Transparent,
                    center = c,
                    radius = 135f * p
                ),
                radius = 135f * p,
                center = c
            )
            drawCircle(pal.logoHalo, radius = 111f * p, center = c)
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF8B55FF), Color(0xFF6430F9)),
                    start = Offset(c.x - 108f * p, c.y - 108f * p),
                    end = Offset(c.x + 108f * p, c.y + 108f * p)
                ),
                radius = 108f * p,
                center = c
            )
            drawCircle(Color(0xFF050421), radius = 101f * p, center = c)
        }
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Sinkey",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(u.d(194f))
                .clip(CircleShape)
        )
    }
}

/** Box.matchParentSize() is only available inside BoxScope; this keeps the Canvas call site simple. */
private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxSize()

private fun DrawScope.drawHeroDecor(p: Float, pal: DevColors) {
    // coordinates are the design's own, minus the 70 px status-bar offset
    fun pt(x: Float, y: Float) = Offset(x * p, (y - 70f) * p)

    // top-left lavender blob
    val left = Path().apply {
        smoothThrough(
            listOf(
                pt(0f, 100f), pt(45f, 118f), pt(100f, 128f), pt(124f, 148f), pt(125f, 170f),
                pt(118f, 195f), pt(98f, 225f), pt(62f, 255f), pt(32f, 270f), pt(0f, 288f)
            ),
            moveFirst = true
        )
        close()
    }
    drawPath(left, pal.blobL)

    // top-right lavender blob
    val right = Path().apply {
        smoothThrough(
            listOf(
                pt(852f, 135f), pt(818f, 145f), pt(793f, 160f), pt(776f, 175f), pt(765f, 190f),
                pt(757f, 205f), pt(752f, 220f), pt(749f, 235f), pt(749f, 250f), pt(752f, 265f),
                pt(757f, 280f), pt(765f, 295f), pt(775f, 310f), pt(782f, 325f), pt(785f, 340f),
                pt(785f, 355f), pt(780f, 370f), pt(803f, 385f), pt(827f, 400f), pt(852f, 412f)
            ),
            moveFirst = true
        )
        close()
    }
    drawPath(right, pal.blobR)

    // sparkles
    sparkle(pt(180f, 258f), 28f * p, pal.sparkleBig)
    sparkle(pt(120f, 306f), 9f * p, pal.sparkleSmall)
    sparkle(pt(716f, 313f), 28f * p, pal.sparkleBig)
    sparkle(pt(671f, 236f), 9f * p, pal.sparkleSmall)
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val i = r * 0.14f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x + i, c.y - i, c.x + r, c.y)
        quadraticBezierTo(c.x + i, c.y + i, c.x, c.y + r)
        quadraticBezierTo(c.x - i, c.y + i, c.x - r, c.y)
        quadraticBezierTo(c.x - i, c.y - i, c.x, c.y - r)
        close()
    }
    drawPath(path, color)
}

/** Catmull-Rom spline through [pts] expressed as cubic Béziers. */
private fun Path.smoothThrough(pts: List<Offset>, moveFirst: Boolean) {
    if (pts.isEmpty()) return
    if (moveFirst) moveTo(pts[0].x, pts[0].y) else lineTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val p0 = pts[maxOf(i - 1, 0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[minOf(i + 2, pts.size - 1)]
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Our Developers
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DevelopersCard() {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    SectionCard(
        decor = { },
        contentPadding = { Modifier.padding(start = u.d(12f), end = u.d(12f), top = u.d(25f), bottom = u.d(7f)) }
    ) {
        // header
        Row(
            modifier = Modifier.padding(start = u.d(18f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(u.d(70f), u.d(66f)), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = pal.groupIcon,
                    modifier = Modifier.requiredSize(u.d(86f))
                )
            }
            Spacer(Modifier.width(u.d(25f)))
            Column {
                DevText("Our Developers", 29f, weight = FontWeight.Bold, lineH = 40f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DevText("Meet the minds behind Sinkey ", 22f, color = pal.sub, lineH = 30f)
                    Text("\u2764\uFE0F", fontSize = u.sp(20f))
                }
            }
        }
        Spacer(Modifier.height(u.d(19f)))
        Row(horizontalArrangement = Arrangement.spacedBy(u.d(15f))) {
            DeveloperMiniCard(
                modifier = Modifier.weight(1f),
                painter = painterResource(id = R.drawable.avatar_developer),
                zoom = 1.08f,
                name = "Sandun Piumal",
                role = "Developer & Founder",
                quote = "\u201CCode today,\nCreate tomorrow.\u201D"
            )
            DeveloperMiniCard(
                modifier = Modifier.weight(1f),
                painter = painterResource(id = R.drawable.avatar_male),
                zoom = 1f,
                name = "Kavishka Sandeepa",
                role = "Co-Developer",
                quote = "\u201CTurning ideas\ninto amazing experiences.\u201D"
            )
        }
    }
}

@Composable
private fun DeveloperMiniCard(
    modifier: Modifier,
    painter: Painter,
    zoom: Float,
    name: String,
    role: String,
    quote: String
) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    val shape = RoundedCornerShape(u.d(34f))
    Box(
        modifier = modifier
            .height(u.d(408f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(pal.miniTop, pal.miniBottom)))
            .border(u.d(2f), pal.miniLine, shape)
            .drawBehind {
                val p = u.k * density
                drawCircle(pal.miniBlob1, radius = 74f * p, center = Offset(-4f * p, size.height - 26f * p))
                drawCircle(pal.miniBlob2, radius = 62f * p, center = Offset(size.width + 8f * p, size.height * 0.5f))
                drawCircle(pal.miniBlob2, radius = 44f * p, center = Offset(-6f * p, size.height * 0.40f))
            }
    ) {
        // "</>" tile, top-left
        Box(
            modifier = Modifier
                .offset(u.d(14f), u.d(13f))
                .size(u.d(62f), u.d(58f))
                .clip(RoundedCornerShape(u.d(16f)))
                .background(pal.codeTile),
            contentAlignment = Alignment.Center
        ) {
            CodeGlyph(u.d(36f), pal.codeGlyph)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(u.d(6f)))
            // avatar + code badge
            Box(Modifier.size(u.d(216f))) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = u.k * density
                    drawCircle(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF9A66FF), Color(0xFF6B2FF0)),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        radius = 108f * p
                    )
                    drawCircle(pal.avatarGap, radius = 103f * p)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(u.d(200f))
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painter,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = zoom, scaleY = zoom)
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(u.d(171f), u.d(154f))
                        .size(u.d(50f))
                        .clip(CircleShape)
                        .background(pal.avatarGap)
                        .padding(u.d(3f))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF7B3CF5), Color(0xFF5A22E0)))),
                    contentAlignment = Alignment.Center
                ) {
                    CodeGlyph(u.d(26f), Color.White)
                }
            }
            Spacer(Modifier.height(u.d(10f)))
            DevText(name, 28f, weight = FontWeight.Bold, lineH = 36f, align = TextAlign.Center)
            Spacer(Modifier.height(u.d(7f)))
            Box(
                modifier = Modifier
                    .height(u.d(38f))
                    .clip(RoundedCornerShape(percent = 50))
                    .background(pal.pill)
                    .padding(horizontal = u.d(17f)),
                contentAlignment = Alignment.Center
            ) {
                DevText(role, 21f, color = pal.pillText, weight = FontWeight.Bold, lineH = 28f)
            }
            Spacer(Modifier.height(u.d(14f)))
            DevText(
                quote, 22f,
                color = pal.quote,
                italic = true,
                lineH = 31f,
                align = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Follow Us
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FollowCard(
    onWebsite: () -> Unit,
    onTelegram: () -> Unit,
    onYouTube: () -> Unit,
    onTikTok: () -> Unit
) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    SectionCard(
        decor = { },
        contentPadding = { Modifier.padding(start = u.d(32f), end = u.d(32f), top = u.d(24f), bottom = u.d(24f)) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(u.d(60f), u.d(56f)), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = pal.megaIcon,
                    modifier = Modifier.requiredSize(u.d(72f))
                )
            }
            Spacer(Modifier.width(u.d(27f)))
            Column {
                DevText("Follow Us", 29f, weight = FontWeight.Bold, lineH = 40f)
                DevText("Stay connected with us for the latest updates!", 20f, color = pal.sub, lineH = 28f)
            }
        }
        Spacer(Modifier.height(u.d(14f)))
        Row(horizontalArrangement = Arrangement.spacedBy(u.d(24f))) {
            FollowTile(Modifier.weight(1f), "Website", onWebsite) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = null,
                    tint = pal.globeIcon,
                    modifier = Modifier.requiredSize(u.d(70f))
                )
            }
            FollowTile(Modifier.weight(1f), "Telegram", onTelegram) { TelegramGlyph(u.d(66f)) }
            FollowTile(Modifier.weight(1f), "YouTube", onYouTube) { YouTubeGlyph(u.d(66f)) }
            FollowTile(Modifier.weight(1f), "TikTok", onTikTok) { TikTokGlyph(u.d(58f)) }
        }
    }
}

@Composable
private fun FollowTile(
    modifier: Modifier,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    val shape = RoundedCornerShape(u.d(28f))
    Column(
        modifier = modifier
            .height(u.d(145f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(pal.tileTop, pal.tile)))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(u.d(20f)))
        Box(Modifier.size(u.d(66f)), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(u.d(14f)))
        DevText(label, 22f, weight = FontWeight.Bold, lineH = 30f, align = TextAlign.Center)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Contact
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ContactCard(onEmail: () -> Unit) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    SectionCard(
        decor = { p ->
            // soft lavender blob bottom-right
            drawCircle(pal.blobR, radius = 82f * p, center = Offset(size.width + 6f * p, size.height + 4f * p))
        },
        contentPadding = { Modifier.padding(start = u.d(30f), end = u.d(30f), top = u.d(26f), bottom = u.d(22f)) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(u.d(58f), u.d(46f))
                    .clip(RoundedCornerShape(u.d(14f)))
                    .background(Brush.linearGradient(listOf(Color(0xFF7B35F5), Color(0xFF6320EF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(u.d(36f)))
            }
            Spacer(Modifier.width(u.d(27f)))
            DevText("Contact", 29f, weight = FontWeight.Bold, lineH = 40f)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(end = u.d(0f))
                    .width(u.d(122f))
                    .height(u.d(2f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF9C6BF6).copy(alpha = 0.1f), Color(0xFF9C6BF6))
                        )
                    )
            )
        }
        DevText(
            "Feel free to reach out for collaboration or just to say hi!",
            19f, color = pal.sub, lineH = 28f
        )
        Spacer(Modifier.height(u.d(18f)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(u.d(7f)))
            // Email
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(u.d(22f)))
                    .clickable { onEmail() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactTile(Icons.Filled.Email)
                Spacer(Modifier.width(u.d(23f)))
                Column {
                    DevText("Email", 20f, color = pal.label, lineH = 28f)
                    DevText(EMAIL_ADDRESS, 20f, weight = FontWeight.Bold, lineH = 32f, maxLines = 1)
                }
            }
            Spacer(Modifier.width(u.d(18f)))
            Box(
                Modifier
                    .width(u.d(2f))
                    .height(u.d(65f))
                    .background(pal.divider)
            )
            Spacer(Modifier.width(u.d(20f)))
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactTile(Icons.Filled.LocationOn)
                Spacer(Modifier.width(u.d(21f)))
                Column {
                    DevText("Location", 20f, color = pal.label, lineH = 28f)
                    DevText("Sri Lanka", 22f, weight = FontWeight.Bold, lineH = 32f, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ContactTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Box(
        modifier = Modifier
            .size(u.d(82f), u.d(80f))
            .clip(RoundedCornerShape(u.d(22f)))
            .background(pal.tile),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = pal.contactIcon, modifier = Modifier.size(u.d(44f)))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Footer + bottom wave
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FooterThanks() {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(u.d(36f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterRule(width = 142f, fadeLeft = true)
            Spacer(Modifier.width(u.d(27f)))
            Text("\uD83D\uDC9C", fontSize = u.sp(30f))
            Spacer(Modifier.width(u.d(27f)))
            FooterRule(width = 141f, fadeLeft = false)
        }
        Spacer(Modifier.height(u.d(13f)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Thanks for using Sinkey!",
                fontFamily = DevFont,
                fontSize = u.sp(24f),
                lineHeight = u.sp(36f),
                letterSpacing = u.sp(3f),
                color = pal.footer,
                maxLines = 1
            )
            Text("  \uD83D\uDE0A", fontSize = u.sp(24f))
        }
    }
}

@Composable
private fun FooterRule(width: Float, fadeLeft: Boolean) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    val strong = Color(0xFF8F63F6)
    val faint = Color(0xFFB899F9).copy(alpha = 0.2f)
    Box(
        modifier = Modifier
            .width(u.d(width))
            .height(u.d(2f))
            .background(Brush.horizontalGradient(if (fadeLeft) listOf(faint, strong) else listOf(strong, faint)))
    )
}

@Composable
private fun BottomWave(modifier: Modifier) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Canvas(modifier) {
        val p = size.width / REF_WIDTH           // design px → real px (wave spans the full width)
        val top = 1650f                           // design y where this canvas starts
        fun pt(x: Float, y: Float) = Offset(x * p, (y - top) * p)
        val bottom = size.height

        val lightEdge = listOf(
            pt(0f, 1689f), pt(43f, 1702f), pt(86f, 1725f), pt(129f, 1753f), pt(172f, 1776f),
            pt(215f, 1788f), pt(258f, 1791f), pt(301f, 1787f), pt(344f, 1780f), pt(387f, 1773f),
            pt(430f, 1769f), pt(473f, 1773f), pt(516f, 1780f), pt(559f, 1786f), pt(602f, 1789f),
            pt(645f, 1783f), pt(688f, 1766f), pt(731f, 1739f), pt(774f, 1707f), pt(817f, 1683f),
            pt(852f, 1670f)
        )
        val deepEdge = listOf(
            pt(0f, 1730f), pt(43f, 1737f), pt(86f, 1755f), pt(129f, 1781f), pt(172f, 1803f),
            pt(215f, 1813f), pt(258f, 1811f), pt(301f, 1800f), pt(344f, 1786f), pt(387f, 1775f),
            pt(430f, 1771f), pt(473f, 1775f), pt(516f, 1787f), pt(559f, 1801f), pt(602f, 1810f),
            pt(645f, 1811f), pt(688f, 1799f), pt(731f, 1775f), pt(774f, 1750f), pt(817f, 1734f),
            pt(852f, 1722f)
        )

        // light lavender wave behind
        val light = Path().apply {
            smoothThrough(lightEdge, moveFirst = true)
            lineTo(size.width, bottom); lineTo(0f, bottom); close()
        }
        drawPath(light, Brush.verticalGradient(listOf(pal.waveLightTop, pal.waveLightBottom)))

        // main purple wave
        val deep = Path().apply {
            smoothThrough(deepEdge, moveFirst = true)
            lineTo(size.width, bottom); lineTo(0f, bottom); close()
        }
        drawPath(
            deep,
            Brush.verticalGradient(
                listOf(pal.waveDeepTop, pal.waveDeepBottom),
                startY = pt(0f, 1730f).y,
                endY = bottom
            )
        )

        // darker swell in the bottom-left corner
        val swell = Path().apply {
            smoothThrough(listOf(pt(0f, 1795f), pt(200f, 1824f), pt(430f, 1846f)), moveFirst = true)
            lineTo(pt(430f, 1846f).x, bottom); lineTo(0f, bottom); close()
        }
        drawPath(
            swell,
            Brush.horizontalGradient(
                listOf(pal.waveSwell, pal.waveSwell.copy(alpha = 0f)),
                startX = 0f,
                endX = 440f * p
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Shared building blocks
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    decor: DrawScope.(Float) -> Unit,
    contentPadding: () -> Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    val shape = RoundedCornerShape(u.d(28f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = u.d(40f))
            .shadow(
                elevation = u.d(8f),
                shape = shape,
                ambientColor = pal.shadow,
                spotColor = pal.shadow
            )
            .clip(shape)
            .background(pal.cardFill)
            .border(u.d(1.5f), pal.cardLine, shape)
            .drawBehind { decor(u.k * density) }
            .then(contentPadding()),
        content = content
    )
}

@Composable
private fun DevText(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalDevColors.current.ink,
    weight: FontWeight = FontWeight.Normal,
    lineH: Float = size * 1.35f,
    align: TextAlign? = null,
    italic: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    val u = LocalDevUnit.current
    val pal = LocalDevColors.current
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = DevFont,
        fontSize = u.sp(size),
        lineHeight = u.sp(lineH),
        fontWeight = weight,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/** The "</>" glyph used on the developer cards. */
@Composable
private fun CodeGlyph(box: Dp, color: Color) {
    Canvas(Modifier.size(box)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.34f, w * 0.28f); lineTo(w * 0.10f, w * 0.50f); lineTo(w * 0.34f, w * 0.72f)
            moveTo(w * 0.66f, w * 0.28f); lineTo(w * 0.90f, w * 0.50f); lineTo(w * 0.66f, w * 0.72f)
            moveTo(w * 0.57f, w * 0.20f); lineTo(w * 0.43f, w * 0.80f)
        }
        drawPath(
            path, color,
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun TelegramGlyph(box: Dp) {
    Canvas(Modifier.size(box)) {
        val s = this.size.width / 13.1f
        withTransform({
            translate(this@Canvas.size.width / 2f, this@Canvas.size.height / 2f)
            scale(s, s, Offset.Zero)
            translate(-11.052f, -12.624f)
        }) {
            drawPath(telegramPath, Color(0xFF2AABEE))
        }
    }
}

@Composable
private fun YouTubeGlyph(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = this.size.width
        val h = w * 0.70f
        val top = (this.size.height - h) / 2f
        drawRoundRect(
            color = Color(0xFFFF0000),
            topLeft = Offset(0f, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(h * 0.30f)
        )
        val tri = Path().apply {
            moveTo(w * 0.40f, top + h * 0.27f)
            lineTo(w * 0.40f, top + h * 0.73f)
            lineTo(w * 0.68f, top + h * 0.50f)
            close()
        }
        drawPath(tri, Color.White)
    }
}

@Composable
private fun TikTokGlyph(box: Dp) {
    Canvas(Modifier.size(box)) {
        val s = this.size.width / 24f
        fun layer(dx: Float, dy: Float, color: Color) {
            withTransform({
                translate(dx * s, dy * s)
                scale(s, s, Offset.Zero)
            }) { drawPath(tikTokPath, color) }
        }
        layer(-0.9f, -0.7f, Color(0xFF25F4EE))
        layer(0.9f, 0.7f, Color(0xFFFE2C55))
        layer(0f, 0f, Color(0xFF0B0B12))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Telegram chooser sheet
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelegramChannelsSheet(
    onDismiss: () -> Unit,
    onOpenLink: (url: String) -> Unit
) {
    val pal = LocalDevColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            Text("Telegram channels", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Choose a channel to open", fontSize = 13.sp, color = pal.sub)
            Spacer(Modifier.height(16.dp))
            TelegramChannelRow(
                title = "SPMods Sandun",
                subtitle = "Apps, mods, and updates",
                onClick = {
                    onOpenLink(TELEGRAM_SANDUN_URL)
                    onDismiss()
                }
            )
            Spacer(Modifier.height(10.dp))
            TelegramChannelRow(
                title = "DH WhatsApp Ultra",
                subtitle = "WhatsApp mod releases",
                onClick = {
                    onOpenLink(TELEGRAM_DHWA_URL)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun TelegramChannelRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val pal = LocalDevColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pal.tile)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDCEEFC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Send, contentDescription = null, tint = Color(0xFF2AA9E0), modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.ink)
            Text(subtitle, fontSize = 12.sp, color = pal.sub)
        }
        Icon(
            Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = pal.sub,
            modifier = Modifier.size(13.dp)
        )
    }
}
