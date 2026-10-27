package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent

@Composable
fun EqCurveCanvas(
    bandsGain: List<Float>,
    isMasterEnabled: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = CyanAccent
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val zeroY = h / 2f
        val numBands = bandsGain.size.coerceAtLeast(1)

        // 1. Grid Guidelines (+12dB, 0dB Flat, -12dB)
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, h * 0.15f),
            end = Offset(w, h * 0.15f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, h * 0.85f),
            end = Offset(w, h * 0.85f),
            strokeWidth = 1.dp.toPx()
        )

        // 2. Compute Points for Spline
        val paddingX = 16.dp.toPx()
        val usableW = w - paddingX * 2
        val stepX = usableW / (numBands - 1).coerceAtLeast(1)

        val points = mutableListOf<Offset>()
        for (i in 0 until numBands) {
            val gain = if (isMasterEnabled) bandsGain[i] else 0f
            val px = paddingX + i * stepX
            // Map -12dB .. +12dB into [h - 8dp, 8dp]
            val py = zeroY - (gain / 12f) * (zeroY - 10.dp.toPx())
            points.add(Offset(px, py))
        }

        if (points.isNotEmpty()) {
            // Build Smooth Bézier Path
            val curvePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val pCurrent = points[i]
                    val pNext = points[i + 1]
                    val cX = (pCurrent.x + pNext.x) / 2f
                    val cY = (pCurrent.y + pNext.y) / 2f
                    quadraticTo(pCurrent.x, pCurrent.y, cX, cY)
                }
                lineTo(points.last().x, points.last().y)
            }

            // Fill area under curve
            val fillPath = Path().apply {
                addPath(curvePath)
                lineTo(points.last().x, zeroY)
                lineTo(points.first().x, zeroY)
                close()
            }

            val fillBrush = Brush.verticalGradient(
                colors = if (isMasterEnabled) {
                    listOf(activeColor.copy(alpha = 0.35f), activeColor.copy(alpha = 0.03f))
                } else {
                    listOf(Color(0x2264748B), Color(0x0564748B))
                },
                startY = 0f,
                endY = h
            )
            drawPath(path = fillPath, brush = fillBrush)

            // Stroke line
            val strokeColor = if (isMasterEnabled) activeColor else Color(0xFF64748B)
            drawPath(
                path = curvePath,
                color = strokeColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Anchor dots on frequency points
            points.forEach { p ->
                drawCircle(
                    color = strokeColor,
                    radius = 3.5.dp.toPx(),
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8.dp.toPx(),
                    center = p
                )
            }
        }
    }
}
