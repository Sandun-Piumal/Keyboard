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

/**
 * Small Sinhala-glyph hint shown in each QWERTY key's top-right corner
 * (mirrors the top row's number hints) so the user can see at a glance
 * which Sinhala letter a key produces in "si"/"mix" mode. Mirrors the
 * single-letter base mapping in [SinhalaTransliterator] — kept as its own
 * small table here (rather than reusing that object's private lists)
 * since this is purely a display hint, not part of transliteration.
 */
val sinhalaKeyHints: Map<Char, String> = mapOf(
    'q' to "ද", 'w' to "ව", 'e' to "එ", 'r' to "ර", 't' to "ට",
    'y' to "ය", 'u' to "උ", 'i' to "ඉ", 'o' to "ඔ", 'p' to "ප",
    'a' to "අ", 's' to "ස", 'd' to "ඩ", 'f' to "ෆ", 'g' to "ග",
    'h' to "හ", 'j' to "ජ", 'k' to "ක", 'l' to "ල",
    'z' to "ෂ", 'x' to "ං", 'c' to "ච", 'v' to "ව", 'b' to "බ",
    'n' to "න", 'm' to "ම"
)

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
 * Latin → Sinhala transliterator.
 *
 * Layer 1 — the supplied phonetic map (Map.zip → [SinhalaPhoneticMap]),
 * case-sensitive, longest match first, used as given except for the
 * adjustments listed on [phoneticMap] (d-family, ru/ruu).
 *
 * Layer 2 — the standard Singlish completion rules, used ONLY where the
 * map has no entry (the same structure the common Singlish→Sinhala
 * converters use: consonant+vowel, bare consonant → hal kirima, and
 * consonant+r → rakaransaya):
 *   • bare consonant (no vowel after it)   m  → ම්      amma  → අම්ම
 *   • consonant + vowel sign (stem + any vowel suffix of the map)
 *   • consonant + r + vowel → rakaransaya  tra → ට්‍ර   kraa  → ක්‍රා
 *   • consonant + y + vowel → yansaya      thya → ත්‍ය  sathya → සත්‍ය
 *   • aliases  ae → ඇ / ැ ,  aee → ඈ / ෑ ,  x → ං ,  H → ඃ ,  zn → ං
 *   • n / m before a different consonant follows its place of articulation:
 *       m + p/b → ම්  (sampath → සම්පත්),   n + t/d → න්  (antha → අන්ත),
 *       otherwise → ං  (sinhala → සිංහල, lanka → ලංක).  x forces ං, _ forces hal.
 * A candidate from layer 2 is only used when it consumes MORE input than
 * the map's own longest match, so the map always wins ties.
 *
 * Anything else (space, digits, punctuation) passes through unchanged.
 */
object SinhalaTransliterator {

    private const val HAL = "\u0DCA"
    private const val RAKARANSAYA = "\u0DCA\u200D\u0DBB"   // ්‍ර
    private const val YANSAYA = "\u0DCA\u200D\u0DBA"       // ්‍ය
    private const val ANUSVARA = "\u0D82"                    // ං

    /** Entries added on top of the supplied map (none of these keys exist in it). */
    private val extras = mapOf(
        "ae" to "\u0D87",   // ඇ
        "aee" to "\u0D88",  // ඈ
        "x" to "\u0D82",    // ං
        "zn" to "\u0D82",   // ං
        "H" to "\u0D83"     // ඃ
    )

    /**
     * The supplied map with the practical adjustments agreed for typing:
     *  - d-family: d -> ද, dh -> ධ, D -> ඩ, Dh -> ඪ  (ද is the commonest "d";
     *    the file's d -> ඩ made "gedara" come out ගෙඩර)
     *  - ru / ruu -> රු / රූ  (the file lists both ඍ and රු; ඍ/ඎ are Ru / Ruu)
 *  - ඳ ඹ ඟ: nda/mba/nga -> nnda/mmba/nnga (so plain nd/mb/ng are two letters)
     * Everything else is the file, untouched.
     */
    private val phoneticMap: Map<String, String> by lazy {
        val m = HashMap<String, String>(SinhalaPhoneticMap.entries)
        for ((k, v) in SinhalaPhoneticMap.entries) {
            val first = v.firstOrNull() ?: continue
            val swapped: Char = when {
                k.startsWith("dh") && first == '\u0DAF' -> '\u0DB0'   // ද -> ධ
                k.startsWith("Dh") && first == '\u0DB0' -> '\u0DAA'   // ධ -> ඪ
                k.startsWith("d") && !k.startsWith("dh") && first == '\u0DA9' -> '\u0DAF'   // ඩ -> ද
                k.startsWith("D") && !k.startsWith("Dh") && first == '\u0DAA' -> '\u0DA9'   // ඪ -> ඩ
                else -> continue
            }
            m[k] = swapped + v.substring(1)
        }
        // prenasalised letters ඳ ඹ ඟ are typed with the nasal doubled (nnda, mmba, nnga);
        // plain nd / mb / ng are then just two consonants (sandesaya, sambandha, sangeetha)
        for ((k, v) in SinhalaPhoneticMap.entries) {
            val first = v.firstOrNull() ?: continue
            val doubled: String = when {
                k.startsWith("nd") && first == '\u0DB3' -> "n$k"
                k.startsWith("mb") && first == '\u0DB9' -> "m$k"
                k.startsWith("ng") && first == '\u0D9F' -> "n$k"
                else -> continue
            }
            m.remove(k)
            m[doubled] = v
        }
        m["ru"] = "\u0DBB\u0DD4"; m["ruu"] = "\u0DBB\u0DD6"
        m["Ru"] = "\u0D8D"; m["Ruu"] = "\u0D8E"
        m
    }

    private val literal: Map<String, String> by lazy {
        val m = HashMap<String, String>(1024)
        m.putAll(extras)
        m.putAll(phoneticMap)                    // the (adjusted) file always wins
        m
    }
    private val literalMax: Int by lazy { literal.keys.maxOf { it.length } }

    /** Consonant stems derived from the map itself: "k" for ka→ක, "ny" for nya→ඤ … */
    private val stems: Map<String, String> by lazy {
        val m = HashMap<String, String>()
        for ((k, v) in phoneticMap) {
            if (k.length >= 2 && k.endsWith("a") && v.length == 1 && v[0] in '\u0D9A'..'\u0DC6') {
                m[k.dropLast(1)] = v
            }
        }
        m["X"] = "\u0D9E"   // ඞ (the map only has it inside "kaXa")
        m
    }
    private val stemMax: Int by lazy { stems.keys.maxOf { it.length } }

    /** Vowel signs exactly as the map uses them ("a" = inherent vowel, no sign) + ae/aee aliases. */
    private val vowelSuffixes: List<Pair<String, String>> = listOf(
        "a" to "", "aa" to "\u0DCF", "A" to "\u0DD0", "Aa" to "\u0DD1", "AA" to "\u0DD1",
        "ae" to "\u0DD0", "aee" to "\u0DD1",
        "i" to "\u0DD2", "ii" to "\u0DD3", "u" to "\u0DD4", "uu" to "\u0DD6",
        "ru" to "\u0DD8", "ruu" to "\u0DF2",
        "e" to "\u0DD9", "ee" to "\u0DDA", "ei" to "\u0DDA", "E" to "\u0DDB",
        "o" to "\u0DDC", "oo" to "\u0DDD", "oe" to "\u0DDD", "au" to "\u0DDE"
    ).sortedByDescending { it.first.length }

    /** The map is case-sensitive, so input is never case-folded. */
    fun normalizeCase(rawInput: String): String = rawInput

    private fun literalLen(s: String, i: Int): Int {
        var len = minOf(literalMax, s.length - i)
        while (len >= 1) {
            if (literal.containsKey(s.substring(i, i + len))) return len
            len--
        }
        return 0
    }

    private fun stemAt(s: String, i: Int): String? {
        var len = minOf(stemMax, s.length - i)
        while (len >= 1) {
            val k = s.substring(i, i + len)
            if (stems.containsKey(k)) return k
            len--
        }
        return null
    }

    private fun suffixAt(s: String, j: Int): Pair<Int, String>? {
        for ((key, sign) in vowelSuffixes) {
            if (s.startsWith(key, j)) return key.length to sign
        }
        return null
    }

    private class Syllable(val len: Int, val text: String)

    private fun syllableAt(s: String, i: Int): Syllable? {
        val stem = stemAt(s, i) ?: return null
        val glyph = stems.getValue(stem)
        val j = i + stem.length
        // consonant + r / y + vowel → rakaransaya / yansaya, generalising the map's
        // own kra / kya (never after r or y themselves: "arra", "ayya")
        if (stem != "r" && stem != "y" && j < s.length && (s[j] == 'r' || s[j] == 'y')) {
            val suf = suffixAt(s, j + 1)
            if (suf != null) {
                val conjunct = if (s[j] == 'r') RAKARANSAYA else YANSAYA
                return Syllable(j + 1 + suf.first - i, glyph + conjunct + suf.second)
            }
        }
        val suf = suffixAt(s, j)
        if (suf != null) return Syllable(j + suf.first - i, glyph + suf.second)
        // no vowel follows:
        //  - m_ / n_ ... force hal kirima (the underscore is consumed): m_pa -> ම්ප
        if (j < s.length && s[j] == '_') return Syllable(stem.length + 1, glyph + HAL)
        //  - n / m directly before a DIFFERENT consonant: the nasal follows the place of
        //    the next consonant, as in Sinhala spelling —
        //      m before p / b family   -> ම් (hal)    sampath -> සම්පත්, sambandha -> සම්බන්ධ
        //      n before t / d family   -> න් (hal)    antha -> අන්ත, sandhya -> සන්ධ්‍ය
        //      anything else           -> ං           sinhala -> සිංහල, lanka -> ලංක
        //    (a doubled letter stays hal kirima: amma -> අම්ම)
        if ((stem == "n" || stem == "m") && j < s.length && s[j] != s[i]) {
            val next = stemAt(s, j)
            if (next != null) {
                val c = next[0]
                val keepHal = (stem == "m" && (c == 'p' || c == 'P' || c == 'b' || c == 'B')) ||
                              (stem == "n" && (c == 't' || c == 'T' || c == 'd' || c == 'D'))
                if (!keepHal) return Syllable(stem.length, ANUSVARA)
            }
        }
        return Syllable(stem.length, glyph + HAL)        // bare consonant -> hal kirima
    }

    fun transliterate(rawInput: String): String {
        var s = rawInput
        val out = StringBuilder(s.length * 2)
        var i = 0
        while (i < s.length) {
            val litLen = literalLen(s, i)
            val syl = syllableAt(s, i)
            when {
                litLen > 0 && (syl == null || litLen >= syl.len) -> {
                    out.append(literal.getValue(s.substring(i, i + litLen)))
                    i += litLen
                }
                syl != null -> {
                    out.append(syl.text)
                    i += syl.len
                }
                s[i].isUpperCase() && s[i].lowercaseChar() != s[i] -> {
                    // unmapped capital (e.g. an accidental Shift): retry as lowercase
                    s = s.substring(0, i) + s[i].lowercaseChar() + s.substring(i + 1)
                }
                else -> { out.append(s[i]); i++ }
            }
        }
        return out.toString()
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
        "d" to listOf("ද", "ඩ", "ද්", "ඩ්", "ඩැ", "දෑ", "ද්‍", "ඩෙ", "ඩේ", "ධ", "ඩ්‍", "දැ", "දේ", "ඩු", "ඩි", "ඩො", "දු", "ඩී", "දෙ", "ඩෝ"),
        "d|a" to listOf("ඩැ", "ඩා", "දා", "ඩෑ"),
        "d|e" to listOf("ඩෙ", "ඩේ", "දේ", "ඩී", "දෙ", "දී"),
        "d|h" to listOf("ධ", "ධා", "ධි", "ධී", "ධු", "ධෝ", "ධේ", "ධූ", "ඪ", "ධෙ"),
        "d|i" to listOf("ඩි", "ඩී", "දි", "දී"),
        "d|o" to listOf("ඩො", "ඩෝ", "දො", "දෝ", "ඩූ", "දූ"),
        "d|u" to listOf("ඩු", "දු", "ඩූ", "දූ"),
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
        "q" to listOf("ද", "ද්", "ක", "ක්", "හ", "ධ"),
        "q|e" to listOf("දෙ", "දේ"),
        "q|i" to listOf("දි", "දී"),
        "q|u" to listOf("දු", "දූ"),
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
        "x" to listOf("ං"),
        "y" to listOf("ය", "ය්", "යි", "යෑ", "යී", "යේ", "යෙ", "යු", "යො", "ර"),
        "y|a" to listOf("යා", "යැ"),
        "y|e" to listOf("යේ", "යෙ"),
        "y|i" to listOf("යි", "යී"),
        "y|o" to listOf("යෝ", "යො", "යූ"),
        "y|u" to listOf("යු", "යූ"),
        "z" to listOf("ෂ", "ෂ්", "ෂා", "ෂි", "ෂු"),
        "z|a" to listOf("ෂා", "ෂැ"),
        "z|e" to listOf("ෂෙ", "ෂේ", "ෂී"),
        "z|i" to listOf("ෂි", "ෂී"),
        "z|o" to listOf("ෂො", "ෂෝ", "ෂූ"),
        "z|u" to listOf("ෂු", "ෂූ"),
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
