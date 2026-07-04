package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Keyboard height" settings screen — mirrors the screenshot exactly:
 *   • Height slider  (S / M / L / XL) with a "Default" reset chip
 *   • Bottom space toggle + slider (S / M / L / XL)
 *   • Show key borders toggle
 *   • Preview FAB (handled in caller via onPreviewClick)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardHeightScreen(
    keyboardHeight: Float,           // 0f=S  1f=M  2f=L  3f=XL
    bottomSpaceEnabled: Boolean,
    bottomSpaceSize: Float,          // 0f=S  1f=M  2f=L  3f=XL
    showKeyBorders: Boolean,
    onKeyboardHeightChange: (Float) -> Unit,
    onBottomSpaceEnabledChange: (Boolean) -> Unit,
    onBottomSpaceSizeChange: (Float) -> Unit,
    onShowKeyBordersChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val isDefault = keyboardHeight == 1f   // M is the factory default

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top app bar ──────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    "Keyboard height",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // ⋮ menu placeholder — keeps layout consistent with screenshot
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // ── Card group ───────────────────────────────────────────────────────
        HeightCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            // ── Height row ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Height", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Change overall height of the keyboard",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isDefault) {
                    Text(
                        "Default",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onKeyboardHeightChange(1f) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        "Default",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            SizeSlider(
                value = keyboardHeight,
                onValueChange = onKeyboardHeightChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Bottom space row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bottom space", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Add extra space at the bottom",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bottomSpaceEnabled,
                    onCheckedChange = onBottomSpaceEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Bottom-space size slider — only shown when toggle is ON
            if (bottomSpaceEnabled) {
                SizeSlider(
                    value = bottomSpaceSize,
                    onValueChange = onBottomSpaceSizeChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Show key borders row ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show key borders", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Show borders for keys",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showKeyBorders,
                    onCheckedChange = onShowKeyBordersChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * A 4-step slider snapping to 0 / 1 / 2 / 3  (S / M / L / XL).
 * The thumb is drawn as a white circle and the labels S M L XL appear beneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = { raw ->
                // Snap to nearest integer step
                onValueChange(Math.round(raw).toFloat())
            },
            valueRange = 0f..3f,
            steps = 2,          // creates 4 positions: 0, 1, 2, 3
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // S / M / L / XL labels under the slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("S", "M", "L", "XL").forEachIndexed { idx, label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (Math.round(value) == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (Math.round(value) == idx)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Rounded card container matching the white/surface card in the screenshot. */
@Composable
private fun HeightCard(
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
