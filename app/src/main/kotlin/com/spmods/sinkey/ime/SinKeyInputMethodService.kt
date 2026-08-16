package com.spmods.sinkey.ime

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.spmods.sinkey.R
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
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

// Caps how many distinct words a single clipboard copy can add to the
// personal dictionary in one go — see learnWordsFromClipboard(). Keeps one
// large paste from flooding the dictionary; a genuine "copied a couple of
// words" case is well under this.
private const val MAX_CLIPBOARD_WORDS_PER_COPY = 12

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
    // Set by reseedSuggestionsForWordAtCursor when the cursor lands inside/
    // after an EXISTING word already sitting in the field as plain text
    // (the user tapped back into something already typed) — holds exactly
    // that on-screen word so the next commitPendingWord()/
    // maybeAutocorrectAndCommitSpace() knows to delete it before committing
    // the (possibly-corrected) word instead of inserting a second copy
    // beside it. null in the ordinary case (word being typed fresh, never
    // resumed), so those commit paths behave exactly as before. Cleared
    // after being consumed, and any time the buffers themselves are cleared
    // for an unrelated reason, so it can never apply to a different word
    // than the one it was captured for.
    private var resumedWordBeforeCursor: String? = null
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
    // Set right after an autocorrect silently swaps what the user typed for
    // a dictionary/spell-checker word at SPACE — holds the original typed
    // word so the "Undo" chip (rendered by KeyboardView whenever this is
    // non-null, see AutocorrectUndo) can put it back if tapped. Cleared on
    // the next keystroke/suggestion tap/field switch — see clearAutocorrectUndo().
    private var autocorrectUndo = mutableStateOf<AutocorrectUndo?>(null)
    private var currentInputTypeState = mutableStateOf(0)

    // Board state lives at service level — NOT inside the Composable — so it
    // survives keyboard hide/show cycles. If held in remember{} it resets to
    // MAIN every time the user dismisses and reopens the keyboard.
    private var boardStack = mutableStateOf(listOf(Board.MAIN))

    // Set right before pushing Board.STICKER_EDIT (via pickImageForSticker's
    // callback) and read by KeyboardView to render StickerImageEditorView's
    // preview. Hoisted here rather than kept as remember{} state inside
    // KeyboardView for the same reason boardStack is: it must survive the
    // keyboard's hide/show recomposition cycles while the editor is open.
    private var pendingStickerImagePath = mutableStateOf<String?>(null)

    // ── Translate row state (TOOL_TRANSLATE) ──────────────────────────
    // ── Translate row state (TOOL_TRANSLATE) ──────────────────────────
    // Hoisted at service level for the same reason as boardStack above —
    // must survive hide/show while the row is open.
    //
    // DESIGN: keys typed while the row is open are NOT intercepted — they
    // go through the exact same pipeline as normal typing (see handleKey),
    // so Sinhala transliteration, autocorrect, suggestions etc. all keep
    // working exactly as if the translate row weren't open, and the
    // user's own words are what actually appear in the real target field
    // as they type. The translate row's job is purely to *watch* what
    // ends up in the field since it was opened, translate that, and show
    // the translated result in the row above — a live preview strip, not
    // a separate typing surface. This matters because the alternative
    // (giving the row its own local text buffer, intercepting keys before
    // they reach handleKey) would require reimplementing Sinhala
    // transliteration/autocorrect from scratch for that buffer, since the
    // real engine (wordBuffer + SinhalaTransliterator, see updateSuggestions)
    // is tightly coupled to writing directly into the real InputConnection.
    private var isTranslateMode = mutableStateOf(false)
    private var translateSourceLang = mutableStateOf("en")
    private var translateTargetLang = mutableStateOf("si")
    // Snapshot of getTextBeforeCursor() at the moment translate mode was
    // opened — the anchor point. What's currently "the translate source
    // text" is whatever the field contains from this offset to the cursor
    // now, read fresh on every keystroke via refreshTranslateSourceFromField().
    private var translateAnchorText = ""
    private var translateSourceText = mutableStateOf("")
    private var translateResultText = mutableStateOf("")
    private var isTranslating = mutableStateOf(false)
    // Bumped on every source-text/language change so a late-arriving
    // translation reply for stale input can recognize itself as stale and
    // discard itself — same staleness-guard idea as mixEnglishRequestId.
    private var translateRequestId = 0L
    private var translateJob: kotlinx.coroutines.Job? = null

    // Shift has 3 states: OFF, ONE_SHOT (next letter only), LOCKED (caps lock).
    // Stored at service level so it survives hide/show cycles.
    // AUTO-SHIFT: enabled at sentence start (after . ! ? or at field open).
    enum class ShiftState { OFF, ONE_SHOT, LOCKED }

    /**
     * What a silent autocorrect swapped, so the undo chip can put it back
     * exactly: [correctedWord] is what's currently committed in the field
     * (what tapping undo needs to delete), [originalTyped] is what the user
     * actually typed (what undo re-commits), and [hadTrailingSpace] records
     * whether the autocorrect's own auto-space-after-word should be undone
     * too — reverting should restore the exact text state from right before
     * the correction, not leave an extra space autocorrect added.
     */
    data class AutocorrectUndo(
        val originalTyped: String,
        val correctedWord: String,
        val hadTrailingSpace: Boolean
    )
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

    // BUG FIX: mixEnglishQuery alone isn't enough to tell replies apart —
    // it's overwritten on every keystroke, so if a fast typist fires two
    // spell-check requests before the first reply arrives, and the buffer
    // later returns to a value equal to the *older* request's query (e.g.
    // typing then backspacing back to the same text), the stale reply could
    // pass the "wordBuffer.toString() == raw" check and get applied even
    // though a newer, still-pending request is what should actually answer
    // for that text. A monotonically increasing token disambiguates: only
    // the reply matching the most recently issued request is ever applied.
    private var mixEnglishRequestId: Long = 0

    // Holds the Sinhala/transliteration side of the current suggestion list
    // separately from the async English (mix mode) results, so the two
    // async sources (Room personal-dictionary lookup + spell-checker) can
    // each update their own half and be recombined, instead of one write
    // clobbering whatever the other already placed into suggestions.value —
    // previously the mix-mode spell-checker callback did
    // `suggestions.value = (suggestions.value + englishWords)...`, which
    // silently ate the other source's results whenever it ran first.
    private var mixSinhalaSuggestions: List<String> = emptyList()
    private var mixEnglishSuggestions: List<String> = emptyList()

    /** Recombines the two mix-mode suggestion sources into a single deduped list. */
    private fun recomputeMixSuggestions() {
        suggestions.value = (mixSinhalaSuggestions + mixEnglishSuggestions).distinct().take(6)
    }

    /**
     * Clears the suggestion strip AND the mix-mode buckets behind it, and
     * invalidates any in-flight mix English request. Use this instead of
     * `suggestions.value = emptyList()` directly at any point where typing
     * has moved on (word committed, buffer cleared, field/language
     * switched, etc.) — BUG FIX: previously only suggestions.value itself
     * was cleared at these sites, leaving mixSinhalaSuggestions /
     * mixEnglishSuggestions holding stale data that a subsequent
     * recomputeMixSuggestions() call (e.g. a late-arriving async reply
     * that still passes its own staleness checks) could resurrect into a
     * suggestion for the wrong word.
     */
    private fun clearSuggestions() {
        suggestions.value = emptyList()
        mixSinhalaSuggestions = emptyList()
        mixEnglishSuggestions = emptyList()
        mixEnglishQuery = ""
        mixEnglishRequestId += 1
    }

    // Tracks where WE expect the cursor to be after our own edits (typing,
    // backspace, committing a suggestion, etc.) — see onUpdateSelection.
    // -1 means "unknown / just switched fields", which suppresses the first
    // check after onStartInputView so that call doesn't spuriously look like
    // an external cursor jump.
    private var expectedCursorPosition = -1

    // BUG FIX (type-then-instantly-erase): syncExpectedCursorPosition() used
    // to read the cursor back via ic.getExtractedText() *synchronously*,
    // right after sending setComposingText()/commitText(). But many editors
    // (WebViews, some messaging apps, some custom EditTexts) don't apply a
    // composing edit and report it back through getExtractedText()
    // synchronously — the real update arrives later, asynchronously, as its
    // own onUpdateSelection() callback. That race meant
    // getExtractedText() sometimes still reflected the *pre-edit* cursor
    // position at the moment we read it, so expectedCursorPosition got set
    // to a stale value. When the real onUpdateSelection() for our own edit
    // then arrived with the correct (different) newSelEnd, it no longer
    // matched expectedCursorPosition, so the edit was misclassified as an
    // "external" cursor jump — and the composing text we'd just set was
    // immediately finished/cleared. Net effect: a letter appears for an
    // instant and then vanishes.
    //
    // Fix: instead of trying to predict/read back the resulting position
    // ourselves, just count how many onUpdateSelection() calls we're
    // expecting as a direct result of edits we made. Every call to
    // syncExpectedCursorPosition() increments this; every onUpdateSelection()
    // callback consumes one (if any are pending) before deciding whether a
    // move looks external. This is race-proof because it doesn't depend on
    // reading the cursor position back at all — only on our own edit having
    // been sent before the resulting callback fires, which is guaranteed by
    // InputConnection ordering.
    private var pendingSelfEdits = 0

    override fun onEvaluateFullscreenMode(): Boolean = false

    // REQUIRED companion to the MATCH_PARENT window-height experiment
    // above (see the comment on composeView.layoutParams in
    // onCreateInputView). A MATCH_PARENT-height IME window covers the
    // WHOLE screen from Android's perspective, not just the visible
    // keyboard strip at the bottom — without telling Android which part
    // of that window is actually "the keyboard", the host app (WhatsApp)
    // would stop receiving touches on its own content above the keyboard,
    // because our now-full-screen window would intercept them first. This
    // did not need to exist before because a WRAP_CONTENT-height window
    // was automatically only as tall as its content, so Android already
    // knew its bounds equalled the visible keyboard. onComputeInsets is
    // the API InputMethodService expects apps to use for exactly this:
    // it reports where the actual usable/touchable IME content starts,
    // measured from the top of our window.
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val windowHeight = window?.window?.decorView?.height ?: 0
        // contentTopInsets / visibleTopInsets are both measured as an
        // absolute Y coordinate within our window, not a height — so we
        // need "window height minus the keyboard's own measured height",
        // not the keyboard's height by itself. If the compose content
        // hasn't been measured yet (e.g. very first frame, or hidden via
        // hostWindowFocused), fall back to reporting the full window as
        // content, matching the platform default so nothing regresses
        // before our first layout pass / while content is intentionally
        // blank.
        val keyboardTop = if (keyboardContentHeightPx > 0) {
            (windowHeight - keyboardContentHeightPx).coerceAtLeast(0)
        } else {
            0
        }
        outInsets.contentTopInsets = keyboardTop
        outInsets.visibleTopInsets = keyboardTop
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

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

        // Loads the bundled base word lists (common English/Sinhala
        // vocabulary — see DictionarySeeder) into the personal dictionary
        // once, mainly so gesture typing (GestureWordMatcher) has a real
        // vocabulary to match against from a fresh install rather than
        // just whatever's been typed so far. Runs off the main thread and
        // is a near-instant no-op on every subsequent start (see
        // DictionarySeeder.seedIfNeeded's version check), so it's safe to
        // fire-and-forget here rather than gating keyboard startup on it.
        serviceScope.launch { wordRepo.seedBaseDictionaryIfNeeded() }

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
                    learnWordsFromClipboard(text)
                }
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        clipboardListener = listener
    }

    /**
     * Feeds real words the user copied anywhere on the device — not just
     * text typed through this keyboard — into the same personal dictionary
     * ordinary typing builds, so those words start showing up as
     * suggestions the next time their prefix is typed. This is the practical
     * substitute for reading the device's actual search/browsing/message
     * history, which a third-party keyboard has no permission to access at
     * all (Android's sandboxing has no exception for keyboard apps) —
     * clipboard content is the one cross-app source of "words the user
     * cares about" this app can legitimately see.
     *
     * Deliberately calls wordRepo.learn() directly per word instead of
     * routing through learnWord() (used for actually-typed words): learnWord
     * also updates lastCommittedWord/lastCommittedLanguage for next-word
     * bigram prediction, which must only reflect the live typing flow, not
     * a block of pasted text that has no relationship to whatever the user
     * types next.
     *
     * Deliberately conservative about what counts as a "word" worth
     * learning — clipboard content is far noisier than typed text (whole
     * paragraphs, URLs, code, phone numbers, receipts, OTP codes, etc.), so
     * this only learns short, clean, letters-only tokens, capped per copy so
     * one big paste can't flood the dictionary with junk in a single event.
     */
    private fun learnWordsFromClipboard(text: String) {
        // Skip anything link/code/structured-data-shaped outright rather
        // than trying to salvage individual words out of it — a URL's path
        // segments or a JSON blob's keys aren't real vocabulary.
        if (text.contains("://") || text.contains("@") ||
            text.contains("{") || text.contains("<")
        ) return
        // A long paste is much more likely to be an article/message body
        // than a short list of words worth learning individually; a real
        // "I copied a couple of words" case is comfortably under this.
        if (text.length > 300) return

        val tokens = text.split(Regex("[\\s,.;:!?\"'()\\[\\]{}/\\\\|]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_CLIPBOARD_WORDS_PER_COPY)

        if (tokens.isEmpty()) return

        serviceScope.launch {
            for (token in tokens) {
                val language = detectWordScript(token) ?: continue
                wordRepo.learn(token, language)
            }
        }
    }

    /**
     * Classifies [word] as "si" (Sinhala), "en" (Latin), or null (neither —
     * e.g. a number, emoji, or mixed-script token, none of which are useful
     * dictionary entries). Checked by Unicode block rather than by reusing
     * isSinhalaTyping()/currentLanguage, since those describe which script
     * the keyboard is *currently configured to type in* — completely
     * unrelated to what script a piece of copied text happens to be in.
     */
    private fun detectWordScript(word: String): String? {
        if (word.length > 40 || word.length < 2) return null
        var sinhalaCount = 0
        var latinCount = 0
        for (ch in word) {
            when {
                ch.code in 0x0D80..0x0DFF -> sinhalaCount++
                ch.isLetter() && ch.code < 0x0250 -> latinCount++ // Basic Latin + Latin-1 Supplement + Latin Extended-A/B letters
                ch.isLetter() -> return null // some other script — not one we suggest in
                ch.isDigit() -> return null  // e.g. "2024", order numbers — not vocabulary
            }
        }
        return when {
            sinhalaCount > 0 && latinCount == 0 -> "si"
            latinCount > 0 && sinhalaCount == 0 -> "en"
            else -> null // mixed script or no letters at all
        }
    }

    // Tracks the most recent pure-English spell-check verdict for
    // englishBuffer's current word — set in initSpellCheckerSession's
    // onGetSuggestions, read by maybeAutocorrectAndCommitSpace() at SPACE.
    // null means "no verdict yet for the current word" (spell checker
    // hasn't replied, or its reply was for a since-changed word) — treated
    // as "don't autocorrect", since acting on a stale or missing verdict
    // risks correcting the wrong word entirely.
    private var lastSpellCheckVerdict: SpellCheckVerdict? = null

    /**
     * [typedWord] is the word this verdict is *for* — guards against a slow
     * async reply landing after englishBuffer has moved on to a different
     * word (same staleness concern as elsewhere in this file, e.g.
     * fetchNextWordSuggestions). [looksLikeTypo] mirrors the spell
     * checker's own RESULT_ATTR_LOOKS_LIKE_TYPO flag — autocorrect only
     * acts when the platform's own spell checker, not just our heuristics,
     * considers the word wrong. [topCorrection] is its first suggested
     * replacement, if any.
     */
    private data class SpellCheckVerdict(
        val typedWord: String,
        val looksLikeTypo: Boolean,
        val topCorrection: String?
    )

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
                            var looksLikeTypo = false
                            var topCorrection: String? = null
                            results?.forEach { info ->
                                if (info.suggestionsCount > 0) {
                                    looksLikeTypo = (info.suggestionsAttributes and
                                        android.view.textservice.SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
                                }
                                for (i in 0 until info.suggestionsCount) {
                                    val s = info.getSuggestionAt(i)
                                    if (topCorrection == null && s != raw) topCorrection = s
                                    if (s != raw && words.size < 5) words.add(s)
                                }
                            }
                            if (words.isNotEmpty()) suggestions.value = words
                            // Recorded even when raw is empty (verdict simply
                            // won't be used — maybeAutocorrectAndCommitSpace
                            // requires a non-blank typedWord match) so a
                            // stale verdict from the previous word can never
                            // be mistakenly reused for an empty buffer.
                            lastSpellCheckVerdict = SpellCheckVerdict(raw, looksLikeTypo, topCorrection)
                            return
                        }

                        // Mix mode: this callback is answering the extra English-side
                        // lookup fired from fetchEnglishSuggestionsForMix — merge its
                        // results into mixEnglishSuggestions and recombine with
                        // whatever the Sinhala side has separately, instead of
                        // clobbering suggestions.value wholesale. Bail out if the
                        // buffer has since moved on (cleared, or now a different
                        // word) — checked both by request id (guards against a
                        // stale in-flight reply whose query text happens to match
                        // the current buffer again, e.g. after a backspace-retype)
                        // and by the buffer content itself, so a slow async reply
                        // can never attach stale suggestions.
                        val raw = mixEnglishQuery
                        val requestId = mixEnglishRequestId
                        if (currentLanguage.value != "mix" || raw.isEmpty()) return
                        if (wordBuffer.toString() != raw) return
                        if (requestId != mixEnglishRequestId) return
                        val englishWords = mutableListOf<String>()
                        englishWords.add(raw)
                        results?.forEach { info ->
                            for (i in 0 until info.suggestionsCount) {
                                val s = info.getSuggestionAt(i)
                                if (s != raw && englishWords.size < 3) englishWords.add(s)
                            }
                        }
                        mixEnglishSuggestions = englishWords
                        recomputeMixSuggestions()
                    }
                    override fun onGetSentenceSuggestions(results: Array<out android.view.textservice.SentenceSuggestionsInfo>?) {}
                },
                false
            )
        } catch (e: Exception) {
            android.util.Log.w("SinKey", "SpellCheckerSession init failed", e)
        }
    }

    // The single ImeComposeView instance reused for the lifetime of the
    // service.
    // (ported from FlorisBoard's proven approach — see
    // FlorisImeService.onCreateInputView()): the previous approach here
    // tried to keep onCreateInputView() returning a normal View and then
    // patch around InputMethodService's own internal setInputView()/
    // mInputFrame add-remove machinery from the outside (decorView tag
    // checks, manually detaching stale parents in onWindowShown, etc).
    // That machinery is what actually owns the swap-in/swap-out of the
    // input view on every window show, and different OEM window managers
    // schedule that swap slightly differently — no amount of guessing at
    // "is the old view's parent stale yet" from outside could reliably
    // win that race on every device, which is why the double-keyboard bug
    // kept resurfacing after each attempted patch.
    //
    // FlorisBoard's approach sidesteps the race entirely instead of
    // trying to win it: onCreateInputView() returns null (disabling the
    // framework's default input view placement/swap logic altogether),
    // and the Compose root is added directly, once, as a plain child of
    // the IME window's own content ViewGroup (android.R.id.content) in
    // onCreate() equivalent (here: the first onCreateInputView() call).
    // Since the framework's setInputView()/mInputFrame swapping is never
    // invoked for our view at all, there is no framework-driven add/
    // remove sequence left to race with — the view is attached exactly
    // once for the service's whole lifetime and never re-attached, so two
    // live copies can never be composited together.
    private var imeComposeView: ImeComposeView? = null

    // BUG FIX (WhatsApp-header-tap double/ghost keyboard): confirmed via
    // logcat instrumentation that onCreateInputView never re-fires and the
    // content ViewGroup's childCount stays constant (2) across this
    // transition — there is genuinely only ONE live keyboard view at all
    // times, so this was never a duplicate-attachment bug on our side.
    // Screen recording of the bug frame-by-frame showed the "second"
    // keyboard is a snapshot/screenshot of the outgoing chat screen (which
    // visually includes our docked keyboard) that WhatsApp's own Activity
    // transition animates off-screen when navigating to/from
    // ContactInfoActivity — Android's screenshot-transition machinery
    // captures whatever is currently composited on screen, including our
    // IME window, before our window has actually been torn down for this
    // transition. We cannot prevent WhatsApp from taking that screenshot,
    // but we CAN make sure our content is already blank at the moment it's
    // taken: this flag drives the composition to render nothing the instant
    // the *host app's* window loses focus, which happens at the START of
    // that transition — well before WhatsApp's screenshot is captured.
    // Driven from a View.OnWindowFocusChangeListener on the decorView
    // (attached in onCreateInputView below), NOT from onWindowShown/
    // onWindowHidden — those fire on OUR window's own show/hide, which for
    // a transient blip like this often lags behind or never fires at all,
    // since our window may never actually be told to hide for a same-app
    // Activity transition.
    private var hostWindowFocused = mutableStateOf(true)

    // Tracks the actual visible keyboard content's measured height in
    // pixels — NOT the same as imeComposeView's own height, which is now
    // MATCH_PARENT (full window height) as part of the FlorisBoard-style
    // window-sizing fix. onComputeInsets needs the real content height to
    // correctly report where the keyboard starts, so touches above it
    // reach the host app instead of being swallowed by our now-full-size
    // window. Updated via Modifier.onGloballyPositioned on the bottom-
    // aligned content Box in onCreateInputView below.
    @Volatile
    private var keyboardContentHeightPx = 0

    override fun onCreateInputView(): View? {
        android.util.Log.d("SinKeyDebug", "onCreateInputView called, imeComposeView is null? ${imeComposeView == null}")
        if (imeComposeView != null) {
            // Already added directly to the window's content in a
            // previous call — nothing to do. Returning null again keeps
            // the framework's default input-view placement disabled.
            android.util.Log.d("SinKeyDebug", "onCreateInputView: returning early, already have a composeView")
            return null
        }

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
                val swipeTypingEnabled by prefs.swipeTypingEnabled.collectAsState(initial = false)
                val smoothImeTransition by prefs.smoothImeTransition.collectAsState(initial = true)
                SinKeyTheme(themeMode = themeMode) {
                  // REMOVED: AnimatedVisibility-based "Smooth IME Transition".
                  //
                  // Every other keyboard app tested (FlorisBoard, and the
                  // user's confirmation that no other keyboard shows this
                  // bug) renders its content with a single, direct
                  // measure/layout pass — no AnimatedVisibility, no
                  // lookahead layout. This composable was the one piece of
                  // this app's rendering pipeline that had no equivalent
                  // anywhere else, and multiple other fix attempts (window
                  // attach/detach, onWindowShown/onWindowHidden logic, even
                  // toggling this feature's own animation curve off via
                  // smoothImeTransition=false) did not resolve the
                  // duplicate-keyboard bug — which pointed away from this
                  // composable, since disabling the animation curve alone
                  // didn't help.
                  //
                  // The detail that mattes: AnimatedVisibility always runs
                  // a two-pass "lookahead" measurement internally (as part
                  // of Compose's LookaheadScope machinery) to compute
                  // enter/exit transitions — this happens regardless of
                  // whether the enter/exit transition itself is
                  // EnterTransition.None/ExitTransition.None. Setting
                  // smoothImeTransition=false only swaps out the animation
                  // curve used *within* that lookahead pass; it does not
                  // remove the lookahead pass itself. If a recomposition of
                  // this subtree is triggered at the exact moment the host
                  // app (WhatsApp) is itself mid-transition — e.g. from a
                  // window-insets change, a configuration change, or simply
                  // one of the collectAsState() flows above re-emitting —
                  // Compose can commit two different measured layout
                  // positions for the same content across consecutive
                  // frames, both of which get drawn before the newer one
                  // fully replaces the older one on screen. That reads
                  // exactly as "duplicate keyboard" and is timing-dependent
                  // in a way that would explain why it reproduces
                  // specifically around WhatsApp's own header-tap
                  // navigation transition and nowhere else.
                  //
                  // Removing AnimatedVisibility entirely removes the
                  // lookahead pass entirely, not just the animation that
                  // played through it. KeyboardView now composes directly;
                  // there is only ever one measure/layout pass for this
                  // content, so there is nothing left that could commit two
                  // different layout positions for the same frame range.
                  //
                  // smoothImeTransition is intentionally left unused here
                  // rather than deleted outright, in case a non-
                  // AnimatedVisibility-based entrance effect (e.g. a plain
                  // Modifier.graphicsLayer { alpha = ... } animated by
                  // Animatable, which does NOT trigger a lookahead pass) is
                  // wanted back later.
                  run {
                    val focused by hostWindowFocused

                    // BUG FIX (WhatsApp-header-tap double/ghost keyboard):
                    // render nothing while the host app's window is
                    // unfocused. See hostWindowFocused field comment for
                    // the full mechanism. This means our content goes
                    // blank an instant before WhatsApp's own screenshot-
                    // transition captures the screen, so the "second"
                    // keyboard that transition animates away is blank —
                    // no visible duplicate. Content reappears the instant
                    // focus returns, which happens fast enough (same
                    // frame budget as any other keyboard show) that it
                    // reads as an ordinary keyboard show, not a flicker.
                    if (focused) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            keyboardContentHeightPx = coords.size.height
                        }
                    ) {
                    KeyboardView(
                        currentLanguage = currentLanguage.value,
                        keyboardHeight = keyboardHeight,
                        bottomSpaceEnabled = bottomSpaceEnabled,
                        bottomSpaceSize = bottomSpaceSize,
                        showKeyBorders = showKeyBorders,
                        isDark = isDark,
                        suggestions = suggestions.value,
                        onSuggestionSelected = ::handleSuggestion,
                        autocorrectUndoWord = autocorrectUndo.value?.originalTyped,
                        onUndoAutocorrect = ::undoAutocorrect,
                        swipeTypingEnabled = swipeTypingEnabled,
                        onSwipeGesture = ::resolveGestureCandidates,
                        onGestureWordCommitted = ::commitGestureWord,
                        onKey = ::handleKey,
                        inputType = currentInputTypeState.value,
                        boardStack = boardStack.value,
                        onBoardStackChange = { boardStack.value = it },
                        shiftState = shiftState.value,
                        onShiftStateChange = { shiftState.value = it },
                        dismissedUpdateVersionCode = dismissedUpdateVersionCode.value,
                        onDismissedUpdateVersionCodeChange = { dismissedUpdateVersionCode.value = it },
                        onStickerSend = { filePath, mimeType -> onStickerSelected(filePath, mimeType) },
                        onPickStickerImage = { pickImageForSticker() },
                        pendingStickerImagePath = pendingStickerImagePath.value,
                        isTranslateMode = isTranslateMode.value,
                        onTranslateModeChange = { open -> if (open) openTranslateMode() else closeTranslateMode() },
                        translateSourceLang = translateSourceLang.value,
                        translateTargetLang = translateTargetLang.value,
                        onTranslateLanguagesSwapped = ::swapTranslateLanguages,
                        translateSourceText = translateSourceText.value,
                        onTranslateSourceTextChanged = ::onTranslateSourceTextChanged,
                        translateResultText = translateResultText.value,
                        isTranslating = isTranslating.value,
                        onSaveImageSticker = { draft ->
                            saveEditedImageSticker(
                                imageScale = draft.imageScale,
                                imageOffsetXFraction = draft.imageOffsetXFraction,
                                imageOffsetYFraction = draft.imageOffsetYFraction,
                                shape = draft.shape,
                                text = draft.text,
                                textColor = draft.textColor,
                                textSizeFraction = draft.textSizeFraction,
                                textXFraction = draft.textXFraction,
                                textYFraction = draft.textYFraction,
                                fontTypeface = when (draft.fontStyle) {
                                    com.spmods.sinkey.keyboard.StickerFontStyle.BOLD -> android.graphics.Typeface.DEFAULT_BOLD
                                    com.spmods.sinkey.keyboard.StickerFontStyle.CLASSIC -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                                    com.spmods.sinkey.keyboard.StickerFontStyle.TYPEWRITER -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                                    com.spmods.sinkey.keyboard.StickerFontStyle.HANDWRITTEN -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
                                    com.spmods.sinkey.keyboard.StickerFontStyle.CLEAN -> android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                                },
                                outlineEnabled = draft.outlineEnabled
                            )
                        }
                    )
                    }
                    }
                    }
                  }
                }
        }

        // Owners must be installed on the decorView before the compose
        // view is attached, same as before (see onCreate()).
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            decor.setViewTreeViewModelStoreOwner(lifecycleOwner)

            // See hostWindowFocused field comment above for why this is
            // driven from a raw window-focus listener rather than
            // onWindowShown/onWindowHidden. hasWindowFocus() here reflects
            // OUR IME window's focus state — which Android reliably flips
            // to false at the very start of a same-app Activity transition
            // (e.g. WhatsApp navigating to ContactInfoActivity and back),
            // even in cases where onWindowHidden/onWindowShown don't fire
            // for the transition at all because our window is never fully
            // torn down.
            decor.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
                android.util.Log.d("SinKeyDebug", "decor window focus changed: hasFocus=$hasFocus")
                hostWindowFocused.value = hasFocus
            }
        }

        // EXPERIMENT (WhatsApp double-keyboard bug): switched from
        // WRAP_CONTENT height to MATCH_PARENT, matching FlorisBoard's
        // ImeRootView exactly (see ime/window/ImeRootView.kt in
        // FlorisBoard's source — LayoutParams(MATCH_PARENT, MATCH_PARENT)).
        // The user confirmed FlorisBoard does not reproduce this bug on
        // the same device performing the same WhatsApp header-tap/back
        // reproduction steps that reliably reproduce it here, and multiple
        // rounds of logcat-verified fixes targeting attach/detach and
        // render-visibility logic did not resolve it — narrowing the
        // remaining plausible cause to this one specific, deep
        // architectural difference: a window that Android auto-resizes
        // to wrap its content (this app, previously) is fundamentally a
        // different kind of window than one with fixed MATCH_PARENT
        // bounds that never resizes (FlorisBoard) from Android's
        // window-manager/compositor perspective, and WhatsApp's own
        // screen-transition screenshot machinery may treat/composite
        // those two cases differently. The keyboard's visual position and
        // size are unchanged for the user — KeyboardView is now wrapped
        // in a full-bounds Box that bottom-aligns it (see below) — only
        // the *window's own* layout params changed.
        composeView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        val content = window?.window?.findViewById<ViewGroup>(android.R.id.content)
        android.util.Log.d("SinKeyDebug", "onCreateInputView: content childCount BEFORE addView = ${content?.childCount}")
        content?.addView(composeView)
        android.util.Log.d("SinKeyDebug", "onCreateInputView: content childCount AFTER addView = ${content?.childCount}")

        imeComposeView = composeView
        // null disables the framework's own input-view placement — see
        // the field comment on imeComposeView above for why.
        return null
    }

    // NOTE: no setInputView() override, and no attempt to intercept
    // Android's own input-frame add/remove sequence at all — see the
    // field comment on imeComposeView above. onCreateInputView() now
    // returns null and disables that machinery entirely, so there's
    // nothing left here to patch around.

    override fun onWindowShown() {
        super.onWindowShown()
        android.util.Log.d("SinKeyDebug", "onWindowShown called, lifecycle state before = ${lifecycleOwner.lifecycle.currentState}, content childCount = ${window?.window?.findViewById<ViewGroup>(android.R.id.content)?.childCount}")
        // Unconditional, matching FlorisBoard's LifecycleInputMethodService
        // exactly. Earlier versions of this method tried to guard against
        // "redundant" resumes with decorView-tag checks and manual stale-
        // parent cleanup, reasoning that re-firing ON_RESUME on a view that
        // was already fully attached and composed was itself forcing an
        // extra recomposition/relayout pass that a race with the window
        // manager could turn into a visible duplicate frame. That guard
        // logic is what kept producing new variants of the same bug —
        // because the actual source of the duplicate-attachment race was
        // the framework's own setInputView() swap machinery, not this
        // lifecycle call. Now that onCreateInputView() returns null and
        // our view is attached exactly once directly to the window content
        // (never re-attached by the framework), simply firing ON_RESUME
        // every time onWindowShown() is called is safe and correct — it
        // only updates lifecycle-aware state (collectAsState flows, etc),
        // it does not touch view attachment at all.
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        android.util.Log.d("SinKeyDebug", "onWindowHidden called, lifecycle state before = ${lifecycleOwner.lifecycle.currentState}")
        // Unconditional, matching FlorisBoard exactly — see onWindowShown
        // for why the earlier "only pause if resumed" guard is no longer
        // needed (and never was the actual fix for the duplicate-keyboard
        // bug; the framework's own view-swap machinery was).
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        android.util.Log.d("SinKeyDebug", "onStartInputView called, restarting=$restarting, packageName=${info?.packageName}")
        // lifecycle ON_RESUME is driven by onWindowShown()

        // Bug O4 Fix: Cancel any active composing span on the previous
        // InputConnection before switching fields. Without this, the underlined
        // Sinhala preview text stays visible in the old field after focus moves
        // to a new one, and the new field starts with a stale composing state —
        // creating the appearance of keyboard text appearing in two places at once.
        currentInputConnection?.finishComposingText()

        wordBuffer.clear()
        englishBuffer.clear()
        resumedWordBeforeCursor = null
        clearSuggestions()
        // A pending undo chip refers to text in the field we're leaving —
        // meaningless (and potentially actionable-on-the-wrong-field) once
        // focus moves elsewhere.
        clearAutocorrectUndoIfAny()
        currentInputTypeState.value = info?.inputType ?: 0
        // -1 = "don't know yet" — the very first onUpdateSelection call for
        // this field will just record the real position instead of treating
        // it as a jump (see onUpdateSelection).
        expectedCursorPosition = -1
        // Any edits made in the previous field are irrelevant now — don't
        // let stale pending-edit counts suppress external-move detection
        // (or worse, get consumed by) callbacks belonging to the new field.
        pendingSelfEdits = 0
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

    /**
     * Fires whenever the cursor/selection actually changes in the field —
     * including moves WE didn't cause (the user tapping elsewhere in the
     * text, using the system's cursor handle, arrow keys from a hardware
     * keyboard, another app repositioning it, etc). Previously this had no
     * override at all, so none of those moves were ever detected: wordBuffer/
     * englishBuffer (this service's in-memory "what's being composed" state)
     * and the suggestion strip kept reflecting whatever was being typed
     * *before* the jump, even though the cursor — and therefore the actual
     * word under it — had changed. E.g. type a partial word, tap earlier in
     * the sentence: the suggestion strip kept showing suggestions for the
     * word you'd abandoned instead of clearing or reflecting the new spot.
     *
     * expectedCursorPosition is updated after every edit *we* make (typing,
     * backspace, committing a suggestion — see setExpectedCursorAfterEdit()
     * calls below) specifically so this override can tell "cursor moved
     * because of our own edit" apart from "cursor moved for some other
     * reason" by comparing newSelEnd against it. Only the latter case needs
     * to abandon in-progress composing state; reacting to our own
     * self-caused moves too would be redundant (updateSuggestions() already
     * runs right after those edits) and would also race with it.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        // If we have edits in flight, this callback is (at least most
        // likely) the async echo of one of them, not a real external jump —
        // consume it and skip the external-move handling below. See the
        // pendingSelfEdits doc comment for why this replaced comparing
        // newSelEnd against a synchronously-read expectedCursorPosition.
        if (pendingSelfEdits > 0) {
            pendingSelfEdits--
            expectedCursorPosition = newSelEnd
            return
        }

        val isExternalMove = expectedCursorPosition != -1 && newSelEnd != expectedCursorPosition
        // Recorded before finishComposingText() runs below — see the
        // StackOverflowError note on that call for why this ordering
        // matters, not just tidiness.
        expectedCursorPosition = newSelEnd
        if (isExternalMove) {
            // Cursor landed somewhere we didn't put it — whatever was being
            // composed is no longer at the cursor, so there's nothing
            // coherent left to keep typing into. Finish it as plain text
            // (matches the Bug O4 Fix behaviour in onStartInputView for the
            // same underlying reason: never leave a stale composing span
            // visible) and clear the now-irrelevant suggestion strip.
            if (wordBuffer.isNotEmpty() || englishBuffer.isNotEmpty()) {
                // wordBuffer/englishBuffer are cleared *before* calling
                // finishComposingText(), not after. Many editors/frameworks
                // dispatch finishComposingText() synchronously back into
                // this same onUpdateSelection() (that round-trip is
                // reflected in real crash traces as
                // SinKeyInputMethodService.onUpdateSelection ->
                // ...finishComposingText -> ...updateSelection ->
                // onUpdateSelection again). With the old clear-after
                // ordering, that reentrant call still saw a non-empty
                // buffer and an unmatched expectedCursorPosition (also
                // fixed above by moving that assignment earlier), so it
                // took the same "isExternalMove" branch and called
                // finishComposingText() again — recursing until a
                // StackOverflowError. Clearing first means the reentrant
                // call's own isExternalMove check (now also seeing a
                // matching expectedCursorPosition) finds nothing left to do.
                wordBuffer.clear()
                englishBuffer.clear()
                // Whatever word was previously resumed (if any) is no
                // longer relevant — this cursor move has abandoned it, same
                // as the buffers themselves just above. Prevents a stale
                // value here from wrongly deleting text near an unrelated
                // later commit.
                resumedWordBeforeCursor = null
                currentInputConnection?.finishComposingText()
            }
            clearSuggestions()
            lastCommittedWord = ""
            lastCommittedLanguage = ""
            wasExplicitShift = false
            // The undo chip's delete-then-retype logic assumes the cursor
            // is still sitting right after the word it corrected — once the
            // user has tapped elsewhere, that assumption no longer holds,
            // so the chip must not be actionable anymore.
            clearAutocorrectUndoIfAny()

            // BUG FIX: the cursor landing mid/after an *existing* word (the
            // user tapping back into text they already typed) used to just
            // leave the suggestion strip empty forever, since nothing here
            // ever looked at what's actually around the new cursor position.
            // Re-derive that word from the field and re-run suggestions for
            // it, so tapping into an existing word offers corrections/
            // completions for it exactly like typing it fresh would.
            currentInputConnection?.let { reseedSuggestionsForWordAtCursor(it) }
        }
    }

    /**
     * Reads the word immediately touching the cursor (before it, extending
     * back to the previous whitespace/punctuation boundary) and reseeds
     * wordBuffer/englishBuffer + the suggestion strip from it, as if the
     * user had just finished typing that word. Only handles the "cursor is
     * right after or inside a word" case — if the cursor is between words
     * (e.g. right after a space), there's no partial word to resume, so
     * this intentionally leaves the buffers empty and lets updateSuggestions()
     * fall back to next-word prediction as before.
     *
     * Deliberately conservative about what counts as "part of a word": stops
     * at the first whitespace or ASCII punctuation character, same set used
     * to detect the composing-word boundary elsewhere (learnWordsFromClipboard's
     * tokenizer). Sinhala combining marks are within 0x0D80-0x0DFF so they're
     * naturally included as letters, not treated as boundaries.
     */
    private fun reseedSuggestionsForWordAtCursor(ic: android.view.inputmethod.InputConnection) {
        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
        if (before.isEmpty()) return
        val boundary = Regex("[\\s,.;:!?\"'()\\[\\]{}/\\\\|]")
        var start = before.length
        while (start > 0 && !boundary.matches(before[start - 1].toString())) start--
        val word = before.substring(start)
        if (word.isEmpty()) return

        // Route into whichever buffer matches the word's own script rather
        // than currentLanguage — the user may have typed this word in a
        // different mode than the one they're currently in (e.g. typed in
        // "si", then switched to "en", then tapped back into the Sinhala
        // word), and resuming into the wrong buffer would just transliterate
        // it incorrectly.
        //
        // detectWordScript returns null for short (<2 char), mixed-script,
        // digit, or other-script words — treat that as "can't tell" and
        // leave the buffers empty rather than guessing, instead of the
        // earlier `script != "si"` check which incorrectly folded null into
        // the English branch and would wrongly try to resume e.g. a lone
        // punctuation-adjacent character or a "2024"-style token as English.
        val script = detectWordScript(word) ?: return
        if (script == "si" && isSinhalaTyping()) {
            wordBuffer.append(word)
        } else if (script == "en") {
            englishBuffer.append(word)
        } else {
            // Sinhala-script word but keyboard is currently in pure "en"
            // mode — nothing sensible to resume typing into; leave buffers
            // empty rather than guess.
            return
        }
        // The word is still sitting in the field as plain committed text —
        // wordBuffer/englishBuffer now also hold a copy of it, purely to
        // drive the suggestion strip as if the user were still mid-typing
        // it. Deliberately NOT touching the field itself here (no delete/
        // recompose): this function runs from onUpdateSelection on every
        // cursor move onUpdateSelection classifies as "external", which can
        // fire more often/eagerly than just deliberate user taps into old
        // text (including races during fast typing) — mutating the field on
        // every such call previously caused text to vanish mid-type. See
        // resumedWordBeforeCursor below, which is what actually makes the
        // next SPACE replace this word instead of duplicating it, without
        // needing to touch the field until SPACE is really pressed.
        resumedWordBeforeCursor = word
        updateSuggestions()
    }

    /**
     * Call after any edit `handleKey` itself makes to the field (typing,
     * backspace, committing a suggestion, punctuation, etc.) so
     * onUpdateSelection can tell that edit's own cursor move apart from an
     * external one (user tapping elsewhere, etc — see onUpdateSelection's
     * doc). Reads the real resulting position back from the InputConnection
     * rather than trying to predict it per edit type (composing spans,
     * multi-codepoint deletes, and styled text all change the cursor by a
     * different amount than the raw key text's length would suggest, so
     * predicting it would be fragile and easy to get subtly wrong).
     */
    private fun syncExpectedCursorPosition(ic: android.view.inputmethod.InputConnection) {
        // Don't try to read the resulting cursor position back synchronously
        // (via getExtractedText()) — on many editors the composing/commit
        // edit we just sent hasn't actually been applied and reflected back
        // yet when this runs, so that read can return a stale (pre-edit)
        // position. Comparing a later, correct onUpdateSelection() callback
        // against that stale value was misclassifying our own edits as
        // external cursor jumps and erasing what was just typed — see the
        // pendingSelfEdits doc comment above its declaration. Instead, just
        // mark that we're expecting one more self-caused onUpdateSelection()
        // callback; that callback itself is what records the real position.
        pendingSelfEdits++
    }

    /**
     * Consumes resumedWordBeforeCursor (see its own doc comment) right
     * before a keystroke is about to continue typing/deleting into a
     * resumed word: deletes the on-screen original so the caller's own
     * composing/commit call replaces it instead of duplicating it beside it.
     *
     * Verifies the text immediately before the cursor actually still equals
     * resumedWordBeforeCursor before deleting anything. onUpdateSelection's
     * own external-move detection can occasionally misfire on OUR OWN edits
     * (a known platform timing race — see its doc comment on the
     * StackOverflowError this already once caused), which would otherwise
     * re-trigger reseedSuggestionsForWordAtCursor mid-type and leave
     * resumedWordBeforeCursor set for a word the user is still actively,
     * correctly typing — blindly trusting it here would then delete text
     * that was never stale in the first place, mid-keystroke. Checking
     * against the real field contents makes this a no-op whenever that
     * race happens, instead of eating live text.
     */
    private fun consumeResumedWordIfStillPresent(ic: android.view.inputmethod.InputConnection) {
        val resumed = resumedWordBeforeCursor ?: return
        resumedWordBeforeCursor = null
        val before = ic.getTextBeforeCursor(resumed.length, 0)?.toString()
        if (before == resumed) {
            ic.deleteSurroundingText(resumed.length, 0)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        android.util.Log.d("SinKeyDebug", "onFinishInputView called, finishingInput=$finishingInput")
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
        // Dispose the Compose composition explicitly now that the service
        // (and therefore this view) is truly being torn down, so it stops
        // observing state and releases its composition resources instead
        // of lingering as a zombie collector.
        imeComposeView?.disposeComposition()
        imeComposeView = null
        super.onDestroy()
    }

    /**
     * Roman letters whose case changes which Sinhala consonant they map to
     * in [com.spmods.sinkey.keyboard.SinhalaTransliterator] (n/N, l/L, t/T,
     * d/D, sh/Sh, th/TH, dh/DH). For these, an uppercase letter must only
     * come from an explicit user Shift press — never from auto-capitalize —
     * or the wrong Sinhala letter gets typed. Every other Roman letter
     * (vowels, k/g/j/p/b/m/y/r/v/w/s/h/f, etc.) has no case-meaning, so it's
     * safe to let auto-shift capitalize it like a normal English keyboard.
     * The keyboard sends single characters here, so "sh"/"th"/"dh" show up
     * as separate keystrokes — checking the leading consonant ('s', 't',
     * 'd', plus 'n' and 'l') covers all of them.
     */
    private fun isCaseSensitiveSinhalaLetter(key: String): Boolean =
        key.lowercase() in setOf("n", "l", "t", "d", "s")

    private fun handleKey(key: String) {
        maybeFeedback()
        val ic = currentInputConnection ?: return
        // Any key press other than tapping the undo chip itself (which goes
        // through undoAutocorrect(), not this function) means the user has
        // moved on from the word that was just autocorrected — the chip
        // shouldn't linger describing a correction that's no longer the
        // most recent thing that happened.
        clearAutocorrectUndoIfAny()

        when (key) {
            "BACKSPACE" -> {
                val selectedText = ic.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    wordBuffer.clear()
                    englishBuffer.clear()
                    resumedWordBeforeCursor = null
                    ic.finishComposingText()
                    ic.commitText("", 1)
                    // Same bug as the wordBuffer branch below: clearing the
                    // buffer without refreshing the strip left whatever was
                    // suggested for the just-deleted selection still showing.
                    updateSuggestions()
                } else if (wordBuffer.isNotEmpty()) {
                    // Same resumed-word handling as the per-letter typing
                    // branches: if this word came from reseedSuggestionsForWordAtCursor
                    // rather than being typed fresh, it's still on screen as
                    // plain text with no composing span — delete it first so
                    // the composing text set below replaces it instead of
                    // duplicating it.
                    consumeResumedWordIfStillPresent(ic)
                    wordBuffer.deleteCharAt(wordBuffer.length - 1)
                    if (wordBuffer.isEmpty()) {
                        ic.setComposingText("", 1)
                        ic.finishComposingText()
                    } else {
                        setComposingTextStyled(ic, renderStyledBuffer())
                    }
                    // BUG FIX: this branch used to never refresh the
                    // suggestion strip, so backspacing through a
                    // partially-typed Sinhala word left stale suggestions
                    // from before the backspace on screen. updateSuggestions()
                    // itself already handles the wordBuffer-now-empty case
                    // (falls back to next-word prediction / clears the bar),
                    // so it's safe to call unconditionally here.
                    updateSuggestions()
                } else {
                    if (englishBuffer.isNotEmpty()) englishBuffer.deleteCharAt(englishBuffer.length - 1)
                    if (englishBuffer.isEmpty()) resumedWordBeforeCursor = null
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
                if (isSinhalaTyping()) {
                    commitPendingWord()
                    ic.commitText(" ", 1)
                } else {
                    maybeAutocorrectAndCommitSpace(ic)
                }
                // After space, check if previous char was sentence-ending punctuation
                updateAutoShift(ic)
                // Word just finished — offer a next-word prediction instead of
                // leaving the suggestion bar empty.
                updateSuggestions()
            }
            "ENTER" -> {
                if (isSinhalaTyping()) commitPendingWord()
                else { learnWord(englishBuffer.toString(), "en"); englishBuffer.clear(); resumedWordBeforeCursor = null; clearSuggestions() }

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
            // TOOL_STICKER, TOOL_FONT, and TOOL_TRANSLATE never reach here —
            // AppsMicBar intercepts them directly (onStickerOpen/onFontOpen/
            // onTranslateOpen) before calling onKey, same as TOOL_CLIPBOARD.
            "TOOL_APPS", "TOOL_SETTINGS" -> {
                android.util.Log.d("SinKey", "Tool action: $key (not yet implemented)")
            }
            "SWITCH_KEYBOARD" -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
            "LANG_TOGGLE" -> {
                commitPendingWord()
                englishBuffer.clear()
                resumedWordBeforeCursor = null
                clearSuggestions()
                currentLanguage.value = when (currentLanguage.value) {
                    "mix" -> "en"
                    "en"  -> "si"
                    else  -> "mix" // "si" -> "mix"
                }
            }
            "," , "." -> {
                commitPendingWord()
                englishBuffer.clear()
                resumedWordBeforeCursor = null
                clearSuggestions()
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
                        resumedWordBeforeCursor = null
                        clearSuggestions()
                        ic.commitText(text, 1)
                    }
                    syncExpectedCursorPosition(ic)
                    return
                }
                val isSinglePrintable = key.length == 1 && !key[0].isLetter()
                if (isSinglePrintable) {
                    commitPendingWord()
                    englishBuffer.clear()
                    resumedWordBeforeCursor = null
                    ic.commitText(key, 1)
                    // Check sentence-ending punctuation (! ?)
                    if (key == "!" || key == "?") {
                        if (shiftState.value == ShiftState.OFF) shiftState.value = ShiftState.ONE_SHOT
                    }
                    syncExpectedCursorPosition(ic)
                    return
                }
                if (isEmoji(key)) {
                    commitPendingWord()
                    ic.commitText(key, 1)
                    serviceScope.launch { prefs.addRecentEmoji(key) }
                } else if (isSinhalaTyping()) {
                    // Sinhala's phonetic scheme uses case to pick between
                    // real, distinct letters for a specific subset of keys
                    // (lowercase n=න vs uppercase N=ණ, l=ල vs L=ළ, t=ට vs
                    // T=ත, d=ද vs D=ද, "sh"=ශ vs "Sh"=ෂ, "th"=ත vs "TH"=ඨ) —
                    // for those, case is never cosmetic, so auto-capitalize
                    // (ONE_SHOT at sentence/field start) must NOT apply the
                    // way it does for English; only an explicit user Shift
                    // press should uppercase them. wasExplicitShift tracks
                    // shift presses that happened after the current
                    // word/field started, distinguishing that from auto.
                    //
                    // Every other letter (vowels, k/g/j/p/b/m/y/r/v/w/h/f)
                    // has no alternate meaning by case, so it's free to
                    // follow normal shift/auto-capitalize behaviour like an
                    // English keyboard — this is what mix mode users expect
                    // when a word starts a sentence.
                    val allowShiftHere = shiftState.value != ShiftState.OFF &&
                        (wasExplicitShift || !isCaseSensitiveSinhalaLetter(key))
                    val typed = if (allowShiftHere) key.uppercase() else key.lowercase()
                    // If wordBuffer's current contents came from resuming an
                    // existing on-screen word (reseedSuggestionsForWordAtCursor —
                    // cursor tapped back into it) rather than being typed fresh
                    // this session, that word is still sitting in the field as
                    // plain text with no composing span backing it. Now that
                    // the user is actually continuing to type it, delete that
                    // original on-screen copy before appending — otherwise the
                    // upcoming setComposingTextStyled below would insert a
                    // second copy beside the first instead of replacing it.
                    consumeResumedWordIfStillPresent(ic)
                    wordBuffer.append(typed)
                    setComposingTextStyled(ic, renderStyledBuffer())
                    updateSuggestions()
                    // Consume one-shot shift after first Sinhala letter
                    if (shiftState.value == ShiftState.ONE_SHOT) { shiftState.value = ShiftState.OFF; wasExplicitShift = false }
                } else {
                    // Apply shift to English letter
                    val typed = if (shiftState.value != ShiftState.OFF) key.uppercase() else key.lowercase()
                    // Same resumed-word handling as the Sinhala branch above —
                    // see that comment. English's on-screen original was set
                    // by reseedSuggestionsForWordAtCursor as plain committed
                    // text (not composing), so it must be deleted before the
                    // live per-letter commitText below, or this would insert
                    // a second copy beside the original.
                    consumeResumedWordIfStillPresent(ic)
                    englishBuffer.append(typed)
                    val styled = com.spmods.sinkey.keyboard.FancyTextMapper.apply(typed, cachedFancyTextStyle)
                    ic.commitText(styled, 1)
                    updateSuggestions()
                    // Consume one-shot shift after letter
                    if (shiftState.value == ShiftState.ONE_SHOT) shiftState.value = ShiftState.OFF
                }
            }
        }
        // Sync expectedCursorPosition after every key handled above — see
        // onUpdateSelection's doc comment for why this needs to run
        // unconditionally on every edit path (typing, backspace, committing
        // a suggestion, punctuation, etc.) rather than only some of them:
        // any path that skips it would make the *next* onUpdateSelection
        // call wrongly look like an external cursor jump and clear
        // in-progress composing state that's actually still valid.
        syncExpectedCursorPosition(ic)

        // Translate row active (TOOL_TRANSLATE) — the key above (whatever
        // it was) has already gone through the completely normal typing
        // pipeline, including full Sinhala transliteration/autocorrect, and
        // is now really sitting in the target app's field. Re-read what's
        // there since the row opened and (debounced) translate it. See
        // the translate-state block's doc comment for why this "watch the
        // real field" design was chosen over giving the row its own typing
        // buffer.
        if (isTranslateMode.value) {
            refreshTranslateSourceFromField(ic)
        }
    }

    /**
     * Re-reads the text between [translateAnchorText]'s anchor point and
     * the current cursor, and if it changed, kicks off a debounced
     * translation of it. Called after every key while the translate row is
     * open (see the end of handleKey). Uses getTextBeforeCursor with a
     * generous length cap and works out the "new since anchor" portion by
     * length difference from the anchor snapshot — simpler and more
     * robust than trying to special-case every key (BACKSPACE, SPACE,
     * autocorrect swaps, suggestion taps, etc.) individually, since all of
     * them already converge on "the field's text changed" by the time this
     * runs.
     */
    private fun refreshTranslateSourceFromField(ic: android.view.inputmethod.InputConnection) {
        val maxLen = translateAnchorText.length + 500
        val currentBeforeCursor = ic.getTextBeforeCursor(maxLen, 0)?.toString() ?: ""
        val newSource = if (currentBeforeCursor.length >= translateAnchorText.length &&
            currentBeforeCursor.startsWith(translateAnchorText)) {
            // Normal case: user has only added to/edited after the anchor
            // point, so the anchor prefix is still intact — take everything
            // typed after it.
            currentBeforeCursor.substring(translateAnchorText.length)
        } else {
            // The user backspaced past the anchor point (deleted into text
            // that existed before translate mode opened) or the anchor
            // text itself changed underneath us (e.g. autocorrect rewrote
            // an earlier word). Re-anchor to "now" rather than showing a
            // garbled diff — matches the same fallback-to-safe-state
            // philosophy as onUpdateSelection's external-move handling.
            translateAnchorText = currentBeforeCursor
            ""
        }
        if (newSource != translateSourceText.value) {
            android.util.Log.d("SinKeyTranslate", "refresh: anchor='${translateAnchorText}' newSource='${newSource}'")
            onTranslateSourceTextChanged(newSource)
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
     * TOOL_TRANSLATE — opens the translate row (AppsMicBar's isTranslateMode
     * branch). Flushes any in-progress normal typing first, the same as
     * other tools that interrupt typing (e.g. LANG_TOGGLE), so a partially
     * composed word doesn't end up straddling the anchor point below.
     * Anchors translateAnchorText to the field's current text-before-cursor
     * — everything typed after this point, until Close, is "the source
     * text" (see refreshTranslateSourceFromField).
     */
    private fun openTranslateMode() {
        commitPendingWord()
        englishBuffer.clear()
        resumedWordBeforeCursor = null
        clearSuggestions()
        val ic = currentInputConnection
        ic?.finishComposingText()
        translateAnchorText = ic?.getTextBeforeCursor(500, 0)?.toString() ?: ""
        translateSourceText.value = ""
        translateResultText.value = ""
        isTranslating.value = false
        isTranslateMode.value = true
        android.util.Log.d("SinKeyTranslate", "openTranslateMode: anchor='${translateAnchorText}' ic=${ic != null}")
    }

    /**
     * Close (X) on the translate row. Leaves whatever's currently in the
     * field exactly as it is — by the time Close is tapped the live
     * translation has already been written into the real field in place of
     * the typed source (see onTranslateSourceTextChanged), so there's
     * nothing left to commit or discard; Close just stops watching the
     * field and returns the toolbar to its normal tools-row/suggestion-
     * strip behaviour.
     */
    private fun closeTranslateMode() {
        translateJob?.cancel()
        translateRequestId++
        isTranslateMode.value = false
        translateAnchorText = ""
        translateSourceText.value = ""
        translateResultText.value = ""
        isTranslating.value = false
    }

    private fun swapTranslateLanguages() {
        val newSource = translateTargetLang.value
        val newTarget = translateSourceLang.value
        translateSourceLang.value = newSource
        translateTargetLang.value = newTarget
        // Re-anchor to "now" and clear the preview — the text already
        // sitting in the field from before the swap was translated in the
        // old direction; trying to carry it over as new source text in the
        // swapped direction would immediately re-translate it back toward
        // where it started. Simplest correct behaviour: swap starts a
        // fresh source from this point, same as opening translate mode.
        val ic = currentInputConnection
        translateAnchorText = ic?.getTextBeforeCursor(500, 0)?.toString() ?: ""
        translateSourceText.value = ""
        translateResultText.value = ""
    }

    /**
     * Called from refreshTranslateSourceFromField (itself called after
     * every key while the translate row is open — see the end of
     * handleKey) whenever the text typed since the anchor point changed.
     * Debounces a call to TranslateService.translate, and once it replies,
     * replaces the anchor-to-cursor range in the REAL field with the
     * translated text — live, continuously, as the user keeps typing —
     * which is what makes the translation actually "apply" at the exact
     * spot being typed into (WhatsApp, Notepad, wherever) rather than only
     * showing in a separate preview area.
     *
     * After replacing, translateAnchorText is left unchanged and
     * translateSourceText continues tracking new keystrokes typed after
     * the translated text, so the user can keep typing more source text
     * and have it keep translating — but note the field itself now
     * contains the *translated* text where the source used to be, so this
     * function's own field-replacement counts as a self-edit for cursor
     * tracking (syncExpectedCursorPosition), same as any other edit path.
     */
    private fun onTranslateSourceTextChanged(newSourceText: String) {
        translateSourceText.value = newSourceText
        translateJob?.cancel()
        translateRequestId++
        val requestId = translateRequestId

        if (newSourceText.isBlank()) {
            translateResultText.value = ""
            isTranslating.value = false
            return
        }

        isTranslating.value = true
        translateJob = serviceScope.launch {
            kotlinx.coroutines.delay(350)
            android.util.Log.d("SinKeyTranslate", "calling TranslateService.translate('$newSourceText', ${translateSourceLang.value}, ${translateTargetLang.value})")
            val result = com.spmods.sinkey.data.TranslateService.translate(
                newSourceText, translateSourceLang.value, translateTargetLang.value
            )
            android.util.Log.d("SinKeyTranslate", "TranslateService result: '$result' (requestId=$requestId current=$translateRequestId)")
            // Stale-reply guard, same idea as mixEnglishRequestId — if the
            // user kept typing (or swapped languages, or closed the row)
            // while this request was in flight, requestId no longer
            // matches and the reply must be discarded rather than
            // overwriting newer state or editing a field that's moved on.
            if (requestId != translateRequestId) return@launch
            isTranslating.value = false
            if (result != null) {
                translateResultText.value = result
                applyTranslationToField(newSourceText, result)
            }
        }
    }

    /**
     * Replaces the just-typed source text (translateAnchorText..cursor)
     * with [result] in the real field, then updates translateAnchorText so
     * the replacement itself isn't mistaken for new source text on the
     * next keystroke. [expectedSourceText] guards against a race where the
     * user typed more after this translation was requested but before it
     * came back — if the field's current source no longer matches what was
     * actually translated, skip the replacement rather than clobbering
     * newer typing with a stale result (the debounced follow-up request
     * for the newer text will correct it shortly after).
     */
    private fun applyTranslationToField(expectedSourceText: String, result: String) {
        val ic = currentInputConnection ?: return
        val currentBeforeCursor = ic.getTextBeforeCursor(translateAnchorText.length + 500, 0)?.toString() ?: ""
        if (!currentBeforeCursor.startsWith(translateAnchorText)) {
            android.util.Log.d("SinKeyTranslate", "applyTranslationToField ABORT: anchor mismatch. anchor='${translateAnchorText}' currentBeforeCursor='${currentBeforeCursor}'")
            return
        }
        val currentSource = currentBeforeCursor.substring(translateAnchorText.length)
        if (currentSource != expectedSourceText) {
            android.util.Log.d("SinKeyTranslate", "applyTranslationToField ABORT: stale. currentSource='${currentSource}' expected='${expectedSourceText}'")
            return // stale reply — see doc comment
        }

        android.util.Log.d("SinKeyTranslate", "applyTranslationToField APPLYING: '${currentSource}' -> '${result}'")
        ic.beginBatchEdit()
        ic.finishComposingText()
        ic.deleteSurroundingText(currentSource.length, 0)
        setComposingTextStyled(ic, result)
        ic.endBatchEdit()
        // The field now reads translateAnchorText + result (as composing
        // text). Re-anchor past it so this replacement itself isn't picked
        // up as "new source text" the next time refreshTranslateSourceFromField
        // runs — the next keystroke should start a fresh source segment
        // after the translated text, not re-translate the translation.
        translateAnchorText += result
        translateSourceText.value = ""
        syncExpectedCursorPosition(ic)
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
        // If this word was resumed from an existing on-screen word (cursor
        // tapped back into it — see reseedSuggestionsForWordAtCursor), that
        // original text is still sitting in the field as plain committed
        // text; delete exactly it before committing finalWord, or this
        // would insert a second copy right beside the first instead of
        // replacing it. Ordinary fresh-typed words never set this, so this
        // is a no-op for the normal case. Verifies the text is still really
        // there first — see consumeResumedWordIfStillPresent's doc comment.
        if (ic != null) consumeResumedWordIfStillPresent(ic) else resumedWordBeforeCursor = null
        ic?.commitText(finalWord, 1)
        wordBuffer.clear()
        clearSuggestions()
        // Learn the plain (unstyled) word, not the fancy-font glyphs, so the
        // personal dictionary and future suggestions stay in normal text.
        learnWord(if (convertToSinhala) finalWord else raw, if (convertToSinhala) "si" else "en")
        // See onUpdateSelection's doc comment. Redundant when called from
        // within handleKey (which syncs again at its own end) but needed
        // for commitPendingWord's other callers (e.g. onFinishInputView) —
        // harmless either way since it just re-reads the real position.
        ic?.let { syncExpectedCursorPosition(it) }
    }

    /**
     * Pure-English SPACE handling: the word was already committed to the
     * field live, letter by letter, as the user typed it (see the per-letter
     * "else" branch in handleKey, which uses ic.commitText, not composing
     * text) — so by the time SPACE is pressed, exactly what the user typed
     * is already on screen. SPACE therefore only ever commits the space
     * itself; it never silently rewrites what's already there.
     *
     * Autocorrect is intentionally NOT applied here. A spell-checker
     * correction, if one exists for this word, only ever reaches the user
     * via the suggestion strip (see updateSuggestions/handleSuggestion) —
     * committed only if they actually tap it. This keeps typing predictable:
     * what you type is what stays, unless you deliberately pick a
     * suggestion.
     */
    private fun maybeAutocorrectAndCommitSpace(ic: android.view.inputmethod.InputConnection) {
        val typed = englishBuffer.toString()
        // Whatever word is here — freshly typed or resumed from an existing
        // on-screen word via reseedSuggestionsForWordAtCursor — is already
        // exactly what's on screen. Nothing left to delete or re-commit;
        // resumedWordBeforeCursor just needs clearing so it can't linger
        // into a later, unrelated commit.
        resumedWordBeforeCursor = null
        ic.commitText(" ", 1)
        clearAutocorrectUndoIfAny()
        if (typed.isNotBlank()) {
            learnWord(typed, "en")
        }
        englishBuffer.clear()
        lastSpellCheckVerdict = null
    }

    /**
     * Reverts the most recent autocorrect (see maybeAutocorrectAndCommitSpace):
     * deletes the corrected word + trailing space it committed and retypes
     * exactly what the user originally typed, then a space, so the visible
     * result matches what would be on screen had autocorrect never fired.
     * Also re-learns the original word — one deliberate revert is a much
     * stronger signal than the single autocorrect that preceded it, so it
     * should outweigh that one auto-learned "correction" going forward.
     */
    private fun undoAutocorrect() {
        val undo = autocorrectUndo.value ?: return
        val ic = currentInputConnection ?: return
        val deleteLength = undo.correctedWord.length + if (undo.hadTrailingSpace) 1 else 0
        ic.deleteSurroundingText(deleteLength, 0)
        ic.commitText(undo.originalTyped, 1)
        if (undo.hadTrailingSpace) ic.commitText(" ", 1)
        learnWord(undo.originalTyped, "en")
        autocorrectUndo.value = null
        syncExpectedCursorPosition(ic)
    }

    /**
     * Clears a pending autocorrect-undo chip without acting on it — called
     * from every edit path that isn't undoAutocorrect() itself, so the chip
     * only ever stays visible for the single word it was created for. Kept
     * as its own function (rather than inlining `autocorrectUndo.value =
     * null` everywhere) so every call site reads as an intentional
     * "this edit invalidates any pending undo" rather than looking like
     * unrelated cleanup.
     */
    private fun clearAutocorrectUndoIfAny() {
        if (autocorrectUndo.value != null) autocorrectUndo.value = null
    }

    /**
     * Bridges GestureTypingOverlay's raw swiped-letter sequence to actual
     * word candidates. Called from KeyboardView's onSwipeGesture — see that
     * param's doc comment for why this lives here rather than in the
     * Compose layer (it needs wordRepo + a coroutine context, neither of
     * which the overlay itself has).
     *
     * [language] is whatever GestureTypingOverlay was showing when the
     * swipe happened (currentLanguage.value at drag-start). "si"/"en" score
     * against their one matching dictionary as before. "mix" doesn't have a
     * single fixed dictionary, so instead of bailing out with no
     * candidates it now scores the swipe against *both* dictionaries and
     * merges the results — same subsequence/length heuristic run twice,
     * best matches from either language interleaved by score. This mirrors
     * how mix mode already treats typed (non-gesture) input: both
     * languages are live candidates at once, not a hard either/or choice.
     */
    private suspend fun resolveGestureCandidates(letters: String, language: String): List<String> {
        if (letters.length < 2) return emptyList()

        val languages = if (language == "mix") listOf("si", "en") else listOf(language)
        if (languages.any { it != "si" && it != "en" }) return emptyList()

        // GestureWordMatcher's full path-shape scoring already ran inside
        // GestureTypingOverlay (which has the actual on-screen key
        // coordinates) to reduce the raw touch path down to this plain
        // letter sequence. Ranking here works directly off that string via
        // subsequence/length heuristics instead, which keeps this function
        // independent of Compose/coordinate state — it only ever receives
        // a String from the overlay, never screen positions.
        //
        // For mix mode, each language's dictionary is ranked separately
        // (its own scores aren't comparable across languages purely by
        // number, since e.g. word-length distributions differ), then the
        // two ranked lists are interleaved so the best candidate from
        // whichever language matched more cleanly comes first, rather than
        // one language's whole list always winning over the other's.
        val rankedPerLanguage = languages.mapNotNull { lang ->
            val dictionary = wordRepo.allWords(lang)
            if (dictionary.isEmpty()) null else rankWordsByLetterSequence(letters, dictionary)
        }
        if (rankedPerLanguage.isEmpty()) return emptyList()
        if (rankedPerLanguage.size == 1) return rankedPerLanguage[0].take(5)

        val merged = LinkedHashSet<String>()
        var index = 0
        while (merged.size < 5) {
            var addedAny = false
            for (ranked in rankedPerLanguage) {
                if (index < ranked.size) {
                    merged.add(ranked[index])
                    addedAny = true
                    if (merged.size >= 5) break
                }
            }
            if (!addedAny) break
            index++
        }
        return merged.toList()
    }

    /**
     * Scores every word in [dictionary] against the swiped [letters]
     * sequence and returns the best matches, best first. This is a
     * simpler, coordinate-free companion to GestureWordMatcher's full
     * path-shape scoring (which already ran inside GestureTypingOverlay to
     * reduce the raw touch path down to this letter sequence in the first
     * place) — it doesn't need key positions, only string containment, so
     * it can run here in the service without threading screen coordinates
     * across the Compose/service boundary.
     *
     * Scoring favors words whose letters appear as a subsequence of
     * [letters] in the same relative order (how a real swipe naturally
     * traces a word's letters), then breaks ties by how close the
     * candidate's length is to the swiped sequence's length and by
     * dictionary frequency already baked into [dictionary]'s ordering
     * (WordDao.getAllForLanguage sorts by frequency DESC, and this
     * function's own sortedBy is stable, so equal-scoring words keep that
     * frequency order).
     */
    private fun rankWordsByLetterSequence(letters: String, dictionary: List<String>): List<String> {
        val swiped = letters.lowercase()
        return dictionary
            .mapNotNull { word ->
                val lower = word.lowercase()
                val subsequenceCost = subsequenceGapCost(swiped, lower) ?: return@mapNotNull null
                val lengthPenalty = kotlin.math.abs(lower.length - swiped.length)
                word to (subsequenceCost + lengthPenalty)
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    /**
     * Null if [word]'s letters don't all appear in [swiped] in order (not a
     * plausible match for this swipe at all). Otherwise, the total gap
     * between consecutive matched letters' positions in [swiped] — a small
     * gap means the swipe passed directly from one of the word's letters to
     * the next with nothing else "detected" in between, which is what a
     * clean, accurate swipe for that exact word looks like; a large gap
     * means the swipe path wandered near a lot of other keys along the way,
     * so it's a weaker match even though every letter technically appears
     * in order.
     */
    private fun subsequenceGapCost(swiped: String, word: String): Int? {
        var searchFrom = 0
        var totalGap = 0
        var lastIndex = -1
        for (ch in word) {
            val idx = swiped.indexOf(ch, searchFrom)
            if (idx == -1) return null
            if (lastIndex >= 0) totalGap += (idx - lastIndex - 1).coerceAtLeast(0)
            lastIndex = idx
            searchFrom = idx + 1
        }
        return totalGap
    }

    /**
     * Commits the word GestureTypingOverlay/resolveGestureCandidates
     * resolved a swipe to. Unlike commitPendingWord()/handleSuggestion(),
     * there's no Latin-to-Sinhala transliteration step needed here: gesture
     * matching scores directly against wordRepo's stored words, which for
     * "si" are already real Sinhala-script text (that's what got learned/
     * seeded into the dictionary in the first place), not a Latin buffer
     * awaiting conversion. So this only needs to apply English fancy-text
     * styling where relevant and commit, mirroring handleSuggestion's
     * trailing-space/learn/undo-clearing behaviour for consistency with
     * how picking an ordinary suggestion feels.
     */
    private fun commitGestureWord(word: String) {
        val ic = currentInputConnection ?: return
        clearAutocorrectUndoIfAny()
        wordBuffer.clear()
        englishBuffer.clear()
        resumedWordBeforeCursor = null
        val language = if (isSinhalaTyping()) "si" else "en"
        val styled = if (language == "en") {
            com.spmods.sinkey.keyboard.FancyTextMapper.apply(word, cachedFancyTextStyle)
        } else {
            word
        }
        ic.commitText(styled, 1)
        ic.commitText(" ", 1)
        learnWord(word, language)
        clearSuggestions()
        updateSuggestions()
        syncExpectedCursorPosition(ic)
    }

    /**
     * User picked a suggestion from the strip (spell-checker word, Sinhala
     * transliteration variant, or a word learned from their own typing).
     * After committing it we also add a trailing space, since picking a
     * suggestion means the word is finished and the user will keep typing.
     */
    private fun handleSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        // Picking a suggestion is a separate edit from whatever autocorrect
        // last did — same reasoning as the clearAutocorrectUndoIfAny() call
        // at the top of handleKey.
        clearAutocorrectUndoIfAny()
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
            resumedWordBeforeCursor = null
            // BUG FIX: previously the mix-mode suggestion buckets
            // (mixSinhalaSuggestions/mixEnglishSuggestions) weren't reset
            // here. wordBuffer.clear() alone doesn't stop a still-in-flight
            // spell-checker reply for the just-committed word from later
            // calling recomputeMixSuggestions() and re-showing a stale
            // suggestion for a word that's already been typed — clearing
            // here (via the same helper onUpdateSelection/etc. use) closes
            // that window and also invalidates the in-flight request id.
            clearSuggestions()
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
            resumedWordBeforeCursor = null
            clearSuggestions()
            learnWord(word, "en")
        }
        // Auto-space after applying a suggestion.
        ic.commitText(" ", 1)
        updateAutoShift(ic)
        // Word just finished — offer a next-word prediction instead of
        // leaving the suggestion bar empty.
        updateSuggestions()
        // See onUpdateSelection's doc comment — this edits the field outside
        // handleKey's own sync call, so it needs its own.
        syncExpectedCursorPosition(ic)
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
     * option. On completion the picked image's temp file path is handed to
     * StickerEditActivity, a full-screen Activity of its own — not rendered
     * inline inside the keyboard's Compose tree — so the user can crop/zoom
     * the photo and add a text caption (matching WhatsApp's own sticker
     * maker) on a screen that isn't constrained to the keyboard's docked
     * panel size. See StickerEditActivity's doc comment for why this needs
     * to be its own Activity rather than a pushed Board state.
     */
    fun pickImageForSticker() {
        com.spmods.sinkey.ime.StickerPickerActivity.onImagePicked = { tempPath ->
            if (tempPath != null) {
                val editIntent = android.content.Intent(this, com.spmods.sinkey.ime.StickerEditActivity::class.java).apply {
                    putExtra(com.spmods.sinkey.ime.StickerEditActivity.EXTRA_IMAGE_PATH, tempPath)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(editIntent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this@SinKeyInputMethodService,
                        "Couldn't open the sticker editor — try again",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                android.widget.Toast.makeText(
                    this@SinKeyInputMethodService,
                    "Couldn't read that image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
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
     * Called from KeyboardView when the user taps "Add to stickers" on
     * Board.STICKER_EDIT. Composites [pendingStickerImagePath]'s image with
     * the crop/zoom/text the editor collected, saves it as a sticker, then
     * pops back to Board.STICKER. [fontTypeface] is resolved here (an
     * Android framework type) from the editor's UI-layer StickerFontStyle
     * enum, keeping StickerRepository/StickerFileStore free of any Compose
     * dependency.
     */
    fun saveEditedImageSticker(
        imageScale: Float,
        imageOffsetXFraction: Float,
        imageOffsetYFraction: Float,
        shape: com.spmods.sinkey.keyboard.StickerShape,
        text: String,
        textColor: Int,
        textSizeFraction: Float,
        textXFraction: Float,
        textYFraction: Float,
        fontTypeface: android.graphics.Typeface,
        outlineEnabled: Boolean
    ) {
        val tempPath = pendingStickerImagePath.value ?: return
        pendingStickerImagePath.value = null
        serviceScope.launch {
            val created = stickerRepo.createFromImageEdit(
                sourceFile = java.io.File(tempPath),
                imageScale = imageScale,
                imageOffsetXFraction = imageOffsetXFraction,
                imageOffsetYFraction = imageOffsetYFraction,
                shape = shape,
                text = text,
                textColor = textColor,
                textSizeFraction = textSizeFraction,
                textXFraction = textXFraction,
                textYFraction = textYFraction,
                fontTypeface = fontTypeface,
                outlineEnabled = outlineEnabled
            )
            if (!created) {
                android.widget.Toast.makeText(
                    this@SinKeyInputMethodService,
                    "Couldn't save that sticker",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            // Pop both STICKER_EDIT and STICKER_CREATE, landing back on the
            // sticker tray (Board.STICKER) where the new sticker now shows up.
            val stack = boardStack.value
            boardStack.value = if (stack.size >= 2 &&
                stack.last() == com.spmods.sinkey.keyboard.Board.STICKER_EDIT &&
                stack[stack.size - 2] == com.spmods.sinkey.keyboard.Board.STICKER_CREATE
            ) {
                stack.dropLast(2)
            } else {
                stack.dropLast(1)
            }
        }
    }

    /**
     * Called from KeyboardView when the user taps a sticker.
     *
     * Sends via commitContent using WhatsApp's special sticker mime type
     * "image/webp.wasticker" (not plain "image/webp" — WhatsApp's compose
     * field specifically declares support for the ".wasticker" suffixed
     * type, confirmed by WhatsApp's own sticker API maintainers, and is
     * how Gboard/Bobble/other keyboards deliver stickers straight into an
     * open WhatsApp chat with no picker screen: see
     * https://github.com/WhatsApp/stickers/issues/619). For apps that
     * don't recognise that exact mime type but do support commitContent
     * generally (Telegram, SMS/Messages, etc.), falls back to plain
     * "image/webp". If neither is accepted, falls back further to
     * ACTION_SEND targeted directly at WhatsApp/WhatsApp Business, which
     * opens WhatsApp's own chat picker with the sticker pre-attached.
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
        val sendUri = stickerContentUri(sendPath)

        if (webpExists) {
            // WhatsApp's own sticker mime type — this is what makes the
            // send land silently in the open chat instead of falling
            // through to a picker.
            val sentAsWhatsAppSticker = sendSticker(sendUri, "image/webp.wasticker")
            if (sentAsWhatsAppSticker) return
        }

        val sendMime = if (webpExists) "image/webp" else mimeType
        val sentViaCommitContent = sendSticker(sendUri, sendMime)
        if (sentViaCommitContent) return

        val sentViaShare = shareStickerToWhatsApp(sendUri, sendMime)
        if (!sentViaShare) {
            android.widget.Toast.makeText(
                this,
                "WhatsApp isn't installed, or this field doesn't support stickers",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Launches ACTION_SEND for [uri] targeted directly at whichever of
     * WhatsApp / WhatsApp Business is installed (checked in that order;
     * the first one found wins — most devices only have one). Skips the
     * system share sheet by setting the package explicitly, so the user
     * goes straight into WhatsApp's own chat picker with the sticker
     * pre-attached, instead of picking WhatsApp from a chooser first.
     * Returns false if neither is installed.
     */
    private fun shareStickerToWhatsApp(uri: android.net.Uri, mimeType: String): Boolean {
        val targetPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }
        } ?: return false

        grantUriPermission(targetPackage, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            setPackage(targetPackage)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            startActivity(intent)
            true
        } catch (e: android.content.ActivityNotFoundException) {
            false
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
                clearSuggestions()
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
            //
            // But once raw is long enough to plausibly be a finished (or
            // near-finished) word — 3+ characters — the user almost always
            // wants the full-word transliteration itself (e.g. "sinhal" ->
            // සිංහල) shown, not just short weighted syllable fragments like
            // සි/සී/ශි left over from when the prefix was 1-2 chars. So
            // for longer input, put `primary` first and only fill the
            // remaining slots with weighted candidates, instead of letting
            // weighted candidates fill all 5 slots and crowd primary out.
            val weighted = com.spmods.sinkey.keyboard.SinhalaCandidateMap.candidatesFor(raw)
            if (raw.length >= 3) {
                list.add(primary)
                for (w in weighted) {
                    if (list.size >= 5) break
                    if (!list.contains(w)) list.add(w)
                }
            } else {
                if (weighted.isNotEmpty()) {
                    list.addAll(weighted.take(5))
                }
                if (!list.contains(primary)) list.add(primary)
            }
            val withA = SinhalaTransliterator.transliterate("${raw}a")
            if (!list.contains(withA) && list.size < 5) list.add(withA)
            if (raw.length > 1) {
                val cap = SinhalaTransliterator.transliterate(raw[0].uppercaseChar() + raw.substring(1))
                if (!list.contains(cap) && list.size < 5) list.add(cap)
            }
            // BUG FIX: in mix mode this used to write straight into
            // suggestions.value, which the async English spell-check reply
            // (see onGetSuggestions' mix branch) or the personal-dictionary
            // merge below could then partially or fully overwrite depending
            // on which one landed last — a plain last-write-wins race.
            // Sinhala results now live in their own bucket and get
            // recombined with the English bucket explicitly, so neither
            // source can erase the other's results.
            if (currentLanguage.value == "mix") {
                mixSinhalaSuggestions = list.take(5)
                recomputeMixSuggestions()
            } else {
                suggestions.value = list.take(5)
            }
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
        // BUG FIX: bump the request id so the onGetSuggestions reply for
        // *this* request can be told apart from a still-pending reply to an
        // earlier request that happens to share the same query text (e.g.
        // type "of", backspace to "o", retype "of" — two requests both
        // querying "of", but only the second's reply should count). See the
        // mixEnglishRequestId field comment for the full race this closes.
        mixEnglishRequestId += 1
        val session = spellCheckerSession
        if (session != null) {
            try {
                session.getSuggestions(android.view.textservice.TextInfo(raw), 3)
            } catch (e: Exception) {
                android.util.Log.w("SinKey", "getSuggestions (mix) failed", e)
            }
        } else {
            // No spell-checker available — still surface the raw word itself.
            mixEnglishSuggestions = listOf(raw)
            recomputeMixSuggestions()
        }
        // Also merge in personal-dictionary English words (things the user
        // has typed as English before, in mix mode or pure "en" mode) that
        // start with the same prefix — mirrors the equivalent
        // fetchPersonalSuggestions(primary, "si", ...) call already made for
        // the Sinhala side of mix mode, just above where this is called
        // from. Without this, mix mode only ever offered spell-checker
        // dictionary words for English, never words the user had actually
        // typed and built up frequency for themselves. Routed through the
        // English bucket (not suggestions.value directly) for the same
        // reason as above — keeps it from clobbering the Sinhala bucket.
        fetchPersonalSuggestionsMixEnglish(raw)
    }

    /**
     * Same idea as [fetchPersonalSuggestions] but for mix mode's English
     * bucket specifically: merges learned personal-dictionary English words
     * into mixEnglishSuggestions and recombines via [recomputeMixSuggestions]
     * instead of writing suggestions.value directly, so it can never race
     * against / erase the Sinhala bucket's results.
     */
    private fun fetchPersonalSuggestionsMixEnglish(prefix: String) {
        if (prefix.isEmpty()) return
        val requestId = mixEnglishRequestId
        serviceScope.launch {
            val learned = wordRepo.suggestionsFor(prefix, "en", limit = 5)
            if (learned.isEmpty()) return@launch
            // Stale-reply guard, same reasoning as onGetSuggestions' mix
            // branch: only apply if this is still the most recent request
            // and the buffer hasn't moved on to different text.
            if (requestId != mixEnglishRequestId) return@launch
            if (currentLanguage.value != "mix" || wordBuffer.toString() != prefix) return@launch
            val current = mixEnglishSuggestions.ifEmpty { listOf(prefix) }
            mixEnglishSuggestions = (learned + current).distinct().take(5)
            recomputeMixSuggestions()
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
        clearSuggestions()
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
        // BUG FIX: in mix mode, "si" personal-dictionary results used to be
        // merged straight into suggestions.value, the same shared field the
        // async English mix results (recomputeMixSuggestions) also write
        // to — whichever finished last won, silently dropping the other's
        // suggestions. Route mix-mode Sinhala results through
        // mixSinhalaSuggestions instead, same as the synchronous list
        // higher up in updateSuggestions(), so this can only ever affect
        // the Sinhala half of the merged result.
        val isMixSinhala = language == "si" && currentLanguage.value == "mix"
        serviceScope.launch {
            val learned = if (language == "si") {
                wordRepo.fuzzySuggestionsFor(prefix, language, limit = 5)
            } else {
                wordRepo.suggestionsFor(prefix, language, limit = 5)
            }
            if (learned.isEmpty()) return@launch
            // Personal-dictionary words are the user's own real, previously
            // typed vocabulary — a much stronger signal than the generic
            // transliteration/weighted-candidate guesses in baseList. Put
            // `learned` first so it can't get crowded out of the 5 slots by
            // a baseList that's already full (which happens for any 3+
            // char word since the transliteration fix above), then fill
            // any remaining room with baseList entries not already present.
            if (isMixSinhala) {
                // Stale-reply guard: only apply if mix mode's Sinhala buffer
                // still holds the word this lookup was for.
                if (currentLanguage.value != "mix" || wordBuffer.toString() != prefix) return@launch
                val current = mixSinhalaSuggestions.ifEmpty { baseList }
                mixSinhalaSuggestions = (learned + current).distinct().take(5)
                recomputeMixSuggestions()
            } else {
                val current = suggestions.value.ifEmpty { baseList }
                val merged = (learned + current).distinct().take(5)
                suggestions.value = merged
            }
        }
    }

    /**
     * FIX #1: No more runBlocking. Reads cached in-memory values (updated via
     * Flow collectors in onCreate) — zero blocking, zero DataStore I/O per tap.
     * FIX #3: Key sound now actually implemented using AudioManager.FX_KEYPRESS_STANDARD.
     *
     * Vibration duration/strength now matches FlorisBoard's own defaults
     * exactly (ime/input/InputFeedbackController.kt's keyPress(), and the
     * underlying Vibrator.vibrate(duration, strength, factor) extension in
     * lib/android/.../Vibrator.kt): 50ms at 50% strength, using amplitude
     * control when the device supports it rather than always firing at
     * full/default amplitude regardless of the configured strength. The
     * previous 12ms/DEFAULT_AMPLITUDE was both much shorter and, on
     * amplitude-capable hardware, effectively always "full strength"
     * (DEFAULT_AMPLITUDE ignores any strength setting entirely) — together
     * that read as a single abrupt, same-feeling click no matter how hard
     * or fast you were typing, rather than the fuller, slightly softer tap
     * FlorisBoard (and most other keyboards) actually produce.
     */
    private fun maybeFeedback() {
        if (cachedVibrateEnabled) {
            val vibrator = getSystemService(Vibrator::class.java)
            if (vibrator != null) {
                // FlorisBoard defaults: duration=50ms, strength=50 (of 100).
                val durationMs = 50L
                val strengthPercent = 50
                val amplitude = if (vibrator.hasAmplitudeControl()) {
                    (255.0 * (strengthPercent / 100.0)).toInt().coerceIn(1, 255)
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            }
        }
        if (cachedSoundEnabled) {
            val audio = getSystemService(AudioManager::class.java)
            audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }
}
