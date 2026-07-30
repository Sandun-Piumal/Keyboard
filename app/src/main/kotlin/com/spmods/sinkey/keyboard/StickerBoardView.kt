package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R
import com.spmods.sinkey.data.sticker.StickerEntity

private enum class StickerTab { ALL, FAVOURITES }

/**
 * Board.STICKER — grid of the user's own created stickers, with a
 * Favourites tab. Tapping a sticker stages a confirmation overlay
 * ("Send this sticker?") rather than sending immediately; favourite star
 * and delete are always-visible small icons on each cell since this is a
 * touch keyboard with no long-press affordance elsewhere.
 *
 * NOTE: this board previously also supported opt-in, read-only access to
 * WhatsApp/Telegram sticker folders via Storage Access Framework. That was
 * removed — in practice the connected folders kept re-scanning/reloading
 * every time the tab was reopened and stickers from them never actually
 * sent successfully, so the whole subsystem (ExternalStickerSource,
 * connectFolder/disconnectFolder, the Settings "Connect sticker folder"
 * row) was more trouble than it was worth. Only user-created stickers
 * (Image Sticker / Text Sticker, see StickerCreateView) remain.
 *
 * Sized exactly like ClipboardHistoryView/FontPickerView: a fixed-height
 * outer Column matching [targetContentHeight] (the main board's real
 * content height) with the scrollable grid taking whatever's left via
 * weight(1f), so this board is always pixel-identical in height to MAIN —
 * see the comment on measuredMainContentHeight in KeyboardView.kt for why
 * that matters.
 */
@Composable
internal fun StickerBoardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    ownStickers: List<StickerEntity>,
    favouriteStickers: List<StickerEntity>,
    onSendOwnSticker: (String) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp
    val tabsHeight = 40.dp
    var tab by remember { mutableStateOf(StickerTab.ALL) }

    // Tapping a sticker doesn't send it immediately — it stages a
    // confirmation ("Send this sticker?") shown as an overlay over the grid,
    // per the user's request to always confirm before a sticker leaves the
    // keyboard. Null means no confirmation is showing.
    var pendingSend by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
        // ── Header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
            Text(
                text = "Stickers",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCreateClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create sticker",
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
        }

        // ── Tabs: All / Favourites ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(tabsHeight).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StickerTabChip(
                label = "All", selected = tab == StickerTab.ALL, colors = colors,
                onClick = { tab = StickerTab.ALL }
            )
            StickerTabChip(
                label = "★ Favourites", selected = tab == StickerTab.FAVOURITES, colors = colors,
                onClick = { tab = StickerTab.FAVOURITES }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val stickers = if (tab == StickerTab.ALL) ownStickers else favouriteStickers
            val emptyMessage = if (tab == StickerTab.ALL) {
                "No stickers yet — tap + to create one"
            } else {
                "Tap ★ on a sticker to add it here"
            }
            OwnStickerGrid(
                stickers = stickers, colors = colors, modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                onSend = { path -> pendingSend = path },
                onToggleFavourite = onToggleFavourite, onDelete = onDeleteSticker,
                emptyMessage = emptyMessage
            )

            // Confirmation overlay — nothing is sent to the target app until
            // the user explicitly taps Send here.
            val pending = pendingSend
            if (pending != null) {
                StickerSendConfirmation(
                    filePath = pending,
                    colors = colors,
                    onConfirm = {
                        onSendOwnSticker(pending)
                        pendingSend = null
                    },
                    onCancel = { pendingSend = null }
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun StickerSendConfirmation(
    filePath: String,
    colors: KeyboardColors,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.bg)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* swallow — don't dismiss when tapping the card itself */ }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBg)
            ) {
                StickerImage(
                    source = filePath,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentDescription = "Sticker"
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
            Text("Send this sticker?", fontSize = 13.sp, color = colors.keyText, fontWeight = FontWeight.Medium)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keyBg)
                        .clickable { onCancel() }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp, color = colors.subText)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeshGreen)
                        .clickable { onConfirm() }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Send", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StickerTabChip(label: String, selected: Boolean, colors: KeyboardColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) DeshGreen.copy(alpha = 0.18f) else colors.keyBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) DeshGreen else colors.subText
        )
    }
}

@Composable
private fun OwnStickerGrid(
    stickers: List<StickerEntity>,
    colors: KeyboardColors,
    modifier: Modifier,
    onSend: (String) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    emptyMessage: String
) {
    if (stickers.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(emptyMessage, fontSize = 13.sp, color = colors.subText)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(stickers, key = { it.filePath }) { sticker ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keyBg)
                    .clickable { onSend(sticker.filePath) }
            ) {
                StickerImage(
                    source = sticker.filePath,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentDescription = "Sticker"
                )
                Icon(
                    imageVector = if (sticker.favourite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (sticker.favourite) "Unfavourite" else "Favourite",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleFavourite(sticker.filePath, !sticker.favourite) }
                        .padding(2.dp),
                    tint = if (sticker.favourite) DeshGreen else colors.subText.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onDelete(sticker.filePath) }
                        .padding(2.dp),
                    tint = colors.subText.copy(alpha = 0.7f)
                )
            }
        }
    }
}
