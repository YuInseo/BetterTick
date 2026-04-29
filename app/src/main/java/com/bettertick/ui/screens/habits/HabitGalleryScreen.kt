package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private val GalleryBlue = Color(0xFF4A90E2)

private data class PresetHabit(
    val name: String,
    val description: String,
    val icon: String,
    val color: String
)

private val categoryTabs = listOf("추천", "자기계발", "건강", "운동", "마음")

private val presetsByCategory = mapOf(
    "추천" to listOf(
        PresetHabit("데일리 체크인", "매일 하루를 점검하세요", "checkin", "#4CAF50"),
        PresetHabit("물 마시기", "하루 8잔 마시기", "water", "#2196F3"),
        PresetHabit("아침 식사", "아침을 거르지 마세요", "food", "#FF9800"),
        PresetHabit("일찍 일어나기", "6시 이전에 기상하기", "sun", "#FFC107"),
        PresetHabit("일찍 자기", "11시 이전에 취침하기", "sleep", "#3F51B5"),
        PresetHabit("새로운 단어 배우기", "매일 새 어휘 습득", "language", "#00BCD4"),
        PresetHabit("책 읽기", "하루 30분 독서", "book", "#1976D2"),
        PresetHabit("간식 금지", "건강한 식습관 유지", "nosmoke", "#607D8B"),
        PresetHabit("운동", "규칙적인 운동 습관", "run", "#FF9800"),
        PresetHabit("명상", "마음의 평화를 찾아요", "yoga", "#9C27B0"),
        PresetHabit("화를 내지 않기", "감정 조절 연습", "smile", "#FFC107"),
        PresetHabit("과일 먹기", "비타민 섭취", "eco", "#4CAF50")
    ),
    "자기계발" to listOf(
        PresetHabit("꽃에 물 주기", "식물 돌보기", "flower", "#E91E63"),
        PresetHabit("강아지 산책시키기", "반려동물과 산책", "pet", "#795548"),
        PresetHabit("고양이를 잘 돌보기", "고양이 케어", "pet", "#FF8C00"),
        PresetHabit("다큐멘터리 보기", "지식 쌓기", "tv", "#F44336"),
        PresetHabit("뉴스 읽기", "세상 소식 파악하기", "library", "#FF8F00"),
        PresetHabit("TV 보기", "취미 시간 확보", "tv", "#607D8B"),
        PresetHabit("일기 쓰기", "하루를 기록하기", "write", "#FF7043"),
        PresetHabit("언어 공부", "외국어 실력 향상", "language", "#00BCD4"),
        PresetHabit("자격증 공부", "스펙 쌓기", "school", "#7B1FA2"),
        PresetHabit("코딩 연습", "개발 실력 키우기", "laptop", "#37474F"),
        PresetHabit("악기 연습", "음악 실력 향상", "music", "#7B1FA2"),
        PresetHabit("사진 촬영", "일상 기록하기", "photo", "#FF9800")
    ),
    "건강" to listOf(
        PresetHabit("일찍 자기", "11시 이전 취침", "sleep", "#3F51B5"),
        PresetHabit("약을 복용하기", "처방약 잊지 않기", "spa", "#EC407A"),
        PresetHabit("눈 보호", "디지털 디톡스 실천", "eye", "#00BCD4"),
        PresetHabit("양치하기", "하루 두 번 양치", "checkin", "#4CAF50"),
        PresetHabit("샤워하기", "매일 청결 유지", "spa", "#EC407A"),
        PresetHabit("피부 관리 하기", "스킨케어 루틴", "spa", "#E91E63"),
        PresetHabit("건강을 지키기", "전반적 건강 관리", "heart", "#E53935"),
        PresetHabit("금연", "담배 끊기", "nosmoke", "#607D8B"),
        PresetHabit("물 마시기", "하루 2L 수분 섭취", "water", "#2196F3"),
        PresetHabit("아침 식사", "아침 꼭 챙기기", "food", "#FF9800"),
        PresetHabit("저녁 식사", "저녁도 챙기기", "food", "#FF5722"),
        PresetHabit("야채 먹기", "식이섬유 챙기기", "eco", "#388E3C"),
        PresetHabit("간식 금지", "다이어트 도움", "target", "#E65100"),
        PresetHabit("설탕 금지", "당 섭취 줄이기", "cake", "#E91E63"),
        PresetHabit("체중 관리", "건강 체중 유지", "target", "#E65100")
    ),
    "운동" to listOf(
        PresetHabit("달리기", "유산소 운동", "run", "#FF9800"),
        PresetHabit("걷기", "하루 만 보 걷기", "walk", "#66BB6A"),
        PresetHabit("헬스", "근력 운동", "gym", "#E53935"),
        PresetHabit("요가", "유연성 향상", "yoga", "#9C27B0"),
        PresetHabit("수영", "전신 운동", "swim", "#03A9F4"),
        PresetHabit("자전거", "사이클링", "bike", "#2196F3"),
        PresetHabit("스트레칭", "몸 풀기", "gym", "#1976D2"),
        PresetHabit("등산", "자연 속 운동", "nature", "#388E3C"),
        PresetHabit("플랭크", "코어 강화", "gym", "#B71C1C"),
        PresetHabit("팔굽혀펴기", "상체 근력", "gym", "#E53935"),
        PresetHabit("스쿼트", "하체 근력", "gym", "#880E4F"),
        PresetHabit("홈트레이닝", "집에서 운동", "gym", "#607D8B")
    ),
    "마음" to listOf(
        PresetHabit("명상", "마음 챙김 연습", "yoga", "#9C27B0"),
        PresetHabit("감사 일기", "긍정적 마음 기르기", "write", "#FF7043"),
        PresetHabit("긍정 확언", "자기 긍정 연습", "smile", "#FFC107"),
        PresetHabit("심호흡", "스트레스 해소", "spa", "#EC407A"),
        PresetHabit("디지털 디톡스", "스마트폰 줄이기", "eye", "#3F51B5"),
        PresetHabit("친구에게 연락", "사회적 유대 강화", "phone", "#43A047"),
        PresetHabit("봉사 활동", "나눔 실천", "heart", "#E53935"),
        PresetHabit("취미 즐기기", "좋아하는 것 하기", "art", "#9C27B0"),
        PresetHabit("자연 감상", "자연 속 힐링", "nature", "#388E3C"),
        PresetHabit("음악 듣기", "음악으로 힐링", "music", "#7B1FA2"),
        PresetHabit("독서", "마음의 양식", "book", "#1976D2"),
        PresetHabit("산책", "여유로운 걷기", "walk", "#66BB6A")
    )
)

@Composable
fun HabitGalleryScreen(
    onBack: () -> Unit,
    onAddHabit: (HabitDraft) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreation by remember { mutableStateOf(false) }

    if (showCreation) {
        HabitCreationScreen(
            onBack = { showCreation = false },
            onDone = { draft -> onAddHabit(draft) }
        )
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
                "갤러리",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Category tabs (horizontal scrollable pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryTabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) GalleryBlue else DarkCard)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        val presets = presetsByCategory[categoryTabs[selectedTab]] ?: emptyList()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                ) {
                    presets.forEachIndexed { index, preset ->
                        PresetHabitRow(
                            preset = preset,
                            onAdd = {
                                onAddHabit(
                                    HabitDraft(
                                        name = preset.name,
                                        description = preset.description,
                                        icon = preset.icon,
                                        color = preset.color
                                    )
                                )
                            }
                        )
                        if (index < presets.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = DarkBackground,
                                thickness = 1.dp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = { showCreation = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
            ) {
                Text(
                    "새로운 습관 만들기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetHabitRow(
    preset: PresetHabit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitIconView(
            iconKey = preset.icon,
            colorHex = preset.color,
            circleSize = 44.dp,
            iconSize = 24.dp,
            fallbackText = preset.name
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                preset.description,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        IconButton(
            onClick = onAdd,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "추가",
                tint = GalleryBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
