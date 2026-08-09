package com.spmods.sinkey.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes and displays a sticker image from either a local file path
 * (user-created stickers) or a content:// Uri (external WhatsApp/Telegram
 * stickers accessed via SAF), off the main thread, with a small loading
 * spinner while decoding.
 *
 * This project has no image-loading library (Coil/Glide) — adding one is
 * unnecessary for what's needed here: a modest number of small (≤512px)
 * local images, not a scrolling feed of remote URLs, so a simple
 * BitmapFactory decode + in-memory LRU-free cache (just enough to avoid
 * redecoding on every recomposition of the same item) is sufficient.
 */
private val bitmapCache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean {
        // Cap the cache so long sticker-browsing sessions don't grow memory
        // unbounded; oldest-accessed entries are evicted first (access-order
        // LinkedHashMap, set via the constructor's `true` above).
        return size > 120
    }
}

private fun decode(context: Context, key: String): Bitmap? {
    synchronized(bitmapCache) {
        bitmapCache[key]?.let { return it }
    }
    val bitmap = if (key.startsWith("content://") || key.startsWith("file://")) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(key))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    } else {
        runCatching { BitmapFactory.decodeFile(key) }.getOrNull()
    }
    if (bitmap != null) {
        synchronized(bitmapCache) { bitmapCache[key] = bitmap }
    }
    return bitmap
}

@Composable
fun StickerImage(
    source: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(source) {
        bitmap = withContext(Dispatchers.IO) { decode(context, source) }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

/**
 * "My themes" custom keyboard background — a user-picked photo or GIF drawn
 * full-bleed (cropped to fill, like a wallpaper) behind the whole keyboard.
 *
 * Image / GIF Backgrounds: static images use the existing [decode]/
 * [bitmapCache] path via [Image]. Animated GIFs (detected by sniffing the
 * first bytes for the "GIF87a"/"GIF89a" header, since content:// Uris don't
 * reliably expose a file extension) are instead played with
 * [android.graphics.drawable.AnimatedImageDrawable] wrapped in Compose's
 * [AndroidView] — decoding a GIF into a single static [Bitmap] would only
 * ever show its first frame. AnimatedImageDrawable requires API 28+; below
 * that this falls back to the first frame as a static image via the same
 * [decode] path used for ordinary photos, so a GIF background never crashes
 * or renders blank on an older device, it just doesn't animate.
 *
 * Deliberately shows nothing (not even the loading spinner [StickerImage]
 * uses) while decoding — for a background image a brief blank/transparent
 * frame is unnoticeable and looks far better than a spinner flashing in
 * the middle of the keyboard, plus the underlying solid `colors.bg` this
 * sits in front of is already transparent so there's a coherent fallback
 * (nothing → keyboard's own board backgrounds show through) rather than a
 * jarring color swap once decoding finishes.
 */
@Composable
fun KeyboardCustomBackground(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isAnimatedGif by remember(uriString) { mutableStateOf<Boolean?>(null) }
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        val animated = withContext(Dispatchers.IO) { sniffIsGif(context, uriString) }
        isAnimatedGif = animated
        if (!animated || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            // Non-GIF, or GIF on a device too old for AnimatedImageDrawable
            // — either way a single static frame via the normal decode path.
            bitmap = withContext(Dispatchers.IO) { decode(context, uriString) }
        }
    }

    when {
        isAnimatedGif == true && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P -> {
            AnimatedGifBackground(uriString = uriString, modifier = modifier)
        }
        bitmap != null -> {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        else -> Unit
    }
}

/** Reads just enough bytes to check for the GIF magic header — no full decode needed to answer "is this a GIF". */
private fun sniffIsGif(context: Context, uriString: String): Boolean {
    return runCatching {
        val header = ByteArray(6)
        val stream = if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            context.contentResolver.openInputStream(Uri.parse(uriString))
        } else {
            java.io.FileInputStream(uriString)
        }
        stream?.use { it.read(header) }
        val ascii = String(header, Charsets.US_ASCII)
        ascii == "GIF87a" || ascii == "GIF89a"
    }.getOrDefault(false)
}

/**
 * Plays an animated GIF full-bleed using the platform's
 * AnimatedImageDrawable inside an AndroidView, since Compose's own Image
 * composable has no animated-GIF support — it only ever shows a single
 * ImageBitmap frame. API 28+ only; callers gate on the SDK check above.
 */
@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.P)
@Composable
private fun AnimatedGifBackground(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx -> android.widget.ImageView(ctx).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP } },
        update = { imageView ->
            runCatching {
                val source = if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(uriString))
                } else {
                    android.graphics.ImageDecoder.createSource(java.io.File(uriString))
                }
                val drawable = android.graphics.ImageDecoder.decodeDrawable(source)
                imageView.setImageDrawable(drawable)
                (drawable as? android.graphics.drawable.AnimatedImageDrawable)?.apply {
                    repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                    start()
                }
            }
        }
    )
}
