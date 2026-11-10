package com.blockstream.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import kotlinx.coroutines.launch

@Stable
class AppTabBarScope internal constructor(
    rowScope: RowScope,
    internal val indicator: AppTabBarIndicatorState
) : RowScope by rowScope

internal class AppTabBarIndicatorState {
    var barCoordinates: LayoutCoordinates? = null
    var selectedPill: Rect? by mutableStateOf(null)
}

@Composable
fun AppTabBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable AppTabBarScope.() -> Unit
) {
    val state = remember { AppTabBarIndicatorState() }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    var indicator: Rect? by remember { mutableStateOf(null) }

    val target = state.selectedPill
    LaunchedEffect(target) {
        target ?: return@LaunchedEffect
        val current = indicator
        indicator = target
        if (current == null) {
            left.snapTo(target.left)
            right.snapTo(target.right)
            return@LaunchedEffect
        }
        val movingRight = target.left > left.value
        launch { left.animateTo(target.left, if (movingRight) TrailingEdgeSpring else LeadingEdgeSpring) }
        launch { right.animateTo(target.right, if (movingRight) LeadingEdgeSpring else TrailingEdgeSpring) }
    }

    val pillColor = AppTheme.colors.surface.segmented.selected

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface.overlay)
            .windowInsetsPadding(windowInsets)
            .onPlaced { state.barCoordinates = it }
            .drawBehind {
                val bounds = indicator ?: return@drawBehind
                drawRoundRect(
                    color = pillColor,
                    topLeft = Offset(left.value, bounds.top),
                    size = Size(right.value - left.value, bounds.height),
                    cornerRadius = CornerRadius(PillRadius.toPx())
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        remember(this, state) { AppTabBarScope(this, state) }.content()
    }
}

@Composable
fun AppTabBarScope.AppTabBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector = icon,
    motion: AppTabBarIconMotion = AppTabBarIconMotion.Pop,
    showBadge: Boolean = false,
    iconContentDescription: String? = null
) {
    val indicatorState = indicator
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium))
    val pop = remember { Animatable(1f) }
    val motionValue = remember { Animatable(0f) }
    var wasSelected by remember { mutableStateOf(selected) }

    LaunchedEffect(selected) {
        if (selected && !wasSelected) {
            launch {
                pop.snapTo(0.8f)
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
            }
            launch { motion.play(motionValue) }
        } else if (!selected) {
            launch { pop.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
            launch { motionValue.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
        }
        wasSelected = selected
    }

    val contentColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.text.primary else AppTheme.colors.text.subtle,
        animationSpec = tween(200)
    )

    var pillBounds: Rect? by remember { mutableStateOf(null) }
    LaunchedEffect(selected) {
        if (selected) pillBounds?.let { indicatorState.selectedPill = it }
    }

    Column(
        modifier = modifier
            .weight(1f)
            .height(64.dp)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .onGloballyPositioned { coordinates ->
                    val bar = indicatorState.barCoordinates?.takeIf { it.isAttached } ?: return@onGloballyPositioned
                    val bounds = bar.localBoundingBoxOf(coordinates, clipBounds = false)
                    pillBounds = bounds
                    if (selected) indicatorState.selectedPill = bounds
                }
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        if (motion == AppTabBarIconMotion.Wiggle) rotationZ = motionValue.value
                    }
            ) {
                AppIcon(
                    if (selected) selectedIcon else icon,
                    contentDescription = iconContentDescription,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            if (motion == AppTabBarIconMotion.Spin) rotationZ = motionValue.value
                        }
                )
                if (showBadge) {
                    val ringColor = if (selected) AppTheme.colors.surface.segmented.selected else AppTheme.colors.surface.overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.5.dp, y = (-2.5).dp)
                            .size(9.dp)
                            .background(ringColor, CircleShape)
                            .padding(1.5.dp)
                            .background(AppTheme.colors.feedback.negative.accent, CircleShape)
                    )
                }
            }
        }
        AppText(
            text = label,
            style = AppTheme.typography.label.smSemiBold,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

enum class AppTabBarIconMotion {
    Pop,
    Wiggle,
    Spin;

    internal suspend fun play(value: Animatable<Float, AnimationVector1D>) {
        when (this) {
            Pop -> Unit
            Wiggle -> value.animateTo(0f, keyframes {
                durationMillis = 450
                -14f at 90
                10f at 200
                -5f at 310
            })

            Spin -> {
                value.snapTo(-120f)
                value.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
            }
        }
    }
}

private val PillRadius = 12.dp
private val LeadingEdgeSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 500f)
private val TrailingEdgeSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 220f)
