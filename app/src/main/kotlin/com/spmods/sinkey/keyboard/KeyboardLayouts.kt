package com.spmods.sinkey.keyboard

/** A single visible key on the QWERTY-shaped keyboard grid. */
data class KeyDef(val label: String, val row: Int)

/** Standard English QWERTY rows shown by the keyboard view. */
val EnglishRows: List<List<String>> = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)

/**
 * Sinhala phonetic ("Singlish") transliteration engine.
 *
 * The user types romanized Sinhala (e.g. "kohomada") and this converts it to
 * Sinhala Unicode ("කොහොමද") using longest-match-first rule substitution —
 * the same general approach used by Helakuru / Google Sinhala input.
 *
 * The QWERTY key grid stays identical to English; only the *committed*
 * text differs, produced by [SinhalaTransliterator.transliterate] which the
 * IME calls on word boundaries (space / punctuation / enter).
 */
object SinhalaTransliterator {

    // Ordered longest-match-first. Vowels/modifiers must be checked before
    // shorter substrings they contain (e.g. "th" before "t").
    private val rules: List<Pair<String, String>> = listOf(
        // independent vowels
        "aa" to "ආ", "ae" to "ඇ", "aae" to "ඈ", "ii" to "ඊ", "i" to "ඉ",
        "uu" to "ඌ", "u" to "උ", "ee" to "ඒ", "e" to "එ", "ai" to "ඓ",
        "oo" to "ඕ", "o" to "ඔ", "au" to "ඖ", "a" to "අ",

        // consonant + inherent vowel combos (romanized syllables -> glyph clusters)
        "kh" to "ඛ", "gh" to "ඝ", "ch" to "ච", "jh" to "ඣ",
        "th" to "ථ", "dh" to "ධ", "ph" to "ඵ", "bh" to "භ",
        "ng" to "ඞ", "ny" to "ඤ", "sh" to "ශ",

        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ",
        "t" to "ට", "d" to "ඩ", "n" to "න", "p" to "ප",
        "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර",
        "l" to "ල", "v" to "ව", "w" to "ව", "s" to "ස",
        "h" to "හ", "f" to "ෆ", "x" to "ක්ස්", "z" to "ස්",

        // dependent vowel signs, appended after a consonant is already committed
        "aa" to "ා", "ae" to "ැ", "i" to "ි", "ii" to "ී",
        "u" to "ු", "uu" to "ූ", "e" to "ෙ", "ee" to "ේ",
        "o" to "ො", "oo" to "ෝ",

        // pure virama / hal kirīma
        "q" to "්"
    )

    /**
     * Converts a single romanized word into Sinhala Unicode.
     * This is a lightweight best-effort transliterator, not a full
     * linguistic engine — it covers the common phonetic patterns typed
     * in everyday chat Sinhala.
     */
    fun transliterate(word: String): String {
        if (word.isEmpty()) return word
        val lower = word.lowercase()
        val out = StringBuilder()
        var i = 0
        while (i < lower.length) {
            var matched = false
            // try longest rule keys first (max key length is 3)
            for (len in 3 downTo 1) {
                if (i + len > lower.length) continue
                val chunk = lower.substring(i, i + len)
                val rule = rules.firstOrNull { it.first == chunk }
                if (rule != null) {
                    out.append(rule.second)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                out.append(lower[i])
                i += 1
            }
        }
        return out.toString()
    }
}
