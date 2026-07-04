package com.spmods.sinkey.ime

import android.inputmethodservice.InputMethodService
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
    private var wordBuffer = StringBuilder()
    private var currentLanguage = mutableStateOf("si") // default per PreferencesManager

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ImeLifecycleOwner()
        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        prefs = PreferencesManager(this)

        currentLanguage.value = runBlocking { prefs.defaultLanguage.first() }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

            setContent {
                SinKeyTheme {
                    KeyboardView(
                        currentLanguage = currentLanguage.value,
                        onKey = ::handleKey
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
                if (wordBuffer.isNotEmpty()) {
                    wordBuffer.deleteCharAt(wordBuffer.length - 1)
                    // re-render committed composing text as the buffer shrinks
                    ic.setComposingText(renderBuffer(), 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            "SPACE" -> {
                commitPendingWord()
                ic.commitText(" ", 1)
            }
            "ENTER" -> {
                commitPendingWord()
                ic.commitText("\n", 1)
            }
            "SHIFT" -> {
                // handled visually inside KeyboardView; no text action here
            }
            "SWITCH_KEYBOARD" -> {
                // Show system keyboard picker (InputMethodManager switcher dialog)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
            "LANG_TOGGLE" -> {
                commitPendingWord()
                currentLanguage.value = if (currentLanguage.value == "en") "si" else "en"
            }
            "," , "." -> {
                commitPendingWord()
                ic.commitText(key, 1)
            }
            else -> {
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
                } else {
                    // English mode: commit directly
                    ic.commitText(key, 1)
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
        ic?.finishComposingText()
        ic?.commitText(finalWord, 1)
        wordBuffer.clear()
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
