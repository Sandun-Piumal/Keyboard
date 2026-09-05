package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.dictionary.WordEntity

/**
 * "Personal dictionary" settings screen: browse every word the keyboard has
 * learned (typed by the user, or added here by hand), split into Sinhala/
 * English tabs, with the ability to delete a word or add one manually.
 * Mirrors QuickTextScreen's card + TopAppBar + FAB structure.
 *
 * Unlike Quick text's shortcuts (which only ever come from what the user
 * explicitly saves), most rows here got there automatically from ordinary
 * typing via WordRepository.learn() — this screen is a window onto that
 * same growing dictionary, not a separate store. Deleting a word here does
 * not blocklist it; if the user types it again it will simply be relearned
 * from frequency 1, same as any brand new word.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDictionaryScreen(
    sinhalaWords: List<WordEntity>,
    englishWords: List<WordEntity>,
    onDelete: (word: String, language: String) -> Unit,
    onAdd: (word: String, language: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DictionaryTab.SINHALA) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WordEntity?>(null) }

    val activeWords = if (selectedTab == DictionaryTab.SINHALA) sinhalaWords else englishWords

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal dictionary", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DictionaryTabRow(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (activeWords.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No words yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Words you type are learned automatically, or tap + to add one",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                DictionaryCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LazyColumn {
                        items(activeWords, key = { it.word }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.word,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { pendingDelete = entry }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            defaultLanguage = selectedTab,
            onDismiss = { showAddDialog = false },
            onSave = { word, language ->
                onAdd(word, language)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete word?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${entry.word}\" will be removed from your personal dictionary. It'll be relearned from scratch if you type it again.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry.word, entry.language)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private enum class DictionaryTab(val label: String, val language: String) {
    SINHALA("සිංහල", "si"),
    ENGLISH("English", "en")
}

@Composable
private fun DictionaryTabRow(
    selected: DictionaryTab,
    onSelect: (DictionaryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        DictionaryTab.values().forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Manual "add word" dialog. [defaultLanguage] pre-selects whichever tab was
 * active when + was tapped, but the language can still be switched inside
 * the dialog before saving — e.g. the user is on the English tab but wants
 * to add a Sinhala word without first switching tabs.
 */
@Composable
private fun AddWordDialog(
    defaultLanguage: DictionaryTab,
    onDismiss: () -> Unit,
    onSave: (word: String, language: String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(defaultLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add word", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DictionaryTabRow(selected = language, onSelect = { language = it })
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word") },
                    placeholder = { Text(if (language == DictionaryTab.SINHALA) "කොහොමද" else "hello") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(word, language.language) },
                enabled = word.isNotBlank()
            ) {
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DictionaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}
