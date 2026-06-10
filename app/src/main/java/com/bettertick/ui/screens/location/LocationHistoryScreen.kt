package com.bettertick.ui.screens.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

private fun Context.dotBitmap(colorInt: Int = 0xFFFF8C00.toInt(), sizeDp: Float = 18f): Bitmap {
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
    return bmp
}

private val COORD_PATTERN = Regex("^-?\\d+\\.\\d+,\\s*-?\\d+\\.\\d+$")

private suspend fun resolveAddress(context: Context, record: LocationRecord): String {
    if (!record.address.matches(COORD_PATTERN)) return record.address
    if (!Geocoder.isPresent()) return record.address
    val geocoder = Geocoder(context, Locale.KOREAN)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(record.latitude, record.longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull()?.getAddressLine(0) ?: record.address)
                        }
                        override fun onError(errorMessage: String?) { cont.resume(record.address) }
                    })
            } catch (e: Exception) { cont.resume(record.address) }
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching {
                geocoder.getFromLocation(record.latitude, record.longitude, 1)
                    ?.firstOrNull()?.getAddressLine(0)
            }.getOrNull() ?: record.address
        }
    }
}

@Composable
private fun resolvedAddress(record: LocationRecord): String {
    val context = LocalContext.current
    var display by remember(record.id) { mutableStateOf(record.address) }
    LaunchedEffect(record.id) {
        val resolved = resolveAddress(context, record)
        if (resolved != record.address) display = resolved
    }
    return display
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
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        // 1) Last known position → dot appears instantly
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) currentLocation = LatLng.from(loc.latitude, loc.longitude)
        }
        // 2) One-shot fresh fix → accurate position within seconds
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) currentLocation = LatLng.from(loc.latitude, loc.longitude)
            }
        // 3) Ongoing updates every 5 s
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { currentLocation = LatLng.from(it.latitude, it.longitude) }
            }
        }
        runCatching { client.requestLocationUpdates(request, callback, Looper.getMainLooper()) }
        onDispose {
            cts.cancel()
            runCatching { client.removeLocationUpdates(callback) }
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
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
private fun RouteMapView(records: List<LocationRecord>, currentLocation: LatLng?) {
    val context = LocalContext.current
    var selectedRecord by remember { mutableStateOf<LocationRecord?>(null) }
    val kakaoMapRef = remember { mutableStateOf<KakaoMap?>(null) }
    val markerLayerRef = remember { mutableStateOf<LabelLayer?>(null) }
    val liveLayerRef = remember { mutableStateOf<LabelLayer?>(null) }
    val liveLabelRef = remember { mutableStateOf<Label?>(null) }
    val points = remember(records) { records.map { LatLng.from(it.latitude, it.longitude) } }

    // Rebuild route overlays when records change
    LaunchedEffect(points, kakaoMapRef.value) {
        val map = kakaoMapRef.value ?: return@LaunchedEffect

        // Camera for route
        when {
            points.size >= 2 ->
                map.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), 200))
            points.size == 1 ->
                map.moveCamera(CameraUpdateFactory.newCenterPosition(points[0], 15))
        }

        // Clear old markers and route
        markerLayerRef.value?.removeAll()
        map.routeLineManager?.layer?.removeAll()

        if (points.size >= 2) {
            val glowSet = RouteLineStylesSet.from(
                "glow",
                RouteLineStyles.from(RouteLineStyle.from(28f, 0x55FF8C00.toInt()))
            )
            val coreSet = RouteLineStylesSet.from(
                "core",
                RouteLineStyles.from(RouteLineStyle.from(10f, 0xFFFF8C00.toInt()))
            )
            map.routeLineManager?.layer?.addRouteLine(
                RouteLineOptions.from(listOf(RouteLineSegment.from(points).setStyles(glowSet.getStyles(0))))
            )
            map.routeLineManager?.layer?.addRouteLine(
                RouteLineOptions.from(listOf(RouteLineSegment.from(points).setStyles(coreSet.getStyles(0))))
            )
        }

        if (records.isNotEmpty()) {
            val layer = map.labelManager?.addLayer(LabelLayerOptions.from("markers"))
                ?: return@LaunchedEffect
            markerLayerRef.value = layer

            records.forEachIndexed { i, record ->
                val isFirst = i == 0
                val isLast = i == records.lastIndex
                val color = when {
                    isFirst -> 0xFF4CAF50.toInt()
                    isLast -> 0xFFF44336.toInt()
                    else -> 0xFFFF8C00.toInt()
                }
                layer.addLabel(
                    LabelOptions.from(LatLng.from(record.latitude, record.longitude))
                        .setStyles(LabelStyles.from(
                            LabelStyle.from(context.dotBitmap(color, if (isFirst || isLast) 22f else 14f))
                                .setAnchorPoint(0.5f, 0.5f)
                        ))
                        .setTag(record)
                )
            }

            map.setOnLabelClickListener { _, _, label ->
                (label.tag as? LocationRecord)?.let { selectedRecord = it }
                true
            }
        }
    }

    // Live location dot — repositioned independently, no overlay rebuild
    LaunchedEffect(currentLocation, kakaoMapRef.value) {
        val map = kakaoMapRef.value ?: return@LaunchedEffect
        val loc = currentLocation ?: return@LaunchedEffect

        if (records.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(loc, 15))
        }

        val existing = liveLabelRef.value
        if (existing != null) {
            existing.moveTo(loc)
        } else {
            val layer = liveLayerRef.value ?: map.labelManager?.addLayer(
                LabelLayerOptions.from("live")
            ) ?: return@LaunchedEffect
            liveLayerRef.value = layer
            liveLabelRef.value = layer.addLabel(
                LabelOptions.from(loc)
                    .setStyles(LabelStyles.from(
                        LabelStyle.from(context.dotBitmap(0xFF2196F3.toInt(), 24f))
                            .setAnchorPoint(0.5f, 0.5f)
                    ))
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    val initCenter = currentLocation ?: points.lastOrNull()
                        ?: LatLng.from(37.5665, 126.9780)
                    mv.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(error: Exception) {}
                        },
                        object : KakaoMapReadyCallback() {
                            override fun getPosition(): LatLng = initCenter
                            override fun getZoomLevel(): Int = if (records.isEmpty()) 15 else 14
                            override fun onMapReady(map: KakaoMap) {
                                kakaoMapRef.value = map
                            }
                        }
                    )
                }
            }
        )

        // Visit count badge (top-right)
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

        // Zoom controls (bottom-left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface.copy(alpha = 0.9f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { kakaoMapRef.value?.moveCamera(CameraUpdateFactory.zoomIn()) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { kakaoMapRef.value?.moveCamera(CameraUpdateFactory.zoomOut()) },
                contentAlignment = Alignment.Center
            ) {
                Text("−", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // "내 위치" button (bottom-right)
        if (records.isNotEmpty() && currentLocation != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .clickable {
                        currentLocation?.let { loc ->
                            kakaoMapRef.value?.moveCamera(CameraUpdateFactory.newCenterPosition(loc))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "내 위치로",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Selected record info card (bottom)
        selectedRecord?.let { rec ->
            val time = remember(rec.timestamp) {
                java.text.SimpleDateFormat("HH:mm", Locale.KOREAN).format(rec.timestamp.toDate())
            }
            val address = resolvedAddress(rec)
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
                        Text(address, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
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
            val address = resolvedAddress(record)

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
                            address,
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
