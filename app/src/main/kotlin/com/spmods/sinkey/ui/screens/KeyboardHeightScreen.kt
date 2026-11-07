package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Keyboard height" settings screen, in the shared frosted-glass style
 * (GlassKit.kt):
 *   • Height slider  (S / M / L / XL) with a "Default" reset pill
 *   • Bottom space toggle + slider (S / M / L / XL)
 *   • Show key borders toggle
 *   • Key opacity slider
 *   • Preview FAB (handled in caller via onPreviewClick)
 */
@Composable
fun KeyboardHeightScreen(
    keyboardHeight: Float,           // 0f=S  1f=M  2f=L  3f=XL
    bottomSpaceEnabled: Boolean,
    bottomSpaceSize: Float,          // 0f=S  1f=M  2f=L  3f=XL
    showKeyBorders: Boolean,
    keyOpacity: Float,
    onKeyboardHeightChange: (Float) -> Unit,
    onBottomSpaceEnabledChange: (Boolean) -> Unit,
    onBottomSpaceSizeChange: (Float) -> Unit,
    onShowKeyBordersChange: (Boolean) -> Unit,
    onKeyOpacityChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    val p = rememberGlassPalette()
    val isDefault = keyboardHeight == 0f   // S is the factory default

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)       // clear of the preview FAB
    ) {
        GlassTopBar(p, "Keyboard height", onBack)

        GlassCard(p, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), radius = 28) {

            // ── Height ───────────────────────────────────────────────────
            SettingRow(
                p, glyph = "📏",
                title = "Height",
                subtitle = "Change overall height of the keyboard",
                last = true
            ) {
                GlassPill(
                    p, "Default",
                    modifier = if (isDefault) Modifier.alpha(0.55f)
                    else Modifier.clickable { onKeyboardHeightChange(0f) }
                )
            }
            SizeSlider(
                p = p,
                value = keyboardHeight,
                onValueChange = onKeyboardHeightChange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassDivider(p)

            // ── Bottom space ─────────────────────────────────────────────
            SettingRow(
                p, glyph = "⬇️",
                title = "Bottom space",
                subtitle = "Add extra space at the bottom",
                last = true
            ) { GlassSwitch(p, bottomSpaceEnabled, onBottomSpaceEnabledChange) }
            // Bottom-space size slider — only shown when toggle is ON
            if (bottomSpaceEnabled) {
                SizeSlider(
                    p = p,
                    value = bottomSpaceSize,
                    onValueChange = onBottomSpaceSizeChange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            GlassDivider(p)

            // ── Show key borders ─────────────────────────────────────────
            SettingRow(
                p, glyph = "▦",
                title = "Show key borders",
                subtitle = "Show borders for keys"
            ) { GlassSwitch(p, showKeyBorders, onShowKeyBordersChange) }

            // ── Key opacity ──────────────────────────────────────────────
            // Continuous 0f..1f (not the 4-step SizeSlider) — lets the user
            // dial in a subtle amount rather than snap between a few fixed
            // steps. Most useful with a custom background photo set (see
            // PhotoEditThemeScreen, which offers the same slider at the
            // point of picking a photo), but works the same either way.
            SettingRow(
                p, glyph = "🔆",
                title = "Key opacity",
                subtitle = "Lower this to let a custom background show through each key",
                last = true
            ) {}
            Slider(
                value = keyOpacity,
                onValueChange = onKeyOpacityChange,
                colors = glassSliderColors(p),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            )
        }
    }
}

/**
 * A 4-step slider snapping to 0 / 1 / 2 / 3  (S / M / L / XL), with the
 * S M L XL labels beneath (current one highlighted).
 */
@Composable
private fun SizeSlider(
    p: HomePalette,
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
            colors = glassSliderColors(p)
        )

        // S / M / L / XL labels under the slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("S", "M", "L", "XL").forEachIndexed { idx, label ->
                val current = Math.round(value) == idx
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (current) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (current) p.iconIndigo else p.sub
                )
            }
        }
    }
}
