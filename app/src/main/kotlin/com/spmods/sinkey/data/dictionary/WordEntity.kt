package com.spmods.sinkey.data.dictionary

import androidx.room.Entity
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
 */
@Entity(tableName = "words", primaryKeys = ["word", "language"])
data class WordEntity(
    val word: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
