package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
 * Maps each emoji category name to its selected/unselected tab-icon drawable
 * pair, matching the standard category glyphs (clock, smiley, paw, coffee
 * cup, baseball, house, lightbulb, heart, flag).
 */
private fun categoryIconRes(name: String, selected: Boolean): Int = when (name) {
    "Recent" -> if (selected) R.drawable.ic_emoji_recents_selected else R.drawable.ic_emoji_recents_unselected
    "Smileys" -> if (selected) R.drawable.ic_emoji_people_selected else R.drawable.ic_emoji_people_unselected
    "People" -> if (selected) R.drawable.ic_emoji_people_selected else R.drawable.ic_emoji_people_unselected
    "Animals" -> if (selected) R.drawable.ic_emoji_animals_selected else R.drawable.ic_emoji_animals_unselected
    "Food" -> if (selected) R.drawable.ic_emoji_food_selected else R.drawable.ic_emoji_food_unselected
    "Travel" -> if (selected) R.drawable.ic_emoji_travel_selected else R.drawable.ic_emoji_travel_unselected
    "Activities" -> if (selected) R.drawable.ic_emoji_activity_selected else R.drawable.ic_emoji_activity_unselected
    "Objects" -> if (selected) R.drawable.ic_emoji_objects_selected else R.drawable.ic_emoji_objects_unselected
    "Symbols" -> if (selected) R.drawable.ic_emoji_symbols_selected else R.drawable.ic_emoji_symbols_unselected
    "New ✨" -> if (selected) R.drawable.ic_emoji_objects_selected else R.drawable.ic_emoji_objects_unselected
    "Flags" -> if (selected) R.drawable.ic_emoji_flags_selected else R.drawable.ic_emoji_flags_unselected
    else -> if (selected) R.drawable.ic_emoji_symbols_selected else R.drawable.ic_emoji_symbols_unselected
}

/**
 * Emoji picker board. Fully theme-aware (dark/light follow [colors], the
 * same palette the rest of the keyboard uses) — previously this screen had
 * its own hardcoded light-grey colors and never changed with the system/app
 * theme. Layout follows a standard tabbed-category picker: back + category
 * tabs + delete on top, an active-tab underline baked into that same row,
 * a scrollable emoji grid, and a bottom icon row (keyboard / emoji / delete).
 *
 * Sized to structurally match MainKeyboardKeys / SymbolsKeyboardView /
 * NumberPadView: the outer Column uses the identical padding
 * (`horizontal = 4dp, vertical = 2dp` + `bottom = bottomPadding`), and it
 * has exactly 4 rows, each `Row().padding(vertical = 3dp)` wrapping
 * `keyHeight`-tall content — top tab bar, grid (2 row-units tall), and
 * bottom icon row. Every row (including the tab row) scales with
 * `keyHeight`, the same unit every other board's rows use, so this board's
 * TOTAL height always matches the main keyboard's total height, at every
 * keyboard-height setting — not just the default. A fixed-dp row here would
 * only match by coincidence at one specific setting.
 */
@Composable
internal fun EmojiPickerView(
    recentEmojis: List<String>,
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
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

    // One "row unit" is exactly what every key row on every other board
    // (Main / Symbols / Numpad) uses: keyHeight of content inside
    // vertical=3dp row padding. Every row in this board — including the
    // tab row — must use this same unit, or the board's TOTAL height stops
    // matching the main keyboard's total height whenever the keyboard-height
    // setting is changed from the default (a fixed-dp row only matches by
    // coincidence at one specific setting).
    val rowUnit = keyHeight + 6.dp
    // Grid spans 2 row-units, same as Symbols' row2+row3 — so total rows
    // here (tab row 1 + grid 2 + bottom row 1 = 4) match MainKeyboardKeys'
    // 4 rows exactly, at every keyboard-height setting.
    val gridHeight = rowUnit * 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // ── Row 1: back + category tabs + delete — same keyHeight-tall row
        // as every other row on this board and on the main keyboard, so the
        // board's total height always matches MainKeyboardKeys' total height,
        // at any keyboard-height setting.
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(keyHeight)
                    .weight(1.4f)
                    .clip(RoundedCornerShape(6.dp))
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

            allCategories.forEachIndexed { index, category ->
                val isSelected = index == selectedCategory
                Box(
                    modifier = Modifier
                        .height(keyHeight)
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) activeTabBg else Color.Transparent)
                        .clickable {
                            selectedCategory = index
                            coroutineScope.launch {
                                gridState.animateScrollToItem(categoryStartIndices[index])
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = categoryIconRes(category.name, isSelected)),
                            contentDescription = category.name,
                            tint = if (isSelected) activeTint else inactiveTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(14.dp)
                                .height(2.dp)
                                .background(
                                    if (isSelected) underlineColor else Color.Transparent,
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(keyHeight)
                    .weight(1.4f)
                    .clip(RoundedCornerShape(6.dp))
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

        // ── Rows 2–3 (combined): scrollable emoji grid, exactly 2 row-units tall.
        // NOTE: vertical padding must be applied OUTSIDE the height(...) box
        // (like every other row's `Row().padding(vertical = 3.dp)` does),
        // not inside it — padding inside a fixed-height Modifier shrinks the
        // grid's actual content area instead of adding to the row's total
        // height, which was leaving unfilled space at the bottom of the board.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .padding(vertical = 3.dp)
        ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
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
                            .padding(start = 2.dp, top = 6.dp, bottom = 4.dp)
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

        // ── Row 4: bottom icon row — keyboard / emoji / delete — same
        // keyHeight-tall row as every other board's final row.
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(keyHeight)
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
                    .height(keyHeight)
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
                    .height(keyHeight)
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
