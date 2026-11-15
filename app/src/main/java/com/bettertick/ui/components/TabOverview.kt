package com.bettertick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary

/** Shared spacing and hierarchy for the main tabs; each screen supplies real scoped data. */
@Composable
fun TabOverview(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DarkCard).padding(18.dp)) {
        Text(title, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 12.sp, lineHeight = 19.sp, color = TextSecondary)
        if (progress != null) {
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }
    }
}

@Composable
fun EmptyTabState(title: String, description: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(description, fontSize = 13.sp, lineHeight = 21.sp, color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 6.dp)) { Text(action) }
    }
}
