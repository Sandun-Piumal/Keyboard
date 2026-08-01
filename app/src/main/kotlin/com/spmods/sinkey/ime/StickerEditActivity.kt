package com.spmods.sinkey.ime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spmods.sinkey.data.sticker.StickerFileStore
import com.spmods.sinkey.data.sticker.StickerRepository
import com.spmods.sinkey.keyboard.DeshGreen
import com.spmods.sinkey.keyboard.ImageStickerDraft
import com.spmods.sinkey.keyboard.StickerFontStyle
import com.spmods.sinkey.keyboard.StickerImageEditorView
import com.spmods.sinkey.keyboard.keyboardColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hosts Board.STICKER_EDIT's "adjust image + add text" screen as its own
 * genuine full-screen Activity, instead of trying to render it inline
 * inside the keyboard's own docked Compose tree.
 *
 * Why this exists: an InputMethodService's window is a fixed-size panel
 * pinned to the bottom of the screen — nothing drawn inside it can float
 * freely over the rest of the screen, take up the full display, or escape
 * that panel's bounds without SYSTEM_ALERT_WINDOW (an extra runtime
 * permission the user would have to separately grant). A real Activity has
 * none of those constraints for free: launching this one gives the editor
 * its own full screen, on top of the keyboard and host app entirely, with
 * no panel-height math or overlay permission needed.
 *
 * Flow: pickImageForSticker() (see SinKeyInputMethodService) already saves
 * the picked photo to a temp file and, instead of pushing Board.STICKER_EDIT
 * on the keyboard's own board stack, starts this Activity with that file's
 * path. The user edits here; tapping "Add to stickers" saves directly via
 * StickerRepository (Room; the same database the keyboard's sticker tray
 * reads from as a Flow, so it picks up the new sticker automatically once
 * this Activity finishes) and finishes. Tapping the close (X) just finishes
 * without saving. Either way, focus returns to the host app's input field
 * and the keyboard reappears showing Board.STICKER, unchanged if cancelled
 * or with the new sticker visible if saved.
 */
class StickerEditActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            finish()
            return
        }

        val stickerRepo = StickerRepository(applicationContext)

        setContent {
            val isDark = isSystemInDarkTheme()
            val colors = keyboardColors(showKeyBorders = true, isDark = isDark)
            val scope = rememberCoroutineScope()
            var saving by remember { mutableStateOf(false) }

            BackHandler(enabled = !saving) { finish() }

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val bitmapResult = produceState<android.graphics.Bitmap?>(initialValue = null, key1 = imagePath) {
                    value = withContext(Dispatchers.IO) {
                        StickerFileStore.decodePreviewBitmap(File(imagePath))
                    }
                }

                val bitmap = bitmapResult.value
                when {
                    saving -> CircularProgressIndicator(color = DeshGreen)
                    bitmap == null -> {
                        // Still decoding — produceState's initial null also
                        // covers a genuine decode failure (no separate
                        // failure state here, matching this screen's only
                        // job: show a spinner then either the editor or, if
                        // decode never succeeds, let the user close and
                        // retry picking from the keyboard).
                        CircularProgressIndicator(color = DeshGreen)
                    }
                    else -> {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        StickerImageEditorView(
                            colors = colors,
                            bottomPadding = 12.dp,
                            targetContentHeight = configuration.screenHeightDp.dp,
                            imageBitmap = bitmap,
                            onSave = { draft: ImageStickerDraft ->
                                saving = true
                                scope.launch {
                                    val fontTypeface = when (draft.fontStyle) {
                                        StickerFontStyle.BOLD -> android.graphics.Typeface.DEFAULT_BOLD
                                        StickerFontStyle.CLASSIC -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                                        StickerFontStyle.TYPEWRITER -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                                        StickerFontStyle.HANDWRITTEN -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
                                        StickerFontStyle.CLEAN -> android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                                    }
                                    val created = stickerRepo.createFromImageEdit(
                                        sourceFile = File(imagePath),
                                        imageScale = draft.imageScale,
                                        imageOffsetXFraction = draft.imageOffsetXFraction,
                                        imageOffsetYFraction = draft.imageOffsetYFraction,
                                        text = draft.text,
                                        textColor = draft.textColor,
                                        textSizeFraction = draft.textSizeFraction,
                                        textXFraction = draft.textXFraction,
                                        textYFraction = draft.textYFraction,
                                        fontTypeface = fontTypeface,
                                        outlineEnabled = draft.outlineEnabled
                                    )
                                    if (!created) {
                                        android.widget.Toast.makeText(
                                            this@StickerEditActivity,
                                            "Couldn't save that sticker",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    // Clean up the temp file regardless of
                                    // success — StickerFileStore.compositeImageSticker
                                    // only reads from it, it never owns/deletes it.
                                    runCatching { File(imagePath).delete() }
                                    finish()
                                }
                            },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}
