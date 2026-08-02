package com.spmods.sinkey.ime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * genuine Activity — presented as a centered, rounded floating card over
 * the dimmed host app (Theme.SinKey.FloatingCard), rather than either (a)
 * rendered inline inside the keyboard's own docked Compose tree, or (b)
 * covering the entire display like a normal opaque Activity.
 *
 * Why this needs to be a real Activity at all: an InputMethodService's
 * window is a fixed-size panel pinned to the bottom of the screen —
 * nothing drawn inside it can float freely over the rest of the screen or
 * escape that panel's bounds without SYSTEM_ALERT_WINDOW (an extra runtime
 * permission the user would have to separately grant). A real Activity has
 * none of those constraints for free.
 *
 * Why it's sized as a card instead of full-screen: the window's size and
 * position can't be fixed purely in XML (they depend on the display's
 * actual size), so onCreate() below explicitly sets a fixed WRAP_CONTENT-
 * ish card size on the decor window via WindowManager.LayoutParams, in
 * addition to the theme's windowIsFloating/backgroundDimEnabled flags.
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

    // Theme.SinKey.FloatingCard makes the window translucent and dims the
    // background, but WindowManager still measures/positions the window
    // itself based on the *default* full-screen bounds before any layout
    // pass — window.setLayout() called from onCreate() (before the
    // DecorView is attached) or even from onResume() (which can still race
    // Compose's first measure pass on some OEM skins) is often silently
    // dropped, so the window falls back to full-screen. Doing it inside a
    // pre-draw listener on the DecorView guarantees it runs after the
    // window actually exists and right before its first real layout pass,
    // so the resize reliably sticks. It's applied on every resume too,
    // since some launchers/skins reset floating windows to full-screen
    // after a config change (e.g. rotation, or returning from the system
    // image picker).
    private fun applyCardWindowBounds() {
        val decor = window.decorView
        decor.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                decor.viewTreeObserver.removeOnPreDrawListener(this)
                window.setLayout(
                    (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                    (resources.displayMetrics.heightPixels * 0.88f).toInt()
                )
                window.setGravity(android.view.Gravity.CENTER)
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        applyCardWindowBounds()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            finish()
            return
        }

        // The window itself (not just our Compose content) needs a
        // transparent background — otherwise the real window draws its own
        // opaque black rectangle at full window bounds behind/around the
        // rounded Compose card, which is what showed up as black bars
        // around the card rather than dimmed host-app content.
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        applyCardWindowBounds()

        val stickerRepo = StickerRepository(applicationContext)

        setContent {
            val isDark = isSystemInDarkTheme()
            val colors = keyboardColors(showKeyBorders = true, isDark = isDark)
            val scope = rememberCoroutineScope()
            var saving by remember { mutableStateOf(false) }

            BackHandler(enabled = !saving) { finish() }

            // The window itself is still sized to 92%/88% of the screen
            // (applyCardWindowBounds()) — only the *card* wraps its content
            // height, so there's leftover vertical space inside the window
            // around the card. An outer Modifier.fillMaxSize() Box with
            // contentAlignment = Center places the card in the middle of
            // that leftover space instead of anchored to the window's top
            // (Android's default gravity for a wrap-content child).
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(Color.Black),
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
                            // Card is sized to 88% of screen height in
                            // onCreate() (window.setLayout) — mirror that
                            // here so the editor's internal layout math
                            // matches the actual space it has, instead of
                            // assuming the old full-screen height.
                            targetContentHeight = (configuration.screenHeightDp * 0.88f).dp,
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
                                        shape = draft.shape,
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
}
