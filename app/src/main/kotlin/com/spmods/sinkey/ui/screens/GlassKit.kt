package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Shared "frosted glass" building blocks for the Settings pages — the same look
 * as Home (HomePalette / glassCard). Settings, Sound & vibration, Quick text,
 * Personal dictionary and Keyboard height all build from these so they stay
 * identical. The pages sit on the About-Developer background painted by
 * MainActivity, so nothing here draws a full-page background.
 */

private val Indigo = Color(0xFF5B2BEF)
private val IndigoDark = Color(0xFF34128F)

@Composable
internal fun rememberGlassPalette(): HomePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(dark) { homePalette(dark) }
}

/** Transparent top bar for sub-pages: glossy round back button + big title. */
@Composable
internal fun GlassTopBar(p: HomePalette, title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                    else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                )
                .border(1.5.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = p.iconIndigo, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
    }
}

@Composable
internal fun GlassCard(
    p: HomePalette,
    modifier: Modifier = Modifier,
    radius: Int = 26,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(p, radius = radius, elevation = 8)
            .padding(vertical = 6.dp)
    ) { content() }
}

/** Switch tinted like the design: deep-purple track when on, pale lavender when off. */
@Composable
internal fun GlassSwitch(p: HomePalette, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Indigo,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = if (p.dark) Color(0xFF2E2560) else Color(0xFFE6DCFB),
            uncheckedBorderColor = if (p.dark) Color(0xFF4A3F85) else Color(0xFFC9B8F3)
        )
    )
}

@Composable
internal fun glassSliderColors(p: HomePalette): SliderColors = SliderDefaults.colors(
    thumbColor = Indigo,
    activeTrackColor = Indigo,
    inactiveTrackColor = if (p.dark) Color(0xFF3A2F66) else Color(0xFFDCD0F6),
    activeTickColor = Color.White.copy(alpha = 0.6f),
    inactiveTickColor = if (p.dark) Color(0xFF5A4D96) else Color(0xFFBFA9F0)
)

/** Glossy rounded icon tile used at the start of every settings row. */
@Composable
internal fun IconTile(p: HomePalette, glyph: String, size: Int = 46, fontSize: Int = 22) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(5.dp, shape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
            .clip(shape)
            .background(
                if (p.dark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                else Brush.verticalGradient(listOf(Color(0xFFF6F1FF), Color(0xFFE4D8FD)))
            )
            .border(1.dp, if (p.dark) SolidColor(p.cardBorder) else GlossyWhite, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, fontSize = fontSize.sp, color = p.iconIndigo, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * One settings row: glossy icon tile · title + subtitle · trailing control
 * (switch / chevron / pill), with a hairline divider underneath unless [last].
 * [compact] is the slimmer variant used in the ABOUT card and lists.
 */
@Composable
internal fun SettingRow(
    p: HomePalette,
    glyph: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    last: Boolean = false,
    trailing: @Composable () -> Unit = {}
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = if (compact) 8.dp else 10.dp, bottom = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconTile(p, glyph, size = if (compact) 38 else 46, fontSize = if (compact) 18 else 22)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, fontSize = 12.sp, color = p.body, lineHeight = 15.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }
            trailing()
        }
        if (!last) GlassDivider(p)
    }
}

@Composable
internal fun GlassDivider(p: HomePalette, startPadding: Int = 74) {
    HorizontalDivider(
        color = if (p.dark) Color(0xFF3A2F66) else Color(0xFFDCD0F6),
        modifier = Modifier.padding(start = startPadding.dp, end = 16.dp)
    )
}

/** Small lavender pill (e.g. "Default (14 ms)", "Default"). */
@Composable
internal fun GlassPill(p: HomePalette, text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = p.chipText,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(p.chipBg)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

/** Round gradient "+" button for the Quick text / Personal dictionary pages. */
@Composable
internal fun GlassFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .shadow(12.dp, CircleShape, ambientColor = Indigo, spotColor = Indigo)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), IndigoDark)))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("+", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** Segmented control; the selected segment is a deep-indigo block. */
@Composable
internal fun GlassSegmented(
    p: HomePalette,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(p, radius = 22, elevation = 6)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .then(
                        if (selected) Modifier.background(Brush.verticalGradient(listOf(Color(0xFF5A2BD8), IndigoDark)))
                        else Modifier
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) Color.White else p.title,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** AlertDialog dressed in the glass look (rounded, palette colours). */
@Composable
internal fun GlassAlertDialog(
    p: HomePalette,
    onDismiss: () -> Unit,
    title: String,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = if (p.dark) Color(0xFF1E1840) else Color(0xFFFCFBFE),
        titleContentColor = p.title,
        textContentColor = p.body,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
