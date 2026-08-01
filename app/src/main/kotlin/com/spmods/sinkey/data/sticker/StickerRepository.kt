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
        return true
    }

    /** Creates a sticker by rendering [text] onto a transparent PNG. Returns true on success. */
    suspend fun createFromText(text: String, textColor: Int = android.graphics.Color.WHITE): Boolean {
        val path = StickerFileStore.saveFromText(context, text, textColor) ?: return false
        dao.insert(StickerEntity(filePath = path, source = StickerEntity.SOURCE_TEXT))
        return true
    }

    suspend fun setFavourite(filePath: String, favourite: Boolean) = dao.setFavourite(filePath, favourite)

    suspend fun delete(filePath: String) {
        dao.delete(filePath)
        StickerFileStore.deleteFile(filePath)
    }
}
