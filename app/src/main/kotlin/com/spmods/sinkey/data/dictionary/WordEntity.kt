package com.spmods.sinkey.data.dictionary

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One learned word in the user's personal dictionary.
 *
 * [word]      the exact text as it was committed (Sinhala or English).
 * [language]  "si" or "en" — words are looked up per-language.
 * [frequency] how many times this word has been typed/used — higher
 *             frequency words are suggested first.
 * [lastUsed]  timestamp (epoch millis) of the most recent use — used as a
 *             tiebreaker so recently used words rank above stale ones with
 *             the same frequency.
 *
 * Indices (see WordDatabase MIGRATION_2_3):
 * - idx_words_language_word: the primary key is (word, language), i.e.
 *   `word` leads — great for findExact, useless for `WHERE language = ?
 *   AND word LIKE 'prefix%'` (SQLite can't range-scan a composite key
 *   whose leading column isn't the equality filter). This index puts
 *   `language` first so findByPrefix's LIKE-prefix scan stays a fast
 *   index range-scan even with tens of thousands of rows, instead of a
 *   full table scan re-evaluating every row.
 * - idx_words_language_frequency: speeds up getAllForLanguage's
 *   `WHERE language = ? ORDER BY frequency DESC` (gesture typing) by
 *   letting SQLite walk the index in already-sorted order.
 */
@Entity(
    tableName = "words",
    primaryKeys = ["word", "language"],
    indices = [
        Index(value = ["language", "word"], name = "idx_words_language_word"),
        Index(value = ["language", "frequency"], name = "idx_words_language_frequency")
    ]
)
data class WordEntity(
    val word: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
