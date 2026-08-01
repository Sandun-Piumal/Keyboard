package com.spmods.sinkey.data.sticker

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * User-created stickers (Board.STICKER's "Create Sticker" flow: Image
 * Sticker + Text Sticker), their favourite state, and deletion. Backed by
 * Room (metadata) + app-private PNG files (pixel data) via StickerFileStore.
 *
 * For read-only WhatsApp/Telegram folder stickers, see
 * WhatsApp/Telegram sticker folder access was tried and removed (see
 * StickerBoardView's doc comment) — only stickers created in-app live here.
 */
class StickerRepository(private val context: Context) {
    private val dao = StickerDatabase.getInstance(context).stickerDao()

    val all: Flow<List<StickerEntity>> = dao.observeAll()
    val favourites: Flow<List<StickerEntity>> = dao.observeFavourites()

    /** Creates a sticker from a picked gallery image. Returns true on success. */
    suspend fun createFromImage(sourceUri: Uri): Boolean {
        val path = StickerFileStore.saveFromImageUri(context, sourceUri) ?: return false
        dao.insert(StickerEntity(filePath = path, source = StickerEntity.SOURCE_IMAGE))
        refreshTrayIcon()
        return true
    }

    /** Creates a sticker by rendering [text] onto a transparent PNG. Returns true on success. */
    suspend fun createFromText(text: String, textColor: Int = android.graphics.Color.WHITE): Boolean {
        val path = StickerFileStore.saveFromText(context, text, textColor) ?: return false
        dao.insert(StickerEntity(filePath = path, source = StickerEntity.SOURCE_TEXT))
        refreshTrayIcon()
        return true
    }

    suspend fun setFavourite(filePath: String, favourite: Boolean) = dao.setFavourite(filePath, favourite)

    suspend fun delete(filePath: String) {
        dao.delete(filePath)
        StickerFileStore.deleteFile(filePath)
        refreshTrayIcon()
    }

    /**
     * Regenerates the WhatsApp pack tray icon from the current newest
     * sticker (matching [dao.observeAll]'s ordering), so "Add to WhatsApp"
     * always shows an icon that reflects what's actually in the pack right
     * now instead of a stale or missing one. No-ops quietly if there are no
     * stickers yet or the source PNG can't be decoded — the tray icon is
     * only actually needed once the user triggers [requestAddToWhatsApp].
     */
    private suspend fun refreshTrayIcon() {
        // dao.observeAll() is a Flow; for a one-shot read here, go through
        // the blocking accessor used by the content provider instead of
        // collecting/canceling a Flow for a single value.
        val list = dao.getAllBlocking()
        val first = list.firstOrNull() ?: return
        val bitmap = android.graphics.BitmapFactory.decodeFile(first.filePath) ?: return
        try {
            StickerFileStore.writeTrayIcon(context, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Fires WhatsApp's "Add to WhatsApp" broadcast for this app's sticker
     * pack (see WhatsAppStickerContentProvider). WhatsApp responds by
     * opening its own "Add sticker pack" confirmation screen, querying this
     * provider for the pack's metadata/stickers/tray icon to show a
     * preview. Returns false if neither WhatsApp nor WhatsApp Business is
     * installed, or if no stickers exist yet to add (nothing meaningful to
     * add — matches how "Save" is disabled with no text in the sticker
     * text composer).
     *
     * Must be called from an Activity context (WhatsApp's receiver expects
     * FLAG_ACTIVITY_NEW_TASK-style semantics via startActivityForResult in
     * its own sample integration; a plain Service context, which is what
     * this IME normally runs as, can still send the intent but some device/
     * WhatsApp version combinations are pickier about the caller being an
     * Activity) — callers from the IME should route this through a
     * trampoline Activity the same way StickerPickerActivity already does
     * for the gallery picker.
     */
    suspend fun requestAddToWhatsApp(activity: android.app.Activity): Boolean {
        val hasStickers = dao.getAllBlocking().isNotEmpty()
        if (!hasStickers) return false
        refreshTrayIcon()

        val targetPackage = whatsAppPackageOrNull() ?: return false
        val intent = android.content.Intent().apply {
            action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
            setPackage(targetPackage)
            putExtra("sticker_pack_id", WhatsAppStickerContentProvider.SINGLE_PACK_ID)
            putExtra("sticker_pack_authority", "${context.packageName}.stickercontentprovider")
            putExtra("sticker_pack_name", WhatsAppStickerContentProvider.PACK_NAME)
        }
        return try {
            activity.startActivityForResult(intent, REQUEST_ADD_TO_WHATSAPP)
            true
        } catch (e: android.content.ActivityNotFoundException) {
            false
        }
    }

    private fun whatsAppPackageOrNull(): String? {
        val pm = context.packageManager
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                // try next candidate
            }
        }
        return null
    }

    companion object {
        const val REQUEST_ADD_TO_WHATSAPP = 9821
    }
}
