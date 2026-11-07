package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Sound & vibration" settings screen, in the same frosted-glass style as the
 * rest of Settings (see GlassKit.kt):
 *   • Sound toggle          ("Sound on key press")
 *   • Vibrate toggle        ("Vibrate on key press")
 *   • Vibration level row   → opens a dialog with a 1..50ms slider,
 *     "Default" reset, Cancel, Save
 *
 * Vibration level's dialog is deliberately its own local edit state
 * (draft) rather than calling onVibrationMsChange on every drag tick —
 * Cancel needs to be able to discard in-progress changes, which isn't
 * possible if the slider already wrote them out live.
 */
@Composable
fun SoundVibrationScreen(
    soundEnabled: Boolean,
    vibrateEnabled: Boolean,
    vibrationMs: Float,       // 1f..50f, default 14f
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrateEnabledChange: (Boolean) -> Unit,
    onVibrationMsChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    val p = rememberGlassPalette()
    var showVibrationDialog by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp)
    ) {
        GlassTopBar(p, "Sound & vibration", onBack)

        GlassCard(p, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), radius = 28) {
            SettingRow(p, glyph = "🔊", title = "Sound", subtitle = "Sound on key press") {
                GlassSwitch(p, soundEnabled, onSoundEnabledChange)
            }
            SettingRow(p, glyph = "📳", title = "Vibrate", subtitle = "Vibrate on key press") {
                GlassSwitch(p, vibrateEnabled, onVibrateEnabledChange)
            }
            SettingRow(
                p, glyph = "🎚️",
                title = "Vibration level",
                subtitle = vibrationLevelLabel(vibrationMs),
                modifier = Modifier.clickable { showVibrationDialog = vibrationMs },
                last = true
            ) {
                Icon(Icons.Filled.ChevronRight, null, tint = p.iconIndigo, modifier = Modifier.size(26.dp))
            }
        }
    }

    if (showVibrationDialog >= 0f) {
        var draft by remember(showVibrationDialog) { mutableFloatStateOf(showVibrationDialog) }
        GlassAlertDialog(
            p = p,
            onDismiss = { showVibrationDialog = -1f },
            title = "Vibration level",
            text = {
                Column {
                    Text(vibrationLevelLabel(draft), fontSize = 13.sp, color = p.body)
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        valueRange = 1f..50f,
                        colors = glassSliderColors(p),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { draft = 14f }) {
                        Text("Default", color = p.iconIndigo, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showVibrationDialog = -1f }) {
                        Text("Cancel", color = p.body)
                    }
                    TextButton(onClick = {
                        onVibrationMsChange(draft)
                        showVibrationDialog = -1f
                    }) {
                        Text("Save", color = p.iconIndigo, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        )
    }
}

private fun vibrationLevelLabel(ms: Float): String {
    val rounded = ms.toInt()
    return if (rounded == 14) "Default ($rounded ms)" else "$rounded ms"
}
