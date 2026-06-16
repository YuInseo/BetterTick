package com.bettertick.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.bettertick.data.model.LocationRecord
import com.bettertick.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var locationRepository: LocationRepository

    private lateinit var fusedClient: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastLat = 0.0
    private var lastLng = 0.0

    // Dwell(머무름) 추적 — 같은 지점 근처에 오래 있으면 "건물 진입"으로 보고
    // 깃발을 한 번 꽂는다.
    private var dwellLat = 0.0
    private var dwellLng = 0.0
    private var dwellStartMs = 0L
    private var dwellFlagged = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val lat = loc.latitude
            val lng = loc.longitude
            val now = System.currentTimeMillis()

            // 1) 경로 waypoint — 충분히 이동했을 때만 기록(선 그리기용).
            if (!isSamePlace(lat, lng)) {
                lastLat = lat; lastLng = lng
                scope.launch { saveLocation(lat, lng, isPlace = false) }
            }

            // 2) 건물 진입(dwell) 감지 — 같은 지점에 일정 시간 머물면 깃발 1개.
            if (dwellStartMs == 0L || distanceMeters(lat, lng, dwellLat, dwellLng) > DWELL_RADIUS_M) {
                // 새 지점으로 이동 → dwell 앵커 재설정.
                dwellLat = lat; dwellLng = lng
                dwellStartMs = now
                dwellFlagged = false
            } else if (!dwellFlagged && now - dwellStartMs >= DWELL_MIN_MS) {
                dwellFlagged = true
                val placeLat = dwellLat; val placeLng = dwellLng
                scope.launch { saveLocation(placeLat, placeLng, isPlace = true) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundCompat()
        startTracking()
    }

    @Suppress("MissingPermission")
    private fun startTracking() {
        // 15초 주기 + 최소거리 0 → 정지해 있어도 업데이트가 자주 와서 dwell
        // (머무름)을 빠르게 감지. 저장 자체는 콜백에서 이동/머무름 조건으로 거른다.
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15 * 1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        runCatching {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val out = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, out)
        return out[0].toDouble()
    }

    private fun isSamePlace(lat: Double, lng: Double): Boolean {
        if (lastLat == 0.0 && lastLng == 0.0) return false
        return abs(lat - lastLat) < 0.002 && abs(lng - lastLng) < 0.002
    }

    private suspend fun saveLocation(lat: Double, lng: Double, isPlace: Boolean) {
        val address = reverseGeocode(lat, lng)
        val placeName = if (isPlace) reverseGeocodePlaceName(lat, lng) else ""
        val today = LocalDate.now().toString()
        locationRepository.addRecord(
            LocationRecord(
                dateStr = today,
                timestamp = Timestamp(Date()),
                latitude = lat,
                longitude = lng,
                address = address,
                isPlace = isPlace,
                placeName = placeName
            )
        )
    }

    /** 건물/POI 이름(featureName) 우선으로 장소명을 해석. 깃발 라벨용. */
    private suspend fun reverseGeocodePlaceName(lat: Double, lng: Double): String {
        if (!Geocoder.isPresent()) return "방문한 장소"
        val geocoder = Geocoder(applicationContext, Locale.KOREAN)
        val addr: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                try {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
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
                runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
            }
        }
        val feature = addr?.featureName
        return when {
            feature != null && feature.isNotBlank() && !feature.all { it.isDigit() } -> feature
            addr?.thoroughfare != null -> addr.thoroughfare!!
            addr?.subLocality != null -> addr.subLocality!!
            else -> "방문한 장소"
        }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String {
        val fallback = "%.4f, %.4f".format(lat, lng)
        if (!Geocoder.isPresent()) return fallback
        val geocoder = Geocoder(applicationContext, Locale.KOREAN)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                try {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull()?.getAddressLine(0) ?: fallback)
                        }
                        override fun onError(errorMessage: String?) {
                            cont.resume(fallback)
                        }
                    })
                } catch (e: Exception) {
                    cont.resume(fallback)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
                }.getOrNull() ?: fallback
            }
        }
    }

    private fun startForegroundCompat() {
        val channelId = "bettertick_location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "위치 기록", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("BetterTick 위치 기록 중")
                .setContentText("오늘 방문한 장소를 자동으로 기록합니다")
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("BetterTick 위치 기록 중")
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(9904, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(9904, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { fusedClient.removeLocationUpdates(locationCallback) }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // 이 반경(m) 안에 머물면 같은 장소로 간주.
        private const val DWELL_RADIUS_M = 45.0
        // 같은 장소에 이만큼(ms) 이상 머물면 "건물 진입"으로 보고 깃발.
        // 거의 실시간에 가깝게 1분으로. (짧으면 신호대기 등 오탐이 늘 수 있음)
        private const val DWELL_MIN_MS = 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}
