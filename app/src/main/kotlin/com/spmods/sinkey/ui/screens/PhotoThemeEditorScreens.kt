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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.keyboard.KeyboardView
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
    // Pan/zoom state for the photo *inside* the crop box itself — matching
    // the reference recording exactly: the crop box IS the photo container
    // (ContentScale.Crop-filled, full brightness, no dimming), with only
    // grid lines + a white border drawn on top of it. There's no separate
    // full-screen preview with a dimmed mask around a smaller window.
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableStateOf(Size.Zero) }

    // Keyboard-shaped crop window — matches the reference recording's box
    // (roughly square-ish, slightly taller than wide).
    val cropBoxWidthFraction = 0.82f

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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(cropBoxWidthFraction)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .pointerInput(sourceBitmap) {
                        detectTransformGestures { _, panDelta, zoomDelta, _ ->
                            scale = (scale * zoomDelta).coerceIn(1f, 5f)
                            offset += panDelta
                        }
                    }
                    .onSizeChangedPx { boxSizePx = it }
            ) {
                Image(
                    bitmap = sourceBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayerPanZoom(scale, offset)
                )

                // 3x3 grid lines + white border, drawn directly over the
                // photo — no dimming, no separate mask layer, so the photo
                // itself stays fully visible under the grid.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = Color.White.copy(alpha = 0.85f)
                    for (i in 1..2) {
                        val x = size.width * i / 3f
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        val y = size.height * i / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    drawRect(
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Button(
            onClick = {
                onNext(
                    cropBitmap(
                        source = sourceBitmap,
                        boxSizePx = boxSizePx,
                        scale = scale,
                        offset = offset
                    )
                )
            },
            enabled = boxSizePx.width > 0f,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Step 2 of the "My themes" custom photo flow: shows the actual keyboard
 * (KeyboardView — the same composable the real IME and MainActivity's own
 * "preview" FAB use, display-only here via no-op onKey/onSuggestionSelected)
 * with the cropped photo as its live background, plus Show key borders /
 * Blur / Brightness / Key opacity controls. Previously this screen drew a
 * hand-built QWERTY mock (MockKeyboardRows) that only approximated the real
 * keyboard's look — swapping in KeyboardView itself means the preview is
 * pixel-for-pixel what the user will actually see while typing, including
 * the toolbar/suggestion strip, key shapes, and language-specific glyphs,
 * not just three rows of plain Latin letters. The in-progress photo/blur/
 * brightness/opacity aren't saved to PreferencesManager until Done is
 * tapped, so they're threaded into KeyboardView's previewXxx override
 * params (see that function's doc comment) rather than relying on its
 * normal prefs-driven path, which would still show the *previous* theme.
 * Done commits [blur]/[brightness] and the bitmap together via [onDone].
 */
@Composable
fun PhotoEditThemeScreen(
    croppedBitmap: Bitmap,
    initialShowKeyBorders: Boolean,
    initialBlur: Float,
    initialBrightness: Float,
    // Key opacity, 0f..1f, default 1f — how solid each key's own background
    // is drawn on top of the photo. Lower values let more of the photo show
    // through each key (not just the gaps between them, which the photo
    // already shows through regardless of this value).
    initialKeyOpacity: Float = 1f,
    // Threaded in so the KeyboardView preview matches what the user would
    // actually see typing — same values MainActivity's own keyboard-preview
    // FAB passes to KeyboardView (see that call site).
    currentLanguage: String = "si",
    isDark: Boolean = false,
    onBack: () -> Unit,
    onDone: (showKeyBorders: Boolean, blur: Float, brightness: Float, keyOpacity: Float) -> Unit
) {
    var showKeyBorders by remember { mutableStateOf(initialShowKeyBorders) }
    var blur by remember { mutableStateOf(initialBlur) }
    var brightness by remember { mutableStateOf(initialBrightness) }
    var keyOpacity by remember { mutableStateOf(initialKeyOpacity) }

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

        // Real keyboard preview — KeyboardView itself, not a mock. Clipped
        // to the same rounded box the mock used to sit in; live blur/
        // brightness/opacity/border edits recompose this immediately since
        // they're passed straight through as previewXxx overrides.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            KeyboardView(
                currentLanguage = currentLanguage,
                isDark = isDark,
                suggestions = emptyList(),
                onSuggestionSelected = { _, _ -> /* preview — no input dispatch */ },
                onKey = { /* preview — no input dispatch */ },
                previewBackgroundBitmap = croppedBitmap,
                previewBlur = blur,
                previewBrightness = brightness,
                previewKeyOpacity = keyOpacity,
                previewShowKeyBorders = showKeyBorders
            )
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

            Text(
                "Key opacity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Text(
                "Lower this so the photo shows through each key, not just the gaps between them",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = keyOpacity,
                onValueChange = { keyOpacity = it },
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Button(
            onClick = { onDone(showKeyBorders, blur, brightness, keyOpacity) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
 * Crops [source] to exactly what's visible inside the crop box in
 * [PhotoCropScreen] — the box IS the photo's container (ContentScale.Crop
 * + pan/zoom applied directly inside it), so this is a direct inverse of
 * that fit: no separate "box position within a bigger preview" step needed
 * since [boxSizePx] already *is* that box.
 */
private fun cropBitmap(
    source: Bitmap,
    boxSizePx: Size,
    scale: Float,
    offset: Offset
): Bitmap {
    if (boxSizePx.width <= 0f || boxSizePx.height <= 0f) return source

    // Base ContentScale.Crop fit of `source` into the box (before pan/zoom)
    // — scale up uniformly until the image covers the box on both axes,
    // then center.
    val baseScale = max(
        boxSizePx.width / source.width.toFloat(),
        boxSizePx.height / source.height.toFloat()
    )
    val baseDrawWidth = source.width * baseScale
    val baseDrawHeight = source.height * baseScale
    val baseLeft = (boxSizePx.width - baseDrawWidth) / 2f
    val baseTop = (boxSizePx.height - baseDrawHeight) / 2f

    // Total effective scale/position including the user's pinch-zoom
    // (applied from the box's center) and pan.
    val totalScale = baseScale * scale
    val centerX = boxSizePx.width / 2f
    val centerY = boxSizePx.height / 2f
    val drawLeft = centerX - (centerX - baseLeft) * scale + offset.x
    val drawTop = centerY - (centerY - baseTop) * scale + offset.y

    // Map the box's four corners (0,0)..(boxWidth,boxHeight) back into the
    // original bitmap's pixel space.
    val srcLeft = ((0f - drawLeft) / totalScale).coerceIn(0f, source.width.toFloat())
    val srcTop = ((0f - drawTop) / totalScale).coerceIn(0f, source.height.toFloat())
    val srcRight = ((boxSizePx.width - drawLeft) / totalScale).coerceIn(0f, source.width.toFloat())
    val srcBottom = ((boxSizePx.height - drawTop) / totalScale).coerceIn(0f, source.height.toFloat())

    val cropW = (srcRight - srcLeft).toInt().coerceAtLeast(1)
    val cropH = (srcBottom - srcTop).toInt().coerceAtLeast(1)
    val safeCropW = min(cropW, source.width - srcLeft.toInt())
    val safeCropH = min(cropH, source.height - srcTop.toInt())
    if (safeCropW <= 0 || safeCropH <= 0) return source

    return runCatching {
        Bitmap.createBitmap(source, srcLeft.toInt(), srcTop.toInt(), safeCropW, safeCropH)
    }.getOrDefault(source)
}
