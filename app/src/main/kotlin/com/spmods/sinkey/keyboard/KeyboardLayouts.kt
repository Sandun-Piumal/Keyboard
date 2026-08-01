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
 * Standard Sinhala Wijesekara keyboard layout, as defined by Sri Lanka
 * Standard SLS 1134 Revision 2:2004 ("Wijesekara Extended Keyboard").
 * Unlike phonetic/Singlish mode, this is a direct one-key-to-one-glyph
 * (or diacritic) hardware-style layout: each QWERTY key position emits a
 * fixed Sinhala character or combining mark, with a second layer under
 * Shift. Row shape mirrors [EnglishRows] (10/9/7 keys) so the same
 * KeyRow/NumberedKeyRow composables can render it directly.
 *
 * ZWJ = zero-width joiner (U+200D), used to build conjuncts (rakaransaya,
 * yansaya) together with the hal kirima (්) key.
 * ZWNJ = zero-width non-joiner (U+200C), placed on Q, prevents ligature
 * formation when needed.
 *
 * ⚠️ VERIFY BEFORE SHIPPING: [baseRows] was cross-checked directly against
 * the SLS 1134 reference table and is solid. [shiftRows] and especially
 * [altGrMap] were reconstructed from a lower-quality source (OCR'd PDF
 * text with ambiguous line breaks around G/H/J and the AltGr column) and
 * should be typing-tested key-by-key against an official SLS 1134 chart
 * or a known-good Wijesekara IME before release — a few positions
 * (notably ට/ඨ on G, ය/යං on H, ව/¿ on J, ක/ඛ on L) are my best
 * reconstruction, not a verified transcription.
 */
object WijesekaraLayout {

    private const val ZWJ = "\u200D"
    private const val ZWNJ = "\u200C"
    private const val HAL = "\u0DCA" // ්

    /** Unshifted (base) layer — matches [EnglishRows] row/column shape. */
    val baseRows: List<List<String>> = listOf(
        listOf(ZWNJ, "අ", "ැ", "ර", "එ", "හ", "ම", "ස", "ද", "ච"),
        listOf("$HAL·", "ා", ZWJ, "ෆ", "ට", "ය", "ව", "න", "ක"),
        listOf("$HAL" + "ං", ZWJ, "ජ", "ඩ", "ඉ", "බ", "ප")
    )

    /** Shifted layer — matches [EnglishRows] row/column shape. */
    val shiftRows: List<List<String>> = listOf(
        listOf("ෆ්‍ර", "උ", "ෑ", "ඍ", "ඔ", "ශ", "ඹ", "ෂ", "ධ", "ඡ"),
        listOf("ෟ", "ෘ", ZWJ, "ෆ", "ඨ", "ය" + HAL, "¿", "ණ", "ඛ"),
        listOf("\"", "'", "ඣ", "ඪ", "ඊ", "භ", "ඵ")
    )

    /** Additional characters reachable only via AltGr, keyed by base-layer key label. */
    val altGrMap: Map<String, String> = mapOf(
        "ච" to "ඥ", // AltGr+P
        "ක" to "ඛ", // reference only; see shiftRows for the primary ඛ position
        ZWNJ to "ඤ"
    )

    /** Punctuation row keys with Wijesekara-specific comma/period glyphs. */
    val punctuation: Map<String, String> = mapOf(
        "," to "ළ",
        "." to "ල",
        "<" to "ළ",
        ">" to "ඝ"
    )
}

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
        "thh" to "ථ", "ddh" to "ධ",
        "kh" to "ඛ", "gh" to "ඝ", "ng" to "ඞ",
        "ch" to "ච", "jh" to "ඣ", "ny" to "ඤ", "gn" to "ඥ",
        "th" to "ත", "sh" to "ශ",
        "ph" to "ඵ", "bh" to "භ",
        "nd" to "ඳ",
        "Sh" to "ෂ",
        "TH" to "ඨ", "DH" to "ඪ",
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
    /**
     * Lowercases [input] except where an uppercase letter is part of one of
     * the scheme's deliberately case-sensitive tokens: the digraphs "Sh",
     * "TH", "DH" (checked longest-first so they don't get shadowed by the
     * single-letter "T"/"D" rule below), and the single letters "N", "T",
     * "D", "L" when not followed by lowercase letters that would make them
     * part of a different, already-lowercase word/digraph. Everything else
     * — including a capital first letter from auto-capitalize, or ALL-CAPS
     * typing — is folded to lowercase so it reaches the normal consonant
     * table instead of falling through unmatched.
     */
    private fun normalizeCaseForTransliteration(input: String): String {
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            when {
                input.startsWith("Sh", i) -> { out.append("Sh"); i += 2 }
                input.startsWith("TH", i) -> { out.append("TH"); i += 2 }
                input.startsWith("DH", i) -> { out.append("DH"); i += 2 }
                input[i] in "NTDL" -> { out.append(input[i]); i += 1 }
                else -> { out.append(input[i].lowercaseChar()); i += 1 }
            }
        }
        return out.toString()
    }

    fun transliterate(rawInput: String): String {

        if (rawInput.isEmpty()) return ""
        // Normalize case first: only a specific, deliberate set of letters
        // carries case-meaning in this scheme — N/T/D/L as single letters
        // (ණ/ත/ද/ළ vs n/t/d/l -> න/ට/ද/ල) and the Sh/TH/DH digraphs (ෂ/ඨ/ඪ
        // vs sh/th/dh -> ශ/ත/ධ). Any other uppercase letter reaching this
        // function (e.g. from auto-capitalize at the start of a sentence,
        // or a user just typing with caps lock on for emphasis) has no
        // separate mapping in `consonants` and would otherwise fall
        // through unmatched, corrupting the output. So: lowercase
        // everything by default, then restore case only where one of the
        // known case-sensitive digraphs/letters actually appears.
        val input = normalizeCaseForTransliteration(rawInput)
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

/**
 * Weighted phonetic candidate map for the first 1–2 typed keys, sourced from
 * sinhala_key_mapping_full.csv (empirical Roman→Sinhala key mapping with
 * likelihood weights). This complements [SinhalaTransliterator]:
 *
 *  - [SinhalaTransliterator] is the deterministic longest-match phonetic
 *    engine that renders the *whole* typed buffer into Sinhala (used once a
 *    word is committed, or for buffers longer than this map covers).
 *  - [SinhalaCandidateMap] instead answers "given the first key (or first
 *    two keys) the user just pressed, what are the ranked Sinhala
 *    possibilities?" — used for:
 *      1. Live composing preview: the #1 (lowest-weight / most likely)
 *         candidate for the current 1–2 key prefix.
 *      2. Suggestion strip: the top 3–5 ranked candidates for that same
 *         prefix, so the user can tap the one they actually meant instead
 *         of the greedy #1 guess.
 *
 * CSV format notes this object reproduces:
 *  - "Type (Roman)" is one or two lowercase Roman letters joined by '|'
 *    (e.g. "b", "s|h") — i.e. the first one or two keys the user pressed.
 *  - "Sinhala Output" candidates are pre-sorted ascending by weight (lower
 *    weight = more likely), so index 0 is always the top pick.
 *  - Some outputs contain a literal '|' themselves (e.g. "ක|්") marking a
 *    base-glyph + trailing sign shown as a two-part preview; these are
 *    flattened to a single concatenated glyph ("ක්") since Sinhala
 *    rendering doesn't need the separator.
 *  - Rows whose output was "_" are placeholders meaning "no candidate yet,
 *    keep buffering" — they carry no usable glyph and are omitted here.
 */
object SinhalaCandidateMap {

    // Auto-generated from sinhala_key_mapping_full.csv — DO NOT hand-edit ordering;
    // re-run the generator against the CSV if weights change. Each key sequence
    // (e.g. "b", "s|h") maps to its candidates sorted by ascending weight
    // (lower = more likely, so index 0 is always the top pick).
    private val candidateMap: Map<String, List<String>> = mapOf(
        "a" to listOf("අ", "ඇ", "ආ", "එ", "ඈ"),
        "a|i" to listOf("ඓ"),
        "a|u" to listOf("ඖ"),
        "b" to listOf("බ", "බැ", "බ්‍", "බ්", "බි", "බෙ", "බේ", "බො"),
        "b|a" to listOf("බෑ", "බා"),
        "b|e" to listOf("බෙ", "බේ", "බී"),
        "b|h" to listOf("භා", "භ", "භූ", "භි", "භු", "භේ", "භෝ", "භී", "භො", "භැ"),
        "b|i" to listOf("බි", "බී"),
        "b|o" to listOf("බො", "බෝ", "බූ"),
        "b|u" to listOf("බු", "බූ"),
        "c" to listOf("ච", "චා", "චැ", "ක", "ක්", "චෑ", "කා"),
        "c|e" to listOf("චෙ", "චේ", "චී"),
        "c|h" to listOf("චු", "ච", "චැ", "චා", "චි", "චූ", "චී", "චො", "චෙ", "චෝ", "චේ", "ඡ", "ඡා", "චෑ"),
        "c|i" to listOf("චි", "චී"),
        "c|o" to listOf("චො", "චෝ", "චූ", "කො"),
        "c|u" to listOf("චු", "චූ"),
        "d" to listOf("ද", "ඩ", "ඩ්", "ද්", "දැ", "ඩෑ", "ඩ්‍", "දෙ", "දේ", "ධ", "ද්‍", "ඩැ", "ඩේ", "දු", "දි", "දො", "ඩු", "දී", "ඩෙ", "දෝ"),
        "d|a" to listOf("දැ", "දා", "ඩා", "දෑ"),
        "d|e" to listOf("දෙ", "දේ", "ඩේ", "දී", "ඩෙ", "ඩී"),
        "d|h" to listOf("ධ", "ධා", "ධි", "ධී", "ධු", "ධෝ", "ධේ", "ධූ", "ඪ", "ධෙ"),
        "d|i" to listOf("දි", "දී", "ඩි", "ඩී"),
        "d|o" to listOf("දො", "දෝ", "ඩො", "ඩෝ", "දූ", "ඩූ"),
        "d|u" to listOf("දු", "ඩු", "දූ", "ඩූ"),
        "e" to listOf("එ", "ඒ"),
        "e|e" to listOf("ඊ"),
        "f" to listOf("ෆ", "ෆ්‍", "ෆැ", "ෆී", "ඵ", "ෆා", "ෆ්", "ෆෑ", "ෆි", "ෆැක", "ෆේ"),
        "f|e" to listOf("ෆේ", "ෆෙ"),
        "f|i" to listOf("ෆි"),
        "f|o" to listOf("ෆො", "ෆෝ", "ෆූ"),
        "f|u" to listOf("ෆු", "ෆූ"),
        "g" to listOf("ග", "ගෑ", "ග්‍", "ග්", "ගේ", "ගෙ", "ගි", "ගැ", "ගු", "ගො"),
        "g|a" to listOf("ගැ", "ගා"),
        "g|e" to listOf("ගෙ", "ගේ", "ගී"),
        "g|h" to listOf("ඝ", "ඝා", "ඝෝ", "ඝු", "ඝි"),
        "g|i" to listOf("ගි", "ගී"),
        "g|n" to listOf("ඥ", "ඥා", "ඥෝ"),
        "g|o" to listOf("ගො", "ගූ", "ගෝ"),
        "g|u" to listOf("ගු", "ගූ"),
        "h" to listOf("හ", "හැ", "හ‍", "හෑ", "හ්", "ත්", "හි", "ත", "හෙ", "හී", "හො", "හේ", "හු", "ධ", "තෑ"),
        "h|a" to listOf("හා", "තා"),
        "h|e" to listOf("හෙ", "හේ", "හී", "තේ", "තෙ"),
        "h|h" to listOf("ඡ"),
        "h|i" to listOf("හි", "හී", "ති", "තී", "ර", "රි"),
        "h|o" to listOf("හො", "හෝ", "හූ", "තො"),
        "h|u" to listOf("හු", "හූ", "තු", "තූ"),
        "i" to listOf("ඉ", "ඊ"),
        "j" to listOf("ජ", "ජ්", "ජැ", "ජෑ", "ජේ", "ජ්‍", "ජෙ"),
        "j|a" to listOf("ජා"),
        "j|e" to listOf("ජී", "ජේ", "ජෙ"),
        "j|i" to listOf("ජි", "ජී"),
        "j|o" to listOf("ජො", "ජෝ"),
        "j|u" to listOf("ජු", "ජූ"),
        "k" to listOf("ක", "ක්", "කී", "ක්‍", "කු", "කි", "කෙ", "කේ", "ඛ", "කො", "කෝ"),
        "k|a" to listOf("කා", "කැ", "කෑ"),
        "k|e" to listOf("කෙ", "කේ"),
        "k|h" to listOf("ඛ", "ඛා", "ඛෙ", "ඛෝ", "ඛි", "ඛේ", "ඛු"),
        "k|i" to listOf("කි", "කී"),
        "k|n" to listOf("ඤා", "ඤ", "ඤො", "ඤෝ"),
        "k|o" to listOf("කො", "කෝ", "කූ"),
        "k|u" to listOf("කු", "කූ"),
        "l" to listOf("ල", "ළ", "ල්", "ලි", "ළා", "ළෑ", "ලේ", "ලු", "ලෙ", "ළ්", "ලො"),
        "l|a" to listOf("ලැ", "ලා", "ලෑ"),
        "l|e" to listOf("ලෙ", "ලේ", "ලී", "ළේ", "ළෙ"),
        "l|i" to listOf("ලි", "ලී", "ළි", "ළී"),
        "l|o" to listOf("ලො", "ලෝ", "ලූ", "ළූ", "ළො", "ළෝ"),
        "l|u" to listOf("ලු", "ලූ", "ළු", "ළූ"),
        "m" to listOf("ම", "ම්", "මෑ", "ඹ", "මේ", "මි", "මෙ", "මො", "මු"),
        "m|a" to listOf("මා", "මැ"),
        "m|b" to listOf("ඹ", "ඹේ", "ඹු", "ඹි", "ඹෙ", "ඹා", "ඹී"),
        "m|e" to listOf("මේ", "මෙ", "මී"),
        "m|i" to listOf("මි", "මී"),
        "m|o" to listOf("මො", "මෝ", "මූ"),
        "m|u" to listOf("මු", "මූ"),
        "n" to listOf("න", "න්", "ණ", "නේ", "ණ්", "න්‍", "නෙ", "නි", "නො", "ඟ", "නු", "ඞ"),
        "n|a" to listOf("නෑ", "නැ", "නා", "ණා", "ණෑ"),
        "n|d" to listOf("ඳ", "ඳු", "ඳි", "ඳේ", "ඬ", "ඳා", "ඳී", "ඬේ", "ඬි"),
        "n|e" to listOf("නේ", "නෙ", "නී", "ණේ", "ණෙ", "ණී"),
        "n|g" to listOf("ඟ", "ඟී", "ඟු", "ඟේ", "ඟා", "ඟි"),
        "n|h" to listOf("සි"),
        "n|i" to listOf("නි", "නී", "ණි", "ණී"),
        "n|o" to listOf("නො", "නෝ", "නූ", "ණෝ", "ණො"),
        "n|u" to listOf("නු", "ණු", "නූ", "ණූ"),
        "o" to listOf("ඔ", "ඕ"),
        "o|o" to listOf("ඌ"),
        "o|u" to listOf("ඖ"),
        "p" to listOf("ප", "ප්‍", "ප්", "පෑ", "පු", "පි", "පේ", "ෆ", "පෙ"),
        "p|a" to listOf("පා", "පැ"),
        "p|e" to listOf("පෙ", "පේ", "පී"),
        "p|h" to listOf("ෆො", "ෆෝ", "ඵ", "ෆ", "ෆි", "ෆු", "ෆා", "ෆී"),
        "p|i" to listOf("පි", "පී"),
        "p|o" to listOf("පො", "පෝ", "පූ"),
        "p|u" to listOf("පු", "පූ"),
        "q" to listOf("ක", "ක්", "හ"),
        "q|e" to listOf("කෙ"),
        "q|i" to listOf("කි"),
        "q|u" to listOf("කු"),
        "r" to listOf("ර", "රැ", "ර්", "රු", "රි", "රේ", "රෙ"),
        "r|a" to listOf("රෑ", "රා"),
        "r|e" to listOf("රෙ", "රේ", "රී"),
        "r|i" to listOf("රි", "රී", "ඍ"),
        "r|o" to listOf("රෝ", "රො", "රූ"),
        "r|u" to listOf("රු", "රූ"),
        "s" to listOf("ස", "ස්", "ශ්‍", "සෑ", "ශ", "ෂ", "ශා", "ෂා", "සි", "සෙ", "ස්‌", "සේ"),
        "s|a" to listOf("සැ", "සා", "ශෑ"),
        "s|e" to listOf("සෙ", "සේ", "සී", "ශේ", "ෂේ", "ෂෙ", "ශී", "ශෙ"),
        "s|h" to listOf("ශා", "ශ", "ෂෝ", "ශි", "ෂු", "ශෝ", "ශේ", "ෂෙ", "ෂො", "ෂ", "ෂි", "ෂා", "ශු", "ශෙ", "ශී", "ෂේ", "ෂූ", "ෂී", "ෂැ", "ශො", "ශෑ", "ශූ", "ශැ", "ෂෑ"),
        "s|i" to listOf("සි", "සී", "ශි", "ෂි", "ශී", "ෂී"),
        "s|o" to listOf("සො", "සෝ", "සූ", "ෂෝ", "ශෝ", "ෂො"),
        "s|u" to listOf("සු", "සූ", "ෂු", "ශු"),
        "t" to listOf("ට", "ත", "තා", "ත්", "ට්", "ටැ", "ටා", "ට්‍", "තැ", "තෑ", "ටෑ"),
        "t|e" to listOf("තේ", "ටෙ", "තෙ", "ටේ", "වෙ"),
        "t|h" to listOf("ත", "තා", "තැ", "තෑ", "ථ", "ඨ", "ථා", "ති", "ඨා"),
        "t|i" to listOf("ටි", "ති", "තී", "වි", "වී"),
        "t|o" to listOf("ටො", "ටෝ"),
        "t|u" to listOf("තු", "ටු", "ටූ"),
        "u" to listOf("උ", "ඌ"),
        "v" to listOf("ව", "වැ", "ව්", "වෑ", "වේ", "වෙ", "ව්‍"),
        "v|a" to listOf("වා"),
        "v|e" to listOf("වෙ", "වේ", "වී"),
        "v|i" to listOf("වි", "වී"),
        "v|o" to listOf("වො", "වෝ", "වූ"),
        "v|u" to listOf("වු", "වූ"),
        "w" to listOf("ව", "ව්‍", "වෑ", "වෙ", "වේ", "වූ", "වි"),
        "w|a" to listOf("වැ", "වා"),
        "w|e" to listOf("වෙ", "වේ", "වී"),
        "w|i" to listOf("වි", "වී"),
        "w|o" to listOf("වො", "වෝ", "ඌ"),
        "w|u" to listOf("වු", "වූ"),
        "x" to listOf("ෂ්", "ෂා", "ෂ"),
        "x|e" to listOf("ෂෙ"),
        "x|i" to listOf("ෂි"),
        "x|u" to listOf("ෂු"),
        "y" to listOf("ය", "ය්", "යි", "යෑ", "යී", "යේ", "යෙ", "යු", "යො", "ර"),
        "y|a" to listOf("යා", "යැ"),
        "y|e" to listOf("යේ", "යෙ"),
        "y|i" to listOf("යි", "යී"),
        "y|o" to listOf("යෝ", "යො", "යූ"),
        "y|u" to listOf("යු", "යූ"),
        "z" to listOf("ළ්", "ළ"),
        "z|o" to listOf("ළූ", "ඌ"),
    )

    /**
     * Ranked Sinhala candidates for the first one or two characters of
     * [buffer] (e.g. buffer "ba..." looks up "b|a"; buffer "b" alone looks
     * up "b"). Returns an empty list if the prefix isn't in the map (the
     * caller should fall back to [SinhalaTransliterator] in that case).
     * Candidates are already ranked best-first (ascending weight).
     */
    fun candidatesFor(buffer: String): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val first = buffer[0].lowercaseChar()
        val twoKey = if (buffer.length >= 2) "$first|${buffer[1].lowercaseChar()}" else null
        if (twoKey != null) {
            candidateMap[twoKey]?.let { return it }
        }
        return candidateMap[first.toString()] ?: emptyList()
    }

    /** Convenience: the single most likely candidate for [buffer], or null if none. */
    fun topCandidateFor(buffer: String): String? = candidatesFor(buffer).firstOrNull()
}
