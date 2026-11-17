package com.bettertick.recorder

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class Recording(
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long,
    val uploaded: Boolean,
    val inProgress: Boolean,
)

/**
 * 녹음 파일 목록과 드라이브 업로드 기록.
 * 파일은 앱 전용 폴더(files/recordings)에 저장한다.
 */
class RecordingStore private constructor(private val context: Context) {
    private val uploads: SharedPreferences =
        context.applicationContext.getSharedPreferences("uploads", Context.MODE_PRIVATE)
    private val durations: SharedPreferences =
        context.applicationContext.getSharedPreferences("durations", Context.MODE_PRIVATE)

    val dir: File = File(context.filesDir, "recordings").apply { mkdirs() }

    private val _items = MutableStateFlow<List<Recording>>(emptyList())
    val items: StateFlow<List<Recording>> = _items.asStateFlow()

    init { refresh() }

    fun refresh() {
        val current = RecorderState.status.value.currentFile
        _items.value = (dir.listFiles { f -> f.isFile && f.name.endsWith(".m4a") } ?: emptyArray())
            .sortedByDescending { it.name }
            .map {
                Recording(
                    file = it,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                    uploaded = uploads.contains(it.name),
                    inProgress = it.absolutePath == current,
                )
            }
    }

    fun newFile(name: String): File = File(dir, name)

    fun isUploaded(name: String) = uploads.contains(name)

    fun markUploaded(name: String, driveId: String) {
        uploads.edit().putString(name, driveId).apply()
        refresh()
    }

    /** 업로드 기록을 지운다(다른 계정으로 바꿨을 때 다시 올리기 위해). */
    fun clearUploadMarks() {
        uploads.edit().clear().apply()
        refresh()
    }

    fun delete(file: File) {
        file.delete()
        durations.edit().remove(file.name).apply()
        refresh()
    }

    /** 길이(ms). 한 번 구하면 저장해 둔다. 진행 중인 파일은 구하지 않는다. */
    fun durationMs(rec: Recording): Long? {
        if (rec.inProgress) return null
        val key = rec.file.name
        val cached = durations.getLong(key, -1L)
        if (cached >= 0) return cached
        val ms = runCatching {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(rec.file.absolutePath)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                r.release()
            }
        }.getOrNull() ?: return null
        durations.edit().putLong(key, ms).apply()
        return ms
    }

    companion object {
        @Volatile private var instance: RecordingStore? = null
        fun get(context: Context): RecordingStore =
            instance ?: synchronized(this) {
                instance ?: RecordingStore(context.applicationContext).also { instance = it }
            }
    }
}
