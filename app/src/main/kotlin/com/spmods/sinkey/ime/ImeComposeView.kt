package com.spmods.sinkey.ime

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner

/**
 * A minimal AbstractComposeView subclass for use in an InputMethodService.
 *
 * The ViewTreeLifecycleOwner / SavedStateRegistryOwner / ViewModelStoreOwner
 * tags that Compose requires are NOT set here. They must be set on the IME
 * window's decorView BEFORE this view is attached, so that when Android's
 * addViewInner() calls dispatchAttachedToWindow() the tags are already
 * present anywhere in the tree above us.
 *
 * See SinKeyInputMethodService.onCreate() where the tags are set on
 * window.window.decorView, which is the correct place and time.
 *
 * THE DUPLICATE-RENDERING FIX: AbstractComposeView's default
 * ViewCompositionStrategy (Default) disposes the composition on the
 * view's *first* detach-from-window and assumes a normal Activity-style
 * attach/detach/destroy cycle. An InputMethodService's window does not
 * follow that cycle — the system detaches and re-attaches the IME window
 * from the WindowManager on ordinary focus churn (e.g. tapping anywhere
 * in the host app that isn't the text field, which is exactly what apps
 * like WhatsApp do with their own overlay/header UI) without ever
 * destroying the service or this view. Left on Default, Compose can end
 * up re-attaching and recomposing a composition it already half-tore-down
 * from the previous attach, visibly rendering old and new frames
 * together — a duplicate keyboard. This happens to every user because
 * it's a structural mismatch between AbstractComposeView's assumptions
 * and InputMethodService's window lifecycle, not something dependent on
 * device or timing.
 *
 * Tying composition disposal to our own manually-driven ImeLifecycleOwner
 * (see SinKeyInputMethodService.lifecycleOwner, driven explicitly from
 * onWindowShown/onWindowHidden/onDestroy) instead removes the dependency
 * on Android's view attach/detach events entirely — the composition is
 * now only ever disposed when we say so, once, in onDestroy.
 */
internal class ImeComposeView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val composableContent: @Composable () -> Unit
) : AbstractComposeView(context) {

    init {
        setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner)
        )
    }

    @Composable
    override fun Content() {
        composableContent()
    }
}
