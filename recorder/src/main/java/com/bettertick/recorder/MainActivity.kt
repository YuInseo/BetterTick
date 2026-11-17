package com.bettertick.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var pendingStart = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true || hasMic()) {
            if (pendingStart) RecordingService.send(this, RecordingService.ACTION_START)
        } else {
            Toast.makeText(this, "녹음하려면 마이크 권한이 필요해요", Toast.LENGTH_LONG).show()
        }
        pendingStart = false
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        lifecycleScopeLaunch {
            try {
                val data = res.data ?: error("연결을 취소했어요")
                val result = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
                val token = result.accessToken ?: error("권한을 받지 못했어요")
                finishConnect(token)
            } catch (e: Exception) {
                toast("드라이브 연결 실패: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecorderTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RecorderScreen(
                        onRecord = ::startRecording,
                        onConnectDrive = ::connectDrive,
                        onBatteryExempt = ::requestBatteryExemption,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RecordingStore.get(this).refresh()
    }

    override fun onStart() {
        super.onStart()
        // 앱을 열 때 조용히 업데이트 확인(하루 한 번). 녹음 중이면 나갈 때 설치.
        val now = System.currentTimeMillis()
        val last = getPreferences(MODE_PRIVATE).getLong("last_update_check", 0)
        if (now - last > 12 * 60 * 60 * 1000) {
            getPreferences(MODE_PRIVATE).edit().putLong("last_update_check", now).apply()
            lifecycleScopeLaunch { runCatching { Updater.run(this, Updater.Trigger.APP_OPEN) } }
        }
    }

    private fun hasMic() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startRecording() {
        val needed = buildList {
            if (!hasMic()) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isEmpty()) {
            RecordingService.send(this, RecordingService.ACTION_START)
        } else {
            pendingStart = true
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun connectDrive() {
        lifecycleScopeLaunch {
            try {
                val result = DriveClient.authorize(this, null)
                if (result.hasResolution()) {
                    val pi = result.pendingIntent ?: error("동의 화면을 열 수 없어요")
                    authLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    finishConnect(result.accessToken ?: error("권한을 받지 못했어요"))
                }
            } catch (e: Exception) {
                toast("드라이브 연결 실패: ${e.message}")
            }
        }
    }

    private suspend fun finishConnect(token: String) {
        val email = try {
            DriveClient.accountEmail(token)
        } catch (e: Exception) {
            toast("계정 확인 실패: ${e.message}")
            return
        } ?: run {
            toast("계정 주소를 알 수 없어요")
            return
        }
        val settings = Settings.get(this)
        if (settings.driveAccount != null && settings.driveAccount != email) {
            // 다른 계정으로 바꾸면 새 계정에 다시 올린다.
            RecordingStore.get(this).clearUploadMarks()
        }
        settings.setDriveAccount(email)
        settings.setLastBackupMessage("$email 에 연결됨")
        if (settings.autoBackup) BackupScheduler.reschedule(this)
        toast("구글 드라이브에 연결했어요")
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast("이미 배터리 최적화에서 빠져 있어요")
            return
        }
        runCatching {
            startActivity(
                Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }.onFailure {
            startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        lifecycleScope.launch { block() }
    }
}

// ─── 테마 ───

@Composable
private fun RecorderTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private val RecordRed = Color(0xFFE5484D)

// ─── 화면 ───

private enum class Tab(val label: String) { RECORD("녹음"), LIST("목록"), SETTINGS("설정") }

@Composable
private fun RecorderScreen(
    onRecord: () -> Unit,
    onConnectDrive: () -> Unit,
    onBatteryExempt: () -> Unit,
) {
    val ctx = LocalContext.current
    val status by RecorderState.status.collectAsState()
    val settings by Settings.get(ctx).state.collectAsState()
    val items by RecordingStore.get(ctx).items.collectAsState()
    val player = remember { PlayerHolder() }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    var tab by rememberSaveable { mutableStateOf(Tab.RECORD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val badge = if (t == Tab.LIST && items.isNotEmpty()) items.size else null
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            val icon = when (t) {
                                Tab.RECORD -> Icons.Filled.FiberManualRecord
                                Tab.LIST -> Icons.AutoMirrored.Filled.List
                                Tab.SETTINGS -> Icons.Filled.Settings
                            }
                            if (badge != null) {
                                BadgedBox(badge = { Badge { Text("$badge") } }) { Icon(icon, null) }
                            } else {
                                Icon(icon, null)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        when (tab) {
            Tab.RECORD -> Column(
                Modifier.fillMaxSize().padding(inner).padding(16.dp),
            ) {
                RecorderPanel(status, onRecord)
                if (settings.driveAccount == null) {
                    Spacer(Modifier.height(12.dp))
                    DriveHint(onConnectDrive)
                }
            }

            Tab.LIST -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "녹음 ${items.size}개",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
                    )
                }
                if (items.isEmpty()) {
                    item {
                        Text(
                            "아직 녹음이 없어요. '녹음' 탭에서 시작하세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
                items(items, key = { it.file.name }) { rec ->
                    RecordingRow(rec, player)
                }
            }

            Tab.SETTINGS -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { BackupCard(settings, onConnectDrive) }
                item { OptionsCard(settings, status.recording, onBatteryExempt) }
                item { StealthCard(settings) }
                item { UpdateCard() }
            }
        }
    }
}

@Composable
private fun DriveHint(onConnectDrive: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("구글 드라이브에 연결하면 녹음이 자동으로 백업돼요", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onConnectDrive) { Text("연결") }
        }
    }
}

@Composable
private fun RecorderPanel(status: RecorderState.Status, onRecord: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(status.recording, status.paused) {
        while (status.recording && !status.paused) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
        now = SystemClock.elapsedRealtime()
    }
    val level by RecorderState.level.collectAsState()
    val shownLevel by animateFloatAsState(if (status.recording && !status.paused) level else 0f, label = "level")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when {
                    !status.recording -> "대기 중"
                    status.paused -> "일시정지됨"
                    else -> "녹음 중 · 앱을 꺼도 계속돼요"
                },
                color = if (status.recording && !status.paused) RecordRed else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatDuration(if (status.recording) status.elapsedMs(now) else 0L),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { shownLevel },
                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp),
                color = RecordRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (status.recording) {
                    FilledTonalIconButton(
                        onClick = {
                            RecordingService.send(
                                ctx,
                                if (status.paused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE,
                            )
                        },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            if (status.paused) Icons.Filled.FiberManualRecord else Icons.Filled.Pause,
                            contentDescription = if (status.paused) "이어서 녹음" else "일시정지",
                        )
                    }
                    Box(
                        Modifier.size(76.dp).background(RecordRed, CircleShape)
                            .clickable { RecordingService.send(ctx, RecordingService.ACTION_STOP) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Stop, "정지", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                } else {
                    Box(
                        Modifier.size(76.dp).background(RecordRed, CircleShape).clickable(onClick = onRecord),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Mic, "녹음 시작", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }
            status.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BackupCard(settings: Settings.Values, onConnectDrive: () -> Unit) {
    val ctx = LocalContext.current
    val s = Settings.get(ctx)
    var confirmDisconnect by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("구글 드라이브 백업", style = MaterialTheme.typography.titleMedium)
                    Text(
                        settings.driveAccount ?: "연결 안 됨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (settings.driveAccount == null) {
                    FilledTonalButton(onClick = onConnectDrive) { Text("연결") }
                } else {
                    TextButton(onClick = { confirmDisconnect = true }) { Text("해제") }
                }
            }
            if (settings.driveAccount != null) {
                Text(
                    "드라이브의 '${DriveClient.FOLDER_NAME}' 폴더에 올라가요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow("파일이 끝나면 자동 백업", settings.autoBackup) {
                s.setAutoBackup(it); BackupScheduler.reschedule(ctx)
            }
            SwitchRow("와이파이에서만", settings.wifiOnly, enabled = settings.autoBackup) {
                s.setWifiOnly(it); BackupScheduler.reschedule(ctx)
            }
            SwitchRow("올린 뒤 휴대폰에서 지우기", settings.deleteAfterUpload) { s.setDeleteAfterUpload(it) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    settings.lastBackupMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = {
                        BackupScheduler.backupNow(ctx)
                        Toast.makeText(ctx, "백업을 시작해요", Toast.LENGTH_SHORT).show()
                    },
                    enabled = settings.driveAccount != null,
                ) { Text("지금 백업") }
            }
        }
    }
    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("드라이브 연결 해제") },
            text = { Text("이미 올린 파일은 드라이브에 그대로 남아요. 새 녹음은 백업되지 않아요.") },
            confirmButton = {
                TextButton(onClick = {
                    s.setDriveAccount(null)
                    s.setLastBackupMessage(null)
                    confirmDisconnect = false
                }) { Text("해제") }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun OptionsCard(settings: Settings.Values, recording: Boolean, onBatteryExempt: () -> Unit) {
    val s = Settings.get(LocalContext.current)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("녹음 설정", style = MaterialTheme.typography.titleMedium)
            if (recording) {
                Text(
                    "바뀐 설정은 다음 파일부터 적용돼요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("파일 나누기", style = MaterialTheme.typography.labelLarge)
            ChipRow(listOf(0 to "안 나눔", 15 to "15분", 30 to "30분", 60 to "1시간"), settings.segmentMinutes) {
                s.setSegmentMinutes(it)
            }
            Spacer(Modifier.height(4.dp))
            Text("음질 (비트레이트)", style = MaterialTheme.typography.labelLarge)
            ChipRow(
                listOf(96 to "보통", 192 to "좋음", 256 to "고음질", 320 to "최고"),
                settings.bitrateKbps,
            ) { s.setBitrateKbps(it) }
            Spacer(Modifier.height(4.dp))
            Text("샘플레이트", style = MaterialTheme.typography.labelLarge)
            ChipRow(
                listOf(44_100 to "44.1kHz", 48_000 to "48kHz"),
                settings.sampleRate,
            ) { s.setSampleRate(it) }
            SwitchRow("스테레오로 녹음 (끄면 모노)", settings.stereo) { s.setStereo(it) }
            Text(
                "1시간에 약 ${settings.bitrateKbps * 3600 / 8 / 1024}MB" +
                    (if (settings.stereo) " · 스테레오" else " · 모노"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "배터리 최적화에서 빼면 오래 녹음해도 끊기지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBatteryExempt) { Text("설정") }
            }
        }
    }
}

@Composable
private fun StealthCard(settings: Settings.Values) {
    val s = Settings.get(LocalContext.current)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("스텔스 모드", style = MaterialTheme.typography.titleMedium)
            }
            SwitchRow("알림을 눈에 안 띄게", settings.stealth) { s.setStealth(it) }
            Text(
                "켜면 녹음 알림이 '동기화 중'이라는 평범한 알림으로 바뀌고, 시간·정지 버튼이 숨겨지며 잠금화면에서 내용이 안 보여요. 녹음을 멈추려면 앱을 직접 열어야 해요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Icon(
                    Icons.Filled.Info, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "상단바의 초록색 마이크 점은 안드로이드 12부터 시스템이 강제로 띄우는 표시라, 어떤 앱도 숨길 수 없어요. 이건 가릴 수 없다는 점을 알아 두세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UpdateCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val version = remember { runCatching { Updater.currentVersionName(ctx) }.getOrDefault("?") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("앱 업데이트", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "현재 v$version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (checking) return@FilledTonalButton
                        checking = true
                        message = "확인 중…"
                        scope.launch {
                            val r = Updater.run(ctx, Updater.Trigger.USER)
                            message = when (r) {
                                is Updater.Outcome.UpToDate -> "최신 버전이에요"
                                is Updater.Outcome.Installing -> "v${r.versionName} 설치 중…"
                                is Updater.Outcome.Ready -> "v${r.versionName} 준비됨 — 앱을 나가면 설치돼요"
                                is Updater.Outcome.Publishing -> Updater.PUBLISHING_MESSAGE
                                is Updater.Outcome.Busy -> "이미 확인 중이에요"
                                is Updater.Outcome.Failed -> r.message
                            }
                            checking = false
                        }
                    },
                    enabled = !checking,
                ) { Text("업데이트 확인") }
            }
            Text(
                "앱을 열 때 자동으로 새 버전을 확인하고 받아서 설치해요. 녹음 중이면 녹음이 끝난 뒤 설치돼요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            message?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ChipRow(options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) })
        }
    }
}

// ─── 목록 ───

private class PlayerHolder {
    var playing by mutableStateOf<String?>(null)
    private var mp: MediaPlayer? = null

    fun toggle(file: File) {
        if (playing == file.name) { stop(); return }
        stop()
        val p = MediaPlayer()
        try {
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener { stop() }
            p.prepare()
            p.start()
            mp = p
            playing = file.name
        } catch (e: Exception) {
            p.release()
        }
    }

    fun stop() {
        mp?.let { runCatching { it.stop() }; it.release() }
        mp = null
        playing = null
    }
}

private val nameFormat = SimpleDateFormat("'rec_'yyyyMMdd_HHmmss'.m4a'", Locale.US)
private val shownFormat = SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA)

private fun displayName(file: File): String =
    runCatching { nameFormat.parse(file.name)?.let { shownFormat.format(it) } }.getOrNull() ?: file.name

@Composable
private fun RecordingRow(rec: Recording, player: PlayerHolder) {
    val ctx = LocalContext.current
    val store = RecordingStore.get(ctx)
    val duration by produceState<Long?>(null, rec.file.name, rec.inProgress, rec.sizeBytes) {
        value = withContext(Dispatchers.IO) { store.durationMs(rec) }
    }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isPlaying = player.playing == rec.file.name

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.toggle(rec.file) }, enabled = !rec.inProgress) {
                Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (isPlaying) "멈춤" else "재생")
            }
            Column(Modifier.weight(1f)) {
                Text(displayName(rec.file), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    buildString {
                        if (rec.inProgress) append("녹음 중 · ")
                        duration?.let { append(formatDuration(it)).append(" · ") }
                        append(formatSize(rec.sizeBytes))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                when {
                    rec.uploaded -> Icons.Filled.CloudDone
                    else -> Icons.Filled.CloudUpload
                },
                contentDescription = if (rec.uploaded) "백업됨" else "백업 전",
                tint = if (rec.uploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
            Box {
                IconButton(onClick = { menu = true }, enabled = !rec.inProgress) {
                    Icon(Icons.Filled.MoreVert, "메뉴")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("공유") },
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = {
                            menu = false
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", rec.file)
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("audio/mp4")
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(send, "녹음 공유"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("녹음 삭제") },
            text = {
                Text(
                    if (rec.uploaded) "휴대폰에서만 지워요. 드라이브에 올라간 파일은 남아요."
                    else "아직 백업되지 않은 녹음이에요. 지우면 되돌릴 수 없어요."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (isPlaying) player.stop()
                    scope.launch(Dispatchers.IO) { store.delete(rec.file) }
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> String.format(Locale.US, "%.1fMB", bytes / 1024.0 / 1024.0)
    else -> "${bytes / 1024}KB"
}
