package com.blockstream.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val md_theme_primary = Color(0xFF00BCFF)
val md_theme_onPrimary = Color(0xFF0A0A0A)
val md_theme_primaryContainer = Color(0xFF00BCFF)
val md_theme_onPrimaryContainer = Color(0xFF0A0A0A)

val md_theme_secondary = Color(0xFF00BCFF)
val md_theme_onSecondary = Color(0xFF0A0A0A)
val md_theme_secondaryContainer = Color(0xFF525252)
val md_theme_onSecondaryContainer = Color(0xFFFFFFFF)

val md_theme_tertiary = Color(0xFF00BCFF)
val md_theme_onTertiary = Color(0xFF0A0A0A)
val md_theme_tertiaryContainer = Color(0xFF00BCFF)
val md_theme_onTertiaryContainer = Color(0xFF0A0A0A)

val md_theme_error = Color(0xFFCB1D36)
val md_theme_errorContainer = Color(0xFF9A0000)
val md_theme_onError = Color(0xFF690005)
val md_theme_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_background = Color(0xFF101115)
val md_theme_backgroundVariant = Color(0xFF0E1016)
val md_theme_onBackground = Color(0xFFFFFFFF)

val md_theme_surface = Color(0xFF181818) // card, text fields
val md_theme_onSurface = Color(0xFFFFFFFF)
val md_theme_surfaceVariant = Color(0xFF232323)
val md_theme_onSurfaceVariant = Color(0xFFC1C9BE)

val md_theme_outline = Color(0xFF262626)
val md_theme_inverseOnSurface = Color(0xFFEEF0FF)
val md_theme_inverseSurface = Color(0xFF273545)
val md_theme_inversePrimary = Color(0xFF00BCFF)
val md_theme_shadow = Color(0xFF000000)
val md_theme_surfaceTint = Color(0xFF222226) // Make it the same as surface https://stackoverflow.com/a/76160786/914358
val md_theme_outlineVariant = Color(0xFF262626)
val md_theme_scrim = Color(0xFF000000)

val md_theme_brandSurface = Color(0xFF19222c)

val bottom_nav_bg = Color(0xFF2E3033)

val green = md_theme_primary
val green20 = Color(0x3300f113)

val red = Color(0xFFEA5262)
val redDark = Color(0xFF9A0000)
val orange = Color(0xFFBB7B00)

val orangeSurface = Color(0xFF432004)
val orangeOutline = Color(0xFF7e2a0e)

val blueSurface = Color(0xFF062f4a)
val blueOutline = Color(0xFF034a71)

val md_theme_surfaceCircle = Color(0xFF363636)

val whiteHigh @Composable
    get() = MaterialTheme.colorScheme.onSurface
val whiteMedium @Composable
    get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
val whiteLow @Composable
    get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

val bitcoin = Color(0xFFfe8e02)
val liquid = Color(0xFF46BEAE)
val lightning = Color(0xFFDFB316)
val bitcoin_testnet = Color(0xFF8C8C8C)
val liquid_testnet = Color(0xFF9ab8b4)
val amp = Color(0xFF51AAB0)
val amp_testnet = Color(0xFF7fadb0)


