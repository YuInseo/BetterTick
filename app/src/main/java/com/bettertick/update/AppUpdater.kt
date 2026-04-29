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

    /** 마지막 fetchLatest 실패 사유. UI 진단용 — 성공 시 null. */
    @Volatile var lastFetchError: String? = null
        private set

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        try {
            val releaseJson = JSONObject(httpGetText(API_URL, githubAccept = true))
            val assets = releaseJson.optJSONArray("assets")
            if (assets == null) {
                lastFetchError = "API 응답에 assets 없음 (release 미발행?): ${releaseJson.optString("message")}"
                return@withContext null
            }

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
            if (apkUrl.isEmpty() || versionJsonUrl.isEmpty()) {
                lastFetchError = "asset 누락 (apk=${apkUrl.isNotEmpty()}, json=${versionJsonUrl.isNotEmpty()})"
                return@withContext null
            }

            val v = JSONObject(httpGetText(versionJsonUrl, githubAccept = false))
            lastFetchError = null
            Release(
                versionCode = v.optLong("versionCode"),
                versionName = v.optString("versionName"),
                apkUrl = apkUrl,
                apkName = apkName,
                sizeBytes = apkSize
            )
        } catch (e: Exception) {
            lastFetchError = "${e.javaClass.simpleName}: ${e.message ?: "(no message)"}"
            Log.w(TAG, "fetchLatest failed", e)
            null
        }
    }

    private fun httpGetText(url: String, githubAccept: Boolean): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            if (githubAccept) setRequestProperty("Accept", "application/vnd.github+json")
        }
        // GitHub API는 rate limit/오류 시 403/422 등으로 응답 + JSON body 포함.
        // inputStream은 4xx/5xx에서 throw하므로 errorStream을 같이 읽어 사유 노출.
        val code = conn.responseCode
        return if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw java.io.IOException("HTTP $code from $url — body: ${errBody.take(300)}")
        }
    }

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    fun currentVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

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
