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
    // 보간/노이즈로 안 간 역까지 경로가 뻗는 걸 막기 위해, 양 끝은 '실제 GPS가
    // 이 거리 안에 있는 역'까지만 남긴다(역 좌표점은 보통 역 중심이라 다소 넉넉히).
    private const val NEAR_VISIT_M = 300.0
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
        // GPS 튐(스파이크) 제거: 앞뒤 점과 모두 멀지만 앞뒤끼리는 가까운 글리치
        // 점은 버린다 → 엉뚱한 먼 역에 스냅돼 '안 간 역'으로 뚝 떨어지는 것 방지.
        val pts = if (points.size >= 3) {
            val out = ArrayList<LatLng>(points.size)
            out.add(points.first())
            for (i in 1 until points.size - 1) {
                val a = points[i - 1]; val p = points[i]; val b = points[i + 1]
                val dpa = dist(a.latitude, a.longitude, p.latitude, p.longitude)
                val dpb = dist(p.latitude, p.longitude, b.latitude, b.longitude)
                val dab = dist(a.latitude, a.longitude, b.latitude, b.longitude)
                if (dpa > 1000.0 && dpb > 1000.0 && dab < (dpa + dpb) * 0.5) continue
                out.add(p)
            }
            out.add(points.last())
            out
        } else points

        // 각 GPS 점을 가장 가까운 역에 스냅하고, 연속 중복은 합치며 점 개수를 센다.
        val seq = ArrayList<Int>()
        val cnt = ArrayList<Int>()
        pts.forEach { p ->
            val st = nearest(p.latitude, p.longitude) ?: return@forEach
            if (seq.isEmpty() || seq.last() != st) { seq.add(st); cnt.add(1) }
            else cnt[cnt.lastIndex]++
        }
        // 한 점만 받쳐주는(노이즈) 경유지가 앞뒤 역 대비 크게 우회하면 — 즉 잘못
        // 스냅돼 '안 간 역'으로 뻗었다 돌아오면 — 제거한다. (A-B-A 되돌아오기 포함)
        var changed = true
        while (changed && seq.size > 2) {
            changed = false
            var i = 1
            while (i < seq.size - 1) {
                val a = stations[seq[i - 1]]; val b = stations[seq[i]]; val c = stations[seq[i + 1]]
                val viaB = dist(a.lat, a.lng, b.lat, b.lng) + dist(b.lat, b.lng, c.lat, c.lng)
                val direct = dist(a.lat, a.lng, c.lat, c.lng)
                if (cnt[i] <= 1 && viaB > direct + 1500.0) {
                    cnt[i - 1] += cnt[i]
                    seq.removeAt(i); cnt.removeAt(i)
                    // 제거 후 양옆이 같은 역이 되면(A-B-A) 중복도 합쳐 없앤다.
                    if (i < seq.size && seq[i - 1] == seq[i]) {
                        cnt[i - 1] += cnt[i]; seq.removeAt(i); cnt.removeAt(i)
                    }
                    changed = true
                } else i++
            }
        }
        if (seq.size < 2) return null
        // 연속한 스냅 역들 사이를 최단경로로 이어 붙이되, 직전 진행 방향으로 곧장
        // 되돌아가는(역주행) 경유지는 잘못 스냅된 점으로 보고 건너뛴다 → 안 간
        // 역까지 길이 뻗는 것을 막는다.
        val merged = ArrayList<Int>()
        for (st in seq) {
            if (merged.isEmpty()) { merged.add(st); continue }
            val leg = shortestPath(merged.last(), st) ?: continue
            if (leg.size < 2) continue
            if (merged.size >= 2 && leg[1] == merged[merged.size - 2]) continue
            merged.addAll(leg.drop(1))
        }
        // 보간으로 실제 위치보다 멀리 뻗었거나 노이즈로 스냅된 '끝 역'을 잘라낸다.
        // 양 끝에서 근처(NEAR_VISIT_M)에 실제 GPS 점이 있는 역까지만 남기고,
        // 그 사이의 보간된 역(두 실측 역 사이)은 그대로 둔다.
        fun reallyVisited(stIdx: Int): Boolean {
            val s = stations[stIdx]
            return pts.any { dist(it.latitude, it.longitude, s.lat, s.lng) <= NEAR_VISIT_M }
        }
        var lo = 0
        while (lo < merged.size - 1 && !reallyVisited(merged[lo])) lo++
        var hi = merged.size - 1
        while (hi > lo && !reallyVisited(merged[hi])) hi--
        val clipped = if (lo < hi) merged.subList(lo, hi + 1) else merged
        val result = clipped.map { LatLng.from(stations[it].lat, stations[it].lng) }
        return if (result.size >= 2) result else null
    }
}
