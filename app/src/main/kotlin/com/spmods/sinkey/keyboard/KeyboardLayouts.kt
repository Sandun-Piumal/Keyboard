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
/**
 * Romanized-Singlish → Sinhala Unicode transliterator.
 *
 * Rebuilt to follow the full "SINKEY — COMPLETE SINHALA PHONETIC RULESET"
 * spec: longest-match-first, context-aware parsing, ZWJ conjuncts
 * (rakaransaya/yansaya/kSha), explicit virama via 'x', productive
 * consonant-cluster support, and context-sensitive anusvara handling.
 */
object SinhalaTransliterator {

    private const val ZWJ = "\u200D"
    private const val HAL = "\u0DCA"           // ්
    private const val ANUSVARA = "\u0D82"      // ං

    // ---- Independent vowels (word-initial / standalone), longest first ----
    private val independentVowels = listOf(
        "aee" to "ඈ",
        "aa"  to "ආ",
        "ae"  to "ඇ",
        "ii"  to "ඊ",
        "i"   to "ඉ",
        "uu"  to "ඌ",
        "u"   to "උ",
        "ruu" to "ඎ",
        "ru"  to "ඍ",
        "luu" to "ඐ",
        "lu"  to "ඏ",
        "ee"  to "ඒ",
        "ai"  to "ඓ",
        "e"   to "එ",
        "oo"  to "ඕ",
        "au"  to "ඖ",
        "o"   to "ඔ",
        "a"   to "අ"
    )

    // ---- Consonant bases, longest key first so digraphs win over single letters ----
    private val consonants = listOf(
        "chh" to "ඡ",
        "kh" to "ඛ", "gh" to "ඝ", "ng" to "ඞ",
        "ch" to "ච", "jh" to "ඣ", "ny" to "ඤ", "gn" to "ඥ",
        "th" to "ථ", "dh" to "ධ", "sh" to "ශ",
        "nd" to "ඳ",
        "Sh" to "ෂ",
        "N"  to "ණ",
        "T"  to "ත",
        "D"  to "ද",
        "L"  to "ළ",
        "k" to "ක", "g" to "ග",
        "j" to "ජ",
        "t" to "ට", "d" to "ද", "n" to "න",
        "p" to "ප", "b" to "බ", "m" to "ම",
        "y" to "ය", "r" to "ර", "l" to "ල", "v" to "ව", "w" to "ව",
        "s" to "ස", "h" to "හ", "f" to "ෆ"
    )

    // Prenasalized letters — tried before the plain consonant list
    // (e.g. "nng" -> ඟ before "ng" -> ඞ, "nnd" -> ඬ before "nd" -> ඳ).
    private val prenasalized = listOf(
        "nng"  to "ඟ",
        "nyny" to "ඦ",
        "nnd"  to "ඬ",
        "mb"   to "ඹ"
    )

    // ---- Dependent vowel signs (pillam) that attach to a consonant base ----
    private val vowelSigns = listOf(
        "aee" to "ෑ",
        "aa"  to "ා",
        "ae"  to "ැ",
        "ii"  to "ී",
        "i"   to "ි",
        "uu"  to "ූ",
        "u"   to "ු",
        "ruu" to "ෲ",
        "ru"  to "ෘ",
        "luu" to "෣",
        "lu"  to "෢",
        "ee"  to "ේ",
        "ai"  to "ෛ",
        "e"   to "ෙ",
        "oo"  to "ෝ",
        "au"  to "ෞ",
        "o"   to "ො"
        // bare "a" = inherent vowel — no sign emitted
    )

    /**
     * Transliterates a romanized Singlish string into Sinhala Unicode
     * following the longest-match, context-aware ruleset.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0

        while (i < input.length) {
            val c = input[i]

            if (c.isWhitespace()) {
                out.append(c); i++; continue
            }

            // ---- Explicit special-sign tokens ----
            if (input.startsWith("ng_", i)) { out.append(ANUSVARA); i += 3; continue }
            if (input.startsWith("h_", i))  { out.append("\u0D83"); i += 2; continue } // ඃ visarga
            if (input.startsWith("h.", i))  { out.append("\u0D81"); i += 2; continue } // ඁ candrabindu

            // ---- Sinhala Lith digits: "<digit>s" ----
            if (i + 1 < input.length && input[i + 1] == 's' && c in '0'..'9') {
                val lith = "\u0DE6\u0DE7\u0DE8\u0DE9\u0DEA\u0DEB\u0DEC\u0DED\u0DEE\u0DEF"
                out.append(lith[c - '0']); i += 2; continue
            }

            // ---- kunda (෴) ----
            if (input.startsWith("kunda", i)) { out.append("\u0DF4"); i += 5; continue }

            // ---- Anusvara context rule for bare "ng" (spec §16) ----
            // Mid-word "ng" followed by a vowel letter renders as ං + the
            // 'g' continuing on as an ordinary consonant with that vowel
            // (sinhala, ganga, ranga); at buffer start or before a
            // consonant it falls through to the explicit ඞ consonant
            // instead. Only the 'n' is consumed here — 'g' is deliberately
            // left for the next loop iteration to process normally.
            if (input.startsWith("ng", i) && i > 0) {
                val afterNg = i + 2
                val nextIsVowelLetter = afterNg < input.length && input[afterNg] in "aeiouAEIOU"
                if (nextIsVowelLetter) {
                    out.append(ANUSVARA); i += 1; continue
                }
            }

            // ---- Generalized anusvara: bare n/m before a DIFFERENT ----
            // ---- consonant that isn't part of a known multi-letter   ----
            // ---- compound (nd, ndh, ng, mb, nng, nnd, nyny…) (spec §16) --
            // e.g. "sinhala" -> සිංහල (n before h becomes ං).
            if ((c == 'n' || c == 'm') && i > 0) {
                val compound = tryMatch(input, i, prenasalized) ?: tryMatch(input, i, consonants)
                val isMultiLetterCompound = compound != null && compound.first.length > 1
                if (!isMultiLetterCompound) {
                    val next = i + 1
                    if (next < input.length &&
                        input[next].isLetter() &&
                        input[next] !in "aeiouAEIOU" &&
                        input[next] != c
                    ) {
                        out.append(ANUSVARA); i = next; continue
                    }
                }
            }

            // ---- Consonant base (prenasalized first, then normal) ----
            val baseMatch = tryMatch(input, i, prenasalized) ?: tryMatch(input, i, consonants)

            if (baseMatch != null) {
                val (key, glyph) = baseMatch
                val pos = i + key.length

                // ksha conjunct: k + "sha"/"sh" -> ක්‍ෂ (+ vowel sign)
                if (key == "k" && (input.startsWith("sha", pos) || input.startsWith("sh", pos))) {
                    val afterSh = pos + if (input.startsWith("sha", pos)) 2 else 2
                    out.append("ක").append(HAL).append(ZWJ).append("ෂ")
                    val v = tryMatch(input, afterSh, vowelSigns)
                    when {
                        v != null -> { out.append(v.second); i = afterSh + v.first.length }
                        afterSh < input.length && input[afterSh] == 'a' &&
                            !input.startsWith("aa", afterSh) && !input.startsWith("ae", afterSh) &&
                            !input.startsWith("aee", afterSh) && !input.startsWith("ai", afterSh) &&
                            !input.startsWith("au", afterSh) -> { i = afterSh + 1 }
                        else -> i = afterSh
                    }
                    continue
                }

                // Try a vowel-sign match on the base first — this must win
                // over the implicit rakaransaya heuristic below so that
                // "ru"/"ruu" (dependent vowel signs) aren't mistaken for
                // consonant+r ligatures, e.g. "kru" -> කෘ, not ක්‍රු.
                val vowelEarly = tryMatch(input, pos, vowelSigns)

                // Implicit rakaransaya (no explicit 'x'): C + r + vowel/end
                // -> C + ් + ZWJ + ර [+ vowel]. Per spec §8/§32 examples
                // such as "kra", "kri", "kree", "pra" which omit the 'x'.
                if (vowelEarly == null && pos < input.length && input[pos] == 'r' && glyph != "ර") {
                    val afterR = pos + 1
                    val v = tryMatch(input, afterR, vowelSigns)
                    val bareA = afterR < input.length && input[afterR] == 'a' &&
                        !input.startsWith("aa", afterR) && !input.startsWith("ae", afterR) &&
                        !input.startsWith("aee", afterR) && !input.startsWith("ai", afterR) &&
                        !input.startsWith("au", afterR)
                    val atBoundary = afterR >= input.length || input[afterR].isWhitespace()
                    if (v != null || bareA || atBoundary) {
                        out.append(glyph).append(HAL).append(ZWJ).append("ර")
                        when {
                            v != null -> { out.append(v.second); i = afterR + v.first.length }
                            bareA -> i = afterR + 1
                            else -> i = afterR
                        }
                        continue
                    }
                }

                // Implicit yansaya (no explicit 'x'): C + y + vowel/'a'
                // -> C + ් + ZWJ + ය [+ vowel].
                if (pos < input.length && input[pos] == 'y' && glyph != "ය") {
                    val afterY = pos + 1
                    val v = tryMatch(input, afterY, vowelSigns)
                    val bareA = afterY < input.length && input[afterY] == 'a' &&
                        !input.startsWith("aa", afterY) && !input.startsWith("ae", afterY) &&
                        !input.startsWith("aee", afterY) && !input.startsWith("ai", afterY) &&
                        !input.startsWith("au", afterY)
                    if (v != null || bareA) {
                        out.append(glyph).append(HAL).append(ZWJ).append("ය")
                        if (v != null) { out.append(v.second); i = afterY + v.first.length }
                        else i = afterY + 1
                        continue
                    }
                }

                // Virama / pure consonant, and ZWJ conjuncts: C + x [+ r|y]
                if (pos < input.length && input[pos] == 'x') {
                    val afterX = pos + 1

                    // Rakaransaya: C + x + r -> C + ් + ZWJ + ර
                    if (afterX < input.length && input[afterX] == 'r') {
                        out.append(glyph).append(HAL).append(ZWJ).append("ර")
                        val afterR = afterX + 1
                        val v = tryMatch(input, afterR, vowelSigns)
                        if (v != null) { out.append(v.second); i = afterR + v.first.length }
                        else i = afterR
                        continue
                    }
                    // Yansaya: C + x + y -> C + ් + ZWJ + ය
                    if (afterX < input.length && input[afterX] == 'y') {
                        out.append(glyph).append(HAL).append(ZWJ).append("ය")
                        val afterY = afterX + 1
                        val v = tryMatch(input, afterY, vowelSigns)
                        if (v != null) { out.append(v.second); i = afterY + v.first.length }
                        else i = afterY
                        continue
                    }
                    // Rephaya: r + x + ZWJ (bare rephaya form)
                    if (glyph == "ර" && afterX >= input.length) {
                        out.append("ර").append(HAL).append(ZWJ)
                        i = afterX
                        continue
                    }
                    // Productive conjunct: C1 + x + C2 -> C1 + ් + C2.
                    // Nothing more to consume here — the next loop
                    // iteration naturally re-processes C2 (and any vowel
                    // that follows it) via the normal consonant path.
                    out.append(glyph).append(HAL)
                    i = afterX
                    continue
                }

                // Bare 'a' → inherent vowel, consonant alone
                if (pos < input.length && input[pos] == 'a' &&
                    !input.startsWith("aa", pos) && !input.startsWith("ae", pos) &&
                    !input.startsWith("aee", pos) && !input.startsWith("ai", pos) &&
                    !input.startsWith("au", pos)
                ) {
                    out.append(glyph)
                    i = pos + 1
                    continue
                }

                // Vowel sign follows
                if (vowelEarly != null) {
                    out.append(glyph).append(vowelEarly.second)
                    i = pos + vowelEarly.first.length
                    continue
                }

                // Implicit cluster: consonant directly followed by another
                // consonant with no vowel between them → hal kirima, and let
                // the next loop iteration render the following consonant.
                out.append(glyph).append(HAL)
                i = pos
                continue
            }

            // ---- Independent vowel (word-initial or standalone) ----
            val vowel = tryMatch(input, i, independentVowels)
            if (vowel != null) {
                out.append(vowel.second); i += vowel.first.length; continue
            }

            // ---- Fallback: unknown input, preserve original character ----
            out.append(c)
            i++
        }

        return out.toString()
    }

    private fun tryMatch(
        input: String,
        pos: Int,
        rules: List<Pair<String, String>>
    ): Pair<String, String>? {
        if (pos >= input.length) return null
        for (rule in rules) {
            val key = rule.first
            if (pos + key.length <= input.length &&
                input.regionMatches(pos, key, 0, key.length)
            ) return rule
        }
        return null
    }
}
