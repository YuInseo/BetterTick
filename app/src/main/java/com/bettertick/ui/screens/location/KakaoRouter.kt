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
 * 주어진 경유지(waypoints)를 순서대로 지나는 도로 경로 geometry를 Kakao Mobility
 * 길찾기(자동차) REST API로 받아온다. 지하철 노선은 서울 도심에서 대체로
 * 간선도로 아래를 지나므로, 라우터가 찾은 역 좌표열을 도로망에 라우팅하면
 * 역-역 사이를 직선으로 잇는 것보다 실제 노선에 훨씬 가깝게 그려진다.
 *
 * 실패(키 미설정/네트워크/무경로)하면 null을 반환 → 호출부에서 역 좌표 직선으로
 * 폴백하므로 회귀가 없다.
 *
 * 주의: 여기 쓰는 키는 지도 SDK 초기화에 쓰는 '네이티브 앱 키'가 아니라 같은
 * 카카오 앱의 'REST API 키'다(카카오 디벨로퍼스 > 내 앱 > 앱 키 > REST API 키).
 */
object KakaoRouter {
    private const val TAG = "KakaoRouter"

    // 카카오 디벨로퍼스 REST API 키(지도 SDK 네이티브 앱 키와 다름).
    // 미설정이면 라우팅을 건너뛰고 호출부가 직선으로 폴백한다.
    private const val REST_API_KEY = "1b06baf1d1cddc028d1bc1edcdffaf23"

    // 다중 경유지 길찾기(GET)의 경유지 상한. origin/destination을 제외한 중간점 기준.
    private const val MAX_WAYPOINTS = 28

    suspend fun routeRoad(waypoints: List<LatLng>): List<LatLng>? = withContext(Dispatchers.IO) {
        if (waypoints.size < 2) return@withContext null
        if (REST_API_KEY.startsWith("<")) return@withContext null  // 키 미설정 → 폴백

        try {
            // 경유지가 너무 많으면 처음/끝은 유지하며 균등 다운샘플.
            val pts = if (waypoints.size > MAX_WAYPOINTS + 2) {
                val step = waypoints.size / (MAX_WAYPOINTS + 2) + 1
                waypoints.filterIndexed { i, _ -> i % step == 0 || i == waypoints.lastIndex }
            } else waypoints

            // Kakao는 좌표를 'x(경도),y(위도)' 순으로 받는다.
            val origin = "${pts.first().longitude},${pts.first().latitude}"
            val dest = "${pts.last().longitude},${pts.last().latitude}"
            val mids = if (pts.size > 2) pts.subList(1, pts.size - 1) else emptyList()

            val sb = StringBuilder("https://apis-navi.kakaomobility.com/v1/directions")
            sb.append("?origin=").append(origin)
            sb.append("&destination=").append(dest)
            if (mids.isNotEmpty()) {
                val wp = mids.joinToString("|") { "${it.longitude},${it.latitude}" }
                sb.append("&waypoints=").append(URLEncoder.encode(wp, "UTF-8"))
            }
            sb.append("&priority=RECOMMEND")

            val conn = (URL(sb.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "KakaoAK $REST_API_KEY")
                setRequestProperty("User-Agent", "BetterTick-KakaoRouter")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val routes = json.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null
            val route0 = routes.getJSONObject(0)
            if (route0.optInt("result_code", -1) != 0) return@withContext null  // 0 == 성공

            // sections[].roads[].vertexes = [x1,y1,x2,y2,...] (x=경도, y=위도)
            val sections = route0.optJSONArray("sections") ?: return@withContext null
            val result = ArrayList<LatLng>()
            for (s in 0 until sections.length()) {
                val roads = sections.getJSONObject(s).optJSONArray("roads") ?: continue
                for (r in 0 until roads.length()) {
                    val vtx = roads.getJSONObject(r).optJSONArray("vertexes") ?: continue
                    var i = 0
                    while (i + 1 < vtx.length()) {
                        result.add(LatLng.from(vtx.getDouble(i + 1), vtx.getDouble(i)))
                        i += 2
                    }
                }
            }
            if (result.size >= 2) result else null
        } catch (e: Exception) {
            Log.w(TAG, "kakao route failed", e)
            null
        }
    }
}
