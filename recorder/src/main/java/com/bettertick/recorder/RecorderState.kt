package com.bettertick.recorder

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

/** 녹음 서비스와 화면이 함께 보는 상태. */
object RecorderState {
    data class Status(
        val recording: Boolean = false,
        val paused: Boolean = false,
        /** 일시정지 전까지 누적된 녹음 시간 */
        val accumulatedMs: Long = 0,
        /** 지금 녹음이 돌기 시작한 시각(elapsedRealtime). 일시정지면 null */
        val runningSince: Long? = null,
        val currentFile: String? = null,
        val segmentIndex: Int = 0,
        val error: String? = null,
    ) {
        fun elapsedMs(now: Long = SystemClock.elapsedRealtime()): Long =
            accumulatedMs + (runningSince?.let { now - it } ?: 0L)
    }

    val status = MutableStateFlow(Status())

    /** 0..1 입력 음량 */
    val level = MutableStateFlow(0f)
}
