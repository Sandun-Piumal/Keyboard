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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The keyboard engine itself. Android binds this service whenever SinKey is
 * the active input method; [onCreateInputView] returns the Compose UI that
 * the system displays above the app currently being typed into.
 */
class SinKeyInputMethodService : InputMethodService() {

    private lateinit var lifecycleOwner: ImeLifecycleOwner
    private lateinit var prefs: PreferencesManager

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
            "LANG_TOGGLE" -> {
                commitPendingWord()
                currentLanguage.value = if (currentLanguage.value == "en") "si" else "en"
            }
            "," , "." -> {
                commitPendingWord()
                ic.commitText(key, 1)
            }
            else -> {
                if (currentLanguage.value == "si") {
                    wordBuffer.append(key)
                    ic.setComposingText(renderBuffer(), 1)
                } else {
                    ic.commitText(key, 1)
                }
            }
        }
    }

    /** Converts the in-progress romanized buffer into live Sinhala preview text. */
    private fun renderBuffer(): String = SinhalaTransliterator.transliterate(wordBuffer.toString())

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
