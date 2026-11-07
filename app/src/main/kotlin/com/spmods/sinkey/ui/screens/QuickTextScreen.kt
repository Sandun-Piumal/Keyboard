package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.data.shortcut.ShortcutEntity

/**
 * "Quick text" settings screen: typing a short shortcut (e.g. "gm") then
 * space/enter expands it in place to a longer phrase (e.g. "Good
 * morning"). Frosted-glass style shared with the other Settings pages
 * (GlassKit.kt).
 *
 * The feature-level on/off switch sits at the top of its own card — off by
 * default (see PreferencesManager.QUICK_TEXT_ENABLED) since silently
 * rewriting short strings the user types is surprising until they've
 * deliberately opted in. The shortcut list below is always visible (so the
 * user can set shortcuts up before turning the feature on) but only takes
 * effect while the switch is on.
 */
@Composable
fun QuickTextScreen(
    enabled: Boolean,
    shortcuts: List<ShortcutEntity>,
    onEnabledChange: (Boolean) -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit,
    onDelete: (shortcut: String) -> Unit,
    onBack: () -> Unit
) {
    val p = rememberGlassPalette()
    // Non-null while the add/edit dialog is open. Editing an existing
    // entry pre-fills both fields (see rows below); adding starts blank.
    var editing by remember { mutableStateOf<ShortcutEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { GlassTopBar(p, "Quick text", onBack) },
        floatingActionButton = { GlassFab(onClick = { showAddDialog = true }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GlassCard(p, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), radius = 28) {
                SettingRow(
                    p, glyph = "⚡",
                    title = "Quick text",
                    subtitle = "Expand a shortcut into a full phrase as you type, e.g. \"gm\" → \"Good morning\"",
                    last = true
                ) { GlassSwitch(p, enabled, onEnabledChange) }
            }

            if (shortcuts.isEmpty()) {
                GlassCard(p, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), radius = 28) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚡", fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No shortcuts yet", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                        Text("Tap + to add one", fontSize = 12.sp, color = p.body)
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
                        items(shortcuts, key = { it.shortcut }) { entry ->
                            SettingRow(
                                p, glyph = "⚡", compact = true,
                                title = entry.shortcut,
                                subtitle = entry.expansion,
                                modifier = Modifier.clickable { editing = entry },
                                last = entry == shortcuts.last()
                            ) { DeleteButton { pendingDelete = entry.shortcut } }
                        }
                    }
                }
                // keep the last row clear of the floating + button
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        ShortcutEditDialog(
            p = p,
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
            p = p,
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
        GlassAlertDialog(
            p = p,
            onDismiss = { pendingDelete = null },
            title = "Delete shortcut?",
            text = { Text("\"$shortcut\" will no longer expand.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(shortcut)
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

/** Round red-tinted delete button used at the end of list rows. */
@Composable
internal fun DeleteButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5383B).copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE5383B), modifier = Modifier.size(20.dp))
    }
}

/** Shared add/edit dialog — [initial] non-null pre-fills both fields for editing. */
@Composable
private fun ShortcutEditDialog(
    p: HomePalette,
    initial: ShortcutEntity?,
    onDismiss: () -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit
) {
    var shortcut by remember { mutableStateOf(initial?.shortcut ?: "") }
    var expansion by remember { mutableStateOf(initial?.expansion ?: "") }

    GlassAlertDialog(
        p = p,
        onDismiss = onDismiss,
        title = if (initial == null) "Add shortcut" else "Edit shortcut",
        text = {
            Column {
                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Shortcut") },
                    placeholder = { Text("gm") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = expansion,
                    onValueChange = { expansion = it },
                    label = { Text("Expands to") },
                    placeholder = { Text("Good morning") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(shortcut, expansion) },
                enabled = shortcut.isNotBlank() && expansion.isNotBlank()
            ) {
                Text("Save", color = p.iconIndigo, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = p.body) }
        }
    )
}
