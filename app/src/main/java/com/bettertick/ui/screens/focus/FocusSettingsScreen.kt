package com.bettertick.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.calendar.OptionCard
import com.bettertick.ui.screens.calendar.OptionPage
import com.bettertick.ui.screens.calendar.OptionSwitchRow
import com.bettertick.ui.screens.more.OptionChoiceRow
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState

/** 포커스 > 설정 — TickTick 뽀모도로 설정. */
@Composable
fun FocusSettingsScreen(onBack: () -> Unit) {
    OptionPage(title = "포커스 설정", onBack = onBack) {
        OptionCard {
            OptionChoiceRow(
                title = "뽀모 길이", value = FocusPrefs.pomoMinutes,
                options = (1..18).map { it * 5 }, label = { "${it}분" },
                onPick = FocusPrefs::updatePomoMinutes
            )
            OptionChoiceRow(
                title = "짧은 휴식", value = FocusPrefs.shortBreakMinutes,
                options = listOf(1, 2, 3, 5, 10, 15, 20, 30), label = { "${it}분" },
                onPick = FocusPrefs::updateShortBreak
            )
            OptionChoiceRow(
                title = "긴 휴식", value = FocusPrefs.longBreakMinutes,
                options = listOf(5, 10, 15, 20, 25, 30, 45, 60), label = { "${it}분" },
                onPick = FocusPrefs::updateLongBreak
            )
            OptionChoiceRow(
                title = "긴 휴식 간격", subtitle = "몇 번의 뽀모마다 긴 휴식을 할지",
                value = FocusPrefs.longBreakInterval,
                options = (2..8).toList(), label = { "${it}뽀모" },
                onPick = FocusPrefs::updateLongInterval
            )
        }
        OptionCard {
            OptionSwitchRow(
                title = "다음 뽀모 자동 시작", subtitle = "휴식이 끝나면 바로 집중을 시작합니다.",
                checked = FocusPrefs.autoStartNextPomo, onCheckedChange = FocusPrefs::updateAutoStartNextPomo
            )
            OptionSwitchRow(
                title = "휴식 자동 시작", subtitle = "집중이 끝나면 바로 휴식을 시작합니다.",
                checked = FocusPrefs.autoStartBreak, onCheckedChange = FocusPrefs::updateAutoStartBreak
            )
        }
        OptionCard {
            OptionChoiceRow(
                title = "일일 목표", value = FocusPrefs.dailyGoal,
                options = (1..16).toList(), label = { "${it}뽀모" },
                onPick = FocusPrefs::updateDailyGoal
            )
        }
        OptionCard {
            OptionSwitchRow(
                title = "화면 켜짐 유지", subtitle = "타이머 화면이 보이는 동안 화면을 끄지 않습니다.",
                checked = FocusPrefs.keepScreenOn, onCheckedChange = FocusPrefs::updateKeepScreenOn
            )
            OptionSwitchRow(
                title = "종료 알림음", checked = FocusPrefs.endSound, onCheckedChange = FocusPrefs::updateEndSound
            )
            OptionSwitchRow(
                title = "종료 진동", checked = FocusPrefs.endVibrate, onCheckedChange = FocusPrefs::updateEndVibrate
            )
        }
    }
}

/** 뽀모도로 | 스톱워치 segmented switch. */
@Composable
fun FocusModeSwitch(mode: FocusMode, enabled: Boolean, onChange: (FocusMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        FocusMode.entries.forEach { m ->
            val sel = m == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) ThemeState.accentColor else Color.Transparent)
                    .clickable(enabled = enabled) { onChange(m) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    m.label,
                    fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) Color.White else if (enabled) TextSecondary else TextTertiary
                )
            }
        }
    }
}

/** 오늘의 뽀모 n/목표 + 집중 시간. */
@Composable
fun FocusTodaySummary(pomos: Int, goal: Int, focusMinutes: Long, showPomos: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (showPomos) {
                Text("$pomos", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(" / ${goal}뽀모", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.weight(1f))
            }
            val h = focusMinutes / 60
            val m = focusMinutes % 60
            Text(
                "오늘 집중 " + (if (h > 0) "${h}시간 ${m}분" else "${m}분"),
                fontSize = 14.sp, color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (showPomos) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (pomos.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ThemeState.accentColor,
                trackColor = TextTertiary.copy(alpha = 0.25f)
            )
        }
    }
}
