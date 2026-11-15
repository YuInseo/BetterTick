package com.bettertick.ui.screens.diary

import com.bettertick.data.model.DiaryEntry
import java.time.LocalDate

/** Resolve identity before launching a write: changing the selected day must not retarget it. */
internal fun diarySaveSnapshot(date: LocalDate, content: String, mood: Int, existing: DiaryEntry?): DiaryEntry {
    val sameDay = existing?.takeIf { it.dateStr == date.toString() }
    return sameDay?.copy(content = content, mood = mood.coerceIn(0, 4))
        ?: DiaryEntry(dateStr = date.toString(), content = content, mood = mood.coerceIn(0, 4))
}
