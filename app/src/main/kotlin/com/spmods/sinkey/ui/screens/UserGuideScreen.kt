package com.spmods.sinkey.ui.screens

import androidx.compose.runtime.Composable

/**
 * Thin wrapper over [LegalTextScreen] with the guide's title/content —
 * same "title + scrolling paragraphs" shape as Privacy policy/Terms, so
 * there's no separate layout to maintain here.
 */
@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    LegalTextScreen(
        title = "User guide",
        body = AboutContent.USER_GUIDE,
        onBack = onBack
    )
}
