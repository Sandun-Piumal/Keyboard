package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R
import kotlinx.coroutines.launch

/**
 * Returns true if this emoji string is renderable on the current device.
 * We check by seeing if the string's codepoints are all in supported ranges
 * and that it doesn't contain unsupported flag/ZWJ sequences on older APIs.
 */
private fun String.isSupported(): Boolean {
    val codePoints = codePoints().toArray()
    val sdk = android.os.Build.VERSION.SDK_INT

    // Flag sequences (U+1F1E6..U+1F1FF): need Android 6.0+
    val hasFlagIndicator = codePoints.any { it in 0x1F1E6..0x1F1FF }
    if (hasFlagIndicator && sdk < 23) return false

    // Unicode 15.0 emojis (e.g. 🫨 U+1FAE8, 🪽 U+1FABD, etc.): need Android 14+ (API 34)
    // Range: new codepoints beyond 0x1FAF8 added in Unicode 15.0
    if (codePoints.any { it in 0x1FAD7..0x1FAFF } && sdk < 34) return false

    // Unicode 15.1 ZWJ sequences (e.g. 🙂‍↕️): need Android 14+ (API 34)
    // Detect by checking for ZWJ (U+200D) combined with new directional arrows
    val hasZwj = codePoints.any { it == 0x200D }
    val hasDirectionalArrow = codePoints.any { it == 0x2195 || it == 0x2194 }
    if (hasZwj && hasDirectionalArrow && sdk < 34) return false

    // Unicode 14.0 emojis: need Android 12+ (API 31)
    if (codePoints.any { it in 0x1FAB7..0x1FAC2 } && sdk < 31) return false

    return true
}

/**
 * Emoji picker board. Fully theme-aware (dark/light follow [colors], the
 * same palette the rest of the keyboard uses) — previously this screen had
 * its own hardcoded light-grey colors and never changed with the system/app
 * theme. Layout follows a standard tabbed-category picker: back + category
 * tabs + delete on top, an active-tab underline, a scrollable emoji grid
 * grouped by category, and a bottom icon row (keyboard / emoji / delete)
 * mirroring the other boards (Symbols, Numpad) so switching between them
 * doesn't change the keyboard's overall height.
 *
 * [keyHeight] and [bottomPadding] are the same values MainKeyboardKeys /
 * SymbolsKeyboardView use for their key rows — passing them in here keeps
 * this board's total height identical to the others instead of the previous
 * fixed 200dp grid, which made the keyboard visibly resize on every switch.
 */
@Composable
internal fun EmojiPickerView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    val allCategories = remember {
        buildList {
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

    // Tab / active-state colors derived from the shared keyboard palette so
    // this board always matches whatever theme (dark or light) is active.
    val activeTint = colors.keyText
    val inactiveTint = colors.subText
    val activeTabBg = colors.specialKeyBg
    val underlineColor = colors.keyText

    // Auto-update selected tab based on scroll position
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        val firstVisible = gridState.firstVisibleItemIndex
        // Find which category this item belongs to
        val catIndex = categoryStartIndices.indexOfLast { it <= firstVisible }
        if (catIndex >= 0 && catIndex != selectedCategory) {
            selectedCategory = catIndex
        }
    }

    // Match SymbolsKeyboardView's total content height: 3 key rows + 1 bottom
    // row, each (keyHeight + 6dp vertical padding), plus bottomPadding. The
    // top bar + underline here take the place of one of those rows visually,
    // so the grid gets the remaining space to land on the same overall height.
    val rowUnit = keyHeight + 6.dp
    val topBarHeight = 42.dp
    val underlineRowHeight = 10.dp
    val bottomIconRowHeight = keyHeight
    val gridHeight = (rowUnit * 4) - topBarHeight - underlineRowHeight - bottomIconRowHeight - 6.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(bottom = bottomPadding)
    ) {
        // ── Top bar: back + category tabs + delete ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.keyText,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Icon-only category tabs, scrollable if they overflow width.
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                allCategories.forEachIndexed { index, category ->
                    val isSelected = index == selectedCategory
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) activeTabBg else Color.Transparent)
                            .clickable {
                                selectedCategory = index
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(categoryStartIndices[index])
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) activeTint else inactiveTint
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Backspace,
                    contentDescription = null,
                    tint = colors.keyText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Active-tab underline — thin bar centered under the selected tab.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(underlineRowHeight)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading spacer matches the back-button width so the tab row
            // (which sits in a weighted middle section) lines up below.
            Spacer(modifier = Modifier.size(32.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                allCategories.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(
                                if (index == selectedCategory) underlineColor else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.size(32.dp))
        }

        // ── Single unified grid with all categories ────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 4.dp)
        ) {
            allCategories.forEachIndexed { catIndex, category ->
                // Category header — full width
                item(
                    key = "header_$catIndex",
                    span = { GridItemSpan(8) }
                ) {
                    Text(
                        text = category.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.subText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
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

        // ── Bottom icon row — matches the height of other boards' final
        // row (SymbolsKeyboardView's ABC/space/enter row, NumberPad's row).
        // Keyboard icon returns to the typing board; emoji icon is a no-op
        // shortcut (already on this board) kept for visual/layout parity
        // with standard emoji pickers; delete removes the last character.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(bottomIconRowHeight)
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
            Box(
                modifier = Modifier
                    .height(bottomIconRowHeight)
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = activeTint
                )
            }
            Box(
                modifier = Modifier
                    .height(bottomIconRowHeight)
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Backspace,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
        }
    }
}
