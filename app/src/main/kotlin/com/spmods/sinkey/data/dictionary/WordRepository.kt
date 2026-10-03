package com.spmods.sinkey.data.dictionary

import android.content.Context
import com.spmods.sinkey.keyboard.SinhalaTransliterator
import com.spmods.sinkey.keyboard.SinhalaVariants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln

/**
 * User's personal, growing word dictionary — every word committed while
 * typing (Sinhala or English) is learned here so it can be suggested again
 * next time the user types the same prefix, even across app restarts.
 */
class WordRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = WordDatabase.getInstance(context).wordDao()
    private val bigramDao = WordDatabase.getInstance(context).bigramDao()
    private val variantChoiceDao = WordDatabase.getInstance(context).variantChoiceDao()

    /**
     * Total distinct words known (bundled dictionary + user-learned,
     * across both languages) — see WordDao.countAll's doc comment.
     */
    suspend fun totalWordCount(): Int = dao.countAll()

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
     * True if [word] exists in the dictionary for [language] — either the
     * bundled base list or something the user has typed/learned before.
     * Used by transliteration candidate-ranking (see
     * SinKeyInputMethodService's onGetSuggestions Sinhala branch) to prefer
     * a dictionary-confirmed reading over the raw phonetic default when a
     * romanized letter is genuinely ambiguous between two Sinhala letters
     * (e.g. "d" -> ද vs ඩ — see SinhalaTransliterator's consonant table
     * comment). A single findExact() lookup is cheap enough to call for
     * each ambiguous-letter candidate generated per keystroke.
     */
    suspend fun isKnownWord(word: String, language: String): Boolean =
        dao.findExact(word, language) != null

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

    // ------------------------------------------------------------------
    // Sinhala variant ranking
    // ------------------------------------------------------------------

    /**
     * Remembers that, for the typed buffer [rawBuffer], the user tapped
     * [chosen] in the suggestion strip. Next time the same typing comes up,
     * [rankSinhalaVariants] puts [chosen] first. Call only for explicit taps.
     */
    suspend fun learnVariantChoice(rawBuffer: String, chosen: String) {
        val key = SinhalaTransliterator.normalizeCase(rawBuffer).trim()
        val word = chosen.trim()
        if (key.isEmpty() || word.isEmpty() || key.length > 40 || word.length > 40) return
        variantChoiceDao.learn(key, word)
    }

    /**
     * Ranks Sinhala readings of the typed Singlish buffer [rawBuffer].
     *
     * Every combination of the ambiguous letters (see [SinhalaVariants]) is
     * checked against the dictionary — as a finished word and as the start of
     * longer words — and scored from:
     *   - how common the word is (bundled corpus position + the user's own use),
     *   - a small penalty per letter changed from the default reading,
     *   - what the user picked before for this same typing,
     *   - how often the word followed [previousWord] before (bigram context).
     *
     * Returns up to [limit] words, best first. Only dictionary-backed or
     * previously chosen words are returned — the plain default transliteration
     * is not included unless it is itself a known word, so callers should add
     * it separately.
     */
    suspend fun rankSinhalaVariants(
        rawBuffer: String,
        previousWord: String?,
        limit: Int = 5
    ): List<String> {
        val key = SinhalaTransliterator.normalizeCase(rawBuffer).trim()
        if (key.length < 2) return emptyList()

        val variants = withContext(Dispatchers.Default) { SinhalaVariants.generate(key) }
        if (variants.isEmpty()) return emptyList()

        val scores = HashMap<String, Double>()
        fun offer(word: String, score: Double) {
            val old = scores[word]
            if (old == null || score > old) scores[word] = score
        }

        // Every form worth an exact lookup: the reading itself, and (when it
        // ends in hal kirima because the word isn't finished) the reading
        // without it — "kad" may well be heading for කද.
        val exactSet = HashSet<String>()
        val devOf = HashMap<String, Int>()
        for (v in variants) {
            exactSet.add(v.sinhala)
            devOf[v.sinhala] = v.deviations
            val p = SinhalaVariants.completionPrefix(v.sinhala)
            if (p.isNotEmpty() && p != v.sinhala && !devOf.containsKey(p)) devOf[p] = v.deviations
        }

        // 1) Known words among all readings.
        for (chunk in devOf.keys.toList().chunked(VARIANT_LOOKUP_CHUNK)) {
            for (e in dao.findExactMany(chunk, "si")) {
                val dev = devOf[e.word] ?: 0
                val bonus = if (e.word in exactSet) EXACT_BONUS else EXACT_PREFIXFORM_BONUS
                offer(e.word, bonus + wordScore(e) - DEVIATION_PENALTY * dev)
            }
        }

        // 2) Longer dictionary words each reading could be the start of.
        //    Only the first few readings (fewest changes) are searched.
        val completionSources = variants
            .map { it to SinhalaVariants.completionPrefix(it.sinhala) }
            .filter { it.second.length >= 2 }
            .take(MAX_COMPLETION_VARIANTS)
        for ((v, prefix) in completionSources) {
            val rows = dao.findByRange(prefix, prefix + "\uFFFF", "si", COMPLETIONS_PER_VARIANT)
            for (e in rows) {
                val extra = (e.word.length - prefix.length).coerceAtLeast(0)
                offer(
                    e.word,
                    COMPLETION_WEIGHT * wordScore(e) - COMPLETION_LENGTH_PENALTY * extra -
                        DEVIATION_PENALTY * v.deviations
                )
            }
        }

        // 3) What the user picked for this exact typing before (strong), and
        //    for longer typings that start with it (weaker).
        for (c in variantChoiceDao.findByRaw(key, 5)) {
            offer(c.chosen, CHOICE_EXACT_BASE + CHOICE_EXACT_SCALE * ln(1.0 + c.uses))
        }
        for (c in variantChoiceDao.findByRawRange(key, key + "\uFFFF", 5)) {
            if (c.rawKey == key) continue
            offer(c.chosen, CHOICE_PREFIX_BASE + CHOICE_PREFIX_SCALE * ln(1.0 + c.uses))
        }

        // 4) Sentence context: words that followed the previous word before.
        val prev = previousWord?.trim().orEmpty()
        if (prev.isNotEmpty() && scores.isNotEmpty()) {
            val followers = bigramDao.findFollowers(
                prev, "si", scores.keys.toList().take(VARIANT_LOOKUP_CHUNK)
            )
            for (b in followers) {
                val old = scores[b.nextWord] ?: continue
                scores[b.nextWord] = old + bigramBonus(b.frequency)
            }
            // Followers the steps above never surfaced, but which extend one
            // of the readings, are exactly what context is for.
            val prefixes = variants.map { SinhalaVariants.completionPrefix(it.sinhala) }
                .filter { it.isNotEmpty() }
            for (b in bigramDao.findByPreviousWord(prev, "si", BIGRAM_SCAN_LIMIT)) {
                if (scores.containsKey(b.nextWord)) continue
                if (prefixes.any { b.nextWord.startsWith(it) }) {
                    offer(b.nextWord, BIGRAM_ONLY_BASE + bigramBonus(b.frequency))
                }
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }

    /** How "good" a dictionary word is on its own: corpus commonness + the user's own use. */
    private fun wordScore(e: WordEntity): Double {
        val inCorpus = e.corpusRank > 0
        // A seeded word starts at SEED_FREQUENCY; only uses above that are the user's own.
        val uses = if (inCorpus) (e.frequency - DictionarySeeder.SEED_FREQUENCY).coerceAtLeast(0) else e.frequency
        val user = USER_USE_WEIGHT * ln(1.0 + uses)
        val corpus = if (inCorpus) (CORPUS_MAX - CORPUS_SLOPE * ln(e.corpusRank.toDouble())).coerceAtLeast(0.0) else UNRANKED_BASE
        return user + corpus
    }

    private fun bigramBonus(frequency: Int): Double = BIGRAM_BASE + BIGRAM_SCALE * ln(1.0 + frequency)

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
     * Same as [allWords] but capped to the [limit] most-frequent words —
     * see WordDao.getTopForLanguage's doc comment for why gesture typing
     * uses this instead of the uncapped [allWords] now that wordlist_si.txt
     * is a 200K-word frequency corpus rather than ~1,654 words.
     */
    suspend fun topWords(language: String, limit: Int = GESTURE_CANDIDATE_LIMIT): List<String> =
        dao.getTopForLanguage(language, limit).map { it.word }

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
        // A remembered "typed X -> picked this word" choice would otherwise
        // keep resurrecting a word the user just removed.
        if (language == "si") variantChoiceDao.deleteByChosen(word.trim())
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
    suspend fun seedBaseDictionaryIfNeeded() = withContext(Dispatchers.IO) {
        DictionarySeeder.seedIfNeeded(appContext, dao)
    }

    companion object {
        // ---- Variant ranking weights (see rankSinhalaVariants) ----
        private const val VARIANT_LOOKUP_CHUNK = 400      // stays under SQLite's 999 bound variables
        private const val MAX_COMPLETION_VARIANTS = 12    // readings searched for longer words
        private const val COMPLETIONS_PER_VARIANT = 5
        private const val EXACT_BONUS = 12.0              // reading is itself a known word
        private const val EXACT_PREFIXFORM_BONUS = 6.0    // reading minus trailing hal is a known word
        private const val COMPLETION_WEIGHT = 0.6
        private const val COMPLETION_LENGTH_PENALTY = 0.8 // per extra character still to be typed
        private const val DEVIATION_PENALTY = 2.0         // per letter changed from the default reading
        private const val USER_USE_WEIGHT = 6.0
        private const val CORPUS_MAX = 14.0               // corpus rank 1
        private const val CORPUS_SLOPE = 1.1              // falls with ln(rank)
        private const val UNRANKED_BASE = 3.0             // known word outside the corpus
        private const val CHOICE_EXACT_BASE = 40.0
        private const val CHOICE_EXACT_SCALE = 6.0
        private const val CHOICE_PREFIX_BASE = 12.0
        private const val CHOICE_PREFIX_SCALE = 3.0
        private const val BIGRAM_BASE = 10.0
        private const val BIGRAM_SCALE = 5.0
        private const val BIGRAM_ONLY_BASE = 3.0
        private const val BIGRAM_SCAN_LIMIT = 40

        /**
         * How many extra learnWord() bumps a manually added word gets
         * beyond its first insert, so it starts at a frequency comparable
         * to a word typed several times already (see manualAdd's doc
         * comment) rather than frequency=1, which ordinary prefix/fuzzy
         * ranking would place behind almost everything else in the
         * dictionary.
         */
        private const val MANUAL_ADD_FREQUENCY_BOOST = 4

        /**
         * Default candidate-pool size for [topWords]/gesture typing. Large
         * enough to cover virtually every word someone would realistically
         * swipe (well beyond everyday vocabulary), small enough that
         * scoring it against a swipe path on every gesture stays fast on
         * typical devices. See WordDao.getTopForLanguage's doc comment.
         */
        private const val GESTURE_CANDIDATE_LIMIT = 15000
    }
}
