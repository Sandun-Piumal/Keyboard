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
        // GetContent()'s URI grant is transient and scoped to this Activity
        // — it is not guaranteed to still be valid once this Activity has
        // finished and a different component (SinKeyInputMethodService, a
        // Service) tries to read it later via contentResolver. Without
        // this, decodeScaledBitmap's openInputStream() calls can fail with
        // a SecurityException (surfaced to the user as "Couldn't read that
        // image") depending on timing. Taking a persistable read grant here
        // — before finish() — keeps the Uri readable for as long as this
        // app needs it, regardless of which component reads it or when.
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some providers (e.g. certain OEM galleries, or the modern
                // Photo Picker's synthetic com.android.providers.media.photopicker
                // Uris) don't support persistable grants — the transient
                // grant from GetContent() usually still covers a same-process,
                // immediate read in that case, so this is not fatal; only
                // an actual read failure downstream should surface an error.
            }
        }
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
