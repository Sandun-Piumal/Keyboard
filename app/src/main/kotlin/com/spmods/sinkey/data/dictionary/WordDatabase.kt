package com.spmods.sinkey.data.dictionary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WordEntity::class, BigramEntity::class], version = 2, exportSchema = false)
abstract class WordDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun bigramDao(): BigramDao

    companion object {
        @Volatile private var instance: WordDatabase? = null

        /**
         * v1 -> v2: adds the "bigrams" table used for next-word prediction.
         * Existing per-word learning data (the "words" table) is untouched.
         */
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bigrams (
                        previousWord TEXT NOT NULL,
                        nextWord TEXT NOT NULL,
                        language TEXT NOT NULL,
                        frequency INTEGER NOT NULL,
                        lastUsed INTEGER NOT NULL,
                        PRIMARY KEY(previousWord, nextWord, language)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): WordDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "sinkey_words.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
