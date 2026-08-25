package com.spmods.sinkey.keyboard


object ZeroWidthEncoder {
    private const val ZW_ZERO = '\u200B'
    private const val ZW_ONE = '\u200C'
    private const val ZW_START = '\u200D' 
    private const val ZW_END = '\u2060'   

    private const val PATTERN_DOTS = "....."
    private const val PATTERN_QUOTES = "'''''"


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

        while (bitPos < bits.length) {
            result.append(bits[bitPos])
            bitPos++
        }
        return result.toString()
    }


    private fun buildPattern(minVisibleLength: Int): String {
        val unit = PATTERN_DOTS + PATTERN_QUOTES
        val sb = StringBuilder()
        while (sb.length < minVisibleLength) sb.append(unit)
        return sb.toString()
    }


    fun containsEncoded(text: String): Boolean {
        val start = text.indexOf(ZW_START)
        if (start == -1) return false
        return text.indexOf(ZW_END, start) != -1
    }


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

