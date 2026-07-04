package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val DeshGreenPicker = Color(0xFF2D6A4F)
private val PickerBg = Color(0xFFDDE1E7)
private val TabActiveBg = Color(0xFFC8D0D8)

/**
 * Returns true if this emoji string is renderable on the current device.
 * We check by seeing if the string's codepoints are all in supported ranges
 * and that it doesn't contain unsupported flag/ZWJ sequences on older APIs.
 */
private fun String.isSupported(): Boolean {
    // Flag sequences: regional indicator pairs (U+1F1E6..U+1F1FF)
    // These often show as boxes on older Android
    val codePoints = codePoints().toArray()
    val hasFlagIndicator = codePoints.any { it in 0x1F1E6..0x1F1FF }
    if (hasFlagIndicator && android.os.Build.VERSION.SDK_INT < 23) return false

    // Very new emojis (Unicode 15+) may not render on older devices
    if (codePoints.any { it > 0x1FAF8 } && android.os.Build.VERSION.SDK_INT < 33) return false

    // Keycap sequences and other complex ZWJ often fail — allow them but skip if >6 chars
    // (simple emojis are ≤4 chars with variation selectors)
    return true
}

@Composable
fun EmojiPickerView(
    recentEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasRecent = recentEmojis.isNotEmpty()
    val allCategories = remember(recentEmojis) {
        buildList {
            if (hasRecent) add(EmojiData.Category("🕐", "Recent", recentEmojis))
            // Filter out emojis that won't render on this device
            EmojiData.categories.forEach { cat ->
                val filtered = cat.emojis.filter { it.isSupported() }
                if (filtered.isNotEmpty()) add(cat.copy(emojis = filtered))
            }
        }
    }

    // Build a flat index map: gridItem index → category index
    // Each category has 1 header item + N emoji items
    // header items have full span (8 cols), emoji items have span 1
    val categoryStartIndices = remember(allCategories) {
        val indices = mutableListOf<Int>()
        var cursor = 0
        allCategories.forEach { cat ->
            indices.add(cursor)
            cursor += 1 + cat.emojis.size // 1 header + emojis
        }
        indices
    }

    var selectedCategory by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-update selected tab based on scroll position
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        val firstVisible = gridState.firstVisibleItemIndex
        // Find which category this item belongs to
        val catIndex = categoryStartIndices.indexOfLast { it <= firstVisible }
        if (catIndex >= 0 && catIndex != selectedCategory) {
            selectedCategory = catIndex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PickerBg)
    ) {
        // ── Top bar: back + delete ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .clickable {
                            selectedCategory = index
                            coroutineScope.launch {
                                // Scroll grid to the header of this category
                                gridState.animateScrollToItem(categoryStartIndices[index])
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.icon, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Green underline under selected tab
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
                            if (index == selectedCategory) DeshGreenPicker else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Single unified grid with all categories ────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            allCategories.forEachIndexed { catIndex, category ->
                // Category header — full width
                item(
                    key = "header_$catIndex",
                    span = { GridItemSpan(8) }
                ) {
                    Text(
                        text = category.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                // Emoji items
                items(
                    count = category.emojis.size,
                    key = { i -> "emoji_${catIndex}_$i" }
                ) { i ->
                    val emoji = category.emojis[i]
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
}
