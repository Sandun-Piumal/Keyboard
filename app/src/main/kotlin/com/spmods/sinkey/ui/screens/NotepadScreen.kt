package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Free-form typing scratchpad opened from Home's "Start Typing" button.
 * Deliberately not a "typing test" with scored sentences/WPM — just a
 * blank field with the keyboard already open, so a new user can try
 * SinKey's Sinhala/English/Mix typing on whatever they feel like typing,
 * with zero setup or pressure.
 *
 * Text here is intentionally throwaway: nothing is persisted to
 * DataStore/Room, so navigating back always lands on a blank page next
 * time — this is a place to try typing, not a place to draft something
 * you'd want to keep (Personal dictionary/Quick text already cover the
 * "I want this to stick around" cases elsewhere in Settings).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Requesting focus immediately on composition also opens the software
    // keyboard for a field that's already focused when the screen appears,
    // but a brief delay is needed first — requesting focus in the same
    // frame the screen is still being laid out/animated in sometimes gets
    // silently dropped (the classic "keyboard doesn't show up right after
    // navigating to a screen" issue). explicitly calling
    // keyboardController.show() right after is a second nudge for cases
    // where the OS decides focus arrived but doesn't auto-open the
    // keyboard on its own.
    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val wordCount = remember(text) { text.trim().split(Regex("\\s+")).count { it.isNotBlank() } }
    val charCount = text.length

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Try typing", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "$wordCount words · $charCount characters",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "Type anything to try Sinhala, English, or Mix mode…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}
