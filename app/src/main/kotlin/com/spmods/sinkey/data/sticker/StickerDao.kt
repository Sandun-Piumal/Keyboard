package com.spmods.sinkey.data.sticker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {

    /** All user-created stickers, newest first. Used by the "All" tab of Board.STICKER. */
    @Query("SELECT * FROM stickers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StickerEntity>>

    /** Just the favourited ones, newest-favourited-looking first (createdAt order — see note on setFavourite). */
    @Query("SELECT * FROM stickers WHERE favourite = 1 ORDER BY createdAt DESC")
    fun observeFavourites(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity)

    @Query("UPDATE stickers SET favourite = :favourite WHERE filePath = :filePath")
    suspend fun setFavourite(filePath: String, favourite: Boolean)

    @Query("DELETE FROM stickers WHERE filePath = :filePath")
    suspend fun delete(filePath: String)

    @Query("SELECT * FROM stickers WHERE filePath = :filePath LIMIT 1")
    suspend fun get(filePath: String): StickerEntity?
}
