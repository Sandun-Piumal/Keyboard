package com.spmods.sinkey.data.shortcut

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShortcutEntity::class], version = 1, exportSchema = false)
abstract class ShortcutDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile private var instance: ShortcutDatabase? = null

        fun getInstance(context: Context): ShortcutDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShortcutDatabase::class.java,
                    "sinkey_shortcuts.db"
                ).build().also { instance = it }
            }
    }
}
