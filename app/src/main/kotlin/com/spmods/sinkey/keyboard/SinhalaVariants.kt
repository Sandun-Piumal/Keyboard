package com.spmods.sinkey.keyboard

/**
 * Generates every plausible Sinhala reading of a typed Singlish buffer by
 * trying all the alternatives for each ambiguous letter, instead of only
 * swapping one letter at a time.
 *
 *   t  -> ට / ත            d -> ද / ඩ / ධ / ඪ
 *   n  -> න / ණ            l -> ල / ළ
 *   s  -> ස / ශ / ෂ        th -> ත / ඨ / ථ
 *   nd -> ඳ / ඬ / න්ද      ng -> (ං)ග / ඟ      mb -> ඹ / ම්බ
 *
 * The result is a list of [Variant]s (default reading first, then readings
 * with fewer changes before readings with more). Ranking them against the
 * dictionary is done in WordRepository.rankSinhalaVariants — this object is
 * pure string logic with no database access.
 */
object SinhalaVariants {

    /** One candidate reading. [deviations] = how many letters differ from the default reading. */
    data class Variant(
        val raw: String,
        val sinhala: String,
        val deviations: Int
    )

    private const val HAL = '\u0DCA'

    private class Token(val options: List<String>)

    // Fixed multi-letter tokens that must not be split or varied.
    // Longest first so e.g. "nyny" wins over "ny".
    private val fixedTokens = listOf(
        "kunda", "nyny", "chh", "thh", "ddh", "nng", "nnd", "ng_",
        "kh", "gh", "ch", "jh", "ny", "gn", "ph", "bh", "h_", "h."
    )

    // Ambiguous multi-letter tokens (default option first).
    private val multiTokens = listOf(
        "th" to listOf("th", "TH", "thh"),
        "sh" to listOf("sh", "Sh"),
        "dh" to listOf("dh", "DH"),
        "nd" to listOf("nd", "nnd", "nxd"),
        "ng" to listOf("ng", "nng"),
        "mb" to listOf("mb", "mxb")
    )

    // Ambiguous single letters (default option first).
    private val singleTokens = mapOf(
        't' to listOf("t", "T"),
        'd' to listOf("d", "D"),
        'n' to listOf("n", "N"),
        'l' to listOf("l", "L"),
        's' to listOf("s", "sh", "Sh")
    )

    private fun tokenize(s: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]

            // Explicit case choices made by the user (Shift) are kept as typed.
            if (s.startsWith("Sh", i) || s.startsWith("TH", i) || s.startsWith("DH", i)) {
                out.add(Token(listOf(s.substring(i, i + 2)))); i += 2; continue
            }
            if (c == 'N' || c == 'T' || c == 'D' || c == 'L') {
                out.add(Token(listOf(c.toString()))); i++; continue
            }

            // Lith-digit marker "<digit>s" — the 's' is not a letter here.
            if (c == 's' && i > 0 && s[i - 1].isDigit()) {
                out.add(Token(listOf("s"))); i++; continue
            }

            val fixed = fixedTokens.firstOrNull { s.startsWith(it, i) }
            if (fixed != null) {
                out.add(Token(listOf(fixed))); i += fixed.length; continue
            }

            val multi = multiTokens.firstOrNull { s.startsWith(it.first, i) }
            if (multi != null) {
                out.add(Token(multi.second)); i += multi.first.length; continue
            }

            val single = singleTokens[c]
            if (single != null) {
                out.add(Token(single)); i++; continue
            }

            out.add(Token(listOf(c.toString()))); i++
        }
        return out
    }

    /**
     * All readings of [rawInput], default reading first. At most [maxVariants]
     * are returned; when the full cross-product would be larger, only readings
     * with a small number of changed letters are produced.
     */
    fun generate(rawInput: String, maxVariants: Int = 400): List<Variant> {
        if (rawInput.isEmpty()) return emptyList()
        val tokens = tokenize(SinhalaTransliterator.normalizeCase(rawInput))

        val ambiguous = tokens.count { it.options.size > 1 }
        var product = 1L
        for (t in tokens) {
            product *= t.options.size
            if (product > 1_000_000L) break
        }
        val maxDeviations = if (product <= maxVariants) ambiguous else minOf(ambiguous, 3)

        val seen = LinkedHashMap<String, Variant>()
        val buf = StringBuilder()

        fun walk(index: Int, devLeft: Int, target: Int) {
            if (seen.size >= maxVariants) return
            if (index == tokens.size) {
                if (devLeft == 0) {
                    val raw = buf.toString()
                    val sinhala = SinhalaTransliterator.transliterate(raw)
                    if (!seen.containsKey(sinhala)) {
                        seen[sinhala] = Variant(raw, sinhala, target)
                    }
                }
                return
            }
            val opts = tokens[index].options
            val mark = buf.length
            for ((k, opt) in opts.withIndex()) {
                val used = if (k == 0) 0 else 1
                if (used > devLeft) continue
                buf.append(opt)
                walk(index + 1, devLeft - used, target)
                buf.setLength(mark)
                if (seen.size >= maxVariants) return
            }
        }

        // Fewest changed letters first, so the default reading always leads.
        for (target in 0..maxDeviations) {
            walk(0, target, target)
            if (seen.size >= maxVariants) break
        }
        return seen.values.toList()
    }

    /**
     * The form to use for dictionary prefix-completion: while a word is still
     * being typed, a trailing consonant is rendered with hal kirima (්), but
     * the word the user is heading toward usually continues with a vowel, so
     * the hal is dropped for the lookup.
     */
    fun completionPrefix(sinhala: String): String =
        if (sinhala.isNotEmpty() && sinhala.last() == HAL) sinhala.dropLast(1) else sinhala
}
