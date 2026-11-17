package com.bettertick.recorder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 녹음 앱 자체 업데이트.
 * GitHub `recorder-latest` 릴리즈의 version.json에 적힌 versionCode를 설치된
 * 버전과 비교해, 더 높으면 APK를 받아 시스템 설치창(또는 조용히) 설치한다.
 * 본 앱(AppUpdater)과 같은 방식.
 */
object Updater {
    private const val TAG = "RecUpdater"
    private const val API_URL =
        "https://api.github.com/repos/YuInseo/BetterTick/releases/tags/recorder-latest"
    const val PUBLISHING_MESSAGE = "새 버전을 배포하는 중이에요. 1~2분 뒤 다시 확인해 주세요."

    data class Release(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        val sizeBytes: Long,
    )

    private class HttpStatusException(val code: Int, message: String) : IOException(message)

    @Volatile var lastFetchError: String? = null; private set
    @Volatile var lastFetchPublishing: Boolean = false; private set
    @Volatile var lastDownloadError: String? = null; private set

    /** 현재 어떤 화면이든 떠 있는 동안 true. [RecorderApp]이 관리. */
    @Volatile var isForeground: Boolean = false

    private val mutex = Mutex()
    @Volatile private var pending: Pair<File, String>? = null

    suspend fun fetchLatest(): Release? {
        repeat(3) { attempt ->
            val r = fetchOnce()
            if (r != null || !lastFetchPublishing) return r
            if (attempt < 2) delay(5_000)
        }
        return null
    }

    private suspend fun fetchOnce(): Release? = withContext(Dispatchers.IO) {
        lastFetchPublishing = false
        try {
            val releaseJson = JSONObject(httpGetText(API_URL, githubAccept = true))
            val assets = releaseJson.optJSONArray("assets") ?: run {
                lastFetchError = "릴리즈에 파일이 없어요: ${releaseJson.optString("message")}"
                return@withContext null
            }
            var apkUrl = ""; var apkSize = 0L; var versionJsonUrl = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                val url = a.optString("browser_download_url")
                when {
                    name.endsWith(".apk", true) -> { apkUrl = url; apkSize = a.optLong("size") }
                    name == "version.json" -> versionJsonUrl = url
                }
            }
            if (apkUrl.isEmpty() || versionJsonUrl.isEmpty()) {
                lastFetchPublishing = true
                lastFetchError = PUBLISHING_MESSAGE
                return@withContext null
            }
            val v = JSONObject(httpGetText(versionJsonUrl, githubAccept = false))
            lastFetchError = null
            Release(
                versionCode = v.optLong("versionCode"),
                versionName = v.optString("versionName"),
                apkUrl = apkUrl,
                sizeBytes = apkSize,
            )
        } catch (e: HttpStatusException) {
            if (e.code == 404) { lastFetchPublishing = true; lastFetchError = PUBLISHING_MESSAGE }
            else lastFetchError = e.message
            Log.w(TAG, "fetch failed", e)
            null
        } catch (e: Exception) {
            lastFetchError = "${e.javaClass.simpleName}: ${e.message ?: ""}"
            Log.w(TAG, "fetch failed", e)
            null
        }
    }

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    fun currentVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

    sealed class Outcome {
        data class UpToDate(val current: Long, val latest: Long) : Outcome()
        data class Failed(val message: String) : Outcome()
        object Publishing : Outcome()
        object Busy : Outcome()
        data class Ready(val versionName: String) : Outcome()
        data class Installing(val versionName: String) : Outcome()
    }

    enum class Trigger { APP_OPEN, USER }

    suspend fun run(context: Context, trigger: Trigger): Outcome {
        val app = context.applicationContext
        if (!mutex.tryLock()) return Outcome.Busy
        try {
            val release = fetchLatest()
                ?: return if (lastFetchPublishing) Outcome.Publishing
                else Outcome.Failed("업데이트 확인 실패: ${lastFetchError ?: "(원인 불명)"}")
            val current = currentVersionCode(app)
            if (release.versionCode <= current) return Outcome.UpToDate(current, release.versionCode)

            val apk = downloadVerified(app, release)
                ?: return Outcome.Failed("다운로드 실패: ${lastDownloadError ?: "(원인 불명)"}")

            // 녹음 중일 수 있으니, 사용자가 직접 누른 게 아니면 앱을 나갈 때 조용히 설치.
            if (trigger != Trigger.USER && canInstallSilently(app) && isForeground) {
                pending = apk to release.versionName
                return Outcome.Ready(release.versionName)
            }
            withContext(Dispatchers.IO) { install(app, apk, release.versionName) }
            return Outcome.Installing(release.versionName)
        } catch (e: Exception) {
            Log.w(TAG, "run failed", e)
            return Outcome.Failed("업데이트 실패: ${e.javaClass.simpleName}: ${e.message ?: ""}")
        } finally {
            mutex.unlock()
        }
    }

    fun onAppBackground(context: Context) {
        val (apk, name) = pending ?: return
        pending = null
        if (!apk.exists()) return
        val app = context.applicationContext
        Thread { runCatching { install(app, apk, name) }.onFailure { Log.w(TAG, "deferred install", it) } }.start()
    }

    fun canInstallSilently(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName ==
                context.packageName
        }.getOrDefault(false)
    }

    private suspend fun downloadVerified(context: Context, release: Release): File? =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "DaydriftRecorder-${release.versionCode}.apk")
            dir.listFiles()?.forEach { if (it != file) it.delete() }
            if (file.exists() && verify(context, file, release) == null) {
                lastDownloadError = null
                return@withContext file
            }
            val part = File(dir, file.name + ".part")
            var problem: String? = null
            repeat(2) {
                file.delete(); part.delete()
                problem = try {
                    downloadTo(release.apkUrl, part)
                    verify(context, part, release)
                } catch (e: Exception) {
                    Log.w(TAG, "download failed", e)
                    "${e.javaClass.simpleName}: ${e.message ?: ""}"
                }
                if (problem == null && part.renameTo(file)) {
                    lastDownloadError = null
                    return@withContext file
                }
            }
            part.delete()
            lastDownloadError = problem ?: "파일 저장 실패"
            null
        }

    private fun verify(context: Context, apk: File, release: Release): String? {
        if (release.sizeBytes > 0 && apk.length() != release.sizeBytes)
            return "받은 파일 크기가 달라요"
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: return "APK를 읽을 수 없어요 (파일 손상)"
        if (info.packageName != context.packageName) return "다른 앱의 APK예요 (${info.packageName})"
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
        if (code <= currentVersionCode(context)) return "APK 버전이 설치된 버전보다 높지 않아요"
        return null
    }

    private fun downloadTo(startUrl: String, out: File) {
        var url = startUrl
        var hop = 0
        while (true) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 60000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Daydrift-Recorder-Updater")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw IOException("HTTP $code without Location")
                    url = if (loc.startsWith("http")) loc else URL(URL(url), loc).toString()
                    if (++hop >= 5) throw IOException("redirect loop")
                    continue
                }
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw HttpStatusException(code, "HTTP $code — ${err.take(200)}")
                }
                conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                return
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun httpGetText(url: String, githubAccept: Boolean): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
            if (githubAccept) {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
        }
        val code = conn.responseCode
        return if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
        else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw HttpStatusException(code, "HTTP $code — ${err.take(200)}")
        }
    }

    fun install(context: Context, apk: File, versionName: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
                .putExtra(UpdateInstallReceiver.EXTRA_VERSION, versionName)
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0)
            val sender = android.app.PendingIntent.getBroadcast(context, id, intent, flags)
            session.commit(sender.intentSender)
        }
    }
}
