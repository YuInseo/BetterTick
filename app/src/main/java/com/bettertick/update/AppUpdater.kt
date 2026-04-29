package com.bettertick.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.bettertick.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class VersionManifest(
    val versionCode: Int,
    val versionName: String,
    val sha: String = "",
    val apkUrl: String,
    val notes: String = "",
)

/**
 * Low-level update operations: fetches a [VersionManifest] from the URL
 * baked into BuildConfig, downloads the APK, and hands off to the system
 * PackageInstaller. The caller (UpdateManager) drives the state machine.
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val PREFS = "app_updater"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L // 12h

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchManifest(): VersionManifest? = withContext(Dispatchers.IO) {
        runCatching {
            val cacheBuster = "?_=" + System.currentTimeMillis()
            val conn = (URL(BuildConfig.VERSION_MANIFEST_URL + cacheBuster)
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 10000
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("User-Agent", "BetterTick/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(VersionManifest.serializer(), text)
        }.onFailure { Log.w(TAG, "fetchManifest failed", it) }.getOrNull()
    }

    fun isNewer(manifest: VersionManifest): Boolean =
        manifest.versionCode > BuildConfig.VERSION_CODE

    suspend fun downloadApk(
        context: Context,
        manifest: VersionManifest,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // 이전 받기 부스러기 청소 — 디스크 누적 방지.
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "BetterTick-${manifest.versionCode}.apk")
            val conn = (URL(manifest.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            outFile
        }.onFailure { Log.w(TAG, "downloadApk failed", it) }.getOrNull()
    }

    /**
     * Stream the APK into a PackageInstaller session and commit. The system
     * shows the install confirmation; result lands in InstallResultReceiver
     * which can relaunch the app on success.
     */
    fun installApk(context: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            installViaPackageInstaller(context, apk)
        } else {
            // 28 minSdk 이라 사실상 안 닿음 — 안전 폴백만.
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apk)
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

    private fun installViaPackageInstaller(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("apk", 0, -1).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                action = InstallResultReceiver.ACTION
                setPackage(context.packageName)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        PendingIntent.FLAG_MUTABLE else 0
            )
            session.commit(pi.intentSender)
        }
    }

    /** API 26+ requires the user to opt the app into installing unknown
     *  sources. minSdk 28 ≥ O so we always run the check. */
    fun canRequestInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "openInstallPermissionSettings failed", it) }
    }

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
