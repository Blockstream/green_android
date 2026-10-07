package com.blockstream.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.LocalAppContentColor
import com.blockstream.designsystem.appContentColorFor
import com.blockstream.designsystem.foundation.appRipple

@Composable
@NonRestartableComposable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface.base,
    contentColor: Color = appContentColorFor(color),
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation)
                .semantics(mergeDescendants = false) { isTraversalGroup = true }
                .pointerInput(Unit) {},
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@Composable
@NonRestartableComposable
fun AppSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface.base,
    contentColor: Color = appContentColorFor(color),
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = appRipple(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation)
                .clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@Composable
@NonRestartableComposable
fun AppSurface(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface.base,
    contentColor: Color = appContentColorFor(color),
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = appRipple(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@Composable
@NonRestartableComposable
fun AppSurface(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface.base,
    contentColor: Color = appContentColorFor(color),
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = appRipple(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation)
                .toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onValueChange = onCheckedChange
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@Composable
private fun Modifier.surface(shape: Shape, backgroundColor: Color, border: BorderStroke?, shadowElevation: Dp): Modifier {
    val shadowPx = with(LocalDensity.current) { shadowElevation.toPx() }
    return this
        .then(if (shadowPx > 0f) Modifier.graphicsLayer(shadowElevation = shadowPx, shape = shape, clip = false) else Modifier)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .background(color = backgroundColor, shape = shape)
        .clip(shape)
}
