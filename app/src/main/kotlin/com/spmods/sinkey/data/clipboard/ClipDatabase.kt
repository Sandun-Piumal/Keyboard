package com.spmods.sinkey.data.clipboard

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClipEntity::class], version = 1, exportSchema = false)
abstract class ClipDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile private var instance: ClipDatabase? = null

        fun getInstance(context: Context): ClipDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipDatabase::class.java,
                    "sinkey_clips.db"
                ).build().also { instance = it }
            }
    }
}
