package com.bettertick.ui.screens.more

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 설정 > 백업 & 내보내기: every task as a CSV (Excel-friendly, UTF-8 BOM). */
fun buildTasksCsv(tasks: List<Task>, lists: List<TaskList>, tags: List<Tag>): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()
    fun cell(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    fun time(t: com.google.firebase.Timestamp?): String =
        t?.toDate()?.toInstant()?.atZone(zone)?.format(fmt) ?: ""
    val priority = mapOf(0 to "없음", 1 to "낮음", 2 to "보통", 3 to "높음")
    val header = listOf("제목", "메모", "목록", "태그", "우선순위", "날짜", "반복", "상태", "완료 시간", "생성 시간")
    val rows = tasks.sortedBy { it.createdAt.seconds }.map { t ->
        listOf(
            t.title,
            t.notes,
            lists.firstOrNull { it.id == t.listId }?.name ?: "기본함",
            t.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id }?.name }.joinToString(" "),
            priority[t.priority] ?: t.priority.toString(),
            time(t.dueDate),
            t.repeatRule.orEmpty(),
            when {
                t.isCompleted -> "완료"
                t.isAbandoned -> "하지 않음"
                else -> "진행 중"
            },
            time(t.completedAt),
            time(t.createdAt)
        ).joinToString(",") { cell(it) }
    }
    return Char(0xFEFF).toString() + (listOf(header.joinToString(",")) + rows).joinToString("\r\n")
}

fun shareTasksCsv(context: Context, csv: String) {
    runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "BetterTick_${java.time.LocalDate.now()}.csv")
        file.writeText(csv, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "할일 내보내기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Splits CSV text into rows of cells, honoring quotes, "" escapes and line breaks inside quotes. */
internal fun parseCsvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    val src = text.removePrefix(Char(0xFEFF).toString())
    while (i < src.length) {
        val c = src[i]
        when {
            quoted && c == '"' && i + 1 < src.length && src[i + 1] == '"' -> { cell.append('"'); i++ }
            c == '"' -> quoted = !quoted
            !quoted && c == ',' -> { row.add(cell.toString()); cell.setLength(0) }
            !quoted && (c == '\n' || c == '\r') -> {
                if (c == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                row.add(cell.toString()); cell.setLength(0)
                rows.add(row); row = mutableListOf()
            }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
    return rows.filter { r -> r.any { it.isNotBlank() } }
}

/**
 * 할일 가져오기: reads a CSV made by [buildTasksCsv] (columns found by their
 * header names, so reordered or missing columns are fine). Unknown lists go to
 * 기본함, unknown tags are dropped, and rows matching an [existing] task with
 * the same title and date are skipped so importing twice doesn't duplicate.
 */
fun parseTasksCsv(text: String, lists: List<TaskList>, tags: List<Tag>, existing: List<Task>): List<Task> {
    val rows = parseCsvRows(text)
    if (rows.size < 2) return emptyList()
    val header = rows.first().map { it.trim() }
    val titleCol = header.indexOf("제목")
    if (titleCol < 0) return emptyList()
    fun List<String>.col(name: String): String = header.indexOf(name).let { getOrNull(it) }?.trim().orEmpty()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()
    fun stamp(v: String): com.google.firebase.Timestamp? = runCatching {
        com.google.firebase.Timestamp(
            java.util.Date.from(java.time.LocalDateTime.parse(v, fmt).atZone(zone).toInstant())
        )
    }.getOrNull()
    val priority = mapOf("없음" to 0, "낮음" to 1, "보통" to 2, "높음" to 3)
    val seen = existing.map { it.title.trim() to it.dueDate?.seconds }.toMutableSet()
    val now = System.currentTimeMillis()
    return rows.drop(1).mapIndexedNotNull { index, r ->
        val title = r.getOrNull(titleCol)?.trim().orEmpty()
        if (title.isEmpty()) return@mapIndexedNotNull null
        val due = stamp(r.col("날짜"))
        if (!seen.add(title to due?.seconds)) return@mapIndexedNotNull null
        val status = r.col("상태")
        val completedAt = stamp(r.col("완료 시간"))
        Task(
            title = title,
            notes = r.col("메모"),
            listId = lists.firstOrNull { it.name == r.col("목록") }?.id.orEmpty(),
            tagIds = r.col("태그").split(' ').mapNotNull { n -> tags.firstOrNull { it.name == n }?.id },
            dueDate = due,
            priority = priority[r.col("우선순위")] ?: 0,
            repeatRule = r.col("반복").ifBlank { null },
            isCompleted = status == "완료",
            completedAt = if (status == "완료") completedAt else null,
            isAbandoned = status == "하지 않음",
            createdAt = stamp(r.col("생성 시간")) ?: com.google.firebase.Timestamp.now(),
            sortOrder = now + index
        )
    }
}
