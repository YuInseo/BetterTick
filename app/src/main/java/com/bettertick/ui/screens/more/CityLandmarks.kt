package com.bettertick.ui.screens.more

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * City landmark silhouettes drawn with Compose Canvas primitives. Each
 * drawing scales to fill its Canvas — so the same Composable works at both
 * grid-thumbnail (small) and preview-card (large) sizes. Strokes are scaled
 * relative to canvas height so line weight feels consistent at any scale.
 *
 * All silhouettes are abstract/geometric; they take a [Color] (typically the
 * theme's accent) and render in a single tone with occasional alpha variation
 * to suggest depth without needing raster assets.
 */
enum class CityLandmark {
    BeijingPearlTower,
    LondonBigBen,
    MoscowStBasils,
    SanFranciscoGoldenGate,
    SeoulHanok,
    TokyoTower
}

@Composable
fun CityLandmarkIllustration(
    landmark: CityLandmark,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        when (landmark) {
            CityLandmark.BeijingPearlTower -> drawPearlTower(color)
            CityLandmark.LondonBigBen -> drawBigBen(color)
            CityLandmark.MoscowStBasils -> drawStBasils(color)
            CityLandmark.SanFranciscoGoldenGate -> drawGoldenGate(color)
            CityLandmark.SeoulHanok -> drawHanok(color)
            CityLandmark.TokyoTower -> drawTokyoTower(color)
        }
    }
}

// =============== Beijing — Pearl-style radio tower ===============
private fun DrawScope.drawPearlTower(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val stroke = (h * 0.022f).coerceAtLeast(2f)
    val baseY = h * 0.95f

    // Tripod legs meeting below the lower sphere
    val apexY = h * 0.80f
    drawLine(color, Offset(w * 0.28f, baseY), Offset(cx, apexY), stroke * 1.2f, StrokeCap.Round)
    drawLine(color, Offset(w * 0.72f, baseY), Offset(cx, apexY), stroke * 1.2f, StrokeCap.Round)

    // Main vertical shaft
    drawLine(color, Offset(cx, apexY), Offset(cx, h * 0.10f), stroke * 1.3f, StrokeCap.Round)

    // Lower large sphere
    drawCircle(color, radius = h * 0.085f, center = Offset(cx, h * 0.60f))
    // Upper smaller sphere
    drawCircle(color, radius = h * 0.055f, center = Offset(cx, h * 0.30f))
    // Tiny antenna bead
    drawCircle(color, radius = h * 0.020f, center = Offset(cx, h * 0.13f))
    // Fine antenna spire above
    drawLine(color, Offset(cx, h * 0.11f), Offset(cx, 0f), stroke * 0.6f, StrokeCap.Round)

    // Ground line
    drawRect(color, topLeft = Offset(w * 0.10f, baseY), size = Size(w * 0.80f, h * 0.03f))
}

// =============== London — clock tower ===============
private fun DrawScope.drawBigBen(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val shaftHalf = w * 0.10f

    // Lower shaft (base to clock housing)
    drawRect(
        color,
        topLeft = Offset(cx - shaftHalf, h * 0.35f),
        size = Size(shaftHalf * 2f, h * 0.65f)
    )
    // Decorative step at base
    drawRect(
        color,
        topLeft = Offset(cx - shaftHalf * 1.4f, h * 0.92f),
        size = Size(shaftHalf * 2.8f, h * 0.08f)
    )

    // Clock housing (wider block)
    val clockHalf = shaftHalf * 1.55f
    val clockTop = h * 0.18f
    val clockH = h * 0.17f
    drawRect(
        color,
        topLeft = Offset(cx - clockHalf, clockTop),
        size = Size(clockHalf * 2f, clockH)
    )
    // Clock face — lighter-tone circle suggesting the dial
    drawCircle(
        color.copy(alpha = 0.35f),
        radius = h * 0.055f,
        center = Offset(cx, clockTop + clockH * 0.5f)
    )

    // Steep roof above clock
    val roof = Path().apply {
        moveTo(cx - clockHalf, clockTop)
        lineTo(cx, h * 0.04f)
        lineTo(cx + clockHalf, clockTop)
        close()
    }
    drawPath(roof, color)

    // Top spike
    drawLine(color, Offset(cx, h * 0.04f), Offset(cx, 0f), (w * 0.012f).coerceAtLeast(1.5f), StrokeCap.Round)
}

// =============== Moscow — onion-dome cathedral ===============
private fun DrawScope.drawStBasils(color: Color) {
    val w = size.width
    val h = size.height

    data class Spire(val cx: Float, val bodyTopY: Float, val bulbR: Float)
    // 5 spires, center tallest
    val spires = listOf(
        Spire(w * 0.14f, h * 0.72f, h * 0.050f),
        Spire(w * 0.33f, h * 0.55f, h * 0.065f),
        Spire(w * 0.50f, h * 0.38f, h * 0.085f),
        Spire(w * 0.67f, h * 0.55f, h * 0.065f),
        Spire(w * 0.86f, h * 0.72f, h * 0.050f)
    )

    for (s in spires) {
        val bodyW = s.bulbR * 1.55f
        // Body rectangle
        drawRect(
            color,
            topLeft = Offset(s.cx - bodyW * 0.5f, s.bodyTopY),
            size = Size(bodyW, h - s.bodyTopY)
        )
        // Onion bulb: big circle, then narrow shoulder where it meets body
        val bulbCY = s.bodyTopY - s.bulbR * 0.75f
        drawCircle(color, radius = s.bulbR, center = Offset(s.cx, bulbCY))
        // Fill the gap under the bulb so the silhouette merges cleanly
        drawRect(
            color,
            topLeft = Offset(s.cx - s.bulbR * 0.45f, bulbCY + s.bulbR * 0.2f),
            size = Size(s.bulbR * 0.9f, s.bodyTopY - (bulbCY + s.bulbR * 0.2f))
        )
        // Pointed tip
        val tip = Path().apply {
            moveTo(s.cx - s.bulbR * 0.30f, bulbCY - s.bulbR * 0.90f)
            lineTo(s.cx, bulbCY - s.bulbR * 1.80f)
            lineTo(s.cx + s.bulbR * 0.30f, bulbCY - s.bulbR * 0.90f)
            close()
        }
        drawPath(tip, color)
    }

    // Ground line
    drawRect(color, topLeft = Offset(0f, h * 0.97f), size = Size(w, h * 0.03f))
}

// =============== San Francisco — suspension bridge ===============
private fun DrawScope.drawGoldenGate(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = (h * 0.018f).coerceAtLeast(1.6f)

    val leftCx = w * 0.24f
    val rightCx = w * 0.76f
    val towerTopY = h * 0.18f
    val towerBottomY = h * 0.85f
    val deckY = h * 0.68f
    val towerW = w * 0.035f

    // Towers — pillars
    for (cx in listOf(leftCx, rightCx)) {
        drawRect(
            color,
            topLeft = Offset(cx - towerW * 0.5f, towerTopY),
            size = Size(towerW, towerBottomY - towerTopY)
        )
        // 3 horizontal crossbeams
        val beamW = towerW * 4.2f
        val beamH = h * 0.013f
        listOf(0.22f, 0.44f, 0.66f).forEach { r ->
            val y = towerTopY + (towerBottomY - towerTopY) * r
            drawRect(
                color,
                topLeft = Offset(cx - beamW * 0.5f, y),
                size = Size(beamW, beamH)
            )
        }
    }

    // Main suspension cable: deck-left → left tower top → sag → right tower top → deck-right
    val anchorY = deckY - h * 0.03f
    val cable = Path().apply {
        moveTo(0f, anchorY)
        lineTo(leftCx, towerTopY)
        cubicTo(
            leftCx + (rightCx - leftCx) * 0.25f, towerTopY + h * 0.32f,
            leftCx + (rightCx - leftCx) * 0.75f, towerTopY + h * 0.32f,
            rightCx, towerTopY
        )
        lineTo(w, anchorY)
    }
    drawPath(cable, color, style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round))

    // Suspender cables (thin vertical lines from main cable to deck)
    val suspenderStroke = stroke * 0.3f
    // Between towers — approximate y on the cubic by lerp in x
    val span = rightCx - leftCx
    val sagBottom = towerTopY + h * 0.26f
    val count = 7
    for (i in 1 until count) {
        val t = i / count.toFloat()
        val x = leftCx + span * t
        // Rough sag curve — parabola approximation
        val dipT = 4f * t * (1f - t)
        val y = towerTopY + (sagBottom - towerTopY) * dipT
        drawLine(
            color.copy(alpha = 0.6f),
            Offset(x, y), Offset(x, deckY),
            suspenderStroke, StrokeCap.Round
        )
    }
    // Outside towers — cables drape from tower to anchor
    for (i in 1..3) {
        val t = i / 4f
        val xL = leftCx - (leftCx - 0f) * t
        val yL = towerTopY + (anchorY - towerTopY) * t
        drawLine(color.copy(alpha = 0.6f), Offset(xL, yL), Offset(xL, deckY), suspenderStroke, StrokeCap.Round)

        val xR = rightCx + (w - rightCx) * t
        val yR = towerTopY + (anchorY - towerTopY) * t
        drawLine(color.copy(alpha = 0.6f), Offset(xR, yR), Offset(xR, deckY), suspenderStroke, StrokeCap.Round)
    }

    // Road deck
    drawRect(color, topLeft = Offset(0f, deckY), size = Size(w, h * 0.022f))
    // Water hint below
    drawRect(color.copy(alpha = 0.25f), topLeft = Offset(0f, h * 0.94f), size = Size(w, h * 0.06f))
}

// =============== Seoul — traditional Hanok roof ===============
private fun DrawScope.drawHanok(color: Color) {
    val w = size.width
    val h = size.height

    val roofPeakY = h * 0.20f
    val roofEaveY = h * 0.55f
    val tileBandY = h * 0.58f
    val platformY = h * 0.92f

    // Sweeping roof — eaves curl up at both ends
    val roof = Path().apply {
        moveTo(w * 0.02f, roofEaveY - h * 0.04f)
        cubicTo(
            w * 0.08f, h * 0.60f,
            w * 0.18f, h * 0.38f,
            w * 0.30f, roofPeakY + h * 0.06f
        )
        cubicTo(
            w * 0.38f, roofPeakY + h * 0.01f,
            w * 0.46f, roofPeakY,
            w * 0.50f, roofPeakY
        )
        cubicTo(
            w * 0.54f, roofPeakY,
            w * 0.62f, roofPeakY + h * 0.01f,
            w * 0.70f, roofPeakY + h * 0.06f
        )
        cubicTo(
            w * 0.82f, h * 0.38f,
            w * 0.92f, h * 0.60f,
            w * 0.98f, roofEaveY - h * 0.04f
        )
        // Tile band (bottom of roof)
        lineTo(w * 0.92f, tileBandY)
        lineTo(w * 0.08f, tileBandY)
        close()
    }
    drawPath(roof, color)

    // Ridge accent — darker line along the crest
    drawLine(
        color.copy(alpha = 0.55f),
        Offset(w * 0.35f, roofPeakY + h * 0.045f),
        Offset(w * 0.65f, roofPeakY + h * 0.045f),
        (h * 0.010f).coerceAtLeast(1.2f),
        StrokeCap.Round
    )

    // Pillars
    val pillarTop = tileBandY + h * 0.02f
    val pillarW = w * 0.028f
    listOf(0.14f, 0.36f, 0.64f, 0.86f).forEach { xr ->
        val x = w * xr
        drawRect(
            color,
            topLeft = Offset(x - pillarW * 0.5f, pillarTop),
            size = Size(pillarW, platformY - pillarTop)
        )
    }

    // Door frame between middle pillars
    drawRect(
        color.copy(alpha = 0.55f),
        topLeft = Offset(w * 0.40f, pillarTop + h * 0.05f),
        size = Size(w * 0.20f, platformY - pillarTop - h * 0.05f)
    )

    // Stone platform
    drawRect(color, topLeft = Offset(0f, platformY), size = Size(w, h * 0.05f))
    drawRect(color.copy(alpha = 0.55f), topLeft = Offset(0f, platformY + h * 0.05f), size = Size(w, h * 0.03f))
}

// =============== Tokyo — lattice tower ===============
private fun DrawScope.drawTokyoTower(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val stroke = (h * 0.017f).coerceAtLeast(1.5f)

    val baseY = h * 0.92f
    val topY = h * 0.20f
    val baseHalf = w * 0.30f
    val topHalf = w * 0.055f

    fun halfAtY(y: Float): Float {
        val t = ((baseY - y) / (baseY - topY)).coerceIn(0f, 1f)
        return baseHalf + (topHalf - baseHalf) * t
    }

    // Main tapered legs
    drawLine(
        color,
        Offset(cx - baseHalf, baseY), Offset(cx - topHalf, topY),
        stroke * 1.6f, StrokeCap.Round
    )
    drawLine(
        color,
        Offset(cx + baseHalf, baseY), Offset(cx + topHalf, topY),
        stroke * 1.6f, StrokeCap.Round
    )

    // Lower large observation deck
    val deck1Y = h * 0.58f
    val deck1Half = halfAtY(deck1Y)
    drawRect(
        color,
        topLeft = Offset(cx - deck1Half * 1.25f, deck1Y),
        size = Size(deck1Half * 2.5f, h * 0.038f)
    )
    // Upper small observation deck
    val deck2Y = h * 0.40f
    val deck2Half = halfAtY(deck2Y)
    drawRect(
        color,
        topLeft = Offset(cx - deck2Half * 1.25f, deck2Y),
        size = Size(deck2Half * 2.5f, h * 0.030f)
    )

    // Lattice X crossbeams in sections between decks
    fun lattice(y1: Float, y2: Float, steps: Int) {
        for (i in 0 until steps) {
            val t1 = i / steps.toFloat()
            val t2 = (i + 1) / steps.toFloat()
            val hy1 = y1 + (y2 - y1) * t1
            val hy2 = y1 + (y2 - y1) * t2
            val half1 = halfAtY(hy1)
            val half2 = halfAtY(hy2)
            drawLine(
                color.copy(alpha = 0.75f),
                Offset(cx - half1, hy1), Offset(cx + half2, hy2),
                stroke * 0.45f, StrokeCap.Round
            )
            drawLine(
                color.copy(alpha = 0.75f),
                Offset(cx + half1, hy1), Offset(cx - half2, hy2),
                stroke * 0.45f, StrokeCap.Round
            )
        }
    }
    lattice(deck1Y + h * 0.038f, baseY - h * 0.02f, 4)
    lattice(deck2Y + h * 0.030f, deck1Y - h * 0.008f, 3)
    lattice(topY + h * 0.02f, deck2Y - h * 0.008f, 2)

    // Top cap
    drawRect(
        color,
        topLeft = Offset(cx - topHalf * 1.3f, topY),
        size = Size(topHalf * 2.6f, h * 0.025f)
    )
    // Antenna
    drawLine(color, Offset(cx, topY), Offset(cx, 0f), stroke * 0.8f, StrokeCap.Round)

    // Base platform
    drawRect(
        color,
        topLeft = Offset(cx - baseHalf * 1.15f, baseY),
        size = Size(baseHalf * 2.3f, h * 0.04f)
    )
    // Ground line
    drawRect(color.copy(alpha = 0.55f), topLeft = Offset(0f, baseY + h * 0.04f), size = Size(w, h * 0.02f))
}
