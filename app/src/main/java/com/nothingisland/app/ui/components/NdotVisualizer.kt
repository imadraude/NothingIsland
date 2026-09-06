package com.nothingisland.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothingisland.app.ui.theme.NothingRed

/**
 * A Nothing OS dot-matrix audio waveform visualizer.
 * Displays animated dot columns that pulse when audio is playing.
 */
@Composable
fun NdotVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = NothingRed,
    barCount: Int = 4,
    size: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "VisualizerTransition")

    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = if (isPlaying) 0.2f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.9f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val bar4 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPlaying) 0.4f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = listOf(bar1, bar2, bar3, bar4)

    Canvas(modifier = modifier.size(size)) {
        val barWidth = 2.5.dp.toPx()
        val spacing = (size.toPx() - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
        val maxHeight = size.toPx()

        for (i in 0 until barCount) {
            val hFactor = if (isPlaying) heights[i % heights.size] else 0.25f
            val currentHeight = (maxHeight * hFactor).coerceAtLeast(barWidth)
            val startX = i * (barWidth + spacing) + (barWidth / 2)
            val startY = (maxHeight - currentHeight) / 2
            val endY = startY + currentHeight

            drawLine(
                color = activeColor,
                start = Offset(startX, startY),
                end = Offset(startX, endY),
                strokeWidth = barWidth
            )
        }
    }
}
