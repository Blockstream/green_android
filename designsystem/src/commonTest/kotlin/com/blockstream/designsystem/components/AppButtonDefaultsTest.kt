package com.blockstream.designsystem.components

import androidx.compose.ui.graphics.Color
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppColorsDark
import com.blockstream.designsystem.AppColorsLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppButtonDefaultsTest {
    private val themes = listOf(AppColorsDark, AppColorsLight)

    @Test
    fun primaryUsesActionTokens() {
        themes.forEach { theme ->
            val colors = AppButtonDefaults.primary(theme)
            assertEquals(theme.action.primary, colors.containerColor(AppButtonState.Default))
            assertEquals(theme.action.onPrimary, colors.contentColor(AppButtonState.Default))
            assertNull(colors.borderColor(AppButtonState.Default))
            assertEquals(theme.action.pressed, colors.containerColor(AppButtonState.Pressed))
            assertEquals(theme.text.brand, colors.contentColor(AppButtonState.Pressed))
        }
    }

    @Test
    fun outlinedVariantsHaveNoFillUntilPressed() {
        themes.forEach { theme ->
            outlined(theme).forEach { (variant, border, pressedFill) ->
                val colors = AppButtonDefaults.colors(variant, theme)
                assertEquals(Color.Transparent, colors.containerColor(AppButtonState.Default), variant.name)
                assertEquals(border, colors.borderColor(AppButtonState.Default), variant.name)
                assertEquals(pressedFill, colors.containerColor(AppButtonState.Pressed), variant.name)
                assertEquals(border, colors.borderColor(AppButtonState.Pressed), variant.name)
            }
        }
    }

    @Test
    fun tertiaryHasNoBorderAndNoFillUntilPressed() {
        themes.forEach { theme ->
            val colors = AppButtonDefaults.tertiary(theme)
            assertEquals(Color.Transparent, colors.containerColor(AppButtonState.Default))
            assertEquals(theme.text.brand, colors.contentColor(AppButtonState.Default))
            assertNull(colors.borderColor(AppButtonState.Default))
            assertEquals(theme.action.pressed, colors.containerColor(AppButtonState.Pressed))
        }
    }

    @Test
    fun invertedVariantsUseInvertedTokens() {
        themes.forEach { theme ->
            val primary = AppButtonDefaults.primaryInverted(theme)
            assertEquals(theme.action.primaryInverted, primary.containerColor(AppButtonState.Default))
            assertEquals(theme.action.onPrimaryInverted, primary.contentColor(AppButtonState.Default))
            assertEquals(theme.action.primaryInvertedPressed, primary.containerColor(AppButtonState.Pressed))

            val secondary = AppButtonDefaults.secondaryInverted(theme)
            assertEquals(theme.action.primaryInvertedPressed, secondary.containerColor(AppButtonState.Pressed))
            assertEquals(theme.action.onPrimary, secondary.contentColor(AppButtonState.Pressed))
        }
    }

    @Test
    fun disabledIsTheSameForEveryVariantExceptInverted() {
        themes.forEach { theme ->
            AppButtonVariant.entries.forEach { variant ->
                val colors = AppButtonDefaults.colors(variant, theme)
                val inverted = variant == AppButtonVariant.PrimaryInverted || variant == AppButtonVariant.SecondaryInverted
                assertEquals(
                    if (inverted) theme.action.primaryInvertedDisabled else theme.action.disabled,
                    colors.containerColor(AppButtonState.Disabled),
                    variant.name
                )
                assertEquals(theme.text.disabled, colors.contentColor(AppButtonState.Disabled), variant.name)
                assertNull(colors.borderColor(AppButtonState.Disabled), variant.name)
            }
        }
    }

    @Test
    fun stateComesFromEnabledAndPressed() {
        assertEquals(AppButtonState.Disabled, AppButtonState.of(enabled = false, pressed = true))
        assertEquals(AppButtonState.Pressed, AppButtonState.of(enabled = true, pressed = true))
        assertEquals(AppButtonState.Default, AppButtonState.of(enabled = true, pressed = false))
    }

    private fun outlined(theme: AppColors) = listOf(
        Triple(AppButtonVariant.Secondary, theme.border.brand, theme.action.pressed),
        Triple(AppButtonVariant.Negative, theme.feedback.negative.accent, theme.feedback.negative.fill),
        Triple(AppButtonVariant.SecondaryInverted, theme.border.inverse, theme.action.primaryInvertedPressed)
    )
}
