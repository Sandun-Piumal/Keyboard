package com.spmods.sinkey.ime

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A soft keyboard (InputMethodService) is not an Activity and therefore
 * cannot host ActivityResultLaunchers (image picker, SAF folder picker) —
 * those APIs need an ActivityResultRegistry, which only Activities/
 * Fragments provide. This is a minimal, invisible (Theme.Translucent.NoTitleBar)
 * trampoline: SinKeyInputMethodService starts it with FLAG_ACTIVITY_NEW_TASK,
 * it immediately launches the requested system picker, and on result it
 * hands the picked Uri back to the *running* IME service instance via the
 * static callback below before finishing itself. If the IME isn't currently
 * running (e.g. it was killed while this Activity was in the foreground),
 * the callback is simply null and the result is dropped — there's nothing
 * else to hand it to.
 *
 * EXTRA_MODE selects which picker to launch:
 *  - MODE_IMAGE:  gallery image picker, for Board.STICKER_CREATE's Image Sticker
 *  - MODE_FOLDER: SAF folder tree picker, for connecting a WhatsApp/Telegram
 *                 sticker folder in Settings
 */
class StickerPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_IMAGE = "image"
        const val MODE_FOLDER = "folder"

        /** Set by the IME service right before starting this Activity; cleared after use. */
        var onImagePicked: ((Uri?) -> Unit)? = null
        var onFolderPicked: ((Uri?) -> Unit)? = null
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImagePicked?.invoke(uri)
        onImagePicked = null
        finish()
    }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Persist read access across reboots/process death — without this
            // the permission would only last for the current app session.
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        onFolderPicked?.invoke(uri)
        onFolderPicked = null
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.getStringExtra(EXTRA_MODE)) {
            MODE_IMAGE -> pickImage.launch("image/*")
            MODE_FOLDER -> pickFolder.launch(null)
            else -> finish()
        }
    }
}
