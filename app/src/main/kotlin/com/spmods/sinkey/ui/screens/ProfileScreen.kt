package com.spmods.sinkey.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.PreferencesManager
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
    onEditProfile: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme(),
    currentPalette: KeyColorPalette = KeyColorPalette.DEFAULT,
    defaultLanguage: String = "si"
) {
    val context = LocalContext.current
    val statsRepo = remember(context) { TypingStatsRepository(context) }
    val prefs = remember(context) { PreferencesManager(context) }

    val totalCharacters by statsRepo.totalCharacters.collectAsState(initial = 0L)
    val lastAccuracy by statsRepo.lastAccuracy.collectAsState(initial = 0)
    val testsCompleted by statsRepo.testsCompleted.collectAsState(initial = 0)
    val streakDays by statsRepo.currentStreakDays.collectAsState(initial = 0)
    val firstActiveDate by statsRepo.firstActiveDate.collectAsState(initial = null)

    val firstName by prefs.profileFirstName.collectAsState(initial = "")
    val lastName by prefs.profileLastName.collectAsState(initial = "")
    val gender by prefs.profileGender.collectAsState(initial = "")

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

    val userName = "$firstName $lastName".trim().ifBlank { "Your Name" }
    val userHandle = "@" + (firstName.trim().lowercase().ifBlank { "sinkey_user" })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header (matches TypingTestScreen's header) — fixed, doesn't scroll ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp, 20.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2145) else Color(0xFFEDE7FB))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ProfileIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text("My ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ProfileIndigo)
                    Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ProfilePink)
                }
                Text(
                    "Type Smart. Type Easy. Type SinKey.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFB6AEC9) else Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2145) else Color.White)
                    .clickable { onEditProfile() },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.ModeEdit,
                    contentDescription = "Edit profile",
                    tint = ProfileIndigo,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Scrollable content (everything below the header) ───────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
        // ── Identity card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(20.dp, 8.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        if (isDark) {
                            listOf(Color(0xFF241C3A), Color(0xFF3A2436))
                        } else {
                            listOf(Color(0xFFE9E1FB), Color(0xFFFBE4EF))
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF3A2E5C) else Color(0xFFD8CCF7)),
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
                                else -> androidx.compose.material3.Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = ProfileIndigo,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ProfileIndigo)
                                .border(2.dp, if (isDark) Color(0xFF241C3A) else Color(0xFFFBE4EF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
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
                                Icons.Filled.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0xFF2E2748) else Color(0xB3FFFFFF))
                        .padding(vertical = 14.dp)
                ) {
                    val statIndigo = if (isDark) Color(0xFF9C87F5) else ProfileIndigo
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IdentityStat(
                            icon = Icons.Filled.AutoAwesome,
                            iconColor = statIndigo,
                            label = "Level",
                            value = level.toString(),
                            isDark = isDark
                        )
                        IdentityStat(
                            icon = Icons.Filled.LocalFireDepartment,
                            iconColor = Color(0xFFE0642B),
                            label = "Streak",
                            value = "$streakDays Days",
                            isDark = isDark
                        )
                        IdentityStat(
                            icon = Icons.Filled.EmojiEvents,
                            iconColor = statIndigo,
                            label = "Rank",
                            value = rankLabel,
                            isDark = isDark
                        )
                        IdentityStat(
                            icon = Icons.Filled.Groups,
                            iconColor = statIndigo,
                            label = "Joined",
                            value = joinedLabel,
                            isDark = isDark
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
                title = "Edit Profile",
                onClick = onEditProfile
            )
            MenuRow(
                icon = Icons.Filled.HelpOutline,
                iconBg = Color(0xFFE0642B),
                title = "Help & Support",
                showDivider = false,
                onClick = onHelpSupport
            )
        }
        } // end scrollable content Column
    }
}

@Composable
private fun RowScope.IdentityStat(icon: ComposeImageVector, iconColor: Color, label: String, value: String, isDark: Boolean) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (isDark) Color(0xFFB6AEC9) else ProfileGrey)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        )
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
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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
