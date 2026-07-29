package com.spmods.sinkey.data.sticker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StickerEntity::class], version = 1, exportSchema = false)
abstract class StickerDatabase : RoomDatabase() {
    abstract fun stickerDao(): StickerDao

    companion object {
        @Volatile private var instance: StickerDatabase? = null

        fun getInstance(context: Context): StickerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StickerDatabase::class.java,
                    "sinkey_stickers.db"
                ).build().also { instance = it }
            }
    }
}
