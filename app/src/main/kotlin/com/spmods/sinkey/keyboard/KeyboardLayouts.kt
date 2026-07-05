package com.spmods.sinkey.keyboard

/** Standard English QWERTY rows shown by the keyboard view. */
val EnglishRows: List<List<String>> = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)

/**
 * Singlish → Sinhala Unicode transliterator.
 *
 * Rules (user-defined):
 *   consonant + "a"        → bare consonant (inherent vowel)   ka → ක
 *   consonant + vowel sign → consonant + sign                  ki → කි
 *   consonant alone        → consonant + hal kirima            k  → ක්
 *   standalone vowel       → independent vowel                 a  → අ
 *   n/m before a different consonant → anusvara               nk → ංක
 *   ruu / ru / lu          → special standalone forms
 */
object SinhalaTransliterator {

    // Special standalone forms — matched before anything else (longest first)
    private val specials = listOf(
        "ruu" to "ඎ",
        "ru"  to "ඍ",
        "lu"  to "ළු"
    )

    // Compound consonant bases (longest first)
    private val compoundBases = listOf(
        "ndh" to "ඳ",
        "nd"  to "ඬ",
        "ng"  to "ඟ",
        "mb"  to "ඹ",
        "gn"  to "ඥ",
        "kn"  to "ඤ",
        "sh"  to "ශ",
        "Sh"  to "ෂ",
        "th"  to "ත",
        "dh"  to "ද",
        "ch"  to "ච"
    )

    // Single consonant bases
    private val singleBases = listOf(
        "k" to "ක",  "g" to "ග",  "t" to "ට",  "d" to "ඩ",
        "p" to "ප",  "b" to "බ",  "c" to "ච",  "j" to "ජ",
        "m" to "ම",  "n" to "න",  "N" to "ණ",  "y" to "ය",
        "r" to "ර",  "l" to "ල",  "L" to "ළ",  "v" to "ව",
        "w" to "ව",  "s" to "ස",  "h" to "හ",  "f" to "ෆ"
    )

    // Vowel signs that follow a consonant base (longest first)
    private val vowelSigns = listOf(
        "aae" to "ෑ",
        "aa"  to "ා",
        "ae"  to "ැ",
        "ii"  to "ී",
        "ie"  to "ී",
        "uu"  to "ූ",
        "ee"  to "ේ",
        "ea"  to "ේ",
        "oo"  to "ෝ",
        "oa"  to "ෝ",
        "ai"  to "ෛ",
        "au"  to "ෞ",
        "i"   to "ි",
        "u"   to "ු",
        "e"   to "ෙ",
        "o"   to "ො"
        // "a" = inherent vowel, handled separately — no sign needed
    )

    // Independent vowels (word-initial or standalone) — longest first
    private val independentVowels = listOf(
        "aae" to "ඈ",
        "aa"  to "ආ",
        "ae"  to "ඇ",
        "ii"  to "ඊ",
        "ie"  to "ඊ",
        "uu"  to "ඌ",
        "ee"  to "ඒ",
        "ea"  to "ඒ",
        "oo"  to "ඕ",
        "oa"  to "ඕ",
        "ai"  to "ඓ",
        "au"  to "ඖ",
        "i"   to "ඉ",
        "u"   to "උ",
        "e"   to "එ",
        "o"   to "ඔ",
        "a"   to "අ"
    )

    private val halKirima = "්"
    private val anusvara  = "ං"

    // Characters that start a consonant (used for anusvara detection)
    private val consonantStarts = setOf(
        'k','g','t','d','p','b','c','j','m','n','N','y','r','l','L',
        'v','w','s','h','f','K','G','T','D','P','B','C','J','M','Y',
        'R','S','H','F'
    )

    /**
     * Transliterates a romanized Singlish string into Sinhala Unicode.
     *
     * Per-position algorithm:
     *  1. Try special standalone forms (ru, ruu, lu).
     *  2. Anusvara check: if current char is n/m and next char is a DIFFERENT
     *     consonant (not forming a known compound), emit anusvara (ං).
     *  3. Try compound bases, then single bases.
     *     After matching a base:
     *       - bare 'a' follows (not ae/ai/au/aa) → emit base only (inherent vowel)
     *       - vowel sign follows → emit base + sign
     *       - nothing vowel-like → emit base + hal kirima (්)
     *  4. Try independent vowel.
     *  5. Fallback: emit character as-is.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0

        while (i < input.length) {

            // 1. Special standalone forms
            val sp = tryMatch(input, i, specials)
            if (sp != null) {
                out.append(sp.second); i += sp.first.length; continue
            }

            // 2. Anusvara: n/m before a different consonant (not gemination,
            //    and not the start of a known compound like nd, ng, mb…)
            if (input[i] == 'n' || input[i] == 'm') {
                val comp = tryMatch(input, i, compoundBases)
                if (comp == null) {
                    val nextPos = i + 1
                    if (nextPos < input.length &&
                        input[nextPos] in consonantStarts &&
                        input[nextPos] != input[i]          // not gemination (mm, nn)
                    ) {
                        out.append(anusvara); i++; continue
                    }
                }
            }

            // 3. Consonant base (compound first, then single)
            val base = tryMatch(input, i, compoundBases)
                ?: tryMatch(input, i, singleBases)

            if (base != null) {
                val afterBase = i + base.first.length

                when {
                    // Inherent 'a': bare consonant, no vowel sign, no hal
                    afterBase < input.length &&
                    input[afterBase] == 'a' &&
                    !input.startsWith("ae", afterBase) &&
                    !input.startsWith("ai", afterBase) &&
                    !input.startsWith("au", afterBase) &&
                    !input.startsWith("aa", afterBase) -> {
                        out.append(base.second)
                        i = afterBase + 1
                    }
                    // Vowel sign
                    else -> {
                        val vowel = tryMatch(input, afterBase, vowelSigns)
                        if (vowel != null) {
                            out.append(base.second)
                            out.append(vowel.second)
                            i = afterBase + vowel.first.length
                        } else {
                            // Consonant alone → hal kirima
                            out.append(base.second)
                            out.append(halKirima)
                            i = afterBase
                        }
                    }
                }
                continue
            }

            // 4. Independent vowel
            val vowel = tryMatch(input, i, independentVowels)
            if (vowel != null) {
                out.append(vowel.second); i += vowel.first.length; continue
            }

            // 5. Fallback
            out.append(input[i]); i++
        }

        return out.toString()
    }

    private fun tryMatch(
        input: String,
        pos: Int,
        rules: List<Pair<String, String>>
    ): Pair<String, String>? {
        for (rule in rules) {
            val key = rule.first
            if (pos + key.length <= input.length &&
                input.substring(pos, pos + key.length) == key
            ) return rule
        }
        return null
    }
}
