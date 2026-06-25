package com.bettertick.ui.screens.location

import android.util.Log
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * GPS 기록만으로 '어떤 버스를 탔는지'를 추정한다(실험적).
 *
 * 우리는 버스 번호를 직접 알 수 없으므로, OpenStreetMap의 버스 노선 관계
 * (relation[route=bus])를 Overpass API(키 불필요)로 조회한다. 승차 지점 근처를
 * 지나는 노선들과 하차 지점 근처를 지나는 노선들의 '교집합'을 후보 버스로 본다.
 * (두 지점을 모두 지나는 노선 = 탔을 가능성이 높은 버스)
 *
 * OSM 커버리지·정확도에 따라 틀릴 수 있어 'N번 추정'으로 표기한다.
 * 실패/후보 없음이면 null → 호출부에서 '이동'으로 폴백.
 */
object BusRouter {
    private const val TAG = "BusRouter"
    // 공개 Overpass 인스턴스(키 불필요). 과부하 시 다른 미러로 바꿀 수 있음.
    private const val OVERPASS = "https://overpass-api.de/api/interpreter"
    private const val SNAP_M = 250  // 정류장/노선과 이 반경 안이면 그 지점을 지난 걸로.

    suspend fun describeBusLeg(start: LatLng, end: LatLng): String? = withContext(Dispatchers.IO) {
        try {
            val a = refsNear(start.latitude, start.longitude)
            if (a.isEmpty()) return@withContext null
            val b = refsNear(end.latitude, end.longitude)
            if (b.isEmpty()) return@withContext null
            val common = a.intersect(b).filter { it.isNotBlank() }.distinct()
            if (common.isEmpty()) return@withContext null
            // 후보가 여러 개면 최대 3개까지만(번호순).
            common.sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
                .take(3).joinToString("·")
        } catch (e: Exception) {
            Log.w(TAG, "describeBusLeg failed", e)
            null
        }
    }

    /** 좌표 근처(SNAP_M)를 지나는 버스 노선들의 번호(ref) 집합. */
    private fun refsNear(lat: Double, lng: Double): Set<String> {
        val q = "[out:json][timeout:25];rel(around:$SNAP_M,$lat,$lng)[route=bus];out tags;"
        val url = "$OVERPASS?data=" + URLEncoder.encode(q, "UTF-8")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            setRequestProperty("User-Agent", "BetterTick-BusRouter")
        }
        if (conn.responseCode !in 200..299) return emptySet()
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val els = JSONObject(text).optJSONArray("elements") ?: return emptySet()
        val refs = HashSet<String>()
        for (i in 0 until els.length()) {
            val tags = els.getJSONObject(i).optJSONObject("tags") ?: continue
            val ref = tags.optString("ref").ifBlank { tags.optString("name") }
            if (ref.isNotBlank()) refs.add(ref.trim())
        }
        return refs
    }
}
