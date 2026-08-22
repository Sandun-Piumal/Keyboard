package com.spmods.sinkey.keyboard

/**
 * "Decorative text" feature — wraps a whole word/phrase in a
 * prefix/suffix template (box glyphs + emoji + arrow, vine borders, etc.),
 * the aesthetic-text style seen in things like the "Teddy keyboard" app
 * (e.g. "වෙනස්-⊟🍒🎀»"). This is deliberately a *separate* mechanism from
 * FancyTextMapper (KeyboardLayouts.kt):
 *   • FancyTextMapper substitutes each English letter for a styled Unicode
 *     look-alike (character-for-character, English only).
 *   • DecorationStyle instead wraps the *whole* word with a fixed
 *     prefix/suffix (works for Sinhala and English alike, since it never
 *     touches the letters themselves) — OR, for GLITCH-kind styles, layers
 *     combining diacritical marks onto each of the word's own characters in
 *     addition to a prefix/suffix wrap (see DecorationKind's doc comment
 *     below).
 * Both can be layered: apply() takes already-fancy-styled text as input
 * when the caller wants both active at once — see IME service call sites.
 */

/**
 * Whether a DecorationStyle wraps text unchanged (WRAP) or also glitches
 * the word's own letters with combining marks first (GLITCH) — see
 * DecorationStyle.kind's doc comment for the full explanation. A top-level
 * type rather than nested inside DecorationStyle: Kotlin requires every
 * enum entry (NONE, BOX_CHERRY_BOW, ...) to appear before any other member
 * declaration in an enum class body, so a nested "enum class Kind" can't
 * sit above them the way a nested type normally could in an ordinary class.
 */
enum class DecorationKind { WRAP, GLITCH }

enum class DecorationStyle(
    val key: String,
    val label: String,
    val prefix: String,
    val suffix: String,
    // WRAP: prefix/suffix concatenated around the word unchanged — the
    // original, simpler mechanism (box+emoji+arrow, vine borders).
    // GLITCH: the word's own characters each get combining diacritical
    // marks layered onto them (the "zalgo text" look — e.g. "පිච්ච" →
    // "පි͠ච̷්ච̶"), in addition to the same prefix/suffix wrap — see the
    // reference example this style was requested from:
    // "පි͠ච̷්ච̶─⃞🌸⃘̬ٜٜٜ͠🍃⃘̬͞⃝🦋》". Handled by TextDecorator.apply, which
    // checks `kind` and calls GlitchMarks.apply() first when it's GLITCH.
    val kind: DecorationKind = DecorationKind.WRAP
) {
    NONE("none", "None", "", ""),

    // ── Matching the reference screenshot's own pattern ─────────────────
    // word + "-" + box glyph + emoji + arrow, e.g. "වෙනස්-⊟🍒🎀»"
    BOX_CHERRY_BOW("box_cherry_bow", "Box · Cherry · Bow", "-⊟", "🍒🎀»"),
    BOX_COFFEE_LOUPE("box_coffee_loupe", "Box · Coffee · Loupe", "-⊟", "☕🔍»"),
    BOX_HEART_ARROW("box_heart_arrow", "Box · Heart · Arrow", "-⊟", "💗→»"),
    BOX_STRAWBERRY("box_strawberry", "Box · Strawberry", "-⊟", "🍓🍥»"),
    BOX_FLOWER_STAR("box_flower_star", "Box · Flower · Star", "-⊟", "🌸⭐»"),
    BOX_RABBIT_HEART("box_rabbit_heart", "Box · Rabbit · Heart", "-⊟", "🐇💗»"),
    BOX_BEAR("box_bear", "Box · Bear", "-⊟", "🧸→»"),
    BOX_HEART_RING("box_heart_ring", "Box · Heart · Ring", "-⊟", "💞→»"),
    BOX_CUPCAKE("box_cupcake", "Box · Cupcake", "-⊟", "🧁→»"),
    BOX_CLOVER("box_clover", "Box · Clover", "-⊟", "🍀→»"),
    BOX_SUNFLOWER("box_sunflower", "Box · Sunflower", "-⊟", "🌻🍀»"),
    BOX_STAR_ONLY("box_star", "Box · Star", "-⊟", "⭐»"),
    BOX_UNICORN_RAINBOW("box_unicorn_rainbow", "Box · Unicorn · Rainbow", "-⊟", "🦄🌈»"),
    BOX_BUTTERFLY("box_butterfly", "Box · Butterfly", "-⊟", "🦋→»"),
    BOX_RAINBOW_STAR("box_rainbow_star", "Box · Rainbow · Star", "-⊟", "🌈⭐»"),

    // ── Vine / decorative border styles ─────────────────────────────────
    // Wraps the whole word left+right instead of only trailing it, for a
    // "framed" look rather than a label-style tag.
    VINE_FLOWER("vine_flower", "Vine · Flower", "══❀••", "••❀══"),
    VINE_LEAF("vine_leaf", "Vine · Leaf", "⋆⁺₊❍", "❍₊⁺⋆"),
    VINE_STAR("vine_star", "Vine · Star", "｡°✩", "✩°｡"),
    VINE_HEART("vine_heart", "Vine · Heart", "♡⋆｡˚", "˚｡⋆♡"),
    VINE_SPARKLE("vine_sparkle", "Vine · Sparkle", "✧･ﾟ:", ":ﾟ･✧"),

    // ── Glitch styles ─────────────────────────────────────────────────
    // Combining-mark "zalgo" effect on the word's own letters + a
    // box/emoji-cluster wrap, matching the requested reference pattern.
    // Each has a fixed emoji cluster for its suffix (so the same style key
    // always looks recognizably the same style) — the letter marks
    // themselves are randomized per call, per GlitchMarks.apply's own doc
    // comment, which is where the "same style, different word, different
    // marks each time" behavior actually lives.
    GLITCH_BUTTERFLY("glitch_butterfly", "Glitch · Butterfly", "─⃞", "🌸⃘̬ٜٜٜ͠🍃⃘̬͞⃝🦋》", DecorationKind.GLITCH),
    GLITCH_MOON("glitch_moon", "Glitch · Moon", "─⃞", "🌙⃘̬ٜٜٜ͠✨⃘̬͞⃝⭐》", DecorationKind.GLITCH),
    GLITCH_ROSE("glitch_rose", "Glitch · Rose", "─⃞", "🥀⃘̬ٜٜٜ͠🍂⃘̬͞⃝🕸》", DecorationKind.GLITCH),
    GLITCH_SKULL("glitch_skull", "Glitch · Skull", "─⃞", "💀⃘̬ٜٜٜ͠⚡⃘̬͞⃝🖤》", DecorationKind.GLITCH),
    GLITCH_SNAKE("glitch_snake", "Glitch · Snake", "─⃞", "🐍⃘̬ٜٜٜ͠🌿⃘̬͞⃝🍄》", DecorationKind.GLITCH);

    companion object {
        fun fromKey(key: String): DecorationStyle = entries.find { it.key == key } ?: NONE

        /**
         * The real, pickable styles — excludes NONE, which only exists as
         * the internal "nothing selected yet" sentinel/preference default
         * (see PreferencesManager.decorationStyle) and as the fallback used
         * when the whole feature is toggled off. NONE is never shown as a
         * row in DecorationPickerView — with no style chosen, "Vary styles"
         * cycles through this full list instead of falling back to plain
         * text (see cycleStyleFor in this file).
         */
        val pickable: List<DecorationStyle> = entries.filter { it != NONE }
    }
}

/**
 * Generates the "zalgo"-style combining-diacritical-mark overlay used by
 * DecorationKind.GLITCH. The person explicitly asked for the marks to
 * vary — the same word decorated twice should look visibly different each
 * time, not use one fixed pattern — but that can't mean truly random on
 * every call: the suggestion-bar chip and the actual committed text are two
 * separate TextDecorator.apply() calls for the same word (render the chip,
 * then commit when tapped — see KeyboardView's chip and
 * SinKeyInputMethodService's decorate()), and if each call rolled its own
 * marks independently, what got typed into the field would never match
 * what the user saw and tapped. So apply() takes a [seed] and marks are
 * derived from a Random seeded with it — same (word, style, seed) always
 * reproduces identically (chip and commit match), while different seeds
 * (different words, different suggestion slots, different times) still
 * look different from each other, which is what satisfies the "vary each
 * time" ask without breaking the preview-matches-commit guarantee.
 */
private object GlitchMarks {
    // A mix of above-character and below-character combining marks —
    // matches the visual mix in the reference example, which combines
    // over-marks (accents/dots above) and under-marks (dots/lines below)
    // on the same letter rather than only one or the other.
    private val marks = listOf(
        '\u0301', '\u0300', '\u0302', '\u0303', '\u0308', // above: ́ ̀ ̂ ̃ ̈
        '\u0330', '\u0331', '\u0336', '\u0337',            // below: ̰ ̱ ̶ ̷
        '\u0489', '\u065F',                                 // ͉ٟ (extra combining marks for density)
        '\u0651', '\u0655'                                  // ّ ٕ
    )

    /**
     * Returns [text] with 1-3 combining marks appended after each character
     * (so they render layered onto that character, per how combining marks
     * work), deterministically derived from [seed] — see this object's doc
     * comment for why determinism-per-seed (not true randomness) is
     * required here.
     */
    fun apply(text: String, seed: Long): String {
        if (text.isEmpty()) return text
        val random = java.util.Random(seed)
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(ch)
            val markCount = 1 + random.nextInt(3)
            repeat(markCount) {
                sb.append(marks[random.nextInt(marks.size)])
            }
        }
        return sb.toString()
    }
}

object TextDecorator {
    /**
     * Wraps [text] in [style]'s prefix/suffix. NONE (or blank [text])
     * returns [text] unchanged. Works on already-styled input (e.g. text
     * that's already been through FancyTextMapper) since it only ever
     * concatenates around the outside — never inspects individual
     * characters, so it's safe to layer after fancy-font styling. For
     * DecorationKind.GLITCH styles, the word's own characters are first run through
     * GlitchMarks.apply() (see its doc comment for why this is
     * seed-derived, not freely random) before the prefix/suffix wrap goes
     * on around the result. [glitchSeed] defaults to [text]'s own hashCode
     * — good enough for the common case (same word ⇒ same marks, so a
     * chip's preview and its commit always match without the caller having
     * to thread a seed through); callers that also want different
     * suggestion-bar slots showing the same style to glitch differently
     * (see KeyboardView's chip rendering) pass their own more specific seed
     * (word + style + index) instead.
     */
    fun apply(text: String, style: DecorationStyle, glitchSeed: Long = text.hashCode().toLong()): String {
        if (style == DecorationStyle.NONE || text.isEmpty()) return text
        val body = if (style.kind == DecorationKind.GLITCH) GlitchMarks.apply(text, glitchSeed) else text
        return style.prefix + body + style.suffix
    }

    /**
     * "Vary styles" mode: no single style is selected, so instead each
     * suggestion-bar slot gets a different style, cycling through
     * DecorationStyle.pickable by [index] (the suggestion's position in the
     * bar). Deterministic per index — not random — so the same suggestion
     * set shows the same style each recomposition instead of flickering
     * between styles on every redraw. A GLITCH style's own marks are
     * derived from apply()'s glitchSeed parameter (default: the word's own
     * hashCode — see apply()'s doc comment), independent of this function;
     * the two kinds of "varying" — which style a slot gets, and what a
     * GLITCH style's marks look like — are unrelated mechanisms that both
     * happen to satisfy the same "make it vary" ask.
     */
    fun cycleStyleFor(index: Int): DecorationStyle {
        val styles = DecorationStyle.pickable
        if (styles.isEmpty()) return DecorationStyle.NONE
        return styles[index % styles.size]
    }
}
