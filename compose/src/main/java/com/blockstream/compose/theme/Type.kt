package com.blockstream.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.blockstream.compose.R

val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extra_light, FontWeight.ExtraLight),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val monospaceFont = FontFamily(
    Font(R.font.monospace_regular, FontWeight.Normal),
    Font(R.font.monospace_bold, FontWeight.Bold),
)

fun copyTextStyle(textStyle: TextStyle, fontSize: TextUnit = textStyle.fontSize, fontWeight: FontWeight? = textStyle.fontWeight, lineHeight: TextUnit = textStyle.lineHeight ): TextStyle = textStyle.copy(
    fontFamily = InterFont,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight
)

val Typography = Typography().let {
    Typography(
        displayLarge = copyTextStyle(it.displayLarge, fontSize = 32.sp),
        displayMedium = copyTextStyle(it.displayMedium, fontSize = 30.sp),
        displaySmall = copyTextStyle(it.displaySmall, fontSize = 28.sp),

        headlineLarge = copyTextStyle(it.headlineLarge, fontSize = 26.sp, fontWeight = FontWeight.Bold),
        headlineMedium = copyTextStyle(it.headlineMedium, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        headlineSmall = copyTextStyle(it.headlineSmall, fontSize = 22.sp, fontWeight = FontWeight.Bold),

        titleLarge = copyTextStyle(it.titleLarge, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleMedium = copyTextStyle(it.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        titleSmall = copyTextStyle(it.titleSmall, fontSize = 16.sp, fontWeight = FontWeight.Bold),

        bodyLarge = copyTextStyle(it.bodyLarge, fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = copyTextStyle(it.bodyMedium, fontSize = 12.sp, lineHeight = 16.sp),
        bodySmall = copyTextStyle(it.bodySmall, fontSize = 10.sp, lineHeight = 14.sp),

        labelLarge = copyTextStyle(it.labelLarge, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        labelMedium = copyTextStyle(it.labelMedium, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        labelSmall = copyTextStyle(it.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold),
    )
}

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