package com.bettertick.ui.screens.location

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.LocationRecord
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CARTO_DARK = object : OnlineTileSourceBase(
    "CartoDark", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    ),
    "© CartoDB, © OpenStreetMap contributors",
    TileSourcePolicy(
        2,
        TileSourcePolicy.FLAG_NO_BULK or TileSourcePolicy.FLAG_NO_PREVENTIVE or
            TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL or TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        "${baseUrl}${MapTileIndex.getZoom(pMapTileIndex)}" +
            "/${MapTileIndex.getX(pMapTileIndex)}" +
            "/${MapTileIndex.getY(pMapTileIndex)}.png"
}

@Composable
fun LocationHistoryScreen(
    viewModel: LocationHistoryViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMap by remember { mutableStateOf(true) }
    val records by viewModel.getRecordsForDate(selectedDate.toString()).collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopBar(
            date = selectedDate,
            showMap = showMap,
            onPrev = { selectedDate = selectedDate.minusDays(1) },
            onNext = { if (selectedDate < LocalDate.now()) selectedDate = selectedDate.plusDays(1) },
            onToggleView = { showMap = !showMap }
        )

        if (records.isEmpty()) {
            EmptyState()
        } else if (showMap) {
            RouteMapView(records = records)
        } else {
            RouteListView(records = records)
        }
    }
}

@Composable
private fun TopBar(
    date: LocalDate,
    showMap: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleView: () -> Unit
) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "이전", tint = TextSecondary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { if (date < today) onNext() }, enabled = date < today) {
            Icon(
                Icons.Outlined.ChevronRight, contentDescription = "다음",
                tint = if (date < today) TextSecondary else TextTertiary
            )
        }
        IconButton(onClick = onToggleView) {
            Icon(
                if (showMap) Icons.Outlined.List else Icons.Outlined.Map,
                contentDescription = if (showMap) "목록 보기" else "지도 보기",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RouteMapView(records: List<LocationRecord>) {
    val context = LocalContext.current
    val points = remember(records) { records.map { GeoPoint(it.latitude, it.longitude) } }
    var selectedRecord by remember { mutableStateOf<LocationRecord?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(points) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        when {
            points.size >= 2 -> {
                val bounds = BoundingBox.fromGeoPoints(points)
                mapView.post { mapView.zoomToBoundingBox(bounds, true, 120) }
            }
            points.size == 1 -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(points[0])
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().apply {
                    load(ctx, ctx.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
                    userAgentValue = ctx.packageName
                    osmdroidBasePath = ctx.cacheDir
                    osmdroidTileCache = java.io.File(ctx.cacheDir, "osm_tiles")
                }
                MapView(ctx).also { mapView ->
                    mapViewRef.value = mapView
                    mapView.setTileSource(CARTO_DARK)
                    mapView.setMultiTouchControls(true)
                    @Suppress("DEPRECATION")
                    mapView.setBuiltInZoomControls(false)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                if (points.size >= 2) {
                    val polyline = Polyline(mapView).apply {
                        setPoints(points)
                        outlinePaint.color = 0xFFFF8C00.toInt()
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.isAntiAlias = true
                    }
                    mapView.overlays.add(polyline)
                }
                records.forEachIndexed { index, record ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(record.latitude, record.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = when (index) {
                            0 -> "출발"
                            records.lastIndex -> "도착"
                            else -> null
                        }
                        setOnMarkerClickListener { _, _ ->
                            selectedRecord = record
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                "${records.size}곳 방문",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        selectedRecord?.let { rec ->
            val time = remember(rec.timestamp) {
                java.text.SimpleDateFormat("HH:mm", Locale.KOREAN).format(rec.timestamp.toDate())
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(16.dp)
            ) {
                Column {
                    Text(time, color = TextTertiary, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            rec.address.ifBlank { "%.4f, %.4f".format(rec.latitude, rec.longitude) },
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteListView(records: List<LocationRecord>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(records, key = { it.id }) { record ->
            val isLast = record == records.last()
            val time = remember(record.timestamp) {
                java.text.SimpleDateFormat("HH:mm", Locale.KOREAN).format(record.timestamp.toDate())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.padding(bottom = if (!isLast) 0.dp else 20.dp)) {
                    Text(time, color = TextTertiary, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            record.address.ifBlank { "%.4f, %.4f".format(record.latitude, record.longitude) },
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.LocationOn, null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("이 날의 위치 기록이 없어요", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("설정에서 위치 기록을 켜주세요", color = TextTertiary, fontSize = 12.sp)
        }
    }
}
