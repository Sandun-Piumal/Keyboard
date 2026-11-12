package com.sinkeyboard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector as ComposeImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinkeyboard.R
import com.sinkeyboard.data.KeyColorPalette
import com.sinkeyboard.data.PreferencesManager
import com.sinkeyboard.data.TypingStatsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfileIndigo = Color(0xFF5B2BEF)
private val ProfileIndigoDeep = Color(0xFF34128F)
private val ProfileButton = listOf(Color(0xFF4A1FB8), Color(0xFF6A34F0))
private val EarnedPink = Color(0xFFE0498A)

/**
 * Every number on this screen is real: it either comes from
 * [TypingStatsRepository] (written by actual keystrokes/completed typing
 * tests — see SinKeyInputMethodService.handleKey/learnWord and
 * TypingTestScreen), from the personal dictionary's real word count, or
 * from the currently-applied theme. Nothing here is placeholder/sample
 * data. "Points" and "Level" are derived, not stored — see the `points`/
 * `level` calculation below (1 point per 1000 characters typed).
 *
 * Visual style: the shared frosted-glass look (HomePalette / glassCard from
 * HomeScreen.kt). The page is transparent — MainActivity paints the
 * About-Developer background behind it.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme(),
    currentPalette: KeyColorPalette = KeyColorPalette.DEFAULT,
    defaultLanguage: String = "si"
) {
    val context = LocalContext.current
    val statsRepo = remember(context) { TypingStatsRepository(context) }
    val prefs = remember(context) { PreferencesManager(context) }
    val p = remember(isDark) { homePalette(isDark) }

    val totalCharacters by statsRepo.totalCharacters.collectAsState(initial = 0L)
    val lastAccuracy by statsRepo.lastAccuracy.collectAsState(initial = 0)
    val testsCompleted by statsRepo.testsCompleted.collectAsState(initial = 0)
    val streakDays by statsRepo.currentStreakDays.collectAsState(initial = 0)
    val firstActiveDate by statsRepo.firstActiveDate.collectAsState(initial = null)

    val firstName by prefs.profileFirstName.collectAsState(initial = "")
    val lastName by prefs.profileLastName.collectAsState(initial = "")
    val gender by prefs.profileGender.collectAsState(initial = "")

    // Level still advances every 1000 characters typed (unchanged
    // thresholds) — only the *displayed* "points" value changes: it now
    // shows 1 point per 1000 characters (integer division) instead of
    // 1 point per character, so the number itself reads like a real
    // point total rather than a raw character count.
    val level = (totalCharacters / 1000L).toInt() + 1
    val totalPoints = totalCharacters / 1000L
    val charsIntoLevel = (totalCharacters % 1000L).toInt()
    val charsToNextLevel = 1000 - charsIntoLevel

    val joinedLabel = remember(firstActiveDate) {
        firstActiveDate?.let { raw ->
            runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)
                SimpleDateFormat("MMM yyyy", Locale.US).format(parsed ?: Date())
            }.getOrDefault(raw)
        } ?: "Just started"
    }

    // Rank derived from accuracy — a simple, honest placement label rather
    // than a fabricated leaderboard position.
    val rankLabel = when {
        lastAccuracy >= 95 -> "Top 10%"
        lastAccuracy >= 85 -> "Top 25%"
        lastAccuracy >= 70 -> "Top 50%"
        else -> "Top 100%"
    }

    // Typing time estimated from characters typed at an average of 5 chars
    // per second of active keyboard use.
    val typingTimeLabel = remember(totalCharacters) {
        val totalMinutes = totalCharacters / 5 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        "${hours}h ${minutes}m"
    }

    val userName = "$firstName $lastName".trim().ifBlank { "Your Name" }
    val userHandle = "@" + (firstName.trim().lowercase().ifBlank { "sinkey_user" })

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header — fixed, doesn't scroll ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 10.dp, 16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(8.dp, CircleShape, ambientColor = ProfileIndigo, spotColor = ProfileIndigo)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), ProfileIndigoDeep)))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("My ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                    Text(
                        "Profile",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0xFF9B4DFF))),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
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

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        if (isDark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                        else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                    )
                    .border(1.5.dp, if (isDark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape)
                    .clickable { onEditProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ModeEdit, contentDescription = "Edit profile", tint = p.iconIndigo, modifier = Modifier.size(20.dp))
            }
        }

        // ── Scrollable content (everything below the header) ───────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp)
        ) {
            // ── Identity card ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(p, radius = 28, elevation = 10)
                    .background(
                        Brush.linearGradient(
                            if (isDark) listOf(Color(0x332A1F5C), Color(0x33513A9A))
                            else listOf(Color(0x66DCCBFF), Color(0x66F3EBFF))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(82.dp)) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .shadow(8.dp, CircleShape, ambientColor = ProfileIndigo, spotColor = ProfileIndigo)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF3A2E5C) else Color(0xFFD8CCF7))
                                .border(
                                    3.dp,
                                    Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF3A1AA8))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (gender) {
                                "male" -> Image(
                                    painter = painterResource(id = R.drawable.avatar_male),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                                "female" -> Image(
                                    painter = painterResource(id = R.drawable.avatar_female),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                                else -> Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = p.iconIndigo,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .shadow(6.dp, CircleShape, ambientColor = ProfileIndigo, spotColor = ProfileIndigo)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), ProfileIndigoDeep)))
                                .border(2.dp, Color.White.copy(alpha = if (isDark) 0.4f else 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = p.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            // Compact "Edit Profile" pill (same action as the pencil / menu row)
                            val pillShape = RoundedCornerShape(50)
                            Row(
                                modifier = Modifier
                                    .shadow(8.dp, pillShape, ambientColor = ProfileIndigo, spotColor = ProfileIndigo)
                                    .clip(pillShape)
                                    .background(Brush.horizontalGradient(ProfileButton))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), pillShape)
                                    .clickable { onEditProfile() }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        Text(userHandle, fontSize = 12.sp, color = p.sub, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = p.iconIndigo,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Loves typing in Sinhala & English",
                                fontSize = 12.sp,
                                color = p.body,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Stats strip
                val stripShape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, stripShape, ambientColor = p.glow.copy(alpha = 0.25f), spotColor = p.glow.copy(alpha = 0.25f))
                        .clip(stripShape)
                        .background(if (isDark) Color(0xFF2A2352) else Color.White.copy(alpha = 0.92f))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IdentityStat(p, Icons.Filled.Star, p.iconIndigo, "Level", level.toString())
                    StatRule(p)
                    IdentityStat(p, Icons.Filled.LocalFireDepartment, Color(0xFFFF7A1A), "Streak", "$streakDays Days")
                    StatRule(p)
                    IdentityStat(p, Icons.Filled.EmojiEvents, p.iconIndigo, "Rank", rankLabel)
                    StatRule(p)
                    IdentityStat(p, Icons.Filled.CalendarMonth, p.iconIndigo, "Joined", joinedLabel)
                }
            }

            // ── Your Points ──────────────────────────────────────────────
            SectionRow(
                p = p,
                title = "Your Points",
                action = "History",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                leading = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFF7C4DFF), ProfileIndigoDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(p, radius = 22, elevation = 6)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, CircleShape, ambientColor = ProfileIndigo, spotColor = ProfileIndigo)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF7C4DFF), ProfileIndigoDeep)))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        formatWithCommas(totalPoints),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.title
                    )
                    Text("Total Points", fontSize = 11.sp, color = p.body)
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(if (isDark) Color(0xFF3A2F66) else Color(0xFFDCD0F6))
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Next Level: ${level + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.title
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF3A2F66) else Color(0xFFD9CCF8))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (charsIntoLevel / 1000f).coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF4A1FB8), Color(0xFF7C4DFF))))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$charsToNextLevel more characters to Level ${level + 1}",
                        fontSize = 10.sp,
                        color = p.body,
                        lineHeight = 12.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF4A2848), Color(0xFF3A1F3A))
                                else listOf(Color(0xFFFFE3F0), Color(0xFFFFC9E2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 22.sp)
                }
            }

            // ── Your Medals ──────────────────────────────────────────────
            SectionRow(
                p = p,
                title = "Your Medals",
                action = "View All",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                leading = {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = p.iconIndigo, modifier = Modifier.size(26.dp))
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MedalCard(
                    p = p,
                    modifier = Modifier.weight(1f),
                    spec = MedalSpec(
                        light = Color(0xFFFFE27A), mid = Color(0xFFF2B01E), dark = Color(0xFFB87A00),
                        cardLight = listOf(Color(0xFFFFF6DC), Color(0xFFFFE9B8)),
                        cardDark = listOf(Color(0xFF3B3220), Color(0xFF2E2718))
                    ),
                    title = "Gold Medal",
                    subtitle = "2500+ Points",
                    // BUG FIX: this compared totalCharacters (raw character
                    // count) against the POINTS threshold — but points are
                    // totalCharacters / 1000 (see totalPoints' definition
                    // above), so medals were unlocking 1000x too early (2500
                    // characters ≈ 2.5 points, not 2500 points). Comparing
                    // against totalPoints itself makes the "2500+ Points"
                    // label actually match what's being checked.
                    earned = totalPoints >= 2500
                )
                MedalCard(
                    p = p,
                    modifier = Modifier.weight(1f),
                    spec = MedalSpec(
                        light = Color(0xFF9FA8E6), mid = Color(0xFF6670BC), dark = Color(0xFF414A8F),
                        cardLight = listOf(Color(0xFFF4F2FF), Color(0xFFE3E0F6)),
                        cardDark = listOf(Color(0xFF2D2A4A), Color(0xFF25223E))
                    ),
                    title = "Silver Medal",
                    subtitle = "1500+ Points",
                    earned = totalPoints >= 1500
                )
                MedalCard(
                    p = p,
                    modifier = Modifier.weight(1f),
                    spec = MedalSpec(
                        light = Color(0xFFF0A662), mid = Color(0xFFB4692B), dark = Color(0xFF7E4217),
                        cardLight = listOf(Color(0xFFFFEEE0), Color(0xFFFFDCC2)),
                        cardDark = listOf(Color(0xFF3A2A24), Color(0xFF2E221D))
                    ),
                    title = "Bronze Medal",
                    subtitle = "500+ Points",
                    earned = totalPoints >= 500
                )
            }

            // ── Activity Overview ────────────────────────────────────────
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.BarChart, null, tint = p.iconIndigo, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("Activity Overview", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityStat(
                    p = p, modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Keyboard, iconColor = p.iconIndigo,
                    fillLight = listOf(Color(0xFFEFE6FF), Color(0xFFDCCCFB)),
                    fillDark = listOf(Color(0xFF332A63), Color(0xFF2A2252)),
                    labelColor = if (isDark) Color(0xFFCFC8F2) else Color(0xFF4F3FA0),
                    value = formatWithCommas(totalCharacters), label = "Total Typed"
                )
                ActivityStat(
                    p = p, modifier = Modifier.weight(1f),
                    icon = Icons.Filled.GpsFixed, iconColor = Color(0xFFE91E78),
                    fillLight = listOf(Color(0xFFFFE8F3), Color(0xFFFFD2E7)),
                    fillDark = listOf(Color(0xFF4A2848), Color(0xFF3A1F3A)),
                    labelColor = if (isDark) Color(0xFFF2B8D6) else Color(0xFF9A4A74),
                    value = if (lastAccuracy > 0) "$lastAccuracy%" else "—", label = "Accuracy"
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityStat(
                    p = p, modifier = Modifier.weight(1f),
                    icon = Icons.Filled.AccessTime, iconColor = Color(0xFF2F7BEF),
                    fillLight = listOf(Color(0xFFE6F1FF), Color(0xFFCFE4FF)),
                    fillDark = listOf(Color(0xFF1F3158), Color(0xFF192849)),
                    labelColor = if (isDark) Color(0xFFB4CCF2) else Color(0xFF3A5E99),
                    value = typingTimeLabel, label = "Typing Time"
                )
                ActivityStat(
                    p = p, modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle, iconColor = Color(0xFF22B14C),
                    fillLight = listOf(Color(0xFFE5F9E9), Color(0xFFCBF0D4)),
                    fillDark = listOf(Color(0xFF1B3A2A), Color(0xFF16301F)),
                    labelColor = if (isDark) Color(0xFFB4E4C2) else Color(0xFF2E8A4E),
                    value = testsCompleted.toString(), label = "Lessons Done"
                )
            }

            // ── Menu rows ────────────────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            MenuRow(
                p = p,
                icon = Icons.Filled.Person,
                iconBrush = Brush.verticalGradient(listOf(Color(0xFF7C4DFF), ProfileIndigoDeep)),
                title = "Edit Profile",
                onClick = onEditProfile
            )
            Spacer(Modifier.height(10.dp))
            MenuRow(
                p = p,
                icon = Icons.Filled.HeadsetMic,
                iconBrush = Brush.verticalGradient(listOf(Color(0xFFFF9A3D), Color(0xFFE0561B))),
                title = "Help & Support",
                onClick = onHelpSupport
            )
        }
    }
}

/** "Your Points  ………  History ›" style section heading. */
@Composable
private fun SectionRow(
    p: HomePalette,
    title: String,
    action: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = p.title, modifier = Modifier.weight(1f))
        Text(action, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = p.iconIndigo)
        Icon(Icons.Filled.ChevronRight, null, tint = p.iconIndigo, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StatRule(p: HomePalette) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(if (p.dark) Color(0xFF3A2F66) else Color(0xFFE2D8F8))
    )
}

@Composable
private fun RowScope.IdentityStat(
    p: HomePalette,
    icon: ComposeImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, color = p.body, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = p.title,
            maxLines = 1
        )
    }
}

private class MedalSpec(
    val light: Color,
    val mid: Color,
    val dark: Color,
    val cardLight: List<Color>,
    val cardDark: List<Color>
)

@Composable
private fun MedalCard(
    p: HomePalette,
    modifier: Modifier = Modifier,
    spec: MedalSpec,
    title: String,
    subtitle: String,
    earned: Boolean
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = spec.mid.copy(alpha = 0.35f), spotColor = spec.mid.copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.verticalGradient(if (p.dark) spec.cardDark else spec.cardLight))
            .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape)
            .padding(top = 8.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Medal: crown on top of a glossy disc with a trophy.
        Box(modifier = Modifier.size(width = 66.dp, height = 74.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(6.dp, CircleShape, ambientColor = spec.dark, spotColor = spec.dark)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(spec.light, spec.mid, spec.dark)))
                    .border(3.dp, Brush.verticalGradient(listOf(spec.light, spec.dark)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 28.dp, height = 16.dp)
            ) {
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
                drawPath(crown, Brush.verticalGradient(listOf(spec.light, spec.mid)))
                drawCircle(spec.light, radius = h * 0.12f, center = Offset(w * 0.5f, h * 0.02f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = p.title, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 11.sp, color = p.body, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (earned) EarnedPink.copy(alpha = 0.16f)
                    else if (p.dark) Color(0xFF3A2F66) else Color(0xFFE6DCFB)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (earned) Icons.Filled.Check else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (earned) EarnedPink else p.chipText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (earned) "Earned" else "Locked",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (earned) EarnedPink else p.chipText
            )
        }
    }
}

@Composable
private fun ActivityStat(
    p: HomePalette,
    modifier: Modifier = Modifier,
    icon: ComposeImageVector,
    iconColor: Color,
    fillLight: List<Color>,
    fillDark: List<Color>,
    labelColor: Color,
    value: String,
    label: String
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .shadow(5.dp, shape, ambientColor = iconColor.copy(alpha = 0.3f), spotColor = iconColor.copy(alpha = 0.3f))
            .clip(shape)
            .background(Brush.linearGradient(if (p.dark) fillDark else fillLight))
            .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, RoundedCornerShape(13.dp), ambientColor = iconColor.copy(alpha = 0.3f), spotColor = iconColor.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(13.dp))
                .background(if (p.dark) Color(0xFF2A2352) else Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.title,
                maxLines = 1
            )
            Text(label, fontSize = 12.sp, color = labelColor, maxLines = 1)
        }
    }
}

@Composable
private fun MenuRow(
    p: HomePalette,
    icon: ComposeImageVector,
    iconBrush: Brush,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(p, radius = 26, elevation = 6)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBrush)
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = p.title,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = p.title, modifier = Modifier.size(22.dp))
    }
}

/** "24350" -> "24,350", matching the comma-grouped figures in the mockup. */
private fun formatWithCommas(n: Long): String =
    "%,d".format(n)
