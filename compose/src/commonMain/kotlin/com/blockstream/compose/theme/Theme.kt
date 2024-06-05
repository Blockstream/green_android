package com.blockstream.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
)

val GreenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
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
fun GreenTheme(content: @Composable () -> Unit)

@Composable
fun GreenThemePreview(
    content: @Composable () -> Unit
) {
    val platformManager = rememberPlatformManager()

    CompositionLocalProvider(
        androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current, // Until Compose 1.7.0 is released https://stackoverflow.com/questions/78490378/java-lang-illegalstateexception-compositionlocal-locallifecycleowner-not-presen/78490602#78490602
        LocalPlatformManager provides platformManager
    ) {
        MaterialTheme(
            colorScheme = GreenColors,
            shapes = GreenShapes,
            typography = GreenTypography()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    }
}