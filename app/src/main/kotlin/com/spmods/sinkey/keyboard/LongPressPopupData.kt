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
/**
 * The row's number (q=1, w=2, e=3 ... p=0), keyed the same way as
 * [longPressPopupAlternates] — only row-0 keys (q w e r t y u i o p) have
 * one; letters on rows 1/2 (a s d f...) simply aren't in this map. Shown
 * as an extra cell inside the long-press popup itself (see NumberedLetterKey),
 * not as a separate long-press-again gesture, so the number stays reachable
 * even on keys that also have accent alternates (e, u, i, o).
 */
val rowNumberForKey: Map<Char, String> = mapOf(
    'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5",
    'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0",
)

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
