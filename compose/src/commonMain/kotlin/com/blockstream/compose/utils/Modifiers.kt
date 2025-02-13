package com.blockstream.compose.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.compose.extensions.dpToPx
import com.blockstream.compose.theme.md_theme_surfaceCircle

@Composable
fun Modifier.noRippleToggleable(
    value: Boolean,
    enabled: Boolean = true,
    onValueChange: (Boolean) -> Unit
): Modifier {
    return this then toggleable(
        value = value,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onValueChange = onValueChange
    )
}

@Composable
inline fun Modifier.noRippleClickable(
    crossinline onClick: () -> Unit
): Modifier {
    return this then clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        onClick()
    }
}

@Composable
inline fun Modifier.roundBackground(
    horizontal: Dp = 8.dp,
    vertical: Dp = 4.dp,
    size : Dp = 16.dp,
    color: Color = md_theme_surfaceCircle
): Modifier {
    return this then background(
        color = color,
        shape = RoundedCornerShape(size)
    ).padding(horizontal = horizontal, vertical = vertical)
}

@Composable
fun Modifier.fadingEdges(
    lazyListState: LazyListState
): Modifier {

    val isAtTop: Boolean by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            // Check if the last visible item is the last item AND if it's fully visible
            lastVisibleItem != null &&
                    lastVisibleItem.index == layoutInfo.totalItemsCount - 1 &&
                    lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset
        }
    }

    val topFadePx by animateFloatAsState(
        if (isAtTop) 0.dp.dpToPx() else 16.dp.dpToPx(),
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    val bottomFadePx by animateFloatAsState(
        if (isAtBottom) 0.dp.dpToPx() else 16.dp.dpToPx(),
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) // Required for blend modes to work correctly
        .drawWithContent {
            drawContent()

            if (topFadePx > 0) {
                val topBrush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black,
                    startY = 0f,
                    endY = topFadePx
                )
                drawRect(
                    brush = topBrush,
                    size = Size(size.width, topFadePx),
                    blendMode = BlendMode.DstIn
                )
            }

            if (bottomFadePx > 0) {
                val bottomBrush = Brush.verticalGradient(
                    0f to Color.Black,
                    1f to Color.Transparent,
                    startY = size.height - bottomFadePx,
                    endY = size.height
                )
                drawRect(
                    brush = bottomBrush,
                    topLeft = Offset(0f, size.height - bottomFadePx),
                    size = Size(size.width, bottomFadePx),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}