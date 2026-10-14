package com.blockstream.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppCheckboxVariant = AppCheckboxVariant.Default,
    interactionSource: MutableInteractionSource? = null
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val style = AppCheckboxDefaults.style(variant)
    val state = AppCheckboxState.of(enabled = enabled, checked = checked)
    val checkProgress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = style.checkDamping, stiffness = style.checkStiffness)
    )
    val semantics = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Checkbox,
            interactionSource = interactions,
            indication = appRipple(bounded = false, radius = style.rippleRadius)
        )
    } else {
        Modifier.semantics {
            if (!enabled) disabled()
            role = Role.Checkbox
            toggleableState = ToggleableState(checked)
        }
    }

    Box(
        modifier = modifier
            .then(semantics)
            .size(style.size)
            .alpha(if (enabled) 1f else style.disabledAlpha),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(style.boxSize)
                .border(style.borderWidth, style.colors.borderColor(state), style.shape),
            contentAlignment = Alignment.Center
        ) {
            CheckMark(style, style.colors.checkColor(state), checkProgress)
        }
    }
}

@Composable
private fun CheckMark(style: AppCheckboxStyle, color: Color, progress: State<Float>) {
    val path = remember { Path() }
    Canvas(
        modifier = Modifier
            .size(style.checkSize)
            .graphicsLayer {
                val scale = style.checkStartScale + (1f - style.checkStartScale) * progress.value
                scaleX = scale
                scaleY = scale
                alpha = progress.value.coerceIn(0f, 1f)
            }
    ) {
        val unit = size.width / 12f
        path.rewind()
        path.moveTo(1.75f * unit, 6f * unit)
        path.lineTo(4.625f * unit, 8.875f * unit)
        path.lineTo(10.25f * unit, 3.125f * unit)
        drawPath(path, color, style = Stroke(width = style.checkStrokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

enum class AppCheckboxVariant {
    Default,
    List
}

enum class AppCheckboxState {
    Checked,
    Unchecked,
    Disabled;

    companion object {
        fun of(enabled: Boolean, checked: Boolean): AppCheckboxState = when {
            !enabled -> Disabled
            checked -> Checked
            else -> Unchecked
        }
    }
}

@Immutable
class AppCheckboxColors(
    val borderColor: Color,
    val checkedBorderColor: Color,
    val disabledBorderColor: Color,
    val checkColor: Color,
    val disabledCheckColor: Color
) {
    @Stable
    fun borderColor(state: AppCheckboxState): Color = when (state) {
        AppCheckboxState.Checked -> checkedBorderColor
        AppCheckboxState.Unchecked -> borderColor
        AppCheckboxState.Disabled -> disabledBorderColor
    }

    @Stable
    fun checkColor(state: AppCheckboxState): Color =
        if (state == AppCheckboxState.Disabled) disabledCheckColor else checkColor
}

@Immutable
class AppCheckboxStyle(
    val colors: AppCheckboxColors,
    val size: Dp,
    val boxSize: Dp,
    val shape: Shape,
    val borderWidth: Dp,
    val checkSize: Dp,
    val checkStrokeWidth: Dp,
    val checkStartScale: Float,
    val checkDamping: Float,
    val checkStiffness: Float,
    val rippleRadius: Dp,
    val disabledAlpha: Float
)

object AppCheckboxDefaults {
    val Size = 48.dp
    val BoxSize = 20.dp
    val Shape: Shape = RoundedCornerShape(4.dp)
    val BorderWidth = 1.dp
    val CheckSize = 12.dp
    val CheckStrokeWidth = 1.5.dp
    val RippleRadius = 24.dp
    const val CheckStartScale = 0.5f
    const val CheckDamping = 0.6f
    const val CheckStiffness = 400f
    const val DisabledAlpha = 0.5f

    @Composable
    fun style(variant: AppCheckboxVariant = AppCheckboxVariant.Default): AppCheckboxStyle {
        val colors = AppTheme.colors
        return remember(variant, colors) { style(variant, colors) }
    }

    fun style(variant: AppCheckboxVariant, colors: AppColors): AppCheckboxStyle = AppCheckboxStyle(
        colors = colors(variant, colors),
        size = Size,
        boxSize = BoxSize,
        shape = Shape,
        borderWidth = BorderWidth,
        checkSize = CheckSize,
        checkStrokeWidth = CheckStrokeWidth,
        checkStartScale = CheckStartScale,
        checkDamping = CheckDamping,
        checkStiffness = CheckStiffness,
        rippleRadius = RippleRadius,
        disabledAlpha = DisabledAlpha
    )

    fun colors(variant: AppCheckboxVariant, colors: AppColors): AppCheckboxColors = AppCheckboxColors(
        borderColor = when (variant) {
            AppCheckboxVariant.Default -> colors.border.brand
            AppCheckboxVariant.List -> colors.border.bold
        },
        checkedBorderColor = colors.border.brand,
        disabledBorderColor = colors.border.bold,
        checkColor = colors.icon.brand,
        disabledCheckColor = colors.icon.disabled
    )
}

@Preview
@Composable
private fun CheckboxPreviewDark() = AppCheckboxPreview(isLight = false)

@Preview
@Composable
private fun CheckboxPreviewLight() = AppCheckboxPreview(isLight = true)

@Composable
private fun AppCheckboxPreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppCheckbox(checked = false, onCheckedChange = {})
                    AppCheckbox(checked = true, onCheckedChange = {})
                    AppCheckbox(checked = false, onCheckedChange = {}, enabled = false)
                    AppCheckbox(checked = true, onCheckedChange = {}, enabled = false)
                    AppCheckbox(checked = false, onCheckedChange = {}, variant = AppCheckboxVariant.List)
                }
            }
        }
    }
}
