package com.bettertick.ui.screens.location

import android.util.Log
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.PriorityQueue

private fun dist(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
    val out = FloatArray(1)
    android.location.Location.distanceBetween(aLat, aLng, bLat, bLng, out)
    return out[0].toDouble()
}

/**
 * 역 좌표열(SubwayRouter 결과)을 '실제 선로 모양(커브)'을 따라가게 다시 그린다.
 *
 * SubwayRouter는 역 좌표만 알기에 인접 역끼리는 직선으로 이어진다. 그래서 신사역
 * →압구정역처럼 선로가 휘는 구간이 건물 사이를 가로지르는 직선으로 보인다.
 * 여기서는 OpenStreetMap의 실제 지하철 선로(way[railway=subway])를 Overpass
 * API(키 불필요)로 받아 선로망 그래프를 만들고, 인접 역쌍마다 그 선로 위 최단
 * 경로를 따라 폴리라인을 뽑아 실제 노선의 곡선을 그대로 따라가게 한다.
 *
 * 네트워크/데이터 실패 시 follow()는 null → 호출부에서 기존 직선(스무딩)으로 폴백.
 */
object SubwayTrackRouter {
    private const val TAG = "SubwayTrackRouter"
    // 공개 Overpass 인스턴스(키 불필요). 버스 추정과 동일.
    private const val OVERPASS = "https://overpass-api.de/api/interpreter"
    // 역 좌표를 선로에 붙일 최대 거리(역 출입구~선로 거리 감안).
    private const val SNAP_M = 400.0
    // 선로 노드 좌표 키 양자화(소수 5자리 ≈ 1.1m). 맞닿은 way를 한 노드로 잇는다.
    private const val GRID = 1e5
    // 경로 전체 직선 길이가 이보다 길면(역 데이터 오스냅 등) 선로 추종을 건너뜀.
    private const val MAX_SPAN_M = 40_000.0

    /**
     * 역 좌표열을 실제 선로를 따라가는 좌표열로 변환. 인접 역쌍 중 선로 최단경로를
     * 못 찾은 구간만 직선으로 잇는다. 선로를 하나도 못 따라가면 null.
     */
    suspend fun follow(stationPath: List<LatLng>): List<LatLng>? = withContext(Dispatchers.IO) {
        if (stationPath.size < 2) return@withContext null
        try {
            val graph = fetchGraph(stationPath) ?: return@withContext null
            val out = ArrayList<LatLng>()
            var followed = false
            for (i in 1 until stationPath.size) {
                val seg = graph.path(stationPath[i - 1], stationPath[i])
                if (seg != null && seg.size >= 2) {
                    followed = true
                    if (out.isEmpty()) out.addAll(seg) else out.addAll(seg.drop(1))
                } else {
                    // 이 구간만 직선 폴백.
                    if (out.isEmpty()) out.add(stationPath[i - 1])
                    out.add(stationPath[i])
                }
            }
            if (followed && out.size >= 2) out else null
        } catch (e: Exception) {
            Log.w(TAG, "follow failed", e)
            null
        }
    }

    /** stationPath를 감싸는 bbox의 지하철 선로를 받아 선로망 그래프를 만든다. */
    private fun fetchGraph(stationPath: List<LatLng>): Graph? {
        var minLat = Double.MAX_VALUE; var minLng = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE; var maxLng = -Double.MAX_VALUE
        for (p in stationPath) {
            minLat = minOf(minLat, p.latitude); maxLat = maxOf(maxLat, p.latitude)
            minLng = minOf(minLng, p.longitude); maxLng = maxOf(maxLng, p.longitude)
        }
        // 경로가 비정상적으로 넓으면(잘못 스냅된 역 등) 추종을 포기.
        if (dist(minLat, minLng, maxLat, maxLng) > MAX_SPAN_M) return null
        // bbox에 ~400m 여유(선로가 역 좌표 밖으로 휘는 만큼).
        val padLat = 0.004
        val padLng = 0.005
        val south = minLat - padLat; val north = maxLat + padLat
        val west = minLng - padLng; val east = maxLng + padLng

        val q = "[out:json][timeout:25];" +
            "way[\"railway\"~\"^(subway|light_rail)$\"]($south,$west,$north,$east);" +
            "out geom;"
        val url = "$OVERPASS?data=" + URLEncoder.encode(q, "UTF-8")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 20000
            setRequestProperty("User-Agent", "BetterTick-SubwayTrackRouter")
        }
        if (conn.responseCode !in 200..299) return null
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val els = JSONObject(text).optJSONArray("elements") ?: return null

        val index = HashMap<String, Int>()
        val pts = ArrayList<DoubleArray>()
        val adj = HashMap<Int, MutableList<Pair<Int, Double>>>()
        fun nodeId(lat: Double, lng: Double): Int {
            val key = "${Math.round(lat * GRID)},${Math.round(lng * GRID)}"
            return index.getOrPut(key) { pts.add(doubleArrayOf(lat, lng)); pts.size - 1 }
        }
        fun edge(x: Int, y: Int) {
            if (x == y) return
            val w = dist(pts[x][0], pts[x][1], pts[y][0], pts[y][1])
            adj.getOrPut(x) { mutableListOf() }.add(y to w)
            adj.getOrPut(y) { mutableListOf() }.add(x to w)
        }
        for (i in 0 until els.length()) {
            val el = els.getJSONObject(i)
            if (el.optString("type") != "way") continue
            val geom = el.optJSONArray("geometry") ?: continue
            var prev = -1
            for (g in 0 until geom.length()) {
                val n = geom.getJSONObject(g)
                val id = nodeId(n.getDouble("lat"), n.getDouble("lon"))
                if (prev >= 0) edge(prev, id)
                prev = id
            }
        }
        if (pts.size < 2) return null
        return Graph(pts, adj)
    }

    private class Graph(
        val pts: List<DoubleArray>,
        val adj: Map<Int, List<Pair<Int, Double>>>
    ) {
        private fun nearest(p: LatLng, maxM: Double): Int? {
            var best = -1; var bestD = maxM
            for (i in pts.indices) {
                val d = dist(pts[i][0], pts[i][1], p.latitude, p.longitude)
                if (d <= bestD) { bestD = d; best = i }
            }
            return if (best >= 0) best else null
        }

        /** 두 점을 선로 위 최단경로로 잇는 좌표열. 못 찾으면 null. */
        fun path(a: LatLng, b: LatLng): List<LatLng>? {
            val s = nearest(a, SNAP_M) ?: return null
            val e = nearest(b, SNAP_M) ?: return null
            if (s == e) return null
            val n = pts.size
            val best = DoubleArray(n) { Double.MAX_VALUE }
            val prev = IntArray(n) { -1 }
            best[s] = 0.0
            val pq = PriorityQueue<Pair<Int, Double>>(compareBy { it.second })
            pq.add(s to 0.0)
            while (pq.isNotEmpty()) {
                val (u, du) = pq.poll()
                if (du > best[u]) continue
                if (u == e) break
                adj[u]?.forEach { (v, w) ->
                    val nd = du + w
                    if (nd < best[v]) { best[v] = nd; prev[v] = u; pq.add(v to nd) }
                }
            }
            if (best[e] == Double.MAX_VALUE) return null
            val idx = ArrayList<Int>()
            var cur = e
            while (cur != -1) { idx.add(cur); cur = prev[cur] }
            idx.reverse()
            return idx.map { LatLng.from(pts[it][0], pts[it][1]) }
        }
    }
}
