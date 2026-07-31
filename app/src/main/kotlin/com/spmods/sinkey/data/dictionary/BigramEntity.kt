package com.spmods.sinkey.data.dictionary

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One learned word pair — records that [nextWord] followed [previousWord]
 * at least once, so the keyboard can predict it again next time the user
 * finishes typing [previousWord].
 *
 * [previousWord] the word that was typed immediately before [nextWord],
 *                stored lowercase/as-committed the same way [WordEntity.word]
 *                is, so lookups are a simple equality match.
 * [nextWord]     the word that followed it.
 * [language]     "si" or "en" — pairs are learned and looked up per-language,
 *                same convention as [WordEntity].
 * [frequency]    how many times this exact pair has been seen — higher
 *                frequency pairs are predicted first.
 * [lastUsed]     timestamp (epoch millis) of the most recent occurrence,
 *                used as a tiebreaker between equally frequent pairs.
 */
@Entity(tableName = "bigrams", primaryKeys = ["previousWord", "nextWord", "language"])
data class BigramEntity(
    val previousWord: String,
    val nextWord: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
