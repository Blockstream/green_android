package com.blockstream.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AppContentColorForTest {
    private val themes = listOf(AppColorsDark, AppColorsLight)

    @Test
    fun actionBackgroundsGetTheirOnColour() {
        themes.forEach { theme ->
            assertEquals(theme.action.onPrimary, theme.appContentColorFor(theme.action.primary))
            assertEquals(theme.action.onPrimaryInverted, theme.appContentColorFor(theme.action.primaryInverted))
        }
    }

    @Test
    fun feedbackFillsGetTheirOnFillColour() {
        themes.forEach { theme ->
            assertEquals(theme.feedback.info.onFill, theme.appContentColorFor(theme.feedback.info.fill))
            assertEquals(theme.feedback.positive.onFill, theme.appContentColorFor(theme.feedback.positive.fill))
            assertEquals(theme.feedback.negative.onFill, theme.appContentColorFor(theme.feedback.negative.fill))
            assertEquals(theme.feedback.warning.onFill, theme.appContentColorFor(theme.feedback.warning.fill))
        }
    }

    @Test
    fun surfacesGetPrimaryText() {
        themes.forEach { theme ->
            listOf(theme.surface.canvas, theme.surface.base, theme.surface.raised, theme.surface.sunken, theme.surface.overlay).forEach {
                assertEquals(theme.text.primary, theme.appContentColorFor(it))
            }
        }
    }

    @Test
    fun mappedBackgroundsDoNotCollide() {
        themes.forEach { theme ->
            val expected = mapOf(
                theme.surface.canvas to theme.text.primary,
                theme.surface.base to theme.text.primary,
                theme.surface.raised to theme.text.primary,
                theme.surface.sunken to theme.text.primary,
                theme.surface.overlay to theme.text.primary,
                theme.feedback.info.fill to theme.feedback.info.onFill,
                theme.feedback.positive.fill to theme.feedback.positive.onFill,
                theme.feedback.negative.fill to theme.feedback.negative.onFill,
                theme.feedback.warning.fill to theme.feedback.warning.onFill,
                theme.action.primary to theme.action.onPrimary,
                theme.action.primaryInverted to theme.action.onPrimaryInverted
            )
            expected.forEach { (background, content) ->
                assertEquals(content, theme.appContentColorFor(background), "background $background")
            }
        }
    }

    @Test
    fun lightDisabledBackgroundFallsThrough() {
        assertEquals(Color.Unspecified, AppColorsLight.appContentColorFor(AppColorsLight.action.disabled))
    }

    @Test
    fun unknownBackgroundIsUnspecified() {
        themes.forEach { theme ->
            assertEquals(Color.Unspecified, theme.appContentColorFor(Color.Magenta))
        }
    }
}
