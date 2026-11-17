package com.bettertick.recorder

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 앱 설정. 바뀌면 [state]로 화면에 알린다. */
class Settings private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("recorder", Context.MODE_PRIVATE)

    data class Values(
        val autoBackup: Boolean,
        val wifiOnly: Boolean,
        val deleteAfterUpload: Boolean,
        val segmentMinutes: Int,
        val bitrateKbps: Int,
        val stereo: Boolean,
        val sampleRate: Int,
        val stealth: Boolean,
        val driveAccount: String?,
        val lastBackupMessage: String?,
    )

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Values> = _state.asStateFlow()

    private fun read() = Values(
        autoBackup = prefs.getBoolean(K_AUTO, true),
        wifiOnly = prefs.getBoolean(K_WIFI, false),
        deleteAfterUpload = prefs.getBoolean(K_DELETE, false),
        segmentMinutes = prefs.getInt(K_SEGMENT, 30),
        bitrateKbps = prefs.getInt(K_BITRATE, 192),
        stereo = prefs.getBoolean(K_STEREO, true),
        sampleRate = prefs.getInt(K_SAMPLE_RATE, 48_000),
        stealth = prefs.getBoolean(K_STEALTH, false),
        driveAccount = prefs.getString(K_ACCOUNT, null),
        lastBackupMessage = prefs.getString(K_LAST_MSG, null),
    )

    val autoBackup get() = _state.value.autoBackup
    val wifiOnly get() = _state.value.wifiOnly
    val deleteAfterUpload get() = _state.value.deleteAfterUpload
    val segmentMinutes get() = _state.value.segmentMinutes
    val bitrateKbps get() = _state.value.bitrateKbps
    val stereo get() = _state.value.stereo
    val sampleRate get() = _state.value.sampleRate
    val stealth get() = _state.value.stealth
    val driveAccount get() = _state.value.driveAccount

    var driveFolderId: String?
        get() = prefs.getString(K_FOLDER, null)
        set(v) { prefs.edit().putString(K_FOLDER, v).apply() }

    /** 녹음 중에 프로세스가 죽었는지 알기 위한 표시. */
    var wasRecording: Boolean
        get() = prefs.getBoolean(K_WAS_RECORDING, false)
        set(v) { prefs.edit().putBoolean(K_WAS_RECORDING, v).commit() }

    fun setAutoBackup(v: Boolean) = put { putBoolean(K_AUTO, v) }
    fun setWifiOnly(v: Boolean) = put { putBoolean(K_WIFI, v) }
    fun setDeleteAfterUpload(v: Boolean) = put { putBoolean(K_DELETE, v) }
    fun setSegmentMinutes(v: Int) = put { putInt(K_SEGMENT, v) }
    fun setBitrateKbps(v: Int) = put { putInt(K_BITRATE, v) }
    fun setStereo(v: Boolean) = put { putBoolean(K_STEREO, v) }
    fun setSampleRate(v: Int) = put { putInt(K_SAMPLE_RATE, v) }
    fun setStealth(v: Boolean) = put { putBoolean(K_STEALTH, v) }
    fun setLastBackupMessage(v: String?) = put { putString(K_LAST_MSG, v) }
    fun setDriveAccount(v: String?) = put {
        putString(K_ACCOUNT, v)
        remove(K_FOLDER)
    }

    private inline fun put(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = read()
    }

    companion object {
        private const val K_AUTO = "auto_backup"
        private const val K_WIFI = "wifi_only"
        private const val K_DELETE = "delete_after_upload"
        private const val K_SEGMENT = "segment_minutes"
        private const val K_BITRATE = "bitrate_kbps"
        private const val K_STEREO = "stereo"
        private const val K_SAMPLE_RATE = "sample_rate"
        private const val K_STEALTH = "stealth"
        private const val K_ACCOUNT = "drive_account"
        private const val K_FOLDER = "drive_folder_id"
        private const val K_LAST_MSG = "last_backup_message"
        private const val K_WAS_RECORDING = "was_recording"

        @Volatile private var instance: Settings? = null
        fun get(context: Context): Settings =
            instance ?: synchronized(this) { instance ?: Settings(context).also { instance = it } }
    }
}
