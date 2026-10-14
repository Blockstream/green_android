package com.blockstream.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val style = AppSwitchDefaults.style()
    val state = AppSwitchState.of(enabled = enabled, checked = checked)
    val spec = tween<Dp>(durationMillis = style.animationDurationMillis, easing = EaseOut)
    val colorSpec = tween<Color>(durationMillis = style.animationDurationMillis, easing = EaseOut)
    val thumbSize = animateDpAsState(if (checked) style.checkedThumbSize else style.thumbSize, spec)
    val thumbCenter = animateDpAsState(if (checked) style.checkedThumbCenter else style.thumbCenter, spec)
    val trackColor = animateColorAsState(style.colors.trackColor(state), colorSpec)
    val thumbColor = animateColorAsState(style.colors.thumbColor(state), colorSpec)
    val borderColor = animateColorAsState(style.colors.borderColor(state) ?: Color.Transparent, colorSpec)
    val semantics = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactions,
            indication = null
        )
    } else {
        Modifier.semantics {
            if (!enabled) disabled()
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        }
    }

    Box(
        modifier = modifier
            .then(semantics)
            .alpha(if (enabled) 1f else style.disabledAlpha)
            .wrapContentSize(Alignment.Center)
            .requiredSize(style.trackWidth, style.trackHeight)
            .track(style, trackColor, borderColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((thumbCenter.value - style.checkedThumbSize / 2).roundToPx(), 0) }
                .size(style.checkedThumbSize)
                .indication(interactions, appRipple(bounded = false, radius = style.rippleRadius))
                .drawBehind { drawCircle(thumbColor.value, radius = thumbSize.value.toPx() / 2) }
        )
    }
}

private fun Modifier.track(style: AppSwitchStyle, trackColor: State<Color>, borderColor: State<Color>): Modifier = drawBehind {
    val outline = style.trackShape.createOutline(size, layoutDirection, this)
    drawOutline(outline, trackColor.value)
    val stroke = style.borderWidth.toPx()
    val inset = stroke / 2
    val innerOutline = style.trackShape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
    translate(inset, inset) {
        drawOutline(innerOutline, borderColor.value, style = Stroke(stroke))
    }
}

enum class AppSwitchState {
    On,
    Off,
    Disabled;

    companion object {
        fun of(enabled: Boolean, checked: Boolean): AppSwitchState = when {
            !enabled -> Disabled
            checked -> On
            else -> Off
        }
    }
}

@Immutable
class AppSwitchColors(
    val trackColor: Color,
    val checkedTrackColor: Color,
    val disabledTrackColor: Color,
    val thumbColor: Color,
    val checkedThumbColor: Color,
    val disabledThumbColor: Color,
    val borderColor: Color?,
    val checkedBorderColor: Color?,
    val disabledBorderColor: Color?
) {
    @Stable
    fun borderColor(state: AppSwitchState): Color? = when (state) {
        AppSwitchState.On -> checkedBorderColor
        AppSwitchState.Off -> borderColor
        AppSwitchState.Disabled -> disabledBorderColor
    }

    @Stable
    fun trackColor(state: AppSwitchState): Color = when (state) {
        AppSwitchState.On -> checkedTrackColor
        AppSwitchState.Off -> trackColor
        AppSwitchState.Disabled -> disabledTrackColor
    }

    @Stable
    fun thumbColor(state: AppSwitchState): Color = when (state) {
        AppSwitchState.On -> checkedThumbColor
        AppSwitchState.Off -> thumbColor
        AppSwitchState.Disabled -> disabledThumbColor
    }
}

@Immutable
class AppSwitchStyle(
    val colors: AppSwitchColors,
    val trackWidth: Dp,
    val trackHeight: Dp,
    val trackShape: Shape,
    val borderWidth: Dp,
    val thumbSize: Dp,
    val checkedThumbSize: Dp,
    val rippleRadius: Dp,
    val disabledAlpha: Float,
    val animationDurationMillis: Int
) {
    val thumbInset: Dp get() = (trackHeight - thumbSize) / 2
    val checkedThumbInset: Dp get() = (trackHeight - checkedThumbSize) / 2
    val thumbCenter: Dp get() = thumbInset + thumbSize / 2
    val checkedThumbCenter: Dp get() = trackWidth - checkedThumbInset - checkedThumbSize / 2
}

object AppSwitchDefaults {
    val TrackWidth = 52.dp
    val TrackHeight = 32.dp
    val BorderWidth = 2.dp
    val ThumbSize = 16.dp
    val CheckedThumbSize = 24.dp
    val RippleRadius = 24.dp
    const val DisabledAlpha = 0.7f
    const val AnimationDurationMillis = 300

    @Composable
    fun style(): AppSwitchStyle {
        val colors = AppTheme.colors
        return remember(colors) { style(colors) }
    }

    fun style(colors: AppColors): AppSwitchStyle = AppSwitchStyle(
        colors = colors(colors),
        trackWidth = TrackWidth,
        trackHeight = TrackHeight,
        trackShape = CircleShape,
        borderWidth = BorderWidth,
        thumbSize = ThumbSize,
        checkedThumbSize = CheckedThumbSize,
        rippleRadius = RippleRadius,
        disabledAlpha = DisabledAlpha,
        animationDurationMillis = AnimationDurationMillis
    )

    fun colors(colors: AppColors): AppSwitchColors = AppSwitchColors(
        trackColor = colors.control.trackOff,
        checkedTrackColor = colors.action.primary,
        disabledTrackColor = colors.action.disabled,
        thumbColor = colors.control.thumbOff,
        checkedThumbColor = colors.control.thumb,
        disabledThumbColor = colors.icon.disabled,
        borderColor = colors.control.borderOff,
        checkedBorderColor = null,
        disabledBorderColor = null
    )
}

@Preview
@Composable
private fun SwitchPreviewDark() = AppSwitchPreview(isLight = false)

@Preview
@Composable
private fun SwitchPreviewLight() = AppSwitchPreview(isLight = true)

@Composable
private fun AppSwitchPreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppSwitch(checked = true, onCheckedChange = {})
                    AppSwitch(checked = false, onCheckedChange = {})
                    AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
                    AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
                }
            }
        }
    }
}
