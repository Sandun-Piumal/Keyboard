package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeshGreenPicker = Color(0xFF2D6A4F)
private val PickerBg = Color(0xFFDDE1E7)
private val TabActiveBg = Color(0xFFC8D0D8)

@Composable
fun EmojiPickerView(
    recentEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    // Recent category is index 0 only if there are recent emojis
    val hasRecent = recentEmojis.isNotEmpty()
    val allCategories = buildList {
        if (hasRecent) add(EmojiData.Category("🕐", "Recent", recentEmojis))
        addAll(EmojiData.categories)
    }
    var selectedCategory by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PickerBg)
    ) {
        // ── Top bar: back button + delete ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back to keyboard
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TabActiveBg)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌨️", fontSize = 18.sp)
            }

            // Backspace
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TabActiveBg)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌫", fontSize = 18.sp, color = Color(0xFF333333))
            }
        }

        // ── Category tab row ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allCategories.forEachIndexed { index, category ->
                val isSelected = index == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TabActiveBg else Color.Transparent)
                        .clickable { selectedCategory = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Thin green indicator line under selected tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allCategories.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (index == selectedCategory) DeshGreenPicker
                            else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Emoji grid ─────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 4.dp)
        ) {
            items(allCategories[selectedCategory].emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
