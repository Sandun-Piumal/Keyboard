package com.spmods.sinkey.ime

import android.inputmethodservice.InputMethodService
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.spmods.sinkey.data.PreferencesManager
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

    private lateinit var lifecycleOwner: ImeLifecycleOwner
    private lateinit var prefs: PreferencesManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Buffer of the word currently being typed, used for Sinhala transliteration.
    private var wordBuffer = StringBuilder()     // Sinhala roman input
    private var englishBuffer = StringBuilder()   // English word tracking (suggestions only)
    private var currentLanguage = mutableStateOf("si") // default per PreferencesManager
    private var suggestions = mutableStateOf<List<String>>(emptyList())
    private var currentInputTypeState = mutableStateOf(0)

    // ── Prevent double-keyboard rendering ────────────────────────────────────

    /** Never go fullscreen — prevents an extra overlay keyboard appearing. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    /** Never show extract-view (the editable text copy above the keyboard). */
    override fun onCreateExtractTextView(): android.view.View? = null

    /**
     * Report correct keyboard height to the host app (e.g. WhatsApp).
     * Without this, apps that query IME insets don't know how tall SinKey is
     * and position their own panels (emoji, sticker) ON TOP of the keyboard,
     * producing the "two keyboards stacked" appearance during page transitions.
     */
    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        outInsets.contentTopInsets = outInsets.visibleTopInsets
    }


    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ImeLifecycleOwner()
        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        prefs = PreferencesManager(this)

        currentLanguage.value = runBlocking { prefs.defaultLanguage.first() }

        // ── EmojiCompat: download latest emoji font from Google Fonts ──────
        // This allows ALL Unicode 15.x emojis to render correctly on any
        // Android device (API 24+), even without a system update.
        // Falls back to the bundled set if network is unavailable.
        initEmojiCompat()
    }

    /**
     * Initialises EmojiCompat with a downloadable Google Fonts emoji font.
     * On first run (or when a new font version is available) it downloads in
     * the background and stores it in the app's private cache.  Subsequent
     * launches use the cached version instantly.
     *
     * Falls back to [BundledEmojiCompatConfig] if the provider is unavailable
     * (e.g. device has no GMS / offline at first launch).
     */
    private fun initEmojiCompat() {
        // Skip if already initialised (service can be re-created)
        if (runCatching { EmojiCompat.get() }.isSuccess) return
        try {
            // BundledEmojiCompatConfig bundles a recent Noto Color Emoji font
            // directly in the APK via the emoji2-bundled artifact.
            // No network access needed, no ByteArray cert hash issues.
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

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

            setContent {
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
                        inputType = currentInputTypeState.value
                    )
                }
            }
        }

        // InputMethodService's window is a Dialog; Compose's WindowRecomposer looks
        // up the ViewTreeLifecycleOwner starting from the *window's decor view*
        // (e.g. the internal "parentPanel" layout), not from composeView itself.
        // Without this, attaching crashes with "ViewTreeLifecycleOwner not found".
        window?.window?.decorView?.apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
        }

        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        wordBuffer.clear()
        englishBuffer.clear()
        suggestions.value = emptyList()
        currentInputTypeState.value = info?.inputType ?: 0
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        commitPendingWord()
    }

    override fun onDestroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleKey(key: String) {
        maybeFeedback()
        val ic = currentInputConnection ?: return

        when (key) {
            "BACKSPACE" -> {
                // PRIORITY 1: If user has a selection, always delete it first —
                // regardless of whether wordBuffer has content.
                // This fixes the bug where Sinhala composing text was being
                // edited instead of deleting the user's text selection.
                val selectedText = ic.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    // Clear the composing buffer too — selection crosses word boundary
                    wordBuffer.clear()
                    ic.finishComposingText()
                    ic.commitText("", 1) // replaces selection with empty = delete
                } else if (wordBuffer.isNotEmpty()) {
                    // PRIORITY 2: In-progress Sinhala word — remove last roman letter.
                    wordBuffer.deleteCharAt(wordBuffer.length - 1)
                    if (wordBuffer.isEmpty()) {
                        // Buffer is now empty — cancel composing without committing.
                        // setComposingText("", 1) removes the composing text entirely.
                        ic.setComposingText("", 1)
                        ic.finishComposingText()
                    } else {
                        ic.setComposingText(renderBuffer(), 1)
                    }
                } else {
                    // PRIORITY 3: No composing, no selection — delete character before cursor.
                    // Use codepoint-aware deletion so emoji/Sinhala chars
                    // (which can be multiple UTF-16 units) are deleted correctly.
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
            }
            "SPACE" -> {
                if (currentLanguage.value == "si") commitPendingWord()
                else { englishBuffer.clear(); suggestions.value = emptyList() }
                ic.commitText(" ", 1)
            }
            "ENTER" -> {
                if (currentLanguage.value == "si") commitPendingWord()
                else { englishBuffer.clear(); suggestions.value = emptyList() }
                ic.commitText("\n", 1)
            }
            "SHIFT" -> {
                // handled visually inside KeyboardView; no text action here
            }
            "SYMBOLS_SHIFT" -> {
                // handled visually inside SymbolsKeyboardView
            }
            "EMOJI" -> {
                // handled visually inside KeyboardView (opens emoji picker)
            }
            "NUMPAD" -> {
                // handled visually inside SymbolsKeyboardView (opens numpad)
            }
            "TOOL_MIC" -> {
                // Voice input - trigger IME action
                sendDefaultEditorAction(true)
            }
            "SWITCH_KEYBOARD" -> {
                // Show system keyboard picker (InputMethodManager switcher dialog)
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
            }
            else -> {
                // Single printable non-letter characters from symbols keyboard
                // (e.g. @, #, !, 1, 2 ...) — commit directly without buffering
                val isSinglePrintable = key.length == 1 && !key[0].isLetter()
                if (isSinglePrintable) {
                    commitPendingWord()
                    englishBuffer.clear()
                    ic.commitText(key, 1)
                    return
                }
                // Check if the key is an emoji
                if (isEmoji(key)) {
                    commitPendingWord()
                    ic.commitText(key, 1)
                    serviceScope.launch {
                        prefs.addRecentEmoji(key)
                    }
                } else if (currentLanguage.value == "si") {
                    // Singlish mode: buffer the raw roman input, show live Sinhala preview
                    val lower = key.lowercase()
                    wordBuffer.append(lower)
                    // Show transliterated preview as composing (underlined) text
                    val preview = SinhalaTransliterator.transliterate(wordBuffer.toString())
                    ic.setComposingText(preview, 1)
                    updateSuggestions()
                } else {
                    // English mode: commit directly, track in englishBuffer for suggestions
                    englishBuffer.append(key)
                    ic.commitText(key, 1)
                    updateSuggestions()
                }
            }
        }
    }

    /** Converts the in-progress romanized buffer into live Sinhala preview text. */
    private fun renderBuffer(): String = SinhalaTransliterator.transliterate(wordBuffer.toString())

    /**
     * Returns true if [key] is an emoji / non-letter character that should be
     * committed directly and tracked in the recent-emojis list.
     * We detect this by checking that the string contains no ASCII letters and
     * has at least one Unicode code point in the emoji ranges.
     */
    private fun isEmoji(key: String): Boolean {
        if (key.length > 8) return false // tool actions like "LANG_TOGGLE" are long
        return key.codePoints().anyMatch { cp ->
            // Emoji ranges: Miscellaneous Symbols, Emoticons, Supplemental Symbols,
            // Enclosed Alphanumerics, Dingbats, flags, etc.
            cp in 0x2600..0x27BF ||   // Misc symbols & dingbats
            cp in 0x1F300..0x1FAFF || // Most emojis
            cp in 0x1F900..0x1F9FF || // Supplemental
            cp in 0x2300..0x23FF ||   // Misc technical
            cp in 0x25A0..0x25FF ||   // Geometric shapes
            cp in 0x2B00..0x2BFF      // Misc symbols & arrows
        }
    }

    private fun commitPendingWord() {
        if (wordBuffer.isEmpty()) return
        val ic = currentInputConnection
        val finalWord = SinhalaTransliterator.transliterate(wordBuffer.toString())
        // setComposingText("") removes the composing span WITHOUT committing it,
        // then commitText commits the final word exactly once.
        ic?.setComposingText("", 1)
        ic?.commitText(finalWord, 1)
        wordBuffer.clear()
        suggestions.value = emptyList()
    }

    /**
     * Called when the user taps a suggestion chip.
     * Replaces the current composing word with the selected suggestion.
     */
    private fun handleSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (currentLanguage.value == "si") {
            // setComposingText("") removes composing span WITHOUT committing it,
            // then commitText commits the suggestion exactly once.
            ic.setComposingText("", 1)
            ic.commitText(word, 1)
            wordBuffer.clear()
        } else {
            // Delete the already-committed English word, then commit the suggestion.
            val len = englishBuffer.length
            if (len > 0) ic.deleteSurroundingText(len, 0)
            ic.commitText(word, 1)
            englishBuffer.clear()
        }
        suggestions.value = emptyList()
    }

    /**
     * Generates word suggestions based on the current [wordBuffer].
     *
     * Sinhala mode  — the transliterated form is always the first suggestion.
     *                 Additional variants (e.g. with/without inherent-a) follow.
     * English mode  — Android's [android.view.textservice.SpellCheckerSession] is
     *                 used when available; a small built-in prefix list fills the
     *                 gap on devices/emulators that lack it.
     */
    private fun updateSuggestions() {
        val raw = if (currentLanguage.value == "si") wordBuffer.toString() else englishBuffer.toString()
        if (raw.isEmpty()) { suggestions.value = emptyList(); return }

        if (currentLanguage.value == "si") {
            // Primary: exact transliteration
            val primary = SinhalaTransliterator.transliterate(raw)
            val list = mutableListOf(primary)

            // Variant 1: append space suggestion (commit as-is)
            // Variant 2: try with trailing 'a' appended (fills inherent vowel)
            val withA = SinhalaTransliterator.transliterate("${raw}a")
            if (withA != primary) list.add(withA)

            // Variant 3: try capitalised first letter
            if (raw.length > 1) {
                val cap = SinhalaTransliterator.transliterate(raw[0].uppercaseChar() + raw.substring(1))
                if (cap != primary && cap != withA) list.add(cap)
            }

            suggestions.value = list.take(5)
        } else {
            // English: use Android TextServicesManager spell-checker for real suggestions
            val tsm = getSystemService(android.view.textservice.TextServicesManager::class.java)
            if (tsm != null) {
                try {
                    val locale = java.util.Locale.ENGLISH
                    val session = tsm.newSpellCheckerSession(null, locale, object :
                        android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener {
                        override fun onGetSuggestions(results: Array<out android.view.textservice.SuggestionsInfo>?) {
                            val words = mutableListOf<String>()
                            // First suggestion = the word typed (as-is)
                            if (raw.isNotEmpty()) words.add(raw)
                            results?.forEach { info ->
                                for (i in 0 until info.suggestionsCount) {
                                    val s = info.getSuggestionAt(i)
                                    if (s != raw && words.size < 5) words.add(s)
                                }
                            }
                            suggestions.value = words
                        }
                        override fun onGetSentenceSuggestions(results: Array<out android.view.textservice.SentenceSuggestionsInfo>?) {}
                    }, false)
                    session?.getSuggestions(
                        android.view.textservice.TextInfo(raw), 4
                    )
                    // Immediately show typed word while waiting for async results
                    if (suggestions.value.isEmpty()) suggestions.value = listOf(raw)
                } catch (_: Exception) {
                    suggestions.value = listOf(raw)
                }
            } else {
                suggestions.value = listOf(raw)
            }
        }
    }

    private fun maybeFeedback() {
        // Sound/vibration prefs are read lazily and cheaply each tap; DataStore
        // caches internally so this is not a meaningful perf concern for a keyboard.
        runBlocking {
            if (prefs.keyVibrateEnabled.first()) {
                val vibrator = getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
