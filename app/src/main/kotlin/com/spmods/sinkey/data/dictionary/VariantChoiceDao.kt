package com.spmods.sinkey.data.dictionary

import androidx.room.Dao
import androidx.room.Query

@Dao
interface VariantChoiceDao {

    /** Records one more time [chosen] was picked for [rawKey] (insert or bump). */
    @Query(
        """
        INSERT INTO variant_choices (rawKey, chosen, uses, lastUsed)
        VALUES (:rawKey, :chosen, 1, :now)
        ON CONFLICT(rawKey, chosen) DO UPDATE SET
            uses = uses + 1,
            lastUsed = :now
        """
    )
    suspend fun learn(rawKey: String, chosen: String, now: Long = System.currentTimeMillis())

    /** Choices made for exactly this typing, most used first. */
    @Query(
        """
        SELECT * FROM variant_choices
        WHERE rawKey = :rawKey
        ORDER BY uses DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByRaw(rawKey: String, limit: Int = 5): List<VariantChoiceEntity>

    /**
     * Choices made for longer typings that START with [lo] (hi = lo + U+FFFF),
     * so a half-typed word can already surface what the user picked for the
     * finished word. Uses a binary range on the indexed column.
     */
    @Query(
        """
        SELECT * FROM variant_choices
        WHERE rawKey >= :lo AND rawKey < :hi
        ORDER BY uses DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByRawRange(lo: String, hi: String, limit: Int = 5): List<VariantChoiceEntity>

    /** Forget every choice that produced [chosen] (used when the word is deleted from the dictionary). */
    @Query("DELETE FROM variant_choices WHERE chosen = :chosen")
    suspend fun deleteByChosen(chosen: String)
}
