package com.spmods.sinkey.data.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Reads/writes the actual PNG pixel data for user-created stickers
 * (StickerEntity.filePath points at files this class creates). Stored under
 * the app's private files dir — no storage permission needed for these,
 * since the app both writes and reads its own sandbox directory.
 *
 * Kept a plain object (not injected) since it has no state beyond the
 * filesystem itself, matching how simple the rest of this app's data layer
 * is (PreferencesManager, ClipRepository) — no DI framework in use here.
 */
object StickerFileStore {

    private const val DIR_NAME = "stickers"
    private const val MAX_DIMENSION_PX = 512 // stickers don't need to be larger than this on any screen
    private const val MAX_STICKER_BYTES = 100 * 1024 // WhatsApp rejects static stickers over 100KB
    // Matches StickerImageEditorView's own text field cap (see its
    // BasicTextField onValueChange). Text Sticker's draft buffer
    // (StickerTextComposeView) previously had no equivalent cap, so very
    // long captions could reach saveFromText() uncapped.
    private const val MAX_TEXT_STICKER_CHARS = 40

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Decodes the image at [sourceFile] (a temp file StickerPickerActivity
     * writes immediately after picking — see that class's doc comment for
     * why a Uri isn't used here), downscales it to fit within
     * [MAX_DIMENSION_PX] on its longest side (stickers are shown small — no
     * reason to keep multi-megapixel photos around), and saves it as a new
     * PNG. Deletes [sourceFile] afterwards either way, since it's a
     * throwaway temp copy. Returns the absolute path of the new sticker
     * PNG, or null if the image couldn't be decoded.
     */
    fun saveFromImageFile(context: Context, sourceFile: File): String? {
        return try {
            val bitmap = decodeScaledBitmap(sourceFile) ?: return null
            try {
                writeBitmap(context, bitmap)
            } finally {
                bitmap.recycle()
            }
        } finally {
            runCatching { sourceFile.delete() }
        }
    }

    /**
     * Renders [text] onto a transparent square canvas and saves it as a new
     * PNG — this is what "Text Sticker" produces. Text auto-shrinks to fit
     * within the canvas width so longer phrases don't clip.
     */
    fun saveFromText(context: Context, text: String, textColor: Int = Color.WHITE): String? {
        // StickerTextComposeView's draft buffer has no length cap of its
        // own (unlike StickerImageEditorView's 40-char text field), so a
        // long caption could reach here uncapped. Truncated defensively
        // here too — belt and braces, and it keeps this function safe even
        // if it's ever called from elsewhere without that same cap.
        val safeText = text.take(MAX_TEXT_STICKER_CHARS)
        if (safeText.isBlank()) return null

        return try {
            val size = MAX_DIMENSION_PX
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Fully transparent background — PNG preserves alpha, so this
            // becomes a "sticker" (no opaque box around the text) once committed.
            canvas.drawColor(Color.TRANSPARENT)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            // Start large and shrink until the text fits within ~85% of the
            // canvas width, so short text (e.g. "Hi") stays big and bold while
            // longer phrases still fit without being clipped. Floored at 12f
            // (not 0f/negative) — Paint.textSize must stay positive, and
            // without a floor a very long string could shrink the loop
            // toward zero/negative before bounds.width() ever drops below
            // maxTextWidth, which is exactly the kind of edge case that used
            // to reach drawText() unguarded by any try/catch below.
            val maxTextWidth = size * 0.85f
            var textSizePx = size * 0.4f
            paint.textSize = textSizePx
            val bounds = Rect()
            paint.getTextBounds(safeText, 0, safeText.length, bounds)
            while (bounds.width() > maxTextWidth && textSizePx > 12f) {
                textSizePx *= 0.9f
                paint.textSize = textSizePx
                paint.getTextBounds(safeText, 0, safeText.length, bounds)
            }

            val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(safeText, size / 2f, yPos, paint)

            try {
                writeBitmap(context, bitmap)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Mirrors compositeImageSticker's own try/finally below: fail
            // gracefully back to the caller (StickerRepository.createFromText
            // already treats a null path as "couldn't save that sticker" and
            // shows a Toast) instead of taking the whole keyboard process
            // down. This function previously had no such guard at all.
            null
        }
    }

    /**
     * Renders the final Image Sticker: the picked photo, cropped/zoomed/
     * positioned exactly as previewed in StickerImageEditorView, masked to
     * [shape] (Circle / Rounded / Square, chosen via the Crop button —
     * mirrors the same clip applied to the on-screen preview), with an
     * optional text caption drawn on top at the position/size/colour/font
     * the user chose there. All positioning inputs are fractions of the
     * square preview box (see ImageStickerDraft), so this reproduces that
     * on-screen preview independent of whatever pixel size the editor
     * screen actually rendered at.
     *
     * [sourceFile] is the temp file StickerPickerActivity wrote when the
     * image was picked (same file the editor screen decoded to show its
     * preview) — deleted afterwards either way, same as saveFromImageFile.
     */
    fun compositeImageSticker(
        context: Context,
        sourceFile: File,
        imageScale: Float,
        imageOffsetXFraction: Float,
        imageOffsetYFraction: Float,
        shape: com.spmods.sinkey.keyboard.StickerShape,
        text: String,
        textColor: Int,
        textSizeFraction: Float,
        textXFraction: Float,
        textYFraction: Float,
        fontTypeface: android.graphics.Typeface,
        outlineEnabled: Boolean
    ): String? {
        return try {
            val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
            val size = MAX_DIMENSION_PX
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.TRANSPARENT)

            // Center-crop the source to a square first (same baseline as
            // ContentScale.Crop in the editor's preview Image composable),
            // then apply the user's additional pinch-zoom/drag on top —
            // this mirrors exactly what ContentScale.Crop + graphicsLayer
            // scale/translation produced on screen.
            val baseCropSize = minOf(decoded.width, decoded.height)
            val baseLeft = (decoded.width - baseCropSize) / 2
            val baseTop = (decoded.height - baseCropSize) / 2
            val squared = Bitmap.createBitmap(decoded, baseLeft, baseTop, baseCropSize, baseCropSize)
            if (squared !== decoded) decoded.recycle()

            val destSize = size.toFloat()
            val drawSize = destSize * imageScale
            // Editor offsets are in on-screen px within a `previewSize`-sized
            // box; re-expressing them as fractions of that same box (done by
            // the caller before this function runs) makes them resolution-
            // independent, so multiplying by this function's own destSize
            // reproduces the same *relative* position at full sticker res.
            val drawLeft = (destSize - drawSize) / 2f + imageOffsetXFraction * destSize
            val drawTop = (destSize - drawSize) / 2f + imageOffsetYFraction * destSize
            val destRect = RectF(drawLeft, drawTop, drawLeft + drawSize, drawTop + drawSize)

            // Clip to the chosen shape (same shape the user picked via the
            // Crop button, mirrored exactly from the editor's preview clip
            // — see StickerImageEditorView's previewShape) instead of the
            // full square canvas, so the saved PNG's transparent-background
            // mask matches what was previewed.
            val clipPath = android.graphics.Path().apply {
                when (shape) {
                    com.spmods.sinkey.keyboard.StickerShape.CIRCLE ->
                        addCircle(destSize / 2f, destSize / 2f, destSize / 2f, android.graphics.Path.Direction.CW)
                    com.spmods.sinkey.keyboard.StickerShape.SQUARE ->
                        addRect(0f, 0f, destSize, destSize, android.graphics.Path.Direction.CW)
                    com.spmods.sinkey.keyboard.StickerShape.ROUNDED_SQUARE -> {
                        val radius = destSize * shape.cornerFraction
                        addRoundRect(0f, 0f, destSize, destSize, radius, radius, android.graphics.Path.Direction.CW)
                    }
                }
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(squared, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
            squared.recycle()

            if (text.isNotBlank()) {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = fontTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = destSize * textSizeFraction
                }
                val cx = destSize / 2f + textXFraction * destSize
                val cy = destSize / 2f + textYFraction * destSize
                val textBounds = Rect()
                fillPaint.getTextBounds(text, 0, text.length, textBounds)
                val baselineY = cy - (fillPaint.descent() + fillPaint.ascent()) / 2f

                if (outlineEnabled) {
                    val outlinePaint = Paint(fillPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = destSize * textSizeFraction * 0.08f
                        color = if (isLightColor(textColor)) Color.BLACK else Color.WHITE
                    }
                    canvas.drawText(text, cx, baselineY, outlinePaint)
                }
                canvas.drawText(text, cx, baselineY, fillPaint)
            }

            try {
                writeBitmap(context, output)
            } finally {
                output.recycle()
            }
        } finally {
            runCatching { sourceFile.delete() }
        }
    }

    /** Simple luminance check used to pick a contrasting outline colour for text (dark outline on light text, light outline on dark text). */
    private fun isLightColor(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luminance > 0.6
    }

    /**
     * Decodes the image at [sourceFile] purely for preview purposes — used
     * by Board.STICKER_EDIT to show the picked photo in the editor before
     * any crop/zoom/text has been applied. Downscaled the same way the
     * final sticker is, so the preview and the saved output share the same
     * effective resolution. Unlike [saveFromImageFile] / [compositeImageSticker],
     * this does NOT delete [sourceFile] — the editor screen needs it to
     * still exist when the user taps "Add to stickers", which is what
     * finally calls [compositeImageSticker] and deletes it then.
     */
    fun decodePreviewBitmap(sourceFile: File): Bitmap? = decodeScaledBitmap(sourceFile)


    fun backfillWebp(pngPath: String): Boolean {
        val pngFile = File(pngPath)
        if (!pngFile.exists()) return false
        val bitmap = BitmapFactory.decodeFile(pngPath) ?: return false
        return try {
            writeWhatsAppWebp(bitmap, File(webpPathFor(pngPath)))
            true
        } finally {
            bitmap.recycle()
        }
    }

    /** Deletes the PNG file backing a sticker, and its sibling WebP if present. Safe to call even if either file is already gone. */
    fun deleteFile(filePath: String) {
        runCatching { File(filePath).delete() }
        runCatching { File(webpPathFor(filePath)).delete() }
    }

    private fun decodeScaledBitmap(file: File): Bitmap? {
        if (!file.exists()) return null

        // First pass: read dimensions only (inJustDecodeBounds), so we can
        // pick a sample size and never fully decode a huge original bitmap
        // into memory just to immediately downscale it.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        var sampleSize = 1
        val longestSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        while (longestSide / sampleSize > MAX_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

        // Center-crop to a square, then scale to exactly MAX_DIMENSION_PX —
        // keeps every sticker's aspect ratio consistent in the grid.
        val cropSize = minOf(decoded.width, decoded.height)
        val left = (decoded.width - cropSize) / 2
        val top = (decoded.height - cropSize) / 2
        val cropped = Bitmap.createBitmap(decoded, left, top, cropSize, cropSize)
        if (cropped !== decoded) decoded.recycle()

        if (cropped.width == MAX_DIMENSION_PX) return cropped
        val scaled = Bitmap.createScaledBitmap(cropped, MAX_DIMENSION_PX, MAX_DIMENSION_PX, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    private fun writeBitmap(context: Context, bitmap: Bitmap): String {
        val id = UUID.randomUUID().toString()
        val pngFile = File(dir(context), "$id.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        // Also write a WhatsApp-compliant WebP copy alongside the PNG. WhatsApp's
        // sticker pipeline (StickerContentProvider) requires WebP — it will not
        // accept a PNG sticker, which is why sending via commitContent as PNG
        // gets treated as a generic photo attachment instead of a real sticker.
        // Same UUID, .webp extension, so callers can derive one path from the
        // other (see StickerFileStore.webpPathFor).
        writeWhatsAppWebp(bitmap, File(dir(context), "$id.webp"))
        return pngFile.absolutePath
    }

    /**
     * Given a sticker's stored PNG path, returns the path of its sibling
     * WhatsApp-ready WebP file (same UUID, .webp extension). Does not check
     * the file actually exists — callers that need that should check
     * File(path).exists() (older stickers created before WebP export was
     * added won't have one).
     */
    fun webpPathFor(pngPath: String): String = pngPath.removeSuffix(".png") + ".webp"

    /**
     * Re-encodes [bitmap] to satisfy WhatsApp's static-sticker requirements:
     * exactly 512x512 WebP, RGBA, under 100KB. WhatsApp's Sticker Pack
     * content provider validates these at "Add to WhatsApp" time and will
     * silently refuse the whole pack if any sticker fails them, so this is
     * not just a nice-to-have — it's required for stickers to work at all.
     */
    private fun writeWhatsAppWebp(source: Bitmap, outFile: File) {
        val sized = if (source.width == MAX_DIMENSION_PX && source.height == MAX_DIMENSION_PX) {
            source
        } else {
            Bitmap.createScaledBitmap(source, MAX_DIMENSION_PX, MAX_DIMENSION_PX, true)
        }
        var quality = 90
        var bytes: ByteArray
        do {
            val stream = java.io.ByteArrayOutputStream()
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
            sized.compress(format, quality, stream)
            bytes = stream.toByteArray()
            quality -= 15
        } while (bytes.size > MAX_STICKER_BYTES && quality > 10)

        FileOutputStream(outFile).use { it.write(bytes) }
        if (sized !== source) sized.recycle()
    }
}
