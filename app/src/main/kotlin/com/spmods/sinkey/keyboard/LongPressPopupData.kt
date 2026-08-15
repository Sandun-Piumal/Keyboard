package com.spmods.sinkey.keyboard

/**
 * Maps each base letter to the row of accented/alternate characters shown
 * in the long-press popup above that key — e.g. long-pressing "a" shows
 * "æ ã å ā à á â ä".
 *
 * Values and ordering ported directly from FlorisBoard's own English
 * popup mapping (app/src/main/assets/ime/keyboard/
 * org.florisboard.localization/popupMappings/en.json, "all" section,
 * "relevant" arrays), which in turn mirrors the standard set most
 * keyboards (Gboard included) show for these keys. Only 8 English letters
 * have any alternates at all — every other letter has none, which matches
 * real keyboard behavior; a key with no entry here simply never triggers
 * the popup on long-press since there's nothing useful to show.
 *
 * Lowercase keys only: LetterKey uppercases the result itself when the
 * base key being long-pressed is currently showing uppercase, so this map
 * doesn't need a separate uppercase copy.
 */
val longPressPopupAlternates: Map<Char, List<String>> = mapOf(
    'a' to listOf("æ", "ã", "å", "ā", "à", "á", "â", "ä"),
    'c' to listOf("ç"),
    'e' to listOf("ē", "ê", "é", "è", "ë"),
    'i' to listOf("ì", "ï", "í", "î", "ī"),
    'n' to listOf("ñ", "ń"),
    'o' to listOf("õ", "ō", "œ", "ø", "ò", "ö", "ó", "ô"),
    's' to listOf("ß"),
    'u' to listOf("ú", "ū", "ü", "û", "ù"),
)
