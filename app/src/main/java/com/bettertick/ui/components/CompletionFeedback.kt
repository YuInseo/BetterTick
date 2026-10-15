package com.bettertick.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.bettertick.ui.theme.AppPrefs

/** 설정 > 사운드 & 알림 > 완료 효과음 / 완료 시 진동. */
object CompletionFeedback {
    fun play(context: Context, haptic: HapticFeedback? = null) {
        if (AppPrefs.completionVibrate) haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        if (AppPrefs.completionSound) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 300)
            }
        }
    }
}
