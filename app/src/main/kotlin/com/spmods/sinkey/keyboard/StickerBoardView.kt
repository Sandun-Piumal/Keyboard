package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.spmods.sinkey.R
import com.spmods.sinkey.data.sticker.ExternalSticker
import com.spmods.sinkey.data.sticker.ExternalStickerSource
import com.spmods.sinkey.data.sticker.StickerEntity

private enum class StickerTab { ALL, FAVOURITES, EXTERNAL }

/**
 * Board.STICKER — grid of the user's own created stickers plus (opt-in)
 * externally-connected WhatsApp/Telegram sticker folders, with a
 * Favourites tab. Tapping a sticker sends it immediately via
 * onSendOwnSticker/onSendExternalSticker; favourite star and delete are
 * always-visible small icons on each cell since this is a touch keyboard
 * with no long-press affordance elsewhere.
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
    connectedExternalFolders: List<DocumentFile>,
    externalStickerSource: ExternalStickerSource,
    onSendOwnSticker: (String) -> Unit,
    onSendExternalSticker: (String, String) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp
    val tabsHeight = 40.dp
    var tab by remember { mutableStateOf(StickerTab.ALL) }
    // Which connected external folder is active, when tab == EXTERNAL and
    // there's more than one folder connected. Defaults to the first.
    var activeFolder by remember(connectedExternalFolders) {
        mutableStateOf(connectedExternalFolders.firstOrNull())
    }

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

        // ── Tabs: All / Favourites / one per connected external folder ────
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
                label = "★", selected = tab == StickerTab.FAVOURITES, colors = colors,
                onClick = { tab = StickerTab.FAVOURITES }
            )
            connectedExternalFolders.forEach { folder ->
                val label = externalFolderLabel(folder)
                StickerTabChip(
                    label = label,
                    selected = tab == StickerTab.EXTERNAL && activeFolder?.uri == folder.uri,
                    colors = colors,
                    onClick = { tab = StickerTab.EXTERNAL; activeFolder = folder }
                )
            }
        }

        when (tab) {
            StickerTab.ALL -> OwnStickerGrid(
                stickers = ownStickers, colors = colors, modifier = Modifier.fillMaxWidth().weight(1f),
                onSend = onSendOwnSticker, onToggleFavourite = onToggleFavourite, onDelete = onDeleteSticker,
                emptyMessage = "No stickers yet — tap + to create one"
            )
            StickerTab.FAVOURITES -> OwnStickerGrid(
                stickers = favouriteStickers, colors = colors, modifier = Modifier.fillMaxWidth().weight(1f),
                onSend = onSendOwnSticker, onToggleFavourite = onToggleFavourite, onDelete = onDeleteSticker,
                emptyMessage = "Tap ★ on a sticker to add it here"
            )
            StickerTab.EXTERNAL -> {
                val folder = activeFolder
                if (folder == null) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No folder connected", fontSize = 13.sp, color = colors.subText)
                    }
                } else {
                    ExternalStickerGrid(
                        folder = folder,
                        source = externalStickerSource,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onSend = onSendExternalSticker
                    )
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(bottomPadding))
    }
}

private fun externalFolderLabel(folder: DocumentFile): String {
    val name = folder.name?.lowercase() ?: ""
    return when {
        "whatsapp" in name -> "WhatsApp"
        "telegram" in name -> "Telegram"
        else -> folder.name?.take(10) ?: "Folder"
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

@Composable
private fun ExternalStickerGrid(
    folder: DocumentFile,
    source: ExternalStickerSource,
    colors: KeyboardColors,
    modifier: Modifier,
    onSend: (String, String) -> Unit
) {
    var stickers by remember(folder.uri) { mutableStateOf<List<ExternalSticker>>(emptyList()) }
    var loaded by remember(folder.uri) { mutableStateOf(false) }

    LaunchedEffect(folder.uri) {
        stickers = source.listStickers(folder)
        loaded = true
    }

    when {
        !loaded -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        stickers.isEmpty() -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No stickers found in this folder", fontSize = 13.sp, color = colors.subText)
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(stickers, key = { it.uri.toString() }) { sticker ->
                val context = LocalContext.current
                val mimeType = remember(sticker.uri) {
                    context.contentResolver.getType(sticker.uri) ?: "image/webp"
                }
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keyBg)
                        .clickable { onSend(sticker.uri.toString(), mimeType) }
                ) {
                    StickerImage(
                        source = sticker.uri.toString(),
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        contentDescription = sticker.displayName
                    )
                }
            }
        }
    }
}
