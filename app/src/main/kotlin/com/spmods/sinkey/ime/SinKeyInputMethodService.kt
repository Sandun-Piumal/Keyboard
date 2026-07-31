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
import com.spmods.sinkey.data.clipboard.ClipRepository
import com.spmods.sinkey.data.dictionary.WordRepository
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
    private lateinit var wordRepo: WordRepository
    private lateinit var clipRepo: ClipRepository
    private lateinit var stickerRepo: com.spmods.sinkey.data.sticker.StickerRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Listens system-wide for clipboard changes (not just copies made inside
    // SinKey) so the clipboard history board has something to show even when
    // the user copied text from another app. Registered in onCreate,
    // unregistered in onDestroy to avoid leaking the listener.
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

    private var wordBuffer = StringBuilder()
    private var englishBuffer = StringBuilder()
    // The most recently committed word and the language it was committed in,
    // used as context for next-word prediction (see learnWordWithContext /
    // updateSuggestions). Reset whenever the cursor moves or the field
    // changes so a prediction never carries over from unrelated text.
    private var lastCommittedWord: String = ""
    private var lastCommittedLanguage: String = ""
    // "si" = pure Sinhala, "en" = pure English, "mix" = default mode — types
    // Sinhala phonetically same as "si" (LANG_TOGGLE still switches to "en"
    // mid-message), but is visually distinguished by a black composing
    // underline instead of Sinhala's green one (see composingSpanFor).
    private var currentLanguage = mutableStateOf("mix")

    /** True when the buffer typing engine should run in Sinhala/phonetic mode ("si" or "mix"). */
    private fun isSinhalaTyping(): Boolean = currentLanguage.value != "en"
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
    // True only when the current ONE_SHOT came from the user tapping SHIFT
    // themselves, not from auto-capitalize at sentence/field start. Sinhala
    // mode needs this distinction since case there selects a different
    // letter rather than just styling one (see the isSinhalaTyping() branch
    // in handleKey's letter-key case).
    private var wasExplicitShift = false

    // Update banner dismiss state — deliberately in-memory only (NOT
    // DataStore/PreferencesManager), and deliberately at service level (not
    // remember{} inside the Composable) for the opposite reason boardStack
    // is hoisted here: boardStack needs to SURVIVE hide/show, but this flag
    // needs to be UNDONE on every hide/show. A dismissed update banner must
    // come back the next time the keyboard is reopened (per product
    // decision — dismiss is a "not right now", not a permanent opt-out),
    // so it's reset unconditionally in onStartInputView below, which fires
    // on every keyboard show including simple hide→show of the same field.
    private var dismissedUpdateVersionCode = mutableStateOf(0)

    // FIX #1 & #3: Cached prefs — read once on start, updated via coroutine.
    // Eliminates runBlocking on every key tap (was causing main-thread lag / ANR).
    // Also enables key sound which was previously unimplemented.
    private var cachedVibrateEnabled = false
    private var cachedSoundEnabled = true

    // Fancy-text style (TOOL_FONT) applied to committed ENGLISH text only —
    // see FancyTextMapper. Cached the same way as the feedback prefs above:
    // read once via a Flow collector so applying it on every keystroke never
    // blocks. Sinhala typing ignores this — there's no styled-Unicode
    // equivalent for Sinhala script, so it's applied only in the "en" branch
    // of handleKey/handleSuggestion below.
    private var cachedFancyTextStyle = com.spmods.sinkey.data.FancyTextStyle.NONE
    // Mix mode: when true, space/enter converts the typed word to Sinhala
    // (same as pure "si"); when false (default), it commits the raw typed
    // Latin text as-is unless the user picked a suggestion.
    private var cachedMixAutoSinhala = false

    // FIX #2: Single reusable SpellCheckerSession — created once, reused across
    // keystrokes. Previous code created a new session per keystroke, leaking OS
    // resources and causing memory growth over time.
    private var spellCheckerSession: android.view.textservice.SpellCheckerSession? = null

    // Tracks the raw Latin buffer last sent to the spell checker from mix
    // mode, so the async onGetSuggestions callback above knows what it's
    // answering (mix mode reuses wordBuffer, not englishBuffer, for typing).
    private var mixEnglishQuery: String = ""

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ImeLifecycleOwner()
        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Set ViewTree owners on the IME window's decorView so that Compose can
        // find them when onAttachedToWindow fires. The IME window exists from
        // onCreate onward, so this is the correct and earliest possible place.
        // Setting on decorView propagates to all children via the view tag system.
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            decor.setViewTreeViewModelStoreOwner(lifecycleOwner)
        }
        prefs = PreferencesManager(this)
        wordRepo = WordRepository(this)
        clipRepo = ClipRepository(this)
        stickerRepo = com.spmods.sinkey.data.sticker.StickerRepository(this)
        registerClipboardListener()

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
        serviceScope.launch {
            prefs.keyboardFont.collect { cachedFancyTextStyle = com.spmods.sinkey.data.FancyTextStyle.fromKey(it) }
        }
        serviceScope.launch {
            prefs.mixAutoSinhala.collect { cachedMixAutoSinhala = it }
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

    /**
     * Records every text copied anywhere on the device (not just inside
     * SinKey) into the persistent clipboard history, so TOOL_CLIPBOARD can
     * show more than "whatever is on the clipboard right now". Fires
     * immediately when registered if a clip already exists, and again on
     * every subsequent copy for the lifetime of the service.
     */
    private fun registerClipboardListener() {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java) ?: return
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this)?.toString()
                if (!text.isNullOrBlank()) {
                    serviceScope.launch { clipRepo.record(text) }
                }
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        clipboardListener = listener
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
                        // Pure English mode: ("en") — this callback owns the whole
                        // suggestion bar keyed off englishBuffer, as before.
                        if (currentLanguage.value == "en") {
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
                            return
                        }

                        // Mix mode: this callback is answering the extra English-side
                        // lookup fired from fetchEnglishSuggestionsForMix — merge its
                        // results onto the end of whatever Sinhala suggestions are
                        // already showing instead of replacing them. Bail out if the
                        // buffer has since moved on (cleared, or now a different word)
                        // so a slow async reply can't attach stale suggestions.
                        val raw = mixEnglishQuery
                        if (currentLanguage.value != "mix" || raw.isEmpty()) return
                        if (wordBuffer.toString() != raw) return
                        val englishWords = mutableListOf<String>()
                        englishWords.add(raw)
                        results?.forEach { info ->
                            for (i in 0 until info.suggestionsCount) {
                                val s = info.getSuggestionAt(i)
                                if (s != raw && englishWords.size < 3) englishWords.add(s)
                            }
                        }
                        suggestions.value = (suggestions.value + englishWords).distinct().take(6)
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
        // Return a fresh ImeComposeView every time. Android's setInputView()
        // will place it inside parentPanel. We override setInputView() below
        // to remove any previously attached view first, preventing the ghost
        // duplicate keyboard that appears when the IME window is re-shown.
        val composeView = ImeComposeView(this) {
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
                        onShiftStateChange = { shiftState.value = it },
                        dismissedUpdateVersionCode = dismissedUpdateVersionCode.value,
                        onDismissedUpdateVersionCodeChange = { dismissedUpdateVersionCode.value = it },
                        onStickerSend = { filePath, mimeType -> onStickerSelected(filePath, mimeType) },
                        onPickStickerImage = { pickImageForSticker() }
                    )
                }
        }

        return composeView
    }

    private var currentInputView: View? = null

    override fun setInputView(view: View) {
        // Before calling super (which does parentPanel.addView(view)), remove
        // the previously attached view from its parent. Without this step,
        // Android stacks the old and new views inside parentPanel producing
        // the ghost/duplicate keyboard visible on screen.
        currentInputView?.let { old ->
            (old.parent as? ViewGroup)?.removeView(old)
        }
        currentInputView = view
        super.setInputView(view)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Re-apply owners on every show in case the IME window was recreated.
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
        // Moving to a different field (or restarting the same one) means the
        // text before the cursor may no longer be what we last committed —
        // don't carry stale next-word context across the switch.
        lastCommittedWord = ""
        lastCommittedLanguage = ""

        // Update-banner dismissal is undone on every keyboard show (not
        // gated by `restarting` like the board-reset below) — see the field
        // comment on dismissedUpdateVersionCode for why this must fire on
        // every show, including simple hide→show of the same field.
        dismissedUpdateVersionCode.value = 0

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
        // Unregister the clipboard listener to avoid leaking it past this
        // service instance's lifetime.
        clipboardListener?.let { listener ->
            getSystemService(android.content.ClipboardManager::class.java)
                ?.removePrimaryClipChangedListener(listener)
        }
        clipboardListener = null
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
                        setComposingTextStyled(ic, renderStyledBuffer())
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
                if (isSinhalaTyping()) commitPendingWord()
                else { learnWord(englishBuffer.toString(), "en"); englishBuffer.clear() }
                ic.commitText(" ", 1)
                // After space, check if previous char was sentence-ending punctuation
                updateAutoShift(ic)
                // Word just finished — offer a next-word prediction instead of
                // leaving the suggestion bar empty.
                updateSuggestions()
            }
            "ENTER" -> {
                if (isSinhalaTyping()) commitPendingWord()
                else { learnWord(englishBuffer.toString(), "en"); englishBuffer.clear(); suggestions.value = emptyList() }

                val editorInfo = currentInputEditorInfo
                val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                val forceMultiline = editorInfo?.inputType
                    ?.and(android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

                val handledAsAction = action != EditorInfo.IME_ACTION_NONE &&
                    action != EditorInfo.IME_ACTION_UNSPECIFIED &&
                    !forceMultiline &&
                    ic.performEditorAction(action)

                if (!handledAsAction) {
                    ic.commitText("\n", 1)
                }
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
                wasExplicitShift = shiftState.value != ShiftState.OFF
            }
            "SHIFT_LOCK" -> {
                shiftState.value = if (shiftState.value == ShiftState.LOCKED) ShiftState.OFF else ShiftState.LOCKED
                wasExplicitShift = shiftState.value != ShiftState.OFF
            }
            "SYMBOLS_SHIFT", "EMOJI", "NUMPAD" -> { /* handled in KeyboardView */ }
            "TOOL_MIC" -> { sendDefaultEditorAction(true) }
            "TOOL_APPS", "TOOL_STICKER", "TOOL_FONT",
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
                currentLanguage.value = when (currentLanguage.value) {
                    "mix" -> "en"
                    "en"  -> "si"
                    else  -> "mix" // "si" -> "mix"
                }
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
                if (key.startsWith("PASTE_TEXT:")) {
                    // User picked an entry from the clipboard history board
                    // (KeyboardView's ClipboardHistoryView) — paste it at the
                    // cursor exactly like TOOL_CLIPBOARD used to, then finish
                    // any in-progress word/suggestions first.
                    val text = key.removePrefix("PASTE_TEXT:")
                    if (text.isNotEmpty()) {
                        commitPendingWord()
                        englishBuffer.clear()
                        suggestions.value = emptyList()
                        ic.commitText(text, 1)
                    }
                    return
                }
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
                } else if (isSinhalaTyping()) {
                    // Sinhala's phonetic scheme uses case to pick between
                    // real, distinct letters (lowercase n=න vs uppercase
                    // N=ණ, lowercase l=ල vs uppercase L=ළ, "sh"=ශ vs
                    // "Sh"=ෂ, "th"=ත vs "thh"=ථ) — it is never cosmetic,
                    // so auto-capitalize (ONE_SHOT at sentence/field start)
                    // must NOT apply here the way it does for English.
                    // Only an explicit, user-pressed Shift (which also
                    // sets ONE_SHOT, but via the SHIFT key itself) should
                    // uppercase a Sinhala key — and by the time a letter
                    // key is pressed we can no longer tell "auto" apart
                    // from "user pressed shift" using shiftState alone.
                    // wasExplicitShift tracks only shift presses that
                    // happened after the current word/field started, so
                    // auto-capitalize at start-of-sentence is ignored
                    // while a deliberate double-tap-to-Shift still works.
                    val typed = if (shiftState.value != ShiftState.OFF && wasExplicitShift) key.uppercase() else key.lowercase()
                    wordBuffer.append(typed)
                    setComposingTextStyled(ic, renderStyledBuffer())
                    updateSuggestions()
                    // Consume one-shot shift after first Sinhala letter
                    if (shiftState.value == ShiftState.ONE_SHOT) { shiftState.value = ShiftState.OFF; wasExplicitShift = false }
                } else {
                    // Apply shift to English letter
                    val typed = if (shiftState.value != ShiftState.OFF) key.uppercase() else key.lowercase()
                    englishBuffer.append(typed)
                    val styled = com.spmods.sinkey.keyboard.FancyTextMapper.apply(typed, cachedFancyTextStyle)
                    ic.commitText(styled, 1)
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

    /**
     * Composing-preview text for the current buffer. Pure Sinhala mode shows
     * the live transliteration ("මම"); mix mode shows the raw Latin text
     * exactly as typed ("mama"), since mix mode types English-looking text
     * on screen and only offers the Sinhala reading via the suggestion bar.
     *
     * For pure Sinhala mode specifically, the first 1–2 keys of a fresh word
     * prefer the empirically-weighted [com.spmods.sinkey.keyboard.SinhalaCandidateMap]
     * (its #1 ranked candidate) over the deterministic rule-based
     * [SinhalaTransliterator], since the weighted map better reflects what
     * users actually mean for short, ambiguous prefixes (e.g. "s" alone is
     * more often ස than ශ). Once the buffer grows past what the map covers,
     * or for mix mode's own English-Latin preview, this falls back to the
     * rule-based transliterator as before.
     */
    private fun renderBuffer(): String {
        if (currentLanguage.value == "mix") return wordBuffer.toString()
        val raw = wordBuffer.toString()
        if (raw.length <= 2) {
            com.spmods.sinkey.keyboard.SinhalaCandidateMap.topCandidateFor(raw)?.let { return it }
        }
        return SinhalaTransliterator.transliterate(raw)
    }

    /**
     * Same as [renderBuffer] but with fancy-font styling applied when in mix
     * mode (which types plain Latin text and should match pure English's
     * font styling); pure Sinhala mode's transliteration is returned as-is.
     */
    private fun renderStyledBuffer(): String {
        val raw = renderBuffer()
        return if (currentLanguage.value == "mix")
            com.spmods.sinkey.keyboard.FancyTextMapper.apply(raw, cachedFancyTextStyle)
        else raw
    }

    // Sinhala composing underline — matches the app's DeshGreen accent.
    private val sinhalaUnderlineColor = android.graphics.Color.rgb(0x6E, 0x9A, 0x65)
    // Mix-mode composing underline — plain black, so it's visually distinct
    // from pure Sinhala mode's green while typing.
    private val mixUnderlineColor = android.graphics.Color.BLACK

    /** CharacterStyle that draws the composing-text underline in a fixed [color]. */
    private class ColoredUnderlineSpan(private val color: Int) : android.text.style.CharacterStyle() {
        override fun updateDrawState(tp: android.text.TextPaint) {
            tp.isUnderlineText = true
            tp.underlineColor = color
            tp.underlineThickness = 4f
        }
    }

    /**
     * Sends [text] to the editor as composing text with an underline colored
     * according to the current language mode: green for pure Sinhala ("si"),
     * black for the default mix mode, and the system default for plain
     * English (no custom span needed there).
     */
    private fun setComposingTextStyled(ic: android.view.inputmethod.InputConnection, text: String) {
        val color = when (currentLanguage.value) {
            "si" -> sinhalaUnderlineColor
            "mix" -> mixUnderlineColor
            else -> null
        }
        if (color == null) {
            ic.setComposingText(text, 1)
        } else {
            val spanned = android.text.SpannableString(text)
            spanned.setSpan(
                ColoredUnderlineSpan(color),
                0, text.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ic.setComposingText(spanned, 1)
        }
    }

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
        val raw = wordBuffer.toString()
        // Mix mode: commit the raw Latin text as typed unless the user has
        // turned on "auto-convert to Sinhala" in settings — pure "si" mode
        // always converts, same as before.
        val convertToSinhala = currentLanguage.value != "mix" || cachedMixAutoSinhala
        val finalWord = when {
            convertToSinhala -> SinhalaTransliterator.transliterate(raw)
            else -> com.spmods.sinkey.keyboard.FancyTextMapper.apply(raw, cachedFancyTextStyle)
        }
        ic?.setComposingText("", 1)
        ic?.commitText(finalWord, 1)
        wordBuffer.clear()
        suggestions.value = emptyList()
        // Learn the plain (unstyled) word, not the fancy-font glyphs, so the
        // personal dictionary and future suggestions stay in normal text.
        learnWord(if (convertToSinhala) finalWord else raw, if (convertToSinhala) "si" else "en")
    }

    /**
     * User picked a suggestion from the strip (spell-checker word, Sinhala
     * transliteration variant, or a word learned from their own typing).
     * After committing it we also add a trailing space, since picking a
     * suggestion means the word is finished and the user will keep typing.
     */
    private fun handleSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (isSinhalaTyping()) {
            // In mix mode the suggestion bar can hold both a Sinhala rendering
            // and the raw-Latin English reading of the same buffer (see
            // fetchEnglishSuggestionsForMix) — style and learn each into its
            // matching path rather than always treating it as Sinhala.
            val pickedEnglish = currentLanguage.value == "mix" &&
                word.equals(mixEnglishQuery, ignoreCase = true)
            val toCommit = if (pickedEnglish)
                com.spmods.sinkey.keyboard.FancyTextMapper.apply(word, cachedFancyTextStyle)
            else word
            ic.setComposingText("", 1)
            ic.commitText(toCommit, 1)
            wordBuffer.clear()
            learnWord(word, if (pickedEnglish) "en" else "si")
        } else {
            // Delete the length of what's actually on screen (the styled/
            // fancy text), not the plain-text buffer length — fancy fonts
            // map many letters to surrogate-pair Unicode glyphs (2 UTF-16
            // units each), so the two lengths can differ and a raw-length
            // delete leaves stray fancy characters behind.
            val committedStyled = com.spmods.sinkey.keyboard.FancyTextMapper.apply(
                englishBuffer.toString(), cachedFancyTextStyle
            )
            val len = committedStyled.length
            if (len > 0) ic.deleteSurroundingText(len, 0)
            val styled = com.spmods.sinkey.keyboard.FancyTextMapper.apply(word, cachedFancyTextStyle)
            ic.commitText(styled, 1)
            englishBuffer.clear()
            learnWord(word, "en")
        }
        // Auto-space after applying a suggestion.
        ic.commitText(" ", 1)
        updateAutoShift(ic)
        // Word just finished — offer a next-word prediction instead of
        // leaving the suggestion bar empty.
        updateSuggestions()
    }

    /**
     * Persist [word] into the personal dictionary (Room) so it can be
     * suggested later. Also learns the (previous word -> this word) pair
     * for next-word prediction, provided the previous word was committed
     * in the same language — mixing languages mid-pair would only ever
     * produce noise, never a useful prediction. The pair is learned before
     * [lastCommittedWord] is updated, so it always reflects "what came
     * before this word", not itself.
     */
    private fun learnWord(word: String, language: String) {
        if (word.isBlank()) return
        val prev = lastCommittedWord
        val prevLanguage = lastCommittedLanguage
        serviceScope.launch {
            wordRepo.learn(word, language)
            if (prev.isNotBlank() && prevLanguage == language) {
                wordRepo.learnBigram(prev, word, language)
            }
        }
        lastCommittedWord = word
        lastCommittedLanguage = language
    }

    /**
     * Converts an app-private sticker file path (StickerEntity.filePath)
     * into a content:// Uri other apps can actually read, via the
     * FileProvider declared in the manifest. External (WhatsApp/Telegram)
     * stickers never go through this — they already have their own
     * content:// Uri from DocumentFile.
     */
    private fun stickerContentUri(filePath: String): android.net.Uri =
        androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.stickerprovider",
            java.io.File(filePath)
        )

    /**
     * Launches the system gallery picker (via StickerPickerActivity, since
     * this Service can't host an ActivityResultLauncher itself — see that
     * class's doc comment) for Board.STICKER_CREATE's "Image Sticker"
     * option. On completion the picked image is decoded, downscaled, and
     * saved as a new sticker by StickerRepository.createFromImage.
     */
    fun pickImageForSticker() {
        com.spmods.sinkey.ime.StickerPickerActivity.onImagePicked = { uri ->
            if (uri != null) {
                serviceScope.launch {
                    val created = stickerRepo.createFromImage(uri)
                    if (!created) {
                        android.widget.Toast.makeText(
                            this@SinKeyInputMethodService,
                            "Couldn't read that image",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else if (boardStack.value.lastOrNull() == com.spmods.sinkey.keyboard.Board.STICKER_CREATE) {
                        boardStack.value = boardStack.value.dropLast(1)
                    }
                }
            }
        }
        val intent = android.content.Intent(this, com.spmods.sinkey.ime.StickerPickerActivity::class.java).apply {
            putExtra(com.spmods.sinkey.ime.StickerPickerActivity.EXTRA_MODE, com.spmods.sinkey.ime.StickerPickerActivity.MODE_IMAGE)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Most likely cause: the OEM/platform blocked a background
            // activity start from this IME process. "Current input method"
            // is documented as an exception to that restriction, but some
            // OEM skins (or a moment where the IME briefly isn't considered
            // foreground) can still reject it — surfacing this beats a
            // silent no-op, which otherwise looks exactly like "does nothing".
            android.widget.Toast.makeText(
                this,
                "Couldn't open the picker — try again",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Called from KeyboardView when the user taps a sticker and confirms
     * sending it. [filePath] is the app-private PNG sticker file path
     * (StickerEntity.filePath).
     *
     * Sends the sticker's WhatsApp-ready WebP sibling file (see
     * StickerFileStore.writeWhatsAppWebp) via commitContent with mime type
     * image/webp, rather than the PNG. Chat apps that implement the sticker
     * side of the Commit Content API (WhatsApp, Telegram, etc.) key off the
     * WebP format + exact 512x512 size + sub-100KB filesize to decide
     * whether to treat incoming content as a real sticker versus a generic
     * photo attachment; a PNG (or an oversized/wrong-size WebP) gets treated
     * as a photo, which triggers that app's own send/preview sheet instead
     * of delivering instantly. No sticker-pack registration is involved —
     * this is a plain one-off commitContent per send, same as SinKey's own
     * emoji/text.
     */
    fun onStickerSelected(filePath: String, mimeType: String) {
        val webpPath = com.spmods.sinkey.data.sticker.StickerFileStore.webpPathFor(filePath)
        if (!java.io.File(webpPath).exists()) {
            // Sticker predates WebP export — backfill synchronously (a single
            // small bitmap decode/encode) so this send still goes out as a
            // proper sticker instead of falling back to the raw PNG.
            com.spmods.sinkey.data.sticker.StickerFileStore.backfillWebp(filePath)
        }
        val webpExists = java.io.File(webpPath).exists()
        val sendPath = if (webpExists) webpPath else filePath
        val sendMime = if (webpExists) "image/webp" else mimeType

        val sent = sendSticker(stickerContentUri(sendPath), sendMime)
        if (!sent) {
            android.widget.Toast.makeText(
                this,
                "This field doesn't support stickers",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Sends a sticker image to the currently focused field via the
     * InputConnection#commitContent (Commit Content API, N+). This is the
     * same mechanism Gboard uses for its own stickers/GIFs — the receiving
     * app (WhatsApp, Telegram, Messages, etc.) must opt in by declaring
     * EditorInfo.EXTRA_CONTENT_MIME_TYPES on its input field, which chat
     * apps generally do; plain single-line text fields (URL bars, search
     * boxes) typically don't, and commitContent then simply returns false.
     *
     * [uri] must be readable by the receiving app — always a FileProvider
     * content:// Uri here (see stickerContentUri), since the raw file://
     * path in app-private storage isn't accessible outside this app's own
     * process. Read permission for the *receiving* app comes from the
     * FLAG_GRANT_READ_URI_PERMISSION passed below, which the platform
     * upgrades into a one-off grant scoped to that receiving package.
     *
     * Returns true if the receiving app accepted the content, false if it
     * doesn't support commitContent (caller should fall back — e.g. no-op
     * with a toast — since there is no universal fallback for images the
     * way there is for text).
     */
    private fun sendSticker(uri: android.net.Uri, mimeType: String): Boolean {
        val ic = currentInputConnection ?: return false
        val editorInfo = currentInputEditorInfo ?: return false

        val supportedMimeTypes = androidx.core.view.inputmethod.EditorInfoCompat
            .getContentMimeTypes(editorInfo)
        val accepts = supportedMimeTypes.any { pattern ->
            android.content.ClipDescription.compareMimeTypes(mimeType, pattern)
        }
        if (!accepts) return false

        grantUriPermission(
            editorInfo.packageName,
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val contentInfo = androidx.core.view.inputmethod.InputContentInfoCompat(
            uri,
            android.content.ClipDescription("sticker", arrayOf(mimeType)),
            null
        )

        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        } else 0

        return androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
            ic, editorInfo, contentInfo, flags, null
        )
    }

    private fun updateSuggestions() {
        val raw = if (isSinhalaTyping()) wordBuffer.toString() else englishBuffer.toString()
        if (raw.isEmpty()) {
            // Nothing typed yet for the next word — if we know what word
            // came before the cursor, offer next-word predictions instead of
            // clearing the bar. isSinhalaTyping() picks the language bucket
            // to predict in ("si" also covers mix-mode Sinhala typing), which
            // matches how learnWord records the pair in the first place.
            val predictLanguage = if (isSinhalaTyping()) "si" else "en"
            if (lastCommittedWord.isNotBlank() && lastCommittedLanguage == predictLanguage) {
                fetchNextWordSuggestions(lastCommittedWord, predictLanguage)
            } else {
                suggestions.value = emptyList()
            }
            return
        }

        if (isSinhalaTyping()) {
            val primary = SinhalaTransliterator.transliterate(raw)
            val list = mutableListOf<String>()

            // For the first 1–2 keys of a word, prefer the empirically
            // weighted candidates (top 3–5, already ranked best-first) over
            // the deterministic rule-based variants below — they reflect
            // real usage frequency, so e.g. typing "s" surfaces ස before
            // the rarer ශ/ෂ readings a purely rule-based pass would give
            // equal footing.
            val weighted = com.spmods.sinkey.keyboard.SinhalaCandidateMap.candidatesFor(raw)
            if (weighted.isNotEmpty()) {
                list.addAll(weighted.take(5))
            }
            if (!list.contains(primary)) list.add(primary)
            val withA = SinhalaTransliterator.transliterate("${raw}a")
            if (!list.contains(withA) && list.size < 5) list.add(withA)
            if (raw.length > 1) {
                val cap = SinhalaTransliterator.transliterate(raw[0].uppercaseChar() + raw.substring(1))
                if (!list.contains(cap) && list.size < 5) list.add(cap)
            }
            suggestions.value = list.take(5)
            // Merge in personal-dictionary words the user has typed before that
            // start with the same rendered prefix (e.g. previously typed
            // Sinhala words matching what's being composed right now).
            fetchPersonalSuggestions(primary, "si", baseList = list)

            // Mix mode only: also surface the raw Latin buffer as a plain-English
            // suggestion (and its spell-checker completions) alongside the Sinhala
            // ones above, since the user may actually be typing an English word
            // while in the default mix mode rather than Singlish.
            if (currentLanguage.value == "mix") {
                fetchEnglishSuggestionsForMix(raw)
            }
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
            fetchPersonalSuggestions(raw, "en", baseList = listOf(raw))
        }
    }

    /**
     * Mix mode only: asks the English spell checker about the raw Latin text
     * the user is typing (e.g. "office"), so its English reading can show up
     * in the suggestion bar next to the Sinhala transliteration ("ඔෆිස්")
     * without the user needing to switch modes first. Also merges the
     * learned personal English dictionary immediately (synchronous data
     * already in memory via wordRepo below), then the async spell-checker
     * results arrive via the shared session's onGetSuggestions.
     */
    private fun fetchEnglishSuggestionsForMix(raw: String) {
        mixEnglishQuery = raw
        val session = spellCheckerSession
        if (session != null) {
            try {
                session.getSuggestions(android.view.textservice.TextInfo(raw), 3)
            } catch (e: Exception) {
                android.util.Log.w("SinKey", "getSuggestions (mix) failed", e)
            }
        } else {
            // No spell-checker available — still surface the raw word itself.
            suggestions.value = (suggestions.value + raw).distinct().take(6)
        }
    }

    /**
     * Looks up the personal dictionary (Room, async) for words starting with
     * [prefix] and merges any new ones onto the end of the current suggestion
     * list once they arrive, without disturbing suggestions already shown
     * (spell-checker results / transliteration variants stay first).
     */
    /**
     * Predicts the word likely to follow [previousWord] (the last word
     * committed) and shows it in the suggestion bar before the user has
     * typed anything of the next word. Async like [fetchPersonalSuggestions]
     * since it's a Room query; shows nothing while waiting rather than
     * flashing stale suggestions, since there's no typed text to fall back to.
     */
    private fun fetchNextWordSuggestions(previousWord: String, language: String) {
        suggestions.value = emptyList()
        serviceScope.launch {
            val predicted = wordRepo.nextWordSuggestions(previousWord, language, limit = 3)
            // Guard against a stale async reply landing after the user has
            // since started typing the next word or moved on entirely.
            val stillRelevant = lastCommittedWord == previousWord &&
                (if (isSinhalaTyping()) wordBuffer else englishBuffer).isEmpty()
            if (stillRelevant && predicted.isNotEmpty()) {
                suggestions.value = predicted
            }
        }
    }

    private fun fetchPersonalSuggestions(prefix: String, language: String, baseList: List<String>) {
        if (prefix.isEmpty()) return
        serviceScope.launch {
            val learned = if (language == "si") {
                wordRepo.fuzzySuggestionsFor(prefix, language, limit = 5)
            } else {
                wordRepo.suggestionsFor(prefix, language, limit = 5)
            }
            if (learned.isEmpty()) return@launch
            val merged = (suggestions.value.ifEmpty { baseList } + learned).distinct().take(5)
            suggestions.value = merged
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
