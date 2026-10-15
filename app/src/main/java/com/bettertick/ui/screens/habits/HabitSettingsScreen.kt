package com.bettertick.ui.screens.habits

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.calendar.OptionCard
import com.bettertick.ui.screens.calendar.OptionNavRow
import com.bettertick.ui.screens.calendar.OptionPage
import com.bettertick.ui.screens.calendar.OptionSwitchRow
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState

/** 습관 > 설정 (TickTick 습관 설정). Every option is persisted in [HabitPrefs]. */
@Composable
fun HabitSettingsScreen(onBack: () -> Unit, onRenameGroup: (String, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    var showColumnManagement by remember { mutableStateOf(false) }
    var ringtoneName by remember { mutableStateOf(habitRingtoneTitle(context, HabitPrefs.ringtone)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        // The picker returns null for 무음 and the default URI for 기본값.
        val value = when {
            uri == null -> ""
            RingtoneManager.isDefault(uri) -> null
            else -> uri.toString()
        }
        HabitPrefs.updateRingtone(value)
        ringtoneName = habitRingtoneTitle(context, value)
    }

    if (showColumnManagement) {
        HabitColumnManagementScreen(onBack = { showColumnManagement = false }, onRenameGroup = onRenameGroup)
        return
    }

    OptionPage(title = "습관 설정", onBack = onBack) {
        OptionCard {
            OptionNavRow("습관 벨소리", value = ringtoneName) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "습관 벨소리")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    val existing = HabitPrefs.ringtone
                    val existingUri = when {
                        existing == null -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        existing.isEmpty() -> null
                        else -> Uri.parse(existing)
                    }
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                }
                runCatching { picker.launch(intent) }
            }
            OptionSwitchRow(
                title = "체크인 상태별 정렬",
                subtitle = "활성화하면 오늘 체크인하지 않은 습관이 목록 맨 위에 표시됩니다.",
                checked = HabitPrefs.sortByCheckIn,
                onCheckedChange = HabitPrefs::updateSortByCheckIn
            )
            OptionNavRow("열 관리", value = "${HabitPrefs.groups.size}개") { showColumnManagement = true }
            OptionSwitchRow(
                title = "\"오늘\" & \"다음 7일\"에 표시",
                subtitle = "해당 날짜에 해야 할 습관을 할일 목록에서 바로 체크인할 수 있습니다.",
                checked = HabitPrefs.showInTodayNext7,
                onCheckedChange = HabitPrefs::updateShowInTodayNext7
            )
        }
    }
}

private fun habitRingtoneTitle(context: android.content.Context, uri: String?): String {
    if (uri == null) return "기본값"
    if (uri.isEmpty()) return "없음"
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
        .getOrNull() ?: "사용자 지정"
}

/** 습관 설정 > 열 관리: add, rename, reorder and delete habit sections. */
@Composable
fun HabitColumnManagementScreen(onBack: () -> Unit, onRenameGroup: (String, String) -> Unit = { _, _ -> }) {
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }

    OptionPage(title = "열 관리", onBack = onBack) {
        OptionCard {
            val groups = HabitPrefs.groups
            groups.forEachIndexed { index, column ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { renameTarget = column }
                        .padding(start = 20.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(column, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { HabitPrefs.moveGroup(column, -1) }, enabled = index > 0) {
                        Icon(
                            Icons.Outlined.KeyboardArrowUp, "위로",
                            tint = if (index > 0) TextSecondary else TextTertiary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { HabitPrefs.moveGroup(column, 1) }, enabled = index < groups.lastIndex) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown, "아래로",
                            tint = if (index < groups.lastIndex) TextSecondary else TextTertiary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { HabitPrefs.removeGroup(column) }, enabled = groups.size > 1) {
                        Icon(
                            Icons.Outlined.Delete, "삭제",
                            tint = if (groups.size > 1) TextSecondary else TextTertiary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = ThemeState.accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("새로운 열", fontSize = 16.sp, color = ThemeState.accentColor)
            }
        }
        Text(
            "습관은 소속 그룹(열)별로 묶여 표시됩니다. 삭제한 열의 습관은 첫 번째 열에 표시됩니다.",
            fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }

    if (showAddDialog) {
        GroupNameDialog(
            title = "새로운 열 추가",
            initial = "",
            confirm = "추가",
            onDismiss = { showAddDialog = false },
            onConfirm = { if (HabitPrefs.addGroup(it)) showAddDialog = false }
        )
    }
    renameTarget?.let { old ->
        GroupNameDialog(
            title = "열 이름 변경",
            initial = old,
            confirm = "저장",
            onDismiss = { renameTarget = null },
            onConfirm = {
                val new = it.trim()
                if (HabitPrefs.renameGroup(old, new)) {
                    if (new != old) onRenameGroup(old, new)
                    renameTarget = null
                }
            }
        )
    }
}

@Composable
internal fun GroupNameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val exists = name.trim() != initial && name.trim() in HabitPrefs.groups
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                placeholder = { Text("열 이름") },
                singleLine = true,
                isError = exists,
                supportingText = if (exists) { { Text("이미 있는 이름입니다") } } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeState.accentColor,
                    cursorColor = ThemeState.accentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && !exists
            ) { Text(confirm, color = ThemeState.accentColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
        },
        containerColor = GroupedCard
    )
}
