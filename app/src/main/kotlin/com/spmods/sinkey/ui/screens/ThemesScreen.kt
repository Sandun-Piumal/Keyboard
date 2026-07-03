package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.ThemeMode

private data class ThemeOption(val label: String, val siLabel: String, val bg: Color, val fg: Color, val emoji: String, val mode: ThemeMode?)

private val themeOptions = listOf(
    ThemeOption("Cream Light", "ආලෝකය", Color(0xFFF4F2ED), Color(0xFF241C14), "☀️", ThemeMode.LIGHT),
    ThemeOption("Night", "අඳුර", Color(0xFF15130F), Color(0xFFF2EDE4), "🌙", ThemeMode.DARK),
    ThemeOption("Follow system", "පද්ධතිය", Color(0xFFEFE6D8), Color(0xFF7A2038), "⚙️", ThemeMode.SYSTEM)
)

@Composable
fun ThemesScreen(currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(modifier = Modifier.padding(22.dp, 18.dp, 22.dp, 4.dp)) {
            Text(
                "APPEARANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text("Choose a theme", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "The keyboard follows your pick everywhere you type.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themeOptions) { option ->
                val selected = option.mode == currentMode
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { option.mode?.let(onSelect) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(option.bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option.emoji, fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.padding(12.dp, 9.dp)) {
                        Text(option.label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(option.siLabel, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
