package com.spmods.sinkey.ime

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.spmods.sinkey.R
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.keyboard.Board
import com.spmods.sinkey.keyboard.KeyboardView
import com.spmods.sinkey.keyboard.SinhalaTransliterator
import com.spmods.sinkey.ui.theme.SinKeyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * The keyboard engine itself. Android binds this service whenever SinKey is
 * the active input method; [onCreateInputView] returns the Compose UI that
 * the system displays above the app currently being typed into.
 */
class SinKeyInputMethodService : InputMethodService() {

    init {
        setTheme(R.style.Theme_SinKey_IME)
    }

    private lateinit var lifecycleOwner: ImeLifecycleOwner
    private lateinit var prefs: PreferencesManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var wordBuffer = StringBuilder()
    private var englishBuffer = StringBuilder()
    private var currentLanguage = mutableStateOf("si")
    private var suggestions = mutableStateOf<List<String>>(emptyList())
    private var currentInputTypeState = mutableStateOf(0)

    // Board state lives at service level — NOT inside the Composable — so it
    // survives keyboard hide/show cycles. If held in remember{} it resets to
    // MAIN every time the user dismisses and reopens the keyboard.
    private var boardStack = mutableStateOf(listOf(Board.MAIN))

    // Shift has 3 states: OFF, ONE_SHOT (next letter only), LOCKED (caps lock).
    // Stored at service level so it survives hide/show cycles.
    // AUTO-SHIFT: enabled at sentence start (after . ! ? or at field open).
    enum class ShiftState { OFF, ONE_SHOT, LOCKED }
    private var shiftState = mutableStateOf(ShiftState.ONE_SHOT) // default: first letter capital

    // FIX #1 & #3: Cached prefs — read once on start, updated via coroutine.
    // Eliminates runBlocking on every key tap (was causing main-thread lag / ANR).
    // Also enables key sound which was previously unimplemented.
    private var cachedVibrateEnabled = false
    private var cachedSoundEnabled = true

    // FIX #2: Single reusable SpellCheckerSession — created once, reused across
    // keystrokes. Previous code created a new session per keystroke, leaking OS
    // resources and causing memory growth over time.
    private var spellCheckerSession: android.view.textservice.SpellCheckerSession? = null

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ImeLifecycleOwner()
        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        prefs = PreferencesManager(this)

        // FIX #1: Still need initial language synchronously, but we only block once
        // here at startup (not on every key tap).
        currentLanguage.value = runBlocking { prefs.defaultLanguage.first() }

        // FIX #1 + #3: Keep feedback prefs cached in memory; update asynchronously
        // whenever the user changes them in Settings. No blocking reads on key taps.
        serviceScope.launch {
            prefs.keyVibrateEnabled.collect { cachedVibrateEnabled = it }
        }
        serviceScope.launch {
            prefs.keySoundEnabled.collect { cachedSoundEnabled = it }
        }

        // FIX #2: Create spell-checker session once for the lifetime of the service.
        initSpellCheckerSession()

        initEmojiCompat()
    }

    private fun initEmojiCompat() {
        if (runCatching { EmojiCompat.get() }.isSuccess) return
        try {
            val config = BundledEmojiCompatConfig(this)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        android.util.Log.i("SinKey", "EmojiCompat (bundled) initialized")
                    }
                    override fun onFailed(throwable: Throwable?) {
                        android.util.Log.w("SinKey", "EmojiCompat init failed", throwable)
                    }
                })
            EmojiCompat.init(config)
        } catch (e: Exception) {
            android.util.Log.w("SinKey", "EmojiCompat init exception", e)
        }
    }

    // FIX #2: Single SpellCheckerSession created once and reused.
    private fun initSpellCheckerSession() {
        val tsm = getSystemService(android.view.textservice.TextServicesManager::class.java)
            ?: return
        try {
            spellCheckerSession = tsm.newSpellCheckerSession(
                null,
                java.util.Locale.ENGLISH,
                object : android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<out android.view.textservice.SuggestionsInfo>?) {
                        val raw = englishBuffer.toString()
                        val words = mutableListOf<String>()
                        if (raw.isNotEmpty()) words.add(raw)
                        results?.forEach { info ->
                            for (i in 0 until info.suggestionsCount) {
                                val s = info.getSuggestionAt(i)
                                if (s != raw && words.size < 5) words.add(s)
                            }
                        }
                        if (words.isNotEmpty()) suggestions.value = words
                    }
                    override fun onGetSentenceSuggestions(results: Array<out android.view.textservice.SentenceSuggestionsInfo>?) {}
                },
                false
            )
        } catch (e: Exception) {
            android.util.Log.w("SinKey", "SpellCheckerSession init failed", e)
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ImeComposeView(this, lifecycleOwner) {
                val themeMode by prefs.themeMode.collectAsState(initial = com.spmods.sinkey.data.ThemeMode.SYSTEM)
                val isDark = when (themeMode) {
                    com.spmods.sinkey.data.ThemeMode.LIGHT  -> false
                    com.spmods.sinkey.data.ThemeMode.DARK   -> true
                    com.spmods.sinkey.data.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                val keyboardHeight by prefs.keyboardHeight.collectAsState(initial = 2f)
                val bottomSpaceEnabled by prefs.bottomSpaceEnabled.collectAsState(initial = true)
                val bottomSpaceSize by prefs.bottomSpaceSize.collectAsState(initial = 0f)
                val showKeyBorders by prefs.showKeyBorders.collectAsState(initial = true)
                SinKeyTheme(themeMode = themeMode) {
                    KeyboardView(
                        currentLanguage = currentLanguage.value,
                        keyboardHeight = keyboardHeight,
                        bottomSpaceEnabled = bottomSpaceEnabled,
                        bottomSpaceSize = bottomSpaceSize,
                        showKeyBorders = showKeyBorders,
                        isDark = isDark,
                        suggestions = suggestions.value,
                        onSuggestionSelected = ::handleSuggestion,
                        onKey = ::handleKey,
                        inputType = currentInputTypeState.value,
                        boardStack = boardStack.value,
                        onBoardStackChange = { boardStack.value = it },
                        shiftState = shiftState.value,
                        onShiftStateChange = { shiftState.value = it }
                    )
                }
        }

        return composeView
    }

    // AbstractComposeView.onAttachedToWindow() looks for ViewTreeLifecycleOwner
    // on the Window, not by walking up the view tree. The window is only
    // accessible via getWindow() on InputMethodService, which corresponds to
    // the IME's own Dialog window — so we must set owners there.
    // We override onWindowShown() because at that point the IME window is fully
    // initialized and window.decorView is non-null and attached.
    override fun onWindowShown() {
        super.onWindowShown()
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            decor.setViewTreeViewModelStoreOwner(lifecycleOwner)
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // lifecycle ON_RESUME is driven by onWindowShown()

        // Bug O4 Fix: Cancel any active composing span on the previous
        // InputConnection before switching fields. Without this, the underlined
        // Sinhala preview text stays visible in the old field after focus moves
        // to a new one, and the new field starts with a stale composing state —
        // creating the appearance of keyboard text appearing in two places at once.
        currentInputConnection?.finishComposingText()

        wordBuffer.clear()
        englishBuffer.clear()
        suggestions.value = emptyList()
        currentInputTypeState.value = info?.inputType ?: 0

        // Reset board to MAIN when the user moves to a different input field
        // (not on simple hide/show of the same field). restarting=true means
        // the same field re-focused, so we keep the current board in that case.
        if (!restarting) {
            boardStack.value = listOf(Board.MAIN)
            shiftState.value = ShiftState.ONE_SHOT // auto-shift: capitalize first letter of new field
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // lifecycle ON_PAUSE is driven by onWindowHidden()
        commitPendingWord()
    }

    override fun onDestroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        // FIX #2: Close spell-checker session to release OS resources.
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    private fun handleKey(key: String) {
        maybeFeedback()
        val ic = currentInputConnection ?: return

        when (key) {
            "BACKSPACE" -> {
                val selectedText = ic.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    wordBuffer.clear()
                    ic.finishComposingText()
                    ic.commitText("", 1)
                } else if (wordBuffer.isNotEmpty()) {
                    wordBuffer.deleteCharAt(wordBuffer.length - 1)
                    if (wordBuffer.isEmpty()) {
                        ic.setComposingText("", 1)
                        ic.finishComposingText()
                    } else {
                        ic.setComposingText(renderBuffer(), 1)
                    }
                } else {
                    if (englishBuffer.isNotEmpty()) englishBuffer.deleteCharAt(englishBuffer.length - 1)
                    val beforeCursor = ic.getTextBeforeCursor(4, 0)
                    if (!beforeCursor.isNullOrEmpty()) {
                        val lastCodePoint = Character.codePointBefore(beforeCursor, beforeCursor.length)
                        val charCount = Character.charCount(lastCodePoint)
                        ic.deleteSurroundingText(charCount, 0)
                    } else {
                        ic.deleteSurroundingText(1, 0)
                    }
                    updateSuggestions()
                }
                // After backspace, check if we're now at a sentence start
                updateAutoShift(ic)
            }
            "SPACE" -> {
                if (currentLanguage.value == "si") commitPendingWord()
                else { englishBuffer.clear(); suggestions.value = emptyList() }
                ic.commitText(" ", 1)
                // After space, check if previous char was sentence-ending punctuation
                updateAutoShift(ic)
            }
            "ENTER" -> {
                if (currentLanguage.value == "si") commitPendingWord()
                else { englishBuffer.clear(); suggestions.value = emptyList() }
                ic.commitText("\n", 1)
                // New line = sentence start → auto-shift
                if (shiftState.value == ShiftState.OFF) shiftState.value = ShiftState.ONE_SHOT
            }
            "SHIFT" -> {
                // Single tap cycles: OFF → ONE_SHOT → OFF
                // Double tap (handled via SHIFT_LOCK from KeyboardView) → LOCKED
                shiftState.value = when (shiftState.value) {
                    ShiftState.OFF      -> ShiftState.ONE_SHOT
                    ShiftState.ONE_SHOT -> ShiftState.OFF
                    ShiftState.LOCKED   -> ShiftState.OFF
                }
            }
            "SHIFT_LOCK" -> {
                shiftState.value = if (shiftState.value == ShiftState.LOCKED) ShiftState.OFF else ShiftState.LOCKED
            }
            "SYMBOLS_SHIFT", "EMOJI", "NUMPAD" -> { /* handled in KeyboardView */ }
            "TOOL_MIC" -> { sendDefaultEditorAction(true) }
            "TOOL_APPS", "TOOL_STICKER", "TOOL_CLIPBOARD", "TOOL_FONT",
            "TOOL_TRANSLATE", "TOOL_SETTINGS" -> {
                android.util.Log.d("SinKey", "Tool action: $key (not yet implemented)")
            }
            "SWITCH_KEYBOARD" -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
            "LANG_TOGGLE" -> {
                commitPendingWord()
                englishBuffer.clear()
                suggestions.value = emptyList()
                currentLanguage.value = if (currentLanguage.value == "en") "si" else "en"
            }
            "," , "." -> {
                commitPendingWord()
                englishBuffer.clear()
                suggestions.value = emptyList()
                ic.commitText(key, 1)
                // Period → next word should be capitalized
                if (key == ".") {
                    if (shiftState.value == ShiftState.OFF) shiftState.value = ShiftState.ONE_SHOT
                }
            }
            else -> {
                val isSinglePrintable = key.length == 1 && !key[0].isLetter()
                if (isSinglePrintable) {
                    commitPendingWord()
                    englishBuffer.clear()
                    ic.commitText(key, 1)
                    // Check sentence-ending punctuation (! ?)
                    if (key == "!" || key == "?") {
                        if (shiftState.value == ShiftState.OFF) shiftState.value = ShiftState.ONE_SHOT
                    }
                    return
                }
                if (isEmoji(key)) {
                    commitPendingWord()
                    ic.commitText(key, 1)
                    serviceScope.launch { prefs.addRecentEmoji(key) }
                } else if (currentLanguage.value == "si") {
                    val lower = key.lowercase()
                    wordBuffer.append(lower)
                    val preview = SinhalaTransliterator.transliterate(wordBuffer.toString())
                    ic.setComposingText(preview, 1)
                    updateSuggestions()
                    // Consume one-shot shift after first Sinhala letter
                    if (shiftState.value == ShiftState.ONE_SHOT) shiftState.value = ShiftState.OFF
                } else {
                    // Apply shift to English letter
                    val typed = if (shiftState.value != ShiftState.OFF) key.uppercase() else key.lowercase()
                    englishBuffer.append(typed)
                    ic.commitText(typed, 1)
                    updateSuggestions()
                    // Consume one-shot shift after letter
                    if (shiftState.value == ShiftState.ONE_SHOT) shiftState.value = ShiftState.OFF
                }
            }
        }
    }

    /** Auto-shift: if the text before cursor ends with ". ", "! ", "? " or is empty → ONE_SHOT */
    private fun updateAutoShift(ic: android.view.inputmethod.InputConnection) {
        if (shiftState.value == ShiftState.LOCKED) return
        val before = ic.getTextBeforeCursor(3, 0)?.toString() ?: ""
        val shouldShift = before.isEmpty() ||
            before.endsWith(". ") || before.endsWith("! ") || before.endsWith("? ") ||
            before.endsWith(".\n") || before.endsWith("!\n") || before.endsWith("?\n")
        shiftState.value = if (shouldShift) ShiftState.ONE_SHOT else ShiftState.OFF
    }

    private fun renderBuffer(): String = SinhalaTransliterator.transliterate(wordBuffer.toString())

    /**
     * FIX #10: Removed the `key.length > 8` guard that rejected ZWJ emoji
     * sequences (family emojis, skin-tone variants, flags) which are often
     * longer than 8 UTF-16 chars. Now we only check for emoji code-point ranges.
     * Tool-action strings are excluded because they contain only ASCII letters.
     */
    private fun isEmoji(key: String): Boolean {
        // Tool action strings (e.g. "LANG_TOGGLE") contain only ASCII letters —
        // they will never match the emoji code-point ranges below.
        return key.codePoints().anyMatch { cp ->
            cp in 0x2600..0x27BF ||
            cp in 0x1F300..0x1FAFF ||
            cp in 0x1F900..0x1F9FF ||
            cp in 0x2300..0x23FF ||
            cp in 0x25A0..0x25FF ||
            cp in 0x2B00..0x2BFF
        }
    }

    private fun commitPendingWord() {
        if (wordBuffer.isEmpty()) return
        val ic = currentInputConnection
        val finalWord = SinhalaTransliterator.transliterate(wordBuffer.toString())
        ic?.setComposingText("", 1)
        ic?.commitText(finalWord, 1)
        wordBuffer.clear()
        suggestions.value = emptyList()
    }

    private fun handleSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (currentLanguage.value == "si") {
            ic.setComposingText("", 1)
            ic.commitText(word, 1)
            wordBuffer.clear()
        } else {
            val len = englishBuffer.length
            if (len > 0) ic.deleteSurroundingText(len, 0)
            ic.commitText(word, 1)
            englishBuffer.clear()
        }
        suggestions.value = emptyList()
    }

    private fun updateSuggestions() {
        val raw = if (currentLanguage.value == "si") wordBuffer.toString() else englishBuffer.toString()
        if (raw.isEmpty()) { suggestions.value = emptyList(); return }

        if (currentLanguage.value == "si") {
            val primary = SinhalaTransliterator.transliterate(raw)
            val list = mutableListOf(primary)
            val withA = SinhalaTransliterator.transliterate("${raw}a")
            if (withA != primary) list.add(withA)
            if (raw.length > 1) {
                val cap = SinhalaTransliterator.transliterate(raw[0].uppercaseChar() + raw.substring(1))
                if (cap != primary && cap != withA) list.add(cap)
            }
            suggestions.value = list.take(5)
        } else {
            // FIX #2: Use the single reusable session instead of creating a new one per keystroke.
            val session = spellCheckerSession
            if (session != null) {
                // Show typed word immediately; async callback will update with real suggestions.
                if (suggestions.value.firstOrNull() != raw) suggestions.value = listOf(raw)
                try {
                    session.getSuggestions(android.view.textservice.TextInfo(raw), 4)
                } catch (e: Exception) {
                    android.util.Log.w("SinKey", "getSuggestions failed", e)
                }
            } else {
                suggestions.value = listOf(raw)
            }
        }
    }

    /**
     * FIX #1: No more runBlocking. Reads cached in-memory values (updated via
     * Flow collectors in onCreate) — zero blocking, zero DataStore I/O per tap.
     * FIX #3: Key sound now actually implemented using AudioManager.FX_KEYPRESS_STANDARD.
     */
    private fun maybeFeedback() {
        if (cachedVibrateEnabled) {
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        if (cachedSoundEnabled) {
            val audio = getSystemService(AudioManager::class.java)
            audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }
}
