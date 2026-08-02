package com.spmods.sinkey.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R
import kotlin.math.max

/** One of a small fixed set of distinct-looking font families for sticker text — Compose's built-in generic families render distinctly enough without needing bundled font files. */
internal enum class StickerFontStyle(val label: String, val fontFamily: FontFamily, val fontWeight: FontWeight) {
    BOLD("Bold", FontFamily.Default, FontWeight.Black),
    CLASSIC("Classic", FontFamily.Serif, FontWeight.Bold),
    TYPEWRITER("Mono", FontFamily.Monospace, FontWeight.Bold),
    HANDWRITTEN("Script", FontFamily.Cursive, FontWeight.Normal),
    CLEAN("Clean", FontFamily.SansSerif, FontWeight.Medium)
}

/**
 * The mask shape applied to the sticker's image — chosen via the Crop
 * button next to Add Text. [cornerFraction] is used only by ROUNDED_SQUARE,
 * expressed as a fraction of the square canvas size (so it scales correctly
 * both in the small on-screen preview and the full-resolution final PNG,
 * the same resolution-independence trick used for text/image position).
 */
internal enum class StickerShape(val label: String, val cornerFraction: Float) {
    CIRCLE("Circle", cornerFraction = 0f),
    ROUNDED_SQUARE("Rounded", cornerFraction = 0.18f),
    SQUARE("Square", cornerFraction = 0f)
}

/**
 * Everything needed to (re)draw the sticker exactly as previewed — handed
 * to StickerFileStore.compositeImageSticker on save so the rendered PNG
 * matches this screen pixel-for-pixel (same square canvas, same relative
 * image transform and text position, both expressed as fractions of the
 * canvas so they're independent of this screen's actual on-device size).
 */
internal data class ImageStickerDraft(
    val imageScale: Float,
    val imageOffsetXFraction: Float,
    val imageOffsetYFraction: Float,
    val shape: StickerShape,
    val text: String,
    val textColor: Int,
    val textSizeFraction: Float,
    val textXFraction: Float,
    val textYFraction: Float,
    val fontStyle: StickerFontStyle,
    val outlineEnabled: Boolean
)

/**
 * Board.STICKER_EDIT — the "adjust image + add text" screen shown after an
 * image is picked for Image Sticker, matching WhatsApp's own sticker-maker
 * flow (pinch/drag to reposition the photo, tap Text to add a caption you
 * can drag into place, pick its colour/font/size, then Add to stickers).
 *
 * All position/scale state here is screen-size-independent (stored as
 * fractions of the square preview box), so [onSave] can hand off a draft
 * that [StickerFileStore.compositeImageSticker] re-renders at full sticker
 * resolution (512x512) without any of this composable's own pixel
 * measurements leaking into the saved file.
 */
@Composable
internal fun StickerImageEditorView(
    colors: KeyboardColors,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    imageBitmap: android.graphics.Bitmap,
    onSave: (ImageStickerDraft) -> Unit,
    onBack: () -> Unit
) {
    // previewSize is derived from targetContentHeight (not a fixed value)
    // so the circular preview scales with the actual screen height instead
    // of overflowing on short screens or looking tiny on tall ones.
    val previewSize = (targetContentHeight.value * 0.42f).coerceIn(150f, 260f).dp

    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) } // in px, within the preview box
    var previewBoxPx by remember { mutableStateOf(IntSize.Zero) }
    var shape by remember { mutableStateOf(StickerShape.CIRCLE) }
    var showShapePicker by remember { mutableStateOf(false) }

    var showTextEditor by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }
    var textSizeFraction by remember { mutableFloatStateOf(0.14f) } // relative to canvas size
    var textOffset by remember { mutableStateOf(Offset.Zero) } // in px, within the preview box, relative to center
    var fontStyle by remember { mutableStateOf(StickerFontStyle.BOLD) }
    var outlineEnabled by remember { mutableStateOf(true) }

    val swatches = remember {
        listOf(
            Color.White, Color.Black, Color(0xFFE53935), Color(0xFFFFB300),
            Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFFF6F00)
        )
    }

    val painter = remember(imageBitmap) { BitmapPainter(imageBitmap.asImageBitmap()) }

    fun save() {
        onSave(
            ImageStickerDraft(
                imageScale = imageScale,
                imageOffsetXFraction = if (previewBoxPx.width > 0) imageOffset.x / previewBoxPx.width else 0f,
                imageOffsetYFraction = if (previewBoxPx.height > 0) imageOffset.y / previewBoxPx.height else 0f,
                shape = shape,
                text = text,
                textColor = textColor.toArgb(),
                textSizeFraction = textSizeFraction,
                textXFraction = if (previewBoxPx.width > 0) textOffset.x / previewBoxPx.width else 0f,
                textYFraction = if (previewBoxPx.height > 0) textOffset.y / previewBoxPx.height else 0f,
                fontStyle = fontStyle,
                outlineEnabled = outlineEnabled
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = targetContentHeight)
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding)
    ) {
        // Close (X) button, top-right.
        Box(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, end = 14.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Circular preview canvas — pinch to zoom / drag to reposition
            // the photo, and (if text has been added) drag the text
            // independently.
            val previewShape = remember(shape, previewSize) {
                when (shape) {
                    StickerShape.CIRCLE -> CircleShape
                    StickerShape.SQUARE -> RoundedCornerShape(0.dp)
                    StickerShape.ROUNDED_SQUARE -> RoundedCornerShape(previewSize * shape.cornerFraction)
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(previewSize)
                    .clip(previewShape)
                    .background(Color(0xFF2B2B2B))
                    .onSizeChanged { previewBoxPx = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            imageScale = (imageScale * zoom).coerceIn(1f, 4f)
                            imageOffset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Sticker image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(previewShape)
                        .graphicsLayer(
                            scaleX = imageScale,
                            scaleY = imageScale,
                            translationX = imageOffset.x,
                            translationY = imageOffset.y
                        )
                )

                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = textColor,
                        fontFamily = fontStyle.fontFamily,
                        fontWeight = fontStyle.fontWeight,
                        fontSize = (previewSize.value * textSizeFraction).sp,
                        maxLines = 2,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    textOffset += dragAmount
                                }
                            }
                            .padding(4.dp)
                            .graphicsLayer(translationX = textOffset.x, translationY = textOffset.y)
                    )
                }
            }

            Text(
                text = if (text.isBlank()) "Pinch to zoom, drag to move the photo" else "Drag the text to position it",
                fontSize = 11.sp,
                color = colors.subText,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!showTextEditor && text.isBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.keyBg)
                            .clickable { showTextEditor = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.TextFields, contentDescription = null, tint = DeshGreen, modifier = Modifier.size(16.dp))
                        Text(text = "Add Text", fontSize = 13.sp, color = colors.keyText, fontWeight = FontWeight.Medium)
                    }

                    // Crop — toggles a row of shape choices (Circle /
                    // Rounded / Square) below; picking one re-clips both
                    // this preview and (via ImageStickerDraft.shape) the
                    // final saved sticker to match, see StickerFileStore.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showShapePicker) DeshGreen.copy(alpha = 0.18f) else colors.keyBg)
                            .clickable { showShapePicker = !showShapePicker }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Crop, contentDescription = null, tint = DeshGreen, modifier = Modifier.size(16.dp))
                        Text(text = "Crop", fontSize = 13.sp, color = colors.keyText, fontWeight = FontWeight.Medium)
                    }
                }

                if (showShapePicker) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StickerShape.entries.forEach { option ->
                            val selected = shape == option
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) DeshGreen else colors.keyBg)
                                    .clickable { shape = option }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else colors.keyText,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                StickerTextControls(
                    colors = colors,
                    text = text,
                    onTextChange = { text = it },
                    textColor = textColor,
                    onColorChange = { textColor = it },
                    swatches = swatches,
                    fontStyle = fontStyle,
                    onFontChange = { fontStyle = it },
                    textSizeFraction = textSizeFraction,
                    onTextSizeChange = { textSizeFraction = it },
                    outlineEnabled = outlineEnabled,
                    onOutlineChange = { outlineEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Add to stickers" — full-width primary action.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeshGreen)
                    .clickable { save() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Add to stickers", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Text style controls shown once "Add Text" is tapped: a lightweight text
 * entry field (still not the real InputConnection — see
 * StickerCreateView's top-level doc comment for why — reusing the same
 * approach isn't practical here alongside drag gestures on the preview, so
 * this uses a real Compose BasicTextField instead, which is safe here
 * because unlike StickerCreateView this whole screen is never the active
 * IME input target itself), font family chooser, colour swatches, a size
 * slider, and an outline/shadow toggle for legibility over busy photos.
 */
@Composable
private fun StickerTextControls(
    colors: KeyboardColors,
    text: String,
    onTextChange: (String) -> Unit,
    textColor: Color,
    onColorChange: (Color) -> Unit,
    swatches: List<Color>,
    fontStyle: StickerFontStyle,
    onFontChange: (StickerFontStyle) -> Unit,
    textSizeFraction: Float,
    onTextSizeChange: (Float) -> Unit,
    outlineEnabled: Boolean,
    onOutlineChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { new -> if (new.length <= 40) onTextChange(new) },
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyText, fontSize = 14.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(DeshGreen),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.keyBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(text = "Type sticker text…", fontSize = 14.sp, color = colors.subText)
                }
                inner()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Font family chooser — horizontally scrollable chip row.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StickerFontStyle.values().forEach { style ->
                val selected = style == fontStyle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) DeshGreen else colors.keyBg)
                        .clickable { onFontChange(style) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style.label,
                        fontSize = 12.sp,
                        fontFamily = style.fontFamily,
                        fontWeight = style.fontWeight,
                        color = if (selected) Color.White else colors.keyText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .clickable { onColorChange(swatch) },
                    contentAlignment = Alignment.Center
                ) {
                    if (swatch == textColor) {
                        Icon(
                            imageVector = Icons.Filled.FormatColorText,
                            contentDescription = "Selected",
                            modifier = Modifier.size(12.dp),
                            tint = if (swatch == Color.White || swatch == Color(0xFFFFB300)) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Outline toggle — helps legibility over busy/light photo areas.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (outlineEnabled) DeshGreen else colors.keyBg)
                    .clickable { onOutlineChange(!outlineEnabled) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Outline",
                    fontSize = 11.sp,
                    color = if (outlineEnabled) Color.White else colors.keyText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Size slider (compact, no Material Slider dependency assumptions —
        // a simple draggable row keeps this consistent with the rest of the
        // keyboard's minimal-dependency custom controls).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Size", fontSize = 11.sp, color = colors.subText, modifier = Modifier.width(32.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.keyBg)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onTextSizeChange(0.06f + fraction * (0.28f - 0.06f))
                        }
                    }
            ) {
                val knobFraction = ((textSizeFraction - 0.06f) / (0.28f - 0.06f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(max(0.04f, knobFraction))
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeshGreen)
                )
            }
        }
    }
}

