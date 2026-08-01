package com.spmods.sinkey.ime

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

/**
 * A soft keyboard (InputMethodService) is not an Activity and therefore
 * cannot host an ActivityResultLauncher (needed for the gallery image
 * picker) — that API needs an ActivityResultRegistry, which only
 * Activities/Fragments provide. This is a minimal, invisible
 * (Theme.SinKey.Transparent) trampoline: SinKeyInputMethodService.
 * pickImageForSticker() starts it with FLAG_ACTIVITY_NEW_TASK, it
 * immediately launches the system gallery picker, and on result it hands
 * the picked image back to the *running* IME service instance via the
 * static callback below before finishing itself. If the IME isn't
 * currently running (e.g. it was killed while this Activity was in the
 * foreground), the callback is simply null and the result is dropped —
 * there's nothing else to hand it to.
 *
 * IMPORTANT: this hands back a file path, not the picked Uri. The Uri
 * returned by GetContent() (including the modern system Photo Picker) only
 * carries a *transient*, Activity-scoped read grant — it is not guaranteed
 * to still be readable once this Activity has called finish() and control
 * has passed to a different component (SinKeyInputMethodService, a
 * Service, reading it later from inside a coroutine). That race is exactly
 * what caused "Couldn't read that image" to show up intermittently.
 * Decoding + saving the bytes here, synchronously, while this Activity and
 * its Uri grant definitely still exist, removes that race entirely.
 */
class StickerPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_IMAGE = "image"
        private const val STATE_PICKER_LAUNCHED = "picker_launched"

        /** Set by the IME service right before starting this Activity; cleared after use. */
        var onImagePicked: ((String?) -> Unit)? = null
    }

    // True once pickImage.launch() has actually been called for this
    // Activity *instance*. Needed because the system Photo Picker (and
    // some OEM gallery apps) run as a separate, heavier process; while
    // it's in the foreground, Android can reclaim this invisible,
    // excludeFromRecents trampoline and later recreate a *new*
    // StickerPickerActivity instance to deliver the pending
    // ActivityResult. Without this guard, that fresh instance's
    // onCreate() would see MODE_IMAGE again and call pickImage.launch()
    // a second time — reopening the picker instead of delivering the
    // result the user already picked, which is exactly the "picker
    // opens, I pick an image, and then nothing happens" symptom.
    private var pickerLaunched = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val savedPath = uri?.let { readAndCacheImage(it) }
        // onImagePicked is a plain static var, so it does NOT survive
        // process death — only in-process recreation (e.g. rotation).
        // If this process was killed and restarted purely to deliver the
        // result, the callback will already be null here and there is
        // nothing to hand the image to. Fail silently rather than crash;
        // the user can just retry.
        onImagePicked?.invoke(savedPath)
        onImagePicked = null
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerLaunched = savedInstanceState?.getBoolean(STATE_PICKER_LAUNCHED) ?: false
        if (pickerLaunched) {
            // We're a recreated instance whose picker request is already
            // in flight. Do NOT call pickImage.launch() again — just wait
            // for the ActivityResultRegistry to redeliver the pending
            // result to our callback above.
            return
        }
        when (intent?.getStringExtra(EXTRA_MODE)) {
            MODE_IMAGE -> {
                pickerLaunched = true
                pickImage.launch("image/*")
            }
            else -> finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PICKER_LAUNCHED, pickerLaunched)
    }

    /**
     * Reads [uri] right now (while its read grant is still guaranteed
     * valid) and writes the raw bytes to a temp file in the app's cache
     * dir, returning that file's absolute path. StickerRepository picks
     * this file up, decodes/downscales/saves it as the actual sticker, and
     * the temp file is cleaned up afterwards (see
     * StickerFileStore.saveFromImageFile). Returns null if the image
     * couldn't be read at all — e.g. the source was deleted between being
     * picked and this call.
     */
    private fun readAndCacheImage(uri: android.net.Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                // Quick sanity check that this is actually decodable image
                // data before committing it to disk — avoids saving garbage
                // for a Uri that resolves but isn't a real image.
                val bytes = input.readBytes()
                if (BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) return null

                val tempFile = File(cacheDir, "sticker_pick_${System.currentTimeMillis()}.tmp")
                FileOutputStream(tempFile).use { out -> out.write(bytes) }
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.w("SinKey", "Couldn't read picked sticker image", e)
            null
        }
    }
}
