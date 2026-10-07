package com.blockstream.designsystem

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

val LocalAppColors = staticCompositionLocalOf { AppColorsDark }

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography is not provided, wrap the content in ProvideAppTheme")
}

val LocalAppContentColor = compositionLocalOf { Color.Black }

val LocalAppTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TextStyle.Default }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

@Composable
@ReadOnlyComposable
fun appContentColorFor(backgroundColor: Color): Color =
    AppTheme.colors.appContentColorFor(backgroundColor).takeIf { it != Color.Unspecified } ?: LocalAppContentColor.current

fun AppColors.appContentColorFor(backgroundColor: Color): Color = when (backgroundColor) {
    surface.canvas, surface.base, surface.raised, surface.sunken, surface.overlay -> text.primary
    feedback.info.fill -> feedback.info.onFill
    feedback.positive.fill -> feedback.positive.onFill
    feedback.negative.fill -> feedback.negative.onFill
    feedback.warning.fill -> feedback.warning.onFill
    action.primary -> action.onPrimary
    action.primaryInverted -> action.onPrimaryInverted
    else -> Color.Unspecified
}

@Composable
fun ProvideAppTheme(isLight: Boolean, content: @Composable () -> Unit) {
    val colors = if (isLight) AppColorsLight else AppColorsDark
    val typography = rememberAppTypography()
    val selectionColors = remember(colors.action.primary) {
        TextSelectionColors(
            handleColor = colors.action.primary,
            backgroundColor = colors.action.primary.copy(alpha = TextSelectionBackgroundOpacity)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppContentColor provides colors.text.primary,
        LocalAppTextStyle provides typography.body.m,
        LocalTextSelectionColors provides selectionColors,
        content = content
    )
}

private const val TextSelectionBackgroundOpacity = 0.4f
