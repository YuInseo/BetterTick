package com.bettertick.data.export

import android.content.Context
import android.os.Environment
import com.bettertick.data.model.DiaryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryTxtExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun export(entry: DiaryEntry): File? = runCatching {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        dir.mkdirs()
        val moodEmoji = when (entry.mood) {
            1 -> "😔"
            2 -> "😐"
            3 -> "🙂"
            4 -> "😄"
            else -> null
        }
        val header = buildString {
            append("날짜: ${entry.dateStr}\n")
            if (moodEmoji != null) append("기분: $moodEmoji\n")
            append("\n")
        }
        val file = File(dir, "diary_${entry.dateStr}.txt")
        file.writeText(header + entry.content)
        file
    }.getOrNull()
}
