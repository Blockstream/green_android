package com.blockstream.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.blockstream.designsystem.generated.resources.Res
import com.blockstream.designsystem.generated.resources.inter_bold
import com.blockstream.designsystem.generated.resources.inter_medium
import com.blockstream.designsystem.generated.resources.inter_regular
import com.blockstream.designsystem.generated.resources.inter_semibold
import com.blockstream.designsystem.generated.resources.roboto_mono_regular
import org.jetbrains.compose.resources.Font

@Immutable
class AppTypography(
    val body: Body,
    val label: Label,
    val title: Title,
    val code: Code,
    val overline: Overline,
    val display: Display
) {
    @Immutable
    class Body(
        val s: TextStyle,
        val m: TextStyle,
        val l: TextStyle
    )

    @Immutable
    class Label(
        val xs: TextStyle,
        val sm: TextStyle,
        val smSemiBold: TextStyle,
        val m: TextStyle,
        val mMedium: TextStyle,
        val mSemiBold: TextStyle,
        val l: TextStyle,
        val lMedium: TextStyle,
        val lSemiBold: TextStyle
    )

    @Immutable
    class Title(
        val xsBold: TextStyle,
        val sBold: TextStyle,
        val sSemiBold: TextStyle,
        val m: TextStyle,
        val mSemiBold: TextStyle,
        val lMedium: TextStyle,
        val lSemiBold: TextStyle,
        val lBold: TextStyle,
        val xl: TextStyle,
        val xlMedium: TextStyle,
        val xlSemiBold: TextStyle,
        val xxlBold: TextStyle
    )

    @Immutable
    class Code(
        val s: TextStyle,
        val m: TextStyle
    )

    // Overline text is shown in capitals. TextStyle cannot change case, so call uppercase() on the text.
    @Immutable
    class Overline(
        val xs: TextStyle,
        val s: TextStyle,
        val sSemiBold: TextStyle
    )

    @Immutable
    class Display(
        val sMedium: TextStyle,
        val mMedium: TextStyle,
        val mSemiBold: TextStyle,
        val lSemiBold: TextStyle,
        val xlMedium: TextStyle
    )
}

// Kept apart from InterFont() so that screens asking for Medium or SemiBold today keep their current look.
@Composable
private fun appInterFontFamily(): FontFamily {
    val regular = Font(Res.font.inter_regular, FontWeight.Normal)
    val medium = Font(Res.font.inter_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.inter_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.inter_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) { FontFamily(regular, medium, semiBold, bold) }
}

@Composable
private fun appMonoFontFamily(): FontFamily {
    val regular = Font(Res.font.roboto_mono_regular, FontWeight.Normal)
    return remember(regular) { FontFamily(regular) }
}

@Composable
fun rememberAppTypography(): AppTypography {
    val inter = appInterFontFamily()
    val mono = appMonoFontFamily()
    return remember(inter, mono) { appTypography(inter = inter, mono = mono) }
}

private fun appTypography(inter: FontFamily, mono: FontFamily): AppTypography {
    fun style(
        family: FontFamily,
        weight: FontWeight,
        size: Int,
        lineHeight: Int,
        letterSpacing: TextUnit = 0.sp
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    val regular = FontWeight.Normal
    val medium = FontWeight.Medium
    val semiBold = FontWeight.SemiBold
    val bold = FontWeight.Bold

    return AppTypography(
        body = AppTypography.Body(
            s = style(inter, regular, 12, 16),
            m = style(inter, regular, 14, 20),
            l = style(inter, regular, 16, 24)
        ),
        label = AppTypography.Label(
            xs = style(inter, regular, 10, 14),
            sm = style(inter, regular, 12, 16),
            smSemiBold = style(inter, semiBold, 12, 16),
            m = style(inter, regular, 14, 20),
            mMedium = style(inter, medium, 14, 20),
            mSemiBold = style(inter, semiBold, 14, 20),
            l = style(inter, regular, 16, 24),
            lMedium = style(inter, medium, 16, 24),
            lSemiBold = style(inter, semiBold, 16, 24)
        ),
        title = AppTypography.Title(
            xsBold = style(inter, bold, 16, 20),
            sBold = style(inter, bold, 18, 22),
            sSemiBold = style(inter, semiBold, 18, 22),
            m = style(inter, regular, 20, 24),
            mSemiBold = style(inter, semiBold, 20, 24),
            lMedium = style(inter, medium, 24, 28),
            lSemiBold = style(inter, semiBold, 24, 28),
            lBold = style(inter, bold, 24, 28),
            xl = style(inter, regular, 28, 32),
            xlMedium = style(inter, medium, 28, 32),
            xlSemiBold = style(inter, semiBold, 28, 32),
            xxlBold = style(inter, bold, 32, 36)
        ),
        code = AppTypography.Code(
            s = style(mono, regular, 12, 16),
            m = style(mono, regular, 14, 18)
        ),
        overline = AppTypography.Overline(
            xs = style(inter, regular, 10, 14, letterSpacing = 1.sp),
            s = style(inter, regular, 12, 16, letterSpacing = 1.2.sp),
            sSemiBold = style(inter, semiBold, 12, 16, letterSpacing = 1.2.sp)
        ),
        display = AppTypography.Display(
            sMedium = style(inter, medium, 20, 24),
            mMedium = style(inter, medium, 28, 32),
            mSemiBold = style(inter, semiBold, 28, 32),
            lSemiBold = style(inter, semiBold, 34, 40),
            xlMedium = style(inter, medium, 40, 40)
        )
    )
}
