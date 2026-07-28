package com.spmods.sinkey.data.dictionary

import android.content.Context

/**
 * User's personal, growing word dictionary — every word committed while
 * typing (Sinhala or English) is learned here so it can be suggested again
 * next time the user types the same prefix, even across app restarts.
 */
class WordRepository(context: Context) {
    private val dao = WordDatabase.getInstance(context).wordDao()

    /** Record a use of [word] for [language]. Safe to call for every committed word. */
    suspend fun learn(word: String, language: String) {
        val trimmed = word.trim()
        // Don't pollute the dictionary with empty strings, pure punctuation,
        // or very long "words" (usually pasted text, not typed words).
        if (trimmed.isEmpty() || trimmed.length > 40) return
        if (trimmed.none { it.isLetter() }) return
        dao.learnWord(trimmed, language)
    }

    /** Personal-dictionary matches for [prefix], most used / most recent first. */
    suspend fun suggestionsFor(prefix: String, language: String, limit: Int = 5): List<String> {
        if (prefix.isEmpty()) return emptyList()
        return dao.findByPrefix(prefix, language, limit).map { it.word }
    }
}
