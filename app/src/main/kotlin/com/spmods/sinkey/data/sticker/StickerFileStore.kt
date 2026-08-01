package com.spmods.sinkey.data.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
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

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Decodes the image at [sourceUri] (e.g. from a gallery picker result),
     * downscales it to fit within [MAX_DIMENSION_PX] on its longest side
     * (stickers are shown small — no reason to keep multi-megapixel photos
     * around), and saves it as a new PNG. Returns the absolute file path, or
     * null if the image couldn't be read.
     */
    fun saveFromImageUri(context: Context, sourceUri: Uri): String? {
        val bitmap = decodeScaledBitmap(context, sourceUri) ?: return null
        return try {
            writeBitmap(context, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Renders [text] onto a transparent square canvas and saves it as a new
     * PNG — this is what "Text Sticker" produces. Text auto-shrinks to fit
     * within the canvas width so longer phrases don't clip.
     */
    fun saveFromText(context: Context, text: String, textColor: Int = Color.WHITE): String? {
        if (text.isBlank()) return null
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
        // longer phrases still fit without being clipped.
        val maxTextWidth = size * 0.85f
        var textSizePx = size * 0.4f
        paint.textSize = textSizePx
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        while (bounds.width() > maxTextWidth && textSizePx > 12f) {
            textSizePx *= 0.9f
            paint.textSize = textSizePx
            paint.getTextBounds(text, 0, text.length, bounds)
        }

        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, size / 2f, yPos, paint)

        return try {
            writeBitmap(context, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Decodes an existing PNG sticker (one created before WebP export was
     * added) and writes its missing sibling WebP file. Returns false if the
     * PNG itself can't be read.
     */
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

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        // First pass: read dimensions only (inJustDecodeBounds), so we can
        // pick a sample size and never fully decode a huge original bitmap
        // into memory just to immediately downscale it.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null
        } catch (e: Exception) {
            // Most commonly a SecurityException (URI permission no longer
            // valid — see StickerPickerActivity's persistable-permission
            // fix) or an IOException from a provider that's since deleted
            // the underlying file. Either way, surfacing this as "can't
            // read the image" (return null) is correct; logging it keeps
            // the real cause diagnosable instead of a silent failure.
            android.util.Log.w("SinKey", "Couldn't read sticker source image bounds", e)
            return null
        }

        var sampleSize = 1
        val longestSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        while (longestSide / sampleSize > MAX_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null
        } catch (e: Exception) {
            android.util.Log.w("SinKey", "Couldn't read sticker source image", e)
            return null
        }

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
