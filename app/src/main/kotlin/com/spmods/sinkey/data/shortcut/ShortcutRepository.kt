package com.spmods.sinkey.data.shortcut

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * "Quick text" shortcuts — typing a short trigger (e.g. "gm") followed by a
 * word-boundary key (space/enter) replaces it with a longer phrase (e.g.
 * "Good morning"). See QuickTextScreen (Settings UI) and
 * SinKeyInputMethodService's SPACE/ENTER handling (English-typing branch),
 * which calls [expand] on the just-finished word before committing it.
 */
class ShortcutRepository(context: Context) {
    private val dao = ShortcutDatabase.getInstance(context).shortcutDao()

    /** Live list for the Settings screen. */
    val all: Flow<List<ShortcutEntity>> = dao.observeAll()

    /**
     * Saves [shortcut] -> [expansion]. Shortcut is trimmed and lowercased
     * (matching is meant to be case-insensitive — [expand] re-applies the
     * user's original capitalization); overwrites any existing expansion
     * for the same shortcut. Blank shortcut or expansion is a no-op.
     */
    suspend fun save(shortcut: String, expansion: String) {
        val key = shortcut.trim().lowercase()
        val value = expansion.trim()
        if (key.isEmpty() || value.isEmpty() || key.length > MAX_SHORTCUT_LENGTH || value.length > MAX_EXPANSION_LENGTH) return
        dao.upsert(ShortcutEntity(key, value))
    }

    suspend fun delete(shortcut: String) = dao.delete(shortcut.trim().lowercase())

    companion object {
        private const val MAX_SHORTCUT_LENGTH = 40
        private const val MAX_EXPANSION_LENGTH = 500

        /**
         * Looks up [typed] (case-insensitively) in [cache] and returns the
         * expansion with the same capitalization pattern as [typed] applied
         * to it — so "GM" -> "GOOD MORNING", "Gm"/"gm " -> "Good morning"
         * stays "Good morning" (only the first letter mattered), and an
         * all-lowercase trigger stays as stored. Returns null if there's no
         * match, leaving the typed word untouched.
         */
        fun expand(typed: String, cache: Map<String, String>): String? {
            if (typed.isEmpty()) return null
            val expansion = cache[typed.lowercase()] ?: return null
            return when {
                typed == typed.uppercase() && typed != typed.lowercase() ->
                    expansion.uppercase()
                typed.first().isUpperCase() ->
                    expansion.replaceFirstChar { it.uppercase() }
                else -> expansion
            }
        }
    }
}
