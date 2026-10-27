package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerMode {
    BARS, WAVE, PULSE
}

@Composable
fun SpectrumVisualizer(
    bins: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp,
    activeColor: Color = CyanAccent
) {
    var mode by remember { mutableStateOf(VisualizerMode.BARS) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable {
                mode = when (mode) {
                    VisualizerMode.BARS -> VisualizerMode.WAVE
                    VisualizerMode.WAVE -> VisualizerMode.PULSE
                    VisualizerMode.PULSE -> VisualizerMode.BARS
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val numBins = bins.size

            when (mode) {
                VisualizerMode.BARS -> {
                    val barSpacing = 3.dp.toPx()
                    val totalSpacing = barSpacing * (numBins - 1)
                    val barWidth = (w - totalSpacing) / numBins

                    for (i in 0 until numBins) {
                        val barHeight = (bins[i] * h * 0.9f).coerceIn(4.dp.toPx(), h)
                        val x = i * (barWidth + barSpacing)
                        val y = h - barHeight

                        val grad = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                activeColor,
                                activeColor.copy(alpha = 0.6f),
                                Color(0xFF0F766E).copy(alpha = 0.3f)
                            ),
                            startY = y,
                            endY = h
                        )

                        drawRoundRect(
                            brush = grad,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        if (barHeight > 8.dp.toPx()) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, 2.5.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }
                VisualizerMode.WAVE -> {
                    val path = Path()
                    val stepX = w / (numBins - 1).coerceAtLeast(1)
                    val midY = h / 2f

                    path.moveTo(0f, midY)
                    for (i in 0 until numBins) {
                        val x = i * stepX
                        val offsetFactor = (bins[i] - 0.2f) * 2f
                        val y = midY - offsetFactor * (h * 0.45f)
                        if (i == 0) path.moveTo(x, y)
                        else {
                            val prevX = (i - 1) * stepX
                            val prevOffset = (bins[i - 1] - 0.2f) * 2f
                            val prevY = midY - prevOffset * (h * 0.45f)
                            val cX = (prevX + x) / 2f
                            val cY = (prevY + y) / 2f
                            path.quadraticTo(prevX, prevY, cX, cY)
                        }
                    }

                    drawPath(
                        path = path,
                        color = activeColor.copy(alpha = 0.35f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                VisualizerMode.PULSE -> {
                    val center = Offset(w / 2f, h / 2f)
                    val maxRadius = (h / 2f) * 0.85f
                    val avgSub = ((bins.getOrElse(0) { 0.2f } + bins.getOrElse(1) { 0.2f } + bins.getOrElse(2) { 0.2f }) / 3f)
                    val pulseRadius = maxRadius * (0.4f + avgSub * 0.6f)

                    drawCircle(
                        color = activeColor.copy(alpha = 0.15f),
                        radius = pulseRadius * 1.3f,
                        center = center
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = 0.3f),
                        radius = pulseRadius,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White, activeColor, Color(0xFF064E3B)),
                            center = center,
                            radius = pulseRadius * 0.7f
                        ),
                        radius = pulseRadius * 0.7f,
                        center = center
                    )

                    for (i in 0 until 16) {
                        val angle = (i * (360f / 16) * PI / 180f).toFloat()
                        val binVal = bins[i * 2 % numBins]
                        val spokeLen = pulseRadius + binVal * 24.dp.toPx()
                        val start = Offset(center.x + pulseRadius * cos(angle), center.y + pulseRadius * sin(angle))
                        val end = Offset(center.x + spokeLen * cos(angle), center.y + spokeLen * sin(angle))
                        drawLine(
                            color = activeColor.copy(alpha = 0.7f),
                            start = start,
                            end = end,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Text(
            text = "TAP FOR ${mode.name}",
            color = TextMuted.copy(alpha = 0.7f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        )
    }
}
