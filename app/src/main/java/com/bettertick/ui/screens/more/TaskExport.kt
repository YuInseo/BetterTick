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
    return "﻿" + (listOf(header.joinToString(",")) + rows).joinToString("\r\n")
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
