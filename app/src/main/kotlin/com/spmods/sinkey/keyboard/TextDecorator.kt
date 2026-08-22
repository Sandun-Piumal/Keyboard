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
 *     touches the letters themselves).
 * Both can be layered: apply() takes already-fancy-styled text as input
 * when the caller wants both active at once — see IME service call sites.
 */
enum class DecorationStyle(val key: String, val label: String, val prefix: String, val suffix: String) {
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
    VINE_SPARKLE("vine_sparkle", "Vine · Sparkle", "✧･ﾟ:", ":ﾟ･✧");

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

object TextDecorator {
    /**
     * Wraps [text] in [style]'s prefix/suffix. NONE (or blank [text])
     * returns [text] unchanged. Works on already-styled input (e.g. text
     * that's already been through FancyTextMapper) since it only ever
     * concatenates around the outside — never inspects individual
     * characters, so it's safe to layer after fancy-font styling.
     */
    fun apply(text: String, style: DecorationStyle): String {
        if (style == DecorationStyle.NONE || text.isEmpty()) return text
        return style.prefix + text + style.suffix
    }

    /**
     * "Vary styles" mode: no single style is selected, so instead each
     * suggestion-bar slot gets a different style, cycling through
     * DecorationStyle.pickable by [index] (the suggestion's position in the
     * bar). Deterministic per index — not random — so the same suggestion
     * set shows the same style each recomposition instead of flickering
     * between styles on every redraw.
     */
    fun cycleStyleFor(index: Int): DecorationStyle {
        val styles = DecorationStyle.pickable
        if (styles.isEmpty()) return DecorationStyle.NONE
        return styles[index % styles.size]
    }
}
