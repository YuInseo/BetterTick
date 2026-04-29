package com.bettertick.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Polls the BetterTick repo's `latest-debug` Release, compares its
 * versionCode (from the version.json asset) against the installed APK's
 * versionCode, and if newer downloads BetterTick.apk and launches the
 * system installer prompt. Network + disk I/O on IO dispatcher.
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val API_URL =
        "https://api.github.com/repos/YuInseo/BetterTick/releases/tags/latest-debug"

    data class Release(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        val apkName: String,
        val sizeBytes: Long
    )

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val releaseJson = JSONObject(httpGetText(API_URL, githubAccept = true))
            val assets = releaseJson.optJSONArray("assets") ?: return@runCatching null

            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L
            var versionJsonUrl = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                val url = a.optString("browser_download_url")
                when {
                    name.endsWith(".apk", ignoreCase = true) -> {
                        apkUrl = url; apkName = name; apkSize = a.optLong("size")
                    }
                    name == "version.json" -> versionJsonUrl = url
                }
            }
            if (apkUrl.isEmpty() || versionJsonUrl.isEmpty()) return@runCatching null

            val v = JSONObject(httpGetText(versionJsonUrl, githubAccept = false))
            Release(
                versionCode = v.optLong("versionCode"),
                versionName = v.optString("versionName"),
                apkUrl = apkUrl,
                apkName = apkName,
                sizeBytes = apkSize
            )
        }.onFailure { Log.w(TAG, "fetchLatest failed", it) }.getOrNull()
    }

    private fun httpGetText(url: String, githubAccept: Boolean): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            if (githubAccept) setRequestProperty("Accept", "application/vnd.github+json")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    fun isNewer(latestCode: Long, currentCode: Long): Boolean = latestCode > currentCode

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
}
