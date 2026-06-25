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
    // 역과 이 거리 이내여야 '그 역'으로 간주(route()/일반 스냅 기본값).
    private const val MAX_SNAP_M = 700.0
    // 출발역·도착역: 양 끝 점은 출입구에서 다소 떨어져도 잡히도록 넉넉히.
    private const val ENDPOINT_SNAP_M = 1200.0
    // 중간 경유역: 확실히 그 역 근처를 지난 점만 채택(노이즈 배제)하도록 타이트하게.
    private const val MID_SNAP_M = 500.0
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

    /** lat,lng에서 maxM 이내 가장 가까운 역 인덱스. 없으면 null. */
    private fun nearestWithin(lat: Double, lng: Double, maxM: Double): Int? {
        var best = -1
        var bestD = maxM
        stations.forEachIndexed { i, s ->
            val d = dist(lat, lng, s.lat, s.lng)
            if (d <= bestD) { bestD = d; best = i }
        }
        return if (best >= 0) best else null
    }

    private fun nearest(lat: Double, lng: Double): Int? = nearestWithin(lat, lng, MAX_SNAP_M)

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
     * 출발역·도착역과, 이동 중 '확실히 지난' 중간 역들을 토대로 지하철 경로의
     * 위치값을 역 좌표로 다시 잡는다(노이즈 GPS를 그대로 그리지 않음).
     *
     *  - 출발/도착역: 양 끝 점에서 가장 가까운 역(넉넉한 반경). 못 찾으면 비대상.
     *  - 중간 경유역: 각 중간 점에서 '타이트한 반경' 안의 역만 채택 → 노이즈로
     *    멀리 튄 점은 무시되어 '안 간 역'으로 새지 않는다.
     *  - 출발 → 중간역들 → 도착을 노선 최단경로로 잇고, 곧장 되돌아가는(역주행)
     *    경유지는 건너뛴다.
     *
     * 실패/비대상이면 null → 호출부에서 직선으로 폴백.
     */
    suspend fun routeVia(points: List<LatLng>): List<LatLng>? {
        ensureLoaded()
        if (!loaded || points.size < 2) return null

        // 출발역·도착역(넉넉히).
        val dep = nearestWithin(points.first().latitude, points.first().longitude, ENDPOINT_SNAP_M)
            ?: return null
        val arr = nearestWithin(points.last().latitude, points.last().longitude, ENDPOINT_SNAP_M)
            ?: return null
        if (dep == arr) return null

        // 경유역: 출발 → (확실히 지난 중간 역) → 도착. 중간점은 타이트 스냅으로
        // 노이즈 배제, 연속 중복 제거.
        val seq = ArrayList<Int>()
        seq.add(dep)
        for (k in 1 until points.size - 1) {
            val st = nearestWithin(points[k].latitude, points[k].longitude, MID_SNAP_M) ?: continue
            if (seq.last() != st) seq.add(st)
        }
        if (seq.last() != arr) seq.add(arr)
        if (seq.size < 2) return null

        // 경유역 중 앞뒤 역 대비 크게 우회하는(노선에서 벗어나게 잘못 스냅된)
        // 중간역은 제거 → 지그재그/스퍼/루프 방지. 출발·도착역은 보존.
        var pruned = true
        while (pruned && seq.size > 2) {
            pruned = false
            var k = 1
            while (k < seq.size - 1) {
                val a = stations[seq[k - 1]]
                val b = stations[seq[k]]
                val c = stations[seq[k + 1]]
                val via = dist(a.lat, a.lng, b.lat, b.lng) + dist(b.lat, b.lng, c.lat, c.lng)
                val direct = dist(a.lat, a.lng, c.lat, c.lng)
                if (via > direct + 1200.0) { seq.removeAt(k); pruned = true } else k++
            }
        }

        // 경유역들을 노선 최단경로로 이어 붙이되, 직전 진행 방향으로 곧장 되돌아가는
        // (역주행) 경유지는 잘못 스냅된 점으로 보고 건너뛴다 → '안 간 역'으로 새는 것 방지.
        val merged = ArrayList<Int>()
        for (st in seq) {
            if (merged.isEmpty()) { merged.add(st); continue }
            if (st == merged.last()) continue
            val leg = shortestPath(merged.last(), st) ?: continue
            if (leg.size < 2) continue
            if (merged.size >= 2 && leg[1] == merged[merged.size - 2]) continue
            merged.addAll(leg.drop(1))
        }
        val result = merged.map { LatLng.from(stations[it].lat, stations[it].lng) }
        return if (result.size >= 2) result else null
    }

    /** 한 이동 구간(승차 지점~하차 지점)의 지하철 정보: 노선·승차역·하차역·역 수. */
    data class LegInfo(val line: String, val from: String, val to: String, val stops: Int)

    /** 두 점(승차~하차)을 잇는 지하철 구간 요약. 지하철 구간이 아니면 null. */
    suspend fun describeLeg(start: LatLng, end: LatLng): LegInfo? {
        ensureLoaded()
        if (!loaded) return null
        val dep = nearestWithin(start.latitude, start.longitude, ENDPOINT_SNAP_M) ?: return null
        val arr = nearestWithin(end.latitude, end.longitude, ENDPOINT_SNAP_M) ?: return null
        if (dep == arr) return null
        val path = shortestPath(dep, arr) ?: return null
        if (path.size < 2) return null
        // 경로에서 가장 많이 등장한 노선을 대표 노선으로(환승 시 주 노선).
        val line = path.map { stations[it].line }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: stations[dep].line
        return LegInfo(line = line, from = stations[dep].name, to = stations[arr].name, stops = path.size - 1)
    }
}
