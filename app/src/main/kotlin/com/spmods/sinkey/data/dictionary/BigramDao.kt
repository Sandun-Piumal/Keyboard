package com.spmods.sinkey.data.dictionary

import androidx.room.Dao
import androidx.room.Query

@Dao
interface BigramDao {

    /**
     * Learns that [nextWord] followed [previousWord]: inserts the pair if
     * new, or bumps its frequency/lastUsed if it's been seen before. Mirrors
     * [WordDao.learnWord]'s upsert pattern.
     */
    @Query(
        """
        INSERT INTO bigrams (previousWord, nextWord, language, frequency, lastUsed)
        VALUES (:previousWord, :nextWord, :language, 1, :now)
        ON CONFLICT(previousWord, nextWord, language) DO UPDATE SET
            frequency = frequency + 1,
            lastUsed = :now
        """
    )
    suspend fun learnPair(
        previousWord: String,
        nextWord: String,
        language: String,
        now: Long = System.currentTimeMillis()
    )

    /**
     * Returns up to [limit] words that have followed [previousWord] before,
     * most frequent / most recent first — used to predict the next word
     * before the user has typed anything of it.
     */
    @Query(
        """
        SELECT * FROM bigrams
        WHERE previousWord = :previousWord AND language = :language
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByPreviousWord(previousWord: String, language: String, limit: Int = 3): List<BigramEntity>

    /**
     * Same as [findByPreviousWord] but additionally filtered to words
     * starting with [prefix] — used once the user has started typing the
     * next word, so predictions stay both context- and prefix-aware.
     */
    @Query(
        """
        SELECT * FROM bigrams
        WHERE previousWord = :previousWord AND language = :language AND nextWord LIKE :prefix || '%'
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByPreviousWordAndPrefix(
        previousWord: String,
        prefix: String,
        language: String,
        limit: Int = 3
    ): List<BigramEntity>
}
