package com.spmods.sinkey.data.clipboard

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One entry in the persistent clipboard history.
 *
 * [text]      the clipped text itself. Also serves as a natural de-dup key —
 *             copying the same text again just bumps [copiedAt] instead of
 *             creating a duplicate row (see ClipDao.upsert).
 * [copiedAt]  timestamp (epoch millis) this text was copied — used to order
 *             the history newest-first and to prune old entries.
 * [pinned]    user can pin an entry so it survives the history size cap and
 *             the "clear all" action, for things like a phone number or
 *             address they paste often.
 */
@Entity(tableName = "clips", primaryKeys = ["text"])
data class ClipEntity(
    val text: String,
    val copiedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
)
