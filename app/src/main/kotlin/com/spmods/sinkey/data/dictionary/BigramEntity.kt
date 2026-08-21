package com.spmods.sinkey.data.dictionary

import androidx.room.Entity
import androidx.room.Index
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
 *
 * Index (see WordDatabase MIGRATION_2_3): the primary key is
 * (previousWord, nextWord, language), which already lets findByPreviousWord
 * range-scan on previousWord, but every query also filters on `language`
 * and findByPreviousWordAndPrefix additionally does `nextWord LIKE
 * 'prefix%'`. idx_bigrams_lookup puts (previousWord, language) first so
 * both queries stay fast index scans — narrowed to the right language
 * before the nextWord prefix check — instead of scanning every pair ever
 * learned for that previousWord across both languages.
 */
@Entity(
    tableName = "bigrams",
    primaryKeys = ["previousWord", "nextWord", "language"],
    indices = [
        Index(value = ["previousWord", "language", "nextWord"], name = "idx_bigrams_lookup")
    ]
)
data class BigramEntity(
    val previousWord: String,
    val nextWord: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
