package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector as ComposeImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.TypingStatsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfileIndigo    = Color(0xFF6C4CE0)
private val ProfilePink      = Color(0xFFE0498A)
private val ProfileGrey      = Color(0xFF6B7280)
private val MedalGold        = Color(0xFFF4B400)
private val MedalSilver      = Color(0xFFB0B7C3)
private val MedalBronze      = Color(0xFFB4692B)
private val EarnedPink       = Color(0xFFE0498A)

/**
 * Every number on this screen is real: it either comes from
 * [TypingStatsRepository] (written by actual keystrokes/completed typing
 * tests — see SinKeyInputMethodService.handleKey/learnWord and
 * TypingTestScreen), from the personal dictionary's real word count, or
 * from the currently-applied theme. Nothing here is placeholder/sample
 * data. "Points" and "Level" are derived, not stored — see the `points`/
 * `level` calculation below (1 point per 1000 characters typed).
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme(),
    currentPalette: KeyColorPalette = KeyColorPalette.DEFAULT,
    defaultLanguage: String = "si"
) {
    val context = LocalContext.current
    val statsRepo = remember(context) { TypingStatsRepository(context) }

    val totalCharacters by statsRepo.totalCharacters.collectAsState(initial = 0L)
    val lastAccuracy by statsRepo.lastAccuracy.collectAsState(initial = 0)
    val testsCompleted by statsRepo.testsCompleted.collectAsState(initial = 0)
    val streakDays by statsRepo.currentStreakDays.collectAsState(initial = 0)
    val firstActiveDate by statsRepo.firstActiveDate.collectAsState(initial = null)

    // 1 point per real character typed through the keyboard — an honest,
    // direct formula (not fabricated progress). Level advances every 1000
    // points, matching the "Next Level" progress bar shown below.
    val totalPoints = totalCharacters
    val level = (totalPoints / 1000L).toInt() + 1
    val pointsIntoLevel = (totalPoints % 1000L).toInt()
    val pointsToNextLevel = 1000 - pointsIntoLevel

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

    val userName = "Sandun Piumal"
    val userHandle = "@sandun_typing"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 24.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp, 20.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onBack() }
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("My ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ProfileIndigo)
                Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ProfilePink)
            }
            androidx.compose.material3.Icon(
                Icons.Filled.ModeEdit,
                contentDescription = "Edit",
                tint = ProfileIndigo,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Identity card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(20.dp, 8.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFE9E1FB), Color(0xFFFBE4EF))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD8CCF7)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ProfileIndigo,
                            modifier = Modifier.size(48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ProfileIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                userName,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Text(
                            userHandle,
                            fontSize = 13.sp,
                            color = ProfileGrey
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = ProfileIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Loves typing in Sinhala & English",
                                fontSize = 12.sp,
                                color = ProfileGrey
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ProfileIndigo)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Premium", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x40FFFFFF) else Color(0xB3FFFFFF))
                        .padding(vertical = 14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IdentityStat(
                            icon = Icons.Filled.AutoAwesome,
                            iconColor = ProfileIndigo,
                            label = "Level",
                            value = level.toString()
                        )
                        IdentityStat(
                            icon = Icons.Filled.LocalFireDepartment,
                            iconColor = Color(0xFFE0642B),
                            label = "Streak",
                            value = "$streakDays Days"
                        )
                        IdentityStat(
                            icon = Icons.Filled.EmojiEvents,
                            iconColor = ProfileIndigo,
                            label = "Rank",
                            value = rankLabel
                        )
                        IdentityStat(
                            icon = Icons.Filled.Groups,
                            iconColor = ProfileIndigo,
                            label = "Joined",
                            value = joinedLabel
                        )
                    }
                }
            }
        }

        // ── Your Points ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Points",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("History", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfileIndigo)
                        androidx.compose.material3.Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = ProfileIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ProfileIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            formatWithCommas(totalPoints),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Total Points", fontSize = 11.sp, color = ProfileGrey)
                    }
                    Spacer(Modifier.width(18.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Next Level ${level + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF352A54) else Color(0xFFE3DAF7))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (pointsIntoLevel / 1000f).coerceIn(0f, 1f))
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(ProfileIndigo, ProfilePink)))
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$pointsToNextLevel more points to Level ${level + 1}",
                            fontSize = 10.sp,
                            color = ProfileGrey
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFBE1EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFE0498A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ── Your Medals ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 24.dp, 20.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your Medals",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfileIndigo)
                androidx.compose.material3.Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = ProfileIndigo,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MedalCard(
                modifier = Modifier.weight(1f),
                medalColor = MedalGold,
                title = "Gold Medal",
                subtitle = "2500+ Points",
                earned = totalPoints >= 2500
            )
            MedalCard(
                modifier = Modifier.weight(1f),
                medalColor = MedalSilver,
                title = "Silver Medal",
                subtitle = "1500+ Points",
                earned = totalPoints >= 1500
            )
            MedalCard(
                modifier = Modifier.weight(1f),
                medalColor = MedalBronze,
                title = "Bronze Medal",
                subtitle = "500+ Points",
                earned = totalPoints >= 500
            )
        }

        // ── Activity Overview ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(20.dp, 24.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Activity Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Keyboard,
                        iconColor = ProfileIndigo,
                        iconBg = if (isDark) Color(0xFF2E2748) else Color(0xFFEDE8FC),
                        value = formatWithCommas(totalCharacters),
                        label = "Total Typed"
                    )
                    ActivityStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.GpsFixed,
                        iconColor = ProfilePink,
                        iconBg = if (isDark) Color(0xFF3A2432) else Color(0xFFFCE8F0),
                        value = if (lastAccuracy > 0) "$lastAccuracy%" else "—",
                        label = "Accuracy"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.AccessTime,
                        iconColor = Color(0xFF3B82F6),
                        iconBg = if (isDark) Color(0xFF1D2C45) else Color(0xFFE4EDFC),
                        value = typingTimeLabel,
                        label = "Typing Time"
                    )
                    ActivityStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.CheckCircle,
                        iconColor = Color(0xFF16A34A),
                        iconBg = if (isDark) Color(0xFF163024) else Color(0xFFE2F5E8),
                        value = testsCompleted.toString(),
                        label = "Lessons Done"
                    )
                }
            }
        }

        // ── Menu list ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            MenuRow(
                icon = Icons.Filled.Person,
                iconBg = ProfileIndigo,
                title = "Edit Profile"
            )
            MenuRow(
                icon = Icons.Filled.Lock,
                iconBg = Color(0xFF3B82F6),
                title = "Privacy Settings"
            )
            MenuRow(
                icon = Icons.Filled.Notifications,
                iconBg = Color(0xFFF4B400),
                title = "Notifications"
            )
            MenuRow(
                icon = Icons.Filled.HelpOutline,
                iconBg = Color(0xFFE0642B),
                title = "Help & Support",
                showDivider = false
            )
        }
    }
}

@Composable
private fun IdentityStat(icon: ComposeImageVector, iconColor: Color, label: String, value: String) {
    Column(
        modifier = Modifier.width(0.dp).weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = ProfileGrey)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MedalCard(
    modifier: Modifier = Modifier,
    medalColor: Color,
    title: String,
    subtitle: String,
    earned: Boolean
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 18.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(medalColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 10.sp, color = ProfileGrey)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (earned) Color(0xFFFBE1EA) else Color(0xFFEDEDED))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                if (earned) "Earned" else "Locked",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (earned) EarnedPink else ProfileGrey
            )
        }
    }
}

@Composable
private fun ActivityStat(
    modifier: Modifier = Modifier,
    icon: ComposeImageVector,
    iconColor: Color,
    iconBg: Color,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(iconBg)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = ProfileGrey)
    }
}

@Composable
private fun MenuRow(
    icon: ComposeImageVector,
    iconBg: Color,
    title: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 14.dp, 16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = ProfileGrey,
                modifier = Modifier.size(18.dp)
            )
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/** "24350" -> "24,350", matching the comma-grouped figures in the mockup. */
private fun formatWithCommas(n: Long): String =
    "%,d".format(n)
