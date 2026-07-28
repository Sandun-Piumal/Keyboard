package com.spmods.sinkey.data.dictionary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WordEntity::class], version = 1, exportSchema = false)
abstract class WordDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile private var instance: WordDatabase? = null

        fun getInstance(context: Context): WordDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "sinkey_words.db"
                ).build().also { instance = it }
            }
    }
}
