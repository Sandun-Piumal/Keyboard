package com.spmods.sinkey.data.sticker

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One user-created sticker (Board.STICKER → "Create Sticker" flow).
 *
 * These are stickers the user made inside SinKey itself — either from a
 * photo they picked ([SOURCE_IMAGE]) or from typed text rendered onto a
 * transparent PNG ([SOURCE_TEXT]). The actual pixel data lives as a PNG file
 * under the app's private files dir (see StickerFileStore); this row just
 * tracks metadata + favourite state + ordering.
 *
 * This is a separate concept from "external" stickers picked up read-only
 * from a WhatsApp/Telegram sticker folder the user has connected via SAF
 * (WhatsApp/Telegram sticker folder access was tried via SAF and later
 * removed — see StickerBoardView's doc comment for why) — external
 * stickers, while supported, were never copied into this table.
 *
 * [filePath]   absolute path to the PNG file in app-private storage.
 * [source]     how this sticker was created — SOURCE_IMAGE or SOURCE_TEXT.
 * [createdAt]  epoch millis, used for default (newest-first) ordering.
 * [favourite]  shown in the Favourites tab when true.
 */
@Entity(tableName = "stickers", primaryKeys = ["filePath"])
data class StickerEntity(
    val filePath: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
    val favourite: Boolean = false
) {
    companion object {
        const val SOURCE_IMAGE = "image"
        const val SOURCE_TEXT = "text"
    }
}
