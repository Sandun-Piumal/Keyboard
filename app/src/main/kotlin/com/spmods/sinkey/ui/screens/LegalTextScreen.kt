package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic static long-form text screen — shared by Privacy policy and
 * Terms & conditions (see AboutScreen's onOpenPrivacyPolicy/onOpenTerms),
 * since both are the same "title + scrolling body text" shape and neither
 * needs any interactivity beyond reading. [body] is plain paragraphs
 * separated by blank lines rather than Markdown/HTML — simplest thing that
 * reads fine in a Column of Text, and this content changes rarely enough
 * that no renderer beyond that is worth the added dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalTextScreen(
    title: String,
    body: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Split on blank lines so each paragraph gets its own Text
            // (and thus normal paragraph spacing) rather than one giant
            // Text with embedded \n\n, which Compose renders with no
            // visible gap between paragraphs.
            body.trim().split(Regex("\n\\s*\n")).forEach { paragraph ->
                val trimmed = paragraph.trim()
                if (trimmed.isEmpty()) return@forEach
                // A short ALL-caps-free line ending without a period, under
                // ~60 chars, is treated as a section heading rather than
                // body text — matches how the drafted content below marks
                // its own section titles (e.g. "1. Information we collect").
                val isHeading = trimmed.length < 60 && !trimmed.endsWith(".") && trimmed.lineSequence().count() == 1
                Text(
                    trimmed,
                    fontSize = if (isHeading) 15.sp else 14.sp,
                    fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHeading) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(bottom = if (isHeading) 8.dp else 18.dp)
                )
            }
        }
    }
}
