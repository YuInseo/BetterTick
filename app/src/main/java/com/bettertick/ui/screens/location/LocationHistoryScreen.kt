package com.bettertick.ui.screens.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bettertick.data.model.FavoritePlace
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

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

/**
 * 깃발 아이콘 — 건물 방문(dwell) 지점 표시용. 세로 깃대 + 위쪽 삼각 깃발.
 * 앵커를 (0.5, 1.0)으로 두면 깃대 밑동이 실제 좌표에 닿는다.
 */
private fun Context.flagBitmap(sizeDp: Float = 28f): Bitmap {
    val h = (sizeDp * resources.displayMetrics.density + 0.5f).toInt()
    val w = (h * 0.8f).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val poleX = w * 0.22f
    val poleTop = h * 0.06f
    val poleW = h * 0.07f
    // 깃대
    cv.drawRect(poleX - poleW / 2, poleTop, poleX + poleW / 2, h.toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF455A64.toInt(); style = Paint.Style.FILL })
    // 깃발(삼각형)
    val flag = android.graphics.Path().apply {
        moveTo(poleX + poleW / 2, poleTop)
        lineTo(w * 0.92f, h * 0.22f)
        lineTo(poleX + poleW / 2, h * 0.40f)
        close()
    }
    cv.drawPath(flag, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF44336.toInt(); style = Paint.Style.FILL })
    // 밑동 점
    cv.drawCircle(poleX, h * 0.97f, poleW * 1.1f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF455A64.toInt(); style = Paint.Style.FILL })
    return bmp
}

/** 별 아이콘 — 즐겨찾기 위치 표시용(금색 5각 별 + 흰 테두리). */
private fun Context.starBitmap(sizeDp: Float = 26f, colorInt: Int = 0xFFFFC107.toInt()): Bitmap {
    val px = (sizeDp * resources.displayMetrics.density + 0.5f).toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val cx = px / 2f
    val cy = px / 2f
    val outer = px * 0.46f
    val inner = outer * 0.5f
    val path = android.graphics.Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val ang = Math.toRadians((-90 + i * 36).toDouble())
        val x = cx + (r * Math.cos(ang)).toFloat()
        val y = cy + (r * Math.sin(ang)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt; style = Paint.Style.FILL })
    cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = px * 0.06f
    })
    return bmp
}

private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val out = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, out)
    return out[0].toDouble()
}

/** 주어진 좌표가 어떤 즐겨찾기 반경(60m) 안이면 그 이름을, 아니면 null. */
private fun favoriteNameFor(
    favorites: List<FavoritePlace>,
    lat: Double,
    lng: Double
): String? {
    var best: String? = null
    var bestDist = 60.0
    favorites.forEach { f ->
        val d = distanceMeters(lat, lng, f.latitude, f.longitude)
        if (d <= bestDist) { bestDist = d; best = f.name }
    }
    return best
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

private suspend fun resolvePlaceName(context: Context, record: LocationRecord): String {
    if (!Geocoder.isPresent()) return "방문한 장소"
    val geocoder = Geocoder(context, Locale.KOREAN)
    val addr: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(record.latitude, record.longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull())
                        }
                        override fun onError(errorMessage: String?) { cont.resume(null) }
                    })
            } catch (e: Exception) { cont.resume(null) }
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(record.latitude, record.longitude, 1)?.firstOrNull() }.getOrNull()
        }
    }
    val feature = addr?.featureName
    return when {
        feature != null && feature.isNotBlank() && !feature.all { it.isDigit() } -> feature
        addr?.thoroughfare != null -> addr.thoroughfare
        addr?.subLocality != null -> addr.subLocality
        else -> "방문한 장소"
    }
}

@Composable
private fun resolvedPlaceName(record: LocationRecord, favorites: List<FavoritePlace>): String {
    val context = LocalContext.current
    val favName = favoriteNameFor(favorites, record.latitude, record.longitude)
    // 깃발(건물 방문)은 기록 시 장소명을 저장해 뒀으니 그대로 사용. 없으면 geocode.
    val seed = record.placeName.takeIf { it.isNotBlank() } ?: "방문한 장소"
    var name by remember(record.id) { mutableStateOf(seed) }
    LaunchedEffect(record.id) {
        if (record.placeName.isBlank()) name = resolvePlaceName(context, record)
    }
    // 즐겨찾기 반경 안이면 그 이름을 최우선으로.
    return favName ?: name
}

@Composable
private fun resolvedAddress(record: LocationRecord, favorites: List<FavoritePlace>): String {
    val context = LocalContext.current
    val favName = favoriteNameFor(favorites, record.latitude, record.longitude)
    var display by remember(record.id) { mutableStateOf(record.address) }
    LaunchedEffect(record.id) {
        val resolved = resolveAddress(context, record)
        if (resolved != record.address) display = resolved
    }
    // 즐겨찾기로 이름을 붙인 위치면 지번 주소 대신 그 이름을 보여준다.
    return favName ?: display
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationHistoryScreen(
    viewModel: LocationHistoryViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMap by remember { mutableStateOf(true) }
    val records by viewModel.getRecordsForDate(selectedDate.toString()).collectAsState(emptyList())
    val favorites by viewModel.favorites.collectAsState()
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    // 즐겨찾기 필터 — 켜면 즐겨찾기된 위치 기록만 보여준다.
    var favoritesOnly by remember { mutableStateOf(false) }
    val displayedRecords = remember(records, favorites, favoritesOnly) {
        if (favoritesOnly) {
            records.filter { favoriteNameFor(favorites, it.latitude, it.longitude) != null }
        } else records
    }

    // 현재 위치 점/줌이 동작하려면 런타임 위치 권한이 필요하다. 없으면 화면
    // 진입 시 요청하고, 허용되면 아래 수집 효과가 다시 돌도록 상태로 추적.
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    var locationGranted by remember { mutableStateOf(hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!locationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 전경에서만 1초·고정확도로 빨간 점을 갱신한다. 화면이 가려지면(ON_PAUSE)
    // 즉시 업데이트를 멈춰 배터리를 아끼고, 다시 보이면(ON_RESUME) 재개한다.
    // 지속적인 백그라운드 기록은 LocationTrackingService가 저전력으로 담당.
    val locationLifecycle = LocalLifecycleOwner.current
    DisposableEffect(locationGranted, locationLifecycle) {
        if (!locationGranted) {
            onDispose { }
        } else {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        currentLocation = LatLng.from(it.latitude, it.longitude)
                        // 화면을 보는 동안 이동을 바로 기록 → 경로가 실시간으로 쌓임.
                        viewModel.recordWaypointIfMoved(it.latitude, it.longitude)
                    }
                }
            }
            @SuppressLint("MissingPermission")
            fun startUpdates() {
                // Last known + one-shot fresh fix → dot appears instantly.
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) currentLocation = LatLng.from(loc.latitude, loc.longitude)
                }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) currentLocation = LatLng.from(loc.latitude, loc.longitude)
                    }
                runCatching { client.requestLocationUpdates(request, callback, Looper.getMainLooper()) }
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> startUpdates()
                    Lifecycle.Event.ON_PAUSE -> runCatching { client.removeLocationUpdates(callback) }
                    else -> {}
                }
            }
            locationLifecycle.lifecycle.addObserver(observer)
            onDispose {
                cts.cancel()
                locationLifecycle.lifecycle.removeObserver(observer)
                runCatching { client.removeLocationUpdates(callback) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopBar(
            date = selectedDate,
            onPrev = { selectedDate = selectedDate.minusDays(1) },
            onNext = { if (selectedDate < LocalDate.now()) selectedDate = selectedDate.plusDays(1) }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            when {
                showMap ->
                    RouteMapView(records = displayedRecords, currentLocation = currentLocation, favorites = favorites)
                displayedRecords.isNotEmpty() ->
                    RouteListView(records = displayedRecords, favorites = favorites)
                else -> EmptyState()
            }

            // 지도/목록 전환 버튼 — 우측 상단에서 좌측 하단으로 이동. 지도·목록
            // 어느 화면에서든 누를 수 있게 콘텐츠 위에 오버레이.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .clickable { showMap = !showMap },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (showMap) Icons.Outlined.List else Icons.Outlined.Map,
                    contentDescription = if (showMap) "목록 보기" else "지도 보기",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 즐겨찾기 버튼 — 우측 하단. 탭하면 즐겨찾기 필터 on/off(켜면 즐겨찾기
            // 위치만 표시), 길게 누르면 현재 위치에 이름 짓기(즐겨찾기 추가).
            // 지도 보기에선 '내 위치' 버튼 위로 올리고, 목록 보기에선 맨 아래로.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = if (showMap) 60.dp else 12.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (favoritesOnly) MaterialTheme.colorScheme.primary
                        else DarkSurface.copy(alpha = 0.9f)
                    )
                    .combinedClickable(
                        onClick = { favoritesOnly = !favoritesOnly },
                        onLongClick = { if (currentLocation != null) showFavoriteDialog = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "즐겨찾기 필터(길게: 추가)",
                    tint = if (favoritesOnly) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showFavoriteDialog) {
        val loc = currentLocation
        FavoriteNameDialog(
            onDismiss = { showFavoriteDialog = false },
            onConfirm = { name ->
                if (loc != null && name.isNotBlank()) {
                    viewModel.addFavorite(name.trim(), loc.latitude, loc.longitude)
                }
                showFavoriteDialog = false
            }
        )
    }
}

@Composable
private fun FavoriteNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("이 위치 이름 짓기", color = Color.White) },
        text = {
            Column {
                Text(
                    "현재 위치에 이름을 붙이면 지번 주소 대신 그 이름으로 표시됩니다.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("예: 집, 회사, 헬스장") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun TopBar(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
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
    }
}

@Composable
private fun RouteMapView(records: List<LocationRecord>, currentLocation: LatLng?, favorites: List<FavoritePlace>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedRecord by remember { mutableStateOf<LocationRecord?>(null) }
    val kakaoMapRef = remember { mutableStateOf<KakaoMap?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val markerLayerRef = remember { mutableStateOf<LabelLayer?>(null) }
    val liveLayerRef = remember { mutableStateOf<LabelLayer?>(null) }
    val liveLabelRef = remember { mutableStateOf<Label?>(null) }
    val points = remember(records) { records.map { LatLng.from(it.latitude, it.longitude) } }
    // Kakao 지도 초기화/인증 실패 사유. 기존엔 Logcat에만 찍혀 사용자는 빈
    // 지도만 보고 원인을 알 수 없었다. 화면에 띄워 키/키해시 등록 문제를
    // 바로 진단할 수 있게 한다.
    var mapError by remember { mutableStateOf<String?>(null) }
    // Bumping this forces the AndroidView/MapView to be recreated from scratch
    // — used by the error overlay's "다시 시도" so a transient init/network
    // failure can recover without restarting the app.
    var mapRetryKey by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> mv.resume()
                Lifecycle.Event.ON_PAUSE -> mv.pause()
                Lifecycle.Event.ON_DESTROY -> mv.finish()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // resume() must be called AFTER the view is attached to the window (not in factory).
    // LaunchedEffect fires after the first composition commit — by then MapView has dimensions.
    LaunchedEffect(mapViewRef.value) {
        mapViewRef.value?.resume()
    }

    // Rebuild route overlays when records (or favorites) change
    LaunchedEffect(points, kakaoMapRef.value, favorites) {
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

        // 구간별 속도로 색을 다르게 칠한다. 걷기로 불가능한 속도이거나 두 점이
        // 멀리 떨어진 점프(지하철은 지하라 GPS가 끊겨 직선 점프가 됨)는
        // 지하철/이동수단으로 보고 파란색, 실제 걸은 구간은 주황색.
        if (records.size >= 2) {
            for (i in 1 until records.size) {
                val a = records[i - 1]
                val b = records[i]
                val seg = listOf(
                    LatLng.from(a.latitude, a.longitude),
                    LatLng.from(b.latitude, b.longitude)
                )
                val dist = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                val dt = (b.timestamp.seconds - a.timestamp.seconds).coerceAtLeast(1L)
                val speedKmh = dist / dt * 3.6
                val transit = speedKmh > 25.0 || dist > 700.0
                val color = if (transit) 0xFF2196F3.toInt() else 0xFFFF8C00.toInt()
                val width = if (transit) 9f else 11f
                val styles = RouteLineStylesSet.from(
                    "seg$i",
                    RouteLineStyles.from(RouteLineStyle.from(width, color))
                )
                map.routeLineManager?.layer?.addRouteLine(
                    RouteLineOptions.from(listOf(RouteLineSegment.from(seg).setStyles(styles.getStyles(0))))
                )
            }
        }

        if (records.isNotEmpty()) {
            val layer = map.labelManager?.addLayer(LabelLayerOptions.from("markers"))
                ?: return@LaunchedEffect
            markerLayerRef.value = layer

            // 건물(머무른 장소)·즐겨찾기만 마커로 표시한다. 도보 경로상의 일반
            // waypoint 점들은 지도를 지저분하게 만들어서 숨기고, 경로는 선으로만
            // 보여준다.
            records.forEach { record ->
                val isFavorite = favoriteNameFor(favorites, record.latitude, record.longitude) != null
                when {
                    isFavorite -> {
                        // 즐겨찾기 위치 → 별(중앙 앵커).
                        layer.addLabel(
                            LabelOptions.from(LatLng.from(record.latitude, record.longitude))
                                .setStyles(LabelStyles.from(
                                    LabelStyle.from(context.starBitmap(26f))
                                        .setAnchorPoint(0.5f, 0.5f)
                                ))
                                .setTag(record)
                        )
                    }
                    record.isPlace -> {
                        // 건물 진입 지점 → 깃발. 깃대 밑동이 좌표에 닿도록.
                        layer.addLabel(
                            LabelOptions.from(LatLng.from(record.latitude, record.longitude))
                                .setStyles(LabelStyles.from(
                                    LabelStyle.from(context.flagBitmap(28f))
                                        .setAnchorPoint(0.22f, 1.0f)
                                ))
                                .setTag(record)
                        )
                    }
                    // 일반 waypoint(도로 위 점)는 마커를 찍지 않는다.
                }
            }

            map.setOnLabelClickListener { _, _, label ->
                (label.tag as? LocationRecord)?.let { selectedRecord = it }
                true
            }
        }

        // 걷는 구간을 도로망에 스냅해 '진짜 걸어다닌 경로'처럼 보이게 한다.
        // 위에서 직선으로 먼저 그려 즉시 피드백을 주고, 잠깐의 디바운스 후
        // (잦은 기록 갱신은 LaunchedEffect 취소로 자연 디바운스됨) 걷기 구간만
        // OSRM에 스냅해 다시 그린다. 지하철/이동수단 구간은 직선 파랑 유지.
        // 스냅이 실패하면 직선으로 폴백하므로 회귀가 없다.
        if (records.size >= 2) {
            delay(700)
            val runs = ArrayList<Pair<Boolean, MutableList<LatLng>>>()
            for (i in 1 until records.size) {
                val a = records[i - 1]
                val b = records[i]
                val dist = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                val dt = (b.timestamp.seconds - a.timestamp.seconds).coerceAtLeast(1L)
                val transit = dist / dt * 3.6 > 25.0 || dist > 700.0
                val pa = LatLng.from(a.latitude, a.longitude)
                val pb = LatLng.from(b.latitude, b.longitude)
                if (runs.isNotEmpty() && runs.last().first == transit) {
                    runs.last().second.add(pb)
                } else {
                    runs.add(transit to mutableListOf(pa, pb))
                }
            }
            // 걷기 구간은 도로망에 스냅, 지하철/이동수단 구간은 지하철 노선으로
            // 라우팅(역들을 따라). 둘 다 실패 시 직선으로 폴백.
            val drawn = runs.map { (transit, pts) ->
                if (transit) {
                    val sub = SubwayRouter.route(
                        pts.first().latitude, pts.first().longitude,
                        pts.last().latitude, pts.last().longitude
                    )
                    if (sub != null) listOf(pts.first()) + sub + listOf(pts.last()) else pts
                } else {
                    RouteSnapper.snapWalking(pts) ?: pts
                }
            }
            map.routeLineManager?.layer?.removeAll()
            runs.forEachIndexed { idx, (transit, _) ->
                val pts = drawn[idx]
                if (pts.size < 2) return@forEachIndexed
                val color = if (transit) 0xFF2196F3.toInt() else 0xFFFF8C00.toInt()
                val width = if (transit) 9f else 11f
                val styles = RouteLineStylesSet.from(
                    "r$idx",
                    RouteLineStyles.from(RouteLineStyle.from(width, color))
                )
                map.routeLineManager?.layer?.addRouteLine(
                    RouteLineOptions.from(listOf(RouteLineSegment.from(pts).setStyles(styles.getStyles(0))))
                )
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
                        // 현재 위치 = 빨간 점. moveTo로 실시간 이동.
                        LabelStyle.from(context.dotBitmap(0xFFFF3B30.toInt(), 24f))
                            .setAnchorPoint(0.5f, 0.5f)
                    ))
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // key(mapRetryKey): recreating the MapView is how "다시 시도" recovers.
        // Each recreation must finish the old view and reset the cached refs so
        // the overlay-rebuild effects re-run against the fresh KakaoMap.
        key(mapRetryKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).also { mv ->
                        mapViewRef.value = mv
                        val initCenter = currentLocation ?: points.lastOrNull()
                            ?: LatLng.from(37.5665, 126.9780)
                        mv.start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {}
                                override fun onMapError(error: Exception) {
                                    android.util.Log.e("KakaoMap", "Map error: ${error.message}", error)
                                    mapError = error.message ?: error.javaClass.simpleName
                                }
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
        }

        // Map authentication / init failure — surfaced so the cause (보통
        // 카카오 네이티브 앱 키 또는 키 해시 미등록)이 화면에 바로 보인다.
        mapError?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface.copy(alpha = 0.95f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "지도를 불러오지 못했어요",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "카카오 앱 키·키 해시 등록을 확인해 주세요",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "다시 시도",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                // Drop the stale map/refs and force a fresh
                                // MapView so a transient failure can recover.
                                kakaoMapRef.value = null
                                markerLayerRef.value = null
                                liveLayerRef.value = null
                                liveLabelRef.value = null
                                mapError = null
                                mapRetryKey++
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // No-records overlay (shown on top of the map)
        if (mapError == null && records.isEmpty() && currentLocation == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    "이 날의 위치 기록이 없어요",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

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

        // "내 위치" button (bottom-right). 좌측 하단은 지도/목록 토글이
        // 차지하므로 우측 하단에 둔다. 현재 위치만 있으면 표시.
        if (currentLocation != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Naver Maps style place info bottom sheet
        var lastSelectedRec by remember { mutableStateOf<LocationRecord?>(null) }
        if (selectedRecord != null) lastSelectedRec = selectedRecord

        AnimatedVisibility(
            visible = selectedRecord != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            lastSelectedRec?.let { rec ->
                val time = remember(rec.timestamp) {
                    java.text.SimpleDateFormat("a h:mm", Locale.KOREAN).format(rec.timestamp.toDate())
                }
                val address = resolvedAddress(rec, favorites)
                val placeName = resolvedPlaceName(rec, favorites)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DarkSurface)
                        .padding(bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 4.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                placeName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                address,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                lineHeight = 17.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { selectedRecord = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "닫기", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("$time 방문", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteListView(records: List<LocationRecord>, favorites: List<FavoritePlace>) {
    // 구간 거리/걸음수 계산. 걸음수는 보폭 0.7m 기준 추정, 빠른 점프(지하철/
    // 이동수단)는 걸음에서 제외하고 거리만 보여준다.
    val totalWalkM = remember(records) {
        var w = 0.0
        for (i in 1 until records.size) {
            val a = records[i - 1]; val b = records[i]
            val d = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            val dt = (b.timestamp.seconds - a.timestamp.seconds).coerceAtLeast(1L)
            if (!(d / dt * 3.6 > 25.0 || d > 700.0)) w += d
        }
        w
    }
    val totalSteps = (totalWalkM / 0.7).roundToInt()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ListStat(label = "걸음수", value = "약 ${"%,d".format(totalSteps)}")
                ListStat(label = "걸은 거리", value = formatDistance(totalWalkM))
            }
        }
        itemsIndexed(records, key = { _, it -> it.id }) { index, record ->
            val isLast = index == records.lastIndex
            val time = remember(record.timestamp) {
                java.text.SimpleDateFormat("HH:mm", Locale.KOREAN).format(record.timestamp.toDate())
            }
            val address = resolvedAddress(record, favorites)
            // 이전 지점에서 여기까지의 거리/걸음수.
            val legText = remember(records, index) {
                if (index == 0) null else {
                    val a = records[index - 1]
                    val d = distanceMeters(a.latitude, a.longitude, record.latitude, record.longitude)
                    val dt = (record.timestamp.seconds - a.timestamp.seconds).coerceAtLeast(1L)
                    val transit = d / dt * 3.6 > 25.0 || d > 700.0
                    if (transit) "🚇 ${formatDistance(d)} 이동"
                    else "🚶 ${formatDistance(d)} · 약 ${"%,d".format((d / 0.7).roundToInt())}걸음"
                }
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
                                .height(64.dp)
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
                    if (legText != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(legText, color = TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

private fun formatDistance(m: Double): String =
    if (m >= 1000) "%.1fkm".format(m / 1000) else "${m.roundToInt()}m"

@Composable
private fun ListStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextTertiary, fontSize = 12.sp)
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
