package com.blockstream.compose.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.appTestTag
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DragAnchors {
    Hidden,
    Preview,
    Extended,
}

val ACTION_ITEM_WIDTH_DP = 62.dp

@Composable
fun SwipeableActionsContainer(
    isSwiped: Boolean,
    onSwipe: (Boolean) -> Unit,
    actions: List<SwipeAction>,
    content: @Composable () -> Unit
) {
    if (actions.isEmpty()) {
        content()
        return
    }

    val density = LocalDensity.current
    val actionsWidthPx = with(density) { (ACTION_ITEM_WIDTH_DP * actions.size).toPx() }

    val state = remember {
        AnchoredDraggableState(initialValue = DragAnchors.Hidden)
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(isSwiped) {
        if (!isSwiped && state.currentValue != DragAnchors.Hidden) {
            state.animateTo(DragAnchors.Hidden)
        }
    }

    LaunchedEffect(state.targetValue) {
        when (state.targetValue) {
            DragAnchors.Preview, DragAnchors.Extended -> {
                onSwipe(true)
            }
            DragAnchors.Hidden -> {
                if (isSwiped) {
                    onSwipe(false)
                }
            }
        }
    }

    // Elastic "spring" effect: snap back to Preview from Extended
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }
            .collect { settledValue ->
                if (settledValue == DragAnchors.Extended) {
                    state.animateTo(DragAnchors.Preview)
                }
            }
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == DragAnchors.Preview) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(actionsWidthPx) {
        val extendedOffsetPx = actionsWidthPx * 0.4f
        state.updateAnchors(
            DraggableAnchors {
                DragAnchors.Hidden at 0f
                DragAnchors.Preview at -actionsWidthPx
                DragAnchors.Extended at -(actionsWidthPx + extendedOffsetPx)
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        val offset = state.offset

        // Initial opacity is 0, to prevent flicking when mounted
        if (!offset.isNaN() && offset < 0f) {
            SwipeActionsRow(
                actions = actions,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(with(density) { abs(offset).toDp() })
                    .graphicsLayer {
                        alpha = (abs(offset) / 20f).coerceIn(0f, 1f)
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val safeOffset = state.offset.takeIf { !it.isNaN() } ?: 0f
                    IntOffset(x = safeOffset.roundToInt(), y = 0)
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior
                )
        ) {
            content()
        }
    }
}

data class SwipeAction(
    val icon: DrawableResource,
    val contentDescription: String,
    val backgroundColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun SwipeActionsRow(
    actions: List<SwipeAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(action.backgroundColor)
                    .combinedClickable { action.onClick() }
                    .appTestTag(action.testTag),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    contentDescription = action.contentDescription,
                    tint = whiteMedium,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}