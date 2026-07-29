package com.spmods.sinkey.keyboard

/**
 * Converts plain English text into the Unicode "fancy text" code points for
 * a given [com.spmods.sinkey.data.FancyTextStyle]. This is what makes
 * TOOL_FONT actually visible in the receiving app: unlike a Compose
 * FontFamily (which only affects how the keyboard itself draws its key
 * labels), these are genuinely different Unicode characters, so any app
 * renders them in the chosen style exactly as committed.
 *
 * Only a-z, A-Z, 0-9 are mapped; anything else (spaces, punctuation,
 * Sinhala) passes through unchanged. A few styles (SMALL_CAPS,
 * UPSIDE_DOWN, CIRCLED) don't have Unicode digit variants for every value,
 * in which case the plain digit is kept.
 */
object FancyTextMapper {

    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"

    // Each table is LOWER+UPPER+DIGITS worth of replacement strings, in the
    // same order, built from the Unicode Mathematical Alphanumeric Symbols
    // block (and a couple of legacy blocks for small-caps/upside-down/circled
    // which predate that block and aren't part of it).
    private fun mathTable(lowerStart: Int, upperStart: Int, digitStart: Int?): List<String> {
        val out = mutableListOf<String>()
        for (i in LOWER.indices) out.add(String(Character.toChars(lowerStart + i)))
        for (i in UPPER.indices) out.add(String(Character.toChars(upperStart + i)))
        if (digitStart != null) {
            for (i in DIGITS.indices) out.add(String(Character.toChars(digitStart + i)))
        } else {
            for (c in DIGITS) out.add(c.toString())
        }
        return out
    }

    private val boldTable = mathTable(0x1D41A, 0x1D400, 0x1D7CE)
    private val italicTable = mathTable(0x1D44E, 0x1D434, null) // no italic digits in Unicode
    private val boldItalicTable = mathTable(0x1D482, 0x1D468, null)
    private val scriptTable = run {
        // Mathematical Script has gaps at a few letters that instead use
        // pre-existing legacy Letterlike Symbols code points.
        val exceptions = mapOf(
            'B' to "ℬ", 'E' to "ℰ", 'F' to "ℱ", 'H' to "ℋ", 'I' to "ℐ",
            'L' to "ℒ", 'M' to "ℳ", 'R' to "ℛ",
            'e' to "ℯ", 'g' to "ℊ", 'o' to "ℴ"
        )
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(exceptions[c] ?: String(Character.toChars(0x1D4B6 + (c - 'a'))))
        for (c in UPPER) out.add(exceptions[c] ?: String(Character.toChars(0x1D49C + (c - 'A'))))
        for (c in DIGITS) out.add(c.toString())
        out
    }
    private val doubleStruckTable = run {
        val exceptions = mapOf('C' to "ℂ", 'H' to "ℍ", 'N' to "ℕ", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'Z' to "ℤ")
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(String(Character.toChars(0x1D552 + (c - 'a'))))
        for (c in UPPER) out.add(exceptions[c] ?: String(Character.toChars(0x1D538 + (c - 'A'))))
        for (i in DIGITS.indices) out.add(String(Character.toChars(0x1D7D8 + i)))
        out
    }
    private val frakturTable = run {
        val exceptions = mapOf('C' to "ℭ", 'H' to "ℌ", 'I' to "ℑ", 'R' to "ℜ", 'Z' to "ℨ")
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(String(Character.toChars(0x1D51E + (c - 'a'))))
        for (c in UPPER) out.add(exceptions[c] ?: String(Character.toChars(0x1D504 + (c - 'A'))))
        for (c in DIGITS) out.add(c.toString())
        out
    }
    private val monospaceTable = mathTable(0x1D68A, 0x1D670, 0x1D7F6)
    private val circledTable = run {
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(String(Character.toChars(0x24D0 + (c - 'a')))) // ⓐ..ⓩ
        for (c in UPPER) out.add(String(Character.toChars(0x24B6 + (c - 'A')))) // Ⓐ..Ⓩ
        out.add("⓪")
        for (i in 1..9) out.add(String(Character.toChars(0x2460 + (i - 1)))) // ①..⑨
        out
    }
    private val smallCapsTable = run {
        // Small caps has no true capitals or digits — keep those as-is.
        val map = mapOf(
            'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ꜰ",
            'g' to "ɢ", 'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ",
            'm' to "ᴍ", 'n' to "ɴ", 'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ǫ", 'r' to "ʀ",
            's' to "s", 't' to "ᴛ", 'u' to "ᴜ", 'v' to "ᴠ", 'w' to "ᴡ", 'x' to "x",
            'y' to "ʏ", 'z' to "ᴢ"
        )
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(map[c] ?: c.toString())
        for (c in UPPER) out.add(map[c.lowercaseChar()] ?: c.toString())
        for (c in DIGITS) out.add(c.toString())
        out
    }
    private val upsideDownTable = run {
        val map = mapOf(
            'a' to "ɐ", 'b' to "q", 'c' to "ɔ", 'd' to "p", 'e' to "ǝ", 'f' to "ɟ",
            'g' to "ƃ", 'h' to "ɥ", 'i' to "ᴉ", 'j' to "ɾ", 'k' to "ʞ", 'l' to "l",
            'm' to "ɯ", 'n' to "u", 'o' to "o", 'p' to "d", 'q' to "b", 'r' to "ɹ",
            's' to "s", 't' to "ʇ", 'u' to "n", 'v' to "ʌ", 'w' to "ʍ", 'x' to "x",
            'y' to "ʎ", 'z' to "z"
        )
        val digitMap = mapOf('0' to "0", '1' to "Ɩ", '2' to "ᄅ", '3' to "Ɛ", '4' to "ㄣ",
            '5' to "5", '6' to "9", '7' to "ㄥ", '8' to "8", '9' to "6")
        val out = mutableListOf<String>()
        for (c in LOWER) out.add(map[c] ?: c.toString())
        for (c in UPPER) out.add(map[c.lowercaseChar()] ?: c.toString())
        for (c in DIGITS) out.add(digitMap[c] ?: c.toString())
        out
    }

    private fun tableFor(style: com.spmods.sinkey.data.FancyTextStyle): List<String>? =
        when (style) {
            com.spmods.sinkey.data.FancyTextStyle.NONE -> null
            com.spmods.sinkey.data.FancyTextStyle.BOLD -> boldTable
            com.spmods.sinkey.data.FancyTextStyle.ITALIC -> italicTable
            com.spmods.sinkey.data.FancyTextStyle.BOLD_ITALIC -> boldItalicTable
            com.spmods.sinkey.data.FancyTextStyle.SCRIPT -> scriptTable
            com.spmods.sinkey.data.FancyTextStyle.DOUBLE_STRUCK -> doubleStruckTable
            com.spmods.sinkey.data.FancyTextStyle.FRAKTUR -> frakturTable
            com.spmods.sinkey.data.FancyTextStyle.MONOSPACE -> monospaceTable
            com.spmods.sinkey.data.FancyTextStyle.CIRCLED -> circledTable
            com.spmods.sinkey.data.FancyTextStyle.SMALL_CAPS -> smallCapsTable
            com.spmods.sinkey.data.FancyTextStyle.UPSIDE_DOWN -> upsideDownTable
        }

    private val alphabet = LOWER + UPPER + DIGITS

    /** Applies [style] to every a-z/A-Z/0-9 character in [input]; everything else passes through unchanged. */
    fun apply(input: String, style: com.spmods.sinkey.data.FancyTextStyle): String {
        val table = tableFor(style) ?: return input
        val out = StringBuilder(input.length)
        for (ch in input) {
            val idx = alphabet.indexOf(ch)
            out.append(if (idx >= 0) table[idx] else ch.toString())
        }
        return out.toString()
    }
}

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
            //    BUG FIX: also skip anusvara when the NEXT position starts a
            //    known compound (th, dh, sh, Sh, ch, kn, gn…) — e.g. "ntha"
            //    is n + tha (අන්ත), not anusvara + ta (අංත). Previously only
            //    checked whether n/m itself began a compound, not whether the
            //    following letters did, so words like "anthaya"/"sampatha"
            //    rendered with a wrong ං instead of ්.
            if (input[i] == 'n' || input[i] == 'm') {
                val comp = tryMatch(input, i, compoundBases)
                val nextStartsCompound = tryMatch(input, i + 1, compoundBases) != null
                if (comp == null && !nextStartsCompound) {
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
