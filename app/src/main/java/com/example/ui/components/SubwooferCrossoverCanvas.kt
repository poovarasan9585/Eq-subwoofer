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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import kotlin.math.log10

@Composable
fun SubwooferCrossoverCanvas(
    cutoffHz: Int,
    slopeDb: Int,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = CyanAccent
) {
    val animatedCutoff by animateFloatAsState(
        targetValue = cutoffHz.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "cutoff_anim"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Frequency mapping: Logarithmic 20Hz to 250Hz
        val minLog = log10(20f)
        val maxLog = log10(250f)
        val targetLog = log10(animatedCutoff.coerceIn(20f, 250f))

        val cutoffX = ((targetLog - minLog) / (maxLog - minLog)) * (w - 24.dp.toPx()) + 12.dp.toPx()
        val passbandY = h * 0.25f

        // Steepness slope control
        val rollOffWidth = when (slopeDb) {
            12 -> 65.dp.toPx() // Gentle roll-off
            48 -> 22.dp.toPx() // Very steep brickwall
            else -> 40.dp.toPx() // 24dB standard Linkwitz-Riley
        }

        val path = Path().apply {
            moveTo(0f, passbandY)
            lineTo(cutoffX - 10.dp.toPx(), passbandY)
            // Quadratic knee roll-off
            quadraticTo(
                cutoffX, passbandY + 6.dp.toPx(),
                (cutoffX + rollOffWidth).coerceAtMost(w), h * 0.9f
            )
            lineTo(w, h * 0.95f)
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Fill area under low-pass crossover
        val fillBrush = Brush.verticalGradient(
            colors = if (isEnabled) {
                listOf(activeColor.copy(alpha = 0.35f), activeColor.copy(alpha = 0.02f))
            } else {
                listOf(Color(0x1864748B), Color(0x0264748B))
            },
            startY = passbandY,
            endY = h
        )
        drawPath(path = fillPath, brush = fillBrush)

        // Draw Crossover Curve
        val strokeColor = if (isEnabled) activeColor else Color(0xFF64748B)
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Vertical Cutoff Marker Line (Dashed)
        drawLine(
            color = if (isEnabled) Color(0xFF38BDF8) else Color(0xFF475569),
            start = Offset(cutoffX, 0f),
            end = Offset(cutoffX, h),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        // Glowing Beacon Circle at Cutoff Knee
        drawCircle(
            color = strokeColor.copy(alpha = 0.4f),
            radius = 7.dp.toPx(),
            center = Offset(cutoffX, passbandY + 6.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(cutoffX, passbandY + 6.dp.toPx())
        )
    }
}
