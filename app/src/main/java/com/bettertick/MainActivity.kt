package com.bettertick

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.bettertick.ui.navigation.BetterTickNavHost
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.update.AppUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val openQuickAdd = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        enableEdgeToEdge()
        setContent {
            BetterTickTheme {
                BetterTickNavHost(openQuickAdd = openQuickAdd)
            }
        }
        checkForUpdate()
    }

    /** Fire-and-forget update check: fetch latest release, download if newer,
     *  hand off to the system installer. Silent on failure — next launch retries.
     *  Throttled to once per 12h so we don't hammer the GitHub API. */
    private fun checkForUpdate() {
        if (!AppUpdater.shouldCheck(this)) return
        lifecycleScope.launch {
            val release = AppUpdater.fetchLatest() ?: return@launch
            AppUpdater.markChecked(this@MainActivity)
            val current = AppUpdater.currentVersion(this@MainActivity)
            if (!AppUpdater.isNewer(release.tag, current)) return@launch
            Log.i("MainActivity", "Update ${release.tag} available (current $current)")
            if (!AppUpdater.canRequestInstall(this@MainActivity)) {
                AppUpdater.openInstallPermissionSettings(this@MainActivity)
                return@launch
            }
            val apk = AppUpdater.downloadApk(this@MainActivity, release) ?: return@launch
            AppUpdater.launchInstall(this@MainActivity, apk)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false) == true) {
            openQuickAdd.value = true
            intent.removeExtra(EXTRA_OPEN_QUICK_ADD)
        }
    }

    companion object {
        const val EXTRA_OPEN_QUICK_ADD = "open_quick_add"
    }
}
