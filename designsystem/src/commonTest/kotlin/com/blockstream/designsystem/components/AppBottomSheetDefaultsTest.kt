package com.blockstream.designsystem.components

import androidx.compose.ui.graphics.Color
import com.blockstream.designsystem.AppColorsDark
import com.blockstream.designsystem.AppColorsLight
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppBottomSheetDefaultsTest {

    @Test
    fun scrimAlphaRoundsToFigmaByte() {
        assertEquals(0x41, (AppBottomSheetDefaults.ScrimAlpha * 255).roundToInt())
    }

    @Test
    fun modalDarkMatchesFigma() {
        val colors = AppBottomSheetDefaults.style(AppBottomSheetType.Modal, AppColorsDark).colors
        assertEquals(Color(0xFF262626), colors.containerColor)
        assertEquals(Color(0xFF404040), colors.handleColor)
        assertEquals(Color(0x41181818), colors.scrimColor)
        assertEquals(Color(0xFFFAFAFA), colors.contentColor)
    }

    @Test
    fun modalLightMatchesTokens() {
        val colors = AppBottomSheetDefaults.colors(AppBottomSheetType.Modal, AppColorsLight)
        assertEquals(Color(0xFFFFFFFF), colors.containerColor)
        assertEquals(Color(0xFFD4D4D4), colors.handleColor)
        assertEquals(Color(0x41F5F5F5), colors.scrimColor)
        assertEquals(Color(0xFF0A0A0A), colors.contentColor)
    }

    @Test
    fun defaultTypeHasTransparentScrim() {
        listOf(AppColorsDark, AppColorsLight).forEach { theme ->
            val modal = AppBottomSheetDefaults.colors(AppBottomSheetType.Modal, theme)
            val default = AppBottomSheetDefaults.colors(AppBottomSheetType.Default, theme)
            assertEquals(Color.Transparent, default.scrimColor)
            assertEquals(modal.containerColor, default.containerColor)
            assertEquals(modal.handleColor, default.handleColor)
            assertEquals(modal.contentColor, default.contentColor)
        }
    }

    @Test
    fun propertiesCompareEveryField() {
        assertEquals(AppBottomSheetProperties(), AppBottomSheetProperties())
        assertNotEquals(AppBottomSheetProperties(), AppBottomSheetProperties(shouldDismissOnBackPress = false))
        assertNotEquals(AppBottomSheetProperties(), AppBottomSheetProperties(shouldDismissOnClickOutside = false))
        assertNotEquals(AppBottomSheetProperties(), AppBottomSheetProperties(securePolicy = AppBottomSheetSecurePolicy.SecureOn))
    }
}
