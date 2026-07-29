package com.spmods.sinkey.data.clipboard

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Persistent clipboard history — every text the user copies (system-wide,
 * not just inside SinKey) is recorded here via the system ClipboardManager
 * listener in SinKeyInputMethodService, so the clipboard tool can show more
 * than just "the one thing currently on the clipboard".
 */
class ClipRepository(context: Context) {
    private val dao = ClipDatabase.getInstance(context).clipDao()

    val history: Flow<List<ClipEntity>> = dao.observeAll()

    /** Record a copy of [text]. Blank text and very long paste-dumps are ignored. */
    suspend fun record(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return
        dao.upsert(trimmed)
        dao.trimTo(MAX_HISTORY)
    }

    suspend fun setPinned(text: String, pinned: Boolean) = dao.setPinned(text, pinned)

    suspend fun delete(text: String) = dao.delete(text)

    suspend fun clearUnpinned() = dao.clearUnpinned()

    companion object {
        private const val MAX_HISTORY = 50
        private const val MAX_LENGTH = 4000
    }
}
