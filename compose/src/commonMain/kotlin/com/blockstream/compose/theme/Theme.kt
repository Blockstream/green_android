package com.blockstream.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager

internal val GreenColors = ColorScheme(
    primary = md_theme_primary,
    onPrimary = md_theme_onPrimary,
    primaryContainer = md_theme_primaryContainer,
    onPrimaryContainer = md_theme_onPrimaryContainer,
    secondary = md_theme_secondary,
    onSecondary = md_theme_onSecondary,
    secondaryContainer = md_theme_secondaryContainer,
    onSecondaryContainer = md_theme_onSecondaryContainer,
    tertiary = md_theme_tertiary,
    onTertiary = md_theme_onTertiary,
    tertiaryContainer = md_theme_tertiaryContainer,
    onTertiaryContainer = md_theme_onTertiaryContainer,
    error = md_theme_error,
    errorContainer = md_theme_errorContainer,
    onError = md_theme_onError,
    onErrorContainer = md_theme_onErrorContainer,
    background = md_theme_background,
    onBackground = md_theme_onBackground,
    surface = md_theme_surface,
    onSurface = md_theme_onSurface,
    surfaceVariant = md_theme_surfaceVariant,
    onSurfaceVariant = md_theme_onSurfaceVariant,
    outline = md_theme_outline,
    inverseOnSurface = md_theme_inverseOnSurface,
    inverseSurface = md_theme_inverseSurface,
    inversePrimary = md_theme_inversePrimary,
    surfaceTint = md_theme_surfaceTint,
    outlineVariant = md_theme_outlineVariant,
    scrim = md_theme_scrim,

    surfaceBright = md_theme_surface,
    surfaceDim = md_theme_surface,
    surfaceContainer = md_theme_surface,
    surfaceContainerHigh = md_theme_surface,
    surfaceContainerHighest = md_theme_surface,
    surfaceContainerLow = md_theme_surface,
    surfaceContainerLowest = md_theme_surface,
)

internal val GreenColorsLight = GreenColors.copy(
    background = Color.White,
    onBackground = Color.Black
)

val GreenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val GreenSmallTop =
    RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
val GreenSmallBottom =
    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val GreenSmallStart =
    RoundedCornerShape(topStart = 0.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 4.dp)
val GreenSmallEnd =
    RoundedCornerShape(topStart = 4.dp, topEnd = 0.dp, bottomStart = 4.dp, bottomEnd = 0.dp)

expect @Composable
fun GreenChrome(isLight: Boolean = false)

@Composable
fun GreenChromePreview(
    content: @Composable () -> Unit
) {
    val platformManager = rememberPlatformManager()

    CompositionLocalProvider(
        LocalPlatformManager provides platformManager
    ) {
        GreenTheme(content = content)
    }
}

@Composable
fun GreenTheme(isLight: Boolean = false, content: @Composable () -> Unit) {

    val colorScheme = if (isLight) GreenColorsLight else GreenColors

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = GreenShapes,
        typography = GreenTypography(),
        content = content
    )
}