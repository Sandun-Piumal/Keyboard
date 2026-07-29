package com.spmods.sinkey.data.sticker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.spmods.sinkey.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * One sticker found inside a connected external folder (WhatsApp/Telegram
 * sticker directory). [uri] is a SAF document URI — resolve it with
 * ContentResolver.openInputStream when actually decoding/committing it;
 * nothing is copied out of the source app's folder ahead of time.
 */
data class ExternalSticker(val uri: Uri, val displayName: String)

/**
 * Opt-in, read-only access to sticker folders living inside other apps'
 * storage (WhatsApp, Telegram) via the Storage Access Framework.
 *
 * IMPORTANT — why SAF and not broad file access: a keyboard app requesting
 * MANAGE_EXTERNAL_STORAGE (all-files access) to read another app's private
 * media folder is very unlikely to survive Google Play's review for a
 * keyboard app, since that permission is meant for file-manager-class apps.
 * ACTION_OPEN_DOCUMENT_TREE instead asks the user, once, to explicitly pick
 * a folder in the system picker; the resulting permission is scoped to just
 * that folder tree and is Play-safe — the same mechanism Google Photos,
 * file managers, etc. use for "let this app access this one folder".
 *
 * Nothing here runs automatically: a folder only appears in the Sticker
 * board's "WhatsApp" / "Telegram" tab after the user has gone through
 * Settings → connected it via the system folder picker themselves.
 */
class ExternalStickerSource(private val context: Context) {

    private val prefs = PreferencesManager(context)

    /** Currently-connected external folders as DocumentFile roots, re-resolved live (not cached) each collection. */
    val connectedFolders: Flow<List<DocumentFile>> = prefs.externalStickerFolders.map { uriStrings ->
        uriStrings.mapNotNull { uriString ->
            runCatching {
                DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            }.getOrNull()?.takeIf { it.isDirectory && it.canRead() }
        }
    }

    /**
     * Lists sticker image files (.webp, .png) directly inside [folder].
     * Not recursive — WhatsApp/Telegram sticker packs are typically one
     * flat folder per pack; if the user points at a parent folder containing
     * multiple pack subfolders, only the picked level is scanned, which
     * keeps this predictable and fast rather than walking an unbounded tree.
     */
    suspend fun listStickers(folder: DocumentFile): List<ExternalSticker> = withContext(Dispatchers.IO) {
        folder.listFiles()
            .filter { file ->
                file.isFile && file.type?.let { it == "image/webp" || it == "image/png" } == true
            }
            .map { file -> ExternalSticker(uri = file.uri, displayName = file.name ?: "sticker") }
    }

    /**
     * Records [uri] as a connected external sticker folder. Callers (e.g.
     * SettingsScreen's folder picker) must call
     * ContentResolver.takePersistableUriPermission on [uri] themselves
     * *before* this — only an Activity context can take a persistable grant,
     * so that step can't live here. Typical folders users pick:
     *   Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers
     *   Android/media/org.telegram.messenger/Telegram/Telegram Stickers
     */
    suspend fun connectFolder(uri: Uri) {
        prefs.addExternalStickerFolder(uri.toString())
    }

    suspend fun disconnectFolder(uri: Uri) {
        prefs.removeExternalStickerFolder(uri.toString())
    }
}
