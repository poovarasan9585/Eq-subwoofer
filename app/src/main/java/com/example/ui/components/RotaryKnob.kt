package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-end Rotary Knob with tactile haptic feedback, glowing arc, and smooth angle tracking.
 */
@Composable
fun RotaryKnob(
    value: Float, // 0f .. 1f
    onValueChange: (Float) -> Unit,
    label: String,
    valueDisplay: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    activeColor: Color = CyanAccent,
    glowColor: Color = activeColor.copy(alpha = 0.4f),
    startAngle: Float = 135f,
    sweepAngle: Float = 270f
) {
    val view = LocalView.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Vertical drag gesture sensitivity
                        val delta = -dragAmount.y / 200f
                        val newValue = (value + delta).coerceIn(0f, 1f)
                        if (Math.abs(newValue - lastHapticValue) > 0.05f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            lastHapticValue = newValue
                        }
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 8.dp.toPx()
                val radius = (this.size.minDimension - strokeWidth * 2) / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)

                // 1. Inactive background track
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Active illuminated arc
                val activeSweep = sweepAngle * value
                if (activeSweep > 0f) {
                    // Outer subtle glow
                    drawArc(
                        color = glowColor,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Core bright arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(activeColor.copy(alpha = 0.7f), activeColor, activeColor),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 3. Dial Body (Dark brushed aluminum circle)
                val innerRadius = radius - strokeWidth * 1.3f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF26334D), AmoledSurface, Color(0xFF090E1A)),
                        center = center,
                        radius = innerRadius
                    ),
                    radius = innerRadius,
                    center = center
                )
                // Rim accent stroke
                drawCircle(
                    color = Color(0xFF334155),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 4. Pointer Dot Indicator
                val currentAngleDeg = startAngle + activeSweep
                val currentAngleRad = (currentAngleDeg * PI / 180f).toFloat()
                val dotDist = innerRadius - 9.dp.toPx()
                val dotCenter = Offset(
                    center.x + dotDist * cos(currentAngleRad),
                    center.y + dotDist * sin(currentAngleRad)
                )

                // Glow behind dot
                drawCircle(
                    color = glowColor,
                    radius = 6.dp.toPx(),
                    center = dotCenter
                )
                // White/active dot
                drawCircle(
                    color = activeColor,
                    radius = 3.5.dp.toPx(),
                    center = dotCenter
                )
            }

            // Value text in center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueDisplay,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
