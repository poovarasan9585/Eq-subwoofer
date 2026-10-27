package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.abs

/**
 * Vertical Equalizer Slider with dB readout, center 0dB notch, and haptic snap.
 */
@Composable
fun VerticalEqSlider(
    gainDb: Float, // -12f .. +12f
    onGainChange: (Float) -> Unit,
    frequencyLabel: String,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    width: Dp = 34.dp,
    activeColor: Color = CyanAccent,
    minGain: Float = -12f,
    maxGain: Float = 12f
) {
    val view = LocalView.current
    var wasAtZero by remember { mutableFloatStateOf(0f) }

    val animatedGain by animateFloatAsState(
        targetValue = gainDb,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "eq_gain_anim"
    )

    Column(
        modifier = modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain dB Readout
        val gainStr = when {
            gainDb > 0.1f -> "+${String.format(java.util.Locale.US, "%.1f", gainDb)}"
            gainDb < -0.1f -> String.format(java.util.Locale.US, "%.1f", gainDb)
            else -> "0.0"
        }
        val gainColor = when {
            gainDb > 0.1f -> activeColor
            gainDb < -0.1f -> Color(0xFF94A3B8)
            else -> TextMuted
        }

        Text(
            text = gainStr,
            color = gainColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Track Canvas with Drag Gesture
        Box(
            modifier = Modifier
                .height(height)
                .width(width)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                            val newGain = minGain + fraction * (maxGain - minGain)
                            val rounded = (Math.round(newGain * 2) / 2f).coerceIn(minGain, maxGain)
                            onGainChange(rounded)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val fraction = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                            val newGain = minGain + fraction * (maxGain - minGain)
                            val rounded = (Math.round(newGain * 2) / 2f).coerceIn(minGain, maxGain)

                            if (abs(rounded) < 0.3f && wasAtZero != 0f) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                wasAtZero = 0f
                            } else if (abs(rounded) >= 0.3f) {
                                wasAtZero = rounded
                            }
                            onGainChange(rounded)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(width, height)) {
                val totalH = this.size.height
                val totalW = this.size.width
                val centerX = totalW / 2
                val centerY = totalH / 2 // 0 dB line

                val trackW = 5.dp.toPx()
                val trackX = centerX - trackW / 2

                // 1. Inactive Track Rail
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(trackX, 0f),
                    size = Size(trackW, totalH),
                    cornerRadius = CornerRadius(trackW / 2, trackW / 2)
                )

                // 2. Center 0dB Notch tick
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(centerX - 9.dp.toPx(), centerY),
                    end = Offset(centerX + 9.dp.toPx(), centerY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 3. Active Fill from center 0dB
                val normFrac = (animatedGain - minGain) / (maxGain - minGain)
                val thumbY = totalH * (1f - normFrac)

                if (animatedGain > 0.05f) {
                    val fillHeight = centerY - thumbY
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(activeColor, activeColor.copy(alpha = 0.6f)),
                            startY = thumbY,
                            endY = centerY
                        ),
                        topLeft = Offset(trackX, thumbY),
                        size = Size(trackW, fillHeight),
                        cornerRadius = CornerRadius(trackW / 2, trackW / 2)
                    )
                } else if (animatedGain < -0.05f) {
                    val fillHeight = thumbY - centerY
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(trackX, centerY),
                        size = Size(trackW, fillHeight),
                        cornerRadius = CornerRadius(trackW / 2, trackW / 2)
                    )
                }

                // 4. Glow behind Thumb
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )

                // 5. Thumb Pill
                val thumbWidth = 24.dp.toPx()
                val thumbHeight = 14.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX - thumbWidth / 2, thumbY - thumbHeight / 2),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                // Thumb border
                drawRoundRect(
                    color = if (abs(animatedGain) > 0.1f) activeColor else Color(0xFF64748B),
                    topLeft = Offset(centerX - thumbWidth / 2, thumbY - thumbHeight / 2),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                // Center accent pip on thumb
                drawCircle(
                    color = if (abs(animatedGain) > 0.1f) activeColor else Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency Label at Bottom
        Text(
            text = frequencyLabel,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
