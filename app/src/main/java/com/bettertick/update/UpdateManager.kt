package com.bettertick.update

import android.content.Context
import com.bettertick.BuildConfig

/**
 * Coordinates the in-app update flow on top of [AppUpdater]. The auto check
 * fired from app start is silent — failures are swallowed and we just retry
 * on the next launch. The manual check from settings drives a UI-facing
 * state machine so the dialog can show progress.
 */
class UpdateManager(private val appContext: Context) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(
            val manifest: VersionManifest,
            val currentVersionName: String,
        ) : State
        data class Downloading(val percent: Int) : State
        data object NeedsInstallPermission : State
        data object Failed : State
    }

    /**
     * Auto check on app start. Throttled — silent on failure, hands off to
     * the system installer if a newer release is present and the user has
     * install-from-unknown-sources permission.
     */
    suspend fun runAutoCheck() {
        if (!AppUpdater.shouldCheck(appContext)) return
        val manifest = AppUpdater.fetchManifest() ?: return
        AppUpdater.markChecked(appContext)
        if (!AppUpdater.isNewer(manifest)) return
        if (!AppUpdater.canRequestInstall(appContext)) {
            AppUpdater.openInstallPermissionSettings(appContext)
            return
        }
        val apk = AppUpdater.downloadApk(appContext, manifest) { /* ignore */ } ?: return
        AppUpdater.installApk(appContext, apk)
    }

    /**
     * Manual check driven from the settings screen. Bypasses the throttle
     * so the user always sees a fresh result.
     */
    suspend fun checkManual(emit: (State) -> Unit) {
        emit(State.Checking)
        val manifest = AppUpdater.fetchManifest()
        if (manifest == null) {
            emit(State.Failed)
            return
        }
        AppUpdater.markChecked(appContext)
        emit(
            if (AppUpdater.isNewer(manifest))
                State.Available(manifest, BuildConfig.VERSION_NAME)
            else
                State.UpToDate
        )
    }

    /**
     * Download the APK with progress callbacks and trigger the install.
     * Surfaces a NeedsInstallPermission state if the per-app
     * "install unknown apps" toggle is off.
     */
    suspend fun downloadAndInstall(
        manifest: VersionManifest,
        emit: (State) -> Unit,
    ) {
        if (!AppUpdater.canRequestInstall(appContext)) {
            emit(State.NeedsInstallPermission)
            return
        }
        emit(State.Downloading(0))
        val apk = AppUpdater.downloadApk(appContext, manifest) { p ->
            emit(State.Downloading(p))
        }
        if (apk == null) {
            emit(State.Failed)
            return
        }
        AppUpdater.installApk(appContext, apk)
    }

    fun openInstallPermissionSettings() {
        AppUpdater.openInstallPermissionSettings(appContext)
    }

    fun currentVersion(): String = BuildConfig.VERSION_NAME
}
