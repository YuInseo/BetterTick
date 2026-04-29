package com.bettertick.ui.screens.habits

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

private val HabitBlue = Color(0xFF4A90E2)

private fun hasAudioPermission(context: Context): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun HabitSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var showColumnManagement by remember { mutableStateOf(false) }
    var sortByCheckIn by remember { mutableStateOf(false) }
    var showInTodayNext7 by remember { mutableStateOf(true) }
    var selectedRingtoneName by remember { mutableStateOf("TickTick Pop") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showRingtonePicker by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showRingtonePicker = true
    }

    if (showColumnManagement) {
        HabitColumnManagementScreen(onBack = { showColumnManagement = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "습관 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(12.dp))

        // Settings card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
        ) {
            // 습관 벨소리
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (hasAudioPermission(context)) {
                            showRingtonePicker = true
                        } else {
                            showPermissionDialog = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("습관 벨소리", color = MaterialTheme.colorScheme.onBackground)
                    Text(selectedRingtoneName, fontSize = 13.sp, color = TextSecondary)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
            }

            HorizontalDivider(color = DarkBackground, thickness = 1.dp)

            // 체크인 상태별 정렬
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("체크인 상태별 정렬", color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "활성화된 경우 체크 아웃되지 않은 습관이 목록 맨 위에\n표시된다.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = sortByCheckIn,
                    onCheckedChange = { sortByCheckIn = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HabitBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF555555)
                    )
                )
            }

            HorizontalDivider(color = DarkBackground, thickness = 1.dp)

            // 열 관리
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showColumnManagement = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("열 관리", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
            }

            HorizontalDivider(color = DarkBackground, thickness = 1.dp)

            // "오늘" & "다음 7일"에 표시
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\"오늘\" & \"다음 7일\"에 표시",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = showInTodayNext7,
                    onCheckedChange = { showInTodayNext7 = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HabitBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF555555)
                    )
                )
            }
        }
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            text = {
                Text(
                    "특정 알림음을 제공하기 위해서는 외부 저장소 쓰기 권한이 필요합니다. 허용을 눌러주세요.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionLauncher.launch(audioPermission)
                }) {
                    Text("다음", color = HabitBlue)
                }
            },
            containerColor = DarkCard
        )
    }

    // Ringtone picker dialog
    if (showRingtonePicker) {
        RingtonePickerDialog(
            context = context,
            selectedName = selectedRingtoneName,
            onSelect = { name ->
                selectedRingtoneName = name
                showRingtonePicker = false
            },
            onDismiss = { showRingtonePicker = false }
        )
    }
}

@Composable
private fun RingtonePickerDialog(
    context: Context,
    selectedName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Load system ringtones
    val ringtones = remember {
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        val list = mutableListOf("없음" to null as Uri?, "기본값" to null as Uri?)
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)
            list.add(title to uri)
        }
        list
    }

    var tempSelected by remember { mutableStateOf(selectedName) }
    var playingRingtone by remember { mutableStateOf<Ringtone?>(null) }

    DisposableEffect(Unit) {
        onDispose { playingRingtone?.stop() }
    }

    AlertDialog(
        onDismissRequest = {
            playingRingtone?.stop()
            onDismiss()
        },
        title = { Text("알림음 선택", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("짧은 알림음", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                ringtones.forEach { (name, uri) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelected = name
                                playingRingtone?.stop()
                                if (uri != null) {
                                    playingRingtone = RingtoneManager.getRingtone(context, uri)?.also {
                                        it.play()
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = name == tempSelected,
                            onClick = {
                                tempSelected = name
                                playingRingtone?.stop()
                                if (uri != null) {
                                    playingRingtone = RingtoneManager.getRingtone(context, uri)?.also {
                                        it.play()
                                    }
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = HabitBlue)
                        )
                        Text(name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        Text("0s", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                playingRingtone?.stop()
                onSelect(tempSelected)
            }) { Text("확인", color = HabitBlue) }
        },
        dismissButton = {
            TextButton(onClick = {
                playingRingtone?.stop()
                onDismiss()
            }) { Text("취소", color = TextSecondary) }
        },
        containerColor = DarkCard
    )
}

@Composable
fun HabitColumnManagementScreen(onBack: () -> Unit) {
    val columns = remember { mutableStateListOf("기타", "오전", "오후", "밤") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newColumnName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "열 관리",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
        ) {
            columns.forEachIndexed { index, column ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        column,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Outlined.DragHandle,
                        contentDescription = "드래그",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (index < columns.size - 1) {
                    HorizontalDivider(color = DarkBackground, thickness = 1.dp)
                }
            }
            HorizontalDivider(color = DarkBackground, thickness = 1.dp)
            // Add column row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = HabitBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("새로운 열", color = HabitBlue)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newColumnName = "" },
            title = { Text("새로운 열 추가", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = newColumnName,
                    onValueChange = { newColumnName = it },
                    placeholder = { Text("열 이름") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HabitBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = HabitBlue,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newColumnName.isNotBlank()) {
                        columns.add(newColumnName.trim())
                        newColumnName = ""
                        showAddDialog = false
                    }
                }) { Text("추가", color = HabitBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newColumnName = "" }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
}
