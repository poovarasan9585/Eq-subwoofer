package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.CyanAccent
import kotlin.math.sin

/**
 * Animated High-Fidelity Subwoofer Cone Diaphragm that pulses with bass frequency and excursion.
 */
@Composable
fun SubwooferDiaphragm(
    subGainPercent: Int,
    cutoffHz: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    activeColor: Color = CyanAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sub_pulse")

    // Pulse rate derived from cutoff frequency
    val durationMs = (1000f / (cutoffHz.toFloat() / 2f).coerceIn(20f, 60f)).toInt().coerceIn(120, 600)

    val excursionPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "excursion_anim"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val outerRadius = this.size.minDimension / 2 - 4.dp.toPx()

            val excursionScale = if (isPlaying) {
                (subGainPercent / 100f) * 0.18f * excursionPulse
            } else 0f

            // 1. Outer Aluminum Basket Ring & Bolt Accents
            drawCircle(
                color = Color(0xFF1E293B),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )

            // 8 Perimeter Mounting Screws
            for (i in 0 until 8) {
                val angle = (i * 45f * Math.PI / 180f).toFloat()
                val boltDist = outerRadius - 3.dp.toPx()
                val boltCenter = Offset(
                    center.x + boltDist * kotlin.math.cos(angle),
                    center.y + boltDist * kotlin.math.sin(angle)
                )
                drawCircle(
                    color = Color(0xFF64748B),
                    radius = 2.dp.toPx(),
                    center = boltCenter
                )
            }

            // 2. Heavy Rubber Surround (Roll)
            val surroundRadius = outerRadius - 6.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF020617)),
                    center = center,
                    radius = surroundRadius
                ),
                radius = surroundRadius,
                center = center
            )

            // 3. Carbon Fiber Diaphragm Cone (Dynamically expands/contracts with excursion)
            val coneRadius = (surroundRadius - 8.dp.toPx()) * (1f + excursionScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0B0F19), Color(0xFF020617)),
                    center = center,
                    radius = coneRadius
                ),
                radius = coneRadius,
                center = center
            )

            // Subwoofer concentric ribs / ribbed texture
            val ribCount = 3
            for (r in 1..ribCount) {
                val ribRad = (coneRadius * (r.toFloat() / (ribCount + 1)))
                drawCircle(
                    color = Color(0x22334155),
                    radius = ribRad,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 4. Inverted Center Dust Cap
            val dustCapRadius = coneRadius * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                    center = Offset(center.x - 3.dp.toPx(), center.y - 3.dp.toPx()),
                    radius = dustCapRadius
                ),
                radius = dustCapRadius,
                center = center
            )
            drawCircle(
                color = if (isPlaying && subGainPercent > 20) activeColor.copy(alpha = 0.5f) else Color(0xFF475569),
                radius = dustCapRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center glowing pulse beacon
            if (isPlaying && subGainPercent > 10) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.25f * (subGainPercent / 100f)),
                    radius = dustCapRadius * 0.7f * (1f + excursionPulse * 0.3f),
                    center = center
                )
            }
        }
    }
}
