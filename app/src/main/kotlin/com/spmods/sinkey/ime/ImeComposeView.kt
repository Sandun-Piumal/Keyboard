package com.spmods.sinkey.ime

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

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
 */
internal class ImeComposeView(
    context: Context,
    private val composableContent: @Composable () -> Unit
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        composableContent()
    }
}
