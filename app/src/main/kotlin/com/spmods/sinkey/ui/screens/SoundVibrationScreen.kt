package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Sound & vibration" settings screen — mirrors the two reference
 * screenshots:
 *   • Sound toggle          ("Sound on key press")
 *   • Vibrate toggle        ("Vibrate on key press")
 *   • Vibration level row   → opens a dialog with a 1..50ms slider,
 *     "Default" reset, Cancel, Save
 *
 * Vibration level's dialog is deliberately its own local edit state
 * (draftVibrationMs) rather than calling onVibrationMsChange on every
 * drag tick — Cancel needs to be able to discard in-progress changes,
 * which isn't possible if the slider already wrote them out live.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var showVibrationDialog by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text("Sound & vibration", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        SoundVibrationCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sound", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sound on key press",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrate", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Vibrate on key press",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = vibrateEnabled,
                    onCheckedChange = onVibrateEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVibrationDialog = vibrationMs }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibration level", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        vibrationLevelLabel(vibrationMs),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showVibrationDialog >= 0f) {
        var draft by remember(showVibrationDialog) { mutableFloatStateOf(showVibrationDialog) }
        AlertDialog(
            onDismissRequest = { showVibrationDialog = -1f },
            title = { Text("Vibration level", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        vibrationLevelLabel(draft),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { draft = 14f }) {
                        Text("Default", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showVibrationDialog = -1f }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        onVibrationMsChange(draft)
                        showVibrationDialog = -1f
                    }) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
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

@Composable
private fun SoundVibrationCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}
