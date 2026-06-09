package com.bettertick.ui.screens.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
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

private val CARTO_VOYAGER = object : OnlineTileSourceBase(
    "CartoVoyager", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
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

/** Colored dot with white border — used instead of the default OSMDroid hand icon. */
private fun Context.dotMarker(colorInt: Int = 0xFFFF8C00.toInt(), sizeDp: Float = 18f): BitmapDrawable {
    val px = (sizeDp * resources.displayMetrics.density + 0.5f).toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val strokeW = px * 0.18f
    val r = px / 2f - strokeW / 2
    cv.drawCircle(px / 2f, px / 2f, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt; style = Paint.Style.FILL
    })
    cv.drawCircle(px / 2f, px / 2f, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = strokeW
    })
    return BitmapDrawable(resources, bmp)
}

@SuppressLint("MissingPermission")
@Composable
fun LocationHistoryScreen(
    viewModel: LocationHistoryViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMap by remember { mutableStateOf(true) }
    val records by viewModel.getRecordsForDate(selectedDate.toString()).collectAsState(emptyList())
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            val loc = LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            if (loc != null) currentLocation = GeoPoint(loc.latitude, loc.longitude)
        }
    }

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

        // weight(1f) ensures TopBar is never covered by the map/list
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                showMap && (records.isNotEmpty() || currentLocation != null) ->
                    RouteMapView(records = records, currentLocation = currentLocation)
                !showMap && records.isNotEmpty() ->
                    RouteListView(records = records)
                else -> EmptyState()
            }
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
private fun RouteMapView(records: List<LocationRecord>, currentLocation: GeoPoint?) {
    val context = LocalContext.current
    val points = remember(records) { records.map { GeoPoint(it.latitude, it.longitude) } }
    var selectedRecord by remember { mutableStateOf<LocationRecord?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(points, currentLocation) {
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
            currentLocation != null -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(currentLocation)
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
                    mapView.setTileSource(CARTO_VOYAGER)
                    mapView.setMultiTouchControls(true)
                    @Suppress("DEPRECATION")
                    mapView.setBuiltInZoomControls(false)
                    val initCenter = currentLocation ?: points.lastOrNull()
                        ?: GeoPoint(37.5665, 126.9780)
                    mapView.controller.setZoom(14.0)
                    mapView.controller.setCenter(initCenter)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                if (points.size >= 2) {
                    // Glow layer (wider, semi-transparent)
                    mapView.overlays.add(Polyline(mapView).apply {
                        setPoints(points)
                        outlinePaint.color = 0x55FF8C00.toInt()
                        outlinePaint.strokeWidth = 28f
                        outlinePaint.isAntiAlias = true
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    })
                    // Core line
                    mapView.overlays.add(Polyline(mapView).apply {
                        setPoints(points)
                        outlinePaint.color = 0xFFFF8C00.toInt()
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.isAntiAlias = true
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    })
                }
                if (records.isNotEmpty()) {
                    records.forEachIndexed { index, record ->
                        val isFirst = index == 0
                        val isLast = index == records.lastIndex
                        // Start = green, end = red, middle = orange
                        val color = when {
                            isFirst -> 0xFF4CAF50.toInt()
                            isLast  -> 0xFFF44336.toInt()
                            else    -> 0xFFFF8C00.toInt()
                        }
                        val size = if (isFirst || isLast) 22f else 14f
                        mapView.overlays.add(Marker(mapView).apply {
                            position = GeoPoint(record.latitude, record.longitude)
                            icon = context.dotMarker(color, size)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = when {
                                isFirst -> "출발"
                                isLast  -> "도착"
                                else    -> null
                            }
                            setOnMarkerClickListener { _, _ ->
                                selectedRecord = record; true
                            }
                        })
                    }
                } else if (currentLocation != null) {
                    mapView.overlays.add(Marker(mapView).apply {
                        position = currentLocation
                        icon = context.dotMarker(0xFF2196F3.toInt(), 22f)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "현재 위치"
                    })
                }
                mapView.invalidate()
            }
        )

        if (records.isNotEmpty()) {
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
