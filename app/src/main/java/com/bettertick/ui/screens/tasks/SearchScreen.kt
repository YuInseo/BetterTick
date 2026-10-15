package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.ui.screens.tasks.components.TaskDetailSheet
import com.bettertick.ui.screens.tasks.components.TaskItem
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState

private const val SEARCH_PREFS = "search_prefs"

/** Drawer 🔍 — searches task titles, notes and tag names. */
@Composable
fun SearchScreen(
    viewModel: TasksViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE) }
    var recent by remember {
        mutableStateOf(prefs.getString("recent", "").orEmpty().split("\n").filter { it.isNotBlank() })
    }
    fun saveRecent(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        recent = (listOf(t) + recent.filter { it != t }).take(8)
        prefs.edit().putString("recent", recent.joinToString("\n")).apply()
    }

    val allTasks by viewModel.allTasks.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var query by remember { mutableStateOf("") }
    var detailFor by remember { mutableStateOf<Task?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val results = remember(query, allTasks, tags) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else allTasks.filter { t ->
            t.title.contains(q, ignoreCase = true) ||
                t.notes.contains(q, ignoreCase = true) ||
                t.tagIds.any { id -> tags.firstOrNull { it.id == id }?.name?.contains(q.removePrefix("#"), ignoreCase = true) == true }
        }.sortedWith(compareBy<Task> { it.isCompleted || it.isAbandoned }.thenByDescending { it.updatedAt.seconds })
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextPrimary)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("할일, 메모, 태그 검색", color = TextTertiary, fontSize = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(ThemeState.accentColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { saveRecent(query) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, contentDescription = "지우기", tint = TextTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { query = "" }
                    )
                }
            }
        }

        if (query.isBlank()) {
            if (recent.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("최근 검색", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        recent = emptyList()
                        prefs.edit().remove("recent").apply()
                    }) { Text("지우기", color = TextSecondary, fontSize = 13.sp) }
                }
                recent.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = r }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(r, fontSize = 15.sp, color = TextPrimary)
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Text("검색 결과가 없습니다", color = TextTertiary, fontSize = 15.sp)
            }
        } else {
            Text(
                "${results.size}개의 결과",
                fontSize = 13.sp, color = TextTertiary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleComplete(task.id, !task.isCompleted) },
                        onClick = { saveRecent(query); detailFor = task },
                        listName = lists.firstOrNull { it.id == task.listId }?.name ?: "기본함",
                        resolvedTags = task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    detailFor?.let { task ->
        TaskDetailSheet(
            task = task,
            listName = lists.firstOrNull { it.id == task.listId }?.name ?: "기본함",
            onDismiss = { detailFor = null },
            onUpdateTask = { viewModel.updateTask(it); detailFor = null },
            onToggleComplete = { viewModel.toggleComplete(task.id, !task.isCompleted); detailFor = null },
            tags = tags,
            onCreateTag = { viewModel.createTag(it) },
            lists = lists,
            onDuplicate = { viewModel.duplicateTask(it) },
            onDelete = { viewModel.deleteTask(it.id) }
        )
    }
}
