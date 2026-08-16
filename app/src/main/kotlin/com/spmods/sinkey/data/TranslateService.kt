package com.spmods.sinkey.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Live text translation for the keyboard's Translate tool (TOOL_TRANSLATE).
 *
 * Uses the free, unofficial `translate.googleapis.com/translate_a/single`
 * endpoint — the same one Gboard-alike open-source keyboards (and many
 * translate-in-app-bar implementations) rely on. No API key, no quota
 * management, works out of the box. Trade-off: it's an undocumented
 * endpoint, not the official Cloud Translation API, so Google could change
 * or rate-limit it without notice — if that ever happens, swapping this one
 * function's implementation for the official (paid, key-required) Cloud
 * Translation API is a drop-in replacement; nothing above this file needs
 * to change.
 *
 * Mirrors [UpdateChecker]'s reasoning for using plain HttpURLConnection
 * instead of a dependency: this project has no Retrofit/OkHttp, and a
 * single small GET request doesn't justify pulling one in.
 */
internal object TranslateService {
    private const val TAG = "TranslateService"
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 6_000

    /**
     * Translates [text] from [sourceLang] to [targetLang] (ISO 639-1 codes,
     * e.g. "en", "si"). Returns null on any failure (offline, timeout,
     * malformed response, blank input) — callers must treat null as
     * "nothing to show" and leave whatever was on screen before untouched,
     * the same silence-on-failure contract as [UpdateChecker.checkForUpdate].
     * This runs on every keystroke in the translate board (debounced by the
     * caller), so it must never throw and must stay cheap to call.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String? =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext null
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single" +
                    "?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encoded"

                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }

                // Response shape: [[["translated chunk","original chunk",...],...],...]
                // Only the first element of each top-level sentence chunk is
                // the translated text; multiple chunks are joined with no
                // separator (matches how the sentence was originally split).
                val root = JSONArray(body)
                val sentences = root.optJSONArray(0) ?: return@withContext null
                val translated = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val chunk = sentences.optJSONArray(i) ?: continue
                    translated.append(chunk.optString(0, ""))
                }

                translated.toString().ifBlank { null }
            } catch (e: Exception) {
                // Offline, timeout, endpoint shape changed, etc. — silent,
                // same contract as UpdateChecker. The translate board shows
                // whatever was last successfully translated (or nothing)
                // rather than an error state.
                Log.w(TAG, "Translate request failed (sl=$sourceLang tl=$targetLang)", e)
                null
            }
        }
}
