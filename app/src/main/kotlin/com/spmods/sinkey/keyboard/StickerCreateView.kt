package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
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

private enum class StickerCreateMode { CHOOSE, TEXT_COMPOSE }

/**
 * Board.STICKER_CREATE — mirrors the two-option "Create Sticker" screen
 * (Image Sticker / Text Sticker).
 *
 * Image Sticker calls [onPickImageRequested], which the caller wires to
 * SinKeyInputMethodService.pickImageForSticker (a system gallery picker
 * launched via a trampoline Activity, since an IME can't host an
 * ActivityResultLauncher itself). This board pops itself once that
 * completes, via KeyboardView's Board.STICKER_CREATE branch popping back
 * after stickerRepository.createFromImage runs.
 *
 * Text Sticker can't use a normal TextField: this Composable *is* the
 * keyboard, so there is no other IME available to type into a field shown
 * here — attempting that would just show ourselves again. Instead,
 * selecting Text Sticker switches this board's own content to
 * [StickerTextComposeView], which reuses MainKeyboardKeys directly with a
 * local onKey that appends into an in-memory draft string (never touching
 * the real InputConnection/target app), shown live in a preview box above
 * the keys — the same look StickerFileStore.saveFromText renders into the
 * actual sticker PNG.
 */
@Composable
internal fun StickerCreateView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    onPickImageRequested: () -> Unit,
    onTextSubmitted: (String) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(StickerCreateMode.CHOOSE) }

    when (mode) {
        StickerCreateMode.CHOOSE -> StickerCreateChooser(
            colors = colors,
            targetContentHeight = targetContentHeight,
            bottomPadding = bottomPadding,
            onImageSticker = onPickImageRequested,
            onTextSticker = { mode = StickerCreateMode.TEXT_COMPOSE },
            onBack = onBack
        )
        StickerCreateMode.TEXT_COMPOSE -> StickerTextComposeView(
            colors = colors,
            keyHeight = keyHeight,
            bottomPadding = bottomPadding,
            targetContentHeight = targetContentHeight,
            onSubmit = onTextSubmitted,
            onBack = { mode = StickerCreateMode.CHOOSE }
        )
    }
}

@Composable
private fun StickerCreateChooser(
    colors: KeyboardColors,
    targetContentHeight: Dp,
    bottomPadding: Dp,
    onImageSticker: () -> Unit,
    onTextSticker: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
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
                text = "Create Sticker",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CreateOptionCard(
                    icon = Icons.Filled.Image,
                    label = "Image Sticker",
                    colors = colors,
                    onClick = onImageSticker
                )
                CreateOptionCard(
                    icon = Icons.Filled.TextFields,
                    label = "Text Sticker",
                    colors = colors,
                    onClick = onTextSticker
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun CreateOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    colors: KeyboardColors,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keyBg)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = DeshGreen, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = colors.keyText, fontWeight = FontWeight.Medium)
    }
}

/**
 * The actual "type your sticker text" screen: a preview box on top (dark
 * neutral background so white bold text stays visible regardless of the
 * keyboard's own theme, approximating what StickerFileStore.saveFromText
 * will render) and the normal QWERTY keys underneath, wired to a local
 * draft buffer instead of the real InputConnection.
 */
@Composable
private fun StickerTextComposeView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf(true) }

    val previewHeight = 90.dp
    val headerHeight = 44.dp

    Column(modifier = Modifier.fillMaxWidth().background(colors.bg)) {
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
                text = "Text Sticker",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (draft.isNotBlank()) DeshGreen else colors.keyBg)
                    .clickable(enabled = draft.isNotBlank()) { onSubmit(draft) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Save",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (draft.isNotBlank()) Color.White else colors.subText
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2B2B2B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = draft.ifBlank { "Type your sticker text…" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (draft.isBlank()) Color(0xFF888888) else Color.White,
                maxLines = 2
            )
        }

        // Reuses the real keyboard layout, but routes keys into the local
        // `draft` buffer instead of the InputConnection — see this file's
        // top-level doc comment for why a real TextField isn't possible here.
        MainKeyboardKeys(
            currentLanguage = "en",
            shift = shift,
            shiftLocked = false,
            onShiftStateChange = { newState ->
                shift = newState != com.spmods.sinkey.ime.SinKeyInputMethodService.ShiftState.OFF
            },
            keyHeight = keyHeight,
            keyShape = RoundedCornerShape(8.dp),
            bottomPadding = bottomPadding,
            colors = colors,
            onKey = { key ->
                when (key) {
                    "BACKSPACE" -> if (draft.isNotEmpty()) draft = draft.dropLast(1)
                    "SPACE" -> draft += " "
                    "ENTER" -> if (draft.isNotBlank()) onSubmit(draft)
                    "SWITCH_KEYBOARD", "SYMBOLS", "ABC" -> Unit // not meaningful in this compose-only context
                    else -> if (key.codePointCount(0, key.length) == 1) {
                        draft += if (shift) key.uppercase() else key.lowercase()
                        if (shift) shift = false // one-shot shift, mirrors main keyboard's default feel
                    }
                }
            },
            onSymbols = {},
            onEmojiPicker = {},
            onLangTooltip = {},
            imeAction = android.view.inputmethod.EditorInfo.IME_ACTION_NONE
        )
    }
}
