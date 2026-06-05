package com.bettertick

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
        requestOverlayPermissionIfNeeded()
    }

    private fun requestOverlayPermissionIfNeeded() {
        if (Settings.canDrawOverlays(this)) return

        val prefs = getSharedPreferences("bettertick_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("overlay_permission_asked", false)) return

        prefs.edit().putBoolean("overlay_permission_asked", true).apply()

        android.app.AlertDialog.Builder(this)
            .setTitle("다른 앱 위에 표시 권한")
            .setMessage(
                "잠금화면 및 다른 앱 위에서 빠른 추가/메모 팝업을 사용하려면\n" +
                "\"다른 앱 위에 표시\" 권한이 필요합니다."
            )
            .setPositiveButton("권한 허용") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("나중에") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val release = AppUpdater.fetchLatest() ?: return@launch
            val current = AppUpdater.currentVersionCode(this@MainActivity)
            if (!AppUpdater.isNewer(release.versionCode, current)) return@launch
            Log.i("MainActivity", "Update ${release.versionName} (code ${release.versionCode}) available (current $current)")
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
