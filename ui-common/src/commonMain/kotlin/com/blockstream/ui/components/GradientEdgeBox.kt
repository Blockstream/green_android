package com.blockstream.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable that displays content with horizontal gradients applied to both edges.
 *
 * @param modifier The modifier to be applied to the box
 * @param startSolidWidth Width of the gradient on the start (left) edge
 * @param endSolidWidth Width of the gradient on the end (right) edge
 * @param startGradientColors Colors for the start gradient (left to right)
 * @param endGradientColors Colors for the end gradient (right to left)
 * @param content The content to be displayed inside the box
 */
@Composable
fun GradientEdgeBox(
    modifier: Modifier = Modifier,
    startSolidWidth: Dp = 16.dp,
    endSolidWidth: Dp = 16.dp,
    gradientWidth: Dp = 16.dp,
    startGradientColors: List<Color> = listOf(MaterialTheme.colorScheme.surface, Color.Transparent),
    endGradientColors: List<Color> = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                // Draw the main content
                drawContent()

                val width = size.width

                val gradientWidthPx = gradientWidth.toPx()

                // Draw start (left) gradient
                val startWidthPx = startSolidWidth.toPx()
                if (startWidthPx > 0 && startGradientColors.size >= 2) {
                    drawRect(
                        brush = SolidColor(startGradientColors.first()),
                        size = size.copy(width = startWidthPx),
                    )

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = startGradientColors,
                            startX = startWidthPx,
                            endX = startWidthPx + gradientWidthPx
                        ),
                        size = size.copy(width = gradientWidthPx + startWidthPx),
                    )
                }

                // Draw end (right) gradient
                val endWidthPx = endSolidWidth.toPx()
                if (endWidthPx > 0 && endGradientColors.size >= 2) {

                    drawRect(
                        brush = SolidColor(endGradientColors.last()),
                        topLeft = Offset(width - endWidthPx, 0f),
                        size = size.copy(width = endWidthPx),
                    )

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = endGradientColors,
                            startX = width - endWidthPx - gradientWidthPx,
                            endX = width - endWidthPx
                        ),
                        topLeft = Offset(width - endWidthPx - gradientWidthPx, 0f),
                        size = size.copy(width = gradientWidthPx),
                    )
                }
            },
        content = content
    )
}