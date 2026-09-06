package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector as ComposeImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.TypingStatsRepository
import com.spmods.sinkey.data.dictionary.WordRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfileIndigo = Color(0xFF6C4CE0)
private val ProfilePink   = Color(0xFFE0498A)
private val ProfileGrey   = Color(0xFF6B7280)

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
    val wordRepo = remember(context) { WordRepository(context) }

    val totalCharacters by statsRepo.totalCharacters.collectAsState(initial = 0L)
    val totalWords by statsRepo.totalWords.collectAsState(initial = 0L)
    val bestWpm by statsRepo.bestWpm.collectAsState(initial = 0)
    val lastAccuracy by statsRepo.lastAccuracy.collectAsState(initial = 0)
    val testsCompleted by statsRepo.testsCompleted.collectAsState(initial = 0)
    val streakDays by statsRepo.currentStreakDays.collectAsState(initial = 0)
    val firstActiveDate by statsRepo.firstActiveDate.collectAsState(initial = null)

    var dictionaryWordCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        dictionaryWordCount = wordRepo.totalWordCount()
    }

    // 1 point per 1000 real characters typed through the keyboard — a
    // direct, honest formula (not fabricated progress). Level advances
    // every 10 points. Both are integer math on totalCharacters, so they
    // always agree with the Total typed figure shown below.
    val points = (totalCharacters / 1000L).toInt()
    val level = (points / 10) + 1
    val pointsIntoLevel = points % 10
    val pointsToNextLevel = 10 - pointsIntoLevel

    val joinedLabel = remember(firstActiveDate) {
        firstActiveDate?.let { raw ->
            runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)
                SimpleDateFormat("MMM yyyy", Locale.US).format(parsed ?: Date())
            }.getOrDefault(raw)
        } ?: "Just started"
    }

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
            Text(
                "My Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        // ── Identity + level card ────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(20.dp, 8.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF241C3A) else Color(0xFFF3EFFC))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ProfileIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Keyboard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Level $level",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Joined $joinedLabel",
                            fontSize = 12.sp,
                            color = ProfileGrey
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(currentPalette.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            currentPalette.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentPalette.accent
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "$pointsToNextLevel more points to Level ${level + 1}",
                    fontSize = 11.sp,
                    color = ProfileGrey
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF352A54) else Color(0xFFE3DAF7))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (pointsIntoLevel / 10f).coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(ProfileIndigo, ProfilePink)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ProfileStatMini(label = "Points", value = points.toString())
                    ProfileStatMini(label = "Streak", value = "$streakDays d")
                    ProfileStatMini(label = "Tests", value = testsCompleted.toString())
                }
            }
        }

        // ── Typing stats ─────────────────────────────────────────────────
        SectionLabel("Typing stats")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Keyboard,
                iconColor = ProfileIndigo,
                value = formatCount(totalCharacters),
                label = "Total typed"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Speed,
                iconColor = ProfilePink,
                value = if (bestWpm > 0) "$bestWpm" else "—",
                label = "Best WPM"
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                iconColor = ProfileIndigo,
                value = if (lastAccuracy > 0) "$lastAccuracy%" else "—",
                label = "Last accuracy"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                iconColor = ProfilePink,
                value = "$streakDays",
                label = "Day streak"
            )
        }

        // ── Vocabulary + theme ───────────────────────────────────────────
        SectionLabel("Vocabulary & theme")
        Column(
            modifier = Modifier
                .padding(20.dp, 4.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ProfileRow(
                icon = Icons.Filled.Book,
                iconBg = if (isDark) Color(0xFF2E2748) else Color(0xFFEDE8FC),
                iconTint = ProfileIndigo,
                title = "Words learned",
                value = dictionaryWordCount.toString()
            )
            ProfileRow(
                icon = Icons.Filled.Bolt,
                iconBg = if (isDark) Color(0xFF3A2432) else Color(0xFFFCE8F0),
                iconTint = ProfilePink,
                title = "Words typed",
                value = formatCount(totalWords)
            )
            ProfileRow(
                icon = Icons.Filled.Palette,
                iconBg = if (isDark) Color(0xFF2E2748) else Color(0xFFEDE8FC),
                iconTint = currentPalette.accent,
                title = "Active theme",
                value = currentPalette.label
            )
            ProfileRow(
                icon = Icons.Filled.CalendarMonth,
                iconBg = if (isDark) Color(0xFF3A2432) else Color(0xFFFCE8F0),
                iconTint = ProfilePink,
                title = "Typing language",
                value = when (defaultLanguage) {
                    "si" -> "Sinhala"
                    "en" -> "English"
                    else -> "Mix"
                },
                showDivider = false
            )
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .padding(20.dp, 0.dp, 20.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF211A33) else Color(0xFFF6F3FC))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "All stats are recorded on this device only — nothing here is sent anywhere.",
                fontSize = 11.sp,
                color = ProfileGrey,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ProfileGrey,
        modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp)
    )
}

@Composable
private fun RowScope.ProfileStatMini(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = ProfileGrey)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ComposeImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = ProfileGrey)
    }
}

@Composable
private fun ProfileRow(
    icon: ComposeImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp, 16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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

/** "24350" -> "24.3k" once it's past 4 digits, to keep the stat card compact. */
private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 10_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}
