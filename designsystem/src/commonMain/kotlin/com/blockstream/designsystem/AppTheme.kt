package com.blockstream.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf { AppColorsDark }

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography is not provided, wrap the content in ProvideAppTheme")
}

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
fun ProvideAppTheme(isLight: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides if (isLight) AppColorsLight else AppColorsDark,
        LocalAppTypography provides rememberAppTypography(),
        content = content
    )
}
