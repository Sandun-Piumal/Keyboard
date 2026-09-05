package com.spmods.sinkey.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.database.ContentObserver
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

/**
 * Same checks HomeScreen's setup card uses (see isImeEnabled/isImeDefault
 * there) — duplicated locally rather than shared, since neither is more
 * than a couple of lines and pulling them into a shared file for two
 * call sites isn't worth the indirection.
 */
private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java)
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeDefault(context: Context): Boolean {
    val defaultIme = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return defaultIme?.startsWith(context.packageName) == true
}

/**
 * One tutorial slide: a big emoji/icon, a title, and a short explanation.
 * Kept intentionally plain (no screenshots) so the tutorial has no
 * screen-density-specific assets to keep in sync as the real UI evolves —
 * see OnboardingScreen's doc comment for the full reasoning.
 */
private data class OnboardingSlide(
    val emoji: String,
    val title: String,
    val description: String
)

private val slides = listOf(
    OnboardingSlide(
        emoji = "🇱🇰",
        title = "Type Sinhala naturally",
        description = "Type in English letters and SinKey converts it to Sinhala as you go — e.g. \"kohomada\" becomes \"කොහොමද\". No separate Sinhala keyboard layout to learn."
    ),
    OnboardingSlide(
        emoji = "🔀",
        title = "Sinhala, English, or both",
        description = "Switch between pure Sinhala, pure English, and Mix mode (Sinhala + English together in the same sentence) any time from the language key."
    ),
    OnboardingSlide(
        emoji = "⚡",
        title = "Quick text shortcuts",
        description = "Set up a short shortcut like \"gm\" to expand into a full phrase like \"Good morning\" as you type. Find it under Settings → Quick text."
    ),
    OnboardingSlide(
        emoji = "📖",
        title = "Your own dictionary",
        description = "SinKey learns the words you type so they're suggested again next time. Manage them any time under Settings → Personal dictionary."
    ),
    OnboardingSlide(
        emoji = "⌨️",
        title = "One last step",
        description = "To start typing with SinKey, enable it and set it as your default keyboard."
    )
)

/**
 * First-launch onboarding tutorial: a swipeable set of slides introducing
 * SinKey's core features, shown once before the user reaches the main app
 * (see MainActivity's hasSeenOnboarding gate). Deliberately illustration-
 * light (emoji + text, no screenshots) — screenshots of the keyboard or
 * settings screens would need re-capturing every time either UI changes,
 * which is a maintenance trap for a tutorial that's easy to forget about;
 * a description in plain words stays accurate on its own.
 *
 * The last slide mirrors HomeScreen's own two-step "Enable SinKey" / "Set
 * as Default Keyboard" setup card exactly — same isImeEnabled/isImeDefault
 * checks, same live refresh via a ContentObserver on
 * Settings.Secure.DEFAULT_INPUT_METHOD/ENABLED_INPUT_METHODS plus an
 * ON_RESUME lifecycle callback, so returning here from the system
 * Settings/IME picker immediately reflects what the user just did rather
 * than showing a stale "not done yet" state. Both steps have to actually
 * be reachable independently (enabling and setting default are two
 * different system flows android exposes separately, per HomeScreen), so
 * this is two buttons rather than one — the same two the "Let's get you
 * started" card on Home already offers, just repeated here so a brand new
 * user isn't left to go find that card on their own right after finishing
 * the tutorial. "Skip" (top right, all slides) and "Finish" (bottom of the
 * last slide, always enabled regardless of setup progress) both call
 * [onFinish] directly — the tutorial shouldn't block someone who wants to
 * finish setup later from Home instead.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(isImeEnabled(context)) }
    var isDefault by remember { mutableStateOf(isImeDefault(context)) }

    // Identical refresh wiring to HomeScreen's setup card — see that
    // composable for the full reasoning; duplicated here rather than
    // shared since it's tightly coupled to the enabled/isDefault state
    // hoisted in each composable's own scope.
    DisposableEffect(lifecycleOwner) {
        fun refresh() {
            enabled = isImeEnabled(context)
            isDefault = isImeDefault(context)
        }

        val handler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            false, contentObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
            false, contentObserver
        )

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage == slides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Skip — always available, top right. Not shown on the last slide
        // since "Maybe later" there already covers the same "I'll do this
        // myself" intent, and having both would be redundant.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastSlide) {
                TextButton(onClick = onFinish) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingSlideContent(slides[page])
        }

        PagerDots(
            pageCount = slides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isLastSlide) {
                SetupStepButton(
                    label = "Enable SinKey",
                    done = enabled,
                    onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SetupStepButton(
                    label = "Set as default keyboard",
                    done = isDefault,
                    onClick = {
                        val imm = context.getSystemService(InputMethodManager::class.java)
                        imm.showInputMethodPicker()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (enabled && isDefault) "Finish" else "Finish setup later",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * One setup-step action on the last slide (Enable SinKey / Set as
 * default). Solid + clickable while [done] is false; once true, becomes a
 * disabled outlined button with a checkmark — visually confirming the
 * step without inviting another tap at a system dialog that's already
 * satisfied. [onClick] itself never flips [done] directly; it only
 * launches the relevant system flow (Settings screen or IME picker) — the
 * caller's ContentObserver-driven refresh is what actually updates [done]
 * once the user completes that flow and returns.
 */
@Composable
private fun SetupStepButton(label: String, done: Boolean, onClick: () -> Unit) {
    if (done) {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.material3.ButtonDefaults.outlinedButtonBorder(enabled = false)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(slide.emoji, fontSize = 72.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            slide.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            slide.description,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

/** Small dot indicator row — filled/wider dot marks [currentPage]. */
@Composable
private fun PagerDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateFloatAsState(if (isSelected) 24f else 8f, label = "dotWidth")
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(width.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}
