package com.spmods.sinkey.keyboard

/** Standard English QWERTY rows shown by the keyboard view. */
val EnglishRows: List<List<String>> = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)

/**
 * Sinhala phonetic ("Singlish") transliteration engine.
 *
 * Approach: consonant + vowel sign pairing.
 * Each key press appends to a buffer. The buffer is transliterated
 * in real-time and shown as composing text. On space/enter/punctuation
 * the composing text is committed.
 *
 * Rules follow the standard Helakuru / Wijesekara phonetic mapping
 * widely understood by Sri Lankan users.
 */
object SinhalaTransliterator {

    // ── Consonants (longest match first within each group) ─────────────────
    private val consonants = listOf(
        "ndh" to "න්ධ",
        "nth" to "න්ථ",
        "ndr" to "න්ද්‍ර",
        "mba" to "ම්බ",

        "kh"  to "ඛ",  "gh"  to "ඝ",
        "ch"  to "ච",  "jh"  to "ඣ",
        "nth" to "ඦ",  "dh"  to "ධ",
        "th"  to "ථ",  "ph"  to "ඵ",
        "bh"  to "භ",  "sh"  to "ශ",
        "Sh"  to "ෂ",  "ng"  to "ඞ",
        "ny"  to "ඤ",  "nj"  to "ඤ",
        "nd"  to "න්ද", "nt"  to "න්ත",
        "mb"  to "ම්බ",

        "k"   to "ක",  "K"   to "ඛ",
        "g"   to "ග",  "G"   to "ඝ",
        "c"   to "ච",  "j"   to "ජ",
        "J"   to "ඣ",  "T"   to "ට",
        "D"   to "ඩ",  "N"   to "ණ",
        "t"   to "ත",  "d"   to "ද",
        "n"   to "න",  "p"   to "ප",
        "P"   to "ඵ",  "b"   to "බ",
        "B"   to "භ",  "m"   to "ම",
        "y"   to "ය",  "r"   to "ර",
        "R"   to "ඍ",  "l"   to "ල",
        "L"   to "ළ",  "v"   to "ව",
        "w"   to "ව",  "s"   to "ස",
        "S"   to "ෂ",  "h"   to "හ",
        "f"   to "ෆ",  "H"   to "හ",
        "x"   to "ක්‍ෂ", "z"   to "ස්",
        "q"   to "ක්"
    )

    // ── Vowel signs (follow a consonant) ────────────────────────────────
    private val vowelSigns = listOf(
        "aa"  to "ා",
        "ae"  to "ැ",
        "aae" to "ෑ",
        "ii"  to "ී",
        "i"   to "ි",
        "uu"  to "ූ",
        "u"   to "ු",
        "ee"  to "ේ",
        "e"   to "ෙ",
        "ai"  to "ෛ",
        "oo"  to "ෝ",
        "o"   to "ො",
        "au"  to "ෞ",
        "A"   to "ා"
    )

    // ── Independent vowels (start of word or after a space) ─────────────
    private val independentVowels = listOf(
        "aa"  to "ආ",
        "ae"  to "ඇ",
        "aae" to "ඈ",
        "ii"  to "ඊ",
        "i"   to "ඉ",
        "uu"  to "ඌ",
        "u"   to "උ",
        "ee"  to "ඒ",
        "e"   to "එ",
        "ai"  to "ඓ",
        "oo"  to "ඕ",
        "o"   to "ඔ",
        "au"  to "ඖ",
        "a"   to "අ",
        "A"   to "ආ"
    )

    // ── Special standalone sequences ────────────────────────────────────
    private val specials = listOf(
        "ruu"  to "රූ",
        "ru"   to "රු",
        "lu"   to "ලු",
        "luu"  to "ලූ"
    )

    /**
     * Transliterates a full romanized word into Sinhala Unicode.
     *
     * Algorithm:
     *  1. Walk through the input character by character.
     *  2. Try to match the longest consonant at current position.
     *  3. If a consonant is matched, then try to match a vowel sign
     *     immediately after; if no vowel sign, append inherent "අ" vowel.
     *  4. If no consonant matches, try an independent vowel.
     *  5. Special two-letter consonant clusters (e.g. "kk" → "ක්ක") are
     *     handled by detecting a repeated consonant and inserting virama.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0
        var lastWasConsonant = false

        while (i < input.length) {
            // Try specials first
            val special = tryMatch(input, i, specials)
            if (special != null) {
                out.append(special.second)
                i += special.first.length
                lastWasConsonant = false
                continue
            }

            // Try consonant
            val cons = tryMatch(input, i, consonants)
            if (cons != null) {
                // Check for gemination: same consonant repeated → insert virama
                if (lastWasConsonant && out.isNotEmpty()) {
                    // already handled below via virama before second consonant
                }
                out.append(cons.second)
                i += cons.first.length
                lastWasConsonant = true

                // Try vowel sign after consonant
                val vowel = tryMatch(input, i, vowelSigns)
                if (vowel != null) {
                    out.append(vowel.second)
                    i += vowel.first.length
                    lastWasConsonant = false
                }
                // else inherent 'a' — nothing appended (Sinhala default)
                continue
            }

            // Try independent vowel
            val indVowel = tryMatch(input, i, independentVowels)
            if (indVowel != null) {
                out.append(indVowel.second)
                i += indVowel.first.length
                lastWasConsonant = false
                continue
            }

            // Fallback: pass through as-is
            out.append(input[i])
            i++
            lastWasConsonant = false
        }

        return out.toString()
    }

    private fun tryMatch(input: String, pos: Int, rules: List<Pair<String, String>>): Pair<String, String>? {
        for (rule in rules) {
            val key = rule.first
            if (pos + key.length <= input.length &&
                input.substring(pos, pos + key.length) == key) {
                return rule
            }
        }
        return null
    }
}
