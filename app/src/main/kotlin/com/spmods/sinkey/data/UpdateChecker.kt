package com.spmods.sinkey.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of a successful remote update check.
 *
 * [versionCode] is compared against the installed app's own versionCode
 * (read from PackageManager at runtime — the project doesn't have
 * `buildConfig = true` set in build.gradle.kts, so BuildConfig.VERSION_CODE
 * isn't generated; PackageManager works regardless and needs no build
 * config changes).
 *
 * [url] is the link opened when the user taps the in-keyboard update
 * banner. It comes straight from the remote JSON, so the destination can
 * be changed at any time (e.g. site vs. a direct APK link) without an app
 * update — just by editing sinkeyboard.json in the Updates repo.
 */
internal data class RemoteUpdateInfo(
    val versionCode: Int,
    val url: String
)

/**
 * Fetches and parses the remote update-check JSON from GitHub, and compares
 * it against the installed app's own versionCode.
 *
 * Remote JSON source (edit here to publish a new "update available" banner):
 *   https://raw.githubusercontent.com/Sandun-Piumal/Updates/refs/heads/main/sinkeyboard.json
 * Expected shape: {"versionCode": 2, "url": "https://www.spmods.download"}
 *
 * This performs a plain network fetch with java.net.HttpURLConnection —
 * the project has no HTTP client dependency (no Retrofit/OkHttp), and the
 * payload is a single tiny JSON object, so pulling in a library for this
 * would be unjustified extra weight. org.json.JSONObject (Android SDK,
 * already available, no dependency needed) parses it.
 */
internal object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/Sandun-Piumal/Updates/refs/heads/main/sinkeyboard.json"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    /**
     * Returns the installed app's versionCode via PackageManager, or null if
     * it can't be read for any reason (should not normally happen, but this
     * runs on every keyboard show so it must never throw).
     */
    private fun installedVersionCode(context: Context): Int? = try {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        info.versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(TAG, "Could not read installed versionCode", e)
        null
    } catch (e: Exception) {
        Log.w(TAG, "Unexpected error reading installed versionCode", e)
        null
    }

    /**
     * Fetches the remote JSON and returns [RemoteUpdateInfo] only if the
     * remote versionCode is strictly greater than the installed one — i.e.
     * only when there's actually something newer to show. Returns null on
     * any failure (no network, malformed JSON, remote not newer, etc.) —
     * callers should treat null as "nothing to show", never as an error to
     * surface to the user; a failed background update check must be silent.
     */
    suspend fun checkForUpdate(context: Context): RemoteUpdateInfo? = withContext(Dispatchers.IO) {
        val installed = installedVersionCode(context) ?: return@withContext null

        try {
            val connection = (URL(UPDATE_JSON_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
            }

            connection.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                val json = JSONObject(body)

                val remoteVersionCode = json.optInt("versionCode", -1)
                val url = json.optString("url", "").trim()

                if (remoteVersionCode <= 0 || url.isBlank()) {
                    Log.w(TAG, "Remote JSON missing versionCode/url, ignoring")
                    return@withContext null
                }

                if (remoteVersionCode > installed) {
                    RemoteUpdateInfo(versionCode = remoteVersionCode, url = url)
                } else {
                    null // already up to date
                }
            }
        } catch (e: Exception) {
            // Any failure here (offline, DNS, malformed JSON, GitHub down, etc.)
            // must be silent — this check runs unattended every time the
            // keyboard opens and must never crash or show an error to the user.
            Log.w(TAG, "Update check failed (treated as no update available)", e)
            null
        }
    }
}
