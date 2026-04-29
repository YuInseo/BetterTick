package com.bettertick.update

import android.content.Context

/**
 * Coordinates the in-app update flow on top of [AppUpdater]. The auto check
 * fired from app start is silent — failures are swallowed and we just retry
 * on the next launch. The manual check from settings drives a UI-facing
 * state machine so the dialog can show "checking", "up to date",
 * "available", etc.
 */
class UpdateManager(private val appContext: Context) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(
            val release: AppUpdater.Release,
            val currentVersion: String,
        ) : State
        data class Downloading(val release: AppUpdater.Release) : State
        data object NeedsInstallPermission : State
        data object Failed : State
    }

    /**
     * Auto check on app start. Throttled by [AppUpdater.shouldCheck], silent
     * on failure, hands off to the system installer if a newer release is
     * present and the user has install-from-unknown-sources permission.
     */
    suspend fun runAutoCheck() {
        if (!AppUpdater.shouldCheck(appContext)) return
        val release = AppUpdater.fetchLatest() ?: return
        AppUpdater.markChecked(appContext)
        val current = AppUpdater.currentVersion(appContext)
        if (!AppUpdater.isNewer(release.tag, current)) return
        if (!AppUpdater.canRequestInstall(appContext)) {
            AppUpdater.openInstallPermissionSettings(appContext)
            return
        }
        val apk = AppUpdater.downloadApk(appContext, release) ?: return
        AppUpdater.launchInstall(appContext, apk)
    }

    /**
     * Manual check driven from the settings screen. Bypasses the throttle
     * so the user always sees a fresh result, and emits a state the caller
     * can render. [emit] is called inline so the UI can update progress
     * (Checking → Available, etc).
     */
    suspend fun checkManual(emit: (State) -> Unit) {
        emit(State.Checking)
        val release = AppUpdater.fetchLatest()
        if (release == null) {
            emit(State.Failed)
            return
        }
        AppUpdater.markChecked(appContext)
        val current = AppUpdater.currentVersion(appContext)
        emit(
            if (AppUpdater.isNewer(release.tag, current))
                State.Available(release, current)
            else
                State.UpToDate
        )
    }

    /**
     * Download the APK and trigger the system installer prompt. Surfaces
     * a NeedsInstallPermission state if the per-app "install unknown apps"
     * toggle is off — caller should still bounce the user to settings via
     * [openInstallPermissionSettings] so they can grant it.
     */
    suspend fun downloadAndInstall(
        release: AppUpdater.Release,
        emit: (State) -> Unit,
    ) {
        if (!AppUpdater.canRequestInstall(appContext)) {
            emit(State.NeedsInstallPermission)
            return
        }
        emit(State.Downloading(release))
        val apk = AppUpdater.downloadApk(appContext, release)
        if (apk == null) {
            emit(State.Failed)
            return
        }
        AppUpdater.launchInstall(appContext, apk)
    }

    fun openInstallPermissionSettings() {
        AppUpdater.openInstallPermissionSettings(appContext)
    }

    fun currentVersion(): String = AppUpdater.currentVersion(appContext)
}
