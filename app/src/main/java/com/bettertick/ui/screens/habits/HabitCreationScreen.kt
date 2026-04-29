package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

private val CreationBlue = Color(0xFF4A90E2)

private val quotes = listOf(
    "Whatever you do, do it well.",
    "작은 습관이 큰 변화를 만든다.",
    "오늘 하루도 최선을 다하자.",
    "성공은 매일의 노력이 쌓인 결과다.",
    "Keep going, you're doing great!",
    "습관이 운명을 만든다.",
    "One day at a time.",
    "지금 시작하는 것이 최선이다.",
    "꾸준함이 재능을 이긴다.",
    "Every action counts.",
    "작심삼일도 반복하면 습관이 된다.",
    "Be the change you wish to see.",
    "매일 조금씩 더 나아지자.",
    "Success is the sum of small efforts.",
    "Dream it. Believe it. Achieve it."
)

@Composable
fun HabitCreationScreen(
    onBack: () -> Unit,
    onDone: (HabitDraft) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("checkin") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var useTextIcon by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var quoteIndex by remember { mutableIntStateOf(0) }

    if (showOptions) {
        HabitOptionsScreen(
            initialDraft = HabitDraft(
                name = habitName.trim().ifBlank { "새 습관" },
                description = quotes[quoteIndex],
                icon = if (useTextIcon) "" else selectedIcon,
                color = selectedColor
            ),
            onBack = { showOptions = false },
            onSave = { draft -> onDone(draft) }
        )
        return
    }

    if (showColorPicker) {
        HabitColorPickerScreen(
            selectedColor = selectedColor,
            onBack = { showColorPicker = false },
            onSelect = { color ->
                selectedColor = color
                showColorPicker = false
            }
        )
        return
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(selectedColor)) }
        .getOrDefault(Color(0xFF4CAF50))
    val selectedDef = habitIconOptions.firstOrNull { it.key == selectedIcon }

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
                "새로운 습관",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Name card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("이름", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    placeholder = { Text("데일리 체크인", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CreationBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = CreationBlue,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Icon card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("아이콘", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))

                // Mode selector row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Selected icon preview (tap → color picker)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (!useTextIcon) Modifier.border(3.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { useTextIcon = false; showColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedDef != null && !useTextIcon) {
                            Icon(selectedDef.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        } else {
                            Text(selectedIcon.ifBlank { "A" }, fontSize = 28.sp, color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Text initial mode
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9B59B6))
                            .then(
                                if (useTextIcon) Modifier.border(3.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { useTextIcon = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            habitName.take(1).uppercase().ifBlank { "A" },
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Icon grid (7 per row) using Material icons
                habitIconOptions.chunked(7).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { def ->
                            val iconBg = runCatching {
                                Color(android.graphics.Color.parseColor(def.defaultColor))
                            }.getOrDefault(Color.Gray)
                            val isSelected = !useTextIcon && def.key == selectedIcon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(iconBg)
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        selectedIcon = def.key
                                        selectedColor = def.defaultColor
                                        useTextIcon = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(def.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quote card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "인용",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { quoteIndex = (quoteIndex + 1) % quotes.size },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "새 인용", tint = CreationBlue)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(quotes[quoteIndex], color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = { showOptions = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CreationBlue)
            ) {
                Text(
                    "다음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
