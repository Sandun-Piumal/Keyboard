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

    /**
     * Fuzzy matches for [typed], tolerant of small spelling variations —
     * e.g. a dropped vowel sign, one wrong consonant, or a transliteration
     * ambiguity (see [SinhalaTransliterator]/[SinhalaCandidateMap]) that
     * produced a slightly different but recognizable string. Falls back to
     * plain prefix matching first (cheap, exact-prefix hits are common and
     * always relevant), then widens to an edit-distance search over words
     * sharing the same first character if the prefix search comes up short.
     *
     * [maxDistance] caps how many single-character edits (insert/delete/
     * substitute) are tolerated; 2 is a reasonable default for short-to-
     * medium words without matching things that aren't actually related.
     */
    suspend fun fuzzySuggestionsFor(
        typed: String,
        language: String,
        limit: Int = 5,
        maxDistance: Int = 2
    ): List<String> {
        if (typed.isEmpty()) return emptyList()

        val prefixHits = dao.findByPrefix(typed, language, limit).map { it.word }
        if (prefixHits.size >= limit) return prefixHits

        val firstChar = typed.first().toString()
        val candidates = dao.findByFirstChar(firstChar, language, limit = 200)

        val scored = candidates
            .filter { it.word !in prefixHits }
            .map { entity ->
                val distance = levenshtein(typed, entity.word)
                Triple(entity.word, distance, entity.frequency)
            }
            .filter { (_, distance, _) -> distance in 1..maxDistance }
            // Closer matches first; among equal distance, more-used words win.
            .sortedWith(compareBy({ it.second }, { -it.third }))
            .map { it.first }

        return (prefixHits + scored).distinct().take(limit)
    }

    /** Classic Levenshtein edit distance between [a] and [b]. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,      // insertion
                    prev[j] + 1,          // deletion
                    prev[j - 1] + cost    // substitution
                )
            }
            for (j in 0..b.length) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
