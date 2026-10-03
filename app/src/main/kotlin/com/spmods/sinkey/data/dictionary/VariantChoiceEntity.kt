package com.spmods.sinkey.data.dictionary

import androidx.room.Entity
import androidx.room.Index

/**
 * Remembers which Sinhala reading the user picked for a typed Singlish
 * buffer, so the same typing is ranked first next time.
 *
 * [rawKey]  the buffer exactly as typed, with only meaningless case folded
 *           away (see SinhalaTransliterator.normalizeCase) — "kade", "aDa".
 * [chosen]  the Sinhala word the user tapped in the suggestion strip.
 * [uses]    how many times this exact pairing was chosen.
 * [lastUsed] epoch millis of the latest choice (tiebreaker).
 */
@Entity(
    tableName = "variant_choices",
    primaryKeys = ["rawKey", "chosen"],
    indices = [Index(value = ["rawKey"], name = "idx_variant_choices_raw")]
)
data class VariantChoiceEntity(
    val rawKey: String,
    val chosen: String,
    val uses: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
