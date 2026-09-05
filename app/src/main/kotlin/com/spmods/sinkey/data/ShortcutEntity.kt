package com.spmods.sinkey.data.shortcut

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One "Quick text" shortcut: typing [shortcut] then a word-boundary key
 * (space or enter) replaces it in-place with [expansion] — e.g. "gm" ->
 * "Good morning". See ShortcutRepository/QuickTextScreen.
 *
 * [shortcut]   the short trigger text the user types, stored lowercase so
 *              matching is case-insensitive (see ShortcutRepository.expand,
 *              which re-applies the user's original capitalization pattern
 *              to whatever [expansion] returns).
 * [expansion]  the full text substituted in when [shortcut] is matched.
 * [createdAt]  used only to order the list newest-first in Settings.
 */
@Entity(tableName = "shortcuts", primaryKeys = ["shortcut"])
data class ShortcutEntity(
    val shortcut: String,
    val expansion: String,
    val createdAt: Long = System.currentTimeMillis()
)
