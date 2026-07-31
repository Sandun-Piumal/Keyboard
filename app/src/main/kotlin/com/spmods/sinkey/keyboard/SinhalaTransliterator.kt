package com.spmods.sinkey.keyboard

/**
 * Converts a raw Latin ("Singlish") buffer typed by the user into its
 * phonetic Sinhala rendering, e.g. "kohomada" -> "කොහොමද".
 *
 * The mapping tables below were derived from SinKey's bundled transliteration
 * model (an OpenFst/Phonetisaurus grapheme model trained on real Sinhala
 * transliteration data) by decoding its symbol tables and start-state arcs.
 * That gave us, for every consonant/cluster, the ranked set of Sinhala
 * outputs it can produce (e.g. "ka" -> කා/කැ/කෑ). Rather than embedding the
 * ~12MB FST binary and its native (.so) runtime just to look up single
 * syllables, this file captures the *top-ranked* mapping as a small, fast,
 * pure-Kotlin table — cheap enough to run on every keystroke with no native
 * dependency, no asset loading, and no JNI boundary.
 *
 * ALGORITHM
 * The buffer is split greedily from the left into (consonant?)(vowel-or-
 * modifier?) syllables, always preferring the longest matching piece at each
 * position (so "th" is read as a single aspirated consonant before "t"+"h"
 * would ever be tried, and "aa" as the long vowel modifier before "a"+"a").
 * Each syllable is looked up independently and the results are concatenated,
 * which mirrors how the underlying FST composes its output one syllable at a
 * time along its most likely path.
 */
object SinhalaTransliterator {

    // Recognized consonant clusters (including aspirated/digraph forms).
    // Written order doesn't matter -- consonantsByLength below sorts by
    // length so longer clusters ('th') are always tried before shorter
    // prefixes ('t') would otherwise match first.
    private val CONSONANT_MAP: Map<String, String> = linkedMapOf(
        // Digraphs / aspirated / retroflex / special clusters first
        "th" to "ත", "dh" to "ධ", "kh" to "ඛ", "gh" to "ඝ", "ch" to "ච",
        "jh" to "ඣ", "ph" to "ෆ", "bh" to "භ", "sh" to "ශ", "Sh" to "ෂ",
        "ng" to "ඟ", "nd" to "ඳ", "mb" to "ඹ", "gn" to "ඥ", "kn" to "ඤ",
        "th'" to "ථ",
        // Single consonants
        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ", "t" to "ට",
        "d" to "ද", "n" to "න", "p" to "ප", "b" to "බ", "m" to "ම",
        "y" to "ය", "r" to "ර", "l" to "ල", "v" to "ව", "w" to "ව",
        "s" to "ස", "h" to "හ", "f" to "ෆ", "q" to "ක", "x" to "ක්ෂ",
        "z" to "ළ"
    )

    // Special-case *whole consonant* alternates a user may intend by
    // capitalizing the first letter (retroflex/other row), e.g. "Ta" -> ටා
    // instead of "ta" -> තා. Checked only when the syllable's consonant part
    // starts with an uppercase letter.
    private val RETROFLEX_MAP: Map<String, String> = mapOf(
        "t" to "ත", "d" to "ඩ", "n" to "ණ", "l" to "ළ", "s" to "ෂ"
    )

    // Vowel modifiers (pillam), matched longest-first via vowelsByLength
    // below. The inherent vowel (bare consonant, no sign written) is handled
    // directly in transliterate() rather than listed here.
    private val VOWEL_SIGN: Map<String, String> = linkedMapOf(
        "aa" to "ා", "A" to "ා",
        "ae" to "ැ", "aae" to "ෑ", "AE" to "ෑ",
        "ii" to "ී", "I" to "ී",
        "uu" to "ූ", "U" to "ූ",
        "ee" to "ේ", "E" to "ේ",
        "oo" to "ෝ", "O" to "ෝ",
        "ai" to "ෛ",
        "au" to "ෞ",
        "i" to "ි",
        "u" to "ු",
        "e" to "ෙ",
        "o" to "ො"
    )

    // Standalone (non-consonant-attached) vowels, i.e. when a vowel appears
    // at the start of a syllable rather than after a consonant.
    private val STANDALONE_VOWEL: Map<String, String> = linkedMapOf(
        "aa" to "ආ", "A" to "ආ",
        "ae" to "ඇ",
        "aae" to "ඈ", "AE" to "ඈ",
        "ii" to "ඊ", "I" to "ඊ",
        "uu" to "ඌ", "U" to "ඌ",
        "ee" to "ඒ", "E" to "ඒ",
        "oo" to "ඕ", "O" to "ඕ",
        "ai" to "ඓ",
        "au" to "ඖ",
        "a" to "අ",
        "i" to "ඉ",
        "u" to "උ",
        "e" to "එ",
        "o" to "ඔ"
    )

    // Explicit "kill the inherent vowel" marker some Singlish schemes use:
    // typing an apostrophe or trailing "q" after a bare consonant asks for
    // the pure consonant + hal kirīma (්) instead of the inherent-'a' glyph,
    // e.g. "k'" -> ක්. Only the apostrophe form is enabled by default since
    // trailing "q" collides with normal "ක" typing.
    private const val HAL_MARKER = "\u0DCA" // ්

    private val consonantsByLength = CONSONANT_MAP.keys.sortedByDescending { it.length }
    private val vowelsByLength = VOWEL_SIGN.keys.sortedByDescending { it.length }
    private val standaloneByLength = STANDALONE_VOWEL.keys.sortedByDescending { it.length }

    /**
     * Transliterates [input] (raw Latin/Singlish buffer, as typed so far)
     * into Sinhala. Safe to call on partial words on every keystroke; it
     * always returns *some* rendering, degrading to passing through any
     * character it can't map so the user's typing is never silently eaten.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0
        val n = input.length

        while (i < n) {
            // 1) Explicit hal-kirima: consonant immediately followed by an
            // apostrophe forces the "pure" consonant with no vowel sound.
            val consonantMatch = matchLongest(input, i, consonantsByLength)
            if (consonantMatch != null) {
                val (consKey, consLen) = consonantMatch
                val glyph = resolveConsonantGlyph(input, i, consKey)
                var j = i + consLen
                if (j < n && input[j] == '\'') {
                    out.append(glyph).append(HAL_MARKER)
                    i = j + 1
                    continue
                }
                val vowelMatch = matchLongest(input, j, vowelsByLength)
                if (vowelMatch != null) {
                    val (vKey, vLen) = vowelMatch
                    out.append(glyph).append(VOWEL_SIGN.getValue(vKey))
                    i = j + vLen
                } else if (j < n && input[j] == 'a') {
                    // A single bare "a" right after a consonant is the
                    // *inherent* vowel already carried by the bare glyph
                    // (e.g. "ka" -> ක, not ක+අ) -- consume it without adding
                    // a separate vowel sign. "aa"/"ae"/... are already
                    // claimed above by vowelsByLength since it tries longest
                    // matches first, so this only fires for a lone "a".
                    out.append(glyph)
                    i = j + 1
                } else {
                    // No vowel follows at all -> inherent 'a' sound, bare glyph.
                    out.append(glyph)
                    i = j
                }
                continue
            }

            // 2) Standalone vowel (word-initial or after another vowel).
            val standaloneMatch = matchLongest(input, i, standaloneByLength)
            if (standaloneMatch != null) {
                val (vKey, vLen) = standaloneMatch
                out.append(STANDALONE_VOWEL.getValue(vKey))
                i += vLen
                continue
            }

            // 3) Anything unrecognized (digits, punctuation, spaces, already-
            // Sinhala text pasted mid-buffer) passes through unchanged so
            // nothing the user typed silently disappears.
            out.append(input[i])
            i += 1
        }
        return out.toString()
    }

    /** Finds the longest key from [candidates] that matches input starting at [pos], case-sensitively first, then case-insensitively. */
    private fun matchLongest(input: String, pos: Int, candidates: List<String>): Pair<String, Int>? {
        if (pos >= input.length) return null
        for (key in candidates) {
            val len = key.length
            if (pos + len <= input.length) {
                val slice = input.substring(pos, pos + len)
                if (slice == key) return key to len
                if (slice.equals(key, ignoreCase = true) && key.none { it.isUpperCase() }) {
                    return key to len
                }
            }
        }
        return null
    }

    /**
     * Picks the actual glyph for a matched consonant key, applying the
     * retroflex/alternate-row override when the user capitalized the
     * consonant (e.g. "Ta" for ට rather than "ta" for ත).
     */
    private fun resolveConsonantGlyph(input: String, pos: Int, key: String): String {
        val typed = input.substring(pos, pos + key.length)
        if (typed.isNotEmpty() && typed[0].isUpperCase()) {
            RETROFLEX_MAP[key.lowercase()]?.let { return it }
        }
        return CONSONANT_MAP.getValue(key)
    }
}
