package com.bettertick.recorder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 포그라운드 서비스로 녹음한다.
 * - 화면을 끄거나 앱을 최근 앱에서 지워도 계속 녹음한다(stopWithTask=false).
 * - 설정한 길이마다 파일을 나눠서, 중간에 문제가 생겨도 이미 끝난 조각은 안전하다.
 * - 조각 하나가 끝날 때마다 드라이브 백업을 예약한다.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var levelJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession()
            ACTION_PAUSE -> if (recorder == null) stopSelf() else pause()
            ACTION_RESUME -> if (recorder == null) stopSelf() else resume()
            ACTION_STOP -> stopSession()
            null -> {
                // 시스템이 서비스를 다시 살린 경우: 녹음 중이었으면 이어서 녹음한다.
                if (Settings.get(this).wasRecording) startSession() else stopSelf()
            }
        }
        return START_STICKY
    }

    // ─── 세션 ───

    private fun startSession() {
        if (recorder != null) return
        if (!goForeground()) {
            Settings.get(this).wasRecording = false
            RecorderState.status.value = RecorderState.Status(error = "녹음을 시작할 수 없어요 (마이크 권한 확인)")
            stopSelf()
            return
        }
        acquireWakeLock()
        RecorderState.status.value = RecorderState.Status(recording = true)
        Settings.get(this).wasRecording = true
        if (!startSegment()) {
            stopSession()
        }
    }

    private fun stopSession() {
        finishSegment()
        levelJob?.cancel()
        RecorderState.level.value = 0f
        RecorderState.status.value = RecorderState.Status()
        Settings.get(this).wasRecording = false
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        RecordingStore.get(this).refresh()
    }

    private fun pause() {
        val r = recorder ?: return
        val s = RecorderState.status.value
        if (s.paused) return
        runCatching { r.pause() }.onFailure { Log.w(TAG, "pause", it); return }
        RecorderState.status.value = s.copy(
            paused = true,
            accumulatedMs = s.elapsedMs(),
            runningSince = null,
        )
        RecorderState.level.value = 0f
        updateNotification()
    }

    private fun resume() {
        val r = recorder ?: return
        val s = RecorderState.status.value
        if (!s.paused) return
        runCatching { r.resume() }.onFailure { Log.w(TAG, "resume", it); return }
        RecorderState.status.value = s.copy(paused = false, runningSince = SystemClock.elapsedRealtime())
        updateNotification()
    }

    // ─── 조각(파일) ───

    private fun startSegment(): Boolean {
        val settings = Settings.get(this)
        val store = RecordingStore.get(this)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = store.newFile("rec_$stamp.m4a")

        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
        try {
            r.setAudioSource(bestAudioSource())
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(if (settings.stereo) 2 else 1)
            r.setAudioSamplingRate(settings.sampleRate)
            r.setAudioEncodingBitRate(settings.bitrateKbps * 1000)
            r.setOutputFile(file.absolutePath)
            if (settings.segmentMinutes > 0) {
                r.setMaxDuration(settings.segmentMinutes * 60_000)
            }
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    // 이 콜백 안에서 바로 release하지 않고 다음 차례에 조각을 바꾼다.
                    scope.launch { rotateSegment() }
                }
            }
            r.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "recorder error $what/$extra")
                scope.launch {
                    RecorderState.status.value = RecorderState.status.value.copy(error = "녹음 오류 ($what)")
                    rotateSegment()
                }
            }
            r.prepare()
            r.start()
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            runCatching { r.release() }
            file.delete()
            RecorderState.status.value = RecorderState.status.value.copy(error = "마이크를 쓸 수 없어요: ${e.message}")
            return false
        }

        recorder = r
        currentFile = file
        val s = RecorderState.status.value
        RecorderState.status.value = s.copy(
            recording = true,
            paused = false,
            runningSince = SystemClock.elapsedRealtime(),
            currentFile = file.absolutePath,
            segmentIndex = s.segmentIndex + 1,
            error = null,
        )
        store.refresh()
        startLevelMeter()
        updateNotification()
        return true
    }

    /** 지금 조각을 마무리하고 새 조각을 시작한다. 시간은 이어서 센다. */
    private fun rotateSegment() {
        if (recorder == null) return
        val s = RecorderState.status.value
        val elapsed = s.elapsedMs()
        finishSegment()
        RecorderState.status.value = s.copy(accumulatedMs = elapsed, runningSince = null)
        if (!startSegment()) stopSession()
    }

    private fun finishSegment() {
        val r = recorder ?: return
        val file = currentFile
        recorder = null
        currentFile = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        if (file != null) {
            // 너무 짧아 stop()이 실패하면 깨진 파일이 남으므로 지운다.
            if (!ok || file.length() < 1024) file.delete()
        }
        RecorderState.status.value = RecorderState.status.value.copy(currentFile = null)
        RecordingStore.get(this).refresh()
        if (Settings.get(this).autoBackup) BackupScheduler.backupNow(this)
    }

    /**
     * 가장 원음에 가까운 마이크를 고른다.
     * UNPROCESSED(기기 잡음억제·자동게인 꺼짐)를 지원하면 그걸, 아니면 일반 MIC.
     */
    private fun bestAudioSource(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val am = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val supported = am.getProperty(
                android.media.AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED
            )
            if (supported == "true") return MediaRecorder.AudioSource.UNPROCESSED
        }
        return MediaRecorder.AudioSource.MIC
    }

    private fun startLevelMeter() {
        levelJob?.cancel()
        levelJob = scope.launch {
            while (isActive) {
                val r = recorder
                val st = RecorderState.status.value
                if (r != null && !st.paused) {
                    val amp = runCatching { r.maxAmplitude }.getOrDefault(0)
                    RecorderState.level.value = (amp / 32767f).coerceIn(0f, 1f)
                }
                delay(120)
            }
        }
    }

    // ─── 알림 ───

    private fun goForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        true
    } catch (e: Exception) {
        // 백그라운드에서 마이크 포그라운드 서비스를 시작할 수 없는 경우(안드로이드 14 등)
        Log.e(TAG, "startForeground failed", e)
        false
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) return
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val s = RecorderState.status.value
        val stealth = Settings.get(this).stealth
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, RecorderApp.CHANNEL_RECORDING)
            .setSmallIcon(if (stealth) R.drawable.ic_sync else R.drawable.ic_mic)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (stealth) {
            // 눈에 덜 띄게: 녹음이라는 말·시간 표시·조작 버튼을 숨기고
            // 잠금화면에서는 내용을 가린다. (잠금화면 알림을 '민감한 내용 숨김'으로 설정 시)
            b.setContentTitle("동기화 중")
                .setContentText("백그라운드 작업이 실행 중입니다")
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setUsesChronometer(false)
            return b.build()
        }

        if (s.paused) {
            b.setContentTitle("일시정지됨")
                .setContentText("녹음 " + formatDuration(s.elapsedMs()))
                .setUsesChronometer(false)
                .addAction(0, "이어서 녹음", actionIntent(ACTION_RESUME, 1))
        } else {
            b.setContentTitle("녹음 중")
                .setContentText(if (s.segmentIndex > 1) "${s.segmentIndex}번째 파일" else "앱을 꺼도 계속 녹음해요")
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - s.elapsedMs())
                .setShowWhen(true)
                .addAction(0, "일시정지", actionIntent(ACTION_PAUSE, 1))
        }
        b.addAction(0, "정지", actionIntent(ACTION_STOP, 2))
        return b.build()
    }

    private fun actionIntent(action: String, req: Int): PendingIntent =
        PendingIntent.getService(
            this, req,
            Intent(this, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    // ─── 깨우기 ───

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Recorder:session").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        // 시스템이 서비스를 끝내도 지금까지 녹음한 부분은 저장한다.
        // wasRecording 표시는 남겨 두어 START_STICKY로 다시 살아나면 이어서 녹음한다.
        finishSegment()
        levelJob?.cancel()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"

        fun send(context: Context, action: String) {
            val i = Intent(context, RecordingService::class.java).setAction(action)
            if (action == ACTION_START) ContextCompat.startForegroundService(context, i)
            else context.startService(i)
        }
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
