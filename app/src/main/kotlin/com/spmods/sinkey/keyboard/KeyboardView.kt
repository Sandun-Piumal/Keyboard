package com.spmods.sinkey.keyboard

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.spmods.sinkey.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.data.FancyTextStyle
import com.spmods.sinkey.data.RemoteUpdateInfo
import com.spmods.sinkey.data.UpdateChecker
import com.spmods.sinkey.data.clipboard.ClipEntity
import com.spmods.sinkey.data.clipboard.ClipRepository
import com.spmods.sinkey.ime.SinKeyInputMethodService
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo

// Number labels for top row keys
private val topRowNumbers = listOf("1","2","3","4","5","6","7","8","9","0")

// Desh Keyboard exact accent color (accentContainer from ManglishLight theme)
internal val DeshGreen = Color(0xFF6E9A65)

// ── Theme-aware color helpers ─────────────────────────────────────────────────

internal data class KeyboardColors(
    val bg: Color,
    val keyBg: Color,
    val specialKeyBg: Color,
    val keyText: Color,
    val specialKeyText: Color,
    val subText: Color,
    val spaceKeyBg: Color,
    val spaceKeyText: Color,
    // Surface colour for card/tile UI that lives *inside* a board — sticker
    // grid tiles, clipboard item cards, font-style preview cards. Unlike
    // keyBg (which intentionally collapses to the same colour as `bg` when
    // showKeyBorders is off, so letter keys read as flat/floating), these
    // cards need a background that's always visibly distinct from the
    // board's own bg, borders-off or not — otherwise the tiles disappear
    // and only their icons/text float on the page. Kept independent of
    // showKeyBorders on purpose.
    val cardBg: Color,
)

@Composable
internal fun keyboardColors(showKeyBorders: Boolean, isDark: Boolean): KeyboardColors {
    return if (isDark) {
        // FIX #12: Dark theme previously had almost no visible difference between
        // bordered (0xFF2E2E2E) and borderless (0xFF262626) key backgrounds.
        // Now borderless uses the same bg colour as the keyboard background so
        // keys appear "flat/floating", while bordered uses a clearly lighter slab.
        KeyboardColors(
            bg             = Color(0xFF1E1E1E),
            keyBg          = if (showKeyBorders) Color(0xFF3A3A3A) else Color(0xFF1E1E1E),
            specialKeyBg   = if (showKeyBorders) Color(0xFF2C2C2C) else Color(0xFF1E1E1E),
            keyText        = Color(0xFFE8E8E8),
            specialKeyText = Color(0xFFCCCCCC),
            subText        = Color(0xFF888888),
            // Borderless: subtle tint (slightly lighter than bg) so the
            // space-bar pill stays visible without a full bordered look.
            spaceKeyBg     = if (showKeyBorders) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
            spaceKeyText   = Color(0xFF777777),
            // Always the same lighter slab used by bordered keys, regardless
            // of showKeyBorders — cards need to stay visible against bg.
            cardBg         = Color(0xFF3A3A3A),
        )
    } else {
        KeyboardColors(
            // keyboard_background_light_bordered = #E6EAED
            bg             = Color(0xFFE6EAED),
            // Bordered: primaryContainer = White. Borderless: same as keyboard
            // bg so keys appear "flat/floating" (mirrors the dark theme fix).
            keyBg          = if (showKeyBorders) Color(0xFFFFFFFF) else Color(0xFFE6EAED),
            // secondaryContainer (functional keys) = #335f9154 overlay on bg.
            // Borderless: flat with keyboard bg, same as keyBg above.
            specialKeyBg   = if (showKeyBorders) Color(0xFFC5CDD5) else Color(0xFFE6EAED),
            // onPrimaryContainer = black
            keyText        = Color(0xFF000000),
            // specialKeyText - dark grey
            specialKeyText = Color(0xFF444444),
            // subText for number hints
            subText        = Color(0xFF666666),
            // space bar bg: bordered = white, borderless = subtle tint
            // (slightly darker than keyboard bg) so the pill stays visible
            spaceKeyBg     = if (showKeyBorders) Color(0xFFFFFFFF) else Color(0xFFDBE0E4),
            // space bar text - medium grey
            spaceKeyText   = Color(0xFF888888),
            // Always white, regardless of showKeyBorders — cards need to
            // stay visible against bg.
            cardBg         = Color(0xFFFFFFFF),
        )
    }
}

/** Convert a 0..3 slider step to a concrete key-row height in dp. */
private fun stepToKeyHeight(step: Float): Dp = when (Math.round(step)) {
    // Desh exact: config_key_height_qwerty = 48dp (default = step 1)
    0    -> 42.dp
    1    -> 48.dp
    2    -> 54.dp
    else -> 62.dp
}

/** Convert a 0..3 slider step to bottom padding in dp. */
private fun stepToBottomPadding(step: Float): Dp = when (Math.round(step)) {
    0    -> 4.dp
    1    -> 10.dp
    2    -> 18.dp
    else -> 28.dp
}

// Board state enum — tracks which keyboard panel is currently visible.
// Replaces the previous ad-hoc boolean flags (showSymbols, showEmojiPicker)
// which had no memory of which board opened them, so back always went to MAIN.
// Must be internal (not private) so SinKeyInputMethodService can reference it.
enum class Board { MAIN, SYMBOLS, NUMPAD, EMOJI, CLIPBOARD, FONT, STICKER, STICKER_CREATE, STICKER_EDIT }

/** Result of Board.STICKER_EDIT's async preview-image decode — see that branch in KeyboardView's content `when`. */
private sealed class StickerEditDecodeResult {
    data class Ok(val bitmap: android.graphics.Bitmap) : StickerEditDecodeResult()
    object Failed : StickerEditDecodeResult()
}

@Composable
internal fun KeyboardView(
    currentLanguage: String,
    keyboardHeight: Float = 2f,
    bottomSpaceEnabled: Boolean = true,
    bottomSpaceSize: Float = 0f,
    showKeyBorders: Boolean = true,
    isDark: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {},
    // Non-null right after a silent autocorrect swapped what the user
    // typed for a spell-checker correction — holds the original typed
    // word so AppsMicBar can show a one-tap "Undo" chip. Owned by the IME
    // service (see SinKeyInputMethodService.autocorrectUndo) so it's
    // cleared consistently from every edit path, the same reasoning as
    // boardStack/shiftState above. Preview callers omit this and get no chip.
    autocorrectUndoWord: String? = null,
    onUndoAutocorrect: () -> Unit = {},
    // Gesture (swipe-to-type) typing — see GestureTypingOverlay/
    // GestureWordMatcher. Off unless swipeTypingEnabled is true (mirrors
    // PreferencesManager.swipeTypingEnabled, collected by the IME service).
    // onSwipeGesture reports the raw swiped letter sequence (already
    // deduped/nearest-key-resolved by GestureTypingOverlay) plus the
    // current typing language, and gets back candidate words ranked best-
    // first — actual dictionary matching happens in the service (see
    // SinKeyInputMethodService.resolveGestureCandidates) since that's
    // where wordRepo/serviceScope already live; the overlay itself has no
    // coroutine scope of its own for DB access. The best candidate is
    // committed automatically; the rest populate the suggestion strip so
    // the user can tap an alternative, the same as typed-word suggestions.
    swipeTypingEnabled: Boolean = false,
    onSwipeGesture: suspend (letters: String, language: String) -> List<String> = { _, _ -> emptyList() },
    onGestureWordCommitted: (String) -> Unit = {},
    onKey: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    inputType: Int = 0,
    // Board stack owned by the IME service so it survives keyboard hide/show cycles.
    // Preview callers (MainActivity) omit these and get default MAIN behaviour.
    boardStack: List<Board> = listOf(Board.MAIN),
    onBoardStackChange: (List<Board>) -> Unit = {},
    // ShiftState owned by the IME service (survives hide/show). Preview callers omit.
    shiftState: SinKeyInputMethodService.ShiftState = SinKeyInputMethodService.ShiftState.OFF,
    onShiftStateChange: (SinKeyInputMethodService.ShiftState) -> Unit = {},
    // Update-banner dismiss state, owned by the IME service so it's reset
    // on every keyboard show (see the field comment in
    // SinKeyInputMethodService.kt) — unlike boardStack/shiftState above,
    // this one is deliberately NOT meant to survive hide/show. Preview
    // callers (MainActivity) omit these and default to "not dismissed".
    dismissedUpdateVersionCode: Int = 0,
    onDismissedUpdateVersionCodeChange: (Int) -> Unit = {},
    // Sticker board (Board.STICKER / STICKER_CREATE). onStickerSend is
    // called with (filePath, mimeType) — see
    // SinKeyInputMethodService.onStickerSelected for what each means.
    // Preview callers (MainActivity) omit this and get a no-op.
    onStickerSend: (String, String) -> Unit = { _, _ -> },
    // Triggers the system gallery picker for Board.STICKER_CREATE's Image
    // Sticker option — wired to SinKeyInputMethodService.pickImageForSticker,
    // since only the service can start the trampoline Activity needed here.
    onPickStickerImage: () -> Unit = {},
    // Board.STICKER_EDIT state: the temp file path of the image most
    // recently picked via onPickStickerImage, hoisted in the service (same
    // reasoning as boardStack — must survive hide/show while the editor is
    // open). Null when no image sticker is currently being edited.
    pendingStickerImagePath: String? = null,
    // Called when the user taps "Add to stickers" on Board.STICKER_EDIT,
    // wired to SinKeyInputMethodService.saveEditedImageSticker.
    onSaveImageSticker: (ImageStickerDraft) -> Unit = {}
) {
    val colors = keyboardColors(showKeyBorders, isDark)
    val keyHeight = stepToKeyHeight(keyboardHeight)
    val bottomPadding = if (bottomSpaceEnabled) stepToBottomPadding(bottomSpaceSize) else 4.dp
    val keyShape = RoundedCornerShape(6.dp)

    // shift = true whenever shiftState is ONE_SHOT or LOCKED
    val shift = shiftState != SinKeyInputMethodService.ShiftState.OFF
    val shiftLocked = shiftState == SinKeyInputMethodService.ShiftState.LOCKED

    var showLangTooltip by remember { mutableStateOf(false) }

    // Formula-derived height of MainKeyboardKeys' content, in dp. Boards that
    // hide the toolbar/emoji-row and want to fill that reclaimed space
    // exactly (CLIPBOARD, FONT) read this so they always render at exactly
    // the same height as MAIN.
    //
    // This used to be a *measured* value that started at a guessed fallback
    // (keyHeight * 4 + 40.dp) and only became accurate after MAIN had been
    // composed once and reported its real size via onGloballyPositioned. If
    // the user opened Font/Clipboard first in a session — e.g. right when
    // the keyboard popped up, before ever touching MAIN — they'd get the
    // wrong guessed height, visibly mismatching the main board. Deriving it
    // directly from the same row-height + padding formula MainKeyboardKeys
    // itself uses removes that race entirely: it's correct on frame one,
    // every time.
    //
    // MainKeyboardKeys layout: Column(vertical padding 2.dp top + bottom,
    // plus bottomPadding) containing 4 rows of height keyHeight, each row
    // padded 3.dp top+bottom.
    val measuredMainContentHeight = (keyHeight + 6.dp) * 4 + 4.dp + bottomPadding

    val currentBoard = boardStack.last()
    fun pushBoard(b: Board) { onBoardStackChange(boardStack + b) }
    fun popBoard()          { if (boardStack.size > 1) onBoardStackChange(boardStack.dropLast(1)) }

    val isPhoneInput = remember(inputType) {
        (inputType and android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE) ==
            android.view.inputmethod.EditorInfo.TYPE_CLASS_PHONE
    }

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val recentEmojis by prefsManager.recentEmojis.collectAsState(initial = emptyList())
    val clipRepository = remember { ClipRepository(context) }
    val clipHistory by clipRepository.history.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val selectedFontKey by prefsManager.keyboardFont.collectAsState(initial = FancyTextStyle.NONE.key)
    val stickerRepository = remember { com.spmods.sinkey.data.sticker.StickerRepository(context) }
    val ownStickers by stickerRepository.all.collectAsState(initial = emptyList())
    val favouriteStickers by stickerRepository.favourites.collectAsState(initial = emptyList())

    // ── Update check ────────────────────────────────────────────────────────
    // Fetched once per keyboard-composition (not on every recomposition —
    // `Unit` as the LaunchedEffect key means this block runs exactly once
    // for the lifetime of this composable instance, same as the keyboard
    // being shown once per IME session). Any failure inside UpdateChecker
    // is already swallowed there and returns null, so this can't crash or
    // show an error — it just silently stays "no update" on failure.
    var remoteUpdate by remember { mutableStateOf<RemoteUpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        remoteUpdate = UpdateChecker.checkForUpdate(context)
    }
    // dismissedUpdateVersionCode comes from the IME service (see its param
    // doc above) — NOT DataStore/PreferencesManager, because dismissal here
    // must be undone every time the keyboard is reopened, not persisted
    // forever like a normal user preference.
    val showUpdateBanner = remoteUpdate != null && remoteUpdate!!.versionCode > dismissedUpdateVersionCode

    LaunchedEffect(showLangTooltip) {
        if (showLangTooltip) {
            delay(1500)
            showLangTooltip = false
        }
    }

    // ONE Column for the whole keyboard — toolbar always at top, content below
    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(colors.bg)
        ) {
            // ── Toolbar (always visible, never re-created on pad switch,
            // except for the Emoji board — which moves its own category
            // tabs to the very top instead, in place of this toolbar) ──────
            if (currentBoard != Board.EMOJI && currentBoard != Board.CLIPBOARD && currentBoard != Board.FONT &&
                currentBoard != Board.STICKER && currentBoard != Board.STICKER_CREATE) {
                AppsMicBar(
                    colors = colors,
                    isDark = isDark,
                    suggestions = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD) emptyList() else suggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    autocorrectUndoWord = if (isPhoneInput || currentBoard == Board.SYMBOLS || currentBoard == Board.NUMPAD) null else autocorrectUndoWord,
                    onUndoAutocorrect = onUndoAutocorrect,
                    onKey = onKey,
                    onClipboardOpen = { pushBoard(Board.CLIPBOARD) },
                    onFontOpen = { pushBoard(Board.FONT) },
                    onStickerOpen = { pushBoard(Board.STICKER) },
                    selectedFontStyle = FancyTextStyle.fromKey(selectedFontKey)
                )
            }

            // ── Recent emoji row — shown above whichever board is active,
            // except the Emoji board itself (which has its own Recent tab).
            // When an update is available and hasn't been dismissed, this
            // strip is TEMPORARILY replaced by the update banner instead
            // (same slot, same 44dp height, so nothing else on the board
            // shifts or resizes) — the recent-emoji strip itself is not
            // shown at all while the banner is up.
            if (!isPhoneInput && currentBoard != Board.EMOJI && currentBoard != Board.CLIPBOARD && currentBoard != Board.FONT &&
                currentBoard != Board.STICKER && currentBoard != Board.STICKER_CREATE) {
                if (showUpdateBanner) {
                    UpdateBanner(
                        colors = colors,
                        onOpenClick = {
                            openUrl(context, remoteUpdate!!.url)
                        },
                        onDismissClick = {
                            onDismissedUpdateVersionCodeChange(remoteUpdate!!.versionCode)
                        }
                    )
                } else if (recentEmojis.isNotEmpty()) {
                    EmojiRow(
                        emojis = recentEmojis,
                        colors = colors,
                        onKey = onKey,
                        onMoreClick = { pushBoard(Board.EMOJI) }
                    )
                }
            }

            // ── Content area — only this part switches ────────────────────────
            when {
                isPhoneInput -> PhoneDialPadKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding, onKey = onKey
                )
                currentBoard == Board.EMOJI -> EmojiPickerView(
                    recentEmojis = recentEmojis,
                    colors = colors,
                    keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    onEmojiSelected = { emoji -> onKey(emoji) },
                    onBackspace = { onKey("BACKSPACE") },
                    onDismiss = { popBoard() }          // back to whichever board opened emoji
                )
                currentBoard == Board.SYMBOLS -> SymbolsKeyboardKeys(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding,
                    onKey = onKey,
                    onBack = { popBoard() },            // back to MAIN
                    onNumpad = { pushBoard(Board.NUMPAD) },
                    onEmoji  = { pushBoard(Board.EMOJI) }
                )
                currentBoard == Board.NUMPAD -> NumberPadView(
                    colors = colors, keyHeight = keyHeight,
                    keyShape = keyShape, bottomPadding = bottomPadding,
                    onKey = onKey,
                    onBack = { popBoard() },
                    onBoardStackChange = onBoardStackChange
                )
                currentBoard == Board.CLIPBOARD -> ClipboardHistoryView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    history = clipHistory,
                    onPaste = { text -> onKey("PASTE_TEXT:$text") },
                    onTogglePin = { text, pinned ->
                        coroutineScope.launch { clipRepository.setPinned(text, pinned) }
                    },
                    onDelete = { text ->
                        coroutineScope.launch { clipRepository.delete(text) }
                    },
                    onClearAll = {
                        coroutineScope.launch { clipRepository.clearUnpinned() }
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.FONT -> FontPickerView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    selectedFontKey = selectedFontKey,
                    onFontSelected = { fontKey ->
                        coroutineScope.launch { prefsManager.setKeyboardFont(fontKey) }
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER -> StickerBoardView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    ownStickers = ownStickers,
                    favouriteStickers = favouriteStickers,
                    onSendOwnSticker = { filePath -> onStickerSend(filePath, "image/png") },
                    onToggleFavourite = { filePath, fav ->
                        coroutineScope.launch { stickerRepository.setFavourite(filePath, fav) }
                    },
                    onDeleteSticker = { filePath ->
                        coroutineScope.launch { stickerRepository.delete(filePath) }
                    },
                    onCreateClick = { pushBoard(Board.STICKER_CREATE) },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER_CREATE -> StickerCreateView(
                    colors = colors, keyHeight = keyHeight,
                    bottomPadding = bottomPadding,
                    targetContentHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp),
                    onPickImageRequested = onPickStickerImage,
                    onTextSubmitted = { text, textColor ->
                        coroutineScope.launch { stickerRepository.createFromText(text, textColor) }
                        popBoard()
                    },
                    onBack = { popBoard() }
                )
                currentBoard == Board.STICKER_EDIT -> {
                    // Decoding a picked photo (even downscaled) is real I/O +
                    // CPU work — doing it synchronously inside remember{}
                    // blocks Compose's composition phase on the UI thread,
                    // which can freeze the keyboard badly enough to look
                    // exactly like "selecting an image does nothing" (no
                    // crash, no toast, just a stuck/unresponsive picker).
                    // produceState runs the decode in a coroutine instead,
                    // so composition itself stays fast and a loading state
                    // can be shown while it works.
                    //
                    // Three distinct states are tracked (not just "bitmap or
                    // null") so a genuine decode failure can show feedback
                    // and a way back, instead of either an infinite spinner
                    // or a silent pop that looks like nothing happened:
                    //   null    == still decoding (or not started)
                    //   Ok      == decoded successfully
                    //   Failed  == decode finished and returned null
                    val previewState = androidx.compose.runtime.produceState<StickerEditDecodeResult?>(
                        initialValue = null,
                        key1 = pendingStickerImagePath
                    ) {
                        val path = pendingStickerImagePath
                        value = if (path == null) {
                            null
                        } else {
                            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.spmods.sinkey.data.sticker.StickerFileStore.decodePreviewBitmap(java.io.File(path))
                            }
                            if (bitmap != null) StickerEditDecodeResult.Ok(bitmap) else StickerEditDecodeResult.Failed
                        }
                    }
                    val editHeight = measuredMainContentHeight + 48.dp +
                        (if (!isPhoneInput && (showUpdateBanner || recentEmojis.isNotEmpty())) 44.dp else 0.dp)

                    when (val result = previewState.value) {
                        is StickerEditDecodeResult.Ok -> StickerImageEditorView(
                            colors = colors,
                            bottomPadding = bottomPadding,
                            targetContentHeight = editHeight,
                            imageBitmap = result.bitmap,
                            onSave = { draft -> onSaveImageSticker(draft) },
                            onBack = { popBoard() }
                        )
                        StickerEditDecodeResult.Failed -> Box(
                            modifier = Modifier.fillMaxWidth().height(editHeight).background(colors.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Couldn't read that image",
                                    fontSize = 13.sp,
                                    color = colors.subText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBg)
                                        .clickable { popBoard() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(text = "Go back", fontSize = 13.sp, color = colors.keyText)
                                }
                            }
                        }
                        null -> if (pendingStickerImagePath == null) {
                            // Nothing to edit at all (shouldn't normally
                            // happen — STICKER_EDIT is only ever pushed
                            // together with setting this) — back out safely
                            // instead of showing a permanently blank board.
                            LaunchedEffect(Unit) { popBoard() }
                        } else {
                            // Genuinely still decoding.
                            Box(
                                modifier = Modifier.fillMaxWidth().height(editHeight).background(colors.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = DeshGreen)
                            }
                        }
                    }
                }
                else -> Box {
                    // Key-center map built up as MainKeyboardKeys lays out
                    // each letter key (see LetterKey/NumberedLetterKey's
                    // onPositioned) — GestureTypingOverlay reads this once
                    // a swipe finishes to translate the touch path into a
                    // letter sequence. Only actually collected when gesture
                    // typing is on (onKeyPositioned below is null otherwise),
                    // so this costs nothing when the feature is off.
                    val keyPositions = remember { mutableStateMapOf<Char, KeyPoint>() }

                    MainKeyboardKeys(
                        currentLanguage = currentLanguage,
                        shift = shift, shiftLocked = shiftLocked,
                        onShiftStateChange = onShiftStateChange,
                        keyHeight = keyHeight, keyShape = keyShape,
                        bottomPadding = bottomPadding, colors = colors,
                        showKeyBorders = showKeyBorders,
                        onKey = onKey,
                        onSymbols = { pushBoard(Board.SYMBOLS) },
                        onEmojiPicker = { pushBoard(Board.EMOJI) },
                        onLangTooltip = { showLangTooltip = true },
                        imeAction = inputType,
                        onKeyPositioned = if (swipeTypingEnabled) { { ch, coords ->
                            // coords.size is an IntSize (width/height only,
                            // no built-in .center without an extra import)
                            // — compute the center manually instead.
                            val centerX = coords.size.width / 2f
                            val centerY = coords.size.height / 2f
                            val positionInParent = coords.positionInParent()
                            keyPositions[ch] = KeyPoint(ch, positionInParent.x + centerX, positionInParent.y + centerY)
                        } } else null
                    )

                    if (swipeTypingEnabled) {
                        GestureTypingOverlay(
                            keyPositions = keyPositions,
                            currentLanguage = currentLanguage,
                            onSwipeGesture = onSwipeGesture,
                            onWordCommitted = onGestureWordCommitted
                        )
                    }
                }
            }
        }

        // FIX #11: Replaced hardcoded padding(start=96.dp, bottom=62.dp) which was
        // wrong on different keyboard heights / bottom-space settings. Now the tooltip
        // is centered horizontally and floats just above the bottom row by using
        // Alignment.BottomCenter + a single fixed vertical nudge that works at every
        // keyboard height step (the bottom row is always keyHeight + 6dp padding).
        AnimatedVisibility(
            visible = showLangTooltip,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = keyHeight + 10.dp)
                .zIndex(10f)
        ) {
            LangTooltip(currentLanguage = currentLanguage)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// Content-only composables (no toolbar — toolbar is in KeyboardView)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MainKeyboardKeys(
    currentLanguage: String,
    shift: Boolean,
    shiftLocked: Boolean,
    onShiftStateChange: (SinKeyInputMethodService.ShiftState) -> Unit,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    colors: KeyboardColors,
    showKeyBorders: Boolean = true,
    onKey: (String) -> Unit,
    onSymbols: () -> Unit,
    onEmojiPicker: () -> Unit,
    onLangTooltip: () -> Unit,
    imeAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_NONE,
    // Reports every letter key's on-screen center (in this Column's local
    // coordinate space) as they're laid out — GestureTypingOverlay collects
    // these into the key-position map GestureWordMatcher needs. null
    // (the default) skips all the onGloballyPositioned work entirely, so
    // this has no cost when gesture typing is off (see KeyboardView's own
    // swipeTypingEnabled gate, which is what actually decides whether a
    // non-null callback is passed in).
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // FIX #5: Pass onShiftChange so letter rows can reset one-shot shift.
        NumberedKeyRow(EnglishRows[0], topRowNumbers, shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            onKey = { onKey(it); if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF) })
        KeyRow(EnglishRows[1], shift, keyHeight, colors, keyShape,
            onKeyPositioned = onKeyPositioned,
            onKey = { onKey(it); if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF) })
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Double-tap = LOCKED, single tap = toggle ONE_SHOT
            ShiftKey(weight = 1.4f, active = shift, locked = shiftLocked,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onTap = { onKey("SHIFT") },
                onDoubleTap = { onKey("SHIFT_LOCK") }
            )
            EnglishRows[2].forEach { k ->
                val display = if (shift) k.uppercase() else k
                // FIX #5: Reset shift after each letter (one-shot shift behaviour).
                LetterKey(
                    label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                    onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } }
                ) {
                    onKey(display)
                    if (shift && !shiftLocked) onShiftStateChange(SinKeyInputMethodService.ShiftState.OFF)
                }
            }
            BackspaceKey(weight = 1.4f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SymbolsKey(weight = 1.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onSymbols() }
            EmojiKey(weight = 0.9f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onTap = { onKey(",") }, onLongPress = { onEmojiPicker() })
            Box(modifier = Modifier.weight(0.9f)) {
                LangToggleKey(currentLanguage = currentLanguage, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape,
                    onTap = { onKey("LANG_TOGGLE"); onLangTooltip() })
            }
            SpaceKey(weight = 5.5f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                showKeyBorders = showKeyBorders,
                onTap = { onKey("SPACE") }, onLongPress = { onKey("SWITCH_KEYBOARD") })
            SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) { onKey(".") }
            // FIX #8: Pass imeAction so the Enter key label reflects the current field action.
            EnterKey(weight = 2.0f, keyHeight = keyHeight, keyShape = keyShape,
                colors = colors, showKeyBorders = showKeyBorders,
                imeAction = imeAction) { onKey("ENTER") }
        }
    }
}

/**
 * Transparent layer drawn over MainKeyboardKeys' letter rows that turns a
 * finger-drag across the letters into a committed word (gesture / swipe
 * typing). Deliberately a separate sibling Box rather than logic bolted
 * onto LetterKey itself: LetterKey already owns a pointerInput+clickable
 * pair for tap/press-preview, and layering a second, independent drag
 * recognizer directly on the same keys risks the two fighting over the
 * same pointer events. As a sibling overlay, this only starts consuming
 * the gesture once a drag is already unambiguous (see the touchSlop
 * threshold in the drag handler below), by which point it's clearly not a
 * tap — a plain tap still reaches the LetterKey underneath untouched,
 * since a drag that never exceeds slop is never claimed here at all.
 *
 * Visible trail: draws the swiped path with a fading, colour-shifting
 * stroke while dragging (see traiLColor/strokeWidth below) — purely
 * cosmetic feedback, cleared as soon as the gesture ends.
 */
@Composable
private fun GestureTypingOverlay(
    keyPositions: Map<Char, KeyPoint>,
    currentLanguage: String,
    onSwipeGesture: suspend (letters: String, language: String) -> List<String>,
    onWordCommitted: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var pathPoints by remember { mutableStateOf(listOf<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(keyPositions, currentLanguage) {
                // Only a currentLanguage of "si" or "en" makes sense for
                // gesture matching (mix mode doesn't have a single fixed
                // dictionary to score against) — mix mode swipes fall
                // through as an ordinary drag with no word committed,
                // which detectDragGestures still handles safely (an empty
                // onSwipeGesture result below just produces no candidates).
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        pathPoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        pathPoints = pathPoints + change.position
                    },
                    onDragEnd = {
                        isDragging = false
                        val finishedPath = pathPoints
                        pathPoints = emptyList()
                        if (finishedPath.size >= MIN_GESTURE_POINTS && keyPositions.isNotEmpty()) {
                            val swipeKeyPoints = finishedPath.map { KeyPoint(' ', it.x, it.y) }
                            val letters = nearestLettersAlongPath(swipeKeyPoints, keyPositions)
                            if (letters.length >= MIN_GESTURE_LETTERS) {
                                scope.launch {
                                    val candidates = onSwipeGesture(letters, currentLanguage)
                                    candidates.firstOrNull()?.let { onWordCommitted(it) }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        pathPoints = emptyList()
                    }
                )
            }
    ) {
        if (isDragging && pathPoints.size >= 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pathPoints.first().x, pathPoints.first().y)
                    pathPoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = DeshGreen.copy(alpha = 0.55f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 12f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Reduces a raw swipe path down to the sequence of letters it actually
 * passed near, in order, with consecutive duplicates collapsed — e.g. a
 * path that lingers over "e" for several sampled points only contributes
 * one 'e', matching how GestureWordMatcher.idealPath also collapses
 * consecutive-duplicate letters in candidate words. This is a coarse first
 * pass (nearest key at each sample point); the real ranking happens in
 * GestureWordMatcher.match against the full path shape, not just this
 * letter sequence — this function only needs to be good enough to build a
 * plausible query string.
 */
private fun nearestLettersAlongPath(path: List<KeyPoint>, keyPositions: Map<Char, KeyPoint>): String {
    val sb = StringBuilder()
    var lastChar: Char? = null
    for (point in path) {
        var closestChar: Char? = null
        var closestDist = Float.MAX_VALUE
        for ((ch, key) in keyPositions) {
            val dx = point.x - key.x
            val dy = point.y - key.y
            val dist = dx * dx + dy * dy
            if (dist < closestDist) {
                closestDist = dist
                closestChar = ch
            }
        }
        if (closestChar != null && closestChar != lastChar) {
            sb.append(closestChar)
            lastChar = closestChar
        }
    }
    return sb.toString()
}

private const val MIN_GESTURE_POINTS = 6
private const val MIN_GESTURE_LETTERS = 2

@Composable
private fun SymbolsKeyboardKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onNumpad: () -> Unit,
    onEmoji: () -> Unit
) {
    SymbolsKeyboardView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey, onBack = onBack,
        onNumpad = onNumpad, onEmoji = onEmoji
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadKeys(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors, keyHeight = keyHeight, keyShape = keyShape,
        bottomPadding = bottomPadding, onKey = onKey
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar — tools row OR suggestion strip depending on typing state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppsMicBar(
    colors: KeyboardColors,
    isDark: Boolean = false,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    // Non-null right after an autocorrect swap — renders as a distinct
    // "Undo" chip pinned before the regular suggestions. See KeyboardView's
    // doc comment on this same param for ownership/lifecycle.
    autocorrectUndoWord: String? = null,
    onUndoAutocorrect: () -> Unit = {},
    onKey: (String) -> Unit,
    onClipboardOpen: () -> Unit,
    onFontOpen: () -> Unit,
    onStickerOpen: () -> Unit,
    selectedFontStyle: FancyTextStyle = FancyTextStyle.NONE
) {
    // The undo chip must be able to show the strip even when there are no
    // regular suggestions to go with it — right after autocorrect commits a
    // word at SPACE, the word buffer (and therefore `suggestions`) is
    // usually empty, but the chip describing what was just corrected still
    // needs somewhere to render.
    val isTyping = suggestions.isNotEmpty() || autocorrectUndoWord != null

    // AnimatedContent was removed here — the slide/fade transition caused the
    // toolbar to render twice during WhatsApp emoji panel open/close because
    // the IME window resizes mid-animation, leaving ghost frames visible.
    //
    // FIX Ghost-Toolbar: Both branches MUST use the same fixed height (48.dp).
    // The previous code used 52.dp for suggestion strip and 44.dp for tool row.
    // That 8dp height difference caused the IME window to resize every time
    // typing started/stopped, which triggered WhatsApp's emoji panel to overlap
    // the keyboard producing the visible ghost/duplicate toolbar layer.
    if (isTyping) {
            // ── Suggestion strip ─────────────────────────────────────────
            // FIX: The grid/apps icon (TOOL_APPS) used to disappear entirely
            // while typing because this whole branch replaced the tools row.
            // Now it stays pinned on the left of the suggestion strip so the
            // user can always reach it without needing to stop typing first.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onKey("TOOL_APPS") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_unified_menu),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colors.subText
                    )
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    if (autocorrectUndoWord != null) {
                        item {
                            // Distinct pill (not a plain text chip like the
                            // suggestions below) so it visually reads as an
                            // action rather than a word to insert — tapping
                            // it reverts the correction rather than typing
                            // autocorrectUndoWord into the field.
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.subText.copy(alpha = 0.12f))
                                    .clickable { onUndoAutocorrect() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = colors.keyText
                                )
                                Text(
                                    text = "\"$autocorrectUndoWord\"",
                                    fontSize = 15.sp,
                                    color = colors.keyText,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        if (suggestions.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(colors.subText.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                    items(suggestions.size) { idx ->
                        val word = suggestions[idx]
                        // Fancy-font styling is applied to the suggestion chip's
                        // display text too, not just to committed text — otherwise
                        // a user with e.g. Bold font selected never sees that a
                        // chip like the raw-English "mix mode" word ("ba") will
                        // actually commit as its styled form ("𝐛𝐚") until after
                        // they've already tapped it. FancyTextMapper only maps
                        // a-z/A-Z/0-9, so Sinhala suggestion chips pass through
                        // completely unchanged — safe to apply unconditionally.
                        val displayWord = FancyTextMapper.apply(word, selectedFontStyle)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSuggestionSelected(word) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = displayWord, fontSize = 19.sp, color = colors.keyText, maxLines = 1)
                        }
                        if (idx < suggestions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(1.dp)
                                    .background(colors.subText.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        } else {
            // ── Full tools row ───────────────────────────────────────────
            val tools = listOf(
                R.drawable.ic_unified_menu  to "TOOL_APPS",
                R.drawable.ic_sticker       to "TOOL_STICKER",
                R.drawable.ic_clipboard     to "TOOL_CLIPBOARD",
                R.drawable.ic_custom_font   to "TOOL_FONT",
                R.drawable.ic_translation   to "TOOL_TRANSLATE",
                R.drawable.ic_settings      to "TOOL_SETTINGS"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (iconRes, action) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                // TOOL_CLIPBOARD / TOOL_FONT open their own boards (handled by
                                // the parent KeyboardView) instead of going through onKey.
                                // TOOL_CLIPBOARD previously instantly pasted only whatever was
                                // on the system clipboard *right now*, with no way to reach
                                // anything copied earlier. TOOL_FONT previously did nothing at
                                // all (logged "not yet implemented"). Other tool actions still
                                // go through onKey as before.
                                when (action) {
                                    "TOOL_CLIPBOARD" -> onClipboardOpen()
                                    "TOOL_FONT" -> onFontOpen()
                                    "TOOL_STICKER" -> onStickerOpen()
                                    else -> onKey(action)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = colors.subText
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        // Mic button keeps its pill background regardless of
                        // showKeyBorders — it's a distinct action button, not
                        // a regular key, so it shouldn't go flat/invisible.
                        .background(
                            if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
                        )
                        .clickable { onKey("TOOL_MIC") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_microphone_normal),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colors.subText
                    )
                }
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emoji row — recent-emoji strip shown above the main typing keyboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmojiRow(emojis: List<String>, colors: KeyboardColors, onKey: (String) -> Unit, onMoreClick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(colors.bg).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        state = rememberLazyListState()
    ) {
        item { Spacer(modifier = Modifier.width(4.dp)) }
        items(emojis.size) { index ->
            val emoji = emojis[index]
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onKey(emoji) },
                contentAlignment = Alignment.Center
            ) { Text(text = emoji, fontSize = 22.sp) }
        }
        item {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) { Text(text = "•••", fontSize = 14.sp, color = colors.subText) }
        }
        item { Spacer(modifier = Modifier.width(4.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Update banner — temporarily replaces the recent-emoji strip above the main
// keyboard when UpdateChecker finds a newer versionCode in the remote JSON.
// Sized identically to EmojiRow (44dp total: 40dp content + 2dp top/bottom
// padding) so swapping between the two never shifts or resizes anything
// else on the board.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UpdateBanner(
    colors: KeyboardColors,
    onOpenClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 2.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialKeyBg)
            .clickable { onOpenClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎉 New update available — tap to download",
            fontSize = 13.sp,
            color = colors.keyText,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDismissClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.subText
            )
        }
    }
}

/**
 * Opens [url] in the device's default browser. Launched from a Service
 * context (the IME, not an Activity), so FLAG_ACTIVITY_NEW_TASK is required
 * — without it, starting an Activity from a non-Activity context throws
 * android.util.AndroidRuntimeException at runtime.
 */
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("KeyboardView", "Could not open update URL: $url", e)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LangTooltip(currentLanguage: String) {
    val label = when (currentLanguage) {
        "en" -> "English enabled"
        "mix" -> "Mix (සිංහල+English) enabled"
        else -> "සිංහල enabled"
    }
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(if (currentLanguage == "mix") Color.Black else DeshGreen)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓ ", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = when (currentLanguage) {
                    "en" -> buildAnnotatedStringBold("English", " enabled")
                    "mix" -> buildAnnotatedStringBold("Mix", " (සිංහල+English) enabled")
                    else -> buildAnnotatedStringBold("සිංහල", " enabled")
                },
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun buildAnnotatedStringBold(bold: String, normal: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
        append(bold)
        pop()
        append(normal)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Key rows
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberedKeyRow(
    keys: List<String>,
    numbers: List<String>,
    shift: Boolean,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    // (keyChar, coordinates) -> Unit — see LetterKey's onPositioned doc.
    // keyChar is the *unshifted* lowercase char, since GestureWordMatcher
    // always matches against lowercase candidate words.
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEachIndexed { index, k ->
            val display = if (shift) k.uppercase() else k
            val num = numbers.getOrNull(index) ?: ""
            NumberedLetterKey(
                label = display, number = num, weight = 1f,
                keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } },
                onTap = { onKey(display) },
                onLongPress = { onKey(num) }
            )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    shift: Boolean,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onKeyPositioned: ((Char, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.weight(0.5f))
        keys.forEach { k ->
            val display = if (shift) k.uppercase() else k
            LetterKey(
                label = display, weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape,
                onPositioned = onKeyPositioned?.let { cb -> { coords: androidx.compose.ui.layout.LayoutCoordinates -> cb(k.lowercase().firstOrNull() ?: ' ', coords) } }
            ) { onKey(display) }
        }
        Box(modifier = Modifier.weight(0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual keys
// ─────────────────────────────────────────────────────────────────────────────

// Bug 3 Fix: Key label font sizes were hardcoded to 22sp regardless of the
// keyboard height setting. Small keys (42dp) look cramped at 22sp and large
// keys (62dp) have too much empty space. Scale font proportionally to keyHeight.
private fun keyLabelFontSize(keyHeight: Dp): androidx.compose.ui.unit.TextUnit =
    (keyHeight.value * 0.50f).sp   // ~21sp @ 42dp, ~24sp @ 48dp, ~27sp @ 54dp, ~31sp @ 62dp

private fun keyNumberFontSize(keyHeight: Dp): androidx.compose.ui.unit.TextUnit =
    (keyHeight.value * 0.25f).sp   // ~10sp @ 42dp, ~12sp @ 48dp, ~15sp @ 62dp

/**
 * Animates a quick "bump" scale for key-press feedback: snaps down fast on press,
 * springs back up with a slight overshoot on release.
 */
@Composable
private fun rememberKeyBumpScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = if (pressed) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh * 1.5f)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "keyBumpScale"
    )
    return scale
}

@Composable
private fun KeyPreviewPopup(label: String, keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape) {
    val size = (keyHeight.value * 1.1f).dp
    // Real keyboards (Gboard/SwiftKey) pop this bubble in with a quick
    // scale+fade rather than having it snap fully-formed into place, which
    // reads as more "alive"/responsive even though the key itself already
    // has its own bump animation. Animate from freshly-composed (this
    // popup is created and destroyed each press, so `remember`ing false
    // and flipping true right after first composition via LaunchedEffect
    // is what actually triggers the transition — starting `true`
    // immediately would skip it, since animateFloatAsState only animates
    // on subsequent target changes).
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val animScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "keyPreviewScale"
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyPreviewAlpha"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animScale
                scaleY = animScale
                alpha = animAlpha
                // Scale from the bottom edge (closest to the key/finger)
                // rather than dead-center, so the bubble reads as growing
                // up out of the key instead of expanding from its own
                // middle — matches how real keyboard preview bubbles move.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(keyShape)
            .background(colors.keyBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (keyHeight.value * 0.60f).sp,
            color = colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.NumberedLetterKey(
    label: String, number: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
    ) {
        Text(text = number, fontSize = keyNumberFontSize(keyHeight), color = colors.subText,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp))
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center))
        if (pressed) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@Composable
private fun RowScope.LetterKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    // Reports this key's center in the root keyboard's coordinate space
    // whenever it's laid out/moved — used only by GestureTypingOverlay
    // (see KeyboardKeys' gestureKeyPositions state) to build the key-center
    // map GestureWordMatcher needs. Left null everywhere gesture typing
    // isn't relevant (symbol/numpad keys, previews) so this stays a no-op
    // there instead of needing every LetterKey call site updated.
    onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .let { m -> if (onPositioned != null) m.onGloballyPositioned(onPositioned) else m }
            // clickable handles the actual tap/long-press logic reliably.
            // pointerInput only tracks down/up for the visual pressed state.
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), color = colors.keyText,
            fontWeight = FontWeight.Normal)
        if (pressed) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.ShiftKey(
    weight: Float, active: Boolean, locked: Boolean,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    val bg = when {
        locked -> DeshGreen.copy(alpha = 0.85f)
        active -> colors.specialKeyBg.copy(alpha = 0.7f)
        else   -> colors.specialKeyBg
    }
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape).background(bg)
            .combinedClickable(
                onClick = onTap,
                onDoubleClick = onDoubleTap
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (active) R.drawable.ic_shift_key_shifted else R.drawable.ic_shift_key),
            contentDescription = if (locked) "Caps Lock" else "Shift",
            modifier = Modifier.size(26.dp),
            tint = if (active || locked) Color.White else colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape).background(colors.specialKeyBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        pressed = true
                        val longPressDelay = 400L
                        val repeatInterval = 50L
                        val released = withTimeoutOrNull(longPressDelay) { tryAwaitRelease() }
                        if (released != null) {
                            onTap()
                        } else {
                            onTap()
                            try {
                                while (true) {
                                    delay(repeatInterval)
                                    onTap()
                                    val done = withTimeoutOrNull(1L) { tryAwaitRelease() }
                                    if (done != null) break
                                }
                            } catch (_: Exception) { }
                        }
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_backspace),
            contentDescription = "Backspace",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun RowScope.SpecialKey(
    label: String, weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = keyLabelFontSize(keyHeight), fontWeight = FontWeight.Medium, color = colors.specialKeyText)
        if (pressed) {
            Popup(alignment = Alignment.TopCenter,
                offset = IntOffset(0, -((keyHeight.value * 1.4f).toInt()))) {
                KeyPreviewPopup(label = label, keyHeight = keyHeight, colors = colors, keyShape = keyShape)
            }
        }
    }
}

@Composable
private fun RowScope.SymbolsKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back_to_symbols),
            contentDescription = "Symbols",
            modifier = Modifier.size(26.dp),
            tint = colors.specialKeyText
        )
    }
}

@Composable
private fun LangToggleKey(
    currentLanguage: String,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    val isSinhala = currentLanguage == "si"
    val isMix = currentLanguage == "mix"
    val indicatorColor = when (currentLanguage) {
        "si" -> DeshGreen
        "mix" -> colors.specialKeyText
        else -> Color.Transparent
    }
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).fillMaxWidth()
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_native_letter),
                contentDescription = "Language",
                modifier = Modifier.size(18.dp),
                tint = if (isSinhala || isMix) indicatorColor else colors.specialKeyText
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp).height(2.dp).width(18.dp)
                    .background(
                        color = indicatorColor,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.EmojiKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.specialKeyBg.copy(alpha = 0.6f) else colors.specialKeyBg)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                contentDescription = "Emoji",
                modifier = Modifier.size(20.dp),
                tint = colors.specialKeyText
            )
            Text(text = ",", fontSize = 10.sp, color = colors.specialKeyText, lineHeight = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SpaceKey(
    weight: Float,
    keyHeight: Dp, colors: KeyboardColors, keyShape: RoundedCornerShape,
    showKeyBorders: Boolean = true,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    // Desh-style pill shape only when borders are hidden; bordered mode keeps
    // the standard key corner radius like every other key.
    val spaceShape = if (showKeyBorders) keyShape else RoundedCornerShape(keyHeight / 2)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .clip(spaceShape)
            .background(if (pressed) colors.spaceKeyBg.copy(alpha = 0.6f) else colors.spaceKeyBg)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sinkey board", fontSize = 12.sp,
            color = colors.spaceKeyText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * FIX #8: EnterKey previously had a [useSearchIcon] flag that was only set
 * for the dial-pad, while the main keyboard always showed the generic enter
 * icon regardless of the current IME action (Search, Send, Go, Next, Done…).
 *
 * Now accepts [imeAction] derived from [EditorInfo.imeOptions] so the icon/
 * label correctly reflects what the action will do in the focused field.
 * Callers that don't pass imeAction fall back to the generic enter icon.
 */
@Composable
private fun RowScope.EnterKey(
    weight: Float,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    colors: KeyboardColors? = null,
    showKeyBorders: Boolean = true,
    imeAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_NONE,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)

    if (!showKeyBorders) {
        // Desh-style enter key (borders hidden): filled circle with a
        // checkmark icon for the default action.
        val circleSize = keyHeight * 0.72f
        Box(
            modifier = Modifier
                .height(keyHeight).weight(weight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .scale(bumpScale)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (pressed) DeshGreen.copy(alpha = 0.8f) else DeshGreen)
                    .clickable { onTap() }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            waitForUpOrCancellation()
                            pressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (imeAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> Text(
                        "Send", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_GO -> Text(
                        "Go", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> Text(
                        "Next", fontSize = 11.sp, color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Enter",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }
        }
        return
    }

    // Original bordered-mode enter key: rounded-square key shape, filled
    // green, generic enter/action icon or label.
    Box(
        modifier = Modifier
            .height(keyHeight).weight(weight)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) DeshGreen.copy(alpha = 0.8f) else DeshGreen)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (imeAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
            android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> Text(
                "Send", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_GO -> Text(
                "Go", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> Text(
                "Next", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> Text(
                "Done", fontSize = 12.sp, color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            else -> Icon(
                painter = painterResource(id = R.drawable.ic_enter_key),
                contentDescription = "Enter",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Symbols Keyboard  (Desh-exact from APK XML analysis)
// ─────────────────────────────────────────────────────────────────────────────

// rowkeys_symbols1.xml  → keyspec_symbols_0..9
private val SymRow1 = listOf("1","2","3","4","5","6","7","8","9","0")
// rowkeys_symbols2.xml  → @ # ₹(mainCurrencyKey) % & * - = ( )
private val SymRow2 = listOf("@","#","₹","%","&","*","-","=","(",")")
// rowkeys_symbols3.xml  → ! " ' : + / ?
private val SymRow3 = listOf("!","\"","'",":","+","/","?")

// rowkeys_symbols_shift1.xml  → ~ ` _ ° ± ´ × ÷ • √
private val SymShiftRow1 = listOf("~","`","_","°","±","´","×","÷","•","√")
// rowkeys_symbols_shift2.xml  → ^ ₩ £ € ¥ $ © ® ™ π
private val SymShiftRow2 = listOf("^","₩","£","€","¥","$","©","®","™","π")
// rowkeys_symbols_shift3.xml  → \ | < > ; ¡ ¿
private val SymShiftRow3 = listOf("\\","|","<",">",";","¡","¿")

// FIX #7: Removed dead SymbolsKeyboardContent wrapper. It accepted an
// onShowEmoji parameter but never forwarded it to SymbolsKeyboardView,
// silently discarding the callback. SymbolsKeyboardView is called directly
// everywhere, so this wrapper had no callers and no purpose.

@Composable
private fun SymbolsKeyboardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onNumpad: () -> Unit,  // parent pushes NUMPAD onto back-stack
    onEmoji: () -> Unit    // parent pushes EMOJI onto back-stack
) {
    var shifted by remember { mutableStateOf(false) }
    // Bug fix: removed local showNumpad / showEmojiFromSymbols booleans.
    // Navigation is now delegated to the parent back-stack so that back()
    // from Numpad or Emoji returns to SYMBOLS, not MAIN.

    val row1 = if (shifted) SymShiftRow1 else SymRow1
    val row2 = if (shifted) SymShiftRow2 else SymRow2
    val row3 = if (shifted) SymShiftRow3 else SymRow3

    Column(
        modifier = Modifier.fillMaxWidth().background(colors.bg)
    ) {
        // ── Key rows ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .padding(bottom = bottomPadding)
        ) {
            // Row 1: numbers / shift-symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row1.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 2: symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
            }

            // Row 3: shift-toggle + symbols + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // <\> shift key  (ic_back_to_symbols drawable)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.4f)
                        .clip(keyShape)
                        .background(if (shifted) DeshGreen else colors.specialKeyBg)
                        .clickable { shifted = !shifted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_to_symbols),
                        contentDescription = "Shift",
                        modifier = Modifier.size(22.dp),
                        tint = if (shifted) Color.White else colors.specialKeyText
                    )
                }
                row3.forEach { ch ->
                    LetterKey(label = ch, weight = 1f, keyHeight = keyHeight,
                        colors = colors, keyShape = keyShape) { onKey(ch) }
                }
                BackspaceKey(weight = 1.4f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey("BACKSPACE") }
            }

            // Bottom row  (row_symbols_bottom.xml)
            // toLatinFromSymbolsKeyStyle | comma | toEmojiKeyStyle | space | toNumpadKeyStyle | . | enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABC  (toLatinFromSymbolsKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.8f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ABC", fontSize = 14.sp,
                        color = colors.specialKeyText, fontWeight = FontWeight.Medium)
                }
                // , (comma_key)
                SpecialKey(label = ",", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(",") }
                // Emoji  (toEmojiKeyStyle → ic_emoji_for_compose)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(0.9f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onEmoji() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_emoji_for_compose),
                        contentDescription = "Emoji",
                        modifier = Modifier.size(22.dp),
                        tint = colors.specialKeyText
                    )
                }
                // Space  (spaceKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(5.5f)
                        .clip(keyShape).background(colors.spaceKeyBg)
                        .clickable { onKey("SPACE") },
                    contentAlignment = Alignment.Center
                ) { }
                // 12/34  (toNumpadKeyStyle)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1.0f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onNumpad() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "12\n34", fontSize = 11.sp, color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                // .
                SpecialKey(label = ".", weight = 0.8f, keyHeight = keyHeight,
                    colors = colors, keyShape = keyShape) { onKey(".") }
                // Enter  (enterKeyStyle)
                EnterKey(weight = 2.0f, keyHeight = keyHeight,
                    keyShape = keyShape) { onKey("ENTER") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number Pad  (phone-style 3×3 + bottom row)
// Layout matches screenshot: 1 2 3 / 4 5 6 / 7 8 9 / . 0 _ with ABC, , ⌫ Enter on right
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
    onBoardStackChange: (List<Board>) -> Unit = {}
) {
    // ── Numpad grid ─────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Row 1: 1  2  3  │ ABC
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("1", keyHeight, colors, keyShape) { onKey("1") }
                NumpadDigitKey("2", keyHeight, colors, keyShape) { onKey("2") }
                NumpadDigitKey("3", keyHeight, colors, keyShape) { onKey("3") }
                // ABC — go directly to MAIN board (not just pop to SYMBOLS)
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onBoardStackChange(listOf(Board.MAIN)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        fontSize = 14.sp,
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Row 2: 4  5  6  │ ,
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("4", keyHeight, colors, keyShape) { onKey("4") }
                NumpadDigitKey("5", keyHeight, colors, keyShape) { onKey("5") }
                NumpadDigitKey("6", keyHeight, colors, keyShape) { onKey("6") }
                // comma
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.specialKeyBg)
                        .clickable { onKey(",") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ",",
                        fontSize = keyLabelFontSize(keyHeight),
                        color = colors.specialKeyText,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Row 3: 7  8  9  │ ⌫
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumpadDigitKey("7", keyHeight, colors, keyShape) { onKey("7") }
                NumpadDigitKey("8", keyHeight, colors, keyShape) { onKey("8") }
                NumpadDigitKey("9", keyHeight, colors, keyShape) { onKey("9") }
                // Backspace
                BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                    onKey("BACKSPACE")
                }
            }

            // Row 4: .  0  _  │ Enter (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // . (decimal / period)
                NumpadDigitKey(".", keyHeight, colors, keyShape) { onKey(".") }
                // 0
                NumpadDigitKey("0", keyHeight, colors, keyShape) { onKey("0") }
                // _ (underscore)
                NumpadDigitKey("_", keyHeight, colors, keyShape) { onKey("_") }
                // Enter — green
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape) { onKey("ENTER") }
            }
        }
}

/** A single large numpad digit/symbol key. */
@Composable
private fun RowScope.NumpadDigitKey(
    label: String,
    keyHeight: Dp,
    colors: KeyboardColors,
    keyShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bumpScale = rememberKeyBumpScale(pressed)
    Box(
        modifier = Modifier
            .height(keyHeight).weight(1f)
            .scale(bumpScale)
            .clip(keyShape)
            .background(if (pressed) colors.keyBg.copy(alpha = 0.6f) else colors.keyBg)
            .clickable { onTap() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = keyLabelFontSize(keyHeight),
            color = colors.keyText,
            fontWeight = FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clipboard history — persistent list of everything the user has copied
// (system-wide, captured by SinKeyInputMethodService's clipboard listener).
// Tap a row to paste it, tap the pin to keep it pinned to the top (pinned
// entries survive "Clear all"), tap the trash icon on a row to remove just
// that entry.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ClipboardHistoryView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    // The REAL measured height of MainKeyboardKeys' content (see
    // measuredMainContentHeight in KeyboardView) plus however much of the
    // toolbar/emoji-row above it this board reclaims by hiding them —
    // passed in rather than recomputed from row-padding math, which drifted
    // by a dp or two from the actual rendered height due to rounding.
    targetContentHeight: Dp,
    history: List<ClipEntity>,
    onPaste: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
        // Header: back arrow, title, clear-all
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBack() },
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
                text = "Clipboard",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            if (history.any { !it.pinned }) {
                Text(
                    text = "Clear all",
                    fontSize = 13.sp,
                    color = colors.subText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClearAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing copied yet",
                    fontSize = 13.sp,
                    color = colors.subText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(history, key = { it.text }) { clip ->
                    ClipRow(clip = clip, colors = colors, onPaste = onPaste, onTogglePin = onTogglePin, onDelete = onDelete)
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun ClipRow(
    clip: ClipEntity,
    colors: KeyboardColors,
    onPaste: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBg)
            .clickable { onPaste(clip.text) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = clip.text,
            fontSize = 14.sp,
            color = colors.keyText,
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onTogglePin(clip.text, !clip.pinned) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (clip.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (clip.pinned) "Unpin" else "Pin",
                modifier = Modifier.size(16.dp),
                tint = if (clip.pinned) DeshGreen else colors.subText
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDelete(clip.text) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp),
                tint = colors.subText
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Font picker — Board.FONT. Same layout pattern as ClipboardHistoryView:
// hides the toolbar/emoji-row above it and reclaims that space so switching
// boards never resizes the keyboard.
//
// Each row previews a FancyTextStyle (see FancyTextMapper) using the exact
// Unicode substitution that will be committed to the target app — not a
// Compose FontFamily, which (as the old implementation showed) only affects
// how the keyboard draws its own labels and has no effect on typed text.
// English text only; Sinhala has no equivalent styled-Unicode block.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FontPickerView(
    colors: KeyboardColors,
    keyHeight: Dp,
    bottomPadding: Dp,
    targetContentHeight: Dp,
    selectedFontKey: String,
    onFontSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val headerHeight = 44.dp
    val captionHeight = 20.dp

    Column(modifier = Modifier.fillMaxWidth().height(targetContentHeight).background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBack() },
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
                text = "Font",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.keyText,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Text(
            text = "Applies to English text only",
            fontSize = 11.sp,
            color = colors.subText,
            modifier = Modifier.height(captionHeight).padding(horizontal = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(FancyTextStyle.entries.toList(), key = { it.key }) { font ->
                FontRow(
                    font = font,
                    selected = font.key == selectedFontKey,
                    colors = colors,
                    onSelect = { onFontSelected(font.key) }
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun FontRow(
    font: FancyTextStyle,
    selected: Boolean,
    colors: KeyboardColors,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DeshGreen.copy(alpha = 0.15f) else colors.cardBg)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = font.label,
                fontSize = 13.sp,
                color = colors.subText,
            )
            Text(
                // Real Unicode substitution — this is exactly what gets typed
                // into the target app when this style is selected, not just a
                // keyboard-side preview approximation.
                text = FancyTextMapper.apply("Hello World", font),
                fontSize = 17.sp,
                color = colors.keyText,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DeshGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phone Dial Pad  (auto-shown when system sends TYPE_CLASS_PHONE)
// Layout: 1 / 2 ABC / 3 DEF / 4 GHI / 5 JKL / 6 MNO
//          7 PQRS / 8 TUV / 9 WXYZ / *# / 0+ / _ / Search(green)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadContent(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    PhoneDialPadView(
        colors = colors,
        keyHeight = keyHeight,
        keyShape = keyShape,
        bottomPadding = bottomPadding,
        onKey = onKey
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneDialPadView(
    colors: KeyboardColors,
    keyHeight: Dp,
    keyShape: RoundedCornerShape,
    bottomPadding: Dp,
    onKey: (String) -> Unit
) {
    val dialKeys = listOf(
        Triple("1", "", "1"),
        Triple("2", "ABC", "2"),
        Triple("3", "DEF", "3"),
        Triple("4", "GHI", "4"),
        Triple("5", "JKL", "5"),
        Triple("6", "MNO", "6"),
        Triple("7", "PQRS", "7"),
        Triple("8", "TUV", "8"),
        Triple("9", "WXYZ", "9")
    )

    // ── Grid ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Rows 1-3: 1  2ABC  3DEF │ -   /   4GHI  5JKL  6MNO │ .   /   7PQRS  8TUV  9WXYZ │ ⌫
            val sideKeys = listOf("-", ".", null) // right-side special keys per row
            dialKeys.chunked(3).forEachIndexed { rowIdx, trio ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trio.forEach { (digit, sub, key) ->
                        Box(
                            modifier = Modifier
                                .height(keyHeight).weight(1f)
                                .clip(keyShape).background(colors.keyBg)
                                .clickable { onKey(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = digit,
                                    fontSize = keyLabelFontSize(keyHeight),
                                    color = colors.keyText,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 28.sp
                                )
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        fontSize = 9.sp,
                                        color = colors.keyText.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    // Right-side key
                    when (rowIdx) {
                        0 -> { // -
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey("-") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = keyLabelFontSize(keyHeight), color = colors.specialKeyText)
                            }
                        }
                        1 -> { // .
                            Box(
                                modifier = Modifier
                                    .height(keyHeight).weight(1f)
                                    .clip(keyShape).background(colors.specialKeyBg)
                                    .clickable { onKey(".") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(".", fontSize = keyLabelFontSize(keyHeight), color = colors.specialKeyText)
                            }
                        }
                        2 -> { // ⌫
                            BackspaceKey(weight = 1f, keyHeight = keyHeight, colors = colors, keyShape = keyShape) {
                                onKey("BACKSPACE")
                            }
                        }
                    }
                }
            }

            // Row 4: *#  /  0+  /  _  │ Search (green)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // *# — tap → *, long press → #
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey("*") }, onLongClick = { onKey("#") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("* #", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // 0+ — tap → 0, long press → +
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey("0") }, onLongClick = { onKey("+") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("0 +", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText, fontWeight = FontWeight.Normal)
                }
                // FIX #6: Was sending " " (space) on tap despite showing "_" label.
                // Now correctly sends "_" on tap; long press still switches keyboard.
                Box(
                    modifier = Modifier
                        .height(keyHeight).weight(1f)
                        .clip(keyShape).background(colors.keyBg)
                        .combinedClickable(onClick = { onKey("_") }, onLongClick = { onKey("SWITCH_KEYBOARD") }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("_", fontSize = keyLabelFontSize(keyHeight), color = colors.keyText)
                }
                // Search / Enter (green) — dial pad always shows Search icon.
                EnterKey(weight = 1f, keyHeight = keyHeight, keyShape = keyShape,
                    imeAction = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    onKey("ENTER")
                }
            }
        }
    }
