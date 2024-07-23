package com.blockstream.compose.theme


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.inter_bold
import blockstream_green.common.generated.resources.inter_extra_light
import blockstream_green.common.generated.resources.inter_regular
import blockstream_green.common.generated.resources.inter_thin
import blockstream_green.common.generated.resources.monospace_bold
import blockstream_green.common.generated.resources.monospace_regular
import org.jetbrains.compose.resources.Font

@Composable
fun InterFont() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_thin, FontWeight.Thin),
    Font(Res.font.inter_extra_light, FontWeight.ExtraLight),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
@Stable
fun MonospaceFont() = FontFamily(
    Font(Res.font.monospace_regular, FontWeight.Normal),
    Font(Res.font.monospace_bold, FontWeight.Bold),
)

@Composable
fun copyTextStyle(textStyle: TextStyle, fontSize: TextUnit = textStyle.fontSize, fontWeight: FontWeight? = textStyle.fontWeight, lineHeight: TextUnit = textStyle.lineHeight ): TextStyle = textStyle.copy(
    fontFamily = InterFont(),
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight
)

@Composable
fun GreenTypography() = Typography().let {
    Typography(
        displayLarge = copyTextStyle(it.displayLarge, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp),
        displayMedium = copyTextStyle(it.displayMedium, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
        displaySmall = copyTextStyle(it.displaySmall, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),

        headlineLarge = copyTextStyle(it.headlineLarge, fontSize = 26.sp, fontWeight = FontWeight.Bold),
        headlineMedium = copyTextStyle(it.headlineMedium, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        headlineSmall = copyTextStyle(it.headlineSmall, fontSize = 22.sp, fontWeight = FontWeight.Bold),

        titleLarge = copyTextStyle(it.titleLarge, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleMedium = copyTextStyle(it.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        titleSmall = copyTextStyle(it.titleSmall, fontSize = 16.sp, fontWeight = FontWeight.Bold),

        bodyLarge = copyTextStyle(it.bodyLarge, fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = copyTextStyle(it.bodyMedium, fontSize = 12.sp, lineHeight = 16.sp),
        bodySmall = copyTextStyle(it.bodySmall, fontSize = 11.sp, lineHeight = 14.sp),

        labelLarge = copyTextStyle(it.labelLarge, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        labelMedium = copyTextStyle(it.labelMedium, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        labelSmall = copyTextStyle(it.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold),
    )
}

val displayLarge
    @Composable
    get() = MaterialTheme.typography.displayLarge

val displayMedium
    @Composable
    get() = MaterialTheme.typography.displayMedium

val displaySmall
    @Composable
    get() = MaterialTheme.typography.displaySmall

val headlineLarge
    @Composable
    get() = MaterialTheme.typography.headlineLarge

val headlineMedium
    @Composable
    get() = MaterialTheme.typography.headlineMedium

val headlineSmall
    @Composable
    get() = MaterialTheme.typography.headlineSmall

val titleLarge
    @Composable
    get() = MaterialTheme.typography.titleLarge

val titleMedium
    @Composable
    get() = MaterialTheme.typography.titleMedium

val titleSmall
    @Composable
    get() = MaterialTheme.typography.titleSmall

val bodyLarge
    @Composable
    get() = MaterialTheme.typography.bodyLarge

val bodyMedium
    @Composable
    get() = MaterialTheme.typography.bodyMedium

@Deprecated("Use bodyMedium instead")
val bodySmall
    @Composable
    get() = MaterialTheme.typography.bodySmall

val labelLarge
    @Composable
    get() = MaterialTheme.typography.labelLarge

val labelMedium
    @Composable
    get() = MaterialTheme.typography.labelMedium

val labelSmall
    @Composable
    get() = MaterialTheme.typography.labelSmall