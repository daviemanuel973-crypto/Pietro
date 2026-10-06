package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.WaveformGreen

@Composable
fun AudioPeakMeter(
    peak: Float, // 0.0f to 1.0f+
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    width: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF13151C))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val h = size.height
            val w = size.width
            val normPeak = peak.coerceIn(0f, 1.2f)

            // Segmented LEDs or smooth gradient fill from bottom
            val fillHeight = h * normPeak.coerceAtMost(1.0f)
            val isClipping = normPeak >= 0.98f

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    if (isClipping) RecordingRed else StudioAmber,
                    WaveformGreen,
                    Color(0xFF047857)
                ),
                startY = 0f,
                endY = h
            )

            // Draw level from bottom up
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(0f, h - fillHeight),
                size = Size(w, fillHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Clipping LED at the very top
            if (isClipping) {
                drawRect(
                    color = RecordingRed,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, 8.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    waveColor: Color = NeonCyan,
    progressFraction: Float = 0f
) {
    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val step = w / amplitudes.size.toFloat()

        amplitudes.forEachIndexed { index, amp ->
            val barX = index * step + (step * 0.2f)
            val barW = (step * 0.6f).coerceAtLeast(2f)
            val barH = (amp * (h * 0.85f)).coerceAtLeast(4f)
            val isPlayed = (index.toFloat() / amplitudes.size) <= progressFraction

            val color = if (isPlayed) waveColor else Color(0xFF334155)

            drawRoundRect(
                color = color,
                topLeft = Offset(barX, centerY - (barH / 2f)),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
