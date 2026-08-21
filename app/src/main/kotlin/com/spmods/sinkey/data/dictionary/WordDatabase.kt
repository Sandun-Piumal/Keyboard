package com.spmods.sinkey.data.dictionary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WordEntity::class, BigramEntity::class], version = 3, exportSchema = false)
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

        /**
         * v2 -> v3: adds the indices declared on WordEntity/BigramEntity
         * (see their doc comments for why each one exists). Pure
         * performance migration — no schema/column changes, so it's just
         * CREATE INDEX statements; existing word/bigram rows are untouched
         * and don't need to be rewritten or re-seeded.
         */
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_words_language_word ON words(language, word)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_words_language_frequency ON words(language, frequency)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_bigrams_lookup ON bigrams(previousWord, language, nextWord)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}
