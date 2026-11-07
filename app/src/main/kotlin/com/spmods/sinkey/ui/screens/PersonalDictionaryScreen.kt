package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.dictionary.WordEntity

/**
 * "Personal dictionary" settings screen: browse every word the keyboard has
 * learned (typed by the user, or added here by hand), split into Sinhala/
 * English tabs, with the ability to delete a word or add one manually.
 * Frosted-glass style shared with the other Settings pages (GlassKit.kt).
 *
 * Unlike Quick text's shortcuts (which only ever come from what the user
 * explicitly saves), most rows here got there automatically from ordinary
 * typing via WordRepository.learn() — this screen is a window onto that
 * same growing dictionary, not a separate store. Deleting a word here does
 * not blocklist it; if the user types it again it will simply be relearned
 * from frequency 1, same as any brand new word.
 */
@Composable
fun PersonalDictionaryScreen(
    sinhalaWords: List<WordEntity>,
    englishWords: List<WordEntity>,
    onDelete: (word: String, language: String) -> Unit,
    onAdd: (word: String, language: String) -> Unit,
    onBack: () -> Unit
) {
    val p = rememberGlassPalette()
    var selectedTab by remember { mutableStateOf(DictionaryTab.SINHALA) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WordEntity?>(null) }

    val activeWords = if (selectedTab == DictionaryTab.SINHALA) sinhalaWords else englishWords

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { GlassTopBar(p, "Personal dictionary", onBack) },
        floatingActionButton = { GlassFab(onClick = { showAddDialog = true }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DictionaryTabRow(
                p = p,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (activeWords.isEmpty()) {
                GlassCard(p, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), radius = 28) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📖", fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No words yet", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                        Text(
                            "Words you type are learned automatically, or tap + to add one",
                            fontSize = 12.sp,
                            color = p.body
                        )
                    }
                }
            } else {
                GlassCard(
                    p,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    radius = 28
                ) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 4.dp)) {
                        items(activeWords, key = { it.word }) { entry ->
                            SettingRow(
                                p,
                                glyph = if (selectedTab == DictionaryTab.SINHALA) "අ" else "A",
                                compact = true,
                                title = entry.word,
                                subtitle = "",
                                last = entry == activeWords.last()
                            ) { DeleteButton { pendingDelete = entry } }
                        }
                    }
                }
                // keep the last row clear of the floating + button
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            p = p,
            defaultLanguage = selectedTab,
            onDismiss = { showAddDialog = false },
            onSave = { word, language ->
                onAdd(word, language)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { entry ->
        GlassAlertDialog(
            p = p,
            onDismiss = { pendingDelete = null },
            title = "Delete word?",
            text = { Text("\"${entry.word}\" will be removed from your personal dictionary. It'll be relearned from scratch if you type it again.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry.word, entry.language)
                    pendingDelete = null
                }) {
                    Text("Delete", color = Color(0xFFE5383B), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = p.body) }
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
    p: HomePalette,
    selected: DictionaryTab,
    onSelect: (DictionaryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = DictionaryTab.values()
    GlassSegmented(
        p = p,
        labels = tabs.map { it.label },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        modifier = modifier
    )
}

/**
 * Manual "add word" dialog. [defaultLanguage] pre-selects whichever tab was
 * active when + was tapped, but the language can still be switched inside
 * the dialog before saving — e.g. the user is on the English tab but wants
 * to add a Sinhala word without first switching tabs.
 */
@Composable
private fun AddWordDialog(
    p: HomePalette,
    defaultLanguage: DictionaryTab,
    onDismiss: () -> Unit,
    onSave: (word: String, language: String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(defaultLanguage) }

    GlassAlertDialog(
        p = p,
        onDismiss = onDismiss,
        title = "Add word",
        text = {
            Column {
                DictionaryTabRow(p = p, selected = language, onSelect = { language = it })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word") },
                    placeholder = { Text(if (language == DictionaryTab.SINHALA) "කොහොමද" else "hello") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
                Text("Add", color = p.iconIndigo, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = p.body) }
        }
    )
}
