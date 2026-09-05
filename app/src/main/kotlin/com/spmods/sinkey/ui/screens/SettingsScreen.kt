package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    // Replays the first-launch onboarding tutorial — see
    // OnboardingScreen.kt / MainActivity's hasSeenOnboarding gate.
    onShowTutorial: () -> Unit = {}
) {
    // Bug fix: this Column had no verticalScroll at all, so once the
    // switches list + Theme Mode section together exceeded one screen's
    // height, everything past the fold was simply clipped — there was no
    // way to reach it, not even by dragging; the page just didn't scroll.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // The old "PREFERENCES / Settings" section header text used to open
        // this screen — removed since AppHeader (fixed above this scrolling
        // content, added in MainActivity) already shows the page title now.
        // Left with just a small gap so the first group isn't flush against
        // the header.
        Spacer(modifier = Modifier.height(10.dp))

        SettingsGroup {
            SettingRow(
                icon = "🌐",
                title = "Default language",
                subtitle = when (defaultLanguage) {
                    "si" -> "Sinhala first"
                    "en" -> "English first"
                    else -> "Mix — Sinhala + English"
                }
            ) {
                Text(
                    when (defaultLanguage) {
                        "si" -> "සිංහල"
                        "en" -> "English"
                        else -> "Mix"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        onLanguageChange(
                            when (defaultLanguage) {
                                "mix" -> "en"
                                "en" -> "si"
                                else -> "mix"
                            }
                        )
                    }
                )
            }
            SettingRow(
                icon = "🔁",
                title = "Mix mode: auto-convert to Sinhala",
                subtitle = "On space/enter, turn the typed word into Sinhala"
            ) {
                Switch(checked = mixAutoSinhala, onCheckedChange = onMixAutoSinhalaChange)
            }
            SettingRow(
                icon = "අ",
                title = "Sinhala letter hints",
                subtitle = "Show the matching සිංහල letter on each key"
            ) {
                Switch(checked = sinhalaKeyHintsEnabled, onCheckedChange = onSinhalaKeyHintsChange)
            }
            SettingRow(
                icon = "👆",
                title = "Swipe to type",
                subtitle = "Drag across letters instead of tapping each one — works for Sinhala & English"
            ) {
                Switch(checked = swipeTypingEnabled, onCheckedChange = onSwipeTypingChange)
            }
            SettingRow(
                icon = "🔊",
                title = "Sound & vibration",
                subtitle = "Key sound, vibrate, vibration level",
                modifier = Modifier.clickable { onOpenSoundVibration() }
            ) {
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingRow(
                icon = "⚡",
                title = "Quick text",
                subtitle = "Type a shortcut like \"gm\" and expand it to a full phrase",
                modifier = Modifier.clickable { onOpenQuickText() }
            ) {
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingRow(
                icon = "📖",
                title = "Personal dictionary",
                subtitle = "Words you've typed — view, add, or remove them",
                modifier = Modifier.clickable { onOpenPersonalDictionary() }
            ) {
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingRow(
                icon = "🎓",
                title = "Show tutorial again",
                subtitle = "Replay the getting-started guide",
                modifier = Modifier.clickable { onShowTutorial() }
            ) {
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingRow(
                icon = "✨",
                title = "Smooth keyboard transition",
                subtitle = "Gentle fade + slide when the keyboard appears"
            ) {
                Switch(checked = smoothImeTransition, onCheckedChange = onSmoothImeTransitionChange)
            }
            // Keyboard height — navigates to sub-screen
            SettingRow(
                icon = "⌨",
                title = "Keyboard height",
                subtitle = "Adjust height, bottom space & borders",
                modifier = Modifier.clickable { onOpenKeyboardHeight() }
            ) {
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionHeader("THEME MODE", null)
        SettingsGroup {
            ThemeRadioRow("Follow system", ThemeMode.SYSTEM, themeMode, onThemeModeChange)
            ThemeRadioRow("Always light", ThemeMode.LIGHT, themeMode, onThemeModeChange)
            ThemeRadioRow("Always dark", ThemeMode.DARK, themeMode, onThemeModeChange)
        }
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String?) {
    Column(modifier = Modifier.padding(22.dp, 18.dp, 22.dp, 4.dp)) {
        Text(eyebrow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.secondary)
        if (title != null) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ThemeRadioRow(label: String, mode: ThemeMode, current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(16.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (mode == current) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.background
                )
        )
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
