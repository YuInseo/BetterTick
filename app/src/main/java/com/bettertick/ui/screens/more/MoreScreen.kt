package com.bettertick.ui.screens.more

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.update.AppUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    userName: String,
    userPhotoUrl: String?,
    onSignOut: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToTabBar: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    val currentVersion = remember { AppUpdater.currentVersionName(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile card
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAccount() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    if (!userPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifEmpty { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tab bar
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.GridView,
                    title = "탭 바",
                    onClick = onNavigateToTabBar
                )
            }

            // Settings group
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "외관",
                    onClick = onNavigateToAppearance
                )
                SettingsItem(
                    icon = Icons.Outlined.AccessTime,
                    title = "날짜 & 시간",
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Outlined.MusicNote,
                    title = "사운드 & 알림",
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Outlined.Widgets,
                    title = "위젯",
                    onClick = onNavigateToWidgets
                )
                SettingsItem(
                    icon = Icons.Outlined.Tune,
                    title = "일반",
                    onClick = {}
                )
            }

            // Integration
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.IosShare,
                    title = "통합 및 가져오기",
                    onClick = {}
                )
            }

            // Update check
            SettingsCard {
                UpdateCheckItem(
                    currentVersion = currentVersion,
                    checking = checkingUpdate,
                    onClick = {
                        if (checkingUpdate) return@UpdateCheckItem
                        checkingUpdate = true
                        scope.launch {
                            try {
                                val release = AppUpdater.fetchLatest()
                                if (release == null) {
                                    val reason = AppUpdater.lastFetchError ?: "(원인 불명)"
                                    Toast.makeText(context, "업데이트 확인 실패: $reason", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val current = AppUpdater.currentVersionCode(context)
                                if (!AppUpdater.isNewer(release.versionCode, current)) {
                                    Toast.makeText(
                                        context,
                                        "이미 최신 버전입니다 (설치: ${current} / 최신: ${release.versionCode})",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                Toast.makeText(context, "v${release.versionName} 다운로드 중…", Toast.LENGTH_SHORT).show()
                                val apk = AppUpdater.downloadApk(context, release)
                                if (apk == null) {
                                    val reason = AppUpdater.lastDownloadError ?: "(원인 불명)"
                                    Toast.makeText(context, "다운로드 실패: $reason", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                AppUpdater.launchInstall(context, apk)
                            } finally {
                                checkingUpdate = false
                            }
                        }
                    }
                )
            }

            // Logout
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "로그아웃",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OverdueRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun UpdateCheckItem(
    currentVersion: String,
    checking: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !checking) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.SystemUpdate,
            contentDescription = "업데이트 확인",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "업데이트 확인",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "현재 v$currentVersion",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        if (checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
