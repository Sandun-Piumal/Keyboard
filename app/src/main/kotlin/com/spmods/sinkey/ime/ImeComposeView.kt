package com.spmods.sinkey.ime

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

internal class ImeComposeView(
    context: Context,
    private val owner: ImeLifecycleOwner,
    private val composableContent: @Composable () -> Unit
) : AbstractComposeView(context) {

    init {
        setOwners()
    }

    private fun setOwners() {
        setViewTreeLifecycleOwner(owner as LifecycleOwner)
        setViewTreeSavedStateRegistryOwner(owner as SavedStateRegistryOwner)
        setViewTreeViewModelStoreOwner(owner as ViewModelStoreOwner)
    }

    @Composable
    override fun Content() {
        composableContent()
    }

    override fun onAttachedToWindow() {
        setOwners()
        (parent as? ViewGroup)?.let { p ->
            p.setViewTreeLifecycleOwner(owner as LifecycleOwner)
            p.setViewTreeSavedStateRegistryOwner(owner as SavedStateRegistryOwner)
            p.setViewTreeViewModelStoreOwner(owner as ViewModelStoreOwner)
        }
        super.onAttachedToWindow()
    }
}
