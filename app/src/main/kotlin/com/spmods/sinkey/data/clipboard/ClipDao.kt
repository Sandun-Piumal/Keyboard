package com.spmods.sinkey.data.clipboard

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    /** Pinned entries first (newest-pinned first), then unpinned newest-first. */
    @Query(
        """
        SELECT * FROM clips
        ORDER BY pinned DESC, copiedAt DESC
        """
    )
    fun observeAll(): Flow<List<ClipEntity>>

    /**
     * Records a copy of [text]: inserts it if new, or just refreshes
     * [copiedAt] (bumping it back to the top) if it was already copied
     * before. Pinned state is preserved on conflict.
     */
    @Query(
        """
        INSERT INTO clips (text, copiedAt, pinned)
        VALUES (:text, :now, 0)
        ON CONFLICT(text) DO UPDATE SET copiedAt = :now
        """
    )
    suspend fun upsert(text: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE clips SET pinned = :pinned WHERE text = :text")
    suspend fun setPinned(text: String, pinned: Boolean)

    @Query("DELETE FROM clips WHERE text = :text")
    suspend fun delete(text: String)

    /** Clears everything except pinned entries. */
    @Query("DELETE FROM clips WHERE pinned = 0")
    suspend fun clearUnpinned()

    /**
     * Keeps history from growing forever: deletes unpinned rows beyond the
     * newest [keep] of them. Called after every insert.
     */
    @Query(
        """
        DELETE FROM clips
        WHERE pinned = 0 AND text NOT IN (
            SELECT text FROM clips WHERE pinned = 0
            ORDER BY copiedAt DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)
}
