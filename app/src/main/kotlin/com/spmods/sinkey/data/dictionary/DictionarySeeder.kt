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
 * learns into) at a low starting frequency.
 *
 * Why this exists: WordRepository's suggestion/gesture-matching logic reads
 * entirely from the `words` table, which otherwise starts completely empty
 * on a fresh install — gesture typing in particular (GestureWordMatcher)
 * needs a real vocabulary to score swipe paths against, not just whatever
 * the user happens to have typed so far. Seeding these bundled lists once
 * gives every feature that reads from `words` a reasonable starting point.
 *
 * Seeded words use SEED_FREQUENCY (1) so they never outrank a word the user
 * has actually typed more than once — learnWord() bumps frequency by 1 on
 * every real use, so a genuinely-typed word overtakes its seeded frequency
 * the moment it's used a second time. seedWord()'s "INSERT OR IGNORE" also
 * means seeding can never lower or reset a frequency an already-learned
 * word has built up, even if that word happens to also be in the bundled
 * list.
 */
object DictionarySeeder {

    // Bump this if the bundled word list assets are ever replaced with a
    // larger/updated set — seedIfNeeded() re-runs (cheaply; OR IGNORE skips
    // rows that already exist) whenever the stored version is lower than
    // this, so returning users pick up new bundled words without any of
    // their learned frequencies being touched.
    private const val SEED_VERSION = 1
    private const val SEED_FREQUENCY = 1

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

        for ((language, assetName) in ASSET_FILES) {
            loadAssetWords(context, assetName).forEach { word ->
                dao.seedWord(word = word, language = language, frequency = SEED_FREQUENCY, now = 0L)
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
