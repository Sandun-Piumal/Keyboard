package com.spmods.sinkey.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Step 1 of the "My themes" custom photo flow, after the system photo
 * picker returns a Uri: shows the picked photo full-bleed with a draggable/
 * pinchable keyboard-shaped crop window overlaid (matching the reference
 * "Adjust the frame" screen — a WhatsApp-chat mock behind a 3x3 grid crop
 * box shaped like the actual keyboard's aspect ratio). Tapping Next commits
 * the crop and hands the cropped Bitmap to [onNext].
 *
 * The crop box itself is fixed in size/position (centered, a constant
 * fraction of the screen) — what moves is the *photo* underneath it via
 * pan/zoom, exactly like the reference recording (the palm-tree sunset photo
 * slides and scales under a static grid window). This is simpler and more
 * predictable than a resizable crop box, and matches what was recorded.
 */
@Composable
fun PhotoCropScreen(
    sourceBitmap: Bitmap,
    onBack: () -> Unit,
    onNext: (Bitmap) -> Unit
) {
    // Pan/zoom state for the photo underneath the fixed crop window, in the
    // crop preview's own local px space (see onGloballyPositioned below for
    // where cropBoxSizePx/previewSizePx get filled in).
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var previewSizePx by remember { mutableStateOf(Size.Zero) }

    // Keyboard-shaped crop window — a tall, narrow phone-keyboard aspect
    // roughly matching the reference screenshot's crop box (about 0.62 of
    // the preview's width, centered, keyboard-ish 1.62:1 height:width).
    val cropBoxWidthFraction = 0.62f

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Adjust the frame",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .pointerInput(sourceBitmap) {
                    detectTransformGestures { _, panDelta, zoomDelta, _ ->
                        scale = (scale * zoomDelta).coerceIn(1f, 5f)
                        offset += panDelta
                    }
                }
                .onSizeChangedPx { previewSizePx = it },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = sourceBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayerPanZoom(scale, offset)
            )

            // Crop window + 3x3 grid lines + dimmed surroundings, drawn as a
            // single Canvas overlay so the grid lines sit exactly on the
            // crop box edges regardless of preview size.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * cropBoxWidthFraction
                val boxHeight = boxWidth * 1.62f
                val left = (size.width - boxWidth) / 2f
                val top = (size.height - boxHeight) / 2f

                // Dim everything outside the crop box.
                drawRect(color = Color.Black.copy(alpha = 0.55f))
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // 3x3 grid lines inside the box.
                val gridColor = Color.White.copy(alpha = 0.85f)
                for (i in 1..2) {
                    val x = left + boxWidth * i / 3f
                    drawLine(gridColor, Offset(x, top), Offset(x, top + boxHeight), strokeWidth = 1.dp.toPx())
                    val y = top + boxHeight * i / 3f
                    drawLine(gridColor, Offset(left, y), Offset(left + boxWidth, y), strokeWidth = 1.dp.toPx())
                }
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        Button(
            onClick = {
                onNext(
                    cropBitmap(
                        source = sourceBitmap,
                        previewSizePx = previewSizePx,
                        scale = scale,
                        offset = offset,
                        cropBoxWidthFraction = cropBoxWidthFraction
                    )
                )
            },
            enabled = previewSizePx.width > 0f,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Step 2 of the "My themes" custom photo flow: shows a mock keyboard with
 * the cropped photo as its background (WhatsApp-chat mock above it, exactly
 * matching the reference "Edit theme" screen), plus Show key borders /
 * Blur / Brightness controls. Done commits [blur]/[brightness] and the
 * bitmap together via [onDone].
 */
@Composable
fun PhotoEditThemeScreen(
    croppedBitmap: Bitmap,
    initialShowKeyBorders: Boolean,
    initialBlur: Float,
    initialBrightness: Float,
    onBack: () -> Unit,
    onDone: (showKeyBorders: Boolean, blur: Float, brightness: Float) -> Unit
) {
    var showKeyBorders by remember { mutableStateOf(initialShowKeyBorders) }
    var blur by remember { mutableStateOf(initialBlur) }
    var brightness by remember { mutableStateOf(initialBrightness) }

    val brightnessDelta = (brightness - 0.5f) * 2f * 255f
    val brightnessFilter = remember(brightnessDelta) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessDelta,
                    0f, 1f, 0f, 0f, brightnessDelta,
                    0f, 0f, 1f, 0f, brightnessDelta,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Edit theme",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Mock keyboard preview — WhatsApp chat header mock above a plain
        // QWERTY grid, background photo showing through with the live
        // blur/brightness applied, matching the reference screenshot.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(0.95f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = croppedBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = brightnessFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (blur > 0.01f) it.blur(20.dp * blur) else it }
            )
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "WhatsApp",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f))
                MockKeyboardRows()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Show key borders", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Show borders for keys",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = showKeyBorders, onCheckedChange = { showKeyBorders = it })
            }

            Text(
                "Blur",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )
            Slider(value = blur, onValueChange = { blur = it })

            Text(
                "Brightness",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Button(
            onClick = { onDone(showKeyBorders, blur, brightness) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A plain, non-interactive QWERTY row mock for the Edit theme preview — visual only. */
@Composable
private fun MockKeyboardRows() {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ch.toString(), color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** Applies [scale] and pan [offset] (in preview-local px) as a transform. */
private fun Modifier.graphicsLayerPanZoom(scale: Float, offset: Offset): Modifier =
    this
        .scale(scale)
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(offset.x.toInt(), offset.y.toInt())
            }
        }

/** Reports the composable's measured size in px via [onSize], for crop-math below. */
private fun Modifier.onSizeChangedPx(onSize: (Size) -> Unit): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            onSize(Size(placeable.width.toFloat(), placeable.height.toFloat()))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
    )

/**
 * Crops [source] to whatever the fixed crop window in [PhotoCropScreen]
 * is currently showing, given the photo's current [scale]/[offset] within
 * a [previewSizePx]-sized preview box (ContentScale.Crop-filled, then
 * panned/zoomed on top of that).
 */
private fun cropBitmap(
    source: Bitmap,
    previewSizePx: Size,
    scale: Float,
    offset: Offset,
    cropBoxWidthFraction: Float
): Bitmap {
    if (previewSizePx.width <= 0f || previewSizePx.height <= 0f) return source

    // Step 1: figure out the base ContentScale.Crop fit of `source` into
    // the preview box (before any user pan/zoom) — the same math
    // ContentScale.Crop itself uses: scale up uniformly until the image
    // covers the box on both axes, then center.
    val baseScale = max(
        previewSizePx.width / source.width.toFloat(),
        previewSizePx.height / source.height.toFloat()
    )
    val baseDrawWidth = source.width * baseScale
    val baseDrawHeight = source.height * baseScale
    val baseLeft = (previewSizePx.width - baseDrawWidth) / 2f
    val baseTop = (previewSizePx.height - baseDrawHeight) / 2f

    // Step 2: total effective scale/position on top of that base fit,
    // including the user's pinch-zoom (applied from the preview's center)
    // and pan.
    val totalScale = baseScale * scale
    val centerX = previewSizePx.width / 2f
    val centerY = previewSizePx.height / 2f
    // Where the image's top-left ends up on screen after the scale-from-
    // center transform plus pan.
    val drawLeft = centerX - (centerX - baseLeft) * scale + offset.x
    val drawTop = centerY - (centerY - baseTop) * scale + offset.y

    // Step 3: crop window position (matches PhotoCropScreen's Canvas math).
    val boxWidth = previewSizePx.width * cropBoxWidthFraction
    val boxHeight = boxWidth * 1.62f
    val boxLeft = (previewSizePx.width - boxWidth) / 2f
    val boxTop = (previewSizePx.height - boxHeight) / 2f

    // Step 4: map the crop window from preview-space back into the
    // original bitmap's pixel space.
    val srcLeft = ((boxLeft - drawLeft) / totalScale).coerceIn(0f, source.width.toFloat())
    val srcTop = ((boxTop - drawTop) / totalScale).coerceIn(0f, source.height.toFloat())
    val srcRight = ((boxLeft + boxWidth - drawLeft) / totalScale).coerceIn(0f, source.width.toFloat())
    val srcBottom = ((boxTop + boxHeight - drawTop) / totalScale).coerceIn(0f, source.height.toFloat())

    val cropW = (srcRight - srcLeft).toInt().coerceAtLeast(1)
    val cropH = (srcBottom - srcTop).toInt().coerceAtLeast(1)
    val safeCropW = min(cropW, source.width - srcLeft.toInt())
    val safeCropH = min(cropH, source.height - srcTop.toInt())
    if (safeCropW <= 0 || safeCropH <= 0) return source

    return runCatching {
        Bitmap.createBitmap(source, srcLeft.toInt(), srcTop.toInt(), safeCropW, safeCropH)
    }.getOrDefault(source)
}
