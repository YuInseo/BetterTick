package com.bettertick.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight GitHub Releases updater. Polls the bettertick repo's latest
 * release, compares its tag (semver-ish) against the installed versionName,
 * and if newer downloads the APK asset and launches the system installer
 * prompt. Network + disk I/O happen on IO dispatcher; callers handle the UI.
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val API_URL =
        "https://api.github.com/repos/yuinseo/bettertick/releases/latest"
    private const val PREFS = "app_updater"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L // 12h

    data class Release(
        val tag: String,
        val apkUrl: String,
        val apkName: String,
        val sizeBytes: Long
    )

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").trimStart('v')
            val assets = json.optJSONArray("assets") ?: return@runCatching null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return@runCatching Release(
                        tag = tag,
                        apkUrl = a.optString("browser_download_url"),
                        apkName = name,
                        sizeBytes = a.optLong("size")
                    )
                }
            }
            null
        }.onFailure { Log.w(TAG, "fetchLatest failed", it) }.getOrNull()
    }

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

    /** Strict numeric compare on dot-separated segments. "0.1.1" > "0.1.0". */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val n = maxOf(l.size, c.size)
        for (i in 0 until n) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun downloadApk(context: Context, release: Release): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, release.apkName)
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }.onFailure { Log.w(TAG, "downloadApk failed", it) }.getOrNull()
        }

    fun launchInstall(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        context.startActivity(intent)
    }

    /** Without REQUEST_INSTALL_PACKAGES granted, the install intent silently
     *  no-ops. minSdk 28 ≥ O so we always run the check. */
    fun canRequestInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Bounce the user to the per-app "install unknown apps" toggle so they
     *  can grant permission, then come back to the app for the next launch. */
    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "openInstallPermissionSettings failed", it) }
    }

    /** True if it has been longer than [CHECK_INTERVAL_MS] since the last
     *  successful poll. Avoids hammering the GitHub API on every cold start. */
    fun shouldCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }
}
