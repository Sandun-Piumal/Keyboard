package com.spmods.sinkey.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real, on-device typing statistics — everything here is written by actual
 * usage (the IME committing real keystrokes, or a completed Typing Test
 * result), never fabricated. Backed by the same DataStore as
 * [PreferencesManager] (see Context.dataStore there) so no second
 * DataStore file is created.
 *
 * "Points" and "Level" (used by the Profile screen) are *derived*, not
 * stored: 1 point per 1000 characters actually typed through the keyboard,
 * levels advance every 10 points. Nothing here is random or placeholder —
 * every number traces back to characters/words the user actually typed or
 * a typing test they actually completed.
 */
class TypingStatsRepository(private val context: Context) {

    private object Keys {
        // Every character committed to a real input field by the IME
        // (letters, punctuation typed one at a time — NOT bulk
        // paste/expansion text, see SinKeyInputMethodService's call site).
        val TOTAL_CHARACTERS = longPreferencesKey("stats_total_characters")
        // Every word boundary crossed (space/enter/punctuation after a
        // non-empty word buffer) — same signal already used to trigger
        // dictionary learning, reused here as the word-count source.
        val TOTAL_WORDS = longPreferencesKey("stats_total_words")
        // Best-ever result from the Typing Test screen.
        val BEST_WPM = intPreferencesKey("stats_best_wpm")
        // Most recent Typing Test result, shown on Profile as "last run".
        val LAST_WPM = intPreferencesKey("stats_last_wpm")
        val LAST_ACCURACY = intPreferencesKey("stats_last_accuracy")
        val TESTS_COMPLETED = intPreferencesKey("stats_tests_completed")
        // Consecutive-day streak bookkeeping. LAST_ACTIVE_DATE is a
        // yyyy-MM-dd string in the device's default locale/timezone so
        // "today" vs "yesterday" comparisons are simple date arithmetic,
        // not timestamp math that has to account for time-of-day.
        val LAST_ACTIVE_DATE = stringPreferencesKey("stats_last_active_date")
        val CURRENT_STREAK_DAYS = intPreferencesKey("stats_current_streak_days")
        // First day this device ever recorded typing activity — shown on
        // Profile as "Joined" instead of an invented signup date, since
        // this app has no accounts.
        val FIRST_ACTIVE_DATE = stringPreferencesKey("stats_first_active_date")
    }

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayString(): String = dayFormat.format(Date())

    val totalCharacters: Flow<Long> =
        context.dataStore.data.map { it[Keys.TOTAL_CHARACTERS] ?: 0L }
    val totalWords: Flow<Long> =
        context.dataStore.data.map { it[Keys.TOTAL_WORDS] ?: 0L }
    val bestWpm: Flow<Int> =
        context.dataStore.data.map { it[Keys.BEST_WPM] ?: 0 }
    val lastWpm: Flow<Int> =
        context.dataStore.data.map { it[Keys.LAST_WPM] ?: 0 }
    val lastAccuracy: Flow<Int> =
        context.dataStore.data.map { it[Keys.LAST_ACCURACY] ?: 0 }
    val testsCompleted: Flow<Int> =
        context.dataStore.data.map { it[Keys.TESTS_COMPLETED] ?: 0 }
    val currentStreakDays: Flow<Int> =
        context.dataStore.data.map { it[Keys.CURRENT_STREAK_DAYS] ?: 0 }
    val firstActiveDate: Flow<String?> =
        context.dataStore.data.map { it[Keys.FIRST_ACTIVE_DATE] }

    /**
     * Call once per character actually committed to a real input field by
     * the keyboard (see SinKeyInputMethodService's single handleKey() entry
     * point). Also rolls the daily-streak bookkeeping forward, since a
     * committed character is unambiguous evidence the user actively typed
     * today.
     */
    suspend fun recordCharacterTyped() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_CHARACTERS] = (prefs[Keys.TOTAL_CHARACTERS] ?: 0L) + 1
        }
        bumpStreakForToday()
    }

    /** Call once per completed word (space/enter/punctuation after a word). */
    suspend fun recordWordTyped() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_WORDS] = (prefs[Keys.TOTAL_WORDS] ?: 0L) + 1
        }
    }

    /** Call when a Typing Test screen run finishes with a real wpm/accuracy result. */
    suspend fun recordTestResult(wpm: Int, accuracy: Int) {
        context.dataStore.edit { prefs ->
            val previousBest = prefs[Keys.BEST_WPM] ?: 0
            if (wpm > previousBest) prefs[Keys.BEST_WPM] = wpm
            prefs[Keys.LAST_WPM] = wpm
            prefs[Keys.LAST_ACCURACY] = accuracy
            prefs[Keys.TESTS_COMPLETED] = (prefs[Keys.TESTS_COMPLETED] ?: 0) + 1
        }
        bumpStreakForToday()
    }

    /**
     * Advances the streak counter for "today" at most once per day:
     * unchanged if today was already recorded, +1 if yesterday was the
     * last active day, reset to 1 if there's a gap (or this is the very
     * first day). Also stamps FIRST_ACTIVE_DATE the first time this is
     * ever called on this device.
     */
    private suspend fun bumpStreakForToday() {
        val today = todayString()
        context.dataStore.edit { prefs ->
            val lastActive = prefs[Keys.LAST_ACTIVE_DATE]
            if (lastActive == today) return@edit // already counted today

            if (prefs[Keys.FIRST_ACTIVE_DATE] == null) {
                prefs[Keys.FIRST_ACTIVE_DATE] = today
            }

            val yesterday = dayFormat.format(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
            val currentStreak = prefs[Keys.CURRENT_STREAK_DAYS] ?: 0
            prefs[Keys.CURRENT_STREAK_DAYS] = if (lastActive == yesterday) currentStreak + 1 else 1
            prefs[Keys.LAST_ACTIVE_DATE] = today
        }
    }
}
