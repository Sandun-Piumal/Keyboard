package com.spmods.sinkey.ime

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A soft keyboard (InputMethodService) is not an Activity and therefore
 * cannot host an ActivityResultLauncher (needed for the gallery image
 * picker) — that API needs an ActivityResultRegistry, which only
 * Activities/Fragments provide. This is a minimal, invisible
 * (Theme.SinKey.Transparent) trampoline: SinKeyInputMethodService.
 * pickImageForSticker() starts it with FLAG_ACTIVITY_NEW_TASK, it
 * immediately launches the system gallery picker, and on result it hands
 * the picked Uri back to the *running* IME service instance via the static
 * callback below before finishing itself. If the IME isn't currently
 * running (e.g. it was killed while this Activity was in the foreground),
 * the callback is simply null and the result is dropped — there's nothing
 * else to hand it to.
 */
class StickerPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_IMAGE = "image"

        /** Set by the IME service right before starting this Activity; cleared after use. */
        var onImagePicked: ((Uri?) -> Unit)? = null
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImagePicked?.invoke(uri)
        onImagePicked = null
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.getStringExtra(EXTRA_MODE)) {
            MODE_IMAGE -> pickImage.launch("image/*")
            else -> finish()
        }
    }
}
