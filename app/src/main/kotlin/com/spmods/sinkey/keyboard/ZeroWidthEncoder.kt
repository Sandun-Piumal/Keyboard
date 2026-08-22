package com.spmods.sinkey.keyboard

/**
 * Encodes text into invisible zero-width Unicode characters, wrapped
 * inside a visible dot/quote pattern (".....'''''.....'''''" — repeating,
 * length matched to the encoded payload) so the result always reads as an
 * intentional decorative pattern in any chat app, never as an empty or
 * suspiciously blank message. Deliberately never produces an
 * all-invisible/no-visible-content result — see encode()'s doc comment.
 *
 * Mechanism: each byte of the UTF-8 payload becomes 8 zero-width bits
 * (ZW_ZERO / ZW_ONE), framed by ZW_START/ZW_END markers so decode() can
 * find an encoded span inside arbitrary surrounding text (e.g. after a
 * paste picks up extra characters). Interleaved one-for-one with the
 * visible pattern characters, alternating, so the invisible characters
 * aren't all clumped at the end (a long invisible tail after a short
 * visible pattern can render as a suspicious-looking cursor gap on some
 * keyboards/apps).
 */
object ZeroWidthEncoder {
    private const val ZW_ZERO = '\u200B'  // zero-width space      → bit 0
    private const val ZW_ONE = '\u200C'   // zero-width non-joiner → bit 1
    private const val ZW_START = '\u200D' // zero-width joiner     → payload start marker
    private const val ZW_END = '\u2060'   // word joiner           → payload end marker

    private const val PATTERN_DOTS = "....."
    private const val PATTERN_QUOTES = "'''''"

    /**
     * Wraps [visibleText] with the dot/quote pattern and interleaves
     * [hiddenText]'s zero-width encoding through it. [visibleText] is
     * *required*, not optional — if the caller has nothing to visibly
     * show, this function generates a short default pattern span on its
     * own rather than accept an empty string, so the output is never
     * indistinguishable from an empty message. That's a deliberate
     * limitation, not an oversight: this tool is for hiding a payload
     * inside content that's visibly there, not for disguising a message
     * as blank.
     */
    fun encode(hiddenText: String, visibleText: String = ""): String {
        if (hiddenText.isEmpty()) return visibleText
        val pattern = buildPattern(minVisibleLength = hiddenText.length.coerceAtLeast(10))
        val visible = visibleText.ifEmpty { pattern }
        val bits = StringBuilder()
        bits.append(ZW_START)
        for (byte in hiddenText.toByteArray(Charsets.UTF_8)) {
            for (bitIndex in 7 downTo 0) {
                val bit = (byte.toInt() shr bitIndex) and 1
                bits.append(if (bit == 1) ZW_ONE else ZW_ZERO)
            }
        }
        bits.append(ZW_END)

        // Interleave one visible char + a few invisible bits at a time,
        // rather than dumping all invisible characters after the visible
        // pattern — spreads the zero-width run out so it doesn't sit as one
        // long invisible block a renderer might collapse oddly.
        val result = StringBuilder()
        var bitPos = 0
        val bitsPerChunk = (bits.length / visible.length.coerceAtLeast(1)).coerceAtLeast(1)
        for (ch in visible) {
            result.append(ch)
            var taken = 0
            while (taken < bitsPerChunk && bitPos < bits.length) {
                result.append(bits[bitPos])
                bitPos++
                taken++
            }
        }
        // Any remaining bits (rounding leftover) go at the end.
        while (bitPos < bits.length) {
            result.append(bits[bitPos])
            bitPos++
        }
        return result.toString()
    }

    /** Repeats ".....'''''" until at least [minVisibleLength] characters long. */
    private fun buildPattern(minVisibleLength: Int): String {
        val unit = PATTERN_DOTS + PATTERN_QUOTES
        val sb = StringBuilder()
        while (sb.length < minVisibleLength) sb.append(unit)
        return sb.toString()
    }

    /**
     * True if [text] contains a complete ZW_START…ZW_END encoded span
     * anywhere in it (e.g. after being pasted alongside other text).
     */
    fun containsEncoded(text: String): Boolean {
        val start = text.indexOf(ZW_START)
        if (start == -1) return false
        return text.indexOf(ZW_END, start) != -1
    }

    /**
     * Extracts and decodes the first ZW_START…ZW_END span found in [text].
     * Returns null if no complete span is present (e.g. only half of it
     * was copied, or there's nothing encoded at all).
     */
    fun decode(text: String): String? {
        val start = text.indexOf(ZW_START)
        if (start == -1) return null
        val end = text.indexOf(ZW_END, start)
        if (end == -1) return null

        val bits = StringBuilder()
        for (i in (start + 1) until end) {
            when (text[i]) {
                ZW_ZERO -> bits.append('0')
                ZW_ONE -> bits.append('1')
                // Ignore any other character between the markers — the
                // visible pattern characters are interleaved through this
                // exact range by encode() above, so this is expected, not
                // an error.
                else -> {}
            }
        }
        if (bits.length < 8) return null

        val byteCount = bits.length / 8
        val bytes = ByteArray(byteCount)
        for (i in 0 until byteCount) {
            val byteBits = bits.substring(i * 8, i * 8 + 8)
            bytes[i] = byteBits.toInt(2).toByte()
        }
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
