package com.bettertick.ui.screens.location

import android.util.Log
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GPS 점들을 도로망에 맞춰(map matching) 실제 걸은 경로처럼 보이게 스냅한다.
 * 공개 OSRM 데모 서버를 쓰며 API 키가 필요 없다. 실패하면 null을 반환해
 * 호출부에서 원래 직선으로 폴백하도록 한다(회귀 없음).
 *
 * 데모 서버는 driving 프로파일만 제공하지만, 도심 보행 경로를 거리망에
 * 스냅하는 용도로는 직선보다 훨씬 사실적이다.
 */
object RouteSnapper {
    private const val TAG = "RouteSnapper"
    private const val MAX_COORDS = 90  // OSRM /match 좌표 수 제한 여유

    suspend fun snapWalking(points: List<LatLng>): List<LatLng>? = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext null
        try {
            // 좌표가 너무 많으면 균등 다운샘플.
            val pts = if (points.size > MAX_COORDS) {
                val step = points.size / MAX_COORDS + 1
                points.filterIndexed { i, _ -> i % step == 0 || i == points.lastIndex }
            } else points

            val coordStr = pts.joinToString(";") { "${it.longitude},${it.latitude}" }
            val url = "https://router.project-osrm.org/match/v1/driving/$coordStr" +
                "?geometries=geojson&overview=full&tidy=true"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "BetterTick-RouteSnapper")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") return@withContext null

            val matchings = json.optJSONArray("matchings") ?: return@withContext null
            val result = ArrayList<LatLng>()
            for (m in 0 until matchings.length()) {
                val coords = matchings.getJSONObject(m)
                    .optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                for (c in 0 until coords.length()) {
                    val pair = coords.getJSONArray(c)
                    // GeoJSON은 [lon, lat] 순서.
                    result.add(LatLng.from(pair.getDouble(1), pair.getDouble(0)))
                }
            }
            if (result.size >= 2) result else null
        } catch (e: Exception) {
            Log.w(TAG, "snap failed", e)
            null
        }
    }
}
