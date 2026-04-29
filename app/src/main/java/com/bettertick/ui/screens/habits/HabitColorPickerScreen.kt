package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private val macaronColors = listOf(
    "#F08080", "#FFB6C1", "#FFDAB9", "#B2EBE0", "#87CEEB", "#B0C4DE", "#DDA0DD", "#FF69B4"
)
private val morandiColors = listOf(
    "#C4836B", "#E8B89A", "#C8C490", "#8BBFB8", "#8BB8C9", "#A9AACC", "#C3AACC", "#D4878D"
)
private val rococoColors = listOf(
    "#C27B7B", "#E08030", "#D4C050", "#6B9E90", "#5BBEB8", "#7090B0", "#9090C0", "#C09090"
)
private val classicColors = listOf(
    "#E85050", "#E89030", "#E8D830", "#90D060", "#40D0C0", "#60B0E0", "#B060E0", "#F06080"
)
private val memphisColors = listOf(
    "#F03030", "#F07050", "#F0C030", "#10D0A0", "#20C0D0", "#6060D0", "#C070D0", "#F040A0"
)

@Composable
fun HabitColorPickerScreen(
    selectedColor: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedColor) }

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
                "컬러 픽",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Palette groups card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                listOf(
                    "마카롱" to macaronColors,
                    "모란디 색상" to morandiColors,
                    "로코코" to rococoColors,
                    "클래식" to classicColors,
                    "멤피스 색조" to memphisColors
                ).forEachIndexed { groupIndex, (label, colors) ->
                    Text(label, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        colors.forEach { hex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                .getOrDefault(Color.Gray)
                            val isSelected = tempSelected.equals(hex, ignoreCase = true)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(2.dp, c, CircleShape)
                                        .clickable { tempSelected = hex; onSelect(hex) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .clickable { tempSelected = hex; onSelect(hex) }
                                )
                            }
                        }
                    }
                    if (groupIndex < 4) Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Custom colors card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text("사용자 정의", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    // Sample custom colors shown as placeholders
                    listOf("#3F51B5", "#E91E63").forEach { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Gray)
                        val isSelected = tempSelected.equals(hex, ignoreCase = true)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.dp, c, CircleShape)
                                    .clickable { tempSelected = hex; onSelect(hex) }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { tempSelected = hex; onSelect(hex) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
