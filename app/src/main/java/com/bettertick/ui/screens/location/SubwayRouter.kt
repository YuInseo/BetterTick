package com.bettertick.ui.screens.location

import android.util.Log
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.PriorityQueue

/**
 * 지하철 구간을 직선이 아니라 실제 노선(역들을 따라)으로 그리기 위한 라우터.
 *
 * 공개 데이터셋(서울 지하철 역 좌표 + 노선번호, CSV: lat,lon,name,no_line)을
 * 런타임에 한 번 받아 그래프를 만든다. 같은 노선의 연속된 역은 인접 간선,
 * 같은 이름의 역은 환승 간선으로 연결한 뒤 다익스트라로 최단 경로를 찾는다.
 *
 * 데이터/네트워크 실패 시 route()는 null을 반환 → 호출부에서 직선으로 폴백.
 */
object SubwayRouter {
    private const val TAG = "SubwayRouter"
    private const val DATA_URL =
        "https://gist.githubusercontent.com/yoon-gu/902efb6d5bd345e3837e035a3c0642b8/raw/station_latlen.csv"
    private const val MAX_SNAP_M = 1300.0  // 역과 이 거리 이내여야 지하철로 간주
    // 환승(같은 이름 역) 간선 가중치. 너무 작으면(예: 1m) 환승이 거의 공짜라
    // 그래프상 최단거리만 좇아 실제로 안 탄 노선으로 갈아타며 엉뚱하게 돌아간다.
    // 환승에 ~500m 상당 패널티를 줘 한 노선을 유지하는 경로를 선호하게 한다.
    private const val TRANSFER_W = 500.0

    private class Station(val name: String, val line: String, val lat: Double, val lng: Double)

    private var stations: List<Station> = emptyList()
    private var adj: Map<Int, List<Pair<Int, Double>>> = emptyMap()
    @Volatile private var loaded = false
    @Volatile private var failed = false
    private val mutex = Mutex()

    private fun dist(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val out = FloatArray(1)
        android.location.Location.distanceBetween(aLat, aLng, bLat, bLng, out)
        return out[0].toDouble()
    }

    private suspend fun ensureLoaded() {
        if (loaded || failed) return
        mutex.withLock {
            if (loaded || failed) return
            try {
                val text = withContext(Dispatchers.IO) {
                    val conn = (URL(DATA_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 10000
                        setRequestProperty("User-Agent", "BetterTick-SubwayRouter")
                    }
                    if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
                build(text)
                loaded = stations.isNotEmpty()
                if (!loaded) failed = true
            } catch (e: Exception) {
                Log.w(TAG, "subway data load failed", e)
                failed = true
            }
        }
    }

    private fun build(text: String) {
        val list = ArrayList<Station>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val p = line.split(",")
            if (p.size < 4) return@forEach
            val lat = p[0].toDoubleOrNull() ?: return@forEach   // 헤더/잘못된 줄 건너뜀
            val lng = p[1].toDoubleOrNull() ?: return@forEach
            list.add(Station(p[2].trim(), p[3].trim(), lat, lng))
        }
        val a = HashMap<Int, MutableList<Pair<Int, Double>>>()
        fun edge(x: Int, y: Int, w: Double) {
            a.getOrPut(x) { mutableListOf() }.add(y to w)
            a.getOrPut(y) { mutableListOf() }.add(x to w)
        }
        // 같은 노선의 연속된 역 = 인접(파일 순서가 역 순서)
        for (i in 1 until list.size) {
            if (list[i - 1].line == list[i].line) {
                edge(i - 1, i, dist(list[i - 1].lat, list[i - 1].lng, list[i].lat, list[i].lng))
            }
        }
        // 같은 이름 = 환승역으로 연결
        val byName = HashMap<String, MutableList<Int>>()
        list.forEachIndexed { i, s -> byName.getOrPut(s.name) { mutableListOf() }.add(i) }
        byName.values.forEach { idxs ->
            for (i in idxs.indices) for (j in i + 1 until idxs.size) edge(idxs[i], idxs[j], TRANSFER_W)
        }
        stations = list
        adj = a
    }

    private fun nearest(lat: Double, lng: Double): Int? {
        var best = -1
        var bestD = MAX_SNAP_M
        stations.forEachIndexed { i, s ->
            val d = dist(lat, lng, s.lat, s.lng)
            if (d <= bestD) { bestD = d; best = i }
        }
        return if (best >= 0) best else null
    }

    /** 두 역 인덱스 사이 최단 역경로(인덱스열). 없으면 null. */
    private fun shortestPath(s: Int, e: Int): List<Int>? {
        if (s == e) return listOf(s)
        val n = stations.size
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
        val path = ArrayList<Int>()
        var cur = e
        while (cur != -1) { path.add(cur); cur = prev[cur] }
        path.reverse()
        return path
    }

    /** 두 점을 잇는 지하철 노선 경로(역 좌표열). 실패/비대상이면 null. */
    suspend fun route(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): List<LatLng>? {
        ensureLoaded()
        if (!loaded) return null
        val s = nearest(startLat, startLng) ?: return null
        val e = nearest(endLat, endLng) ?: return null
        if (s == e) return null
        val path = shortestPath(s, e) ?: return null
        val result = path.map { LatLng.from(stations[it].lat, stations[it].lng) }
        return if (result.size >= 2) result else null
    }

    /**
     * 이동 중 기록된 GPS 점들을 '경유지'로 삼아 실제로 지나간 역들을 따라가는
     * 지하철 경로를 만든다. 시작·끝 두 점만 보면 그래프상 최단경로(엉뚱한 노선/
     * 환승)를 골라 실제 탄 노선과 달라지므로, 중간 점들을 각각 가까운 역에 스냅한
     * 뒤 연속한 역들끼리 최단경로를 이어 붙인다. 점이 끝점뿐이면 route()와 동일.
     *
     * 실패/비대상이면 null → 호출부에서 직선으로 폴백.
     */
    suspend fun routeVia(points: List<LatLng>): List<LatLng>? {
        ensureLoaded()
        if (!loaded || points.size < 2) return null
        // 각 GPS 점을 가장 가까운 역에 스냅하고 연속 중복은 제거.
        val seq = ArrayList<Int>()
        points.forEach { p ->
            val st = nearest(p.latitude, p.longitude) ?: return@forEach
            if (seq.isEmpty() || seq.last() != st) seq.add(st)
        }
        if (seq.size < 2) return null
        // 연속한 스냅 역들 사이를 최단경로로 이어 붙인다.
        val merged = ArrayList<Int>()
        for (i in 1 until seq.size) {
            val leg = shortestPath(seq[i - 1], seq[i]) ?: continue
            if (merged.isEmpty()) merged.addAll(leg) else merged.addAll(leg.drop(1))
        }
        val result = merged.map { LatLng.from(stations[it].lat, stations[it].lng) }
        return if (result.size >= 2) result else null
    }
}
