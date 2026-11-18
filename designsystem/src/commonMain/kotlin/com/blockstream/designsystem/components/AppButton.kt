package com.blockstream.designsystem.components

import com.blockstream.designsystem.AppRadius
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.AppTypography
import com.blockstream.designsystem.foundation.ProvideAppContentColorTextStyle
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.LG,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    rippleEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    val iconSize = AppButtonDefaults.style(variant, size, rippleEnabled).iconSize
    AppButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        rippleEnabled = rippleEnabled,
        interactionSource = interactionSource
    ) {
        leadingIcon?.let { AppIcon(it, contentDescription = null, modifier = Modifier.size(iconSize)) }
        AppText(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        trailingIcon?.let { AppIcon(it, contentDescription = null, modifier = Modifier.size(iconSize)) }
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.LG,
    enabled: Boolean = true,
    rippleEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val style = AppButtonDefaults.style(variant, size, rippleEnabled)
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val state = AppButtonState.of(enabled = enabled, pressed = pressed && !style.rippleEnabled)

    AppButtonSurface(
        state = state,
        style = style,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

enum class AppButtonVariant {
    Primary,
    Secondary,
    Tertiary,
    PrimaryInverted,
    Negative,
    SecondaryInverted
}

enum class AppButtonSize {
    XS,
    SM,
    MD,
    LG
}

enum class AppButtonState {
    Default,
    Pressed,
    Disabled;

    companion object {
        fun of(enabled: Boolean, pressed: Boolean): AppButtonState = when {
            !enabled -> Disabled
            pressed -> Pressed
            else -> Default
        }
    }
}

@Immutable
class AppButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color?,
    val pressedContainerColor: Color,
    val pressedContentColor: Color,
    val pressedBorderColor: Color?,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val disabledBorderColor: Color?
) {
    @Stable
    fun containerColor(state: AppButtonState): Color = when (state) {
        AppButtonState.Default -> containerColor
        AppButtonState.Pressed -> pressedContainerColor
        AppButtonState.Disabled -> disabledContainerColor
    }

    @Stable
    fun contentColor(state: AppButtonState): Color = when (state) {
        AppButtonState.Default -> contentColor
        AppButtonState.Pressed -> pressedContentColor
        AppButtonState.Disabled -> disabledContentColor
    }

    @Stable
    fun borderColor(state: AppButtonState): Color? = when (state) {
        AppButtonState.Default -> borderColor
        AppButtonState.Pressed -> pressedBorderColor
        AppButtonState.Disabled -> disabledBorderColor
    }
}

@Immutable
class AppButtonStyle(
    val colors: AppButtonColors,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val iconSpacing: Dp,
    val rippleEnabled: Boolean
)

object AppButtonDefaults {
    val Shape: Shape = RoundedCornerShape(AppRadius.lg)
    val BorderWidth = 1.dp

    @Composable
    fun style(variant: AppButtonVariant, size: AppButtonSize, rippleEnabled: Boolean = true): AppButtonStyle {
        val colors = AppTheme.colors
        val typography = AppTheme.typography
        return remember(variant, size, rippleEnabled, colors, typography) { style(variant, size, rippleEnabled, colors, typography) }
    }

    fun style(variant: AppButtonVariant, size: AppButtonSize, rippleEnabled: Boolean, colors: AppColors, typography: AppTypography): AppButtonStyle {
        return when (size) {
            AppButtonSize.LG -> AppButtonStyle(
                colors = colors(variant, colors),
                shape = Shape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                textStyle = typography.label.lMedium,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                rippleEnabled = rippleEnabled
            )

            AppButtonSize.MD -> AppButtonStyle(
                colors = colors(variant, colors),
                shape = Shape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                textStyle = typography.label.lMedium,
                iconSize = 20.dp,
                iconSpacing = 8.dp,
                rippleEnabled = rippleEnabled
            )

            AppButtonSize.SM -> AppButtonStyle(
                colors = colors(variant, colors),
                shape = Shape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                textStyle = typography.label.mMedium,
                iconSize = 16.dp,
                iconSpacing = 4.dp,
                rippleEnabled = rippleEnabled
            )

            AppButtonSize.XS -> AppButtonStyle(
                colors = colors(variant, colors),
                shape = Shape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                textStyle = typography.label.mMedium,
                iconSize = 16.dp,
                iconSpacing = 4.dp,
                rippleEnabled = rippleEnabled
            )
        }
    }

    @Composable
    fun colors(variant: AppButtonVariant): AppButtonColors = colors(variant, AppTheme.colors)

    fun colors(variant: AppButtonVariant, colors: AppColors): AppButtonColors = when (variant) {
        AppButtonVariant.Primary -> primary(colors)
        AppButtonVariant.Secondary -> secondary(colors)
        AppButtonVariant.Tertiary -> tertiary(colors)
        AppButtonVariant.PrimaryInverted -> primaryInverted(colors)
        AppButtonVariant.Negative -> negative(colors)
        AppButtonVariant.SecondaryInverted -> secondaryInverted(colors)
    }

    fun primary(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = colors.action.primary,
            contentColor = colors.action.onPrimary,
            borderColor = null,
            pressedContainerColor = colors.action.pressed,
            pressedContentColor = colors.text.brand,
            pressedBorderColor = null,
            disabledContainerColor = colors.action.disabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }

    fun secondary(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.text.brand,
            borderColor = colors.border.brand,
            pressedContainerColor = colors.action.pressed,
            pressedContentColor = colors.text.brand,
            pressedBorderColor = colors.border.brand,
            disabledContainerColor = colors.action.disabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }

    fun tertiary(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.text.brand,
            borderColor = null,
            pressedContainerColor = colors.action.pressed,
            pressedContentColor = colors.text.brand,
            pressedBorderColor = null,
            disabledContainerColor = colors.action.disabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }

    fun primaryInverted(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = colors.action.primaryInverted,
            contentColor = colors.action.onPrimaryInverted,
            borderColor = null,
            pressedContainerColor = colors.action.primaryInvertedPressed,
            pressedContentColor = colors.action.onPrimaryInverted,
            pressedBorderColor = null,
            disabledContainerColor = colors.action.primaryInvertedDisabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }

    fun negative(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.feedback.negative.accent,
            borderColor = colors.feedback.negative.accent,
            pressedContainerColor = colors.feedback.negative.fill,
            pressedContentColor = colors.feedback.negative.accent,
            pressedBorderColor = colors.feedback.negative.accent,
            disabledContainerColor = colors.action.disabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }

    fun secondaryInverted(colors: AppColors): AppButtonColors {
        return AppButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.text.primary,
            borderColor = colors.border.inverse,
            pressedContainerColor = colors.action.primaryInvertedPressed,
            pressedContentColor = colors.action.onPrimary,
            pressedBorderColor = colors.border.inverse,
            disabledContainerColor = colors.action.primaryInvertedDisabled,
            disabledContentColor = colors.text.disabled,
            disabledBorderColor = null
        )
    }
}

@Composable
internal fun AppButtonSurface(
    state: AppButtonState,
    style: AppButtonStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val containerColor = style.colors.containerColor(state)
    val contentColor = style.colors.contentColor(state)
    val border = style.colors.borderColor(state)?.let { BorderStroke(AppButtonDefaults.BorderWidth, it) }

    val rowContent: @Composable () -> Unit = {
        ProvideAppContentColorTextStyle(contentColor = contentColor, textStyle = style.textStyle) {
            Row(
                modifier = Modifier.padding(style.contentPadding),
                horizontalArrangement = Arrangement.spacedBy(style.iconSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }

    if (onClick == null) {
        AppSurface(
            modifier = modifier,
            shape = style.shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            content = rowContent
        )
    } else {
        AppSurface(
            onClick = onClick,
            modifier = modifier.semantics { role = Role.Button },
            enabled = enabled,
            shape = style.shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            interactionSource = interactionSource,
            indication = if (style.rippleEnabled) appRipple() else null,
            content = rowContent
        )
    }
}

@Preview
@Composable
private fun ButtonPreviewDark() = AppButtonPreview(isLight = false)

@Preview
@Composable
private fun ButtonPreviewLight() = AppButtonPreview(isLight = true)

@Composable
private fun AppButtonPreview(isLight: Boolean) {
    com.blockstream.designsystem.ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButtonVariant.entries.forEach { variant ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButtonState.entries.forEach { state ->
                            AppButtonSurface(state = state, style = AppButtonDefaults.style(variant, AppButtonSize.MD)) {
                                AppText(state.name)
                            }
                        }
                    }
                }
                AppButton(text = "Leading icon", leadingIcon = HexagonIcon, size = AppButtonSize.SM, onClick = {})
            }
        }
    }
}
