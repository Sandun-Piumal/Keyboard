package com.spmods.sinkey.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R
import com.spmods.sinkey.data.TranslateService
import kotlinx.coroutines.delay

/** Debounce gap before a translate request fires after the last keystroke. */
private const val TRANSLATE_DEBOUNCE_MS = 350L

/**
 * Board.TRANSLATE — the keyboard's translate tool (TOOL_TRANSLATE).
 *
 * Same reasoning as StickerTextComposeView (see that file's top-level doc
 * comment) for why this can't use a real TextField: this Composable *is*
 * the keyboard, so there's no other IME to type into a field shown here.
 * Instead it reuses MainKeyboardKeys with a local `sourceText` draft
 * buffer, and as that buffer changes, debounces a call to
 * [TranslateService.translate] and reports the live translated result back
 * through [onTranslatedTextChanged] — the caller (SinKeyInputMethodService)
 * is responsible for pushing that into the real InputConnection as
 * composing text, so it appears live in the actual target field exactly
 * where the user was typing, not just in a preview here.
 *
 * [sourceLang]/[targetLang] follow ISO 639-1 codes ("en"/"si"). The swap
 * button flips both the language pair and the current sourceText/committed
 * translation so the user can immediately continue typing in the other
 * direction without retyping anything.
 */
@Composable
internal fun TranslateView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    onTranslatedTextChanged: (String) -> Unit,
    onTranslationCommitted: () -> Unit,
    onBack: () -> Unit
) {
    var sourceLang by remember { mutableStateOf("en") }
    var targetLang by remember { mutableStateOf("si") }
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(true) }

    // Debounced live translation: waits for typing to pause briefly before
    // firing a request, same idea as mix-mode English suggestions'
    // requestId staleness guard in SinKeyInputMethodService, but simpler
    // here since LaunchedEffect's own key-based cancellation already
    // supersedes any in-flight request the moment sourceText/lang changes —
    // no manual request-id bookkeeping needed.
    LaunchedEffect(sourceText, sourceLang, targetLang) {
        if (sourceText.isBlank()) {
            translatedText = ""
            isTranslating = false
            onTranslatedTextChanged("")
            return@LaunchedEffect
        }
        isTranslating = true
        delay(TRANSLATE_DEBOUNCE_MS)
        val result = TranslateService.translate(sourceText, sourceLang, targetLang)
        isTranslating = false
        if (result != null) {
            translatedText = result
            onTranslatedTextChanged(result)
        }
    }

    val headerHeight = 44.dp
    val languageBarHeight = 40.dp
    val previewHeight = 64.dp

    Column(modifier = Modifier.fillMaxWidth().background(colors.bg)) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        // Leaving the board mid-translation shouldn't leave
                        // half-translated composing text sitting in the
                        // real field — clear it the same way an empty
                        // sourceText does above.
                        onTranslatedTextChanged("")
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_to_keyboard),
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = colors.subText
                )
            }
            Text(
                text = "Translate",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            // Done: commits the live-translated text into the real field as
            // final (non-composing) text and returns to the main keyboard —
            // mirrors StickerTextComposeView's Save button placement.
            // onTranslationCommitted (not onTranslatedTextChanged) is what
            // actually finalizes it — see SinKeyInputMethodService's
            // onTranslationCommitted doc comment for why a separate
            // "finish composing" step is needed here rather than reusing
            // the same live-preview callback that set the composing text.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (translatedText.isNotBlank()) DeshGreen else colors.keyBg)
                    .clickable(enabled = translatedText.isNotBlank()) {
                        onTranslationCommitted()
                        onBack()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Done",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (translatedText.isNotBlank()) androidx.compose.ui.graphics.Color.White else colors.subText
                )
            }
        }

        // ── Language pair + swap ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(languageBarHeight).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.keyBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = languageLabel(sourceLang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.keyText
                )
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        // Swap direction. Whatever was already translated
                        // becomes the new starting point to keep typing
                        // from, rather than discarding it — matches the
                        // swap affordance in the screenshot this feature
                        // was modeled on.
                        val newSource = targetLang
                        val newTarget = sourceLang
                        sourceLang = newSource
                        targetLang = newTarget
                        val carried = translatedText
                        translatedText = ""
                        sourceText = carried
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap languages",
                    modifier = Modifier.size(20.dp),
                    tint = DeshGreen
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.keyBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = languageLabel(targetLang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.keyText
                )
            }
        }

        // ── Live preview: what's being typed → what it translates to ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.keyBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = sourceText.ifBlank { "Type to translate…" },
                fontSize = 16.sp,
                color = if (sourceText.isBlank()) colors.subText else colors.keyText,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = translatedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeshGreen,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 8.dp).size(14.dp),
                        strokeWidth = 2.dp,
                        color = DeshGreen
                    )
                }
            }
        }

        // Reuses the real keyboard layout — see StickerTextComposeView's
        // top-level doc comment for why a real TextField isn't possible in
        // an IME's own UI. Always Latin/English layout regardless of the
        // app's current typing language, since the source text is typed in
        // whichever language sourceLang currently is (swappable above) —
        // for the common "type English, read Sinhala" direction this is
        // exactly the QWERTY layout needed; typing Sinhala source text
        // works too, transliterated the same way the main keyboard does,
        // since MainKeyboardKeys("si", ...) would apply transliteration —
        // but that's not needed here since onKey below appends raw key
        // characters directly with no transliteration, matching how
        // StickerTextComposeView's plain-English draft buffer works.
        MainKeyboardKeys(
            currentLanguage = "en",
            shift = shift,
            shiftLocked = false,
            onShiftStateChange = { newState ->
                shift = newState != com.spmods.sinkey.ime.SinKeyInputMethodService.ShiftState.OFF
            },
            keyHeight = keyHeight,
            keyShape = RoundedCornerShape(8.dp),
            bottomPadding = bottomPadding,
            colors = colors,
            onKey = { key ->
                when (key) {
                    "BACKSPACE" -> if (sourceText.isNotEmpty()) sourceText = sourceText.dropLast(1)
                    "SPACE" -> sourceText += " "
                    "ENTER" -> Unit // no multi-line composing here; Done commits instead
                    "SWITCH_KEYBOARD", "SYMBOLS", "ABC" -> Unit // not meaningful in this compose-only context
                    else -> if (key.codePointCount(0, key.length) == 1) {
                        sourceText += if (shift) key.uppercase() else key.lowercase()
                        if (shift) shift = false // one-shot shift, mirrors main keyboard's default feel
                    }
                }
            },
            onSymbols = {},
            onEmojiPicker = {},
            onLangTooltip = {},
            imeAction = android.view.inputmethod.EditorInfo.IME_ACTION_NONE
        )
    }
}

private fun languageLabel(code: String): String = when (code) {
    "en" -> "English"
    "si" -> "සිංහල"
    else -> code
}
