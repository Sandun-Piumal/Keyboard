package com.spmods.sinkey.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

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
     * Distinguishes *why* a translation didn't come back, so the caller can
     * show the user the right message instead of one generic "didn't work"
     * state. [Success.text] is guaranteed non-blank.
     */
    sealed class TranslateResult {
        data class Success(val text: String) : TranslateResult()

        /**
         * No connectivity, DNS failure, or the request timed out — the
         * device couldn't reach the server at all. Maps to a
         * "No internet connection" message, distinct from [ServiceError].
         */
        object NoConnection : TranslateResult()

        /**
         * The device reached a server, but the response wasn't usable: a
         * non-200 HTTP status (e.g. 403 if Google blocks the endpoint),
         * malformed/unexpected JSON shape, or a blank translation. Maps to
         * a "Translation failed" message, distinct from [NoConnection]
         * since retrying immediately might actually help here in a way it
         * usually won't for a connectivity problem.
         */
        object ServiceError : TranslateResult()
    }

    /**
     * Translates [text] from [sourceLang] to [targetLang] (ISO 639-1 codes,
     * e.g. "en", "si"). Returns null only for blank input (nothing to
     * translate, not a failure). Every other outcome is a [TranslateResult]
     * so the caller can tell "offline" apart from "reached the server but
     * it failed" and show the right message for each — callers must still
     * treat any non-Success as "nothing to show" and leave whatever was on
     * screen before untouched, same as the old silence-on-failure contract,
     * but now with enough information to also surface *why*.
     * This runs on every keystroke in the translate board (debounced by the
     * caller), so it must never throw and must stay cheap to call.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): TranslateResult? =
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
                    // Without a browser-like User-Agent, translate.googleapis.com
                    // treats the request as bot traffic and returns 403 Forbidden
                    // instead of a translation — this header is required, not
                    // cosmetic. (Confirmed against community implementations of
                    // this same unofficial endpoint.)
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Reached a server, got a real HTTP response, just not
                    // a usable one (e.g. 403 if the endpoint starts
                    // rejecting us) — a service-side problem, not a
                    // connectivity one.
                    Log.w(TAG, "Translate request failed: HTTP $responseCode (sl=$sourceLang tl=$targetLang)")
                    return@withContext TranslateResult.ServiceError
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }

                // Response shape: [[["translated chunk","original chunk",...],...],...]
                // Only the first element of each top-level sentence chunk is
                // the translated text; multiple chunks are joined with no
                // separator (matches how the sentence was originally split).
                val root = JSONArray(body)
                val sentences = root.optJSONArray(0)
                    ?: run {
                        Log.w(TAG, "Translate response missing expected array shape (sl=$sourceLang tl=$targetLang)")
                        return@withContext TranslateResult.ServiceError
                    }
                val translated = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val chunk = sentences.optJSONArray(i) ?: continue
                    translated.append(chunk.optString(0, ""))
                }

                val finalText = translated.toString()
                if (finalText.isBlank()) {
                    TranslateResult.ServiceError
                } else {
                    TranslateResult.Success(finalText)
                }
            } catch (e: UnknownHostException) {
                // DNS lookup failed — the classic "no internet" signature.
                Log.w(TAG, "Translate request failed: no connection (sl=$sourceLang tl=$targetLang)", e)
                TranslateResult.NoConnection
            } catch (e: SocketTimeoutException) {
                // Couldn't reach the server in time — treated as a
                // connectivity problem rather than a service one, since a
                // slow/flaky connection is the far more common cause than
                // the server itself being slow.
                Log.w(TAG, "Translate request failed: timeout (sl=$sourceLang tl=$targetLang)", e)
                TranslateResult.NoConnection
            } catch (e: IOException) {
                // Broader network-layer failure (connection reset, no
                // route to host, etc.) — same bucket as the two above.
                Log.w(TAG, "Translate request failed: network error (sl=$sourceLang tl=$targetLang)", e)
                TranslateResult.NoConnection
            } catch (e: Exception) {
                // Anything else (malformed JSON, unexpected shape, etc.) —
                // we did get a response, it just wasn't usable.
                Log.w(TAG, "Translate request failed: service error (sl=$sourceLang tl=$targetLang)", e)
                TranslateResult.ServiceError
            }
        }
}
