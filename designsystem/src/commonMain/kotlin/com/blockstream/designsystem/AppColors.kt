package com.blockstream.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class AppColors(
    val text: Text,
    val surface: Surface,
    val border: Border,
    val icon: Icon,
    val action: Action,
    val feedback: Feedback,
    val overlay: Overlay
) {
    @Immutable
    class Text(
        val brand: Color,
        val brandHover: Color,
        val disabled: Color,
        val inverse: Color,
        val primary: Color,
        val subtle: Color
    )

    @Immutable
    class Surface(
        val base: Color,
        val canvas: Color,
        val overlay: Color,
        val raised: Color,
        val scrim: Color,
        val sunken: Color,
        val segmented: Segmented
    ) {
        @Immutable
        class Segmented(
            val selected: Color
        )
    }

    @Immutable
    class Border(
        val bold: Color,
        val brand: Color,
        val inverse: Color,
        val subtle: Color
    )

    @Immutable
    class Icon(
        val base: Color,
        val brand: Color,
        val disabled: Color,
        val inverse: Color,
        val subtle: Color
    )

    @Immutable
    class Action(
        val disabled: Color,
        val onPrimary: Color,
        val onPrimaryInverted: Color,
        val pressed: Color,
        val primary: Color,
        val primaryInverted: Color,
        val primaryInvertedDisabled: Color,
        val primaryInvertedPressed: Color
    )

    @Immutable
    class Feedback(
        val info: Role,
        val negative: Role,
        val positive: Role,
        val warning: Role
    ) {
        @Immutable
        class Role(
            val accent: Color,
            val fill: Color,
            val onFill: Color,
            val stroke: Color
        )
    }

    @Immutable
    class Overlay(
        val scrim: Color
    )
}

val AppColorsDark = AppColors(
    text = AppColors.Text(
        brand = Color(0xFF00BCFF),
        brandHover = Color(0xFF4FD1FF),
        disabled = Color(0xFF525252),
        inverse = Color(0xFF0A0A0A),
        primary = Color(0xFFFAFAFA),
        subtle = Color(0xFFA0A0A0)
    ),
    surface = AppColors.Surface(
        base = Color(0xFF181818),
        canvas = Color(0xFF121416),
        overlay = Color(0xFF262626),
        raised = Color(0xFF262626),
        scrim = Color(0xCC181818),
        sunken = Color(0xFF0A0A0A),
        segmented = AppColors.Surface.Segmented(selected = Color(0xFF404040))
    ),
    border = AppColors.Border(
        bold = Color(0xFF404040),
        brand = Color(0xFF00BCFF),
        inverse = Color(0xFFFFFFFF),
        subtle = Color(0xFF262626)
    ),
    icon = AppColors.Icon(
        base = Color(0xFFFAFAFA),
        brand = Color(0xFF00BCFF),
        disabled = Color(0xFF525252),
        inverse = Color(0xFF0A0A0A),
        subtle = Color(0xFFA0A0A0)
    ),
    action = AppColors.Action(
        disabled = Color(0xFF262626),
        onPrimary = Color(0xFF0A0A0A),
        onPrimaryInverted = Color(0xFF0A0A0A),
        pressed = Color(0xFF062F4A),
        primary = Color(0xFF00BCFF),
        primaryInverted = Color(0xFFFFFFFF),
        primaryInvertedDisabled = Color(0xFF262626),
        primaryInvertedPressed = Color(0xFFD4D4D4)
    ),
    feedback = AppColors.Feedback(
        info = AppColors.Feedback.Role(
            accent = Color(0xFF00BCFF),
            fill = Color(0xFF062F4A),
            onFill = Color(0xFFFFFFFF),
            stroke = Color(0xFF004A70)
        ),
        negative = AppColors.Feedback.Role(
            accent = Color(0xFFFF6467),
            fill = Color(0xFF460708),
            onFill = Color(0xFFFFFFFF),
            stroke = Color(0xFF82181A)
        ),
        positive = AppColors.Feedback.Role(
            accent = Color(0xFF00C60D),
            fill = Color(0xFF004301),
            onFill = Color(0xFFFFFFFF),
            stroke = Color(0xFF007B05)
        ),
        warning = AppColors.Feedback.Role(
            accent = Color(0xFFFDC700),
            fill = Color(0xFF432004),
            onFill = Color(0xFFFFFFFF),
            stroke = Color(0xFF7E2A0D)
        )
    ),
    overlay = AppColors.Overlay(
        scrim = Color(0xFF181818)
    )
)

val AppColorsLight = AppColors(
    text = AppColors.Text(
        brand = Color(0xFF0069A8),
        brandHover = Color(0xFF00598A),
        disabled = Color(0xFFA0A0A0),
        inverse = Color(0xFFFAFAFA),
        primary = Color(0xFF0A0A0A),
        subtle = Color(0xFF525252)
    ),
    surface = AppColors.Surface(
        base = Color(0xFFF0F0F0),
        canvas = Color(0xFFF5F5F5),
        overlay = Color(0xFFFFFFFF),
        raised = Color(0xFFFFFFFF),
        scrim = Color(0xCCF5F5F5),
        sunken = Color(0xFFFAFAFA),
        segmented = AppColors.Surface.Segmented(selected = Color(0xFFE5E5E5))
    ),
    border = AppColors.Border(
        bold = Color(0xFFD4D4D4),
        brand = Color(0xFF0084D1),
        inverse = Color(0xFF0A0A0A),
        subtle = Color(0xFFE5E5E5)
    ),
    icon = AppColors.Icon(
        base = Color(0xFF0A0A0A),
        brand = Color(0xFF0084D1),
        disabled = Color(0xFFA0A0A0),
        inverse = Color(0xFFFAFAFA),
        subtle = Color(0xFF525252)
    ),
    action = AppColors.Action(
        disabled = Color(0xFFE5E5E5),
        onPrimary = Color(0xFFFAFAFA),
        onPrimaryInverted = Color(0xFFFAFAFA),
        pressed = Color(0xFFE0F2FE),
        primary = Color(0xFF0069A8),
        primaryInverted = Color(0xFF0A0A0A),
        primaryInvertedDisabled = Color(0xFFE5E5E5),
        primaryInvertedPressed = Color(0xFF404040)
    ),
    feedback = AppColors.Feedback(
        info = AppColors.Feedback.Role(
            accent = Color(0xFF0069A8),
            fill = Color(0xFFB8E6FF),
            onFill = Color(0xFF00598A),
            stroke = Color(0xFF00BCFF)
        ),
        negative = AppColors.Feedback.Role(
            accent = Color(0xFF82181A),
            fill = Color(0xFFFFD3D3),
            onFill = Color(0xFF82181A),
            stroke = Color(0xFFFF6467)
        ),
        positive = AppColors.Feedback.Role(
            accent = Color(0xFF00A609),
            fill = Color(0xFFCBEACC),
            onFill = Color(0xFF007B05),
            stroke = Color(0xFF00C60D)
        ),
        warning = AppColors.Feedback.Role(
            accent = Color(0xFFCB9407),
            fill = Color(0xFFFEEBCB),
            onFill = Color(0xFF7E2A0D),
            stroke = Color(0xFFFDC700)
        )
    ),
    overlay = AppColors.Overlay(
        scrim = Color(0xFFF5F5F5)
    )
)
