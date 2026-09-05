package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.spmods.sinkey.data.shortcut.ShortcutEntity

/**
 * "Quick text" settings screen: typing a short shortcut (e.g. "gm") then
 * space/enter expands it in place to a longer phrase (e.g. "Good
 * morning"). Mirrors SoundVibrationScreen/KeyboardHeightScreen's card +
 * TopAppBar structure.
 *
 * The feature-level on/off switch sits at the top of its own card — off by
 * default (see PreferencesManager.QUICK_TEXT_ENABLED) since silently
 * rewriting short strings the user types is surprising until they've
 * deliberately opted in. The shortcut list below is always visible (so the
 * user can set shortcuts up before turning the feature on) but only takes
 * effect while the switch is on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTextScreen(
    enabled: Boolean,
    shortcuts: List<ShortcutEntity>,
    onEnabledChange: (Boolean) -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit,
    onDelete: (shortcut: String) -> Unit,
    onBack: () -> Unit
) {
    // Non-null while the add/edit dialog is open. Editing an existing
    // entry pre-fills both fields (see rows below); adding starts blank.
    var editing by remember { mutableStateOf<ShortcutEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick text", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            QuickTextCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quick text", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Expand a shortcut into a full phrase as you type, e.g. \"gm\" → \"Good morning\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            if (shortcuts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No shortcuts yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add one",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                QuickTextCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LazyColumn {
                        items(shortcuts, key = { it.shortcut }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editing = entry }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.shortcut, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        entry.expansion,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                IconButton(onClick = { pendingDelete = entry.shortcut }) {
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
        ShortcutEditDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { shortcut, expansion ->
                onSave(shortcut, expansion)
                showAddDialog = false
            }
        )
    }

    editing?.let { entry ->
        ShortcutEditDialog(
            initial = entry,
            onDismiss = { editing = null },
            onSave = { shortcut, expansion ->
                // A shortcut edit that changes the trigger text itself
                // needs the old row removed — the primary key is the
                // shortcut text, so upserting a new one would leave the
                // old trigger behind as a stale duplicate entry.
                if (shortcut.trim().lowercase() != entry.shortcut) {
                    onDelete(entry.shortcut)
                }
                onSave(shortcut, expansion)
                editing = null
            }
        )
    }

    pendingDelete?.let { shortcut ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete shortcut?", fontWeight = FontWeight.Bold) },
            text = { Text("\"$shortcut\" will no longer expand.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(shortcut)
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

/** Shared add/edit dialog — [initial] non-null pre-fills both fields for editing. */
@Composable
private fun ShortcutEditDialog(
    initial: ShortcutEntity?,
    onDismiss: () -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit
) {
    var shortcut by remember { mutableStateOf(initial?.shortcut ?: "") }
    var expansion by remember { mutableStateOf(initial?.expansion ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add shortcut" else "Edit shortcut", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Shortcut") },
                    placeholder = { Text("gm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = expansion,
                    onValueChange = { expansion = it },
                    label = { Text("Expands to") },
                    placeholder = { Text("Good morning") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(shortcut, expansion) },
                enabled = shortcut.isNotBlank() && expansion.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QuickTextCard(
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
