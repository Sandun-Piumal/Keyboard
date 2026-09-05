package com.spmods.sinkey.data.dictionary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {

    /**
     * Returns up to [limit] learned words for [language] whose text starts
     * with [prefix], ordered by frequency (most-used first) then recency.
     * An empty prefix is never queried by callers, but the LIKE pattern
     * would simply match everything if it were.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE language = :language AND word LIKE :prefix || '%'
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByPrefix(prefix: String, language: String, limit: Int = 5): List<WordEntity>

    /**
     * Same intent as a "starts with [firstChar]" filter, but expressed as a
     * LIKE-prefix range (`word LIKE firstChar || '%'`) instead of
     * `substr(word, 1, 1) = firstChar`. The substr() form can't use
     * idx_words_language_word — SQLite has no way to know what substr()
     * will return for a row without reading it first, so every row for the
     * language was being scanned. A LIKE 'x%' condition is a genuine
     * prefix range the query planner can walk directly on the index, same
     * as findByPrefix. Matches the exact same rows as before since a
     * single-character firstChar makes the two conditions equivalent.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE language = :language AND word LIKE :firstChar || '%'
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun findByFirstChar(firstChar: String, language: String, limit: Int = 200): List<WordEntity>

    @Query("SELECT * FROM words WHERE word = :word AND language = :language LIMIT 1")
    suspend fun findExact(word: String, language: String): WordEntity?

    /**
     * Every word known for [language] (bundled base dictionary + anything
     * the user has typed/learned), ordered by frequency. Used only by
     * gesture typing's word matcher (GestureWordMatcher) — swipe input
     * doesn't have clean letter boundaries the way typed prefixes do, so it
     * can't narrow the search with a WHERE...LIKE prefix query the way
     * findByPrefix does; it needs the full candidate pool to score against
     * the swiped path instead.
     */
    @Query("SELECT * FROM words WHERE language = :language ORDER BY frequency DESC")
    suspend fun getAllForLanguage(language: String): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WordEntity)

    /**
     * All learned words for [language], newest/most-used first, for the
     * Personal Dictionary screen's browse list. Distinct from
     * getAllForLanguage (which orders by frequency only, for gesture
     * typing's scoring) — this orders by lastUsed first so recently
     * added/used words surface at the top of the list the user sees and
     * manages, which matters more for browsing than for swipe-matching.
     *
     * observeAllForLanguage below returns the same rows as a live Flow,
     * used by the Personal Dictionary screen so it updates immediately
     * after a delete or manual add — the same way ShortcutDao.observeAll
     * does for Quick text's shortcut list. getAllForLanguageBrowse is kept
     * as a plain suspend query alongside it for any one-off read that
     * doesn't need to observe changes.
     */
    @Query("SELECT * FROM words WHERE language = :language ORDER BY lastUsed DESC, frequency DESC")
    fun observeAllForLanguage(language: String): kotlinx.coroutines.flow.Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE language = :language ORDER BY lastUsed DESC, frequency DESC")
    suspend fun getAllForLanguageBrowse(language: String): List<WordEntity>

    /**
     * Deletes one word the user chose to remove from their personal
     * dictionary (via the Personal Dictionary screen). Scoped to both
     * [word] and [language] together since the primary key is the pair —
     * the same word can exist as separate Sinhala and English entries.
     */
    @Query("DELETE FROM words WHERE word = :word AND language = :language")
    suspend fun delete(word: String, language: String)

    /**
     * Learns [word]: inserts it if new, or bumps its frequency/lastUsed if it
     * already exists. This is how the keyboard "remembers" words the user
     * has typed so they can be suggested again later.
     */
    @Query(
        """
        INSERT INTO words (word, language, frequency, lastUsed)
        VALUES (:word, :language, 1, :now)
        ON CONFLICT(word, language) DO UPDATE SET
            frequency = frequency + 1,
            lastUsed = :now
        """
    )
    suspend fun learnWord(word: String, language: String, now: Long = System.currentTimeMillis())

    /**
     * Inserts [word] as a base-dictionary entry (see DictionarySeeder) at a
     * moderate starting [frequency] — only if it isn't already present.
     * Unlike learnWord(), this never bumps an existing row: seeding runs on
     * every app start (see DictionarySeeder's own has-seeded guard, which
     * this duplicates defensively), and repeatedly "learning" ~3000 bundled
     * words on every launch would both be wasteful and would let bundled
     * words silently out-rank words the user has genuinely typed many
     * times, defeating the point of frequency-based ranking.
     */
    @Query(
        """
        INSERT OR IGNORE INTO words (word, language, frequency, lastUsed)
        VALUES (:word, :language, :frequency, :now)
        """
    )
    suspend fun seedWord(word: String, language: String, frequency: Int = 1, now: Long = 0L)

    /**
     * Raises an already-seeded word's frequency/lastUsed up to the current
     * seed baseline — used only when DictionarySeeder's SEED_VERSION bumps
     * with a higher SEED_FREQUENCY than a previous install already seeded
     * at (its doc comment explains why: v1 shipped frequency=1/lastUsed=0,
     * which made seeded words effectively invisible against any word the
     * user had ever typed). Deliberately scoped to `WHERE frequency =
     * :oldFrequency` rather than a blanket update — this must NEVER touch a
     * row the user has actually typed (frequency > the old seed baseline),
     * or it would silently erase real usage history under the guise of a
     * dictionary "upgrade".
     */
    @Query(
        """
        UPDATE words
        SET frequency = :newFrequency, lastUsed = :now
        WHERE language = :language AND frequency = :oldFrequency
        """
    )
    suspend fun upgradeStaleSeedFrequency(language: String, oldFrequency: Int, newFrequency: Int, now: Long)
}
