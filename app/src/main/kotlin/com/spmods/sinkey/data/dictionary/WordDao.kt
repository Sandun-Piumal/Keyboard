package com.spmods.sinkey.data.dictionary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {

    /**
     * Returns up to [limit] learned words for [language] whose text starts
     * with [prefix], ordered by frequency (most-used first) then recency.
     * An empty prefix is never queried by callers, but the LIKE pattern
     * would simply match everything if it were.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE language = :language AND word LIKE :prefix || '%'
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByPrefix(prefix: String, language: String, limit: Int = 5): List<WordEntity>

    @Query("SELECT * FROM words WHERE word = :word AND language = :language LIMIT 1")
    suspend fun findExact(word: String, language: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WordEntity)

    /**
     * Learns [word]: inserts it if new, or bumps its frequency/lastUsed if it
     * already exists. This is how the keyboard "remembers" words the user
     * has typed so they can be suggested again later.
     */
    @Query(
        """
        INSERT INTO words (word, language, frequency, lastUsed)
        VALUES (:word, :language, 1, :now)
        ON CONFLICT(word, language) DO UPDATE SET
            frequency = frequency + 1,
            lastUsed = :now
        """
    )
    suspend fun learnWord(word: String, language: String, now: Long = System.currentTimeMillis())
}
