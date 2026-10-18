package com.bettertick.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

    /**
     * True when the last check failed only because CI was re-publishing the
     * release (it is deleted and re-created on every push to main, so for a
     * minute the tag 404s or lacks the APK). Not a real error — try later.
     */
    @Volatile var lastFetchPublishing: Boolean = false
        private set

    const val PUBLISHING_MESSAGE = "새 버전을 배포하는 중이에요. 1~2분 뒤 다시 확인해 주세요."

    private class HttpStatusException(val code: Int, message: String) : java.io.IOException(message)

    /** 마지막 downloadApk 실패 사유. UI 진단용 — 성공 시 null. */
    @Volatile var lastDownloadError: String? = null
        private set

    /**
     * Latest release, or null (reason in [lastFetchError]). While CI is
     * re-publishing it retries a few times before giving up.
     */
    suspend fun fetchLatest(): Release? {
        repeat(3) { attempt ->
            val r = fetchOnce()
            if (r != null || !lastFetchPublishing) return r
            if (attempt < 2) kotlinx.coroutines.delay(5_000)
        }
        return null
    }

    private suspend fun fetchOnce(): Release? = withContext(Dispatchers.IO) {
        lastFetchPublishing = false
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
                // Release exists but its files are still uploading.
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
                apkName = apkName,
                sizeBytes = apkSize
            )
        } catch (e: HttpStatusException) {
            if (e.code == 404) {
                // Old release already deleted, new one not created yet.
                lastFetchPublishing = true
                lastFetchError = PUBLISHING_MESSAGE
            } else {
                lastFetchError = e.message
            }
            Log.w(TAG, "fetchLatest failed", e)
            null
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
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            if (githubAccept) {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
        }
        // GitHub API는 rate limit/오류 시 403/422 등으로 응답 + JSON body 포함.
        // inputStream은 4xx/5xx에서 throw하므로 errorStream을 같이 읽어 사유 노출.
        val code = conn.responseCode
        return if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw HttpStatusException(code, "HTTP $code from $url — body: ${errBody.take(300)}")
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

    /** Result of [run], for the UI. */
    sealed class Outcome {
        data class UpToDate(val current: Long, val latest: Long) : Outcome()
        data class Failed(val message: String) : Outcome()
        object Publishing : Outcome()
        object Busy : Outcome()
        /** Downloaded; installs when the app goes to the background. */
        data class Ready(val versionName: String) : Outcome()
        /** Handed to the system installer (silent, or its confirm screen). */
        data class Installing(val versionName: String) : Outcome()
    }

    enum class Trigger {
        /** App opened — quiet; a silent update waits until the app is left. */
        APP_OPEN,
        /** 설정 > 업데이트 확인 — install right away. */
        USER,
        /** Periodic worker, app not on screen. */
        BACKGROUND
    }

    private val mutex = kotlinx.coroutines.sync.Mutex()

    /** A verified APK waiting for the app to leave the screen (silent update). */
    @Volatile private var pending: Pair<File, String>? = null

    /** True while any activity of ours is started — kept by BetterTickApplication. */
    @Volatile var isForeground: Boolean = false

    /**
     * Check → download → verify → install, one run at a time (an app-open
     * check, the worker and the settings button used to download into the
     * same file at once and the installer got a half-written APK —
     * "앱 파일에 문제가 있습니다").
     */
    suspend fun run(
        context: Context,
        trigger: Trigger,
        onDownloading: suspend (Release) -> Unit = {}
    ): Outcome {
        val app = context.applicationContext
        if (!mutex.tryLock()) return Outcome.Busy
        try {
            val release = fetchLatest()
                ?: return if (lastFetchPublishing) Outcome.Publishing
                else Outcome.Failed("업데이트 확인 실패: ${lastFetchError ?: "(원인 불명)"}")
            val current = currentVersionCode(app)
            if (!isNewer(release.versionCode, current)) return Outcome.UpToDate(current, release.versionCode)

            onDownloading(release)
            val apk = downloadVerified(app, release)
                ?: return Outcome.Failed("다운로드 실패: ${lastDownloadError ?: "(원인 불명)"}")

            // Updating ourselves without a prompt (Android 12+, once this app
            // installed the current version) would close the app mid-use, so
            // unless the user asked for it, it waits until the app is left.
            if (trigger != Trigger.USER && canInstallSilently(app) && isForeground) {
                pending = apk to release.versionName
                return Outcome.Ready(release.versionName)
            }
            withContext(Dispatchers.IO) { install(app, apk, release.versionName) }
            return Outcome.Installing(release.versionName)
        } catch (e: Exception) {
            Log.w(TAG, "update run failed", e)
            return Outcome.Failed("업데이트 실패: ${e.javaClass.simpleName}: ${e.message ?: ""}")
        } finally {
            mutex.unlock()
        }
    }

    /** The app left the screen — install a silent update that was waiting. */
    fun onAppBackground(context: Context) {
        val (apk, name) = pending ?: return
        pending = null
        if (!apk.exists()) return
        val app = context.applicationContext
        // Copying the APK into the session is disk work — off the main thread.
        Thread {
            runCatching { install(app, apk, name) }
                .onFailure { Log.w(TAG, "deferred install failed", it) }
        }.start()
    }

    /**
     * Android 12+ lets an app update itself with no confirm screen when it
     * was the installer of the version now on the phone (true after the
     * first update through [install]).
     */
    fun canInstallSilently(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName ==
                context.packageName
        }.getOrDefault(false)
    }

    /**
     * Downloads [release] to updates/BetterTick-<code>.apk and checks it is a
     * whole APK of this app (size + the package manager can read it). A file
     * already downloaded for that version is reused. Two tries.
     */
    private suspend fun downloadVerified(context: Context, release: Release): File? =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "BetterTick-${release.versionCode}.apk")
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
                    Log.w(TAG, "downloadApk failed", e)
                    "${e.javaClass.simpleName}: ${e.message ?: "(no message)"}"
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

    /** Null when [apk] is a complete APK of this app; else what is wrong. */
    private fun verify(context: Context, apk: File, release: Release): String? {
        if (release.sizeBytes > 0 && apk.length() != release.sizeBytes) {
            return "받은 파일 크기가 달라요 (${apk.length()} / ${release.sizeBytes} 바이트)"
        }
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: return "APK를 읽을 수 없어요 (파일 손상)"
        if (info.packageName != context.packageName) return "다른 앱의 APK예요 (${info.packageName})"
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
        if (code <= currentVersionCode(context)) return "APK 버전($code)이 설치된 버전보다 높지 않아요"
        return null
    }

    private fun downloadTo(startUrl: String, out: File) {
        // GitHub asset URL은 objects.githubusercontent.com 같은 CDN으로
        // cross-host redirect됨. Android의 HttpURLConnection은 일부 버전에서
        // 자동 follow가 깨지므로 수동으로 최대 5회까지 따라간다.
        var url = startUrl
        var hop = 0
        while (true) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 60000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "BetterTick-AppUpdater")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw java.io.IOException("HTTP $code without Location header")
                    url = if (loc.startsWith("http")) loc else URL(URL(url), loc).toString()
                    if (++hop >= 5) throw java.io.IOException("redirect loop > 5 hops")
                    continue
                }
                if (code !in 200..299) {
                    val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw HttpStatusException(code, "HTTP $code from $url — body: ${errBody.take(300)}")
                }
                conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                return
            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * Hands [apk] to the system through a PackageInstaller session. The bytes
     * are copied into the session first, so later checks can't spoil them.
     * Without a prompt when allowed ([canInstallSilently]); otherwise
     * [UpdateInstallReceiver] shows the system confirm screen.
     */
    fun install(context: Context, apk: File, versionName: String) {
        val installer = context.packageManager.packageInstaller
        val params = android.content.pm.PackageInstaller.SessionParams(
            android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
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
