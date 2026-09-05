package com.spmods.sinkey.data.dictionary

import android.content.Context

/**
 * User's personal, growing word dictionary — every word committed while
 * typing (Sinhala or English) is learned here so it can be suggested again
 * next time the user types the same prefix, even across app restarts.
 */
class WordRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = WordDatabase.getInstance(context).wordDao()
    private val bigramDao = WordDatabase.getInstance(context).bigramDao()

    /** Record a use of [word] for [language]. Safe to call for every committed word. */
    suspend fun learn(word: String, language: String) {
        val trimmed = word.trim()
        // Don't pollute the dictionary with empty strings, pure punctuation,
        // or very long "words" (usually pasted text, not typed words).
        if (trimmed.isEmpty() || trimmed.length > 40) return
        if (trimmed.none { it.isLetter() }) return
        dao.learnWord(trimmed, language)
    }

    /**
     * Record that [nextWord] was typed right after [previousWord], so the
     * pair can be used later to predict [nextWord] as soon as [previousWord]
     * is finished again. Safe to call for every committed word — same
     * blank/length/letter filtering as [learn], applied to both words.
     */
    suspend fun learnBigram(previousWord: String, nextWord: String, language: String) {
        val prev = previousWord.trim()
        val next = nextWord.trim()
        if (prev.isEmpty() || next.isEmpty() || prev.length > 40 || next.length > 40) return
        if (prev.none { it.isLetter() } || next.none { it.isLetter() }) return
        bigramDao.learnPair(prev, next, language)
    }

    /**
     * Predicts the next word given [previousWord], optionally narrowed to
     * ones starting with [prefix] once the user has begun typing it.
     * Most frequent / most recently used pairing wins. Returns an empty
     * list if [previousWord] is blank or nothing has ever followed it.
     */
    suspend fun nextWordSuggestions(
        previousWord: String,
        language: String,
        prefix: String = "",
        limit: Int = 3
    ): List<String> {
        val prev = previousWord.trim()
        if (prev.isEmpty()) return emptyList()
        val pairs = if (prefix.isEmpty()) {
            bigramDao.findByPreviousWord(prev, language, limit)
        } else {
            bigramDao.findByPreviousWordAndPrefix(prev, prefix, language, limit)
        }
        return pairs.map { it.nextWord }
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
     *
     * Every committed word is still learned (via [learn]) after just one
     * use — that part is unchanged, so a word can start climbing frequency
     * immediately. But a word typed only once is exactly as likely to be a
     * typo as a real word the user wants remembered, and typos are what
     * fuzzy matching is most likely to surface (they're "close" to lots of
     * things by definition). So the *fuzzy* half of this search — not the
     * exact-prefix half, which reflects what's actually being typed right
     * now — requires frequency >= [minFuzzyTrust] before a word counts as
     * "confirmed" enough to suggest via edit-distance. A word graduates
     * into fuzzy-eligibility the moment it's typed a second time.</br>
     */
    suspend fun fuzzySuggestionsFor(
        typed: String,
        language: String,
        limit: Int = 5,
        maxDistance: Int = 2,
        minFuzzyTrust: Int = 2
    ): List<String> {
        if (typed.isEmpty()) return emptyList()

        val prefixHits = dao.findByPrefix(typed, language, limit).map { it.word }
        if (prefixHits.size >= limit) return prefixHits

        val firstChar = typed.first().toString()
        val candidates = dao.findByFirstChar(firstChar, language, limit = 200)

        val scored = candidates
            .filter { it.word !in prefixHits && it.frequency >= minFuzzyTrust }
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

    /**
     * Every known word for [language] — bundled base dictionary plus
     * whatever the user has typed/learned. Used exclusively by gesture
     * typing's word matcher (GestureWordMatcher); see WordDao.getAllForLanguage
     * for why a full scan is unavoidable here instead of a prefix query.
     */
    suspend fun allWords(language: String): List<String> =
        dao.getAllForLanguage(language).map { it.word }

    /**
     * Live-updating word lists for the Personal Dictionary screen — one per
     * language, so the two tabs can each collectAsState() independently.
     * Mirrors ShortcutRepository.all's Flow-backed pattern for Quick text's
     * list; unlike that one there are two here (language isn't a column the
     * UI ever needs to see all values of at once, so no single unified
     * Flow is exposed).
     */
    val sinhalaWords: kotlinx.coroutines.flow.Flow<List<WordEntity>> = dao.observeAllForLanguage("si")
    val englishWords: kotlinx.coroutines.flow.Flow<List<WordEntity>> = dao.observeAllForLanguage("en")

    /**
     * Every learned word for [language], newest/most-used first — for the
     * Personal Dictionary screen's browse list. See WordDao.getAllForLanguageBrowse
     * for why this orders differently than allWords (which serves gesture
     * typing's scoring instead).
     */
    suspend fun browseAll(language: String): List<WordEntity> =
        dao.getAllForLanguageBrowse(language)

    /**
     * Removes [word] from the personal dictionary for [language], as
     * chosen by the user on the Personal Dictionary screen. Does nothing
     * to the bundled base dictionary beyond this one row — if [word]
     * happens to also be a seeded base-dictionary word, deleting it here
     * simply forgets the user's own usage of it; DictionarySeeder won't
     * re-seed it (seeding only ever runs once per SEED_VERSION, and
     * re-adding a word the user just deleted would be surprising).
     */
    suspend fun delete(word: String, language: String) {
        dao.delete(word.trim(), language)
    }

    /**
     * Adds [word] to the personal dictionary by hand, from the Personal
     * Dictionary screen's "add word" flow — as opposed to [learn], which
     * only ever fires from words actually typed and committed.
     *
     * Manually added words start at a frequency matching a word already
     * typed a few times (rather than [learn]'s frequency=1 for a brand
     * new word), so a word the user cared enough about to add by hand
     * starts showing up in suggestions right away instead of needing
     * several more real uses to out-rank other candidates first — same
     * reasoning as DictionarySeeder's SEED_FREQUENCY baseline for the
     * bundled word lists.
     *
     * Uses the same learnWord() upsert the typing path uses, so adding a
     * word that's already present bumps its existing frequency/lastUsed
     * rather than erroring or creating a duplicate row.
     */
    suspend fun manualAdd(word: String, language: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty() || trimmed.length > 40) return
        if (trimmed.none { it.isLetter() }) return
        dao.learnWord(trimmed, language, now = System.currentTimeMillis())
        repeat(MANUAL_ADD_FREQUENCY_BOOST) { dao.learnWord(trimmed, language, now = System.currentTimeMillis()) }
    }

    /**
     * Loads the bundled base word lists (assets/wordlist_en.txt,
     * assets/wordlist_si.txt — see DictionarySeeder) into the personal
     * dictionary at a low starting frequency, once. Exists mainly so
     * gesture typing (GestureWordMatcher) has a reasonably useful
     * vocabulary to match against from a fresh install, rather than only
     * the handful of words the user has typed so far — but the seeded
     * words also naturally strengthen ordinary prefix/fuzzy suggestions
     * too, via the same words table both features read from.
     *
     * Safe to call on every app start: DictionarySeeder itself tracks
     * whether seeding has already run (via PreferencesManager) and
     * short-circuits instantly if so, and seedWord()'s OR IGNORE means
     * even a redundant call can't re-bump frequencies or duplicate rows.
     */
    suspend fun seedBaseDictionaryIfNeeded() {
        DictionarySeeder.seedIfNeeded(appContext, dao)
    }

    companion object {
        /**
         * How many extra learnWord() bumps a manually added word gets
         * beyond its first insert, so it starts at a frequency comparable
         * to a word typed several times already (see manualAdd's doc
         * comment) rather than frequency=1, which ordinary prefix/fuzzy
         * ranking would place behind almost everything else in the
         * dictionary.
         */
        private const val MANUAL_ADD_FREQUENCY_BOOST = 4
    }
}
