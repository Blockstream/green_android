package com.blockstream.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    val style = AppRadioDefaults.style()
    val state = AppRadioState.of(enabled = enabled, selected = selected)
    val dotColor = style.colors.dotColor(state)
    val progress = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = style.dotDamping, stiffness = style.dotStiffness)
    )
    val semantics = if (onClick != null) {
        Modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = appRipple(bounded = false, radius = style.rippleRadius),
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        )
    } else {
        Modifier.semantics {
            if (!enabled) disabled()
            role = Role.RadioButton
            this.selected = selected
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
                .size(style.ringSize)
                .border(style.borderWidth, style.colors.borderColor(state), CircleShape)
                .drawBehind {
                    val p = progress.value
                    val r = (style.dotSize.toPx() / 2f * (style.dotStartScale + (1f - style.dotStartScale) * p)).coerceAtLeast(0f)
                    if (r > 0f) drawCircle(dotColor.copy(alpha = dotColor.alpha * p.coerceIn(0f, 1f)), radius = r)
                }
        )
    }
}

enum class AppRadioState {
    Selected,
    Unselected,
    Disabled;

    companion object {
        fun of(enabled: Boolean, selected: Boolean): AppRadioState = when {
            !enabled -> Disabled
            selected -> Selected
            else -> Unselected
        }
    }
}

@Immutable
class AppRadioColors(
    val borderColor: Color,
    val selectedBorderColor: Color,
    val disabledBorderColor: Color,
    val dotColor: Color,
    val disabledDotColor: Color
) {
    @Stable
    fun borderColor(state: AppRadioState): Color = when (state) {
        AppRadioState.Selected -> selectedBorderColor
        AppRadioState.Unselected -> borderColor
        AppRadioState.Disabled -> disabledBorderColor
    }

    @Stable
    fun dotColor(state: AppRadioState): Color =
        if (state == AppRadioState.Disabled) disabledDotColor else dotColor
}

@Immutable
class AppRadioStyle(
    val colors: AppRadioColors,
    val size: Dp,
    val ringSize: Dp,
    val borderWidth: Dp,
    val dotSize: Dp,
    val dotStartScale: Float,
    val dotDamping: Float,
    val dotStiffness: Float,
    val rippleRadius: Dp,
    val disabledAlpha: Float
)

object AppRadioDefaults {
    val Size = 48.dp
    val RingSize = 20.dp
    val BorderWidth = 1.dp
    val DotSize = 10.dp
    val RippleRadius = 24.dp
    const val DotStartScale = 0.5f
    const val DotDamping = 0.6f
    const val DotStiffness = 400f
    const val DisabledAlpha = 0.5f

    @Composable
    fun style(): AppRadioStyle {
        val colors = AppTheme.colors
        return remember(colors) { style(colors) }
    }

    fun style(colors: AppColors): AppRadioStyle = AppRadioStyle(
        colors = colors(colors),
        size = Size,
        ringSize = RingSize,
        borderWidth = BorderWidth,
        dotSize = DotSize,
        dotStartScale = DotStartScale,
        dotDamping = DotDamping,
        dotStiffness = DotStiffness,
        rippleRadius = RippleRadius,
        disabledAlpha = DisabledAlpha
    )

    fun colors(colors: AppColors): AppRadioColors = AppRadioColors(
        borderColor = colors.border.bold,
        selectedBorderColor = colors.border.brand,
        disabledBorderColor = colors.border.bold,
        dotColor = colors.icon.brand,
        disabledDotColor = colors.icon.disabled
    )
}

@Preview
@Composable
private fun RadioPreviewDark() = AppRadioPreview(isLight = false)

@Preview
@Composable
private fun RadioPreviewLight() = AppRadioPreview(isLight = true)

@Composable
private fun AppRadioPreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppRadio(selected = false, onClick = {})
                    AppRadio(selected = true, onClick = {})
                    AppRadio(selected = false, onClick = {}, enabled = false)
                    AppRadio(selected = true, onClick = {}, enabled = false)
                }
            }
        }
    }
}
