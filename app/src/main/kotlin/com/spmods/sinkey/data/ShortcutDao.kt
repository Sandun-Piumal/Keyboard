package com.spmods.sinkey.data.shortcut

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    /** Newest-added first — shown in Settings' Quick text list. */
    @Query("SELECT * FROM shortcuts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ShortcutEntity>>

    /** Insert new, or overwrite the expansion if [ShortcutEntity.shortcut] already exists. */
    @Upsert
    suspend fun upsert(entity: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE shortcut = :shortcut")
    suspend fun delete(shortcut: String)
}
