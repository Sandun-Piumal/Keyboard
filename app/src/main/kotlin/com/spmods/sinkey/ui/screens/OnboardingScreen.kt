package com.spmods.sinkey.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
        description = "To start typing with SinKey, it needs to be enabled and selected as your keyboard. Tap below to turn it on."
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
 * The last slide's primary action is "Enable keyboard" rather than
 * "Finish", since the tutorial's real goal is a working, selected
 * keyboard, not just having been shown some slides — [onEnableKeyboard]
 * opens the system's input method settings (same
 * Settings.ACTION_INPUT_METHOD_SETTINGS flow HomeScreen's own "Enable
 * keyboard" prompt uses) rather than completing the tutorial by itself, so
 * [onFinish] is called separately once the user comes back. "Skip" (top
 * right, all slides) and "Maybe later" (bottom of the last slide) both
 * call [onFinish] directly without enabling anything — the tutorial
 * shouldn't block someone who already knows how to enable a keyboard, or
 * wants to do it later from Home instead.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onEnableKeyboard: () -> Unit
) {
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
                Button(
                    onClick = onEnableKeyboard,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Enable keyboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
