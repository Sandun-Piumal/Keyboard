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
                        onStickerSend = { pathOrUri, isOwn, mimeType -> onStickerSelected(pathOrUri, isOwn, mimeType) },
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
                else { learnWord(englishBuffer.toString(), "en"); englishBuffer.clear(); suggestions.value = emptyList() }
                ic.commitText(" ", 1)
                // After space, check if previous char was sentence-ending punctuation
                updateAutoShift(ic)
            }
            "ENTER" -> {
                if (currentLanguage.value == "si") commitPendingWord()
                else { learnWord(englishBuffer.toString(), "en"); englishBuffer.clear(); suggestions.value = emptyList() }
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
        learnWord(finalWord, "si")
    }

    /**
     * User picked a suggestion from the strip (spell-checker word, Sinhala
     * transliteration variant, or a word learned from their own typing).
     * After committing it we also add a trailing space, since picking a
     * suggestion means the word is finished and the user will keep typing.
     */
    private fun handleSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (currentLanguage.value == "si") {
            ic.setComposingText("", 1)
            ic.commitText(word, 1)
            wordBuffer.clear()
            learnWord(word, "si")
        } else {
            val len = englishBuffer.length
            if (len > 0) ic.deleteSurroundingText(len, 0)
            val styled = com.spmods.sinkey.keyboard.FancyTextMapper.apply(word, cachedFancyTextStyle)
            ic.commitText(styled, 1)
            englishBuffer.clear()
            learnWord(word, "en")
        }
        // Auto-space after applying a suggestion.
        ic.commitText(" ", 1)
        suggestions.value = emptyList()
        updateAutoShift(ic)
    }

    /** Persist [word] into the personal dictionary (Room) so it can be suggested later. */
    private fun learnWord(word: String, language: String) {
        if (word.isBlank()) return
        serviceScope.launch { wordRepo.learn(word, language) }
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
                    if (created && boardStack.value.lastOrNull() == com.spmods.sinkey.keyboard.Board.STICKER_CREATE) {
                        boardStack.value = boardStack.value.dropLast(1)
                    }
                }
            }
        }
        val intent = android.content.Intent(this, com.spmods.sinkey.ime.StickerPickerActivity::class.java).apply {
            putExtra(com.spmods.sinkey.ime.StickerPickerActivity.EXTRA_MODE, com.spmods.sinkey.ime.StickerPickerActivity.MODE_IMAGE)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /**
     * Called from KeyboardView when the user taps a sticker to send it.
     * [isOwnSticker] distinguishes a user-created sticker (needs the
     * FileProvider content Uri built from its file path) from an external
     * WhatsApp/Telegram sticker (whose Uri from DocumentFile is already a
     * usable content:// Uri as-is).
     *
     * Shows a short toast when the receiving field doesn't support image
     * content (see sendSticker's doc comment) since there's no way to
     * gracefully degrade an image the way we can for styled text.
     */
    fun onStickerSelected(pathOrUri: String, isOwnSticker: Boolean, mimeType: String) {
        val uri = if (isOwnSticker) stickerContentUri(pathOrUri) else android.net.Uri.parse(pathOrUri)
        val sent = sendSticker(uri, mimeType)
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
     * [uri] must be readable by the receiving app. For user-created
     * stickers this is a FileProvider content:// Uri (see
     * StickerContentProvider) since the raw file:// path in app-private
     * storage isn't accessible outside this app's own process. For external
     * (WhatsApp/Telegram) stickers, [uri] is already the SAF content:// Uri
     * returned by DocumentFile, which the source app itself owns — read
     * permission for the *receiving* app still comes from the
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
            // Merge in personal-dictionary words the user has typed before that
            // start with the same rendered prefix (e.g. previously typed
            // Sinhala words matching what's being composed right now).
            fetchPersonalSuggestions(primary, "si", baseList = list)
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
     * Looks up the personal dictionary (Room, async) for words starting with
     * [prefix] and merges any new ones onto the end of the current suggestion
     * list once they arrive, without disturbing suggestions already shown
     * (spell-checker results / transliteration variants stay first).
     */
    private fun fetchPersonalSuggestions(prefix: String, language: String, baseList: List<String>) {
        if (prefix.isEmpty()) return
        serviceScope.launch {
            val learned = wordRepo.suggestionsFor(prefix, language, limit = 5)
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
