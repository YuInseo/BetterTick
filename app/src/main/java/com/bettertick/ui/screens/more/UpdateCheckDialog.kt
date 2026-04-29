package com.bettertick.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bettertick.update.UpdateManager
import kotlinx.coroutines.launch

/**
 * Shown when the user taps "업데이트 확인" in settings. Drives a small state
 * machine: Checking → UpToDate / Available / Failed; Available transitions
 * to Downloading after the user confirms, and Downloading shows live
 * progress until the system installer takes over.
 */
@Composable
fun UpdateCheckDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { UpdateManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateManager.State>(UpdateManager.State.Idle) }

    LaunchedEffect(Unit) {
        manager.checkManual { state = it }
    }

    when (val s = state) {
        UpdateManager.State.Idle, UpdateManager.State.Checking -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("업데이트 확인 중") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("최신 릴리스를 확인하고 있어요…")
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        )

        UpdateManager.State.UpToDate -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("최신 버전입니다") },
            text = { Text("현재 ${manager.currentVersion()} 버전을 사용 중이에요.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        )

        is UpdateManager.State.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("새 버전 ${s.manifest.versionName}") },
            text = {
                Column {
                    Text(
                        "현재 ${s.currentVersionName} → ${s.manifest.versionName} " +
                            "(code ${s.manifest.versionCode}) 으로 업데이트할 수 있습니다."
                    )
                    if (s.manifest.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            s.manifest.notes,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        manager.downloadAndInstall(s.manifest) { state = it }
                    }
                }) { Text("지금 설치") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("나중에") } },
        )

        is UpdateManager.State.Downloading -> AlertDialog(
            onDismissRequest = {}, // 다운로드 중엔 닫기 막음
            title = { Text("다운로드 중") },
            text = {
                Column {
                    Text("새 버전을 받는 중… ${s.percent}%")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { s.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
        )

        UpdateManager.State.NeedsInstallPermission -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("설치 권한 필요") },
            text = {
                Text(
                    "이 앱이 APK 를 설치할 수 있도록 시스템 설정에서 " +
                        "\"이 출처에서 허용\" 을 켜주세요."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    manager.openInstallPermissionSettings()
                    onDismiss()
                }) { Text("설정 열기") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        )

        UpdateManager.State.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("업데이트 확인 실패") },
            text = {
                Text(
                    "네트워크 오류이거나 매니페스트를 가져오지 못했습니다. " +
                        "잠시 후 다시 시도해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        )
    }
}
