package com.spmods.sinkey.ime

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * AbstractComposeView subclass for use inside InputMethodService.
 *
 * Problem: Android wraps our view in a LinearLayout (parentPanel) and calls
 * parentPanel.addView(our view) inside InputMethodService.setInputView().
 * ComposeView (which extends AbstractComposeView) calls
 * ViewTreeLifecycleOwner.get(this) in onAttachedToWindow(). That lookup
 * reads the tag on the view itself. If the tag is not set BEFORE
 * onAttachedToWindow() fires, it crashes with "ViewTreeLifecycleOwner not found".
 *
 * Solution: set the tags in init{} (before any attach), and re-set on the
 * parent (parentPanel) in onAttachedToWindow() before calling super.
 * ComposeView is final so we subclass AbstractComposeView directly instead.
 */
internal class ImeComposeView(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val content: @Composable () -> Unit
) : AbstractComposeView(context) {

    init {
        // Set tags immediately on construction — before any addView/attach.
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner as SavedStateRegistryOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner as androidx.lifecycle.ViewModelStoreOwner)
    }

    override fun Content() = content()

    override fun onAttachedToWindow() {
        // Re-apply to self before super — super triggers Compose initialization.
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner as SavedStateRegistryOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner as androidx.lifecycle.ViewModelStoreOwner)
        // Apply to parentPanel (Android inserts it above us before this call).
        (parent as? ViewGroup)?.let { p ->
            p.setViewTreeLifecycleOwner(lifecycleOwner)
            p.setViewTreeSavedStateRegistryOwner(lifecycleOwner as SavedStateRegistryOwner)
            p.setViewTreeViewModelStoreOwner(lifecycleOwner as androidx.lifecycle.ViewModelStoreOwner)
        }
        super.onAttachedToWindow()
    }
}
