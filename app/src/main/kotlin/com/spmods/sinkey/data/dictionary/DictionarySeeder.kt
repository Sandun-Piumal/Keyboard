package com.spmods.sinkey.data.dictionary

import android.content.Context
import com.spmods.sinkey.data.PreferencesManager
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads the bundled base word lists (assets/wordlist_en.txt,
 * assets/wordlist_si.txt — one word per line, common everyday vocabulary)
 * into the personal dictionary (the same `words` table ordinary typing
 * learns into and ordinary prefix-based suggestions read from) at a
 * moderate starting frequency.
 *
 * Why this exists: WordRepository's suggestion/gesture-matching logic reads
 * entirely from the `words` table, which otherwise starts completely empty
 * on a fresh install — gesture typing in particular (GestureWordMatcher)
 * needs a real vocabulary to score swipe paths against, not just whatever
 * the user happens to have typed so far. Seeding these bundled lists once
 * gives every feature that reads from `words` a reasonable starting point,
 * including ordinary typed-prefix suggestions (findByPrefix/
 * fetchPersonalSuggestions) — not just gesture typing.
 *
 * Seeded words use SEED_FREQUENCY (3) rather than 1: findByPrefix orders by
 * "frequency DESC, lastUsed DESC" and is called with a small limit (3-5),
 * so a frequency of 1 meant a seeded word got pushed out of the suggestion
 * strip by literally any word the user had ever typed for that prefix, even
 * once — in effect making seeded words invisible to ordinary typing
 * suggestions and only ever reachable via gesture typing's full-table scan
 * (WordDao.getAllForLanguage, which doesn't apply a limit before scoring).
 * 3 is still low enough that a genuinely-typed word overtakes it after just
 * two real uses (learnWord bumps frequency by 1 per use), so this doesn't
 * let bundled words drown out the user's own vocabulary — it only stops
 * them from being ranked *last* against words that have never been typed
 * at all.
 *
 * lastUsed is similarly set to the seeding run's current time rather than
 * epoch 0: with epoch 0, a seeded word always lost the "lastUsed DESC"
 * tiebreaker to literally any real word ever typed, compounding the same
 * invisibility problem independently of the frequency fix above.
 */
object DictionarySeeder {

    // Bump this if the bundled word list assets are ever replaced with a
    // larger/updated set, or if SEED_FREQUENCY changes — seedIfNeeded()
    // re-runs (cheaply; OR IGNORE skips rows that already exist) whenever
    // the stored version is lower than this, so returning users pick up
    // new bundled words without any of their learned frequencies being
    // touched.
    //
    // v1 → v2: raised SEED_FREQUENCY from 1 to 3 and lastUsed from epoch 0
    // to seed-time (see SEED_FREQUENCY's doc comment) — devices already on
    // v1 get their existing seeded rows upgraded in place via
    // upgradeStaleSeedFrequency() below, not re-inserted (seedWord's OR
    // IGNORE would otherwise leave v1's stale frequency=1 rows untouched
    // forever, since the words themselves already exist).
    private const val SEED_VERSION = 2
    private const val SEED_FREQUENCY = 3

    // The exact frequency/lastUsed v1 shipped with — needed to safely
    // identify "this row is still at its original v1 seed value and hasn't
    // been typed since" when upgrading. If SEED_FREQUENCY is ever bumped
    // again in the future, add a new PREVIOUS_SEED_FREQUENCY constant for
    // that version rather than reusing this one, so upgradeStaleSeedFrequency
    // always targets the *immediately preceding* seed baseline.
    private const val V1_SEED_FREQUENCY = 1

    private val ASSET_FILES = mapOf(
        "en" to "wordlist_en.txt",
        "si" to "wordlist_si.txt"
    )

    /**
     * No-ops instantly if this device's stored seed version (see
     * PreferencesManager.dictionarySeedVersion) is already current — safe
     * to call unconditionally on every app/service start (see
     * WordRepository.seedBaseDictionaryIfNeeded, its only caller).
     */
    suspend fun seedIfNeeded(context: Context, dao: WordDao) {
        val prefs = PreferencesManager(context)
        val currentVersion = prefs.dictionarySeedVersion.first()
        if (currentVersion >= SEED_VERSION) return

        // One shared timestamp for this whole seeding run, not
        // System.currentTimeMillis() per word — a real "lastUsed" should
        // mean "last time this exact word was actually used", and giving
        // thousands of never-used seeded words a spread of fake distinct
        // timestamps would be more misleading than one honest "this was
        // seeded at approximately this moment" value shared across all of
        // them.
        val seedTime = System.currentTimeMillis()

        for ((language, assetName) in ASSET_FILES) {
            loadAssetWords(context, assetName).forEach { word ->
                dao.seedWord(word = word, language = language, frequency = SEED_FREQUENCY, now = seedTime)
            }
            // Devices that already ran v1 seeding have these words present
            // at frequency=1/lastUsed=0 already, so the seedWord() call
            // above skipped them (OR IGNORE). Explicitly raise any row
            // still sitting at exactly the old v1 baseline up to the
            // current one — scoped by frequency so a word the user has
            // since typed (frequency > 1) is never touched.
            if (currentVersion in 1 until SEED_VERSION) {
                dao.upgradeStaleSeedFrequency(
                    language = language,
                    oldFrequency = V1_SEED_FREQUENCY,
                    newFrequency = SEED_FREQUENCY,
                    now = seedTime
                )
            }
        }

        prefs.setDictionarySeedVersion(SEED_VERSION)
    }

    /**
     * Reads one word per line from assets/[assetName]. Returns an empty
     * list (rather than throwing) if the asset is missing — seeding is a
     * best-effort enhancement, not something that should crash keyboard
     * startup if, say, a build variant strips assets.
     */
    private fun loadAssetWords(context: Context, assetName: String): List<String> {
        return try {
            context.assets.open(assetName).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readLines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DictionarySeeder", "Failed to load $assetName", e)
            emptyList()
        }
    }
}
