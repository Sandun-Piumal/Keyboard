package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    keySoundEnabled: Boolean,
    keyVibrateEnabled: Boolean,
    themeMode: ThemeMode,
    mixAutoSinhala: Boolean,
    swipeTypingEnabled: Boolean,
    smoothImeTransition: Boolean = true,
    onLanguageChange: (String) -> Unit,
    onKeySoundChange: (Boolean) -> Unit,
    onKeyVibrateChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMixAutoSinhalaChange: (Boolean) -> Unit,
    onSwipeTypingChange: (Boolean) -> Unit,
    onSmoothImeTransitionChange: (Boolean) -> Unit = {},
    onOpenKeyboardHeight: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        SectionHeader(" ", " ")

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
                icon = "👆",
                title = "Swipe to type",
                subtitle = "Drag across letters instead of tapping each one — works for Sinhala & English"
            ) {
                Switch(checked = swipeTypingEnabled, onCheckedChange = onSwipeTypingChange)
            }
            SettingRow(icon = "🔊", title = "Key sound", subtitle = "Play a click on tap") {
                Switch(checked = keySoundEnabled, onCheckedChange = onKeySoundChange)
            }
            SettingRow(icon = "📳", title = "Vibrate on tap", subtitle = "Haptic feedback") {
                Switch(checked = keyVibrateEnabled, onCheckedChange = onKeyVibrateChange)
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
